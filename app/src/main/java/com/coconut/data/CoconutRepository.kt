package com.coconut.data

import android.content.Context
import com.coconut.core.SafePackageHelper
import com.coconut.dns.DnsProviders
import com.coconut.policy.CoconutPolicyManager
import com.coconut.policy.PolicyReconciliationManager
import com.coconut.policy.PolicyResult
import com.coconut.policy.ReconciliationSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CoconutRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)
    private val snapDao = database.snappedAppDao()
    private val configDao = database.protectionConfigDao()
    private val stateDao = database.devicePolicyStateDao()
    private val auditDao = database.securityAuditDao()

    val policyManager = CoconutPolicyManager(appContext)
    val safePackageHelper = SafePackageHelper(appContext)
    val reconciliationManager = PolicyReconciliationManager(appContext)

    fun observeSnappedApps(): Flow<List<SnappedAppEntity>> = snapDao.observeAll()
    fun observeProtectionConfig(): Flow<ProtectionConfigurationEntity?> = configDao.observeConfig()
    fun observeDevicePolicyState(): Flow<DevicePolicyStateEntity?> = stateDao.observeState()
    fun observeAuditEvents(limit: Int = 50): Flow<List<SecurityAuditEventEntity>> = auditDao.observeRecentEvents(limit)

    suspend fun snapApp(
        packageName: String,
        applicationName: String,
        category: String
    ): PolicyResult = withContext(Dispatchers.IO) {
        if (safePackageHelper.isProtectedPackage(packageName)) {
            val reason = safePackageHelper.getProtectionReason(packageName) ?: "System-protected package."
            return@withContext PolicyResult.Failure("Cannot snap this application: $reason")
        }

        val isDO = policyManager.isDeviceOwner()
        val enforcement = if (isDO) {
            policyManager.suspendPackage(packageName)
        } else {
            PolicyResult.Success
        }

        val actualState = if (isDO && enforcement is PolicyResult.Success) {
            SnapState.PROTECTED
        } else {
            SnapState.RECONCILIATION_REQUIRED
        }

        // Persist in Room Snap Registry
        val now = System.currentTimeMillis()
        val existing = snapDao.getByPackageName(packageName)
        val entity = existing?.copy(
            applicationName = applicationName,
            category = category,
            desiredState = "SUSPENDED",
            actualState = actualState,
            installationState = InstallationState.INSTALLED,
            lastAppliedAt = if (isDO) now else 0L,
            lastError = if (isDO) null else "Device Owner required for OS suspension enforcement.",
            updatedAt = now
        ) ?: SnappedAppEntity(
            packageName = packageName,
            applicationName = applicationName,
            category = category,
            desiredState = "SUSPENDED",
            actualState = actualState,
            installationState = InstallationState.INSTALLED,
            firstSnappedAt = now,
            lastAppliedAt = if (isDO) now else 0L,
            lastError = if (isDO) null else "Device Owner required for OS suspension enforcement.",
            createdAt = now,
            updatedAt = now
        )

        snapDao.insert(entity)
        auditDao.insert(
            SecurityAuditEventEntity(
                eventType = if (isDO) "SNAP_ENFORCED" else "SNAP_QUEUED",
                target = packageName,
                details = if (isDO) {
                    "Successfully suspended and registered in permanent Snap Registry: $applicationName ($packageName)"
                } else {
                    "Registered in Snap Registry (pending Device Owner provisioning): $applicationName ($packageName)"
                },
                severity = "INFO"
            )
        )

        PolicyResult.Success
    }

    suspend fun unsnapApp(packageName: String): PolicyResult = withContext(Dispatchers.IO) {
        if (!policyManager.isDeviceOwner()) {
            return@withContext PolicyResult.Failure("Device Owner privileges are required to Unsnap applications.")
        }

        // Call native unsuspension
        val restoration = policyManager.unsuspendPackage(packageName)
        if (restoration !is PolicyResult.Success) {
            auditDao.insert(
                SecurityAuditEventEntity(
                    eventType = "UNSNAP_FAILED",
                    target = packageName,
                    details = "Unsuspension failed: ${(restoration as PolicyResult.Failure).reason}",
                    severity = "ERROR"
                )
            )
            return@withContext restoration
        }

        // Remove from Snap Registry only AFTER verified unsuspension
        snapDao.deleteByPackageName(packageName)
        auditDao.insert(
            SecurityAuditEventEntity(
                eventType = "UNSNAP_ENFORCED",
                target = packageName,
                details = "Successfully restored and removed from Snap Registry: $packageName",
                severity = "INFO"
            )
        )

        PolicyResult.Success
    }

    suspend fun setContentShield(enabled: Boolean, providerHostname: String? = null): PolicyResult = withContext(Dispatchers.IO) {
        val currentConfig = configDao.getConfig() ?: ProtectionConfigurationEntity()
        val hostname = providerHostname ?: currentConfig.dnsHostname
        val provider = DnsProviders.fromHostname(hostname)

        val isDO = policyManager.isDeviceOwner()
        val policyResult = if (isDO) {
            if (enabled) {
                policyManager.enableContentShield(hostname)
            } else {
                policyManager.disableContentShield()
            }
        } else {
            PolicyResult.Success
        }

        configDao.insertOrUpdate(
            currentConfig.copy(
                contentShieldEnabled = enabled,
                dnsProviderName = provider.displayName,
                dnsHostname = hostname,
                updatedAt = System.currentTimeMillis()
            )
        )
        auditDao.insert(
            SecurityAuditEventEntity(
                eventType = if (enabled) "CONTENT_SHIELD_ENABLED" else "CONTENT_SHIELD_DISABLED",
                target = hostname,
                details = "Content Shield ${if (enabled) "enabled" else "disabled"}. Provider: ${provider.displayName} ($hostname)",
                severity = "INFO"
            )
        )

        policyResult
    }

    suspend fun setAdvancedRestriction(restriction: String, enabled: Boolean): PolicyResult = withContext(Dispatchers.IO) {
        val isDO = policyManager.isDeviceOwner()
        val result = if (isDO) {
            policyManager.setUserRestriction(restriction, enabled)
        } else {
            PolicyResult.Success
        }

        val currentConfig = configDao.getConfig() ?: ProtectionConfigurationEntity()
        val updated = when (restriction) {
            android.os.UserManager.DISALLOW_CONFIG_PRIVATE_DNS -> currentConfig.copy(disallowConfigPrivateDns = enabled)
            android.os.UserManager.DISALLOW_CONFIG_VPN -> currentConfig.copy(disallowConfigVpn = enabled)
            android.os.UserManager.DISALLOW_DEBUGGING_FEATURES -> currentConfig.copy(disallowDebugging = enabled)
            android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY -> currentConfig.copy(disallowUnknownSources = enabled)
            android.os.UserManager.DISALLOW_SAFE_BOOT -> currentConfig.copy(disallowSafeBoot = enabled)
            android.os.UserManager.DISALLOW_FACTORY_RESET -> currentConfig.copy(disallowFactoryReset = enabled)
            android.os.UserManager.DISALLOW_APPS_CONTROL -> currentConfig.copy(disallowAppsControl = enabled)
            else -> currentConfig
        }
        configDao.insertOrUpdate(updated.copy(updatedAt = System.currentTimeMillis()))
        auditDao.insert(
            SecurityAuditEventEntity(
                eventType = "HARDENING_RESTRICTION_CHANGED",
                target = restriction,
                details = "Restriction $restriction set to $enabled.",
                severity = "INFO"
            )
        )
        result
    }

    suspend fun setSelfProtection(enabled: Boolean): PolicyResult = withContext(Dispatchers.IO) {
        val isDO = policyManager.isDeviceOwner()
        val result = if (isDO) {
            policyManager.setSelfProtection(enabled)
        } else {
            PolicyResult.Success
        }

        val currentConfig = configDao.getConfig() ?: ProtectionConfigurationEntity()
        configDao.insertOrUpdate(currentConfig.copy(selfProtectionEnabled = enabled, updatedAt = System.currentTimeMillis()))
        auditDao.insert(
            SecurityAuditEventEntity(
                eventType = "SELF_PROTECTION_CHANGED",
                target = "Coconut",
                details = "Self protection set to $enabled.",
                severity = "INFO"
            )
        )
        result
    }

    suspend fun reconcile(reason: String = "MANUAL"): ReconciliationSummary {
        return reconciliationManager.reconcile(reason)
    }

    fun isDeviceOwner(): Boolean = policyManager.isDeviceOwner()
}

