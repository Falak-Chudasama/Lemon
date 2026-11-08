package com.coconut.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.coconut.R
import com.coconut.data.AppDatabase
import com.coconut.ui.BlockedAppActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * SnapAccessibilityService detects foreground application transitions to enforce
 * user-configured Snap app blocking deterministically.
 *
 * Privacy & Policy Compliance:
 * - Does NOT request or retrieve window content (canRetrieveWindowContent = false).
 * - Does NOT inspect text, passwords, messages, keystrokes, or personal data.
 * - Does NOT monitor or prevent navigation within Android Settings.
 * - Observes ONLY window state transitions (event.packageName) to match against user-chosen Snapped apps.
 * - All blocking rules are stored locally; zero data is transmitted off the device.
 */
class SnapAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val snappedPackages = ConcurrentHashMap.newKeySet<String>()
    private val lastNotificationTime = ConcurrentHashMap<String, Long>()
    private lateinit var windowManager: WindowManager
    private lateinit var notificationManager: NotificationManager

    private var activeOverlayView: View? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0L

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "coconut_snap_enforcement"
        private const val NOTIFICATION_RATE_LIMIT_MS = 10_000L // 10 seconds per package

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isRunning.value = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Continuously synchronize in-memory snapped package set with local Room database
        serviceScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.get(applicationContext).snappedAppDao()
            dao.observeAll().collectLatest { entities ->
                val active = entities.map { it.packageName }.toSet()
                snappedPackages.clear()
                snappedPackages.addAll(active)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventPackage = event.packageName?.toString() ?: return
        val myPackage = applicationContext.packageName

        // Never self-block Lemon
        if (eventPackage == myPackage) {
            removeOverlayIfPresent()
            return
        }

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        // Deterministic Snap enforcement: check if foreground package is on user's blocked list
        if (snappedPackages.contains(eventPackage)) {
            handleSnappedPackageForeground(eventPackage)
            return
        }

        // When a permitted app or launcher is in foreground, reset blocked package tracker
        if (eventPackage != lastBlockedPackage) {
            lastBlockedPackage = null
        }

        // Dismiss any lingering overlay when permitted app is in foreground
        removeOverlayIfPresent()
    }

    private fun handleSnappedPackageForeground(packageName: String) {
        val now = SystemClock.elapsedRealtime()
        // Debounce rapid redundant window state events for the SAME in-progress launch within 300ms
        if (packageName == lastBlockedPackage && (now - lastBlockedTime) < 300) {
            return
        }
        lastBlockedPackage = packageName
        lastBlockedTime = now

        // Step 1: Immediately take user Home with zero artificial latency
        val homeSuccess = performGlobalAction(GLOBAL_ACTION_HOME)

        // Step 2: Post rate-limited notification (low noise channel, does not open Coconut UI)
        postBlockedNotification(packageName)

        // Step 3: Only if Home action failed, use fallback overlay
        if (!homeSuccess) {
            val appLabel = getAppLabel(packageName)
            showAccessibilityOverlay(packageName, appLabel)
        }
    }

    private fun postBlockedNotification(packageName: String) {
        val now = SystemClock.elapsedRealtime()
        val lastTime = lastNotificationTime[packageName] ?: 0L
        if (now - lastTime < NOTIFICATION_RATE_LIMIT_MS) {
            return
        }
        lastNotificationTime[packageName] = now

        val appLabel = getAppLabel(packageName)
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle("Lemon Snap")
            .setContentText("$appLabel is snapped and can't be launched.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setSilent(true)
            .build()

        try {
            // Stable notification ID derived from package name hash
            val notifId = 3000 + (packageName.hashCode() and 0x7FFF)
            notificationManager.notify(notifId, notification)
        } catch (_: SecurityException) {
            // Permission denied on Android 13+; blocking still proceeds silently
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Snap Enforcement",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications when a snapped application is blocked"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showAccessibilityOverlay(packageName: String, label: String) {
        if (activeOverlayView != null) return

        try {
            val layoutType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val inflater = LayoutInflater.from(this)
            val overlay = inflater.inflate(R.layout.overlay_blocked_app, null)
            overlay.findViewById<TextView>(R.id.tvBlockedTitle)?.text = "$label is Snapped"
            overlay.findViewById<Button>(R.id.btnReturnHome)?.setOnClickListener {
                performGlobalAction(GLOBAL_ACTION_HOME)
                removeOverlayIfPresent()
            }

            windowManager.addView(overlay, params)
            activeOverlayView = overlay
        } catch (_: Exception) {
            // Overlay fallback could not be displayed
        }
    }

    private fun removeOverlayIfPresent() {
        activeOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
            activeOverlayView = null
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    override fun onInterrupt() {
        removeOverlayIfPresent()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        removeOverlayIfPresent()
        serviceScope.cancel()
    }
}
