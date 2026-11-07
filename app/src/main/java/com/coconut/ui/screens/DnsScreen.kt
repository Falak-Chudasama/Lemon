package com.coconut.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coconut.data.DomainRuleEntity
import com.coconut.ui.CoconutColors
import com.coconut.ui.MainViewModel

@Composable
fun DnsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showBulkDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header and Master Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Local DNS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (state.isVpnRunning) "Filtering active on-device" else "Filtering paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.isVpnRunning) CoconutColors.ProtectedGreen else MaterialTheme.colorScheme.outline
                )
            }

            Switch(
                checked = state.isVpnRunning,
                onCheckedChange = { enable ->
                    if (enable) {
                        val vpnIntent = VpnService.prepare(context)
                        if (vpnIntent != null) {
                            vpnPrepareLauncher.launch(vpnIntent)
                        } else {
                            viewModel.toggleDns(true)
                        }
                    } else {
                        viewModel.toggleDns(false)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Built-in Threat Protection Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Threat Intelligence",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "HaGeZi threat feeds (phishing, malware, scam)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = state.dnsConfig?.blockThreats ?: true,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockThreats = it) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adult Content",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "HaGeZi NSFW adult domain filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = state.dnsConfig?.blockAdultContent ?: false,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockAdultContent = it) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ads & Trackers",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Optional ad & telemetry blocklist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = state.dnsConfig?.blockAdsTrackers ?: false,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockAds = it) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.blocklistStatus ?: "Blocklists ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { viewModel.triggerBlocklistUpdate() },
                        enabled = !state.isUpdatingBlocklist
                    ) {
                        if (state.isUpdatingBlocklist) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Update Blocklists",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Private DNS Limitation Notice
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Notice: Android Private DNS must be set to 'Off' or 'Automatic' in Settings. Strict Private DNS hostnames bypass local VPN DNS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom Domain Rules Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                selectedTabIndex = if (state.selectedRuleTab == "BLOCK") 0 else 1,
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = state.selectedRuleTab == "BLOCK",
                    onClick = { viewModel.setSelectedRuleTab("BLOCK") },
                    text = {
                        Text(
                            text = "Blocked (${blockedRules.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (state.selectedRuleTab == "BLOCK") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = state.selectedRuleTab == "ALLOW",
                    onClick = { viewModel.setSelectedRuleTab("ALLOW") },
                    text = {
                        Text(
                            text = "Allowed (${allowedRules.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (state.selectedRuleTab == "ALLOW") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Domain",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Domain Input
        if (currentRules.size > 5) {
            OutlinedTextField(
                value = state.domainFilterQuery,
                onValueChange = viewModel::setDomainFilterQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search domain rules...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Domain Rule List
        if (filteredRules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.selectedRuleTab == "BLOCK") "No custom blocked domains" else "No custom allowed domains",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Add Domain", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = filteredRules,
                    key = { it.domain }
                ) { rule ->
                    DomainRuleRow(
                        rule = rule,
                        onDelete = { viewModel.deleteDomain(rule.domain) }
                    )
                }
            }
        }
    }

    // Add Domain Dialog
    if (showAddDialog) {
        AddDomainDialog(
            initialType = state.selectedRuleTab,
            onDismiss = { showAddDialog = false },
            onAddSingle = { domain, type ->
                viewModel.addDomain(domain, type)
                showAddDialog = false
            },
            onOpenBulk = {
                showAddDialog = false
                showBulkDialog = true
            }
        )
    }

    // Bulk Domain Dialog
    if (showBulkDialog) {
        BulkDomainDialog(
            initialType = state.selectedRuleTab,
            onDismiss = { showBulkDialog = false },
            onAddBulk = { text, type ->
                viewModel.addBulkDomains(text, type)
                showBulkDialog = false
            }
        )
    }
}

@Composable
private fun DomainRuleRow(
    rule: DomainRuleEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
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
                    text = rule.domain,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Subdomains included",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun AddDomainDialog(
    initialType: String,
    onDismiss: () -> Unit,
    onAddSingle: (String, String) -> Unit,
    onOpenBulk: () -> Unit
) {
    var domainInput by remember { mutableStateOf("") }
    var ruleType by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Domain Rule", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    label = { Text("Domain (e.g. reddit.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = ruleType == "BLOCK",
                        onClick = { ruleType = "BLOCK" },
                        label = { Text("Block") }
                    )
                    FilterChip(
                        selected = ruleType == "ALLOW",
                        onClick = { ruleType = "ALLOW" },
                        label = { Text("Allow") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onOpenBulk,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Paste multiple domains...", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddSingle(domainInput, ruleType) },
                enabled = domainInput.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BulkDomainDialog(
    initialType: String,
    onDismiss: () -> Unit,
    onAddBulk: (String, String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var ruleType by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Add Domains", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text(
                    text = "Paste newline-separated domains:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = ruleType == "BLOCK",
                        onClick = { ruleType = "BLOCK" },
                        label = { Text("Block") }
                    )
                    FilterChip(
                        selected = ruleType == "ALLOW",
                        onClick = { ruleType = "ALLOW" },
                        label = { Text("Allow") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddBulk(textInput, ruleType) },
                enabled = textInput.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

