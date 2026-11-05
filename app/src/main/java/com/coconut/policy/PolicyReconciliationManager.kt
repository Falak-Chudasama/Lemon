package com.coconut.policy

import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager
import com.coconut.data.AppDatabase
import com.coconut.data.DevicePolicyStateEntity
import com.coconut.data.InstallationState
import com.coconut.data.ProtectionConfigurationEntity
import com.coconut.data.SecurityAuditEventEntity
import com.coconut.data.SnapState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ReconciliationSummary(
    val success: Boolean,
    val isDeviceOwner: Boolean,
    val selfProtectionEnforced: Boolean,
    val dnsEnforced: Boolean,
    val totalSnapped: Int,
    val resuspendCount: Int,
    val awaitingInstallCount: Int,
    val errors: List<String>
)

class PolicyReconciliationManager(context: Context) {
    private val appContext = context.applicationContext
    private val policyManager = CoconutPolicyManager(appContext)
    private val database = AppDatabase.get(appContext)
    private val snapDao = database.snappedAppDao()
    private val configDao = database.protectionConfigDao()
    private val stateDao = database.devicePolicyStateDao()
    private val auditDao = database.securityAuditDao()
    private val pm = appContext.packageManager

    suspend fun reconcile(triggerReason: String = "MANUAL"): ReconciliationSummary = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var resuspendCount = 0
        var awaitingInstallCount = 0

        val isDo = policyManager.isDeviceOwner()
        if (!isDo) {
            val summary = ReconciliationSummary(
                success = false,
                isDeviceOwner = false,
                selfProtectionEnforced = false,
                dnsEnforced = false,
                totalSnapped = 0,
                resuspendCount = 0,
                awaitingInstallCount = 0,
                errors = listOf("Coconut is not provisioned as Device Owner. Policy reconciliation paused.")
            )
            stateDao.insertOrUpdate(
                DevicePolicyStateEntity(
                    isDeviceOwner = false,
                    isUninstallBlocked = false,
                    activeDnsMode = "UNAVAILABLE",
                    activeDnsHost = null,
                    isDnsProtected = false,
                    totalSnappedCount = snapDao.count(),
                    activeSnappedCount = 0,
                    lastReconciledAt = System.currentTimeMillis(),
                    reconciliationStatus = "NON_DEVICE_OWNER"
                )
            )
            auditDao.insert(
                SecurityAuditEventEntity(
                    eventType = "RECONCILIATION_HALTED",
                    target = "DPC",
                    details = "Reconciliation triggered by $triggerReason but device is not in Device Owner mode.",
                    severity = "WARNING"
                )
            )
            return@withContext summary
        }

        // 1. Load desired config (or default if uninitialized)
        val config = configDao.getConfig() ?: ProtectionConfigurationEntity().also {
            configDao.insertOrUpdate(it)
        }

        // 2. Reconcile Self-Protection (Uninstall block on Coconut itself)
        var selfProtectionActive = policyManager.isSelfProtectionActive()
        if (config.selfProtectionEnabled != selfProtectionActive) {
            val result = policyManager.setSelfProtection(config.selfProtectionEnabled)
            if (result is PolicyResult.Success) {
                selfProtectionActive = config.selfProtectionEnabled
                auditDao.insert(
                    SecurityAuditEventEntity(
                        eventType = if (config.selfProtectionEnabled) "SELF_PROTECTION_RESTORED" else "SELF_PROTECTION_DISABLED",
                        target = appContext.packageName,
                        details = "Uninstall protection reconciled to ${config.selfProtectionEnabled}.",
                        severity = "INFO"
                    )
                )
            } else if (result is PolicyResult.Failure) {
                errors.add("Failed to enforce self-protection: ${result.reason}")
            }
        }

        // 3. Reconcile Content Shield (Private DNS)
        val dnsStatus = policyManager.getPrivateDnsStatus()
        var dnsEnforced = dnsStatus.isEnforced
        if (config.contentShieldEnabled) {
            val needsDnsUpdate = !dnsStatus.isEnforced ||
                    dnsStatus.host == null ||
                    !dnsStatus.host.equals(config.dnsHostname, ignoreCase = true) ||
                    !dnsStatus.isConfigBlocked

            if (needsDnsUpdate) {
                val dnsResult = policyManager.enableContentShield(config.dnsHostname)
                if (dnsResult is PolicyResult.Success) {
                    dnsEnforced = true
                    auditDao.insert(
                        SecurityAuditEventEntity(
                            eventType = "DNS_SHIELD_RECONCILED",
                            target = config.dnsHostname,
                            details = "Content Shield DNS enforced with provider host: ${config.dnsHostname}",
                            severity = "INFO"
                        )
                    )
                } else if (dnsResult is PolicyResult.Failure) {
                    errors.add("Failed to reconcile Content Shield: ${dnsResult.reason}")
                    dnsEnforced = false
                }
            }
        } else {
            // Disabled
            if (dnsStatus.isConfigBlocked || dnsStatus.mode == 3) {
                policyManager.disableContentShield()
                dnsEnforced = false
            }
        }

