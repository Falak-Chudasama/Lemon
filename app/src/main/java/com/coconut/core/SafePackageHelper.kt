package com.coconut.core

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

class SafePackageHelper(private val context: Context) {
    private val appContext = context.applicationContext
    private val pm = appContext.packageManager
    private val dpm = appContext.getSystemService(DevicePolicyManager::class.java)

    // Critical Android OS packages that must NEVER be snapped
    private val hardcodedCorePackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.gms",
        "com.android.vending", // Play Store needed for critical system security & updates
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.emergency",
        "com.google.android.emergency",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.cellbroadcastreceiver",
        "com.google.android.cellbroadcastreceiver",
        "com.android.providers.telephony",
        "com.android.providers.downloads",
        "com.android.documentsui",
        "com.google.android.documentsui",
        appContext.packageName // Coconut itself
    )

    // Samsung One UI critical packages
    private val samsungOneUiPackages = setOf(
        "com.sec.android.app.launcher",
        "com.sec.android.app.camera",
        "com.samsung.android.incallui",
        "com.samsung.android.emergency",
        "com.samsung.android.dialer",
        "com.samsung.android.messaging",
        "com.samsung.android.biometrics",
        "com.sec.android.app.myfiles",
        "com.samsung.android.app.telephonyui",
        "com.samsung.android.honeyboard", // Samsung Keyboard
        "com.sec.android.inputmethod"
    )

    fun isProtectedPackage(packageName: String): Boolean {
        // 1. Coconut itself is always protected
        if (packageName == appContext.packageName) return true

        // 2. Hardcoded core OS packages
        if (packageName in hardcodedCorePackages) return true

        // 3. Samsung One UI critical components
        if (packageName in samsungOneUiPackages) return true

        // 4. Active Home Launcher
        val launcher = getDefaultLauncherPackage()
        if (packageName == launcher) return true

        // 5. Active Keyboard / Input Methods (IME)
        val keyboardPackages = getEnabledKeyboardPackages()
        if (packageName in keyboardPackages) return true

        // 6. Default Dialer / Telephony
        val defaultDialer = getDefaultDialerPackage()
        if (packageName == defaultDialer) return true

        // 7. Default SMS Provider
        val defaultSms = getDefaultSmsPackage()
        if (packageName == defaultSms) return true

        // 8. Emergency Assistance packages
        val emergencyPackages = getEmergencyPackages()
        if (packageName in emergencyPackages) return true

        // 9. Active Device Administrators
        val activeAdmins = getActiveDeviceAdminPackages()
        if (packageName in activeAdmins) return true

        // 10. Required Package Installer / Uninstaller
        val installerPackages = getPackageInstallerPackages()
        if (packageName in installerPackages) return true

        // 11. Permission Controller
        val permissionController = getPermissionControllerPackage()
        if (packageName == permissionController) return true

        // 12. System UID apps (Process.SYSTEM_UID = 1000)
        if (isSystemUidPackage(packageName)) return true

        return false
    }

    fun getProtectionReason(packageName: String): String? {
        if (packageName == appContext.packageName) {
            return "Coconut cannot be snapped by itself."
        }
        if (packageName == getDefaultLauncherPackage()) {
            return "Active system launcher is protected to prevent lock-out from home screen."
        }
        if (packageName in getEnabledKeyboardPackages()) {
            return "Active keyboard/IME is protected so text input remains accessible."
        }
        if (packageName == getDefaultDialerPackage() || packageName in getEmergencyPackages()) {
            return "Emergency calling and dialer components are strictly protected for safety."
        }
        if (packageName in getActiveDeviceAdminPackages()) {
            return "Active device administrators cannot be suspended per Android OS security rules."
        }
        if (packageName in getPackageInstallerPackages()) {
            return "Package installer/verifier is protected to maintain system maintenance."
        }
        if (packageName == getPermissionControllerPackage()) {
            return "Android permission controller is a protected core security component."
        }
        if (packageName in samsungOneUiPackages) {
            return "Critical Samsung One UI framework component is protected."
        }
        if (packageName in hardcodedCorePackages || isSystemUidPackage(packageName)) {
            return "Core Android operating system component is protected."
        }
        return null
    }

    private fun getDefaultLauncherPackage(): String? {
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName
        }.getOrNull()
    }

    private fun getEnabledKeyboardPackages(): Set<String> {
        return runCatching {
            val imm = appContext.getSystemService(InputMethodManager::class.java) ?: return emptySet()
            imm.enabledInputMethodList.map { it.packageName }.toSet()
        }.getOrDefault(emptySet())
    }

    private fun getDefaultDialerPackage(): String? {
        return runCatching {
            val telecomManager = appContext.getSystemService(TelecomManager::class.java)
            telecomManager?.defaultDialerPackage
        }.getOrNull()
    }

    private fun getDefaultSmsPackage(): String? {
        return runCatching {
            Telephony.Sms.getDefaultSmsPackage(appContext)
        }.getOrNull()
    }

    private fun getEmergencyPackages(): Set<String> {
        val result = mutableSetOf("com.android.emergency", "com.google.android.emergency")
        runCatching {
            val emergencyIntent = Intent("android.telephony.action.EMERGENCY_ASSISTANCE")
            val resolves = pm.queryIntentActivities(emergencyIntent, 0)
            for (res in resolves) {
                res.activityInfo?.packageName?.let { result.add(it) }
            }
        }
        return result
    }

    private fun getActiveDeviceAdminPackages(): Set<String> {
        return runCatching {
            dpm?.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()
        }.getOrDefault(emptySet())
    }

    private fun getPackageInstallerPackages(): Set<String> {
        val result = mutableSetOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )
        runCatching {
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(Uri.parse("content://dummy/package.apk"), "application/vnd.android.package-archive")
            }
            pm.queryIntentActivities(installIntent, 0).forEach {
                it.activityInfo?.packageName?.let { pkg -> result.add(pkg) }
            }
        }
        return result
    }

    private fun getPermissionControllerPackage(): String? {
        return runCatching {
            val intent = Intent("android.permission.action.REVIEW_PERMISSIONS")
            val resolve = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolve?.activityInfo?.packageName ?: "com.google.android.permissioncontroller"
        }.getOrDefault("com.google.android.permissioncontroller")
    }

    private fun isSystemUidPackage(packageName: String): Boolean {
        return runCatching {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.uid == Process.SYSTEM_UID || appInfo.uid == Process.PHONE_UID
        }.getOrDefault(false)
    }
}
