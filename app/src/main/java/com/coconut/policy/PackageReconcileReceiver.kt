package com.coconut.policy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coconut.core.SafePackageHelper
import com.coconut.data.AppDatabase
import com.coconut.data.InstallationState
import com.coconut.data.SecurityAuditEventEntity
import com.coconut.data.SnapState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageReconcileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(appContext)
                val snapDao = db.snappedAppDao()
                val auditDao = db.securityAuditDao()
                val reconciliationManager = PolicyReconciliationManager(appContext)
                val policyManager = CoconutPolicyManager(appContext)
                val safePackageHelper = SafePackageHelper(appContext)

                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> {
                        auditDao.insert(
                            SecurityAuditEventEntity(
                                eventType = "BOOT_COMPLETED",
                                target = "System",
                                details = "Device reboot completed. Reconciling all policies.",
                                severity = "INFO"
                            )
                        )
                        reconciliationManager.reconcile("BOOT_COMPLETED")
                    }

                    Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        auditDao.insert(
                            SecurityAuditEventEntity(
                                eventType = "UPDATE_PRESERVED",
                                target = appContext.packageName,
                                details = "Coconut in-place application update verified. Preserving policies.",
                                severity = "INFO"
                            )
                        )
                        // Make sure we release legacy self-protection on update
                        policyManager.releaseSelfProtectionIfActive()
                        reconciliationManager.reconcile("COCONUT_UPDATE")
                    }

                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        val packageName = intent.data?.schemeSpecificPart ?: return@launch
                        if (packageName == appContext.packageName) {
                            return@launch
                        }

                        // Check if this newly added/replaced package is registered in Snap Registry
                        val snappedApp = snapDao.getByPackageName(packageName)
                        if (snappedApp != null) {
                            // Section 5.6: Target application reinstalled or updated!
                            // 1. Identify target
                            // 2. Verify permissible target
                            if (safePackageHelper.isProtectedPackage(packageName)) {
                                auditDao.insert(
                                    SecurityAuditEventEntity(
                                        eventType = "RESUSPEND_REJECTED",
                                        target = packageName,
                                        details = "Package $packageName is critical to system operation. Skipping resuspension.",
                                        severity = "WARNING"
                                    )
                                )
                                return@launch
                            }

                            // 3 & 4. Reapply suspension & uninstall protection
                            val result = policyManager.suspendAndProtectPackage(packageName)
                            val now = System.currentTimeMillis()

                            when (result) {
                                is PolicyResult.AppliedAndVerified -> {
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
                                            eventType = "AUTO_RESUSPEND_ON_INSTALL",
                                            target = packageName,
                                            details = "Package $packageName reinstalled/updated and automatically restored to permanent Snap.",
                                            severity = "INFO"
                                        )
                                    )
                                }
                                is PolicyResult.PartiallyApplied -> {
                                    snapDao.update(
                                        snappedApp.copy(
                                            actualState = SnapState.PARTIALLY_ENFORCED,
                                            uninstallBlocked = false,
                                            installationState = InstallationState.INSTALLED,
                                            lastAppliedAt = now,
                                            lastVerifiedAt = now,
                                            lastError = result.details,
                                            updatedAt = now
                                        )
                                    )
                                }
                                else -> {
                                    snapDao.update(
                                        snappedApp.copy(
                                            actualState = SnapState.SNAP_FAILED,
                                            installationState = InstallationState.INSTALLED,
                                            lastError = result.errorMessage,
                                            updatedAt = now
                                        )
                                    )
                                    auditDao.insert(
                                        SecurityAuditEventEntity(
                                            eventType = "AUTO_RESUSPEND_FAILED",
                                            target = packageName,
                                            details = "Package $packageName reinstalled but suspension failed: ${result.errorMessage}",
                                            severity = "ERROR"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
