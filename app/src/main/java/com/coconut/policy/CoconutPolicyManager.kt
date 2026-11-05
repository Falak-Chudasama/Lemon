package com.coconut.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager

sealed interface PolicyResult {
    data object Success : PolicyResult
    data class Failure(val reason: String) : PolicyResult
}

data class PrivateDnsStatus(
    val mode: Int,
    val modeName: String,
    val host: String?,
    val isEnforced: Boolean,
    val isConfigBlocked: Boolean
)

class CoconutPolicyManager(context: Context) {
    private val appContext = context.applicationContext
    private val dpm = appContext.getSystemService(DevicePolicyManager::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    val adminComponent = ComponentName(appContext, CoconutDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean {
        return runCatching { dpm.isDeviceOwnerApp(appContext.packageName) }.getOrDefault(false)
    }

    fun isDeviceAdmin(): Boolean {
        return runCatching { dpm.isAdminActive(adminComponent) }.getOrDefault(false)
    }

    // --- FEATURE 1: THE SNAP (SUSPENSION) ---

    fun suspendPackage(packageName: String): PolicyResult {
        if (!isDeviceOwner()) {
            return PolicyResult.Failure("Coconut is not provisioned as Device Owner.")
        }

        return runCatching {
            val failedPackages = dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
            if (failedPackages.isNotEmpty() && failedPackages.contains(packageName)) {
                return PolicyResult.Failure("Android refused to suspend package: $packageName")
            }

            // Verify enforcement state
            val verified = isPackageSuspended(packageName)
            if (verified) {
                PolicyResult.Success
            } else {
                PolicyResult.Failure("Suspension verification failed for $packageName: package remained active.")
            }
        }.getOrElse { error ->
            PolicyResult.Failure(error.message ?: "Suspension operation failed with exception.")
        }
    }

    fun unsuspendPackage(packageName: String): PolicyResult {
        if (!isDeviceOwner()) {
            return PolicyResult.Failure("Coconut is not provisioned as Device Owner.")
        }

        return runCatching {
            val failedPackages = dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
            if (failedPackages.isNotEmpty() && failedPackages.contains(packageName)) {
                return PolicyResult.Failure("Android refused to restore package: $packageName")
            }

            // Verify unsuspension
            val verified = !isPackageSuspended(packageName)
            if (verified) {
                PolicyResult.Success
            } else {
                PolicyResult.Failure("Unsuspension verification failed: $packageName is still suspended.")
            }
        }.getOrElse { error ->
            PolicyResult.Failure(error.message ?: "Unsuspension operation failed with exception.")
        }
    }

    fun isPackageSuspended(packageName: String): Boolean {
        if (!isDeviceOwner()) return false
        return runCatching {
            dpm.isPackageSuspended(adminComponent, packageName)
        }.getOrElse {
            runCatching {
                val pm = appContext.packageManager
                pm.isPackageSuspended(packageName)
            }.getOrDefault(false)
        }
    }

    // --- FEATURE 2: CONTENT SHIELD (PRIVATE DNS) ---

    fun enableContentShield(hostname: String): PolicyResult {
        if (!isDeviceOwner()) {
            return PolicyResult.Failure("Coconut is not provisioned as Device Owner.")
        }

        return runCatching {
            // Apply DNS-over-TLS specified host
            val status = dpm.setGlobalPrivateDnsModeSpecifiedHost(adminComponent, hostname)
            when (status) {
                DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR -> {
                    // Lock down Private DNS in Settings so user cannot modify it
                    dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)

                    // Verify active configuration
                    val currentHost = dpm.getGlobalPrivateDnsHost(adminComponent)
                    if (currentHost.equals(hostname, ignoreCase = true)) {
                        PolicyResult.Success
                    } else {
                        PolicyResult.Failure("Private DNS configuration applied, but active host mismatch ($currentHost != $hostname).")
                    }
                }
                DevicePolicyManager.PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING -> {
                    PolicyResult.Failure("Provider host '$hostname' is not currently serving DNS-over-TLS requests.")
                }
                DevicePolicyManager.PRIVATE_DNS_SET_ERROR_FAILURE_SETTING -> {
                    PolicyResult.Failure("Android system failure setting specified Private DNS host.")
                }
                else -> {
                    PolicyResult.Failure("Unexpected Private DNS return code: $status")
                }
            }
        }.getOrElse { error ->
            PolicyResult.Failure(error.message ?: "Failed to set Private DNS configuration.")
        }
    }

    fun disableContentShield(): PolicyResult {
        if (!isDeviceOwner()) {
            return PolicyResult.Failure("Coconut is not provisioned as Device Owner.")
        }

        return runCatching {
            // Allow user modification in Settings before clearing
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)

            // Switch to opportunistic or off
            dpm.setGlobalPrivateDnsModeOpportunistic(adminComponent)
            PolicyResult.Success
        }.getOrElse { error ->
            PolicyResult.Failure(error.message ?: "Failed to restore standard DNS mode.")
        }
    }

