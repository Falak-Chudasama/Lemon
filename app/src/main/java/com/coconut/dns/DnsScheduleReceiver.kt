package com.coconut.dns

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coconut.data.AppDatabase
import com.coconut.data.SecurityAuditEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DnsScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.TIME_SET",
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            DnsScheduleManager.ACTION_SCHEDULE_BOUNDARY -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                            AppDatabase.get(context).securityAuditDao().insert(
                                SecurityAuditEventEntity(
                                    eventType = "EXACT_ALARM_STATE_CHANGED",
                                    target = "Lemon",
                                    details = "SCHEDULE_EXACT_ALARM permission state changed. Boundary alarms rescheduled.",
                                    severity = "INFO"
                                )
                            )
                        }

                        val manager = DnsScheduleManager(context)
                        manager.reconcileSchedule(enforceBoundary = true)
                        com.coconut.policy.QuarantineManager.getInstance(context).reconcile()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}

