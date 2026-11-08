package com.coconut.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.coconut.ui.AppIconCache
import com.coconut.ui.InstalledApp
import com.coconut.ui.LemonTheme
import com.coconut.ui.MainViewModel
import com.coconut.ui.components.LemonBadge
import com.coconut.ui.components.LemonBadgeStyle
import com.coconut.ui.components.LemonEmptyState
import com.coconut.ui.components.LemonSearchField
import com.coconut.ui.components.LemonSegmentItem
import com.coconut.ui.components.LemonSegmentedControl

@Composable
fun SnapScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    val snappedPackageNames = remember(state.snappedApps) {
        state.snappedApps.map { it.packageName }.toSet()
    }

    // Dynamic categories present among installed apps
    val availableCategories = remember(state.installedApps) {
        listOf("All") + state.installedApps.map { it.category }.distinct().sorted()
    }

    val filteredApps = remember(
        state.installedApps,
        state.searchQuery,
        selectedStatusFilter,
        selectedCategoryFilter,
        snappedPackageNames
    ) {
        state.installedApps.filter { app ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    app.label.contains(state.searchQuery, ignoreCase = true) ||
                    app.packageName.contains(state.searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "Snapped" -> snappedPackageNames.contains(app.packageName)
                "Available" -> !snappedPackageNames.contains(app.packageName)
                else -> true
            }

            val matchesCategory = selectedCategoryFilter == "All" || app.category == selectedCategoryFilter

            matchesSearch && matchesStatus && matchesCategory
        }
    }

    val snappedCount = snappedPackageNames.size
    val restrictedGrammarText = when (snappedCount) {
        0 -> "0 apps restricted"
        1 -> "1 app restricted"
        else -> "$snappedCount apps restricted"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status bar top spacing
        item(key = "status_bar_spacing") {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }

        // 1. Page Header (Large 34sp Title + Counter)
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "The Snap",
                        style = LemonTheme.typography.displayLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = restrictedGrammarText,
                        style = LemonTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp
                        ),
                        color = LemonTheme.colors.textSecondary
                    )
                }

                if (state.isStrictModeActive) {
                    LemonBadge(
                        text = "Strict Mode",
                        style = LemonBadgeStyle.WARNING
                    )
                }
            }
        }

        // 2. Accessibility Permission Notice Banner
        if (!state.isAccessibilityGranted) {
            item(key = "accessibility_card") {
                Surface(
                    shape = LemonTheme.shapes.card,
                    color = LemonTheme.colors.warningContainer,
                    border = BorderStroke(1.dp, LemonTheme.colors.warning.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Accessibility required",
                                    style = LemonTheme.typography.titleSmall.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = LemonTheme.colors.onWarningContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Required to enforce app restrictions.",
                                style = LemonTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp
                                ),
                                color = LemonTheme.colors.onWarningContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { showAccessibilityDisclosure = true },
                            shape = LemonTheme.shapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LemonTheme.colors.primaryAccent,
                                contentColor = LemonTheme.colors.onPrimaryAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Enable",
                                style = LemonTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Search Field (Capsule 48dp)
        item(key = "search") {
            LemonSearchField(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                placeholder = "Search installed apps..."
            )
        }

        // 4. Status Filter Segmented Pill Control (All / Snapped / Available)
        item(key = "status_filters") {
            val segmentItems = listOf(
                LemonSegmentItem(id = "All", title = "All", count = state.installedApps.size),
                LemonSegmentItem(id = "Snapped", title = "Snapped", count = snappedCount),
                LemonSegmentItem(
                    id = "Available",
                    title = "Available",
                    count = (state.installedApps.size - snappedCount).coerceAtLeast(0)
                )
            )
            LemonSegmentedControl(
                items = segmentItems,
                selectedId = selectedStatusFilter,
                onItemSelected = { selectedStatusFilter = it }
            )
        }

        // 5. Category Filter Chips (if more than 2 categories exist)
        if (availableCategories.size > 2) {
            item(key = "category_filters") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableCategories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = cat },
                            label = {
                                Text(
                                    text = cat,
                                    style = LemonTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LemonTheme.colors.primaryAccent,
                                selectedLabelColor = LemonTheme.colors.onPrimaryAccent,
                                containerColor = LemonTheme.colors.cardSecondaryBackground,
                                labelColor = LemonTheme.colors.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color.Transparent else LemonTheme.colors.cardBorder
                            )
                        )
                    }
                }
            }
        }

        // 6. Application Rows or Empty State
        if (filteredApps.isEmpty()) {
            item(key = "empty_apps") {
                LemonEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No applications found",
                    description = if (state.searchQuery.isNotEmpty())
                        "No installed apps match '${state.searchQuery}'."
                    else
                        "No applications match the selected filter.",
                    actionLabel = if (state.searchQuery.isNotEmpty()) "Clear search" else null,
                    onActionClick = if (state.searchQuery.isNotEmpty()) {
                        { viewModel.setSearchQuery("") }
                    } else null
                )
            }
        } else {
            items(
                items = filteredApps,
                key = { it.packageName }
            ) { app ->
                val isSnapped = snappedPackageNames.contains(app.packageName)
                LemonAppRow(
                    app = app,
                    isSnapped = isSnapped,
                    onSnapClicked = {
                        if (!state.isAccessibilityGranted) {
                            showAccessibilityDisclosure = true
                        } else if (activity != null) {
                            viewModel.startSnapProcess(activity, app)
                        }
                    }
                )
            }
        }
    }

    // Two-step Typed Confirmation Sheet
    if (state.showSnapTypedSheet && state.pendingSnapApp != null) {
        val targetApp = state.pendingSnapApp!!
        AlertDialog(
            onDismissRequest = viewModel::cancelSnapConfirmation,
            shape = LemonTheme.shapes.dialog,
            containerColor = LemonTheme.colors.cardBackground,
            title = {
                Text(
                    text = "Confirm Snap",
                    style = LemonTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LemonTheme.colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Restrict ${targetApp.label}?",
                        style = LemonTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                    Text(
                        text = "This Snap cannot be reversed through normal Lemon controls.",
                        style = LemonTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = LemonTheme.colors.danger
                    )
                    Text(
                        text = "Type SNAP in uppercase to confirm:",
                        style = LemonTheme.typography.bodySmall.copy(
                            fontSize = 13.sp
                        ),
                        color = LemonTheme.colors.textSecondary
                    )
                    OutlinedTextField(
                        value = state.typedSnapText,
                        onValueChange = viewModel::updateTypedSnapText,
                        singleLine = true,
                        placeholder = { Text("SNAP") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = LemonTheme.shapes.button,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LemonTheme.colors.primaryAccent,
                            unfocusedBorderColor = LemonTheme.colors.cardBorder,
                            focusedContainerColor = LemonTheme.colors.cardSecondaryBackground,
                            unfocusedContainerColor = LemonTheme.colors.cardSecondaryBackground,
                            focusedTextColor = LemonTheme.colors.textPrimary,
                            unfocusedTextColor = LemonTheme.colors.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::commitSnap,
                    enabled = state.typedSnapText == "SNAP",
                    shape = LemonTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LemonTheme.colors.danger,
                        contentColor = Color.White,
                        disabledContainerColor = LemonTheme.colors.cardSecondaryBackground,
                        disabledContentColor = LemonTheme.colors.textTertiary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Confirm Snap",
                        style = LemonTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelSnapConfirmation,
                    shape = LemonTheme.shapes.button
                ) {
                    Text(
                        text = "Cancel",
                        style = LemonTheme.typography.labelMedium,
                        color = LemonTheme.colors.textSecondary
                    )
                }
            }
        )
    }

    // Accessibility Disclosure Dialog (3 Structured Disclosure Rows)
    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            containerColor = LemonTheme.colors.cardBackground,
            shape = LemonTheme.shapes.dialog,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = LemonTheme.colors.primaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Accessibility Disclosure",
                        style = LemonTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Lemon uses the Android AccessibilityService API strictly to detect when you launch a restricted app and return home.",
                        style = LemonTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = LemonTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Row 1: Foreground transitions only
                    DisclosurePointRow(
                        icon = Icons.Default.Lock,
                        text = "Detects foreground transitions of snapped apps only."
                    )

                    // Row 2: Never reads content
                    DisclosurePointRow(
                        icon = Icons.Default.VisibilityOff,
                        text = "Never inspects screen content, text, passwords or keystrokes."
                    )

                    // Row 3: Zero network collection
                    DisclosurePointRow(
                        icon = Icons.Default.CheckCircle,
                        text = "Zero data is collected or transmitted off your device."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDisclosure = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    shape = LemonTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LemonTheme.colors.primaryAccent,
                        contentColor = LemonTheme.colors.onPrimaryAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Open Settings",
                        style = LemonTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAccessibilityDisclosure = false },
                    shape = LemonTheme.shapes.button
                ) {
                    Text(
                        text = "Cancel",
                        style = LemonTheme.typography.labelMedium,
                        color = LemonTheme.colors.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
private fun DisclosurePointRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(LemonTheme.colors.primaryAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LemonTheme.colors.textAccent,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = LemonTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            color = LemonTheme.colors.textPrimary
        )
    }
}

@Composable
private fun LemonAppRow(
    app: InstalledApp,
    isSnapped: Boolean,
    onSnapClicked: () -> Unit
) {
    val context = LocalContext.current
    var iconBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        iconBitmap = AppIconCache.loadIcon(context, app.packageName)
    }

    Surface(
        shape = LemonTheme.shapes.card,
        color = if (isSnapped) LemonTheme.colors.cardSecondaryBackground else LemonTheme.colors.cardBackground,
        border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon in 12dp squircle
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap!!,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(LemonTheme.shapes.iconSquircle)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(LemonTheme.shapes.iconSquircle)
                        .background(LemonTheme.colors.cardSecondaryBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        style = LemonTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // App Label & Clean Category
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = LemonTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = LemonTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.category,
                    style = LemonTheme.typography.bodySmall.copy(
                        fontSize = 13.sp
                    ),
                    color = LemonTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Status or Action Button (Minimum 48dp touch target)
            if (isSnapped) {
                LemonBadge(
                    text = "Snapped",
                    style = LemonBadgeStyle.DANGER,
                    showDot = true
                )
            } else {
                Button(
                    onClick = onSnapClicked,
                    shape = LemonTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LemonTheme.colors.primaryAccent,
                        contentColor = LemonTheme.colors.onPrimaryAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Snap",
                        style = LemonTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
