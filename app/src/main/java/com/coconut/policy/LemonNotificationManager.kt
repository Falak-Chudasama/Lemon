package com.coconut.policy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.coconut.MainActivity
import com.coconut.R
import java.util.concurrent.ConcurrentHashMap

object LemonNotificationManager {

    const val CHANNEL_DNS_STATUS = "lemon_dns_status_channel"
    const val CHANNEL_QUARANTINE = "lemon_quarantine_channel"
    const val CHANNEL_SECURITY_ALERTS = "lemon_security_alerts_channel"

    const val NOTIFICATION_ID_FOREGROUND_VPN = 2026
    const val NOTIFICATION_ID_VPN_ALERT = 2027
    const val NOTIFICATION_ID_QUARANTINE = 2028
    const val NOTIFICATION_ID_PERMISSION_ALERT = 2029

    private val lastDispatchedTimestamps = ConcurrentHashMap<String, Long>()
    private const val DEFAULT_COOLDOWN_MS = 60_000L // 1 minute deduplication cooldown

    fun initializeChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return

            // 1. Ongoing DNS Service
            val dnsChannel = NotificationChannel(
                CHANNEL_DNS_STATUS,
                "DNS Protection Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active on-device DNS protection status"
                setShowBadge(false)
            }

            // 2. Quarantine Session & Timer
            val quarantineChannel = NotificationChannel(
                CHANNEL_QUARANTINE,
                "Quarantine & Self-Control",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when Quarantine starts, warns before expiry, and alerts on completion"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Security & Recovery Warnings
            val securityChannel = NotificationChannel(
                CHANNEL_SECURITY_ALERTS,
                "Security & Recovery Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when VPN protection is revoked or permissions are disabled"
                enableVibration(true)
                setShowBadge(true)
            }

            nm.createNotificationChannels(listOf(dnsChannel, quarantineChannel, securityChannel))
        }
    }

    private fun canPostNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun shouldThrottle(eventKey: String, cooldownMs: Long = DEFAULT_COOLDOWN_MS): Boolean {
        val now = System.currentTimeMillis()
        val last = lastDispatchedTimestamps[eventKey] ?: 0L
        if (now - last < cooldownMs) {
            return true
        }
        lastDispatchedTimestamps[eventKey] = now
        return false
    }

    fun notifyVpnRevoked(context: Context) {
        if (!canPostNotification(context)) return
        if (shouldThrottle("vpn_revoked", 30_000L)) return

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY_ALERTS)
            .setContentTitle("DNS Protection Revoked")
            .setContentText("VPN consent was revoked. Tap to reopen Lemon and restore protection.")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_VPN_ALERT, notification)
    }

    fun notifyVpnRecovered(context: Context) {
        if (!canPostNotification(context)) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.cancel(NOTIFICATION_ID_VPN_ALERT) // Clear failure alert

        if (shouldThrottle("vpn_recovered", 60_000L)) return

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DNS_STATUS)
            .setContentTitle("Protection Restored")
            .setContentText("Lemon DNS filtering is active and verified.")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_VPN_ALERT, notification)
    }

    fun notifyQuarantineStarted(context: Context, durationMinutes: Int, blockedAppCount: Int) {
        if (!canPostNotification(context)) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        val durationStr = if (hours > 0 && mins > 0) "${hours}h ${mins}m" else if (hours > 0) "${hours}h" else "${mins}m"

        val notification = NotificationCompat.Builder(context, CHANNEL_QUARANTINE)
            .setContentTitle("Quarantine Active ($durationStr)")
            .setContentText("$blockedAppCount apps restricted. Protection locked until period ends.")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        nm.notify(NOTIFICATION_ID_QUARANTINE, notification)
    }

    fun notifyQuarantineExpiringSoon(context: Context, remainingMinutes: Int) {
        if (!canPostNotification(context)) return
        if (shouldThrottle("quarantine_10min", 600_000L)) return

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_QUARANTINE)
            .setContentTitle("Quarantine Ending Soon")
            .setContentText("Approximately $remainingMinutes minutes remaining in the active session.")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_QUARANTINE + 1, notification)
    }

    fun notifyQuarantineExpired(context: Context) {
        if (!canPostNotification(context)) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.cancel(NOTIFICATION_ID_QUARANTINE)
        nm.cancel(NOTIFICATION_ID_QUARANTINE + 1)

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            2003,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_QUARANTINE)
            .setContentTitle("Quarantine Complete")
            .setContentText("Self-control period has ended. Temporary restrictions lifted.")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_QUARANTINE + 2, notification)
    }

    fun notifyPermissionRevoked(context: Context, permissionName: String) {
        if (!canPostNotification(context)) return
        if (shouldThrottle("perm_$permissionName", 120_000L)) return

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY_ALERTS)
            .setContentTitle("Enforcement Permission Disabled")
            .setContentText("$permissionName was disabled. Lemon cannot enforce restrictions until restored.")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_PERMISSION_ALERT, notification)
    }
}

