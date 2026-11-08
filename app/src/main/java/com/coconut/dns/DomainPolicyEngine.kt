package com.coconut.dns

import java.net.IDN
import java.util.Locale

enum class PolicyDecision {
    ALLOW,
    BLOCK_THREAT,
    BLOCK_USER,
    BLOCK_CATEGORY
}

class DomainPolicyEngine {

    private val userAllowlist = HashSet<String>()
    private val userBlocklist = HashSet<String>()
    private val threatBlocklist = HashSet<String>()
    private val adultBlocklist = HashSet<String>()
    private val adsTrackersBlocklist = HashSet<String>()

    var blockThreatsEnabled: Boolean = true
    var blockAdultEnabled: Boolean = false
    var blockAdsTrackersEnabled: Boolean = false

    @Synchronized
    fun isThreatActive(): Boolean = blockThreatsEnabled && threatBlocklist.isNotEmpty()

    @Synchronized
    fun isAdultActive(): Boolean = blockAdultEnabled && adultBlocklist.isNotEmpty()

    @Synchronized
    fun isAdsTrackersActive(): Boolean = blockAdsTrackersEnabled && adsTrackersBlocklist.isNotEmpty()

    @Synchronized
    fun updateRules(
        allowlist: Collection<String>,
        blocklist: Collection<String>
    ) {
        userAllowlist.clear()
        userBlocklist.clear()
        for (domain in allowlist) {
            normalizeDomain(domain)?.let { userAllowlist.add(it) }
        }
        for (domain in blocklist) {
            normalizeDomain(domain)?.let { userBlocklist.add(it) }
        }
    }

    @Synchronized
    fun updateThreatList(threatDomains: Collection<String>) {
        threatBlocklist.clear()
        for (d in threatDomains) {
            normalizeDomain(d)?.let { threatBlocklist.add(it) }
        }
    }

    @Synchronized
    fun updateAdultList(adultDomains: Collection<String>) {
        adultBlocklist.clear()
        for (d in adultDomains) {
            normalizeDomain(d)?.let { adultBlocklist.add(it) }
        }
    }

    @Synchronized
    fun updateAdsTrackersList(adDomains: Collection<String>) {
        adsTrackersBlocklist.clear()
        for (d in adDomains) {
            normalizeDomain(d)?.let { adsTrackersBlocklist.add(it) }
        }
    }

    /**
     * Evaluates domain against policies with deterministic precedence:
     * 1. Built-in threat blocks (HaGeZi TIF Mini) - Security threats
     * 2. User allowlist - Explicit user overrides for false positives in category/adblock feeds
     * 3. Adult content blocklist (HaGeZi NSFW)
     * 4. User personal blocklist
     * 5. Optional category lists (Ads/Trackers - HaGeZi Multi PRO Mini)
     * 6. Allow and resolve
     */
    @Synchronized
    fun evaluate(rawDomain: String): PolicyDecision {
        val domain = normalizeDomain(rawDomain) ?: return PolicyDecision.ALLOW

        // Rule 1: Mandatory security threat blocks
        if (blockThreatsEnabled && matchesRule(domain, threatBlocklist)) {
            return PolicyDecision.BLOCK_THREAT
        }

        // Rule 2: User personal allowlist (takes precedence over adult and adblock categories)
        if (matchesRule(domain, userAllowlist)) {
            return PolicyDecision.ALLOW
        }

        // Rule 3: Adult content blocklist (HaGeZi NSFW)
        if (blockAdultEnabled && matchesRule(domain, adultBlocklist)) {
            return PolicyDecision.BLOCK_CATEGORY
        }

        // Rule 4: User personal blocklist
        if (matchesRule(domain, userBlocklist)) {
            return PolicyDecision.BLOCK_USER
        }

        // Rule 5: Optional Ads and Trackers category
        if (blockAdsTrackersEnabled && matchesRule(domain, adsTrackersBlocklist)) {
            return PolicyDecision.BLOCK_CATEGORY
        }

        return PolicyDecision.ALLOW
    }

    /**
     * Checks if candidate domain matches rule or is a subdomain of rule.
     * e.g., candidate "sub.example.com" matches rule "example.com",
     * but candidate "notexample.com" does NOT match "example.com".
     */
    private fun matchesRule(domain: String, ruleSet: Set<String>): Boolean {
        if (ruleSet.isEmpty()) return false
        if (ruleSet.contains(domain)) return true

        // Walk domain labels from right to left
        var index = domain.indexOf('.')
        while (index >= 0) {
            val parentDomain = domain.substring(index + 1)
            if (ruleSet.contains(parentDomain)) {
                return true
            }
            index = domain.indexOf('.', index + 1)
        }
        return false
    }

    companion object {
        /**
         * Normalizes domains: strips leading/trailing whitespace, converts to lowercase,
         * removes trailing dot, and converts internationalized domain names (IDN) to ASCII Punycode.
         */
        fun normalizeDomain(input: String): String? {
            val trimmed = input.trim().lowercase(Locale.ROOT)
            if (trimmed.isEmpty() || trimmed.contains('/') || trimmed.contains(':') || trimmed.contains('?')) return null

            val withoutTrailingDot = if (trimmed.endsWith('.')) {
                trimmed.dropLast(1)
            } else {
                trimmed
            }

            if (withoutTrailingDot.isEmpty() || withoutTrailingDot.length > 253) return null

            return try {
                val ascii = IDN.toASCII(withoutTrailingDot, IDN.ALLOW_UNASSIGNED)
                if (ascii.contains(' ') || !ascii.contains('.')) {
                    null
                } else {
                    ascii
                }
            } catch (_: Exception) {
                null
            }
        }

        fun isValidDomain(domain: String): Boolean {
            if (domain.length < 3 || domain.length > 253) return false
            if (!domain.contains('.') || domain.startsWith('.') || domain.endsWith('.')) return false
            if (domain.contains('/') || domain.contains(':') || domain.contains(' ') || domain.contains('?')) return false

            val labels = domain.split('.')
            if (labels.size < 2) return false
            for (label in labels) {
                if (label.isEmpty() || label.length > 63) return false
                if (label.startsWith('-') || label.endsWith('-')) return false
                for (c in label) {
                    if (!c.isLetterOrDigit() && c != '-' && c != '_') return false
                }
            }
            return true
        }
    }
}
