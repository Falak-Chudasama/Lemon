package com.coconut.policy

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

enum class ManagementState {
    NOT_ACTIVATED,
    DEVICE_ADMIN_ONLY,
    DEVICE_OWNER,
    UNSUPPORTED_OR_RESTRICTED,
    PROVISIONING_INCOMPLETE
}

data class PolicyCapabilities(
    val managementState: ManagementState,
    val isDeviceAdmin: Boolean,
    val isDeviceOwner: Boolean,
    val canSuspendPackages: Boolean,
    val canBlockUninstall: Boolean,
    val canConfigurePrivateDns: Boolean,
    val canApplyUserRestrictions: Boolean,
    val receiverComponentValid: Boolean,
    val statusDescription: String
)

class DeviceCapabilityManager(context: Context) {
    private val appContext = context.applicationContext
    private val dpm = appContext.getSystemService(DevicePolicyManager::class.java)
    val adminComponent = ComponentName(appContext, CoconutDeviceAdminReceiver::class.java)

    fun evaluateCapabilities(): PolicyCapabilities {
        val receiverValid = isReceiverComponentValid()
        val isDo = isDeviceOwner()
        val isAdmin = isDeviceAdmin()

        val managementState = when {
            isDo -> ManagementState.DEVICE_OWNER
            isAdmin -> ManagementState.DEVICE_ADMIN_ONLY
            !receiverValid -> ManagementState.UNSUPPORTED_OR_RESTRICTED
            else -> ManagementState.NOT_ACTIVATED
        }

        val canSuspend = isDo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val canBlockUninstall = isDo
        val canSetDns = isDo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val canSetRestrictions = isDo

        val description = when (managementState) {
            ManagementState.DEVICE_OWNER -> "Device Owner provisioned. Full OS-level suspension and Private DNS management active."
            ManagementState.DEVICE_ADMIN_ONLY -> "Device Administrator active. NOTE: Ordinary Device Admin does NOT possess package suspension or Private DNS management privileges. ADB Device Owner provisioning required."
            ManagementState.NOT_ACTIVATED -> "Coconut is not yet activated as an administrator or Device Owner."
            ManagementState.UNSUPPORTED_OR_RESTRICTED -> "Receiver component configuration invalid or restricted by system."
            ManagementState.PROVISIONING_INCOMPLETE -> "Device Owner provisioning incomplete."
        }

        return PolicyCapabilities(
            managementState = managementState,
            isDeviceAdmin = isAdmin,
            isDeviceOwner = isDo,
            canSuspendPackages = canSuspend,
            canBlockUninstall = canBlockUninstall,
            canConfigurePrivateDns = canSetDns,
            canApplyUserRestrictions = canSetRestrictions,
            receiverComponentValid = receiverValid,
            statusDescription = description
        )
    }

    fun isDeviceOwner(): Boolean {
        return runCatching {
            dpm != null && dpm.isDeviceOwnerApp(appContext.packageName)
        }.getOrDefault(false)
    }

    fun isDeviceAdmin(): Boolean {
        return runCatching {
            dpm != null && dpm.isAdminActive(adminComponent)
        }.getOrDefault(false)
    }

    private fun isReceiverComponentValid(): Boolean {
        return runCatching {
            val intent = Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED).apply {
                component = adminComponent
            }
            val receivers = appContext.packageManager.queryBroadcastReceivers(intent, 0)
            receivers.isNotEmpty()
        }.getOrDefault(false)
    }
}
