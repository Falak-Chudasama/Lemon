package com.coconut.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.net.VpnService
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.coconut.core.SafePackageHelper
import com.coconut.dns.BlocklistManager
import com.coconut.dns.CoconutVpnService
import com.coconut.dns.DomainPolicyEngine
import com.coconut.security.StrictModeManager
import com.coconut.service.SnapAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CoconutRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val snapDao = db.snappedAppDao()
    private val domainRuleDao = db.domainRuleDao()
    private val dnsConfigDao = db.dnsConfigurationDao()
    private val blocklistSourceDao = db.blocklistSourceDao()
    private val strictModeDao = db.strictModeConfigDao()

    val safePackageHelper = SafePackageHelper(appContext)
    val blocklistManager = BlocklistManager(appContext)
    val strictModeManager = StrictModeManager(appContext)

    // --- 1. THE SNAP (ACCESSIBILITY FOREGROUND RESTRICTION) ---

    fun observeSnappedApps(): Flow<List<SnappedAppEntity>> = snapDao.observeAll()

    suspend fun snapApp(
        packageName: String,
        applicationName: String,
        category: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (safePackageHelper.isProtectedPackage(packageName)) {
            val reason = safePackageHelper.getProtectionReason(packageName) ?: "System-protected package."
            return@withContext Result.failure(IllegalArgumentException("Cannot snap package: $reason"))
        }

        val now = System.currentTimeMillis()
        val existing = snapDao.getByPackageName(packageName)
        val entity = existing?.copy(
            applicationName = applicationName,
            category = category,
            desiredState = "SNAPPED",
            actualState = SnapState.SNAPPED,
            installationState = InstallationState.INSTALLED,
            updatedAt = now
        ) ?: SnappedAppEntity(
            packageName = packageName,
            applicationName = applicationName,
            category = category,
            desiredState = "SNAPPED",
            actualState = SnapState.SNAPPED,
            installationState = InstallationState.INSTALLED,
            firstSnappedAt = now,
            lastAppliedAt = now,
            createdAt = now,
            updatedAt = now
        )

        snapDao.insert(entity)
        Result.success(Unit)
    }

    suspend fun removeSnap(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        snapDao.deleteByPackageName(packageName)
        Result.success(Unit)
    }

    fun isAccessibilityPermissionGranted(): Boolean {
        if (SnapAccessibilityService.isRunning.value) return true

        val am = appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val myServiceClass = SnapAccessibilityService::class.java.name
        return enabledServices.any { it.resolveInfo.serviceInfo.name == myServiceClass }
    }

    // --- 2. COCONUT LOCAL DNS (VPN SERVICE & DOMAIN POLICY) ---

    fun observeDomainRules(): Flow<List<DomainRuleEntity>> = domainRuleDao.getAllRulesFlow()
    fun observeBlockedRules(): Flow<List<DomainRuleEntity>> = domainRuleDao.getRulesByTypeFlow("BLOCK")
    fun observeAllowedRules(): Flow<List<DomainRuleEntity>> = domainRuleDao.getRulesByTypeFlow("ALLOW")
    fun observeDnsConfig(): Flow<DnsConfigurationEntity?> = dnsConfigDao.getConfigurationFlow()
    fun observeBlocklistSources(): Flow<List<BlocklistSourceEntity>> = blocklistSourceDao.getAllSourcesFlow()

    suspend fun addDomainRule(rawDomain: String, ruleType: String): Result<String> = withContext(Dispatchers.IO) {
        val domain = DomainPolicyEngine.normalizeDomain(rawDomain)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid domain name: enter a hostname like example.com"))

        if (!DomainPolicyEngine.isValidDomain(domain)) {
            return@withContext Result.failure(IllegalArgumentException("Malformed domain: enter a valid hostname without http:// or path"))
        }

        val now = System.currentTimeMillis()
        val rule = DomainRuleEntity(
            domain = domain,
            ruleType = ruleType,
            isEnabled = true,
            source = "USER",
            createdAt = now,
            updatedAt = now
        )
        domainRuleDao.insertOrUpdate(rule)
        Result.success(domain)
    }

    suspend fun addBulkDomains(rawText: String, ruleType: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val lines = rawText.lines()
        val toInsert = mutableListOf<DomainRuleEntity>()
        var invalidCount = 0
        val now = System.currentTimeMillis()

        for (line in lines) {
            val domain = DomainPolicyEngine.normalizeDomain(line)
            if (domain != null && DomainPolicyEngine.isValidDomain(domain)) {
                toInsert.add(
                    DomainRuleEntity(
                        domain = domain,
                        ruleType = ruleType,
                        isEnabled = true,
                        source = "USER",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else if (line.isNotBlank()) {
                invalidCount++
            }
        }

        if (toInsert.isNotEmpty()) {
            domainRuleDao.insertAll(toInsert)
        }
        Pair(toInsert.size, invalidCount)
    }

    suspend fun deleteDomainRule(domain: String) = withContext(Dispatchers.IO) {
        domainRuleDao.deleteByDomain(domain)
    }

    suspend fun clearRules(ruleType: String) = withContext(Dispatchers.IO) {
        domainRuleDao.clearByType(ruleType)
    }

    suspend fun toggleDomainRule(rule: DomainRuleEntity) = withContext(Dispatchers.IO) {
        domainRuleDao.update(rule.copy(isEnabled = !rule.isEnabled, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateDnsConfiguration(
        blockThreats: Boolean? = null,
        blockAdultContent: Boolean? = null,
        blockAdsTrackers: Boolean? = null,
        autoUpdate: Boolean? = null,
        wifiOnly: Boolean? = null
    ) = withContext(Dispatchers.IO) {
        val current = dnsConfigDao.getConfiguration() ?: DnsConfigurationEntity()
        dnsConfigDao.insertOrUpdate(
            current.copy(
                blockThreats = blockThreats ?: current.blockThreats,
                blockAdultContent = blockAdultContent ?: current.blockAdultContent,
                blockAdsTrackers = blockAdsTrackers ?: current.blockAdsTrackers,
                autoUpdate = autoUpdate ?: current.autoUpdate,
                wifiOnly = wifiOnly ?: current.wifiOnly
            )
        )
    }

    suspend fun setDnsServiceEnabled(enabled: Boolean) {
        if (enabled) {
            CoconutVpnService.start(appContext)
        } else {
            CoconutVpnService.stop(appContext)
        }
    }

    fun isVpnPermissionPrepared(): Boolean {
        return VpnService.prepare(appContext) == null
    }

    // --- 3. STRICT MODE ---

    fun observeStrictModeConfig(): Flow<StrictModeConfigEntity?> = strictModeDao.getConfigFlow()

    suspend fun setStrictModeEnabled(enabled: Boolean, passcode: String? = null): Boolean {
        return strictModeManager.setStrictModeEnabled(enabled, passcode)
    }

    suspend fun verifyPasscode(input: String): Boolean {
        return strictModeManager.verifyPasscode(input)
    }

    suspend fun startUnlockCountdown(): Long {
        return strictModeManager.startUnlockCountdown()
    }

    suspend fun completeUnlockAfterCountdown(): Boolean {
        return strictModeManager.completeUnlockAfterCountdown()
    }

    suspend fun cancelUnlockCountdown() {
        strictModeManager.cancelUnlockCountdown()
    }

    // --- 4. EXPORT & IMPORT LOCAL RULES ---

    suspend fun exportRulesToPlainText(): String = withContext(Dispatchers.IO) {
        val rules = domainRuleDao.getActiveRules()
        val sb = StringBuilder()
        sb.append("# Coconut Custom Domain Rules Export\n")
        sb.append("# Generated: ${System.currentTimeMillis()}\n\n")

        val blocked = rules.filter { it.ruleType == "BLOCK" }
        sb.append("# Blocked Domains (${blocked.size})\n")
        for (r in blocked) {
            sb.append("BLOCK: ${r.domain}\n")
        }

        val allowed = rules.filter { it.ruleType == "ALLOW" }
        sb.append("\n# Allowed Domains (${allowed.size})\n")
        for (r in allowed) {
            sb.append("ALLOW: ${r.domain}\n")
        }

        sb.toString()
    }

    suspend fun importRulesFromPlainText(content: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val lines = content.lines()
        var imported = 0
        var invalid = 0
        val now = System.currentTimeMillis()
        val toInsert = mutableListOf<DomainRuleEntity>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue

            val type: String
            val rawDomain: String

            if (trimmed.startsWith("BLOCK:", ignoreCase = true)) {
                type = "BLOCK"
                rawDomain = trimmed.substring(6).trim()
            } else if (trimmed.startsWith("ALLOW:", ignoreCase = true)) {
                type = "ALLOW"
                rawDomain = trimmed.substring(6).trim()
            } else {
                type = "BLOCK"
                rawDomain = trimmed
            }

            val domain = DomainPolicyEngine.normalizeDomain(rawDomain)
            if (domain != null && DomainPolicyEngine.isValidDomain(domain)) {
                toInsert.add(
                    DomainRuleEntity(
                        domain = domain,
                        ruleType = type,
                        isEnabled = true,
                        source = "IMPORT",
                        createdAt = now,
                        updatedAt = now
                    )
                )
                imported++
            } else {
                invalid++
            }
        }

        if (toInsert.isNotEmpty()) {
            domainRuleDao.insertAll(toInsert)
        }
        Pair(imported, invalid)
    }
}
