package com.coconut.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.coconut.R
import com.coconut.security.BiometricHelper
import com.coconut.ui.components.FloatingBottomBar
import com.coconut.ui.screens.DnsScreen
import com.coconut.ui.screens.SettingsScreen
import com.coconut.ui.screens.SnapScreen
import kotlinx.coroutines.delay

@Composable
fun CoconutApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Auto-dismiss transient banner after 4 seconds
    LaunchedEffect(state.bannerMessage) {
        if (state.bannerMessage != null) {
            delay(4000)
            viewModel.clearBanner()
        }
    }

    // In-app Lock Screen Gate
    if (state.isAppLockEnabled && !state.isSessionUnlocked) {
        AppLockScreen(
            onUnlockRequested = {
                if (activity != null) {
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = "Unlock Lemon",
                        subtitle = "Verify biometric or screen lock credential",
                        allowDeviceCredential = true,
                        onSuccess = { viewModel.unlockSession() },
                        onError = { /* Remain on lock screen */ }
                    )
                }
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            FloatingBottomBar(
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Horizontal screen transition between Snap, DNS, and Settings
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    val slideDirection = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(220),
                        initialOffsetX = { fullWidth -> fullWidth * slideDirection }
                    ) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(220),
                                targetOffsetX = { fullWidth -> -fullWidth * slideDirection }
                            ) + fadeOut(animationSpec = tween(220))
                        )
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    NavigationTab.SNAP -> SnapScreen(viewModel = viewModel)
                    NavigationTab.DNS -> DnsScreen(viewModel = viewModel)
                    NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }

            // Top Status / Banner message
            AnimatedVisibility(
                visible = state.bannerMessage != null || state.errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp)
            ) {
                val isError = state.errorMessage != null
                val messageText = state.errorMessage ?: state.bannerMessage ?: ""

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = messageText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = viewModel::clearBanner,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLockScreen(onUnlockRequested: () -> Unit) {
    // Prompt biometric authentication on display
    LaunchedEffect(Unit) {
        onUnlockRequested()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_lemon_mark),
                contentDescription = "Lemon",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Lemon Locked",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Authentication required",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUnlockRequested,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock Lemon", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
