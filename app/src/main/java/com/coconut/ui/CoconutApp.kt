package com.coconut.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.coconut.R
import com.coconut.security.BiometricHelper
import com.coconut.ui.components.FloatingBottomBar
import com.coconut.ui.components.LemonBannerType
import com.coconut.ui.components.LemonFeedbackBanner
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
    LaunchedEffect(state.bannerMessage, state.errorMessage) {
        if (state.bannerMessage != null || state.errorMessage != null) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LemonTheme.colors.pageBackground)
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

        // Floating Bottom Navigation Bar overlaid at bottom
        FloatingBottomBar(
            selectedTab = state.selectedTab,
            onTabSelected = viewModel::selectTab,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Top Status / Banner message
        val hasError = state.errorMessage != null
        val bannerText = state.errorMessage ?: state.bannerMessage ?: ""
        LemonFeedbackBanner(
            message = bannerText,
            visible = bannerText.isNotEmpty(),
            type = if (hasError) LemonBannerType.ERROR else LemonBannerType.INFO,
            onDismiss = viewModel::clearBanner,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun AppLockScreen(onUnlockRequested: () -> Unit) {
    // Prompt biometric authentication on initial display
    LaunchedEffect(Unit) {
        onUnlockRequested()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LemonTheme.colors.pageBackground
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
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Lemon Locked",
                style = LemonTheme.typography.displaySmall.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = LemonTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authentication required to open Lemon",
                style = LemonTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp
                ),
                color = LemonTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUnlockRequested,
                shape = LemonTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LemonTheme.colors.primaryAccent,
                    contentColor = LemonTheme.colors.onPrimaryAccent
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock Lemon",
                    style = LemonTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
