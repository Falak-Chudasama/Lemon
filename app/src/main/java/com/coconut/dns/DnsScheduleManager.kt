package com.coconut.dns

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import com.coconut.data.AppDatabase
import com.coconut.data.DnsScheduleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

class DnsScheduleManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val scheduleDao = db.dnsScheduleDao()
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val ACTION_SCHEDULE_BOUNDARY = "com.coconut.action.DNS_SCHEDULE_BOUNDARY"
        const val PENDING_INTENT_REQUEST_CODE = 4040
    }

    val scheduleFlow: Flow<DnsScheduleEntity?> = scheduleDao.getScheduleFlow()

    /**
     * Determines whether the current local time falls inside the scheduled protection window.
     * Handles crossing midnight (e.g. 23:00 to 06:00).
     */
    fun isInsideScheduleWindow(
        startHour: Int = 23,
        startMinute: Int = 0,
        endHour: Int = 6,
        endMinute: Int = 0,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes > endMinutes) {
            // Window crosses midnight (e.g., 23:00 to 06:00: >= 1380 OR < 360)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            currentMinutes in startMinutes until endMinutes
        }
    }

    /**
     * Calculates the exact epoch milliseconds for the next transition boundary.
     */
    fun calculateNextBoundaryEpochMillis(
        startHour: Int = 23,
        startMinute: Int = 0,
        endHour: Int = 6,
        endMinute: Int = 0,
        nowCalendar: Calendar = Calendar.getInstance()
    ): Long {
        val isInside = isInsideScheduleWindow(startHour, startMinute, endHour, endMinute, nowCalendar)

        val target = Calendar.getInstance(nowCalendar.timeZone).apply {
            timeInMillis = nowCalendar.timeInMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (isInside) {
            // Next boundary is the end boundary (e.g., 06:00)
            target.set(Calendar.HOUR_OF_DAY, endHour)
            target.set(Calendar.MINUTE, endMinute)
            if (target.timeInMillis <= nowCalendar.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Next boundary is the start boundary (e.g., 23:00)
            target.set(Calendar.HOUR_OF_DAY, startHour)
            target.set(Calendar.MINUTE, startMinute)
            if (target.timeInMillis <= nowCalendar.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return target.timeInMillis
    }

    /**
     * Reconciles schedule enforcement state and arms the next AlarmManager boundary.
     * When enforceBoundary is true (boundary alarms, boot, time changes),
     * DNS is actively turned ON from 11:00 PM – 6:00 AM, and turned OFF outside the schedule window.
     */
    suspend fun reconcileSchedule(enforceBoundary: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val schedule = scheduleDao.getSchedule() ?: DnsScheduleEntity(
            startHour = 23,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            enabled = true
        ).also {
            scheduleDao.insertOrUpdate(it)
        }

        // Schedule is permanently enabled
        if (!schedule.enabled) {
            scheduleDao.setEnabled(true)
        }

        val now = System.currentTimeMillis()
        scheduleDao.updateLastEvaluated(now)

        val isInside = isInsideScheduleWindow(
            startHour = 23,
            startMinute = 0,
            endHour = 6,
            endMinute = 0
        )

        // If inside the scheduled window (11:00 PM – 6:00 AM), ensure VPN is running
        if (isInside) {
            if (VpnService.prepare(appContext) == null) {
                if (!CoconutVpnService.isVpnRunning.value) {
                    CoconutVpnService.start(appContext)
                }
            }
        } else if (enforceBoundary) {
            // Outside schedule window at boundary or boot: turn DNS VPN Off
            if (CoconutVpnService.isVpnRunning.value) {
                CoconutVpnService.stop(appContext)
            }
        }

        // Arm next alarm transition
        armNextAlarm(23, 0, 6, 0)

        // Also check if any 30-day domain requests have matured
        DomainQueueManager(appContext).applyMaturedRequests()

        isInside
    }

    private fun armNextAlarm(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        val nextBoundaryMillis = calculateNextBoundaryEpochMillis(startHour, startMinute, endHour, endMinute)

        val intent = Intent(appContext, DnsScheduleReceiver::class.java).apply {
            action = ACTION_SCHEDULE_BOUNDARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextBoundaryMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextBoundaryMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextBoundaryMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextBoundaryMillis,
                pendingIntent
            )
        }
    }

    private fun cancelNextAlarm() {
        val intent = Intent(appContext, DnsScheduleReceiver::class.java).apply {
            action = ACTION_SCHEDULE_BOUNDARY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    suspend fun setScheduleEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        scheduleDao.setEnabled(enabled)
        reconcileSchedule()
    }
}

