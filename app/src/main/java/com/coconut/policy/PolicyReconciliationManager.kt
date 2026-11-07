package com.coconut.policy

import android.content.Context
import android.os.UserManager
import com.coconut.core.SafePackageHelper
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
    val policyManager = CoconutPolicyManager(appContext)
    private val database = AppDatabase.get(appContext)
    private val snapDao = database.snappedAppDao()
    private val configDao = database.protectionConfigDao()
    private val stateDao = database.devicePolicyStateDao()
    private val auditDao = database.securityAuditDao()
    private val safePackageHelper = SafePackageHelper(appContext)
    private val pm = appContext.packageManager

    suspend fun reconcile(triggerReason: String = "MANUAL"): ReconciliationSummary = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var resuspendCount = 0
        var awaitingInstallCount = 0

        val isDo = policyManager.isDeviceOwner()
        if (!isDo) {
            val totalCount = snapDao.count()
            val summary = ReconciliationSummary(
                success = false,
                isDeviceOwner = false,
                selfProtectionEnforced = false,
                dnsEnforced = false,
                totalSnapped = totalCount,
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
                    totalSnappedCount = totalCount,
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

        // 1. Load desired configuration
        val config = configDao.getConfig() ?: ProtectionConfigurationEntity().also {
            configDao.insertOrUpdate(it)
        }

        // 2. Safely reconcile Self-Protection
        // Section 2.8: Coconut must NOT lock itself against uninstall by default.
        // If a previous version applied uninstall protection to itself, release it safely.
        var selfProtectionActive = policyManager.isSelfProtectionActive()
        if (!config.selfProtectionEnabled && selfProtectionActive) {
            val releaseResult = policyManager.releaseSelfProtectionIfActive()
            if (releaseResult is PolicyResult.AppliedAndVerified) {
                selfProtectionActive = false
                auditDao.insert(
                    SecurityAuditEventEntity(
                        eventType = "SELF_PROTECTION_RELEASED",
                        target = appContext.packageName,
                        details = "Legacy self-uninstall protection released to prevent device lock-in.",
                        severity = "INFO"
                    )
                )
            } else {
                errors.add("Failed to release legacy self-protection: ${releaseResult.errorMessage}")
            }
        } else if (config.selfProtectionEnabled != selfProtectionActive) {
            val setResult = policyManager.setSelfProtection(config.selfProtectionEnabled)
            if (setResult is PolicyResult.AppliedAndVerified) {
                selfProtectionActive = config.selfProtectionEnabled
            } else {
                errors.add("Failed to set self-protection: ${setResult.errorMessage}")
            }
        }

        // 3. Reconcile Content Shield (Private DNS)
        // Section 2.4 & 2.5: Content Shield owns its own Private DNS restriction.
        val dnsStatus = policyManager.getPrivateDnsStatus()
        var dnsEnforced = dnsStatus.isEnforced

        if (config.contentShieldEnabled) {
            val needsDnsUpdate = !dnsStatus.isEnforced ||
                    dnsStatus.host == null ||
                    !dnsStatus.host.equals(config.dnsHostname, ignoreCase = true) ||
                    !dnsStatus.isConfigBlocked

            if (needsDnsUpdate) {
                val dnsResult = policyManager.enableContentShield(config.dnsHostname)
                when (dnsResult) {
                    is PolicyResult.AppliedAndVerified -> {
                        dnsEnforced = true
                        auditDao.insert(
                            SecurityAuditEventEntity(
                                eventType = "DNS_SHIELD_RECONCILED",
                                target = config.dnsHostname,
                                details = "Content Shield DNS enforced with provider host: ${config.dnsHostname}",
                                severity = "INFO"
                            )
                        )
                    }
                    is PolicyResult.PartiallyApplied -> {
                        dnsEnforced = true
                        errors.add("Content Shield partial: ${dnsResult.details}")
                    }
                    else -> {
                        dnsEnforced = false
                        errors.add("Failed to reconcile Content Shield: ${dnsResult.errorMessage}")
                    }
                }
            }
        } else {
            // Content Shield is OFF:
            // Crucial: genuinely verify that Private DNS provider is released and user restriction cleared
            if (dnsStatus.isConfigBlocked || dnsStatus.isEnforced || dnsStatus.host != null) {
                val disableResult = policyManager.disableContentShield()
                if (disableResult is PolicyResult.AppliedAndVerified) {
                    dnsEnforced = false
                    auditDao.insert(
                        SecurityAuditEventEntity(
                            eventType = "DNS_SHIELD_OFF_RECONCILED",
                            target = "System DNS",
                            details = "Content Shield verified OFF. Android default Private DNS mode restored.",
                            severity = "INFO"
                        )
                    )
                } else {
                    dnsEnforced = true
                    errors.add("Failed to disable Content Shield during reconciliation: ${disableResult.errorMessage}")
                }
            } else {
                dnsEnforced = false
            }
        }

        // 4. Reconcile Optional Safe Hardening restrictions (Keep dangerous policies OFF)
        syncRestriction(UserManager.DISALLOW_CONFIG_VPN, config.disallowConfigVpn, errors)
        syncRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES, config.disallowDebugging, errors)
        syncRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, config.disallowUnknownSources, errors)

        // 5. Reconcile Permanent Snap Registry
        val now = System.currentTimeMillis()
        val snappedList = snapDao.getAllOnce()

        for (snappedApp in snappedList) {
            val isInstalled = runCatching {
                pm.getApplicationInfo(snappedApp.packageName, 0)
                true
            }.getOrDefault(false)

            if (isInstalled) {
                if (safePackageHelper.isProtectedPackage(snappedApp.packageName)) {
                    // System component protection guard
                    errors.add("Refusing to enforce snap on system-critical package: ${snappedApp.packageName}")
                    continue
                }

                val currentlySuspended = policyManager.isPackageSuspended(snappedApp.packageName)
                val currentlyBlocked = policyManager.isPackageUninstallBlocked(snappedApp.packageName)

                if (!currentlySuspended || !currentlyBlocked) {
                    val snapResult = policyManager.suspendAndProtectPackage(snappedApp.packageName)
                    when (snapResult) {
                        is PolicyResult.AppliedAndVerified -> {
                            resuspendCount++
                            snapDao.update(
                                snappedApp.copy(
                                    actualState = SnapState.SNAP_ACTIVE,
                                    uninstallBlocked = true,
                                    installationState = InstallationState.INSTALLED,
                                    lastAppliedAt = now,
                                    lastVerifiedAt = now,
                                    lastError = null,
                                    updatedAt = now
                                )
                            )
                            auditDao.insert(
                                SecurityAuditEventEntity(
                                    eventType = "PACKAGE_RESUSPENDED",
                                    target = snappedApp.packageName,
                                    details = "Target package restored to permanent Snap state (Suspended & Uninstall-Blocked).",
                                    severity = "INFO"
                                )
                            )
                        }
                        is PolicyResult.PartiallyApplied -> {
                            resuspendCount++
                            snapDao.update(
                                snappedApp.copy(
                                    actualState = SnapState.PARTIALLY_ENFORCED,
                                    uninstallBlocked = false,
                                    installationState = InstallationState.INSTALLED,
                                    lastAppliedAt = now,
                                    lastVerifiedAt = now,
                                    lastError = snapResult.details,
                                    updatedAt = now
                                )
                            )
                        }
                        else -> {
                            errors.add("Failed to resuspend ${snappedApp.packageName}: ${snapResult.errorMessage}")
                            snapDao.update(
                                snappedApp.copy(
                                    actualState = SnapState.SNAP_FAILED,
                                    installationState = InstallationState.INSTALLED,
                                    lastError = snapResult.errorMessage,
                                    updatedAt = now
                                )
                            )
                        }
                    }
                } else {
                    // Already properly suspended & blocked
                    snapDao.update(
                        snappedApp.copy(
                            actualState = SnapState.SNAP_ACTIVE,
                            uninstallBlocked = true,
                            installationState = InstallationState.INSTALLED,
                            lastVerifiedAt = now,
                            lastError = null,
                            updatedAt = now
                        )
                    )
                }
            } else {
                // Application is uninstalled: preserve permanent registry entry awaiting possible reinstallation!
                awaitingInstallCount++
                if (snappedApp.actualState != SnapState.AWAITING_INSTALLATION || snappedApp.installationState != InstallationState.NOT_INSTALLED) {
                    snapDao.update(
                        snappedApp.copy(
                            actualState = SnapState.AWAITING_INSTALLATION,
                            installationState = InstallationState.NOT_INSTALLED,
                            uninstallBlocked = false,
                            lastVerifiedAt = now,
                            lastError = null,
                            updatedAt = now
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
                details = "Triggered by $triggerReason. Snapped: $totalSnapped (Active: $activeSnapped, Awaiting: $awaitingInstallCount, Restored: $resuspendCount). Errors: ${errors.size}.",
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

    private suspend fun syncRestriction(restriction: String, desired: Boolean, errors: MutableList<String>) {
        val actual = policyManager.hasUserRestriction(restriction)
        if (actual != desired) {
            val result = policyManager.setUserRestriction(restriction, desired)
            if (result !is PolicyResult.AppliedAndVerified) {
                errors.add("Failed to set restriction $restriction to $desired: ${result.errorMessage}")
            }
        }
    }
}
