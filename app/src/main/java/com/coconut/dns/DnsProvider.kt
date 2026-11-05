package com.coconut.dns

data class DnsProvider(
    val id: String,
    val displayName: String,
    val hostname: String,
    val description: String,
    val features: List<String>
)

object DnsProviders {
    val CLEAN_BROWSING = DnsProvider(
        id = "cleanbrowsing",
        displayName = "CleanBrowsing Family Filter",
        hostname = "family-filter-dns.cleanbrowsing.org",
        description = "Blocks adult-content domains, pornography, malicious sites, and enforces SafeSearch on major search engines.",
        features = listOf("Adult Content Filtering", "SafeSearch Enforcement", "Malware & Phishing Protection", "No Account Needed")
    )

    val CLOUDFLARE_FAMILY = DnsProvider(
        id = "cloudflare",
        displayName = "Cloudflare 1.1.1.1 for Families",
        hostname = "family.cloudflare-dns.com",
        description = "Global high-performance DNS that blocks malware and adult content across all networks.",
        features = listOf("Adult Content Filtering", "Malware Protection", "High Global Performance", "No Account Needed")
    )

    val ALL = listOf(CLEAN_BROWSING, CLOUDFLARE_FAMILY)

    fun fromHostname(hostname: String?): DnsProvider {
        return ALL.firstOrNull { it.hostname.equals(hostname, ignoreCase = true) } ?: CLEAN_BROWSING
    }
}

