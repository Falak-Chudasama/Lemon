package com.coconut.dns

import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.Network
import android.os.CancellationSignal
import com.coconut.data.DnsConfigurationEntity
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executor
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class DnsUpstreamPreset(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryIp: String,
    val secondaryIp: String?,
    val hostname: String,
    val dotPort: Int = 853,
    val dohUrl: String = "",
    val supportsDoT: Boolean = true,
    val supportsDoH: Boolean = true
)

object DnsUpstreamPresets {
    val PRESETS = listOf(
        DnsUpstreamPreset(
            id = "SYSTEM",
            displayName = "System Default",
            description = "Use physical network default DNS provider",
            primaryIp = "",
            secondaryIp = null,
            hostname = "",
            supportsDoT = false,
            supportsDoH = false
        ),
        DnsUpstreamPreset(
            id = "ADGUARD_DEFAULT",
            displayName = "AdGuard Default",
            description = "dns.adguard-dns.com — Ad & tracker blocking",
            primaryIp = "94.140.14.14",
            secondaryIp = "94.140.15.15",
            hostname = "dns.adguard-dns.com",
            dotPort = 853,
            dohUrl = "https://dns.adguard-dns.com/dns-query"
        ),
        DnsUpstreamPreset(
            id = "ADGUARD_FAMILY",
            displayName = "AdGuard Family",
            description = "family.adguard-dns.com — Family protection & SafeSearch",
            primaryIp = "94.140.14.15",
            secondaryIp = "94.140.15.16",
            hostname = "family.adguard-dns.com",
            dotPort = 853,
            dohUrl = "https://family.adguard-dns.com/dns-query"
        ),
        DnsUpstreamPreset(
            id = "CLEANBROWSING_FAMILY",
            displayName = "CleanBrowsing Family",
            description = "family-filter-dns.cleanbrowsing.org — Filters adult content",
            primaryIp = "185.228.168.168",
            secondaryIp = "185.228.169.168",
            hostname = "family-filter-dns.cleanbrowsing.org",
            dotPort = 853,
            dohUrl = "https://doh.cleanbrowsing.org/doh/family-filter/"
        ),
        DnsUpstreamPreset(
            id = "CLOUDFLARE",
            displayName = "Cloudflare (1.1.1.1)",
            description = "High-speed privacy-first resolver",
            primaryIp = "1.1.1.1",
            secondaryIp = "1.0.0.1",
            hostname = "one.one.one.one",
            dotPort = 853,
            dohUrl = "https://cloudflare-dns.com/dns-query"
        ),
        DnsUpstreamPreset(
            id = "GOOGLE",
            displayName = "Google Public DNS",
            description = "Fast, resilient global DNS",
            primaryIp = "8.8.8.8",
            secondaryIp = "8.8.4.4",
            hostname = "dns.google",
            dotPort = 853,
            dohUrl = "https://dns.google/dns-query"
        ),
        DnsUpstreamPreset(
            id = "CUSTOM",
            displayName = "Custom Upstream",
            description = "Custom IP, DoT hostname, or DoH URL",
            primaryIp = "",
            secondaryIp = null,
            hostname = "",
            dotPort = 853,
            dohUrl = ""
        )
    )

    fun getPreset(id: String): DnsUpstreamPreset {
        return PRESETS.firstOrNull { it.id == id } ?: PRESETS.first()
    }
}

