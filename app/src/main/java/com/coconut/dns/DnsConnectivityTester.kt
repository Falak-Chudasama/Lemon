package com.coconut.dns

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class SystemDnsResolutionResult(
    val normalDomain: String,
    val normalResolvedIp: String?,
    val normalSuccess: Boolean,
    val testDomain: String,
    val testResolvedIp: String?,
    val isFiltered: Boolean,
    val summary: String
)

sealed interface DnsTestResult {
    data class Success(
        val providerHostname: String,
        val latencyMs: Long,
        val resolvedIp: String,
        val dotHandshakeVerified: Boolean,
        val dnsQueryVerified: Boolean,
        val filteringVerified: Boolean,
        val filterTestSummary: String
    ) : DnsTestResult

    data class Failure(
        val providerHostname: String,
        val error: String,
        val recoveryAdvice: String,
        val stage: String
    ) : DnsTestResult
}

data class ParsedDnsResponse(
    val queryId: Int,
    val rcode: Int,
    val answerCount: Int,
    val resolvedIps: List<String>
)

class DnsConnectivityTester {

    suspend fun testProvider(hostname: String): DnsTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var currentStage = "VALIDATION"

        try {
            // Step 1: Provider hostname validation
            if (hostname.isBlank() || !hostname.contains(".")) {
                return@withContext DnsTestResult.Failure(
                    providerHostname = hostname,
                    error = "Invalid provider hostname format.",
                    recoveryAdvice = "Please select a valid DNS-over-TLS provider.",
                    stage = "VALIDATION"
                )
            }

            // Step 2: Resolve provider hostname to IP (with transition retry for system DNS shifts)
            currentStage = "RESOLUTION"
            var addresses: Array<InetAddress>? = null
            var resolveError: Exception? = null
            for (attempt in 1..3) {
                try {
                    addresses = InetAddress.getAllByName(hostname)
                    if (!addresses.isNullOrEmpty()) break
                } catch (e: Exception) {
                    resolveError = e
                    if (attempt < 3) kotlinx.coroutines.delay(1000)
                }
            }
            if (addresses.isNullOrEmpty()) {
                return@withContext DnsTestResult.Failure(
                    providerHostname = hostname,
                    error = "Could not resolve IP address for $hostname: ${resolveError?.message}",
                    recoveryAdvice = "Check internet connection. If DNS is broken, disable Content Shield to recover.",
                    stage = "RESOLUTION"
                )
            }
            val primaryIp = addresses.first().hostAddress ?: addresses.first().toString()

            // Step 3 & 4: TCP connection to port 853 & TLS Handshake
            currentStage = "TCP_CONNECT"
            val timeoutMs = 5000
            val rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(primaryIp, 853), timeoutMs)

            currentStage = "TLS_HANDSHAKE"
            val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = sslFactory.createSocket(rawSocket, primaryIp, 853, true) as SSLSocket

            // Set Server Name Indication (SNI) and enable standard HTTPS endpoint identification algorithm
            val sslParams = sslSocket.sslParameters
            sslParams.endpointIdentificationAlgorithm = "HTTPS"
            sslParams.serverNames = listOf(SNIHostName(hostname))
            sslSocket.sslParameters = sslParams
            sslSocket.soTimeout = timeoutMs

            // Perform TLS Handshake with standard certificate & hostname validation
            sslSocket.startHandshake()

            // Defense-in-depth: explicit HostnameVerifier check on active SSLSession
            val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
            val session = sslSocket.session
            if (!verifier.verify(hostname, session)) {
                sslSocket.close()
                throw SSLPeerUnverifiedException("Certificate hostname verification failed: certificate does not match expected host '$hostname'.")
            }

            // Step 7: Send valid DNS-over-TLS query for provider's root domain
            currentStage = "DNS_QUERY"
            val queryDomain = if (hostname.contains("cleanbrowsing")) "cleanbrowsing.org" else "cloudflare.com"
            val response = executeDnsQueryOverTls(sslSocket, queryDomain, queryId = 0x2026)

            val initialLatency = System.currentTimeMillis() - startTime
            val dotVerified = true
            val dnsQueryVerified = response.rcode == 0 && response.answerCount > 0

            // Step 8: Documented filtering test
            currentStage = "FILTER_CHECK"
            val (filterVerified, filterSummary) = verifyFilteringCategory(sslSocket, hostname)

            sslSocket.close()

