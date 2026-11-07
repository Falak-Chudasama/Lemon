package com.coconut.data

import android.content.Context
import com.coconut.core.SafePackageHelper
import com.coconut.dns.DnsProviders
import com.coconut.policy.CoconutPolicyManager
import com.coconut.policy.DeviceCapabilityManager
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
    val capabilityManager = DeviceCapabilityManager(appContext)
    val safePackageHelper = SafePackageHelper(appContext)
    val reconciliationManager = PolicyReconciliationManager(appContext)

    fun observeSnappedApps(): Flow<List<SnappedAppEntity>> = snapDao.observeAll()
    fun observeProtectionConfig(): Flow<ProtectionConfigurationEntity?> = configDao.observeConfig()
    fun observeDevicePolicyState(): Flow<DevicePolicyStateEntity?> = stateDao.observeState()
    fun observeAuditEvents(limit: Int = 50): Flow<List<SecurityAuditEventEntity>> = auditDao.observeRecentEvents(limit)

    // --- 1. THE SNAP (PERMANENT OS SUSPENSION & UNINSTALL BLOCKING) ---

    suspend fun snapApp(
        packageName: String,
        applicationName: String,
        category: String
    ): PolicyResult = withContext(Dispatchers.IO) {
        if (safePackageHelper.isProtectedPackage(packageName)) {
            val reason = safePackageHelper.getProtectionReason(packageName) ?: "System-protected package."
            return@withContext PolicyResult.Unsupported("Cannot snap this application: $reason")
        }

        val now = System.currentTimeMillis()
        val isDO = policyManager.isDeviceOwner()

        if (!isDO) {
            // Section 2.1: Never present a pending registry entry as an enforced Snap!
            val existing = snapDao.getByPackageName(packageName)
            val pendingEntity = existing?.copy(
                applicationName = applicationName,
                category = category,
                desiredState = "SUSPENDED",
                actualState = SnapState.PENDING_ENFORCEMENT,
                uninstallBlocked = false,
                installationState = InstallationState.INSTALLED,
                lastAppliedAt = 0L,
                lastVerifiedAt = 0L,
                lastError = "Awaiting Device Owner provisioning for OS-level suspension enforcement.",
                updatedAt = now
            ) ?: SnappedAppEntity(
                packageName = packageName,
                applicationName = applicationName,
                category = category,
                desiredState = "SUSPENDED",
                actualState = SnapState.PENDING_ENFORCEMENT,
                uninstallBlocked = false,
                installationState = InstallationState.INSTALLED,
                firstSnappedAt = now,
                lastAppliedAt = 0L,
                lastVerifiedAt = 0L,
                lastError = "Awaiting Device Owner provisioning for OS-level suspension enforcement.",
                createdAt = now,
                updatedAt = now
            )
            snapDao.insert(pendingEntity)

            auditDao.insert(
                SecurityAuditEventEntity(
                    eventType = "SNAP_QUEUED_NOT_ENFORCED",
                    target = packageName,
                    details = "Registered in Snap Registry (Pending Device Owner): $applicationName ($packageName). No OS suspension applied.",
                    severity = "WARNING"
                )
            )

            return@withContext PolicyResult.AwaitingDeviceOwner(
                "Application registered in Snap Registry. Device Owner privileges required for native Android suspension."
            )
        }

        // Device Owner is provisioned: perform actual native OS suspension & uninstall blocking
        val enforcement = policyManager.suspendAndProtectPackage(packageName)
        val existing = snapDao.getByPackageName(packageName)

        when (enforcement) {
            is PolicyResult.AppliedAndVerified -> {
                val entity = existing?.copy(
                    applicationName = applicationName,
                    category = category,
                    desiredState = "SUSPENDED",
                    actualState = SnapState.SNAP_ACTIVE,
                    uninstallBlocked = true,
                    installationState = InstallationState.INSTALLED,
                    lastAppliedAt = now,
                    lastVerifiedAt = now,
                    lastError = null,
                    updatedAt = now
                ) ?: SnappedAppEntity(
                    packageName = packageName,
                    applicationName = applicationName,
                    category = category,
                    desiredState = "SUSPENDED",
                    actualState = SnapState.SNAP_ACTIVE,
                    uninstallBlocked = true,
                    installationState = InstallationState.INSTALLED,
                    firstSnappedAt = now,
                    lastAppliedAt = now,
                    lastVerifiedAt = now,
                    lastError = null,
                    createdAt = now,
                    updatedAt = now
                )
                snapDao.insert(entity)

                auditDao.insert(
                    SecurityAuditEventEntity(
                        eventType = "SNAP_ENFORCED",
                        target = packageName,
                        details = "Permanently snapped and verified: $applicationName ($packageName). OS suspension and uninstall blocking active.",
                        severity = "INFO"
                    )
                )
                PolicyResult.AppliedAndVerified
            }

            is PolicyResult.PartiallyApplied -> {
                val entity = existing?.copy(
                    applicationName = applicationName,
                    category = category,
                    desiredState = "SUSPENDED",
                    actualState = SnapState.PARTIALLY_ENFORCED,
                    uninstallBlocked = false,
                    installationState = InstallationState.INSTALLED,
                    lastAppliedAt = now,
                    lastVerifiedAt = now,
                    lastError = enforcement.details,
                    updatedAt = now
                ) ?: SnappedAppEntity(
                    packageName = packageName,
                    applicationName = applicationName,
                    category = category,
                    desiredState = "SUSPENDED",
                    actualState = SnapState.PARTIALLY_ENFORCED,
                    uninstallBlocked = false,
                    installationState = InstallationState.INSTALLED,
                    firstSnappedAt = now,
                    lastAppliedAt = now,
                    lastVerifiedAt = now,
                    lastError = enforcement.details,
                    createdAt = now,
                    updatedAt = now
                )
                snapDao.insert(entity)
                enforcement
            }

            else -> {
                val entity = existing?.copy(
                    applicationName = applicationName,
                    category = category,
                    desiredState = "SUSPENDED",
                    actualState = SnapState.SNAP_FAILED,
                    uninstallBlocked = false,
                    installationState = InstallationState.INSTALLED,
                    lastError = enforcement.errorMessage,
                    updatedAt = now
                ) ?: SnappedAppEntity(
                    packageName = packageName,
                    applicationName = applicationName,
                    category = category,
                    desiredState = "SUSPENDED",
                    actualState = SnapState.SNAP_FAILED,
                    uninstallBlocked = false,
                    installationState = InstallationState.INSTALLED,
                    firstSnappedAt = now,
                    lastAppliedAt = 0L,
                    lastVerifiedAt = 0L,
                    lastError = enforcement.errorMessage,
                    createdAt = now,
                    updatedAt = now
                )
                snapDao.insert(entity)

                auditDao.insert(
                    SecurityAuditEventEntity(
                        eventType = "SNAP_FAILED",
                        target = packageName,
                        details = "Enforcement failed for $applicationName: ${enforcement.errorMessage}",
                        severity = "ERROR"
                    )
                )
                enforcement
            }
        }
    }

    // --- EXCEPTIONAL EMERGENCY RECOVERY (SAFETY MECHANISM, NOT ORDINARY FEATURE) ---

    suspend fun emergencyRecoveryUnsnap(packageName: String): PolicyResult = withContext(Dispatchers.IO) {
        if (!policyManager.isDeviceOwner()) {
            return@withContext PolicyResult.AwaitingDeviceOwner("Device Owner privileges are required for recovery operations.")
        }

        val restoration = policyManager.emergencyRecoveryUnsnap(packageName)
        if (restoration !is PolicyResult.AppliedAndVerified) {
            auditDao.insert(
                SecurityAuditEventEntity(
                    eventType = "EMERGENCY_RECOVERY_FAILED",
                    target = packageName,
                    details = "Emergency unsnap failed: ${restoration.errorMessage}",
                    severity = "ERROR"
                )
            )
            return@withContext restoration
        }

        snapDao.deleteByPackageName(packageName)
        auditDao.insert(
            SecurityAuditEventEntity(
                eventType = "EMERGENCY_RECOVERY_ENFORCED",
                target = packageName,
                details = "Emergency recovery invoked and verified for package: $packageName",
                severity = "WARNING"
            )
        )

        PolicyResult.AppliedAndVerified
    }

    // --- 2. CONTENT SHIELD (PRIVATE DNS WITH VERIFIED TRANSITIONS) ---

    suspend fun setContentShield(enabled: Boolean, providerHostname: String? = null): PolicyResult = withContext(Dispatchers.IO) {
        val currentConfig = configDao.getConfig() ?: ProtectionConfigurationEntity()
        val hostname = providerHostname ?: currentConfig.dnsHostname
        val provider = DnsProviders.fromHostname(hostname)

        val isDO = policyManager.isDeviceOwner()
        if (!isDO) {
            // Section 2.2: Do NOT persist enabled configuration or return success when not Device Owner!
            return@withContext PolicyResult.AwaitingDeviceOwner("Cannot activate Content Shield: Device Owner privileges are required.")
        }

        val policyResult = if (enabled) {
            policyManager.enableContentShield(hostname)
        } else {
            policyManager.disableContentShield()
        }

        // Section 2.2: Persist state ONLY after successful policy enforcement!
        if (policyResult is PolicyResult.AppliedAndVerified) {
            configDao.insertOrUpdate(
                currentConfig.copy(
                    contentShieldEnabled = enabled,
                    dnsProviderName = provider.displayName,
                    dnsHostname = hostname,
                    disallowConfigPrivateDns = enabled,
                    updatedAt = System.currentTimeMillis()
                )
            )
            auditDao.insert(
                SecurityAuditEventEntity(
                    eventType = if (enabled) "CONTENT_SHIELD_ENABLED" else "CONTENT_SHIELD_DISABLED",
                    target = hostname,
                    details = "Content Shield ${if (enabled) "enforced" else "released"}. Provider: ${provider.displayName} ($hostname)",
                    severity = "INFO"
                )
            )
        } else {
            auditDao.insert(
                SecurityAuditEventEntity(
                    eventType = if (enabled) "CONTENT_SHIELD_ENABLE_FAILED" else "CONTENT_SHIELD_DISABLE_FAILED",
                    target = hostname,
                    details = "Policy enforcement failed: ${policyResult.errorMessage}",
                    severity = "ERROR"
                )
            )
        }

        policyResult
    }

    // --- 3. HARDENING & RESTRICTIONS ---

    suspend fun setAdvancedRestriction(restriction: String, enabled: Boolean): PolicyResult = withContext(Dispatchers.IO) {
        val isDO = policyManager.isDeviceOwner()
        if (!isDO) {
            return@withContext PolicyResult.AwaitingDeviceOwner("Device Owner privileges required to configure system restrictions.")
        }

        val result = policyManager.setUserRestriction(restriction, enabled)

        // Section 2.3: Only persist when Android actually accepted the restriction!
        if (result is PolicyResult.AppliedAndVerified) {
            val currentConfig = configDao.getConfig() ?: ProtectionConfigurationEntity()
            val updated = when (restriction) {
                android.os.UserManager.DISALLOW_CONFIG_VPN -> currentConfig.copy(disallowConfigVpn = enabled)
                android.os.UserManager.DISALLOW_DEBUGGING_FEATURES -> currentConfig.copy(disallowDebugging = enabled)
                android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY -> currentConfig.copy(disallowUnknownSources = enabled)
                else -> currentConfig
            }
            configDao.insertOrUpdate(updated.copy(updatedAt = System.currentTimeMillis()))
            auditDao.insert(
                SecurityAuditEventEntity(
                    eventType = "HARDENING_RESTRICTION_CHANGED",
                    target = restriction,
                    details = "Restriction $restriction successfully applied to $enabled.",
                    severity = "INFO"
                )
            )
        }

        result
    }

    suspend fun setSelfProtection(enabled: Boolean): PolicyResult = withContext(Dispatchers.IO) {
        val isDO = policyManager.isDeviceOwner()
        if (!isDO) {
            return@withContext PolicyResult.AwaitingDeviceOwner("Device Owner required for self protection.")
        }

        val result = policyManager.setSelfProtection(enabled)
        if (result is PolicyResult.AppliedAndVerified) {
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
        }
        result
    }

    suspend fun reconcile(reason: String = "MANUAL"): ReconciliationSummary {
        return reconciliationManager.reconcile(reason)
    }

    fun isDeviceOwner(): Boolean = policyManager.isDeviceOwner()
}
