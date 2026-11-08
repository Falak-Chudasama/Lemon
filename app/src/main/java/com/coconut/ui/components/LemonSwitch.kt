package com.coconut.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coconut.ui.LemonTheme

/**
 * Premium iOS-styled capsule switch component.
 * Exact dimensions: 51dp width x 31dp height track, 27dp circular thumb, 2dp inset margin.
 * Smooth physics-based spring animation with tactile haptic feedback.
 */
@Composable
fun LemonSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> (if (checked) LemonTheme.colors.switchTrackOn else LemonTheme.colors.switchTrackOff).copy(alpha = 0.45f)
            checked -> LemonTheme.colors.switchTrackOn
            else -> LemonTheme.colors.switchTrackOff
        },
        animationSpec = tween(durationMillis = 180),
        label = "LemonSwitchTrackColor"
    )

    // Track width: 51dp, thumb size: 27dp, margin: 2dp -> travel distance = 51 - 27 - 2 = 22dp
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "LemonSwitchThumbOffset"
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { newValue ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange?.invoke(newValue)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Track: 51x31dp capsule
        Box(
            modifier = Modifier
                .size(width = 51.dp, height = 31.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(trackColor)
        ) {
            // Thumb: 27dp circle with elevation
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset, y = 2.dp)
                    .size(27.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(LemonTheme.colors.switchThumb)
            )
        }
    }
}