        // 4. Reconcile Advanced Hardening restrictions
        val shouldLockPrivateDns = config.contentShieldEnabled || config.disallowConfigPrivateDns
        syncRestriction(UserManager.DISALLOW_CONFIG_PRIVATE_DNS, shouldLockPrivateDns, errors)
        syncRestriction(UserManager.DISALLOW_CONFIG_VPN, config.disallowConfigVpn, errors)
        syncRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES, config.disallowDebugging, errors)
        syncRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, config.disallowUnknownSources, errors)
        syncRestriction(UserManager.DISALLOW_SAFE_BOOT, config.disallowSafeBoot, errors)
        syncRestriction(UserManager.DISALLOW_FACTORY_RESET, config.disallowFactoryReset, errors)
        syncRestriction(UserManager.DISALLOW_APPS_CONTROL, config.disallowAppsControl, errors)

        // 5. Reconcile Permanent Snap Registry
        val snappedList = snapDao.getAllOnce()
        for (snappedApp in snappedList) {
            val isInstalled = runCatching {
                pm.getApplicationInfo(snappedApp.packageName, 0)
                true
            }.getOrDefault(false)

            if (isInstalled) {
                val currentlySuspended = policyManager.isPackageSuspended(snappedApp.packageName)
                if (!currentlySuspended) {
                    val suspendResult = policyManager.suspendPackage(snappedApp.packageName)
                    if (suspendResult is PolicyResult.Success) {
                        resuspendCount++
                        snapDao.update(
                            snappedApp.copy(
                                actualState = SnapState.PROTECTED,
                                installationState = InstallationState.INSTALLED,
                                lastAppliedAt = System.currentTimeMillis(),
                                lastError = null,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        auditDao.insert(
                            SecurityAuditEventEntity(
                                eventType = "PACKAGE_RESUSPENDED",
                                target = snappedApp.packageName,
                                details = "Reconciliation detected un-suspended target package. Resuspended successfully.",
                                severity = "WARNING"
                            )
                        )
                    } else if (suspendResult is PolicyResult.Failure) {
                        errors.add("Failed to resuspend ${snappedApp.packageName}: ${suspendResult.reason}")
                        snapDao.update(
                            snappedApp.copy(
                                actualState = SnapState.SNAP_FAILED,
                                installationState = InstallationState.INSTALLED,
                                lastError = suspendResult.reason,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    // Already suspended and installed
                    if (snappedApp.actualState != SnapState.PROTECTED || snappedApp.installationState != InstallationState.INSTALLED) {
                        snapDao.update(
                            snappedApp.copy(
                                actualState = SnapState.PROTECTED,
                                installationState = InstallationState.INSTALLED,
                                lastError = null,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } else {
                // Application is uninstalled
                awaitingInstallCount++
                if (snappedApp.actualState != SnapState.AWAITING_INSTALLATION || snappedApp.installationState != InstallationState.NOT_INSTALLED) {
                    snapDao.update(
                        snappedApp.copy(
                            actualState = SnapState.AWAITING_INSTALLATION,
                            installationState = InstallationState.NOT_INSTALLED,
                            lastError = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        // 6. Record policy state snapshot
        val totalSnapped = snappedList.size
        val activeSnapped = totalSnapped - awaitingInstallCount
        val finalDns = policyManager.getPrivateDnsStatus()

        stateDao.insertOrUpdate(
            DevicePolicyStateEntity(
                isDeviceOwner = true,
                isUninstallBlocked = selfProtectionActive,
                activeDnsMode = finalDns.modeName,
                activeDnsHost = finalDns.host,
                isDnsProtected = dnsEnforced,
                totalSnappedCount = totalSnapped,
                activeSnappedCount = activeSnapped,
                lastReconciledAt = System.currentTimeMillis(),
                reconciliationStatus = if (errors.isEmpty()) "HEALTHY" else "DEGRADED"
            )
        )

        // 7. Audit log event
        auditDao.insert(
            SecurityAuditEventEntity(
                eventType = "RECONCILIATION_COMPLETED",
                target = triggerReason,
                details = "Triggered by $triggerReason. Snapped: $totalSnapped (Active: $activeSnapped, Awaiting: $awaitingInstallCount, Resuspended: $resuspendCount). Errors: ${errors.size}.",
                severity = if (errors.isEmpty()) "INFO" else "WARNING"
            )
        )

        ReconciliationSummary(
            success = errors.isEmpty(),
            isDeviceOwner = true,
            selfProtectionEnforced = selfProtectionActive,
            dnsEnforced = dnsEnforced,
            totalSnapped = totalSnapped,
            resuspendCount = resuspendCount,
            awaitingInstallCount = awaitingInstallCount,
            errors = errors
        )
    }

    private fun syncRestriction(restriction: String, desired: Boolean, errors: MutableList<String>) {
        val actual = policyManager.hasUserRestriction(restriction)
        if (actual != desired) {
            val result = policyManager.setUserRestriction(restriction, desired)
            if (result is PolicyResult.Failure) {
                errors.add("Failed to set restriction $restriction to $desired: ${result.reason}")
            }
        }
    }
}

