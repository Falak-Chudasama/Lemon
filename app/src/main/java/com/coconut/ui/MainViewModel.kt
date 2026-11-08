package com.coconut.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coconut.data.AppCategoryOverrideEntity
import com.coconut.data.CoconutRepository
import com.coconut.data.DnsConfigurationEntity
import com.coconut.data.DnsScheduleEntity
import com.coconut.data.DomainChangeRequestEntity
import com.coconut.data.DomainRuleEntity
import com.coconut.data.SnappedAppEntity
import com.coconut.data.StrictModeConfigEntity
import com.coconut.dns.CoconutVpnService
import com.coconut.security.AppLockManager
import com.coconut.security.BiometricHelper
import com.coconut.service.SnapAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavigationTab {
    SNAP,
    DNS,
    SETTINGS
}

data class InstalledApp(
    val packageName: String,
    val label: String,
    val category: String,
    val isSystemApp: Boolean
)

data class MainUiState(
    val selectedTab: NavigationTab = NavigationTab.SNAP,
    val isLoading: Boolean = false,
    val isSessionUnlocked: Boolean = false,
    val isAppLockEnabled: Boolean = true,
    val isAccessibilityGranted: Boolean = false,
    val isVpnRunning: Boolean = false,
    val isVpnPrepared: Boolean = false,
    val isStrictModeActive: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val snappedApps: List<SnappedAppEntity> = emptyList(),
    val domainRules: List<DomainRuleEntity> = emptyList(),
    val pendingDomainRequests: List<DomainChangeRequestEntity> = emptyList(),
    val dnsSchedule: DnsScheduleEntity? = null,
    val isInsideNightSchedule: Boolean = false,
    val dnsConfig: DnsConfigurationEntity? = null,
    val strictModeConfig: StrictModeConfigEntity? = null,
    val categoryOverrides: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val domainFilterQuery: String = "",
    val selectedRuleTab: String = "BLOCK", // "BLOCK" or "ALLOW"
    val isUpdatingBlocklist: Boolean = false,
    val blocklistStatus: String? = null,
    // Two-step Snap confirmation state
    val pendingSnapApp: InstalledApp? = null,
    val showSnapTypedSheet: Boolean = false,
    val typedSnapText: String = "",
    val bannerMessage: String? = null,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repo = CoconutRepository(application)
    val themePreferences = ThemePreferences(application)
    private val pm = application.packageManager

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        observeData()
        refresh()
        loadInstalledApps()
    }

    private fun observeData() {
        // App Lock session state
        viewModelScope.launch {
            AppLockManager.isSessionUnlocked.collect { unlocked ->
                _state.update {
                    it.copy(
                        isSessionUnlocked = unlocked,
                        isAppLockEnabled = repo.appLockManager.isAppLockEnabled
                    )
                }
            }
        }

        // Snapped apps
        viewModelScope.launch {
            repo.observeSnappedApps().collect { snapped ->
                _state.update { it.copy(snappedApps = snapped) }
            }
        }

        // Domain rules
        viewModelScope.launch {
            repo.observeDomainRules().collect { rules ->
                _state.update { it.copy(domainRules = rules) }
            }
        }

        // 30-Day pending requests
        viewModelScope.launch {
            repo.observePendingChangeRequests().collect { pending ->
                _state.update { it.copy(pendingDomainRequests = pending) }
            }
        }

        // Nightly DNS Schedule
        viewModelScope.launch {
            repo.observeDnsSchedule().collect { schedule ->
                val inside = schedule != null && schedule.enabled &&
                        repo.scheduleManager.isInsideScheduleWindow(
                            startHour = schedule.startHour,
                            startMinute = schedule.startMinute,
                            endHour = schedule.endHour,
                            endMinute = schedule.endMinute
                        )
                _state.update {
                    it.copy(
                        dnsSchedule = schedule,
                        isInsideNightSchedule = inside
                    )
                }
            }
        }

        // DNS Config
        viewModelScope.launch {
            repo.observeDnsConfig().collect { config ->
                _state.update { it.copy(dnsConfig = config) }
            }
        }

        // Category Overrides
        viewModelScope.launch {
            repo.observeCategoryOverrides().collect { overrides ->
                val map = overrides.associate { it.packageName to it.category }
                _state.update { it.copy(categoryOverrides = map) }
                // Re-categorize installed apps when overrides change
                refreshAppCategories(map)
            }
        }

        // Strict Mode
        viewModelScope.launch {
            repo.observeStrictModeConfig().collect { strict ->
                _state.update {
                    it.copy(
                        strictModeConfig = strict,
                        isStrictModeActive = repo.strictModeManager.isStrictProtectionActive()
                    )
                }
            }
        }

        // Accessibility Service
        viewModelScope.launch {
            SnapAccessibilityService.isRunning.collect { running ->
                _state.update { it.copy(isAccessibilityGranted = running || repo.isAccessibilityPermissionGranted()) }
            }
        }

        // VPN Service
        viewModelScope.launch {
            CoconutVpnService.isVpnRunning.collect { running ->
                _state.update { it.copy(isVpnRunning = running) }
            }
        }

        // Blocklist updates
        viewModelScope.launch {
            repo.blocklistManager.isUpdating.collect { updating ->
                _state.update { it.copy(isUpdatingBlocklist = updating) }
            }
        }

        viewModelScope.launch {
            repo.blocklistManager.lastUpdateStatus.collect { status ->
                _state.update { it.copy(blocklistStatus = status) }
            }
        }
    }

    fun refresh() {
        val schedule = _state.value.dnsSchedule
        val inside = schedule != null && schedule.enabled &&
                repo.scheduleManager.isInsideScheduleWindow(
                    startHour = schedule.startHour,
                    startMinute = schedule.startMinute,
                    endHour = schedule.endHour,
                    endMinute = schedule.endMinute
                )

        _state.update {
            it.copy(
                isAccessibilityGranted = repo.isAccessibilityPermissionGranted(),
                isVpnRunning = CoconutVpnService.isVpnRunning.value,
                isVpnPrepared = repo.isVpnPermissionPrepared(),
                isStrictModeActive = repo.strictModeManager.isStrictProtectionActive(),
                isInsideNightSchedule = inside,
                isAppLockEnabled = repo.appLockManager.isAppLockEnabled
            )
        }

        // Trigger schedule reconciliation and mature request checks
        viewModelScope.launch {
            repo.scheduleManager.reconcileSchedule()
        }
    }

    // --- APP LOCK ACTIONS ---

    fun unlockSession() {
        AppLockManager.unlockSession()
    }

    fun lockCoconut() {
        AppLockManager.lockSession()
    }

    fun toggleAppLockEnabled(enabled: Boolean) {
        repo.appLockManager.isAppLockEnabled = enabled
        _state.update { it.copy(isAppLockEnabled = enabled) }
    }

    // --- NAVIGATION & UI STATE ---

    fun selectTab(tab: NavigationTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setDomainFilterQuery(query: String) {
        _state.update { it.copy(domainFilterQuery = query) }
    }

    fun setSelectedRuleTab(tab: String) {
        _state.update { it.copy(selectedRuleTab = tab) }
    }

    fun clearBanner() {
        _state.update { it.copy(bannerMessage = null, errorMessage = null) }
    }

    // --- INSTALLED APPS & CATEGORIES ---

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val launchIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val launchablePackages = pm.queryIntentActivities(launchIntent, 0).map { it.activityInfo.packageName }.toSet()
            val overrides = _state.value.categoryOverrides

            val apps = packages
                .filter { it.packageName != getApplication<Application>().packageName && launchablePackages.contains(it.packageName) }
                .map { info ->
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val category = repo.safePackageHelper.resolveCategory(info, overrides[info.packageName])
                    InstalledApp(
                        packageName = info.packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        category = category,
                        isSystemApp = isSystem
                    )
                }
                .sortedBy { it.label.lowercase() }

            _state.update { it.copy(installedApps = apps, isLoading = false) }
        }
    }

    private fun refreshAppCategories(overrides: Map<String, String>) {
        val updated = _state.value.installedApps.map { app ->
            val manual = overrides[app.packageName]
            if (manual != null) app.copy(category = manual) else app
        }
        _state.update { it.copy(installedApps = updated) }
    }

    fun setAppCategoryOverride(packageName: String, category: String) {
        viewModelScope.launch {
            repo.setCategoryOverride(packageName, category)
        }
    }

    // --- SNAP TWO-STEP CONFIRMATION WORKFLOW ---

    /**
     * Step 1: Trigger Strong Biometric prompt before creating a Snap.
     */
    fun startSnapProcess(activity: FragmentActivity, app: InstalledApp) {
        if (repo.safePackageHelper.isProtectedPackage(app.packageName)) {
            val reason = repo.safePackageHelper.getProtectionReason(app.packageName) ?: "System-protected package."
            _state.update { it.copy(errorMessage = reason) }
            return
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = "Snap ${app.label}",
            subtitle = "Biometric required to confirm restriction",
            allowDeviceCredential = false, // Strong biometric required for Snap
            onSuccess = {
                // Step 2: Open exact typed confirmation sheet
                _state.update {
                    it.copy(
                        pendingSnapApp = app,
                        showSnapTypedSheet = true,
                        typedSnapText = ""
                    )
                }
            },
            onError = { error ->
                _state.update { it.copy(errorMessage = "Biometric failed: $error") }
            }
        )
    }

    fun updateTypedSnapText(text: String) {
        _state.update { it.copy(typedSnapText = text) }
    }

    /**
     * Step 2: Commits the Snap only when exact case-sensitive "SNAP" is submitted.
     */
    fun commitSnap() {
        val app = _state.value.pendingSnapApp ?: return
        if (_state.value.typedSnapText != "SNAP") {
            _state.update { it.copy(errorMessage = "Enter exact uppercase SNAP to confirm") }
            return
        }

        viewModelScope.launch {
            val result = repo.snapApp(app.packageName, app.label, app.category)
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            pendingSnapApp = null,
                            showSnapTypedSheet = false,
                            typedSnapText = "",
                            bannerMessage = "Snapped ${app.label}"
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            pendingSnapApp = null,
                            showSnapTypedSheet = false,
                            typedSnapText = "",
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun cancelSnapConfirmation() {
        _state.update {
            it.copy(
                pendingSnapApp = null,
                showSnapTypedSheet = false,
                typedSnapText = ""
            )
        }
    }

    // --- DNS & 30-DAY QUEUE ACTIONS ---

    fun toggleDns(enable: Boolean) {
        if (!enable && _state.value.isInsideNightSchedule) {
            _state.update { it.copy(errorMessage = "DNS locked: Night schedule is active (23:00 - 06:00)") }
            return
        }
        viewModelScope.launch {
            repo.setDnsServiceEnabled(enable)
            refresh()
        }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.scheduleManager.setScheduleEnabled(enabled)
            refresh()
        }
    }

    /**
     * Add domain to blocklist: requires biometric, applies immediately (strengthens filtering).
     */
    fun requestAddBlocklistDomain(activity: FragmentActivity, rawDomain: String) {
        val normalized = com.coconut.dns.DomainPolicyEngine.normalizeDomain(rawDomain)
        if (normalized == null || !com.coconut.dns.DomainPolicyEngine.isValidDomain(normalized)) {
            _state.update { it.copy(errorMessage = "Invalid domain format") }
            return
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = "Block $normalized",
            subtitle = "Biometric required to add domain",
            allowDeviceCredential = true,
            onSuccess = {
                viewModelScope.launch {
                    val result = repo.addBlocklistDomain(normalized)
                    result.fold(
                        onSuccess = { domain ->
                            _state.update { it.copy(bannerMessage = "Blocked $domain") }
                        },
                        onFailure = { error ->
                            _state.update { it.copy(errorMessage = error.message) }
                        }
                    )
                }
            },
            onError = { error ->
                _state.update { it.copy(errorMessage = "Authentication failed: $error") }
            }
        )
    }

    /**
     * Remove domain from blocklist: requires biometric, enforces exact 30-day queue (weakens filtering).
     */
    fun requestRemoveBlocklistDomain(activity: FragmentActivity, domain: String) {
        BiometricHelper.authenticate(
            activity = activity,
            title = "Remove $domain",
            subtitle = "Biometric required: 30-day delay applies",
            allowDeviceCredential = true,
            onSuccess = {
                viewModelScope.launch {
                    val result = repo.queueRemoveBlocklistDomain(domain)
                    result.fold(
                        onSuccess = {
                            _state.update { it.copy(bannerMessage = "Removal queued: 30 days remaining") }
                        },
                        onFailure = { error ->
                            _state.update { it.copy(errorMessage = error.message) }
                        }
                    )
                }
            },
            onError = { error ->
                _state.update { it.copy(errorMessage = "Authentication failed: $error") }
            }
        )
    }

    /**
     * Add domain to allowlist: requires biometric, enforces exact 30-day queue (weakens filtering).
     */
    fun requestAddAllowlistDomain(activity: FragmentActivity, rawDomain: String) {
        val normalized = com.coconut.dns.DomainPolicyEngine.normalizeDomain(rawDomain)
        if (normalized == null || !com.coconut.dns.DomainPolicyEngine.isValidDomain(normalized)) {
            _state.update { it.copy(errorMessage = "Invalid domain format") }
            return
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = "Allow $normalized",
            subtitle = "Biometric required: 30-day delay applies",
            allowDeviceCredential = true,
            onSuccess = {
                viewModelScope.launch {
                    val result = repo.queueAddAllowlistDomain(normalized)
                    result.fold(
                        onSuccess = {
                            _state.update { it.copy(bannerMessage = "Allow queued: 30 days remaining") }
                        },
                        onFailure = { error ->
                            _state.update { it.copy(errorMessage = error.message) }
                        }
                    )
                }
            },
            onError = { error ->
                _state.update { it.copy(errorMessage = "Authentication failed: $error") }
            }
        )
    }

    /**
     * Remove domain from allowlist: requires biometric, applies immediately (strengthens filtering).
     */
    fun requestRemoveAllowlistDomain(activity: FragmentActivity, domain: String) {
        BiometricHelper.authenticate(
            activity = activity,
            title = "Remove allowed $domain",
            subtitle = "Biometric required",
            allowDeviceCredential = true,
            onSuccess = {
                viewModelScope.launch {
                    repo.removeAllowlistDomain(domain)
                    _state.update { it.copy(bannerMessage = "Removed $domain from allowlist") }
                }
            },
            onError = { error ->
                _state.update { it.copy(errorMessage = "Authentication failed: $error") }
            }
        )
    }

    /**
     * Cancel pending domain request (preserves stronger filtering).
     */
    fun cancelPendingRequest(id: Long) {
        viewModelScope.launch {
            repo.cancelDomainChangeRequest(id)
            _state.update { it.copy(bannerMessage = "Request cancelled") }
        }
    }

    fun addBulkDomains(text: String, ruleType: String) {
        viewModelScope.launch {
            val (added, invalid) = repo.addBulkDomains(text, ruleType)
            _state.update {
                it.copy(
                    bannerMessage = "Added $added domains" + (if (invalid > 0) " ($invalid invalid skipped)" else "")
                )
            }
        }
    }

    fun toggleDomainRule(rule: DomainRuleEntity) {
        viewModelScope.launch {
            repo.toggleDomainRule(rule)
        }
    }

    fun clearDomainRules(type: String) {
        viewModelScope.launch {
            repo.clearRules(type)
            _state.update { it.copy(bannerMessage = "Cleared all $type rules") }
        }
    }

    fun updateDnsConfiguration(
        blockThreats: Boolean? = null,
        blockAdultContent: Boolean? = null,
        blockAds: Boolean? = null,
        autoUpdate: Boolean? = null,
        wifiOnly: Boolean? = null
    ) {
        viewModelScope.launch {
            repo.updateDnsConfiguration(blockThreats, blockAdultContent, blockAds, autoUpdate, wifiOnly)
        }
    }

    fun triggerBlocklistUpdate() {
        viewModelScope.launch {
            val wifiOnly = _state.value.dnsConfig?.wifiOnly ?: true
            repo.blocklistManager.updateBlocklist(wifiOnly = wifiOnly)
        }
    }

    // --- STRICT MODE ACTIONS ---

    fun setStrictMode(enabled: Boolean, passcode: String? = null) {
        viewModelScope.launch {
            repo.setStrictModeEnabled(enabled, passcode)
            _state.update {
                it.copy(
                    isStrictModeActive = repo.strictModeManager.isStrictProtectionActive(),
                    bannerMessage = if (enabled) "Strict Mode enabled" else "Strict Mode disabled"
                )
            }
        }
    }

    fun startUnlockCountdown() {
        viewModelScope.launch {
            repo.startUnlockCountdown()
        }
    }

    fun completeUnlock() {
        viewModelScope.launch {
            val unlocked = repo.completeUnlockAfterCountdown()
            if (unlocked) {
                _state.update {
                    it.copy(
                        isStrictModeActive = false,
                        bannerMessage = "Temporary 15-minute Settings window active"
                    )
                }
            } else {
                _state.update { it.copy(errorMessage = "Unlock countdown still active") }
            }
        }
    }

    fun cancelUnlock() {
        viewModelScope.launch {
            repo.cancelUnlockCountdown()
        }
    }

    // --- EXPORT & IMPORT ---

    suspend fun exportRules(): String = repo.exportRulesToPlainText()

    suspend fun importRules(content: String): Pair<Int, Int> = repo.importRulesFromPlainText(content)
}
