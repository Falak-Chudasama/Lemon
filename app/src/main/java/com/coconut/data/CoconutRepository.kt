package com.coconut.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.net.VpnService
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.coconut.core.SafePackageHelper
import com.coconut.dns.BlocklistManager
import com.coconut.dns.CoconutVpnService
import com.coconut.dns.DnsScheduleManager
import com.coconut.dns.DomainPolicyEngine
import com.coconut.dns.DomainQueueManager
import com.coconut.security.AppLockManager
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
    private val categoryOverrideDao = db.appCategoryOverrideDao()

    val safePackageHelper = SafePackageHelper(appContext)
    val blocklistManager = BlocklistManager(appContext)
    val strictModeManager = StrictModeManager(appContext)
    val queueManager = DomainQueueManager(appContext)
    val scheduleManager = DnsScheduleManager(appContext)
    val appLockManager = AppLockManager(appContext)

    // --- 1. THE SNAP (ACCESSIBILITY FOREGROUND RESTRICTION) ---

    fun observeSnappedApps(): Flow<List<SnappedAppEntity>> = snapDao.observeAll()

    fun observeCategoryOverrides(): Flow<List<AppCategoryOverrideEntity>> = categoryOverrideDao.getAllOverridesFlow()

    suspend fun setCategoryOverride(packageName: String, category: String) = withContext(Dispatchers.IO) {
        categoryOverrideDao.insertOrUpdate(
            AppCategoryOverrideEntity(
                packageName = packageName,
                category = category,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

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

        val settingValue = android.provider.Settings.Secure.getString(
            appContext.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val myServiceClass = SnapAccessibilityService::class.java.name
        if (settingValue.contains(myServiceClass) || settingValue.contains("com.coconut")) {
            return true
        }

        val am = appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.name == myServiceClass }
    }

    // --- 2. COCONUT LOCAL DNS (VPN SERVICE & DOMAIN POLICY) ---

    fun observeDomainRules(): Flow<List<DomainRuleEntity>> = domainRuleDao.getAllRulesFlow()
    fun observeBlockedRules(): Flow<List<DomainRuleEntity>> = domainRuleDao.getRulesByTypeFlow("BLOCK")
    fun observeAllowedRules(): Flow<List<DomainRuleEntity>> = domainRuleDao.getRulesByTypeFlow("ALLOW")
    fun observeDnsConfig(): Flow<DnsConfigurationEntity?> = dnsConfigDao.getConfigurationFlow()
    fun observeBlocklistSources(): Flow<List<BlocklistSourceEntity>> = blocklistSourceDao.getAllSourcesFlow()
    fun observePendingChangeRequests(): Flow<List<DomainChangeRequestEntity>> = queueManager.pendingRequestsFlow
    fun observeDnsSchedule(): Flow<DnsScheduleEntity?> = scheduleManager.scheduleFlow

    /**
     * Adds domain to Blocklist immediately (strengthens filtering).
     * Cancels any pending removal request, removes conflicting allow rule, and writes rule atomically.
     */
    suspend fun addBlocklistDomain(rawDomain: String): Result<String> = withContext(Dispatchers.IO) {
        val domain = DomainPolicyEngine.normalizeDomain(rawDomain)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid domain name: enter a hostname like example.com"))

        if (!DomainPolicyEngine.isValidDomain(domain)) {
            return@withContext Result.failure(IllegalArgumentException("Malformed domain: enter a valid hostname without http:// or path"))
        }

        // Cancel any pending removal request for this domain
        db.domainChangeRequestDao().cancelPendingForDomain(domain)

        val now = System.currentTimeMillis()
        val rule = DomainRuleEntity(
            domain = domain,
            ruleType = "BLOCK",
            isEnabled = true,
            source = "USER",
            createdAt = now,
            updatedAt = now
        )
        // Primary key replacement guarantees mutual exclusivity against previous ALLOW
        domainRuleDao.insertOrUpdate(rule)
        Result.success(domain)
    }

    /**
     * Queues domain removal from Blocklist (weakens filtering).
     * Enforces an exact 30-day (720 hours) waiting period while keeping the block active.
     */
    suspend fun queueRemoveBlocklistDomain(domain: String): Result<DomainChangeRequestEntity> {
        return queueManager.queueRemoveBlock(domain)
    }

    /**
     * Queues domain addition to Allowlist (weakens filtering).
     * Enforces an exact 30-day (720 hours) waiting period while keeping the domain unallowed.
     */
    suspend fun queueAddAllowlistDomain(rawDomain: String): Result<DomainChangeRequestEntity> {
        return queueManager.queueAddAllow(rawDomain)
    }

    /**
     * Removes domain from Allowlist immediately (strengthens filtering).
     */
    suspend fun removeAllowlistDomain(domain: String) = withContext(Dispatchers.IO) {
        val normalized = DomainPolicyEngine.normalizeDomain(domain) ?: domain
        domainRuleDao.deleteByDomain(normalized)
    }

    /**
     * Cancels any pending domain change request without weakening protection.
     */
    suspend fun cancelDomainChangeRequest(requestId: Long): Boolean {
        return queueManager.cancelRequest(requestId)
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
        sb.append("# Lemon Custom Domain Rules Export\n")
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
