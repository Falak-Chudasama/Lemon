package com.coconut.policy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                            // Target application was reinstalled! Automatically apply suspension!
                            val suspendResult = policyManager.suspendPackage(packageName)
                            val now = System.currentTimeMillis()

                            if (suspendResult is PolicyResult.Success) {
                                snapDao.update(
                                    snappedApp.copy(
                                        actualState = SnapState.PROTECTED,
                                        installationState = InstallationState.INSTALLED,
                                        lastAppliedAt = now,
                                        lastError = null,
                                        updatedAt = now
                                    )
                                )
                                auditDao.insert(
                                    SecurityAuditEventEntity(
                                        eventType = "AUTO_RESUSPEND_ON_INSTALL",
                                        target = packageName,
                                        details = "Package $packageName was reinstalled and automatically suspended via Snap Registry.",
                                        severity = "INFO"
                                    )
                                )
                            } else if (suspendResult is PolicyResult.Failure) {
                                snapDao.update(
                                    snappedApp.copy(
                                        actualState = SnapState.SNAP_FAILED,
                                        installationState = InstallationState.INSTALLED,
                                        lastError = suspendResult.reason,
                                        updatedAt = now
                                    )
                                )
                                auditDao.insert(
                                    SecurityAuditEventEntity(
                                        eventType = "AUTO_RESUSPEND_FAILED",
                                        target = packageName,
                                        details = "Package $packageName reinstalled but suspension failed: ${suspendResult.reason}",
                                        severity = "ERROR"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Prevent unhandled crashes in background receiver
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
