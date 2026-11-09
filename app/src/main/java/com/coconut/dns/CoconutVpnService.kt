package com.coconut.dns

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.coconut.MainActivity
import com.coconut.R
import com.coconut.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

private data class TcpDnsSession(
    val key: String,
    val isIpv6: Boolean,
    var serverSeq: Long,
    var clientSeq: Long,
    val rxBuffer: ByteArrayOutputStream = ByteArrayOutputStream(),
    var createdAt: Long = System.currentTimeMillis()
)

class CoconutVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val policyEngine = DomainPolicyEngine()
    private val dnsCache = DnsCache(maxEntries = 1024)
    private lateinit var blocklistManager: BlocklistManager
    private val dnsExecutor = Executors.newFixedThreadPool(4)
    private val upstreamResolver = DnsUpstreamResolver(dnsExecutor)
    private val tcpSessions = ConcurrentHashMap<String, TcpDnsSession>()
    @Volatile private var currentDnsConfig: com.coconut.data.DnsConfigurationEntity? = null

    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null
    private var activePhysicalNetwork: Network? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        private const val NOTIFICATION_ID = 2026
        private const val CHANNEL_ID = "coconut_local_dns_channel"

        const val ACTION_START_VPN = "com.coconut.action.START_VPN"
        const val ACTION_STOP_VPN = "com.coconut.action.STOP_VPN"

        private val _isVpnRunning = MutableStateFlow(false)
        val isVpnRunning = _isVpnRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, CoconutVpnService::class.java).apply {
                action = ACTION_START_VPN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CoconutVpnService::class.java).apply {
                action = ACTION_STOP_VPN
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        blocklistManager = BlocklistManager(this)
        createNotificationChannel()

        // Sync rules from database continuously
        serviceScope.launch {
            val db = AppDatabase.get(applicationContext)
            val ruleDao = db.domainRuleDao()
            ruleDao.getAllRulesFlow().collectLatest { rules ->
                val allowlist = rules.filter { it.ruleType == "ALLOW" && it.isEnabled }.map { it.domain }
                val blocklist = rules.filter { it.ruleType == "BLOCK" && it.isEnabled }.map { it.domain }
                policyEngine.updateRules(allowlist, blocklist)
                dnsCache.clear() // Invalidate cache so newly added block rules take effect immediately
            }
        }

        // Sync configuration toggles from database
        serviceScope.launch {
            val db = AppDatabase.get(applicationContext)
            val configDao = db.dnsConfigurationDao()
            configDao.getConfigurationFlow().collectLatest { config ->
                if (config != null) {
                    currentDnsConfig = config
                    policyEngine.blockThreatsEnabled = config.blockThreats
                    policyEngine.blockAdultEnabled = config.blockAdultContent
                    policyEngine.blockAdsTrackersEnabled = config.blockAdsTrackers

                    if (config.blockThreats && !policyEngine.isThreatActive()) {
                        val threats = blocklistManager.loadThreatDomains()
                        policyEngine.updateThreatList(threats)
                    }
                    if (config.blockAdultContent && !policyEngine.isAdultActive()) {
                        val adult = blocklistManager.loadAdultDomains()
                        policyEngine.updateAdultList(adult)
                    }
                    if (config.blockAdsTrackers && !policyEngine.isAdsTrackersActive()) {
                        val ads = blocklistManager.loadAdsTrackersDomains()
                        policyEngine.updateAdsTrackersList(ads)
                    }
                }
            }
        }

        registerPhysicalNetworkTracker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            stopVpnInternal()
            return START_NOT_STICKY
        }

        startVpnInternal()
        return START_STICKY
    }

    private fun startVpnInternal() {
        if (_isVpnRunning.value) return

        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            // Configure DNS-only virtual TUN endpoint
            // Narrow routes: only route virtual DNS endpoints (10.255.0.2 and fd00::2)
            // DO NOT route 0.0.0.0/0 or ::/0!
            val builder = Builder()
                .setSession("Lemon Local DNS")
                .setMtu(1500)
                .addAddress("10.255.0.1", 32)
                .addDnsServer("10.255.0.2")
                .addRoute("10.255.0.2", 32)
                .addAddress("fd00::1", 128)
                .addDnsServer("fd00::2")
                .addRoute("fd00::2", 128)
                .setBlocking(true)

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                _isVpnRunning.value = true
                com.coconut.policy.LemonPolicyCoordinator.getInstance(this).onTunnelEstablished()
                startPacketLoop(vpnInterface!!)
            } else {
                stopVpnInternal()
                com.coconut.policy.LemonPolicyCoordinator.getInstance(this).onServiceStoppedUnexpectedly()
            }
        } catch (_: Exception) {
            stopVpnInternal()
            com.coconut.policy.LemonPolicyCoordinator.getInstance(this).onServiceStoppedUnexpectedly()
        }
    }

    private fun startPacketLoop(pfd: ParcelFileDescriptor) {
        packetLoopJob = serviceScope.launch {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val packetBuffer = ByteArray(4096)

            while (isActive) {
                try {
                    val bytesRead = inputStream.read(packetBuffer)
                    if (bytesRead <= 0) continue

                    val ipInfo = DnsPacketParser.parseIpPacket(packetBuffer, bytesRead) ?: continue
                    if (ipInfo.dstPort != 53) {
                        // Safely drop non-DNS traffic addressed to the virtual endpoint
                        continue
                    }

                    if (ipInfo.protocol == 6) {
                        // DNS-over-TCP transaction handling (RFC 1035 / RFC 7766)
                        handleTcpDnsPacket(ipInfo, packetBuffer, outputStream)
                        continue
                    }

                    if (ipInfo.protocol != 17) {
                        // Safely drop non-UDP traffic
                        continue
                    }

                    val query = DnsPacketParser.parseDnsQuery(
                        packetBuffer,
                        ipInfo.transportPayloadOffset,
                        ipInfo.transportPayloadLength
                    ) ?: continue

                    // 1. Evaluate policy
                    val decision = policyEngine.evaluate(query.question.qname)

                    if (decision != PolicyDecision.ALLOW) {
                        // BLOCKED: Return synthetic local NXDOMAIN response
                        val nxDomainDns = DnsPacketParser.buildNxDomainResponse(query)
                        val responsePacket = DnsPacketParser.wrapInIpUdpPacket(
                            dnsPayload = nxDomainDns,
                            srcIp = ipInfo.dstIp, // Swap src and dst
                            dstIp = ipInfo.srcIp,
                            srcPort = ipInfo.dstPort,
                            dstPort = ipInfo.srcPort,
                            isIpv6 = ipInfo.isIpv6
                        )
                        synchronized(outputStream) {
                            outputStream.write(responsePacket)
                            outputStream.flush()
                        }
                    } else {
                        // ALLOWED: Check local DNS cache
                        val cachedDns = dnsCache.get(query.question.qname, query.question.qtype, query.txId)
                        if (cachedDns != null) {
                            val cachedPacket = DnsPacketParser.wrapInIpUdpPacket(
                                dnsPayload = cachedDns,
                                srcIp = ipInfo.dstIp,
                                dstIp = ipInfo.srcIp,
                                srcPort = ipInfo.dstPort,
                                dstPort = ipInfo.srcPort,
                                isIpv6 = ipInfo.isIpv6
                            )
                            synchronized(outputStream) {
                                outputStream.write(cachedPacket)
                                outputStream.flush()
                            }
                        } else {
                            // CACHE MISS: Forward through Android native DnsResolver on physical network
                            forwardUpstreamDns(query, ipInfo, outputStream)
                        }
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
        }
    }

    private fun forwardUpstreamDns(
        query: ParsedDnsQuery,
        ipInfo: IpPacketInfo,
        tunOutStream: FileOutputStream
    ) {
        val cm = getSystemService(ConnectivityManager::class.java)
        upstreamResolver.resolve(
            query = query,
            config = currentDnsConfig,
            physicalNetwork = activePhysicalNetwork,
            connectivityManager = cm,
            onSuccess = { answer ->
                try {
                    // Inspect response for CNAME targets against policy
                    val (minTtl, cnames) = DnsPacketParser.inspectResponse(answer)
                    var blockedByCname = false
                    for (cname in cnames) {
                        if (policyEngine.evaluate(cname) != PolicyDecision.ALLOW) {
                            blockedByCname = true
                            break
                        }
                    }

                    val finalDnsPayload = if (blockedByCname) {
                        DnsPacketParser.buildNxDomainResponse(query)
                    } else {
                        dnsCache.put(query.question.qname, query.question.qtype, answer, minTtl)
                        answer
                    }

                    val responsePacket = DnsPacketParser.wrapInIpUdpPacket(
                        dnsPayload = finalDnsPayload,
                        srcIp = ipInfo.dstIp,
                        dstIp = ipInfo.srcIp,
                        srcPort = ipInfo.dstPort,
                        dstPort = ipInfo.srcPort,
                        isIpv6 = ipInfo.isIpv6
                    )
                    synchronized(tunOutStream) {
                        tunOutStream.write(responsePacket)
                        tunOutStream.flush()
                    }
                } catch (_: Exception) {}
            },
            onError = {
                sendServFail(query, ipInfo, tunOutStream)
            }
        )
    }

    private fun sendServFail(query: ParsedDnsQuery, ipInfo: IpPacketInfo, tunOutStream: FileOutputStream) {
        try {
            val servFail = DnsPacketParser.buildServFailResponse(query.txId, query.question.rawQuestionBytes)
            val packet = DnsPacketParser.wrapInIpUdpPacket(
                dnsPayload = servFail,
                srcIp = ipInfo.dstIp,
                dstIp = ipInfo.srcIp,
                srcPort = ipInfo.dstPort,
                dstPort = ipInfo.srcPort,
                isIpv6 = ipInfo.isIpv6
            )
            synchronized(tunOutStream) {
                tunOutStream.write(packet)
                tunOutStream.flush()
            }
        } catch (_: Exception) {}
    }

    private fun handleTcpDnsPacket(
        ipInfo: IpPacketInfo,
        packetBuffer: ByteArray,
        outputStream: FileOutputStream
    ) {
        val sessionKey = (if (ipInfo.isIpv6) "6:" else "4:") +
                ipInfo.srcIp.joinToString(".") { (it.toInt() and 0xFF).toString() } +
                ":" + ipInfo.srcPort

        pruneStaleTcpSessions()

        // 1. RST received: immediately clean up session
        if ((ipInfo.tcpFlags and 0x04) != 0) {
            tcpSessions.remove(sessionKey)
            return
        }

        // 2. SYN received: initiate 3-way handshake with SYN-ACK
        if ((ipInfo.tcpFlags and 0x02) != 0) {
            val serverSeq = (System.currentTimeMillis() and 0x7FFFFFFF)
            val synAck = DnsPacketParser.buildTcpSynAckPacket(ipInfo, serverSeq)
            synchronized(outputStream) {
                outputStream.write(synAck)
                outputStream.flush()
            }
            val session = TcpDnsSession(
                key = sessionKey,
                isIpv6 = ipInfo.isIpv6,
                serverSeq = serverSeq + 1L,
                clientSeq = ipInfo.tcpSeqNum + 1L
            )
            tcpSessions[sessionKey] = session
            return
        }

        // 3. Existing session check
        val session = tcpSessions[sessionKey]
        if (session == null) {
            // Stray or timed out packet without active session: reset
            if ((ipInfo.tcpFlags and 0x01) == 0 && (ipInfo.tcpFlags and 0x04) == 0) {
                val rstPacket = DnsPacketParser.buildTcpRstPacket(ipInfo)
                synchronized(outputStream) {
                    outputStream.write(rstPacket)
                    outputStream.flush()
                }
            }
            return
        }

        // 4. FIN received: acknowledge client teardown
        if ((ipInfo.tcpFlags and 0x01) != 0) {
            val ack = DnsPacketParser.buildTcpAckPacket(ipInfo, session.serverSeq, ipInfo.tcpSeqNum + 1)
            synchronized(outputStream) {
                outputStream.write(ack)
                outputStream.flush()
            }
            tcpSessions.remove(sessionKey)
            return
        }

        // 5. Data payload received: accumulate into RFC 1035 / 7766 stream buffer
        if (ipInfo.transportPayloadLength > 0) {
            session.clientSeq = ipInfo.tcpSeqNum + ipInfo.transportPayloadLength
            session.rxBuffer.write(packetBuffer, ipInfo.transportPayloadOffset, ipInfo.transportPayloadLength)
            val data = session.rxBuffer.toByteArray()
            if (data.size >= 2) {
                val expectedLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                if (data.size >= 2 + expectedLen) {
                    val query = DnsPacketParser.parseDnsQuery(data, 2, expectedLen)
                    if (query != null) {
                        handleTcpDnsQuery(query, ipInfo, session, outputStream)
                    } else {
                        val rst = DnsPacketParser.buildTcpRstPacket(ipInfo)
                        synchronized(outputStream) {
                            outputStream.write(rst)
                            outputStream.flush()
                        }
                        tcpSessions.remove(sessionKey)
                    }
                }
            }
        }
    }

    private fun handleTcpDnsQuery(
        query: ParsedDnsQuery,
        ipInfo: IpPacketInfo,
        session: TcpDnsSession,
        outputStream: FileOutputStream
    ) {
        val decision = policyEngine.evaluate(query.question.qname)
        if (decision != PolicyDecision.ALLOW) {
            val nxDomain = DnsPacketParser.buildNxDomainResponse(query)
            sendTcpDnsResponse(nxDomain, ipInfo, session, outputStream)
            tcpSessions.remove(session.key)
        } else {
            val cached = dnsCache.get(query.question.qname, query.question.qtype, query.txId)
            if (cached != null) {
                sendTcpDnsResponse(cached, ipInfo, session, outputStream)
                tcpSessions.remove(session.key)
            } else {
                forwardUpstreamTcpDns(query, ipInfo, session, outputStream)
            }
        }
    }

    private fun sendTcpDnsResponse(
        dnsPayload: ByteArray,
        ipInfo: IpPacketInfo,
        session: TcpDnsSession,
        outputStream: FileOutputStream
    ) {
        try {
            val framed = ByteBuffer.allocate(2 + dnsPayload.size)
            framed.putShort(dnsPayload.size.toShort())
            framed.put(dnsPayload)

            val dataPacket = DnsPacketParser.buildTcpDataPacket(
                incoming = ipInfo,
                serverSeqNum = session.serverSeq,
                clientAckNum = session.clientSeq,
                payload = framed.array(),
                fin = true
            )
            synchronized(outputStream) {
                outputStream.write(dataPacket)
                outputStream.flush()
            }
        } catch (_: Exception) {}
    }

    private fun forwardUpstreamTcpDns(
        query: ParsedDnsQuery,
        ipInfo: IpPacketInfo,
        session: TcpDnsSession,
        outputStream: FileOutputStream
    ) {
        val cm = getSystemService(ConnectivityManager::class.java)
        upstreamResolver.resolve(
            query = query,
            config = currentDnsConfig,
            physicalNetwork = activePhysicalNetwork,
            connectivityManager = cm,
            onSuccess = { answer ->
                try {
                    val (minTtl, cnames) = DnsPacketParser.inspectResponse(answer)
                    var blockedByCname = false
                    for (cname in cnames) {
                        if (policyEngine.evaluate(cname) != PolicyDecision.ALLOW) {
                            blockedByCname = true
                            break
                        }
                    }
                    val finalPayload = if (blockedByCname) {
                        DnsPacketParser.buildNxDomainResponse(query)
                    } else {
                        dnsCache.put(query.question.qname, query.question.qtype, answer, minTtl)
                        answer
                    }
                    sendTcpDnsResponse(finalPayload, ipInfo, session, outputStream)
                } finally {
                    tcpSessions.remove(session.key)
                }
            },
            onError = {
                try {
                    val servFail = DnsPacketParser.buildServFailResponse(query.txId, query.question.rawQuestionBytes)
                    sendTcpDnsResponse(servFail, ipInfo, session, outputStream)
                } finally {
                    tcpSessions.remove(session.key)
                }
            }
        )
    }

    private fun pruneStaleTcpSessions() {
        if (tcpSessions.isEmpty()) return
        val now = System.currentTimeMillis()
        val it = tcpSessions.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (now - entry.value.createdAt > 10000L || tcpSessions.size > 64) {
                it.remove()
            }
        }
    }

    private fun registerPhysicalNetworkTracker() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activePhysicalNetwork = network
                dnsCache.clear() // Network changed, flush DNS cache
            }

            override fun onLost(network: Network) {
                if (activePhysicalNetwork == network) {
                    activePhysicalNetwork = null
                    dnsCache.clear()
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                    activePhysicalNetwork = network
                }
            }
        }
        cm.registerNetworkCallback(request, networkCallback!!)
    }

    private fun stopVpnInternal(isRevoked: Boolean = false) {
        _isVpnRunning.value = false

        packetLoopJob?.cancel()
        packetLoopJob = null

        vpnInterface?.let {
            try {
                it.close()
            } catch (_: Exception) {}
            vpnInterface = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        if (!isRevoked) {
            com.coconut.policy.LemonPolicyCoordinator.getInstance(this).onServiceStoppedNormally()
        }
    }

    override fun onRevoke() {
        super.onRevoke()
        com.coconut.policy.LemonPolicyCoordinator.getInstance(this).onServiceRevoked()
        stopVpnInternal(isRevoked = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnInternal()
        networkCallback?.let {
            val cm = getSystemService(ConnectivityManager::class.java)
            cm.unregisterNetworkCallback(it)
        }
        dnsExecutor.shutdown()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lemon Local DNS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active local DNS filtering status"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lemon Local DNS")
            .setContentText("Local domain filtering active on-device")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

