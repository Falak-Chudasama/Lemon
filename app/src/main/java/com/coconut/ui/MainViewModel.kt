package com.coconut.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coconut.data.CoconutRepository
import com.coconut.data.DevicePolicyStateEntity
import com.coconut.data.ProtectionConfigurationEntity
import com.coconut.data.SecurityAuditEventEntity
import com.coconut.data.SnappedAppEntity
import com.coconut.dns.DnsConnectivityTester
import com.coconut.dns.DnsTestResult
import com.coconut.policy.PolicyResult
import com.coconut.policy.PrivateDnsStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val selectedTab: NavigationTab = NavigationTab.HOME,
    val isLoading: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isDeviceAdmin: Boolean = false,
    val showSetupDialog: Boolean = false,
    val isSelfProtectionActive: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val snappedApps: List<SnappedAppEntity> = emptyList(),
    val protectionConfig: ProtectionConfigurationEntity? = null,
    val devicePolicyState: DevicePolicyStateEntity? = null,
    val auditEvents: List<SecurityAuditEventEntity> = emptyList(),
    val privateDnsStatus: PrivateDnsStatus? = null,
    val dnsTestResult: DnsTestResult? = null,
    val isTestingDns: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val includeSystemApps: Boolean = false,
    val sortOrder: AppSortOrder = AppSortOrder.NAME_ASC,
    val showRegistryTab: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val activeSnapTarget: InstalledApp? = null,
    val activeBlockedTarget: InstalledApp? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = CoconutRepository(application)
    private val pm = application.packageManager
    private val dnsTester = DnsConnectivityTester()

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        observeData()
        refresh()
    }

    private fun observeData() {
        viewModelScope.launch {
            repo.observeSnappedApps().collect { snapped ->
                _state.update { it.copy(snappedApps = snapped) }
            }
        }

        viewModelScope.launch {
            repo.observeProtectionConfig().collect { config ->
                _state.update { it.copy(protectionConfig = config) }
            }
        }

        viewModelScope.launch {
            repo.observeDevicePolicyState().collect { policyState ->
                _state.update { it.copy(devicePolicyState = policyState) }
            }
        }

        viewModelScope.launch {
            repo.observeAuditEvents().collect { events ->
                _state.update { it.copy(auditEvents = events) }
            }
        }
    }

    fun selectTab(tab: NavigationTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setSelectedCategory(category: String) {
        _state.update { it.copy(selectedCategory = category) }
    }

    fun setIncludeSystemApps(include: Boolean) {
        _state.update { it.copy(includeSystemApps = include) }
        loadInstalledApps()
    }

    fun setSortOrder(order: AppSortOrder) {
        _state.update { it.copy(sortOrder = order) }
    }

    fun setShowRegistryTab(show: Boolean) {
        _state.update { it.copy(showRegistryTab = show) }
    }

    fun dismissSetupDialog() {
        _state.update { it.copy(showSetupDialog = false) }
    }

    fun openSetupDialog() {
        _state.update { it.copy(showSetupDialog = true) }
    }

    fun refresh() {
        viewModelScope.launch {
            val isDO = repo.isDeviceOwner()
            val isAdmin = repo.policyManager.isDeviceAdmin()
            val selfProtection = repo.policyManager.isSelfProtectionActive()
            val dnsStatus = repo.policyManager.getPrivateDnsStatus()

            _state.update {
                it.copy(
                    isDeviceOwner = isDO,
                    isDeviceAdmin = isAdmin,
                    showSetupDialog = !isDO,
                    isSelfProtectionActive = selfProtection,
                    privateDnsStatus = dnsStatus
                )
            }
            loadInstalledApps()
        }
    }

    fun reconcileNow() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val summary = repo.reconcile("USER_REQUEST")
            val dnsStatus = repo.policyManager.getPrivateDnsStatus()
            val selfProtection = repo.policyManager.isSelfProtectionActive()

            _state.update {
                it.copy(
                    isLoading = false,
                    isDeviceOwner = summary.isDeviceOwner,
                    isDeviceAdmin = repo.policyManager.isDeviceAdmin(),
                    isSelfProtectionActive = selfProtection,
                    privateDnsStatus = dnsStatus,
                    successMessage = if (summary.success) {
                        "Reconciliation complete: ${summary.totalSnapped} snapped apps checked. Policies verified."
                    } else null,
                    errorMessage = if (!summary.success) {
                        summary.errors.firstOrNull() ?: "Reconciliation found policy issues."
                    } else null
                )
            }
            loadInstalledApps()
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launchableApps = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                .asSequence()
                .map { it.activityInfo.applicationInfo }
                .distinctBy { it.packageName }
                .toList()

            val ownPackage = getApplication<Application>().packageName
            val includeSys = _state.value.includeSystemApps

            val apps = launchableApps
                .filter { info -> info.packageName != ownPackage }
                .filter { info ->
                    if (includeSys) true
                    else (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .map { info ->
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isProtected = repo.safePackageHelper.isProtectedPackage(info.packageName)
                    val protectionReason = repo.safePackageHelper.getProtectionReason(info.packageName)
                    val isSuspended = repo.policyManager.isPackageSuspended(info.packageName)

                    InstalledApp(
                        packageName = info.packageName,
                        applicationName = info.loadLabel(pm).toString(),
                        category = classify(info),
                        isSystemApp = isSystem,
                        isProtected = isProtected,
                        protectionReason = protectionReason,
                        isSuspended = isSuspended,
                        icon = null // Loaded lazily and cached via AppIconCache
                    )
                }

            withContext(Dispatchers.Main) {
                _state.update { it.copy(installedApps = apps) }
            }
        }
    }

    private fun classify(info: ApplicationInfo): String {
        return when {
            info.category == ApplicationInfo.CATEGORY_GAME -> "Games"
            info.category == ApplicationInfo.CATEGORY_AUDIO -> "Entertainment"
            info.category == ApplicationInfo.CATEGORY_VIDEO -> "Entertainment"
            info.category == ApplicationInfo.CATEGORY_IMAGE -> "Entertainment"
            info.category == ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            info.category == ApplicationInfo.CATEGORY_NEWS -> "Social"
            info.category == ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
            info.category == ApplicationInfo.CATEGORY_MAPS -> "Utilities"
            else -> {
                val pkg = info.packageName.lowercase()
                when {
                    listOf("instagram", "facebook", "reddit", "twitter", "x.com", "snapchat", "discord", "linkedin", "threads", "tiktok").any { it in pkg } -> "Social"
                    listOf("youtube", "netflix", "spotify", "primevideo", "disney", "twitch", "vlc", "music").any { it in pkg } -> "Entertainment"
                    listOf("docs", "sheets", "office", "word", "excel", "drive", "todo", "slack", "notion").any { it in pkg } -> "Productivity"
                    listOf("whatsapp", "telegram", "messenger", "signal", "messages", "mail", "gmail", "meet", "zoom").any { it in pkg } -> "Communication"
                    listOf("camera", "gallery", "clock", "calculator", "chrome", "browser", "settings", "files").any { it in pkg } -> "Utilities"
                    else -> "Other"
                }
            }
        }
    }

    // --- SNAP ACTIONS ---

    fun requestSnap(app: InstalledApp) {
        _state.update { it.copy(activeSnapTarget = app, errorMessage = null) }
    }

    fun dismissSnapRequest() {
        _state.update { it.copy(activeSnapTarget = null) }
    }

    fun executeSnap(app: InstalledApp) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, activeSnapTarget = null, errorMessage = null) }
            val result = repo.snapApp(app.packageName, app.applicationName, app.category)
            when (result) {
                is PolicyResult.Success -> {
                    val msg = if (_state.value.isDeviceOwner) {
                        "Application ${app.applicationName} has been permanently Snapped."
                    } else {
                        "Application ${app.applicationName} Snapped in registry! Device Owner required for OS-level suspension."
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            successMessage = msg
                        )
                    }
                    refresh()
                }
                is PolicyResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    fun showBlockedDialog(app: InstalledApp) {
        _state.update { it.copy(activeBlockedTarget = app) }
    }

    fun dismissBlockedDialog() {
        _state.update { it.copy(activeBlockedTarget = null) }
    }

    fun toggleSelfProtection(enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repo.setSelfProtection(enabled)
            when (result) {
                is PolicyResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSelfProtectionActive = enabled,
                            successMessage = "Self-protection ${if (enabled) "enabled" else "disabled"}."
                        )
                    }
                }
                is PolicyResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    // --- CONTENT SHIELD ACTIONS ---

    fun toggleContentShield(enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repo.setContentShield(enabled)
            when (result) {
                is PolicyResult.Success -> {
                    val dnsStatus = repo.policyManager.getPrivateDnsStatus()
                    val msg = if (_state.value.isDeviceOwner) {
                        "Content Shield ${if (enabled) "enabled" else "disabled"} successfully."
                    } else {
                        "Content Shield preference saved. Activate Device Owner to lock DNS."
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            privateDnsStatus = dnsStatus,
                            successMessage = msg
                        )
                    }
                }
                is PolicyResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    fun selectDnsProvider(hostname: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val currentlyEnabled = _state.value.protectionConfig?.contentShieldEnabled ?: false
            val result = repo.setContentShield(enabled = currentlyEnabled, providerHostname = hostname)
            when (result) {
                is PolicyResult.Success -> {
                    val dnsStatus = repo.policyManager.getPrivateDnsStatus()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            privateDnsStatus = dnsStatus,
                            successMessage = "DNS Provider set to $hostname"
                        )
                    }
                }
                is PolicyResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    fun testDnsConnectivity() {
        val hostname = _state.value.protectionConfig?.dnsHostname ?: "family-filter-dns.cleanbrowsing.org"
        viewModelScope.launch {
            _state.update { it.copy(isTestingDns = true, dnsTestResult = null) }
            val result = dnsTester.testProvider(hostname)
            _state.update { it.copy(isTestingDns = false, dnsTestResult = result) }
        }
    }

    // --- ADVANCED HARDENING ---

    fun setHardeningRestriction(restriction: String, enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repo.setAdvancedRestriction(restriction, enabled)
            when (result) {
                is PolicyResult.Success -> {
                    val dnsStatus = repo.policyManager.getPrivateDnsStatus()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            privateDnsStatus = dnsStatus,
                            successMessage = "Device policy updated."
                        )
                    }
                }
                is PolicyResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    fun showErrorMessage(msg: String) {
        _state.update { it.copy(errorMessage = msg) }
    }

    fun showSuccessMessage(msg: String) {
        _state.update { it.copy(successMessage = msg) }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
