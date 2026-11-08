package com.coconut.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coconut.ui.LemonTheme

data class LemonSegmentItem(
    val id: String,
    val title: String,
    val count: Int? = null
)

/**
 * iOS-styled segmented pill control with sliding animated indicator.
 * Height: 44-48dp, full capsule shape, spring physics.
 */
@Composable
fun LemonSegmentedControl(
    items: List<LemonSegmentItem>,
    selectedId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val selectedIndex = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = LemonTheme.colors.cardSecondaryBackground,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val segmentWidth = maxWidth / items.size

            // Animated sliding selection pill
            val animatedOffsetFraction by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "SegmentPillSlide"
            )

            // The sliding pill background
            Box(
                modifier = Modifier
                    .offset(x = segmentWidth * animatedOffsetFraction)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(100.dp))
                    .clip(RoundedCornerShape(100.dp))
                    .background(LemonTheme.colors.cardBackground)
            )

            // Segment text items
            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(100.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onItemSelected(item.id)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = LemonTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) LemonTheme.colors.textPrimary else LemonTheme.colors.textSecondary
                            )
                            if (item.count != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(
                                            if (isSelected) LemonTheme.colors.cardSecondaryBackground
                                            else LemonTheme.colors.cardBackground.copy(alpha = 0.6f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.count.toString(),
                                        style = LemonTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (isSelected) LemonTheme.colors.textPrimary else LemonTheme.colors.textTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

