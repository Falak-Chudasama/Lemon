package com.coconut.policy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
                val pm = appContext.packageManager

                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> {
                        auditDao.insert(
                            SecurityAuditEventEntity(
                                eventType = "BOOT_COMPLETED",
                                target = "System",
                                details = "Device reboot completed. Snap registry active.",
                                severity = "INFO"
                            )
                        )
                    }

                    Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        auditDao.insert(
                            SecurityAuditEventEntity(
                                eventType = "UPDATE_PRESERVED",
                                target = appContext.packageName,
                                details = "Coconut in-place application update verified. Snap records preserved.",
                                severity = "INFO"
                            )
                        )
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val packageName = intent.data?.schemeSpecificPart ?: return@launch
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!isReplacing) {
                            // Target application was uninstalled: NEVER delete the record!
                            val existing = snapDao.getByPackageName(packageName)
                            if (existing != null) {
                                val now = System.currentTimeMillis()
                                snapDao.update(
                                    existing.copy(
                                        installationState = InstallationState.NOT_INSTALLED,
                                        actualState = SnapState.NOT_INSTALLED,
                                        updatedAt = now
                                    )
                                )
                                auditDao.insert(
                                    SecurityAuditEventEntity(
                                        eventType = "PACKAGE_UNINSTALLED_SNAP_RETAINED",
                                        target = packageName,
                                        details = "Snapped app $packageName uninstalled. Record retained for reinstall detection.",
                                        severity = "INFO"
                                    )
                                )
                            }
                        }
                    }

                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        val packageName = intent.data?.schemeSpecificPart ?: return@launch
                        if (packageName == appContext.packageName) return@launch

                        val snappedApp = snapDao.getByPackageName(packageName)
                        if (snappedApp != null) {
                            // Target application reinstalled or updated!
                            val now = System.currentTimeMillis()
                            val updatedLabel = try {
                                val info = pm.getApplicationInfo(packageName, 0)
                                pm.getApplicationLabel(info).toString()
                            } catch (_: Exception) {
                                snappedApp.applicationName
                            }

                            snapDao.update(
                                snappedApp.copy(
                                    applicationName = updatedLabel,
                                    actualState = SnapState.SNAPPED,
                                    installationState = InstallationState.INSTALLED,
                                    lastAppliedAt = now,
                                    updatedAt = now
                                )
                            )

                            auditDao.insert(
                                SecurityAuditEventEntity(
                                    eventType = "REINSTALL_RECOGNIZED",
                                    target = packageName,
                                    details = "Package $packageName reinstalled. Re-activated Snap enforcement.",
                                    severity = "INFO"
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
