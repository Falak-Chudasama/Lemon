package com.coconut.ui

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.launch

@Composable
fun CoconutApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    // Auto-dismiss transient banner after 4 seconds
    LaunchedEffect(state.bannerMessage, state.errorMessage) {
        if (state.bannerMessage != null || state.errorMessage != null) {
            delay(4000)
            viewModel.clearBanner()
        }
    }

    // In-app Lock Screen Gate: Blocks content whenever app leaves foreground
    if (state.isAppLockEnabled && !state.isSessionUnlocked) {
        AppLockScreen(
            onUnlockSuccess = { viewModel.unlockSession() }
        )
        return
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LemonTheme.colors.pageBackground)
    ) {
        // True Horizontal Pager navigation: Snap (0), DNS (1), Settings (2)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> SnapScreen(viewModel = viewModel)
                1 -> DnsScreen(viewModel = viewModel)
                2 -> SettingsScreen(viewModel = viewModel)
            }
        }

        // Floating Bottom Navigation Bar with shared moving indicator
        FloatingBottomBar(
            pagerState = pagerState,
            onTabSelected = { targetPage ->
                scope.launch {
                    pagerState.animateScrollToPage(targetPage)
                }
            },
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
private fun AppLockScreen(onUnlockSuccess: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var authErrorText by remember { mutableStateOf<String?>(null) }

    val biometricStatus = remember { BiometricHelper.canAuthenticateStrong(context) }

    fun triggerStrongBiometric() {
        if (activity == null) return
        authErrorText = null
        BiometricHelper.authenticateStrong(
            activity = activity,
            title = "Unlock Lemon",
            subtitle = "Touch the fingerprint sensor to continue",
            onSuccess = onUnlockSuccess,
            onError = { code, errString ->
                if (code != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    authErrorText = errString
                }
            }
        )
    }

    // Auto-prompt strong biometric when lock screen first appears
    LaunchedEffect(Unit) {
        if (biometricStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            triggerStrongBiometric()
        }
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
                modifier = Modifier.size(76.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Lemon Locked",
                style = LemonTheme.typography.displaySmall.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = LemonTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (biometricStatus) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    Text(
                        text = "Touch the fingerprint sensor to unlock Lemon",
                        style = LemonTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = LemonTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { triggerStrongBiometric() },
                        shape = LemonTheme.shapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LemonTheme.colors.primaryAccent,
                            contentColor = LemonTheme.colors.onPrimaryAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Unlock with Fingerprint",
                            style = LemonTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Text(
                        text = "No fingerprint is enrolled on this device. Strong biometrics are required to unlock Lemon.",
                        style = LemonTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = LemonTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        shape = LemonTheme.shapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LemonTheme.colors.primaryAccent,
                            contentColor = LemonTheme.colors.onPrimaryAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Open Security Settings",
                            style = LemonTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Strong biometric hardware is unavailable or disabled.",
                        style = LemonTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = LemonTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (authErrorText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = authErrorText!!,
                    style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = LemonTheme.colors.danger,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
