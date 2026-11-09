package com.coconut.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coconut.ui.LemonTheme
import com.coconut.ui.NavigationTab
import kotlin.math.abs
import kotlin.math.roundToInt

// Electric blue brand accent for the iOS reference selected capsule
val ElectricBlueStart = Color(0xFF007AFF)
val ElectricBlueEnd = Color(0xFF0A58CA)
val ElectricBlueGlow = Color(0x66007AFF)

/**
 * Premium floating translucent capsule navigation bar.
 * Matches the reference design with:
 * - Floating translucent glass capsule background with thin border and soft layered shadow
 * - Shared moving electric-blue indicator capsule that glides continuously with Pager swipe progress
 * - Crisp white icons & text on selected tab; refined muted typography on inactive tabs
 * - Fluid spring animation on tap & 1:1 finger tracking during horizontal swipe
 */
@Composable
fun FloatingBottomBar(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val barHeight = (64 * density.fontScale.coerceIn(1.0f, 1.25f)).dp

    val tabs = remember {
        listOf(
            Triple("Snap", Icons.Filled.Lock, Icons.Outlined.Lock),
            Triple("DNS", Icons.Filled.Shield, Icons.Outlined.Shield),
            Triple("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = LemonTheme.colors.navBackground,
            border = BorderStroke(1.dp, LemonTheme.colors.navBorder),
            shadowElevation = 10.dp,
            modifier = Modifier
                .height(barHeight)
                .fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                val totalWidthPx = constraints.maxWidth.toFloat()
                val tabCount = tabs.size
                val tabWidthPx = totalWidthPx / tabCount
                val tabWidthDp = with(density) { tabWidthPx.toDp() }

                // Continuous fractional pager position (e.g. 0.0 -> 2.0)
                val currentPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val indicatorOffsetPx = (currentPosition * tabWidthPx).coerceIn(0f, totalWidthPx - tabWidthPx)

                // Shared Moving Electric Blue Capsule Indicator
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffsetPx.roundToInt(), 0) }
                        .width(tabWidthDp)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(100.dp), ambientColor = ElectricBlueGlow, spotColor = ElectricBlueGlow)
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(ElectricBlueStart, ElectricBlueEnd)
                            )
                        )
                )

                // Tab items row overlay
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
                        // Calculate how close the indicator currently is to this tab (0.0 = directly selected, 1.0+ = unselected)
                        val distanceFromIndicator = abs(currentPosition - index).coerceIn(0f, 1f)
                        val isSelected = distanceFromIndicator < 0.5f

                        // Cross-fade colors based on fractional distance
                        val activeColor = Color.White
                        val inactiveColor = LemonTheme.colors.textTertiary
                        val textColor = interpolateColor(activeColor, inactiveColor, distanceFromIndicator)

                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                    stateDescription = if (isSelected) "Active tab" else "Inactive tab"
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        if (pagerState.currentPage != index) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onTabSelected(index)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                    contentDescription = label,
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    style = LemonTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun interpolateColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}
