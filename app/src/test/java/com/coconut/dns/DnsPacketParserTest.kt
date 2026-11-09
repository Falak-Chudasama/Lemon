package com.coconut.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsPacketParserTest {

    @Test
    fun testBuildAndParseDnsQuery() {
        val domain = "google.com"
        val payload = DnsPacketParser.buildTestDnsQuery(domain)

        assertNotNull(payload)
        // Standard header (12 bytes) + 1 (len) + 6 (google) + 1 (len) + 3 (com) + 1 (0) + 2 (qtype) + 2 (qclass) = 28 bytes
        assertEquals(28, payload.size)

        val parsed = DnsPacketParser.parseDnsQuery(payload, 0, payload.size)
        assertNotNull("Query payload must parse successfully", parsed)
        assertEquals(0x1234, parsed!!.txId)
        assertEquals(0x0100, parsed.flags)
        assertEquals(domain, parsed.question.qname)
        assertEquals(1, parsed.question.qtype) // A record
        assertEquals(1, parsed.question.qclass) // IN class
    }

    @Test
    fun testBuildAndParseMultiSubdomain() {
        val domain = "api.internal.lemon.app"
        val payload = DnsPacketParser.buildTestDnsQuery(domain)

        val parsed = DnsPacketParser.parseDnsQuery(payload, 0, payload.size)
        assertNotNull(parsed)
        assertEquals(domain, parsed!!.question.qname)
    }

    @Test
    fun testParseTruncatedPayloadReturnsNull() {
        val truncated = ByteArray(10) // Less than minimum 12 bytes DNS header
        val parsed = DnsPacketParser.parseDnsQuery(truncated, 0, truncated.size)
        assertNull("Truncated payload under 12 bytes must return null", parsed)
    }
}

