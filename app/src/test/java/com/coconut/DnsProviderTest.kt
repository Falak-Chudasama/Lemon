package com.coconut

import com.coconut.dns.DnsProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsProviderTest {

    @Test
    fun testDefaultCleanBrowsingProvider() {
        val cleanBrowsing = DnsProviders.CLEAN_BROWSING
        assertEquals("family-filter-dns.cleanbrowsing.org", cleanBrowsing.hostname)
        assertEquals("CleanBrowsing Family Filter", cleanBrowsing.displayName)
        assertTrue(cleanBrowsing.features.contains("Adult Content Filtering"))
        assertTrue(cleanBrowsing.features.contains("SafeSearch Enforcement"))
    }

    @Test
    fun testCloudflareFamilyProvider() {
        val cloudflare = DnsProviders.CLOUDFLARE_FAMILY
        assertEquals("family.cloudflare-dns.com", cloudflare.hostname)
        assertEquals("Cloudflare 1.1.1.1 for Families", cloudflare.displayName)
        assertTrue(cloudflare.features.contains("Adult Content Filtering"))
        assertTrue(cloudflare.features.contains("Malware Protection"))
    }

    @Test
    fun testFromHostnameResolution() {
        val resolvedCb = DnsProviders.fromHostname("family-filter-dns.cleanbrowsing.org")
        assertEquals(DnsProviders.CLEAN_BROWSING, resolvedCb)

        val resolvedCf = DnsProviders.fromHostname("family.cloudflare-dns.com")
        assertEquals(DnsProviders.CLOUDFLARE_FAMILY, resolvedCf)

        // Case insensitivity check
        val resolvedCase = DnsProviders.fromHostname("FAMILY-FILTER-DNS.CLEANBROWSING.ORG")
        assertEquals(DnsProviders.CLEAN_BROWSING, resolvedCase)

        // Fallback for null or unknown host
        val fallback = DnsProviders.fromHostname(null)
        assertEquals(DnsProviders.CLEAN_BROWSING, fallback)
    }
}

