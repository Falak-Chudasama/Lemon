package com.coconut.dns

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

sealed interface DnsTestResult {
    data class Success(val latencyMs: Long, val resolvedIp: String) : DnsTestResult
    data class Failure(val error: String, val recoveryAdvice: String) : DnsTestResult
}

class DnsConnectivityTester {
    suspend fun testProvider(hostname: String): DnsTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            // Step 1: Resolve provider hostname to IP
            val addresses = InetAddress.getAllByName(hostname)
            if (addresses.isEmpty()) {
                return@withContext DnsTestResult.Failure(
                    error = "Could not resolve IP addresses for $hostname.",
                    recoveryAdvice = "Check your device's internet connection. If DNS is broken, disable Content Shield temporarily."
                )
            }
            val primaryIp = addresses.first().hostAddress ?: addresses.first().toString()

            // Step 2: Test TCP connection to DNS-over-TLS port 853
            Socket().use { socket ->
                socket.connect(InetSocketAddress(primaryIp, 853), 4000)
            }

            val latency = System.currentTimeMillis() - startTime
            DnsTestResult.Success(latencyMs = latency, resolvedIp = primaryIp)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            DnsTestResult.Failure(
                error = "DoT handshake failed (${e.javaClass.simpleName}: ${e.message ?: "timeout"}).",
                recoveryAdvice = "Your current network or carrier may be blocking TCP port 853 (DNS-over-TLS). Try switching to mobile data or another Wi-Fi network."
            )
        }
    }
}

