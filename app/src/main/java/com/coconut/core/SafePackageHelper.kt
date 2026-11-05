package com.coconut.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

class SafePackageHelper(private val context: Context) {
    private val appContext = context.applicationContext
    private val pm = appContext.packageManager

    private val knownEssentialPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.gms",
        "com.android.vending", // Play Store needed for updates
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.emergency",
        appContext.packageName
    )

    fun isProtectedPackage(packageName: String): Boolean {
        if (packageName == appContext.packageName) return true
        if (packageName in knownEssentialPackages) return true

        // Check if package is active launcher
        val launcher = getDefaultLauncherPackage()
        if (packageName == launcher) return true

        // Check if package is an enabled input method (keyboard)
        val keyboardPackages = getEnabledKeyboardPackages()
        if (packageName in keyboardPackages) return true

        // Check permission controller in known list
        if (packageName in knownEssentialPackages) return true

        // Check default dialer / emergency calls
        runCatching {
            val telecomManager = appContext.getSystemService(TelecomManager::class.java)
            val defaultDialer = telecomManager?.defaultDialerPackage
            if (packageName == defaultDialer) return true
        }

        return false
    }

    fun getProtectionReason(packageName: String): String? {
        if (packageName == appContext.packageName) {
            return "Coconut cannot be snapped by itself."
        }
        if (packageName == getDefaultLauncherPackage()) {
            return "Active system launcher is protected to prevent device lock-out."
        }
        if (packageName in getEnabledKeyboardPackages()) {
            return "Active keyboard/IME is protected so text input remains available."
        }
        if (packageName in knownEssentialPackages) {
            return "Core Android operating system component is protected."
        }
        return null
    }

    private fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun getEnabledKeyboardPackages(): Set<String> {
        val imm = appContext.getSystemService(InputMethodManager::class.java) ?: return emptySet()
        return runCatching {
            imm.enabledInputMethodList.map { it.packageName }.toSet()
        }.getOrDefault(emptySet())
    }
}
