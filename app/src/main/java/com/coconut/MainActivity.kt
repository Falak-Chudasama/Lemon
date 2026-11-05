package com.coconut

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.coconut.policy.CoconutDeviceAdminReceiver
import com.coconut.ui.CoconutApp
import com.coconut.ui.CoconutTheme
import com.coconut.ui.MainViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val adminComponent by lazy {
        ComponentName(this, CoconutDeviceAdminReceiver::class.java)
    }

    private val dpm by lazy {
        getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refresh()
    }

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Immediately request notification permission if running on Android 13+ (API 33+)
        checkAndRequestNotificationPermission()

        // 2. Immediately request Device Admin if not active
        checkAndPromptDeviceAdmin()

        setContent {
            CoconutTheme {
                CoconutApp(
                    viewModel = viewModel,
                    hasNotificationPermission = hasNotificationPermission(),
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onRequestDeviceAdmin = ::requestDeviceAdmin
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkAndPromptDeviceAdmin() {
        if (!dpm.isAdminActive(adminComponent)) {
            requestDeviceAdmin()
        }
    }

    fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate Coconut device administration to enable application blacklisting and policy protection."
            )
        }
        deviceAdminLauncher.launch(intent)
    }
}
