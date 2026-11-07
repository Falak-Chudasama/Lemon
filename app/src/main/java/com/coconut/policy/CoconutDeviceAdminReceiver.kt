package com.coconut.policy

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.coconut.data.AppDatabase
import com.coconut.data.SecurityAuditEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoconutDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.get(appContext)
            db.securityAuditDao().insert(
                SecurityAuditEventEntity(
                    eventType = "DEVICE_ADMIN_ENABLED",
                    target = "Coconut",
                    details = "Coconut Device Admin receiver has been enabled.",
                    severity = "INFO"
                )
            )
            // Trigger reconciliation to evaluate policies
            PolicyReconciliationManager(appContext).reconcile("ADMIN_ENABLED")
        }
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            // Note: Per Section 2.8, do NOT automatically lock Coconut itself against uninstallation.
            // Self-protection must default to false and never trap the user.
            val db = AppDatabase.get(appContext)
            db.securityAuditDao().insert(
                SecurityAuditEventEntity(
                    eventType = "PROVISIONING_COMPLETE",
                    target = "Coconut DPC",
                    details = "Fully managed device provisioning successfully completed. Device Owner active.",
                    severity = "INFO"
                )
            )

            // Run initial reconciliation
            PolicyReconciliationManager(appContext).reconcile("PROVISIONING_COMPLETE")
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.get(appContext)
            db.securityAuditDao().insert(
                SecurityAuditEventEntity(
                    eventType = "DEVICE_ADMIN_DISABLED",
                    target = "Coconut",
                    details = "Coconut Device Admin receiver has been disabled. Policies no longer enforced.",
                    severity = "ERROR"
                )
            )
        }
    }
}