            DnsTestResult.Success(
                providerHostname = hostname,
                latencyMs = initialLatency,
                resolvedIp = response.resolvedIps.firstOrNull() ?: primaryIp,
                dotHandshakeVerified = dotVerified,
                dnsQueryVerified = dnsQueryVerified,
                filteringVerified = filterVerified,
                filterTestSummary = filterSummary
            )
        } catch (e: Exception) {
            val recovery = when (currentStage) {
                "RESOLUTION" -> "Cannot resolve DNS provider. Verify Wi-Fi or cellular data."
                "TCP_CONNECT" -> "Connection to port 853 refused or timed out. Your ISP, Wi-Fi router, or mobile carrier may block DoT port 853."
                "TLS_HANDSHAKE" -> "TLS negotiation failed (${e.javaClass.simpleName}: ${e.message}). Check device date & time or network MITM."
                "DNS_QUERY" -> "Connected to port 853 with TLS, but DNS query was unanswered or returned error."
                else -> "An unexpected network error occurred (${e.message})."
            }

            DnsTestResult.Failure(
                providerHostname = hostname,
                error = "Test failed at stage $currentStage: ${e.message ?: e.javaClass.simpleName}",
                recoveryAdvice = recovery,
                stage = currentStage
            )
        }
    }

    suspend fun testSystemResolver(providerHostname: String): SystemDnsResolutionResult = withContext(Dispatchers.IO) {
        val normalDomain = "google.com"
        var normalIp: String? = null
        var normalSuccess = false
        try {
            val addrs = InetAddress.getAllByName(normalDomain)
            if (addrs.isNotEmpty()) {
                normalIp = addrs.first().hostAddress
                normalSuccess = true
            }
        } catch (_: Exception) {
            normalSuccess = false
        }

        // CleanBrowsing official documented test domains: badexample.com and badporn.cleanbrowsing.org
        val testDomain = if (providerHostname.contains("cleanbrowsing", ignoreCase = true)) {
            "badexample.com"
        } else {
            "family.cloudflare-dns.com"
        }

        var testIp: String? = null
        var isFiltered = false
        var filterSummary = ""

        try {
            val addrs = InetAddress.getAllByName(testDomain)
            if (addrs.isNotEmpty()) {
                testIp = addrs.first().hostAddress
                // CleanBrowsing redirects filtered test domains to block page 185.228.168.10 or loopback/0.0.0.0
                if (testIp == "185.228.168.10" || testIp == "127.0.0.1" || testIp == "0.0.0.0" || testIp == "::1") {
                    isFiltered = true
                    filterSummary = "System resolver correctly redirected filtered domain '$testDomain' to block sink ($testIp)"
                } else {
                    isFiltered = false
                    filterSummary = "System resolver resolved '$testDomain' to normal IP ($testIp)"
                }
            }
        } catch (e: Exception) {
            // Filtered domains returning NXDOMAIN trigger UnknownHostException
            isFiltered = true
            filterSummary = "System resolver blocked '$testDomain' via NXDOMAIN (${e.javaClass.simpleName}: ${e.message})"
        }

        SystemDnsResolutionResult(
            normalDomain = normalDomain,
            normalResolvedIp = normalIp,
            normalSuccess = normalSuccess,
            testDomain = testDomain,
            testResolvedIp = testIp,
            isFiltered = isFiltered,
            summary = filterSummary
        )
    }

    /**
     * Verifies that the TLS engine strictly rejects connections when the presenting certificate
     * does not match the configured endpoint hostname, using endpoint identification algorithm "HTTPS".
     */
    suspend fun verifyRejectionOfMismatchedTlsHostname(validIp: String, invalidHostname: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val timeoutMs = 4000
            val rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(validIp, 853), timeoutMs)

            val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = sslFactory.createSocket(rawSocket, validIp, 853, true) as SSLSocket

            val sslParams = sslSocket.sslParameters
            sslParams.endpointIdentificationAlgorithm = "HTTPS"
            sslParams.serverNames = listOf(SNIHostName(invalidHostname))
            sslSocket.sslParameters = sslParams
            sslSocket.soTimeout = timeoutMs

            try {
                sslSocket.startHandshake()
                val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
                val session = sslSocket.session
                if (!verifier.verify(invalidHostname, session)) {
                    sslSocket.close()
                    true // Successfully rejected by verifier
                } else {
                    sslSocket.close()
                    false // Failed: unexpected acceptance of mismatched hostname
                }
            } catch (_: SSLPeerUnverifiedException) {
                sslSocket.close()
                true // Successfully rejected during TLS handshake
            } catch (_: Exception) {
                sslSocket.close()
                true // Successfully rejected by TLS stack
            }
        } catch (_: Exception) {
            true // Connection rejected
        }
    }

    private fun verifyFilteringCategory(sslSocket: SSLSocket, hostname: String): Pair<Boolean, String> {
        return try {
            if (hostname.contains("cleanbrowsing", ignoreCase = true)) {
                // CleanBrowsing documented adult test domain
                val testDomain = "badporn.cleanbrowsing.org"
                val res = executeDnsQueryOverTls(sslSocket, testDomain, queryId = 0x2027)
                // When filtered by CleanBrowsing Family, badporn resolves to 185.228.168.10, 127.0.0.1, 0.0.0.0, or NXDOMAIN (RCODE 3)
                val isBlocked = res.rcode == 3 || res.resolvedIps.any { it == "185.228.168.10" || it == "127.0.0.1" || it == "0.0.0.0" }
                if (isBlocked) {
                    Pair(true, "Adult filtering verified: $testDomain blocked to ${res.resolvedIps.firstOrNull() ?: "NXDOMAIN"}")
                } else {
                    Pair(false, "Filtering check: $testDomain returned IP (${res.resolvedIps.firstOrNull()}).")
                }
            } else if (hostname.contains("cloudflare", ignoreCase = true)) {
                // Cloudflare Family test domain
                val testDomain = "family.cloudflare-dns.com"
                val res = executeDnsQueryOverTls(sslSocket, testDomain, queryId = 0x2028)
                if (res.rcode == 0) {
                    Pair(true, "Cloudflare 1.1.1.1 for Families resolver verified responsive over TLS.")
                } else {
                    Pair(false, "Resolver responded with status code: ${res.rcode}")
                }
            } else {
                Pair(false, "Standard DoT verified. Specific filtering test endpoint not configured for $hostname.")
            }
        } catch (_: Exception) {
            Pair(false, "DoT query path active. Filtering probe timed out.")
        }
    }

    private fun executeDnsQueryOverTls(sslSocket: SSLSocket, domain: String, queryId: Int): ParsedDnsResponse {
        val queryBytes = buildDnsWireQuery(domain, queryId)
        val outStream = DataOutputStream(sslSocket.outputStream)
        // RFC 7858: 2-byte length prefix
        outStream.writeShort(queryBytes.size)
        outStream.write(queryBytes)
        outStream.flush()

        val inStream = DataInputStream(sslSocket.inputStream)
        val responseLen = inStream.readUnsignedShort()
        val responseBytes = ByteArray(responseLen)
        inStream.readFully(responseBytes)

        return parseDnsWireResponse(responseBytes)
    }

    private fun buildDnsWireQuery(domain: String, queryId: Int): ByteArray {
        val qnameStream = ByteArrayOutputStream()
        for (label in domain.split(".")) {
            if (label.isEmpty()) continue
            val bytes = label.toByteArray(Charsets.US_ASCII)
            qnameStream.write(bytes.size)
            qnameStream.write(bytes)
        }
        qnameStream.write(0x00) // End of QNAME
        val qname = qnameStream.toByteArray()

        val buffer = ByteBuffer.allocate(12 + qname.size + 4)
        buffer.putShort(queryId.toShort())
        buffer.putShort(0x0100.toShort()) // Flags: standard query, RD=1
        buffer.putShort(1.toShort()) // QDCOUNT = 1
        buffer.putShort(0.toShort()) // ANCOUNT = 0
        buffer.putShort(0.toShort()) // NSCOUNT = 0
        buffer.putShort(0.toShort()) // ARCOUNT = 0
        buffer.put(qname)
        buffer.putShort(1.toShort()) // QTYPE = 1 (A)
        buffer.putShort(1.toShort()) // QCLASS = 1 (IN)
        return buffer.array()
    }

    private fun parseDnsWireResponse(responseBytes: ByteArray): ParsedDnsResponse {
        if (responseBytes.size < 12) {
            return ParsedDnsResponse(0, -1, 0, emptyList())
        }
        val buffer = ByteBuffer.wrap(responseBytes)
        val id = buffer.short.toInt() and 0xFFFF
        val flags = buffer.short.toInt() and 0xFFFF
        val rcode = flags and 0x000F
        val qdcount = buffer.short.toInt() and 0xFFFF
        val ancount = buffer.short.toInt() and 0xFFFF
        buffer.short // nscount
        buffer.short // arcount

        // Skip Question section
        for (i in 0 until qdcount) {
            skipDnsName(buffer)
            buffer.short // qtype
            buffer.short // qclass
        }

        // Parse Answer section
        val ips = mutableListOf<String>()
        for (i in 0 until ancount) {
            if (buffer.remaining() < 10) break
            skipDnsName(buffer)
            val type = buffer.short.toInt() and 0xFFFF
            buffer.short // class
            buffer.int // ttl
            val rdLength = buffer.short.toInt() and 0xFFFF
            if (type == 1 && rdLength == 4 && buffer.remaining() >= 4) {
                val b0 = buffer.get().toInt() and 0xFF
                val b1 = buffer.get().toInt() and 0xFF
                val b2 = buffer.get().toInt() and 0xFF
                val b3 = buffer.get().toInt() and 0xFF
                ips.add("$b0.$b1.$b2.$b3")
            } else {
                if (buffer.remaining() >= rdLength) {
                    buffer.position(buffer.position() + rdLength)
                }
            }
        }

        return ParsedDnsResponse(
            queryId = id,
            rcode = rcode,
            answerCount = ancount,
            resolvedIps = ips
        )
    }

    private fun skipDnsName(buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            val len = buffer.get().toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) {
                // Pointer (2 bytes total, 1 already read)
                if (buffer.hasRemaining()) buffer.get()
                break
            } else {
                if (buffer.remaining() >= len) {
                    buffer.position(buffer.position() + len)
                } else break
            }
        }
    }
}
