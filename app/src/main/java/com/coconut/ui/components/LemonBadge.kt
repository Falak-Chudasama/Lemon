package com.coconut.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coconut.ui.LemonTheme

enum class LemonBadgeStyle {
    SUCCESS,
    WARNING,
    DANGER,
    INFO,
    INACTIVE,
    SCHEDULED
}

/**
 * Fruit Family semantic badge with color-matched indicator dot and high-contrast typography.
 * Strictly guarantees single-line text without wrapping or clipping.
 */
@Composable
fun LemonBadge(
    text: String,
    style: LemonBadgeStyle = LemonBadgeStyle.INFO,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
    customColor: Color? = null
) {
    val (foregroundColor, backgroundColor, borderColor) = if (customColor != null) {
        Triple(customColor, customColor.copy(alpha = 0.12f), customColor.copy(alpha = 0.24f))
    } else {
        when (style) {
            LemonBadgeStyle.SUCCESS -> Triple(
                LemonTheme.colors.success,
                LemonTheme.colors.successContainer,
                LemonTheme.colors.success.copy(alpha = 0.22f)
            )
            LemonBadgeStyle.WARNING -> Triple(
                LemonTheme.colors.warning,
                LemonTheme.colors.warningContainer,
                LemonTheme.colors.warning.copy(alpha = 0.25f)
            )
            LemonBadgeStyle.DANGER -> Triple(
                LemonTheme.colors.danger,
                LemonTheme.colors.dangerContainer,
                LemonTheme.colors.danger.copy(alpha = 0.25f)
            )
            LemonBadgeStyle.INFO -> Triple(
                LemonTheme.colors.info,
                LemonTheme.colors.infoContainer,
                LemonTheme.colors.info.copy(alpha = 0.22f)
            )
            LemonBadgeStyle.INACTIVE -> Triple(
                LemonTheme.colors.inactive,
                LemonTheme.colors.inactiveContainer,
                LemonTheme.colors.cardBorder
            )
            LemonBadgeStyle.SCHEDULED -> Triple(
                LemonTheme.colors.brandAmber,
                LemonTheme.colors.warningContainer,
                LemonTheme.colors.warning.copy(alpha = 0.25f)
            )
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(foregroundColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = LemonTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = foregroundColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
