package com.coconut.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coconut.ui.AppIconCache
import com.coconut.ui.CoconutColors
import com.coconut.ui.InstalledApp
import com.coconut.ui.MainUiState

private val CATEGORIES = listOf(
    "All", "Social", "Entertainment", "Games", "Productivity", "Communication", "Utilities", "Other"
)

@Composable
fun SnapScreen(
    state: MainUiState,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTabChange: (Boolean) -> Unit,
    onSnapRequest: (InstalledApp) -> Unit,
    onShowBlockedDialog: (InstalledApp) -> Unit
) {
    val snappedPackageSet = remember(state.snappedApps) {
        state.snappedApps.map { it.packageName }.toSet()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 1. SEARCH BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search apps to snap...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // --- 2. SEGMENTED TABS: APPS vs SNAPPED ---
        TabRow(
            selectedTabIndex = if (state.showRegistryTab) 1 else 0,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (state.showRegistryTab) 1 else 0]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Tab(
                selected = !state.showRegistryTab,
                onClick = { onTabChange(false) },
                text = {
                    Text(
                        text = "Apps (${state.installedApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!state.showRegistryTab) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
            Tab(
                selected = state.showRegistryTab,
                onClick = { onTabChange(true) },
                text = {
                    Text(
                        text = "Snapped (${state.snappedApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (state.showRegistryTab) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 3. CATEGORY CHIPS ---
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CATEGORIES) { cat ->
                val isSelected = state.selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryChange(cat) },
                    label = {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 4. LIST CONTENT ---
        if (!state.showRegistryTab) {
            val filteredApps = remember(state.installedApps, state.searchQuery, state.selectedCategory, snappedPackageSet) {
                var list = state.installedApps
                if (state.searchQuery.isNotBlank()) {
                    val q = state.searchQuery.trim().lowercase()
                    list = list.filter {
                        it.applicationName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                    }
                }
                if (state.selectedCategory != "All") {
                    list = list.filter { it.category == state.selectedCategory }
                }
                list.sortedWith(
                    compareBy<InstalledApp> { it.packageName in snappedPackageSet }
                        .thenBy { it.applicationName.lowercase() }
                )
            }

            if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isLoading) "Loading applications..." else "No applications match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSnapped = app.packageName in snappedPackageSet
                        AppRowCard(
                            app = app,
                            isSnapped = isSnapped,
                            onSnap = { onSnapRequest(app) },
                            onItemClick = {
                                if (isSnapped) {
                                    onShowBlockedDialog(app)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            val snappedList = remember(state.installedApps, state.snappedApps, state.searchQuery, state.selectedCategory) {
                val appMap = state.installedApps.associateBy { it.packageName }
                var list = state.snappedApps.map { record ->
                    appMap[record.packageName] ?: InstalledApp(
                        packageName = record.packageName,
                        applicationName = record.applicationName,
                        category = record.category,
                        isSystemApp = false,
                        isProtected = false,
                        protectionReason = null,
                        isSuspended = true,
                        icon = null
                    )
                }

                if (state.searchQuery.isNotBlank()) {
                    val q = state.searchQuery.trim().lowercase()
                    list = list.filter {
                        it.applicationName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                    }
                }
                if (state.selectedCategory != "All") {
                    list = list.filter { it.category == state.selectedCategory }
                }
                list.sortedBy { it.applicationName.lowercase() }
            }

            if (snappedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No snapped applications yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select apps from the Apps tab to permanently snap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(snappedList, key = { it.packageName }) { app ->
                        AppRowCard(
                            app = app,
                            isSnapped = true,
                            onSnap = {},
                            onItemClick = { onShowBlockedDialog(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRowCard(
    app: InstalledApp,
    isSnapped: Boolean,
    onSnap: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSnapped) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSnapped) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FastAppIcon(packageName = app.packageName)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.applicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSnapped) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isSnapped) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CoconutColors.SnappedRed.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onItemClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CoconutColors.SnappedRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Snapped",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CoconutColors.SnappedRed
                        )
                    }
                }
            } else if (app.isProtected) {
                Text(
                    text = "System",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                Button(
                    onClick = onSnap,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Snap",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FastAppIcon(packageName: String) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) { mutableStateOf(AppIconCache.get(packageName)) }

    LaunchedEffect(packageName) {
        if (iconBitmap == null) {
            iconBitmap = AppIconCache.loadIcon(context, packageName)
        }
    }

    val currentBitmap = iconBitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