    fun getPrivateDnsStatus(): PrivateDnsStatus {
        if (!isDeviceOwner()) {
            return PrivateDnsStatus(
                mode = -1,
                modeName = "UNAVAILABLE (NOT DEVICE OWNER)",
                host = null,
                isEnforced = false,
                isConfigBlocked = false
            )
        }

        return runCatching {
            val mode = dpm.getGlobalPrivateDnsMode(adminComponent)
            val host = dpm.getGlobalPrivateDnsHost(adminComponent)
            val isConfigBlocked = userManager?.hasUserRestriction(UserManager.DISALLOW_CONFIG_PRIVATE_DNS) ?: false

            val modeName = when (mode) {
                DevicePolicyManager.PRIVATE_DNS_MODE_OFF -> "OFF"
                DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC -> "OPPORTUNISTIC"
                DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME -> "SPECIFIED HOST"
                else -> "UNKNOWN ($mode)"
            }

            PrivateDnsStatus(
                mode = mode,
                modeName = modeName,
                host = host,
                isEnforced = mode == DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME && !host.isNullOrBlank(),
                isConfigBlocked = isConfigBlocked
            )
        }.getOrElse {
            PrivateDnsStatus(
                mode = -1,
                modeName = "ERROR: ${it.message}",
                host = null,
                isEnforced = false,
                isConfigBlocked = false
            )
        }
    }

    // --- FEATURE 3: COCONUT SELF-PROTECTION ---

    fun setSelfProtection(enabled: Boolean): PolicyResult {
        if (!isDeviceOwner()) {
            return PolicyResult.Failure("Coconut is not provisioned as Device Owner.")
        }

        return runCatching {
            dpm.setUninstallBlocked(adminComponent, appContext.packageName, enabled)
            val verified = isSelfProtectionActive()
            if (verified == enabled) {
                PolicyResult.Success
            } else {
                PolicyResult.Failure("Failed to verify self-protection state: expected $enabled, got $verified.")
            }
        }.getOrElse { error ->
            PolicyResult.Failure(error.message ?: "Failed to set uninstall block state.")
        }
    }

    fun isSelfProtectionActive(): Boolean {
        if (!isDeviceOwner()) return false
        return runCatching {
            dpm.isUninstallBlocked(adminComponent, appContext.packageName)
        }.getOrDefault(false)
    }

    // --- FEATURE 6: DEVICE HARDENING RESTRICTIONS ---

    fun setUserRestriction(restriction: String, enabled: Boolean): PolicyResult {
        if (!isDeviceOwner()) {
            return PolicyResult.Failure("Coconut is not provisioned as Device Owner.")
        }

        return runCatching {
            if (enabled) {
                dpm.addUserRestriction(adminComponent, restriction)
            } else {
                dpm.clearUserRestriction(adminComponent, restriction)
            }
            val active = hasUserRestriction(restriction)
            if (active == enabled) {
                PolicyResult.Success
            } else {
                PolicyResult.Failure("Verification failed for restriction $restriction (expected: $enabled, actual: $active)")
            }
        }.getOrElse { error ->
            PolicyResult.Failure(error.message ?: "Failed to update user restriction $restriction")
        }
    }

    fun hasUserRestriction(restriction: String): Boolean {
        return runCatching {
            userManager?.hasUserRestriction(restriction) ?: false
        }.getOrDefault(false)
    }
}

