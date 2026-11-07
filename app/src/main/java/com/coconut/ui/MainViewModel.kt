package com.coconut.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coconut.data.CoconutRepository
import com.coconut.data.DnsConfigurationEntity
import com.coconut.data.DomainRuleEntity
import com.coconut.data.SnappedAppEntity
import com.coconut.data.StrictModeConfigEntity
import com.coconut.dns.CoconutVpnService
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
    val isAccessibilityGranted: Boolean = false,
    val isVpnRunning: Boolean = false,
    val isVpnPrepared: Boolean = false,
    val isStrictModeActive: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val snappedApps: List<SnappedAppEntity> = emptyList(),
    val domainRules: List<DomainRuleEntity> = emptyList(),
    val dnsConfig: DnsConfigurationEntity? = null,
    val strictModeConfig: StrictModeConfigEntity? = null,
    val searchQuery: String = "",
    val domainFilterQuery: String = "",
    val selectedRuleTab: String = "BLOCK", // "BLOCK" or "ALLOW"
    val isUpdatingBlocklist: Boolean = false,
    val blocklistStatus: String? = null,
    val bannerMessage: String? = null,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repo = CoconutRepository(application)
    private val pm = application.packageManager

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        observeData()
        refresh()
        loadInstalledApps()
    }

    private fun observeData() {
        viewModelScope.launch {
            repo.observeSnappedApps().collect { snapped ->
                _state.update { it.copy(snappedApps = snapped) }
            }
        }

        viewModelScope.launch {
            repo.observeDomainRules().collect { rules ->
                _state.update { it.copy(domainRules = rules) }
            }
        }

        viewModelScope.launch {
            repo.observeDnsConfig().collect { config ->
                _state.update { it.copy(dnsConfig = config) }
            }
        }

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

        viewModelScope.launch {
            SnapAccessibilityService.isRunning.collect { running ->
                _state.update { it.copy(isAccessibilityGranted = running || repo.isAccessibilityPermissionGranted()) }
            }
        }

        viewModelScope.launch {
            CoconutVpnService.isVpnRunning.collect { running ->
                _state.update { it.copy(isVpnRunning = running) }
            }
        }

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
        _state.update {
            it.copy(
                isAccessibilityGranted = repo.isAccessibilityPermissionGranted(),
                isVpnRunning = CoconutVpnService.isVpnRunning.value,
                isVpnPrepared = repo.isVpnPermissionPrepared(),
                isStrictModeActive = repo.strictModeManager.isStrictProtectionActive()
            )
        }
    }

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

    // --- 1. SNAP ACTIONS ---

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val launchIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val launchablePackages = pm.queryIntentActivities(launchIntent, 0).map { it.activityInfo.packageName }.toSet()

            val apps = packages
                .filter { it.packageName != getApplication<Application>().packageName && launchablePackages.contains(it.packageName) }
                .map { info ->
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    InstalledApp(
                        packageName = info.packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        category = if (isSystem) "System" else "User",
                        isSystemApp = isSystem
                    )
                }
                .sortedBy { it.label.lowercase() }

            _state.update { it.copy(installedApps = apps, isLoading = false) }
        }
    }

    fun toggleSnap(app: InstalledApp) {
        viewModelScope.launch {
            val isAlreadySnapped = _state.value.snappedApps.any { it.packageName == app.packageName }
            if (isAlreadySnapped) {
                repo.removeSnap(app.packageName)
                _state.update { it.copy(bannerMessage = "Unsnapped ${app.label}") }
            } else {
                val result = repo.snapApp(app.packageName, app.label, app.category)
                result.fold(
                    onSuccess = {
                        _state.update { it.copy(bannerMessage = "Snapped ${app.label}") }
                    },
                    onFailure = { error ->
                        _state.update { it.copy(errorMessage = error.message) }
                    }
                )
            }
        }
    }

    // --- 2. LOCAL DNS ACTIONS ---

    fun toggleDns(enable: Boolean) {
        viewModelScope.launch {
            repo.setDnsServiceEnabled(enable)
            refresh()
        }
    }

    fun addDomain(rawDomain: String, ruleType: String) {
        viewModelScope.launch {
            val result = repo.addDomainRule(rawDomain, ruleType)
            result.fold(
                onSuccess = { domain ->
                    _state.update { it.copy(bannerMessage = "Added $domain to $ruleType list") }
                },
                onFailure = { error ->
                    _state.update { it.copy(errorMessage = error.message) }
                }
            )
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

    fun deleteDomain(domain: String) {
        viewModelScope.launch {
            repo.deleteDomainRule(domain)
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

    // --- 3. STRICT MODE ACTIONS ---

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

    // --- 4. EXPORT & IMPORT ---

    suspend fun exportRules(): String = repo.exportRulesToPlainText()

    suspend fun importRules(content: String): Pair<Int, Int> = repo.importRulesFromPlainText(content)
}
