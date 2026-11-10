package com.coconut.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.coconut.R
import com.coconut.ui.AppIconCache
import com.coconut.ui.AppLoadingState
import com.coconut.ui.InstalledApp
import com.coconut.ui.LemonTheme
import com.coconut.ui.MainViewModel
import com.coconut.ui.components.LemonBadge
import com.coconut.ui.components.LemonBadgeStyle
import com.coconut.ui.components.LemonCard
import com.coconut.ui.components.LemonEmptyState
import com.coconut.ui.components.LemonGroupedContainer
import com.coconut.ui.components.LemonGroupedRow
import com.coconut.ui.components.LemonMetricWell
import com.coconut.ui.components.LemonSearchField
import com.coconut.ui.components.LemonSectionEyebrow
import com.coconut.ui.components.LemonSegmentItem
import com.coconut.ui.components.LemonSegmentedControl

/**
 * Pristine Redesigned Snap Screen conforming to the Fruits Family Quiet Utility Design System.
 * Eliminates card-soup and repetitive yellow buttons:
 * - Geometric Unbounded header with Lemon official mark
 * - Unified 3-column metric summary strip
 * - Grouped container list with 40dp squircle app icons, subtle indented dividers, and tactile pill actions
 * - Clean bottom-sheet / modal flow for Quarantine focus configuration
 * - 120dp safe bottom padding preventing bottom navigation clipping
 */
