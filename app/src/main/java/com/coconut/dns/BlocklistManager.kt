package com.coconut.dns

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.coconut.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class BlocklistManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val blocklistDir = File(appContext.filesDir, "blocklists")

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    private val _lastUpdateStatus = MutableStateFlow<String?>(null)
    val lastUpdateStatus = _lastUpdateStatus.asStateFlow()

    init {
        if (!blocklistDir.exists()) {
            blocklistDir.mkdirs()
        }
    }

    /**
     * Loads the threat blocklist (HaGeZi TIF Mini): uses local updated file if present,
     * otherwise falls back to bundled asset snapshot.
     */
    suspend fun loadThreatDomains(): Set<String> = withContext(Dispatchers.IO) {
        loadListFromFileOrAsset("threats.txt", "threat_blocklist.txt")
    }

    /**
     * Loads the adult content blocklist (HaGeZi NSFW): uses local updated file if present,
     * otherwise falls back to bundled asset snapshot.
     */
    suspend fun loadAdultDomains(): Set<String> = withContext(Dispatchers.IO) {
        loadListFromFileOrAsset("adult.txt", "adult_blocklist.txt")
    }

    /**
     * Loads the ads and trackers blocklist (HaGeZi Multi PRO Mini): uses local updated file if present,
     * otherwise falls back to bundled asset snapshot.
     */
    suspend fun loadAdsTrackersDomains(): Set<String> = withContext(Dispatchers.IO) {
        loadListFromFileOrAsset("adblock.txt", "adblock_blocklist.txt")
    }

    private fun loadListFromFileOrAsset(localFilename: String, assetFilename: String): Set<String> {
        val localFile = File(blocklistDir, localFilename)
        if (localFile.exists() && localFile.length() > 0) {
            try {
                val domains = parseDomainList(localFile.readLines())
                if (domains.isNotEmpty()) return domains
            } catch (_: Exception) {}
        }

        // Fallback to bundled asset
        return try {
            appContext.assets.open(assetFilename).use { input ->
                val lines = BufferedReader(InputStreamReader(input)).readLines()
                parseDomainList(lines)
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Atomically downloads and verifies blocklist updates over HTTPS.
     */
    suspend fun updateBlocklist(wifiOnly: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        if (_isUpdating.value) return@withContext false
        _isUpdating.value = true
        _lastUpdateStatus.value = "Checking network..."

        try {
            // Check Wi-Fi constraint
            val cm = appContext.getSystemService(ConnectivityManager::class.java)
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (wifiOnly && !isWifi) {
                _lastUpdateStatus.value = "Skipped: Wi-Fi required"
                return@withContext false
            }

            val feeds = listOf(
                Triple("hagezi_threat_mini", "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/tif.mini.txt", "threats.txt"),
                Triple("hagezi_nsfw", "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/nsfw.txt", "adult.txt"),
                Triple("hagezi_pro_mini", "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/pro.mini.txt", "adblock.txt")
            )

            var totalUpdatedRules = 0
            val now = System.currentTimeMillis()

            for ((sourceId, sourceUrl, filename) in feeds) {
                _lastUpdateStatus.value = "Downloading $filename..."
                val tempFile = File(blocklistDir, "${filename}.tmp")
                val targetFile = File(blocklistDir, filename)

                val downloaded = downloadFile(sourceUrl, tempFile)
                if (!downloaded) continue

                val lines = tempFile.readLines()
                val parsedDomains = parseDomainList(lines)

                if (parsedDomains.size < 100) {
                    tempFile.delete()
                    continue
                }

                // Atomic rename
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                totalUpdatedRules += parsedDomains.size

                val sourceDao = db.blocklistSourceDao()
                val source = sourceDao.getSourceById(sourceId)
                if (source != null) {
                    sourceDao.update(source.copy(lastUpdated = now, ruleCount = parsedDomains.size))
                }
            }

            val dnsDao = db.dnsConfigurationDao()
            val currentConfig = dnsDao.getConfiguration()
            if (currentConfig != null) {
                dnsDao.update(currentConfig.copy(lastUpdateTimestamp = now))
            }

            _lastUpdateStatus.value = "Updated: $totalUpdatedRules rules active across feeds"
            true
        } catch (e: Exception) {
            _lastUpdateStatus.value = "Update failed: ${e.message}"
            false
        } finally {
            _isUpdating.value = false
        }
    }

    private fun downloadFile(urlStr: String, targetFile: File): Boolean {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Coconut-Android/2.0")

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return false
            }

            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            conn.disconnect()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun parseDomainList(lines: List<String>): Set<String> {
        val result = HashSet<String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue

            val domainPart = when {
                // Adblock Plus format: ||example.com^
                line.startsWith("||") && line.endsWith("^") -> line.substring(2, line.length - 1)
                // Wildcard format: *.example.com
                line.startsWith("*.") -> line.substring(2)
                // Hosts format: 0.0.0.0 example.com or 127.0.0.1 example.com
                line.startsWith("0.0.0.0 ") || line.startsWith("127.0.0.1 ") -> line.substringAfter(' ').trim()
                else -> line
            }

            DomainPolicyEngine.normalizeDomain(domainPart)?.let {
                result.add(it)
            }
        }
        return result
    }
}
