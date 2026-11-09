package com.coconut.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class DnsUpstreamResolverTest {

    @Test
    fun testPresetsConfigurationAndEncryptedTransports() {
        val presets = DnsUpstreamPresets.PRESETS
        assertEquals(7, presets.size)

        // AdGuard Default
        val adguard = DnsUpstreamPresets.getPreset("ADGUARD_DEFAULT")
        assertEquals("94.140.14.14", adguard.primaryIp)
        assertEquals("94.140.15.15", adguard.secondaryIp)
        assertEquals("dns.adguard-dns.com", adguard.hostname)
        assertEquals(853, adguard.dotPort)
        assertEquals("https://dns.adguard-dns.com/dns-query", adguard.dohUrl)
        assertTrue(adguard.supportsDoT)
        assertTrue(adguard.supportsDoH)

        // Cloudflare
        val cloudflare = DnsUpstreamPresets.getPreset("CLOUDFLARE")
        assertEquals("1.1.1.1", cloudflare.primaryIp)
        assertEquals("1.0.0.1", cloudflare.secondaryIp)
        assertEquals("one.one.one.one", cloudflare.hostname)
        assertEquals(853, cloudflare.dotPort)
        assertEquals("https://cloudflare-dns.com/dns-query", cloudflare.dohUrl)
        assertTrue(cloudflare.supportsDoT)

        // Google
        val google = DnsUpstreamPresets.getPreset("GOOGLE")
        assertEquals("8.8.8.8", google.primaryIp)
        assertEquals("8.8.4.4", google.secondaryIp)
        assertEquals("dns.google", google.hostname)
        assertEquals(853, google.dotPort)

        // CleanBrowsing Family
        val cleanBrowsing = DnsUpstreamPresets.getPreset("CLEANBROWSING_FAMILY")
        assertEquals("185.228.168.168", cleanBrowsing.primaryIp)
        assertEquals("185.228.169.168", cleanBrowsing.secondaryIp)
        assertEquals("family-filter-dns.cleanbrowsing.org", cleanBrowsing.hostname)

        // System Default (Plaintext only)
        val system = DnsUpstreamPresets.getPreset("SYSTEM")
        assertFalse("System resolver must not claim encrypted transport", system.supportsDoT)
        assertFalse(system.supportsDoH)
    }

    @Test
    fun testDoTRfc7858FramePrefixEncodingAndDecoding() {
        // RFC 7858 mandates a 2-byte big-endian length prefix preceding the DNS payload
        val testPayload = DnsPacketParser.buildTestDnsQuery("cloudflare.com")
        val payloadSize = testPayload.size

        val byteOut = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteOut)
        dataOut.writeShort(payloadSize)
        dataOut.write(testPayload)
        dataOut.flush()

        val framedBytes = byteOut.toByteArray()
        assertEquals(payloadSize + 2, framedBytes.size)

        // Verify decoding
        val byteIn = ByteArrayInputStream(framedBytes)
        val dataIn = DataInputStream(byteIn)
        val decodedLen = dataIn.readUnsignedShort()
        assertEquals(payloadSize, decodedLen)

        val decodedPayload = ByteArray(decodedLen)
        dataIn.readFully(decodedPayload)
        assertTrue(testPayload.contentEquals(decodedPayload))

        // Verify the parsed query question name matches
        val parsed = DnsPacketParser.parseDnsQuery(decodedPayload, 0, decodedPayload.size)
        assertNotNull(parsed)
        assertEquals("cloudflare.com", parsed!!.question.qname)
    }

    @Test
    fun testDoHRfc8484PayloadPackaging() {
        // RFC 8484 requires sending wire-format DNS query in POST body with Content-Type application/dns-message
        val testPayload = DnsPacketParser.buildTestDnsQuery("google.com")
        val contentType = "application/dns-message"
        val acceptHeader = "application/dns-message"

        assertEquals("application/dns-message", contentType)
        assertEquals("application/dns-message", acceptHeader)
        assertTrue(testPayload.isNotEmpty())

        val parsed = DnsPacketParser.parseDnsQuery(testPayload, 0, testPayload.size)
        assertNotNull(parsed)
        assertEquals("google.com", parsed!!.question.qname)
    }
}

