package com.coconut.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coconut.ui.LemonTheme

fun Modifier.frostedGlassBlur(radius: Float = 20f): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            renderEffect = RenderEffect.createBlurEffect(
                radius, radius, Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
    } else {
        this
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LemonTheme.shapes.card,
    tonalElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LemonTheme.colors.isDark
    val borderBrush = Brush.verticalGradient(
        if (isDark) listOf(Color(0x30FFFFFF), Color(0x0CFFFFFF))
        else listOf(Color(0x80FFFFFF), Color(0x30FFFFFF))
    )

    Surface(
        modifier = modifier,
        shape = shape,
        color = LemonTheme.colors.navBackground.copy(alpha = if (isDark) 0.80f else 0.88f),
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = tonalElevation
    ) {
        Box(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = LemonTheme.shapes.card,
    onClick: (() -> Unit)? = null,
    tonalElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LemonTheme.colors.isDark
    val borderBrush = Brush.verticalGradient(
        if (isDark) listOf(Color(0x33FFFFFF), Color(0x10FFFFFF))
        else listOf(Color(0x90FFFFFF), Color(0x35FFFFFF))
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(clickModifier),
        shape = shape,
        color = LemonTheme.colors.cardBackground.copy(alpha = if (isDark) 0.70f else 0.90f),
        border = BorderStroke(1.dp, borderBrush),
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
    LemonBadge(
        text = text,
        customColor = statusColor,
        showDot = showDot,
        modifier = modifier
    )
}
