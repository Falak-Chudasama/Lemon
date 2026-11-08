package com.coconut.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coconut.ui.LemonTheme
import com.coconut.ui.NavigationTab

/**
 * Floating bottom navigation bar.
 * Height: 64dp+ capsule with 20dp horizontal margin, floating above scrolling content.
 * Follows iOS navigation bar translucency and equal-width tab touch targets.
 * Adapts height dynamically for large font scaling.
 */
@Composable
fun FloatingBottomBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val barHeight = (64 * density.fontScale.coerceIn(1.0f, 1.25f)).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = LemonTheme.colors.navBackground,
            border = BorderStroke(1.dp, LemonTheme.colors.navBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .height(barHeight)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Snap",
                    selectedIcon = Icons.Filled.Lock,
                    unselectedIcon = Icons.Outlined.Lock,
                    isSelected = selectedTab == NavigationTab.SNAP,
                    onClick = {
                        if (selectedTab != NavigationTab.SNAP) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(NavigationTab.SNAP)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    label = "DNS",
                    selectedIcon = Icons.Filled.Shield,
                    unselectedIcon = Icons.Outlined.Shield,
                    isSelected = selectedTab == NavigationTab.DNS,
                    onClick = {
                        if (selectedTab != NavigationTab.DNS) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(NavigationTab.DNS)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    label = "Settings",
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    isSelected = selectedTab == NavigationTab.SETTINGS,
                    onClick = {
                        if (selectedTab != NavigationTab.SETTINGS) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(NavigationTab.SETTINGS)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = if (LemonTheme.isDark) LemonTheme.colors.primaryAccent else LemonTheme.colors.textPrimary
    val inactiveColor = LemonTheme.colors.textTertiary

    val tintColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 180),
        label = "TabTintColor"
    )

    val backgroundPillColor = if (isSelected) {
        if (LemonTheme.isDark) {
            LemonTheme.colors.primaryAccent.copy(alpha = 0.20f)
        } else {
            LemonTheme.colors.primaryAccent.copy(alpha = 0.35f)
        }
    } else {
        Color.Transparent
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
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
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(backgroundPillColor)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = LemonTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = tintColor
                )
            }
        }
    }
}
