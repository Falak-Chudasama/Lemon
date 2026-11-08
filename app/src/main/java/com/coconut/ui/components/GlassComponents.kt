package com.coconut.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coconut.ui.LemonColors

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tonalElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) LemonColors.GlassDarkFill else LemonColors.GlassLightFill
    val borderColor = if (isDark) LemonColors.GlassDarkBorder else LemonColors.GlassLightBorder

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = tonalElevation
    ) {
        Box(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    onClick: (() -> Unit)? = null,
    tonalElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }

    val borderColor = if (isDark) {
        LemonColors.BorderDark.copy(alpha = 0.6f)
    } else {
        LemonColors.BorderLight
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(clickModifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = tonalElevation
    ) {
        Box(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun GlassStatusBadge(
    text: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                ) {
                    Surface(shape = CircleShape, color = statusColor) {}
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}
