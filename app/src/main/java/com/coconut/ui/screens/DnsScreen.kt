package com.coconut.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.coconut.data.DomainChangeOperation
import com.coconut.data.DomainChangeRequestEntity
import com.coconut.data.DomainRuleEntity
import com.coconut.ui.LemonColors
import com.coconut.ui.MainViewModel
import com.coconut.ui.components.GlassStatusBadge

@Composable
fun DnsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showAddDomainDialog by remember { mutableStateOf(false) }
    var domainInputText by remember { mutableStateOf("") }
    var isBulkInput by remember { mutableStateOf(false) }

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleDns(true)
        }
    }

    val blockedRules = remember(state.domainRules) {
        state.domainRules.filter { it.ruleType == "BLOCK" }
    }
    val allowedRules = remember(state.domainRules) {
        state.domainRules.filter { it.ruleType == "ALLOW" }
    }

    val currentRules = if (state.selectedRuleTab == "BLOCK") blockedRules else allowedRules
    val filteredRules = remember(currentRules, state.domainFilterQuery) {
        if (state.domainFilterQuery.isBlank()) currentRules
        else currentRules.filter { it.domain.contains(state.domainFilterQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Screen Header
        item(key = "dns_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Content Shield",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (state.isVpnRunning) "DNS active" else "DNS inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GlassStatusBadge(
                    text = if (state.isVpnRunning) "Protected" else "Inactive",
                    statusColor = if (state.isVpnRunning) LemonColors.PrimaryGreenLight else LemonColors.TextSecondaryLight
                )
            }
        }

        // 2. Master DNS Filtering Switch Card
        item(key = "master_dns_card") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DNS Filtering",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.isInsideNightSchedule) {
                            Text(
                                text = "Locked: Night schedule active (11:00 PM – 6:00 AM)",
                                style = MaterialTheme.typography.bodySmall,
                                color = LemonColors.WarningLight,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "Local encrypted DNS loopback",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = state.isVpnRunning,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnPrepareLauncher.launch(prepareIntent)
                                } else {
                                    viewModel.toggleDns(true)
                                }
                            } else {
                                viewModel.toggleDns(false)
                            }
                        },
                        enabled = !state.isInsideNightSchedule,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // 3. Permanent Daily DNS Protection Schedule Card (11:00 PM – 6:00 AM)
        item(key = "daily_dns_schedule_card") {
            val isInside = state.isInsideNightSchedule

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Daily Protection",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Daily DNS Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "11:00 PM – 6:00 AM",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Repeats every day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    GlassStatusBadge(
                        text = if (isInside) "Active" else "Scheduled",
                        statusColor = if (isInside) LemonColors.PrimaryGreenLight else MaterialTheme.colorScheme.primary,
                        showDot = true
                    )
                }
            }
        }

        // 4. Built-in Feeds Card (HaGeZi Feeds: TIF Mini, NSFW, Multi PRO Mini)
        item(key = "builtin_feeds_card") {
            val config = state.dnsConfig
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Built-in Protection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Feed 1: Threat Intelligence (TIF Mini)
                    FeedToggleRow(
                        title = "Threat Intelligence",
                        subtitle = "HaGeZi TIF Mini (189,700 rules)",
                        checked = config?.blockThreats ?: true,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockThreats = it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Feed 2: Adult Content (NSFW)
                    FeedToggleRow(
                        title = "Adult Content",
                        subtitle = "HaGeZi NSFW (77,921 rules)",
                        checked = config?.blockAdultContent ?: false,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockAdultContent = it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Feed 3: Ads & Trackers (Multi PRO Mini)
                    FeedToggleRow(
                        title = "Ads & Trackers",
                        subtitle = "HaGeZi Multi PRO Mini (164,028 rules)",
                        checked = config?.blockAdsTrackers ?: false,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockAds = it) }
                    )
                }
            }
        }

        // 5. Pending Changes Queue Card (Exact 30-Day Delay)
        if (state.pendingDomainRequests.isNotEmpty()) {
            item(key = "pending_changes_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, LemonColors.WarningLight.copy(alpha = 0.35f)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pending Changes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            GlassStatusBadge(
                                text = "${state.pendingDomainRequests.size} queued",
                                statusColor = LemonColors.WarningLight
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        state.pendingDomainRequests.forEach { request ->
                            PendingRequestRow(
                                request = request,
                                onCancel = { viewModel.cancelPendingRequest(request.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // 6. Custom Rules Section Header & Add Domain
        item(key = "custom_domains_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Rules",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = { showAddDomainDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add domain", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // 7. Tabs: Blocklist / Allowlist
        item(key = "domain_tabs") {
            TabRow(
                selectedTabIndex = if (state.selectedRuleTab == "BLOCK") 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (state.selectedRuleTab == "BLOCK") 0 else 1]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = state.selectedRuleTab == "BLOCK",
                    onClick = { viewModel.setSelectedRuleTab("BLOCK") },
                    text = {
                        Text(
                            text = "Blocklist (${blockedRules.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.selectedRuleTab == "BLOCK") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = state.selectedRuleTab == "ALLOW",
                    onClick = { viewModel.setSelectedRuleTab("ALLOW") },
                    text = {
                        Text(
                            text = "Allowlist (${allowedRules.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.selectedRuleTab == "ALLOW") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // 8. Domain Search Field
        item(key = "domain_search") {
            OutlinedTextField(
                value = state.domainFilterQuery,
                onValueChange = viewModel::setDomainFilterQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Filter domains...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                trailingIcon = {
                    if (state.domainFilterQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setDomainFilterQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // 9. Domain Rows
        if (filteredRules.isEmpty()) {
            item(key = "empty_domains") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.selectedRuleTab == "BLOCK") "No blocked domains" else "No allowed domains",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = filteredRules,
                key = { it.domain }
            ) { rule ->
                DomainRuleRow(
                    rule = rule,
                    onDelete = {
                        if (activity != null) {
                            if (rule.ruleType == "BLOCK") {
                                viewModel.requestRemoveBlocklistDomain(activity, rule.domain)
                            } else {
                                viewModel.requestRemoveAllowlistDomain(activity, rule.domain)
                            }
                        }
                    }
                )
            }
        }
    }

    // Add Domain Dialog
    if (showAddDomainDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDomainDialog = false
                domainInputText = ""
            },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (state.selectedRuleTab == "BLOCK") "Add to Blocklist" else "Add to Allowlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (state.selectedRuleTab == "BLOCK")
                            "Biometric verification required. Takes effect immediately."
                        else
                            "Biometric verification required. 30-day delay applies before allowing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = domainInputText,
                        onValueChange = { domainInputText = it },
                        placeholder = { Text("example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = !isBulkInput,
                        maxLines = if (isBulkInput) 6 else 1,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Multiple lines (bulk)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = isBulkInput,
                            onCheckedChange = { isBulkInput = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = domainInputText.trim()
                        if (input.isNotEmpty() && activity != null) {
                            if (isBulkInput) {
                                viewModel.addBulkDomains(input, state.selectedRuleTab)
                            } else {
                                if (state.selectedRuleTab == "BLOCK") {
                                    viewModel.requestAddBlocklistDomain(activity, input)
                                } else {
                                    viewModel.requestAddAllowlistDomain(activity, input)
                                }
                            }
                            showAddDomainDialog = false
                            domainInputText = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDomainDialog = false
                        domainInputText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FeedToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun PendingRequestRow(
    request: DomainChangeRequestEntity,
    onCancel: () -> Unit
) {
    val remainingHours = remember(request.effectiveAtEpochMillis) {
        val diff = request.effectiveAtEpochMillis - System.currentTimeMillis()
        val hours = (diff / (1000 * 3600)).coerceAtLeast(0)
        hours
    }
    val days = remainingHours / 24
    val hours = remainingHours % 24
    val countdownText = if (days > 0) "${days}d ${hours}h left" else "${hours}h left"

    val actionLabel = if (request.operation == DomainChangeOperation.REMOVE_BLOCK) {
        "Remove block: ${request.normalizedDomain}"
    } else {
        "Allow domain: ${request.normalizedDomain}"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Waiting: $countdownText",
                    style = MaterialTheme.typography.bodySmall,
                    color = LemonColors.WarningLight,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DomainRuleRow(
    rule: DomainRuleEntity,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.domain,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = rule.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
