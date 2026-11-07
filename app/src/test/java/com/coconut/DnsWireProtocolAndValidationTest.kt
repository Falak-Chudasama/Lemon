package com.coconut

import com.coconut.dns.DnsConnectivityTester
import com.coconut.dns.DnsTestResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class DnsWireProtocolAndValidationTest {

    private val tester = DnsConnectivityTester()

    @Test
    fun testInvalidHostnameRejectedImmediately() = runBlocking {
        val resultBlank = tester.testProvider("")
        assertTrue(resultBlank is DnsTestResult.Failure)
        assertEquals("VALIDATION", (resultBlank as DnsTestResult.Failure).stage)

        val resultNoDot = tester.testProvider("invalidhostname")
        assertTrue(resultNoDot is DnsTestResult.Failure)
        assertEquals("VALIDATION", (resultNoDot as DnsTestResult.Failure).stage)
    }

    @Test
    fun testDnsWireResponseParsing() {
        // Construct a synthetic RFC 1035 DNS response payload
        // ID: 0x1234, Flags: 0x8180 (Response, Recursion Available, No Error)
        // QDCOUNT: 1, ANCOUNT: 1, NSCOUNT: 0, ARCOUNT: 0
        // Question: "test.com", QTYPE: 1 (A), QCLASS: 1 (IN)
        // Answer: pointer to 0x000C, TYPE: 1, CLASS: 1, TTL: 300, RDLENGTH: 4, IP: 1.2.3.4
        val buffer = ByteBuffer.allocate(64)
        buffer.putShort(0x1234.toShort()) // ID
        buffer.putShort(0x8180.toShort()) // Flags: Response, No error
        buffer.putShort(1.toShort()) // QDCOUNT
        buffer.putShort(1.toShort()) // ANCOUNT
        buffer.putShort(0.toShort()) // NSCOUNT
        buffer.putShort(0.toShort()) // ARCOUNT

        // QNAME: 4 "test" 3 "com" 0
        buffer.put(4.toByte())
        buffer.put("test".toByteArray(Charsets.US_ASCII))
        buffer.put(3.toByte())
        buffer.put("com".toByteArray(Charsets.US_ASCII))
        buffer.put(0.toByte())
        buffer.putShort(1.toShort()) // QTYPE A
        buffer.putShort(1.toShort()) // QCLASS IN

        // Answer:
        buffer.putShort(0xC00C.toShort()) // Pointer to offset 12 (0x0C)
        buffer.putShort(1.toShort()) // TYPE A
        buffer.putShort(1.toShort()) // CLASS IN
        buffer.putInt(300) // TTL
        buffer.putShort(4.toShort()) // RDLENGTH = 4 bytes
        buffer.put(1.toByte())
        buffer.put(2.toByte())
        buffer.put(3.toByte())
        buffer.put(4.toByte()) // 1.2.3.4

        val payload = buffer.array().copyOf(buffer.position())

        // Use reflection to test internal parser
        val parseMethod = DnsConnectivityTester::class.java.getDeclaredMethod(
            "parseDnsWireResponse",
            ByteArray::class.java
        )
        parseMethod.isAccessible = true
        val parsed = parseMethod.invoke(tester, payload) as com.coconut.dns.ParsedDnsResponse

        assertEquals(0x1234, parsed.queryId)
        assertEquals(0, parsed.rcode)
        assertEquals(1, parsed.answerCount)
        assertEquals(1, parsed.resolvedIps.size)
        assertEquals("1.2.3.4", parsed.resolvedIps.first())
    }
}