@Composable
fun SnapScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    // Quarantine Setup State
    var showQuarantineSetup by remember { mutableStateOf(false) }
    var quarantineDurationSlider by remember { mutableStateOf(60f) } // 60 minutes default
    var quarantineSelectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showQuarantineAppPicker by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status bar top spacing
        item(key = "status_bar_spacing") {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }

        // 1. Page Header (Fruit Family PageShell pattern with Unbounded display title)
        item(key = "snap_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = LemonTheme.shapes.iconSquircle,
                            color = LemonTheme.colors.cardBackground,
                            border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_lemon_mark),
                                    contentDescription = "Lemon Logo",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "The Snap",
                                style = LemonTheme.typography.displayLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                        }
                    }

                    if (state.isStrictModeActive) {
                        LemonBadge(
                            text = "Strict Mode",
                            style = LemonBadgeStyle.WARNING,
                            showDot = true
                        )
                    } else if (state.isAccessibilityGranted) {
                        LemonBadge(
                            text = "Active",
                            style = LemonBadgeStyle.SUCCESS,
                            showDot = true
                        )
                    } else {
                        LemonBadge(
                            text = "Attention",
                            style = LemonBadgeStyle.WARNING,
                            showDot = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Instant on-device app launch interception and focus enforcement.",
                    style = LemonTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = LemonTheme.colors.textSecondary
                )
            }
        }

        // 2. Unified Metric Summary Strip (Kiwi ResultPanel Well Style)
        item(key = "metric_summary_well") {
            LemonMetricWell(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$snappedCount",
                            style = LemonTheme.typography.displaySmall.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (snappedCount > 0) LemonTheme.colors.brandAmber else LemonTheme.colors.textPrimary
                        )
                        Text(
                            text = "SNAPPED",
                            style = LemonTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = LemonTheme.colors.textTertiary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .width(1.dp)
                            .background(LemonTheme.colors.separator)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${state.installedApps.size}",
                            style = LemonTheme.typography.displaySmall.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = LemonTheme.colors.textPrimary
                        )
                        Text(
                            text = "INSTALLED",
                            style = LemonTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = LemonTheme.colors.textTertiary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .width(1.dp)
                            .background(LemonTheme.colors.separator)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isQuarantineActive) {
                            val secs = state.quarantineRemainingSeconds
                            val h = secs / 3600
                            val m = (secs % 3600) / 60
                            val timeStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                            Text(
                                text = timeStr,
                                style = LemonTheme.typography.displaySmall.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.danger
                            )
                        } else {
                            Text(
                                text = "Off",
                                style = LemonTheme.typography.displaySmall.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = "QUARANTINE",
                            style = LemonTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = LemonTheme.colors.textTertiary
                        )
                    }
                }
            }
        }

        // 3. Accessibility Permission Notice Banner (Quiet Alert, only when missing)
        if (!state.isAccessibilityGranted) {
            item(key = "accessibility_notice") {
                Surface(
                    shape = LemonTheme.shapes.card,
                    color = LemonTheme.colors.warningContainer,
                    border = BorderStroke(1.dp, LemonTheme.colors.warning.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(LemonTheme.shapes.iconSquircle)
                                    .background(LemonTheme.colors.warning.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.warning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Accessibility Required",
                                    style = LemonTheme.typography.titleSmall.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = LemonTheme.colors.onWarningContainer
                                )
                                Text(
                                    text = "Enable service to observe launched apps",
                                    style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = LemonTheme.colors.onWarningContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = LemonTheme.colors.primaryAccent,
                            modifier = Modifier.clickable { showAccessibilityDisclosure = true }
                        ) {
                            Text(
                                text = "Enable",
                                style = LemonTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.onPrimaryAccent,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Quarantine Mode Action Row
        item(key = "quarantine_action_row") {
            LemonGroupedContainer {
                if (state.isQuarantineActive) {
                    val totalSecs = state.quarantineRemainingSeconds
                    val hrs = totalSecs / 3600
                    val mins = (totalSecs % 3600) / 60
                    val secs = totalSecs % 60
                    val countdownStr = String.format("%02d:%02d:%02d", hrs, mins, secs)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(LemonTheme.shapes.iconSquircle)
                                        .background(LemonTheme.colors.danger.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = LemonTheme.colors.danger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Quarantine Focus Active",
                                        style = LemonTheme.typography.titleSmall.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = LemonTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Strict lockdown • $countdownStr remaining",
                                        style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = LemonTheme.colors.danger
                                    )
                                }
                            }
                            LemonBadge(
                                text = countdownStr,
                                style = LemonBadgeStyle.DANGER,
                                showDot = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = LemonTheme.colors.danger,
                            trackColor = LemonTheme.colors.danger.copy(alpha = 0.15f)
                        )
                    }
                } else {
                    LemonGroupedRow(
                        title = "Quarantine Mode",
                        subtitle = "Schedule a 30m – 5h irreversible focus session",
                        leadingIcon = Icons.Default.HourglassTop,
                        leadingIconTint = LemonTheme.colors.brandAmber,
                        showDivider = false,
                        trailingContent = {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = LemonTheme.colors.cardSecondaryBackground,
                                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                                modifier = Modifier.clickable { showQuarantineSetup = true }
                            ) {
                                Text(
                                    text = "Set Timer...",
                                    style = LemonTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = LemonTheme.colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        // 5. Search Field & Status Segmented Bar
        item(key = "search_and_filter_block") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LemonSearchField(
                    query = state.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    placeholder = "Search installed applications..."
                )

                LemonSegmentedControl(
                    items = listOf(
                        LemonSegmentItem(id = "All", title = "All", count = state.installedApps.size),
                        LemonSegmentItem(id = "Snapped", title = "Snapped", count = snappedCount),
                        LemonSegmentItem(
                            id = "Available",
                            title = "Available",
                            count = (state.installedApps.size - snappedCount).coerceAtLeast(0)
                        )
                    ),
                    selectedId = selectedStatusFilter,
                    onItemSelected = { selectedStatusFilter = it }
                )
            }
        }

        // 6. Category Filter Chips (if present)
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
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LemonTheme.colors.primaryAccent,
                                selectedLabelColor = LemonTheme.colors.onPrimaryAccent,
                                containerColor = LemonTheme.colors.cardBackground,
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

        // 7. Eyebrow Section Title
        item(key = "results_eyebrow") {
            LemonSectionEyebrow(
                title = if (filteredApps.isEmpty()) "APPLICATIONS" else "APPLICATIONS (${filteredApps.size})"
            )
        }

        // 8. Cohesive Grouped Applications Container
        val isInitialLoading = state.appLoadingState == AppLoadingState.INITIAL_LOADING ||
                (state.appLoadingState == AppLoadingState.LOADING && state.installedApps.isEmpty())

        if (isInitialLoading) {
            item(key = "loading_container") {
                LemonGroupedContainer {
                    repeat(6) { index ->
                        LemonSkeletonRow(showDivider = index < 5)
                    }
                }
            }
        } else if (state.appLoadingState == AppLoadingState.ERROR && state.installedApps.isEmpty()) {
            item(key = "error_loading_apps") {
                LemonEmptyState(
                    icon = Icons.Default.Warning,
                    title = "Failed to load applications",
                    description = state.appLoadingError ?: "Could not enumerate installed applications.",
                    actionLabel = "Retry",
                    onActionClick = { viewModel.loadInstalledApps() }
                )
            }
        } else if (filteredApps.isEmpty()) {
            item(key = "empty_apps") {
                val (emptyTitle, emptyDesc, actionText, actionClick) = when {
                    state.searchQuery.isNotEmpty() -> Quadruple(
                        "No applications match",
                        "No installed apps match '${state.searchQuery}'.",
                        "Clear search",
                        { viewModel.setSearchQuery("") }
                    )
                    selectedStatusFilter == "Snapped" -> Quadruple(
                        "No apps snapped",
                        "You haven't restricted any applications yet. Select 'All' to browse and snap apps.",
                        "Show all apps",
                        { selectedStatusFilter = "All" }
                    )
                    selectedCategoryFilter != "All" -> Quadruple(
                        "No apps in $selectedCategoryFilter",
                        "No installed applications match the $selectedCategoryFilter category.",
                        "Show all categories",
                        { selectedCategoryFilter = "All" }
                    )
                    else -> Quadruple(
                        "No applications found",
                        "No user-launchable applications are currently available.",
                        null,
                        null
                    )
                }
                LemonEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = emptyTitle,
                    description = emptyDesc,
                    actionLabel = actionText,
                    onActionClick = actionClick
                )
            }
        } else {
            item(key = "grouped_apps_container") {
                LemonGroupedContainer {
                    filteredApps.forEachIndexed { index, app ->
                        val isSnapped = snappedPackageNames.contains(app.packageName)
                        LemonAppRow(
                            app = app,
                            isSnapped = isSnapped,
                            showDivider = index < filteredApps.lastIndex,
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
        }
    }

    // Quarantine Setup Dialog
    if (showQuarantineSetup) {
        val durationInt = quarantineDurationSlider.toInt()
        val hrs = durationInt / 60
        val mins = durationInt % 60
        val durationFormatted = if (hrs > 0 && mins > 0) "${hrs} hr ${mins} min" else if (hrs > 0) "${hrs} hr" else "${mins} min"

        AlertDialog(
            onDismissRequest = { showQuarantineSetup = false },
            shape = LemonTheme.shapes.dialog,
            containerColor = LemonTheme.colors.cardBackground,
            title = {
                Text(
                    text = "Quarantine Focus Session",
                    style = LemonTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = LemonTheme.colors.textPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Impose an irreversible focus countdown. Selected applications will be strictly inaccessible until the timer expires.",
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = LemonTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duration: $durationFormatted",
                            style = LemonTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = LemonTheme.colors.textPrimary
                        )
                        Text(
                            text = "30m – 5h",
                            style = LemonTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LemonTheme.colors.textTertiary
                        )
                    }

                    Slider(
                        value = quarantineDurationSlider,
                        onValueChange = { quarantineDurationSlider = it },
                        valueRange = 30f..300f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = LemonTheme.colors.brandAmber,
                            activeTrackColor = LemonTheme.colors.primaryAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = LemonTheme.colors.cardSecondaryBackground,
                        border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQuarantineAppPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Apps",
                                style = LemonTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                color = LemonTheme.colors.textPrimary
                            )
                            Text(
                                text = "${quarantineSelectedPackages.size} apps →",
                                style = LemonTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                color = LemonTheme.colors.brandAmber
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (activity != null) {
                            viewModel.startQuarantineSession(
                                activity = activity,
                                durationMinutes = durationInt,
                                packages = quarantineSelectedPackages
                            )
                            showQuarantineSetup = false
                        }
                    },
                    enabled = quarantineSelectedPackages.isNotEmpty(),
                    shape = LemonTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LemonTheme.colors.primaryAccent,
                        contentColor = LemonTheme.colors.onPrimaryAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Start Quarantine",
                        style = LemonTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuarantineSetup = false },
                    shape = LemonTheme.shapes.button
                ) {
                    Text("Cancel", style = LemonTheme.typography.labelMedium, color = LemonTheme.colors.textSecondary)
                }
            }
        )
    }

    // Quarantine Application Picker Dialog
    if (showQuarantineAppPicker) {
        var tempSelected by remember { mutableStateOf(quarantineSelectedPackages) }
        AlertDialog(
            onDismissRequest = { showQuarantineAppPicker = false },
            shape = LemonTheme.shapes.dialog,
            containerColor = LemonTheme.colors.cardBackground,
            title = {
                Text(
                    text = "Select Quarantined Apps",
                    style = LemonTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = LemonTheme.colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    Text(
                        text = "Selected apps will be temporarily blocked for the duration of Quarantine:",
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = LemonTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.installedApps, key = { it.packageName }) { app ->
                            val isChecked = tempSelected.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        tempSelected = if (isChecked) {
                                            tempSelected - app.packageName
                                        } else {
                                            tempSelected + app.packageName
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        tempSelected = if (checked) {
                                            tempSelected + app.packageName
                                        } else {
                                            tempSelected - app.packageName
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = LemonTheme.colors.brandAmber,
                                        checkmarkColor = LemonTheme.colors.onPrimaryAccent
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = app.label,
                                        style = LemonTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                        color = LemonTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = app.category,
                                        style = LemonTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = LemonTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        quarantineSelectedPackages = tempSelected
                        showQuarantineAppPicker = false
                    },
                    shape = LemonTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LemonTheme.colors.primaryAccent,
                        contentColor = LemonTheme.colors.onPrimaryAccent
                    )
                ) {
                    Text(
                        text = "Confirm (${tempSelected.size})",
                        style = LemonTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuarantineAppPicker = false },
                    shape = LemonTheme.shapes.button
                ) {
                    Text("Cancel", style = LemonTheme.typography.labelMedium, color = LemonTheme.colors.textSecondary)
                }
            }
        )
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
                        fontSize = 18.sp,
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
                            fontSize = 16.sp,
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
                            focusedBorderColor = LemonTheme.colors.brandAmber,
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

    // Accessibility Disclosure Dialog
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
                        tint = LemonTheme.colors.brandAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Accessibility Disclosure",
                        style = LemonTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
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

                    DisclosurePointRow(
                        icon = Icons.Default.Lock,
                        text = "Detects foreground transitions of snapped apps only."
                    )

                    DisclosurePointRow(
                        icon = Icons.Default.VisibilityOff,
                        text = "Never inspects screen content, text, passwords or keystrokes."
                    )

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
private fun LemonAppRow(
    app: InstalledApp,
    isSnapped: Boolean,
    showDivider: Boolean,
    onSnapClicked: () -> Unit
) {
    val context = LocalContext.current
    var iconBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        iconBitmap = AppIconCache.loadIcon(context, app.packageName)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isSnapped, onClick = onSnapClicked)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon in 10dp squircle
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap!!,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                        fontSize = 15.sp,
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
                        fontSize = 12.sp
                    ),
                    color = LemonTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status Badge or Subtle Snap Pill Button (Quiet Utility)
            if (isSnapped) {
                LemonBadge(
                    text = "Snapped",
                    style = LemonBadgeStyle.DANGER,
                    showDot = true
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = LemonTheme.colors.cardSecondaryBackground,
                    border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                    modifier = Modifier.clickable(onClick = onSnapClicked)
                ) {
                    Text(
                        text = "Snap",
                        style = LemonTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp),
                thickness = 0.5.dp,
                color = LemonTheme.colors.separator
            )
        }
    }
}

@Composable
private fun LemonSkeletonRow(showDivider: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LemonTheme.colors.cardSecondaryBackground)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LemonTheme.colors.cardSecondaryBackground)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LemonTheme.colors.cardSecondaryBackground.copy(alpha = 0.6f))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 28.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(LemonTheme.colors.cardSecondaryBackground)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp),
                thickness = 0.5.dp,
                color = LemonTheme.colors.separator
            )
        }
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
                .background(LemonTheme.colors.brandYellow.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LemonTheme.colors.brandAmber,
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

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