class DnsUpstreamResolver(
    private val executor: Executor
) {

    /**
     * Resolves DNS query upstream using the configured provider policy.
     * Always routes through the physical network (protecting against VPN routing loops).
     * Supports Plaintext UDP (port 53), DNS-over-TLS (DoT, port 853), and DNS-over-HTTPS (DoH, port 443).
     * Applies primary provider first, falling back to secondary and system resolver if primary fails.
     */
    fun resolve(
        query: ParsedDnsQuery,
        config: DnsConfigurationEntity?,
        physicalNetwork: Network?,
        connectivityManager: ConnectivityManager,
        onSuccess: (ByteArray) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val network = physicalNetwork ?: connectivityManager.activeNetwork
        if (network == null) {
            onError(IllegalStateException("No physical network available for DNS upstream"))
            return
        }

        val providerId = config?.primaryUpstream ?: "SYSTEM"
        if (providerId == "SYSTEM" || providerId.isBlank()) {
            queryViaSystemResolver(query, network, onSuccess, onError)
            return
        }

        val preset = DnsUpstreamPresets.getPreset(providerId)
        val protocol = config?.upstreamProtocol ?: "UDP"

        val primaryTargetIp = if (providerId == "CUSTOM") config?.customUpstreamEndpoint?.trim() ?: "" else preset.primaryIp
        val secondaryTargetIp = if (providerId == "CUSTOM") config?.secondaryUpstream?.trim() ?: "" else (preset.secondaryIp ?: "")
        val hostname = preset.hostname.ifBlank { primaryTargetIp }
        val dohUrl = preset.dohUrl.ifBlank {
            if (providerId == "CUSTOM" && config?.customUpstreamEndpoint?.startsWith("http") == true) {
                config.customUpstreamEndpoint
            } else ""
        }

        executor.execute {
            try {
                val response = when (protocol.uppercase()) {
                    "DOT" -> {
                        resolveViaDoT(query.rawDnsPayload, primaryTargetIp, hostname, preset.dotPort, network)
                    }
                    "DOH" -> {
                        if (dohUrl.isNotBlank()) {
                            resolveViaDoH(query.rawDnsPayload, dohUrl, primaryTargetIp, network)
                        } else {
                            forwardUdpDnsDirect(query.rawDnsPayload, primaryTargetIp, 53, network, 3000)
                        }
                    }
                    else -> { // "UDP" or plaintext
                        forwardUdpDnsDirect(query.rawDnsPayload, primaryTargetIp, 53, network, 3000)
                    }
                }
                onSuccess(response)
            } catch (_: Exception) {
                // Secondary Failover
                if (secondaryTargetIp.isNotBlank()) {
                    try {
                        val secondaryResponse = when (protocol.uppercase()) {
                            "DOT" -> resolveViaDoT(query.rawDnsPayload, secondaryTargetIp, hostname, preset.dotPort, network)
                            "DOH" -> if (dohUrl.isNotBlank()) resolveViaDoH(query.rawDnsPayload, dohUrl, secondaryTargetIp, network)
                                     else forwardUdpDnsDirect(query.rawDnsPayload, secondaryTargetIp, 53, network, 3000)
                            else -> forwardUdpDnsDirect(query.rawDnsPayload, secondaryTargetIp, 53, network, 3000)
                        }
                        onSuccess(secondaryResponse)
                        return@execute
                    } catch (_: Exception) {}
                }

                // Final Fallback: try native system resolver
                queryViaSystemResolver(query, network, onSuccess, onError)
            }
        }
    }

    /**
     * Resolves a DNS query via DNS-over-TLS (DoT, RFC 7858).
     * Binds socket to physical network before TLS handshake, validates TLS certificates,
     * verifies SNI hostname, and formats RFC 7858 2-byte length-prefixed frames.
     */
    fun resolveViaDoT(
        dnsPayload: ByteArray,
        ipAddress: String,
        hostname: String,
        port: Int,
        network: Network,
        timeoutMs: Int = 3000
    ): ByteArray {
        val targetIp = if (ipAddress.isNotBlank()) {
            ipAddress
        } else {
            // Bootstrap resolution via physical network
            network.getAllByName(hostname).firstOrNull()?.hostAddress
                ?: throw IOException("Bootstrap resolution failed for $hostname")
        }

        val plainSocket = Socket()
        try {
            // CRUCIAL: Bind socket to physical network so TLS handshake bypasses VPN TUN
            network.bindSocket(plainSocket)
            plainSocket.connect(InetSocketAddress(targetIp, port), timeoutMs)
            plainSocket.soTimeout = timeoutMs

            val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = sslFactory.createSocket(plainSocket, hostname, port, true) as SSLSocket

            // Configure SNI hostname
            val sslParams = sslSocket.sslParameters
            sslParams.serverNames = listOf(SNIHostName(hostname))
            sslSocket.sslParameters = sslParams

            // Perform TLS handshake and certificate validation
            sslSocket.startHandshake()

            // Verify hostname matches certificate CN / SAN
            val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
            if (!verifier.verify(hostname, sslSocket.session)) {
                throw SSLPeerUnverifiedException("DoT hostname verification failed for $hostname")
            }

            // Write RFC 7858 2-byte length-prefixed query
            val outStream = DataOutputStream(sslSocket.outputStream)
            outStream.writeShort(dnsPayload.size)
            outStream.write(dnsPayload)
            outStream.flush()

            // Read RFC 7858 2-byte length-prefixed response
            val inStream = DataInputStream(sslSocket.inputStream)
            val respLen = inStream.readUnsignedShort()
            val response = ByteArray(respLen)
            inStream.readFully(response)

            return response
        } finally {
            try { plainSocket.close() } catch (_: Exception) {}
        }
    }

    /**
     * Resolves a DNS query via DNS-over-HTTPS (DoH, RFC 8484).
     * Uses HTTPS POST with application/dns-message wire format bound to physical network.
     */
    fun resolveViaDoH(
        dnsPayload: ByteArray,
        dohUrl: String,
        bootstrapIp: String?,
        network: Network,
        timeoutMs: Int = 3000
    ): ByteArray {
        val url = URL(dohUrl)
        val connection = network.openConnection(url) as HttpsURLConnection
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/dns-message")
            connection.setRequestProperty("Accept", "application/dns-message")
            connection.doOutput = true

            // Send raw query bytes in request body
            connection.outputStream.use { out ->
                out.write(dnsPayload)
                out.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("DoH upstream returned HTTP $responseCode")
            }

            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Sends DNS query directly to target upstream IP via UDP DatagramSocket
     * explicitly bound to the physical network so it never loops back into the VPN interface.
     */
    fun forwardUdpDnsDirect(
        dnsPayload: ByteArray,
        ipAddress: String,
        port: Int,
        network: Network,
        timeoutMs: Int = 3000
    ): ByteArray {
        val socket = DatagramSocket()
        try {
            network.bindSocket(socket)
            socket.soTimeout = timeoutMs

            val destAddress = InetAddress.getByName(ipAddress)
            val sendPacket = DatagramPacket(dnsPayload, dnsPayload.size, destAddress, port)
            socket.send(sendPacket)

            val receiveBuf = ByteArray(4096)
            val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
            socket.receive(receivePacket)

            val result = ByteArray(receivePacket.length)
            System.arraycopy(receivePacket.data, 0, result, 0, receivePacket.length)
            return result
        } finally {
            socket.close()
        }
    }

    private fun queryViaSystemResolver(
        query: ParsedDnsQuery,
        network: Network,
        onSuccess: (ByteArray) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val cancellationSignal = CancellationSignal()
        try {
            DnsResolver.getInstance().rawQuery(
                network,
                query.rawDnsPayload,
                DnsResolver.FLAG_NO_CACHE_LOOKUP,
                executor,
                cancellationSignal,
                object : DnsResolver.Callback<ByteArray> {
                    override fun onAnswer(answer: ByteArray, rcode: Int) {
                        onSuccess(answer)
                    }

                    override fun onError(error: DnsResolver.DnsException) {
                        onError(error)
                    }
                }
            )
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Connectivity and latency test for an upstream provider.
     */
    fun testProviderConnectivity(
        ipAddress: String,
        network: Network?,
        connectivityManager: ConnectivityManager
    ): Long {
        val net = network ?: connectivityManager.activeNetwork ?: return -1L
        val testDomain = "google.com"
        val queryBytes = DnsPacketParser.buildTestDnsQuery(testDomain)
        val startTime = System.currentTimeMillis()
        return try {
            val response = forwardUdpDnsDirect(queryBytes, ipAddress, 53, net, timeoutMs = 2500)
            if (response.isNotEmpty()) {
                System.currentTimeMillis() - startTime
            } else {
                -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }
}
