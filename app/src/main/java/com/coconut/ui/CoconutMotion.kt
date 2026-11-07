package com.coconut.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object CoconutMotion {
    // Standard durations (ms)
    const val DURATION_MICRO: Int = 140
    const val DURATION_SHORT: Int = 180
    const val DURATION_MEDIUM: Int = 220
    const val DURATION_LONG: Int = 260

    // Standard easings
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardEasing = FastOutSlowInEasing
    val DecelerateEasing = LinearOutSlowInEasing

    // Standard animation specs
    val MicroInteraction: AnimationSpec<Float> = tween(
        durationMillis = DURATION_MICRO,
        easing = EmphasizedEasing
    )

    val StatusTransition: AnimationSpec<Float> = tween(
        durationMillis = DURATION_SHORT,
        easing = StandardEasing
    )

    val SpringSubtle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val CardExpand = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = EmphasizedEasing
    )

    val ScreenTransition = tween<Float>(
        durationMillis = DURATION_LONG,
        easing = StandardEasing
    )
}

