package com.coconut.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coconut.ui.LemonTheme

enum class LemonBannerType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

/**
 * Top floating feedback toast banner with clear typography, status icon, and 48dp accessible dismiss target.
 */
@Composable
fun LemonFeedbackBanner(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    type: LemonBannerType = LemonBannerType.INFO,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        val (icon, iconTint, containerColor, textColor, borderColor) = when (type) {
            LemonBannerType.SUCCESS -> Quintuple(
                Icons.Default.CheckCircle,
                LemonTheme.colors.success,
                LemonTheme.colors.successContainer,
                LemonTheme.colors.onSuccessContainer,
                LemonTheme.colors.success.copy(alpha = 0.25f)
            )
            LemonBannerType.WARNING -> Quintuple(
                Icons.Default.Warning,
                LemonTheme.colors.warning,
                LemonTheme.colors.warningContainer,
                LemonTheme.colors.onWarningContainer,
                LemonTheme.colors.warning.copy(alpha = 0.25f)
            )
            LemonBannerType.ERROR -> Quintuple(
                Icons.Default.Error,
                LemonTheme.colors.danger,
                LemonTheme.colors.dangerContainer,
                LemonTheme.colors.onDangerContainer,
                LemonTheme.colors.danger.copy(alpha = 0.25f)
            )
            LemonBannerType.INFO -> Quintuple(
                Icons.Default.Info,
                LemonTheme.colors.info,
                LemonTheme.colors.infoContainer,
                LemonTheme.colors.onInfoContainer,
                LemonTheme.colors.info.copy(alpha = 0.25f)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = message,
                    style = LemonTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
