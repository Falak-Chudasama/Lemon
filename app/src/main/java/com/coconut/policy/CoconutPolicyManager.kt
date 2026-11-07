package com.coconut.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager
import com.coconut.core.SafePackageHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface PolicyResult {
    data object AppliedAndVerified : PolicyResult
    data object SavedButNotEnforced : PolicyResult
    data class AwaitingDeviceOwner(val message: String = "Coconut requires Device Owner provisioning for hardware-level enforcement.") : PolicyResult
    data class PartiallyApplied(val details: String) : PolicyResult
    data class Unsupported(val reason: String) : PolicyResult
    data class Failed(val reason: String, val throwable: Throwable? = null) : PolicyResult
    data class RecoveryRequired(val reason: String) : PolicyResult

    val isSuccess: Boolean
        get() = this is AppliedAndVerified

    val errorMessage: String?
        get() = when (this) {
            is Failed -> reason
            is Unsupported -> reason
            is AwaitingDeviceOwner -> message
            is RecoveryRequired -> reason
            is PartiallyApplied -> details
            else -> null
        }
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
    private val capabilityManager = DeviceCapabilityManager(appContext)
    private val safePackageHelper = SafePackageHelper(appContext)
    private val policyMutex = Mutex()

    fun isDeviceOwner(): Boolean = capabilityManager.isDeviceOwner()

    fun isDeviceAdmin(): Boolean = capabilityManager.isDeviceAdmin()

    // --- FEATURE 1: THE SNAP (PERMANENT SUSPENSION & UNINSTALL BLOCKING) ---

    suspend fun suspendAndProtectPackage(packageName: String): PolicyResult = policyMutex.withLock {
        if (!isDeviceOwner()) {
            return PolicyResult.AwaitingDeviceOwner("Device Owner privileges are required to permanently Snap applications.")
        }

        if (safePackageHelper.isProtectedPackage(packageName)) {
            val reason = safePackageHelper.getProtectionReason(packageName) ?: "System-protected package."
            return PolicyResult.Unsupported("Cannot snap protected package: $reason")
        }

        return runCatching {
            val pm = appContext.packageManager
            // Check package existence
            pm.getApplicationInfo(packageName, 0)

            // Step 1: OS-Level Suspension
            val failedPackages = dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
            if (failedPackages.isNotEmpty() && failedPackages.contains(packageName)) {
                return PolicyResult.Failed("Android system refused to suspend package: $packageName")
            }

            // Verify suspension
            val suspensionVerified = isPackageSuspended(packageName)
            if (!suspensionVerified) {
                return PolicyResult.Failed("Suspension verification failed for $packageName: application remained active.")
            }

            // Step 2: Uninstall Blocking on target application
            var uninstallBlockedVerified = false
            try {
                dpm.setUninstallBlocked(adminComponent, packageName, true)
                uninstallBlockedVerified = dpm.isUninstallBlocked(adminComponent, packageName)
            } catch (e: Exception) {
                // Some OEM variants or specific system packages may reject uninstall blocking
                uninstallBlockedVerified = false
            }

            if (suspensionVerified && uninstallBlockedVerified) {
                PolicyResult.AppliedAndVerified
            } else if (suspensionVerified) {
                PolicyResult.PartiallyApplied("Application suspended successfully, but uninstall blocking was not supported.")
            } else {
                PolicyResult.Failed("Failed to enforce permanent snap.")
            }
        }.getOrElse { error ->
            PolicyResult.Failed(error.message ?: "Suspension operation failed with exception.", error)
        }
    }

    suspend fun emergencyRecoveryUnsnap(packageName: String): PolicyResult = policyMutex.withLock {
        if (!isDeviceOwner()) {
            return PolicyResult.AwaitingDeviceOwner("Device Owner privileges are required for emergency recovery.")
        }

        return runCatching {
            // Step 1: Remove uninstall block
            runCatching {
                dpm.setUninstallBlocked(adminComponent, packageName, false)
            }

            // Step 2: Unsuspend package
            val failedPackages = dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
            if (failedPackages.isNotEmpty() && failedPackages.contains(packageName)) {
                return PolicyResult.Failed("Android refused to release suspension for package: $packageName")
            }

            // Verify unsuspension
            val stillSuspended = isPackageSuspended(packageName)
            val stillUninstallBlocked = runCatching { dpm.isUninstallBlocked(adminComponent, packageName) }.getOrDefault(false)

            if (!stillSuspended && !stillUninstallBlocked) {
                PolicyResult.AppliedAndVerified
            } else {
                PolicyResult.Failed("Recovery verification failed: package remains restricted.")
            }
        }.getOrElse { error ->
            PolicyResult.Failed(error.message ?: "Recovery operation failed with exception.", error)
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

    fun isPackageUninstallBlocked(packageName: String): Boolean {
        if (!isDeviceOwner()) return false
        return runCatching {
            dpm.isUninstallBlocked(adminComponent, packageName)
        }.getOrDefault(false)
    }

    // --- FEATURE 2: CONTENT SHIELD (PRIVATE DNS WITH GUARANTEED ON & OFF) ---

    suspend fun enableContentShield(hostname: String): PolicyResult = policyMutex.withLock {
        if (!isDeviceOwner()) {
            return PolicyResult.AwaitingDeviceOwner("Device Owner privileges are required to lock Private DNS.")
        }

        if (hostname.isBlank() || !hostname.contains(".")) {
            return PolicyResult.Failed("Invalid DNS provider hostname: $hostname")
        }

        return runCatching {
            // Apply DNS-over-TLS specified host
            val status = dpm.setGlobalPrivateDnsModeSpecifiedHost(adminComponent, hostname)
            when (status) {
                DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR -> {
                    // Lock down Private DNS in Settings so system filtering cannot be casually disabled
                    dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)

                    // Verify active configuration
                    val currentMode = dpm.getGlobalPrivateDnsMode(adminComponent)
                    val currentHost = dpm.getGlobalPrivateDnsHost(adminComponent)
                    val isConfigBlocked = userManager?.hasUserRestriction(UserManager.DISALLOW_CONFIG_PRIVATE_DNS) ?: false

                    val modeMatch = currentMode == DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME
                    val hostMatch = currentHost?.equals(hostname, ignoreCase = true) == true

                    if (modeMatch && hostMatch && isConfigBlocked) {
                        PolicyResult.AppliedAndVerified
                    } else if (modeMatch && hostMatch) {
                        PolicyResult.PartiallyApplied("Private DNS hostname applied, but configuration lock pending.")
                    } else {
                        PolicyResult.Failed("Private DNS configuration applied, but active host mismatch ($currentHost != $hostname).")
                    }
                }
                DevicePolicyManager.PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING -> {
                    PolicyResult.Failed("Provider host '$hostname' is not currently serving DNS-over-TLS requests on port 853.")
                }
                DevicePolicyManager.PRIVATE_DNS_SET_ERROR_FAILURE_SETTING -> {
                    PolicyResult.Failed("Android system failure setting specified Private DNS host.")
                }
                else -> {
                    PolicyResult.Failed("Unexpected Private DNS return code: $status")
                }
            }
        }.getOrElse { error ->
            PolicyResult.Failed(error.message ?: "Failed to set Private DNS configuration.", error)
        }
    }

    suspend fun disableContentShield(): PolicyResult = policyMutex.withLock {
        if (!isDeviceOwner()) {
            return PolicyResult.AwaitingDeviceOwner("Device Owner privileges are required to modify system DNS.")
        }

        return runCatching {
            // Step 1: Remove user restriction on Private DNS settings
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)

            // Step 2: Switch Private DNS mode back to Opportunistic (Android default automatic mode)
            val status = dpm.setGlobalPrivateDnsModeOpportunistic(adminComponent)

            // Step 3: Verify the DNS mode is no longer enforcing Coconut's provider hostname
            val activeMode = dpm.getGlobalPrivateDnsMode(adminComponent)
            val activeHost = dpm.getGlobalPrivateDnsHost(adminComponent)
            val isConfigStillBlocked = userManager?.hasUserRestriction(UserManager.DISALLOW_CONFIG_PRIVATE_DNS) ?: false

            val hostCleared = activeHost.isNullOrBlank() || activeMode != DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME
            val restrictionCleared = !isConfigStillBlocked

            if (hostCleared && restrictionCleared) {
                PolicyResult.AppliedAndVerified
            } else {
                PolicyResult.Failed("Failed to verify Content Shield release: mode=$activeMode, host=$activeHost, configBlocked=$isConfigStillBlocked")
            }
        }.getOrElse { error ->
            PolicyResult.Failed(error.message ?: "Failed to restore standard DNS mode.", error)
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

    // --- FEATURE 3: SAFE SELF-PROTECTION RELEASE ---

    suspend fun releaseSelfProtectionIfActive(): PolicyResult = policyMutex.withLock {
        if (!isDeviceOwner()) return PolicyResult.AppliedAndVerified

        return runCatching {
            val isBlocked = dpm.isUninstallBlocked(adminComponent, appContext.packageName)
            if (isBlocked) {
                dpm.setUninstallBlocked(adminComponent, appContext.packageName, false)
                val stillBlocked = dpm.isUninstallBlocked(adminComponent, appContext.packageName)
                if (!stillBlocked) {
                    PolicyResult.AppliedAndVerified
                } else {
                    PolicyResult.Failed("Could not release self-protection uninstall lock.")
                }
            } else {
                PolicyResult.AppliedAndVerified
            }
        }.getOrElse {
            PolicyResult.Failed(it.message ?: "Failed to inspect or release self-protection", it)
        }
    }

    suspend fun setSelfProtection(enabled: Boolean): PolicyResult = policyMutex.withLock {
        if (!isDeviceOwner()) {
            return PolicyResult.AwaitingDeviceOwner("Coconut is not provisioned as Device Owner.")
        }

        return runCatching {
            dpm.setUninstallBlocked(adminComponent, appContext.packageName, enabled)
            val verified = isSelfProtectionActive()
            if (verified == enabled) {
                PolicyResult.AppliedAndVerified
            } else {
                PolicyResult.Failed("Failed to verify self-protection state: expected $enabled, got $verified.")
            }
        }.getOrElse { error ->
            PolicyResult.Failed(error.message ?: "Failed to set uninstall block state.", error)
        }
    }

    fun isSelfProtectionActive(): Boolean {
        if (!isDeviceOwner()) return false
        return runCatching {
            dpm.isUninstallBlocked(adminComponent, appContext.packageName)
        }.getOrDefault(false)
    }

    // --- FEATURE 4: OPTIONAL USER RESTRICTIONS ---

    suspend fun setUserRestriction(restriction: String, enabled: Boolean): PolicyResult = policyMutex.withLock {
        if (!isDeviceOwner()) {
            return PolicyResult.AwaitingDeviceOwner("Coconut is not provisioned as Device Owner.")
        }

        return runCatching {
            if (enabled) {
                dpm.addUserRestriction(adminComponent, restriction)
            } else {
                dpm.clearUserRestriction(adminComponent, restriction)
            }
            val active = hasUserRestriction(restriction)
            if (active == enabled) {
                PolicyResult.AppliedAndVerified
            } else {
                PolicyResult.Failed("Verification failed for restriction $restriction (expected: $enabled, actual: $active)")
            }
        }.getOrElse { error ->
            PolicyResult.Failed(error.message ?: "Failed to update user restriction $restriction", error)
        }
    }

    fun hasUserRestriction(restriction: String): Boolean {
        return runCatching {
            userManager?.hasUserRestriction(restriction) ?: false
        }.getOrDefault(false)
    }
}
