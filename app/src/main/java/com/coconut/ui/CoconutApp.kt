package com.coconut.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.coconut.R
import com.coconut.security.BiometricAuthHelper
import com.coconut.ui.components.EmergencyRecoveryDialog
import com.coconut.ui.components.SetupWizardDialog
import com.coconut.ui.components.SnapConfirmationDialog
import com.coconut.ui.components.SnappedBlockedDialog
import com.coconut.ui.screens.ContentShieldScreen
import com.coconut.ui.screens.HomeScreen
import com.coconut.ui.screens.SettingsScreen
import com.coconut.ui.screens.SnapScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoconutApp(
    viewModel: MainViewModel,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onRequestDeviceAdmin: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? FragmentActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_coconut_logo),
                            contentDescription = "Coconut Logo",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Coconut",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                    unselectedTextColor = MaterialTheme.colorScheme.outline
                )

                NavigationBarItem(
                    selected = state.selectedTab == NavigationTab.HOME,
                    onClick = { viewModel.selectTab(NavigationTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = state.selectedTab == NavigationTab.THE_SNAP,
                    onClick = { viewModel.selectTab(NavigationTab.THE_SNAP) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "The Snap") },
                    label = { Text("The Snap", style = MaterialTheme.typography.labelSmall) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = state.selectedTab == NavigationTab.CONTENT_SHIELD,
                    onClick = { viewModel.selectTab(NavigationTab.CONTENT_SHIELD) },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Content Shield") },
                    label = { Text("Shield", style = MaterialTheme.typography.labelSmall) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = state.selectedTab == NavigationTab.SETTINGS,
                    onClick = { viewModel.selectTab(NavigationTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                    colors = navColors
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Subtle error banner
                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    state.errorMessage?.let { error ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::clearMessages, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Subtle success banner
                AnimatedVisibility(
                    visible = state.successMessage != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    state.successMessage?.let { success ->
                        Surface(
                            color = CoconutColors.ProtectedGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CoconutColors.ProtectedGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = success,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = CoconutColors.ProtectedGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::clearMessages, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Screen Tabs
                when (state.selectedTab) {
                    NavigationTab.HOME -> HomeScreen(
                        state = state,
                        onNavigateTab = viewModel::selectTab,
                        onReconcileNow = viewModel::reconcileNow,
                        onOpenSetup = viewModel::openSetupDialog
                    )
                    NavigationTab.THE_SNAP -> SnapScreen(
                        state = state,
                        onSearchChange = viewModel::setSearchQuery,
                        onCategoryChange = viewModel::setSelectedCategory,
                        onTabChange = viewModel::setShowRegistryTab,
                        onSnapRequest = { app ->
                            if (activity != null && BiometricAuthHelper.hasBiometricOrCredential(activity)) {
                                BiometricAuthHelper.authenticate(
                                    activity = activity,
                                    title = "Snap ${app.applicationName}",
                                    subtitle = "Confirm fingerprint or PIN to permanently snap",
                                    onSuccess = { viewModel.executeSnap(app) },
                                    onError = { errorMsg ->
                                        viewModel.showErrorMessage("Snap authentication failed: $errorMsg")
                                    }
                                )
                            } else {
                                viewModel.requestSnap(app)
                            }
                        },
                        onShowBlockedDialog = { app ->
                            viewModel.showBlockedDialog(app)
                        }
                    )
                    NavigationTab.CONTENT_SHIELD -> ContentShieldScreen(
                        state = state,
                        onToggleShield = viewModel::toggleContentShield,
                        onSelectProvider = viewModel::selectDnsProvider,
                        onTestDns = viewModel::testDnsConnectivity
                    )
                    NavigationTab.SETTINGS -> SettingsScreen(
                        state = state,
                        onToggleRestriction = viewModel::setHardeningRestriction,
                        onOpenEmergencyRecovery = viewModel::openEmergencyRecoveryDialog
                    )
                }
            }

            // Setup & Permissions Wizard Dialog
            if (state.showSetupDialog) {
                SetupWizardDialog(
                    state = state,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestDeviceAdmin = onRequestDeviceAdmin,
                    onDismiss = viewModel::dismissSetupDialog
                )
            }

            // Confirmation Dialog for The Snap
            state.activeSnapTarget?.let { app ->
                SnapConfirmationDialog(
                    appName = app.applicationName,
                    packageName = app.packageName,
                    onConfirm = { viewModel.executeSnap(app) },
                    onDismiss = viewModel::dismissSnapRequest
                )
            }

            // Snapped Blocked Dialog
            state.activeBlockedTarget?.let { app ->
                SnappedBlockedDialog(
                    appName = app.applicationName,
                    packageName = app.packageName,
                    onDismiss = viewModel::dismissBlockedDialog
                )
            }

            // Exceptional Emergency Recovery Dialog
            if (state.showRecoveryDialog) {
                val target = state.recoveryTargetApp
                if (target != null) {
                    EmergencyRecoveryDialog(
                        appName = target.applicationName,
                        packageName = target.packageName,
                        onConfirmRecovery = {
                            if (activity != null && BiometricAuthHelper.hasBiometricOrCredential(activity)) {
                                BiometricAuthHelper.authenticate(
                                    activity = activity,
                                    title = "Authorize Recovery for ${target.applicationName}",
                                    subtitle = "Emergency unsuspension authorization",
                                    onSuccess = { viewModel.executeEmergencyRecovery(target.packageName) },
                                    onError = { viewModel.showErrorMessage("Authorization failed: $it") }
                                )
                            } else {
                                viewModel.executeEmergencyRecovery(target.packageName)
                            }
                        },
                        onDismiss = viewModel::dismissEmergencyRecoveryDialog
                    )
                } else {
                    viewModel.showErrorMessage("No snapped applications to recover.")
                    viewModel.dismissEmergencyRecoveryDialog()
                }
            }
        }
    }
}
