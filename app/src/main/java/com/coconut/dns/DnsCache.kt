package com.coconut.dns

import android.os.SystemClock

data class CachedDnsResponse(
    val domain: String,
    val rawPacket: ByteArray,
    val expiresAtRealtime: Long,
    val isNegative: Boolean
)

class DnsCache(private val maxEntries: Int = 1024) {

    private val cache = object : LinkedHashMap<String, CachedDnsResponse>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedDnsResponse>?): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    fun get(domain: String, queryType: Int, targetTxId: Int): ByteArray? {
        val key = "$domain:$queryType"
        val entry = cache[key] ?: return null
        val now = SystemClock.elapsedRealtime()

        if (now >= entry.expiresAtRealtime) {
            cache.remove(key)
            return null
        }

        // Return a defensive copy with the transaction ID rewritten to match the current query
        val copy = entry.rawPacket.copyOf()
        if (copy.size >= 2) {
            copy[0] = ((targetTxId shr 8) and 0xFF).toByte()
            copy[1] = (targetTxId and 0xFF).toByte()
        }
        return copy
    }

    @Synchronized
    fun put(domain: String, queryType: Int, responsePacket: ByteArray, ttlSeconds: Long, isNegative: Boolean = false) {
        val key = "$domain:$queryType"
        val clampedTtl = ttlSeconds.coerceIn(10L, 3600L)
        val expiresAt = SystemClock.elapsedRealtime() + (clampedTtl * 1000L)

        cache[key] = CachedDnsResponse(
            domain = domain,
            rawPacket = responsePacket.copyOf(),
            expiresAtRealtime = expiresAt,
            isNegative = isNegative
        )
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    @Synchronized
    fun size(): Int = cache.size
}

