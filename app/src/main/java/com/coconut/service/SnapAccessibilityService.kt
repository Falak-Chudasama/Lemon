package com.coconut.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
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
    private lateinit var windowManager: WindowManager

    private var activeOverlayView: View? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0L

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isRunning.value = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

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

        // Never self-block Coconut
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
        // Debounce rapid redundant window state events for the SAME in-progress launch within 250ms
        if (packageName == lastBlockedPackage && (now - lastBlockedTime) < 250) {
            return
        }
        lastBlockedPackage = packageName
        lastBlockedTime = now

        // Step 1: Immediately take user Home with zero artificial latency
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Step 2: Present dedicated minimal BlockedAppActivity
        val appLabel = getAppLabel(packageName)
        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockedAppActivity.EXTRA_APP_LABEL, appLabel)
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Fallback: If system rejects Activity launch from background, use Accessibility Overlay
            showAccessibilityOverlay(packageName, appLabel)
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
