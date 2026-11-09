package com.coconut.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.coconut.data.DomainChangeOperation
import com.coconut.data.DomainChangeRequestEntity
import com.coconut.data.DomainRuleEntity
import com.coconut.dns.DnsUpstreamPresets
import com.coconut.policy.VpnRuntimeState
import com.coconut.ui.LemonTheme
import com.coconut.ui.MainViewModel
import com.coconut.ui.components.LemonBadge
import com.coconut.ui.components.LemonBadgeStyle
import com.coconut.ui.components.LemonEmptyState
import com.coconut.ui.components.LemonSearchField
import com.coconut.ui.components.LemonSegmentItem
import com.coconut.ui.components.LemonSegmentedControl
import com.coconut.ui.components.LemonSwitch

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

    // Determine authoritative high-level status with genuine runtime verification
    val (statusLabel, statusStyle) = when (state.vpnRuntimeState) {
        VpnRuntimeState.PROTECTED -> "Protected" to LemonBadgeStyle.SUCCESS
        VpnRuntimeState.STARTING, VpnRuntimeState.RECOVERING -> "Starting" to LemonBadgeStyle.INFO
        VpnRuntimeState.REVOKED -> "Revoked" to LemonBadgeStyle.DANGER
        VpnRuntimeState.FAILED -> "Failed" to LemonBadgeStyle.DANGER
        VpnRuntimeState.RUNNING -> "Tunnel Open" to LemonBadgeStyle.INFO
        else -> {
            if (state.isInsideNightSchedule) "Scheduled" to LemonBadgeStyle.SCHEDULED
            else if (state.isQuarantineActive) "Quarantine" to LemonBadgeStyle.SCHEDULED
            else "Off" to LemonBadgeStyle.INACTIVE
        }
    }

    var showUpstreamDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status bar top spacing
        item(key = "status_bar_spacing") {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }

        // 1. Page Header with Single Clear Status Hierarchy (Responsive, no text wrapping)
        item(key = "dns_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "DNS Protection",
                        style = LemonTheme.typography.displayLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (state.vpnRuntimeState) {
                            VpnRuntimeState.PROTECTED -> "On-device packet filter operational"
                            VpnRuntimeState.STARTING, VpnRuntimeState.RECOVERING -> "Connecting local DNS tunnel..."
                            VpnRuntimeState.REVOKED -> "VPN consent revoked by Android system"
                            VpnRuntimeState.FAILED -> "Tunnel establishment failed"
                            else -> if (state.isInsideNightSchedule) "Enforced by nightly schedule (23:00–06:00)"
                            else if (state.isQuarantineActive) "Enforced by active Quarantine session"
                            else "Protection is currently disabled"
                        },
                        style = LemonTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = LemonTheme.colors.textSecondary
                    )
                }

                LemonBadge(
                    text = statusLabel,
                    style = statusStyle,
                    showDot = true,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // 2. VPN Revocation / Deletion Recovery Card (Requirement 6)
        if (state.vpnRuntimeState == VpnRuntimeState.REVOKED) {
            item(key = "vpn_recovery_card") {
                Surface(
                    shape = LemonTheme.shapes.card,
                    color = LemonTheme.colors.dangerContainer,
                    border = BorderStroke(1.dp, LemonTheme.colors.danger.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = LemonTheme.colors.danger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VPN Consent Required",
                                style = LemonTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.onDangerContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Lemon's VPN configuration was deleted or consent revoked in Android Settings. Re-grant permission to restore encrypted on-device filtering.",
                            style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = LemonTheme.colors.onDangerContainer.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnPrepareLauncher.launch(prepareIntent)
                                } else {
                                    viewModel.toggleDns(true)
                                }
                            },
                            shape = LemonTheme.shapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LemonTheme.colors.danger,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Restore Protection",
                                style = LemonTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // 2.5 One-time Upgrade Confirmation Card (if not yet confirmed)
        if (!state.manualDnsConfirmed) {
            item(key = "one_time_confirmation_card") {
                Surface(
                    shape = LemonTheme.shapes.card,
                    color = LemonTheme.colors.cardBackground,
                    border = BorderStroke(1.dp, LemonTheme.colors.primaryAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = LemonTheme.colors.primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Confirm Daytime Protection",
                                style = LemonTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = LemonTheme.colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lemon is upgrading your DNS engine. Would you like DNS protection active 24/7 or only during your scheduled nightly hours (11:00 PM – 6:00 AM)?",
                            style = LemonTheme.typography.bodySmall,
                            color = LemonTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.confirmManualDnsPreference(true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = LemonTheme.colors.primaryAccent)
                            ) {
                                Text("Always On (24/7)", style = LemonTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { viewModel.confirmManualDnsPreference(false) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Night Only", style = LemonTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 3. Master DNS Filtering Switch Card (Separate Intent from Runtime Status)
        item(key = "master_dns_card") {
            val isLockedOn = state.isInsideNightSchedule || state.isQuarantineActive
            val isChecked = state.manualDnsPreference || state.effectiveDnsDesired || state.isVpnRunning

            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DNS Filtering",
                            style = LemonTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = LemonTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isLockedOn) {
                            val reason = if (state.isQuarantineActive) "Quarantine is active" else "Night schedule active (11:00 PM – 6:00 AM)"
                            Text(
                                text = "Locked ON: $reason",
                                style = LemonTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = LemonTheme.colors.textAccent
                            )
                        } else {
                            Text(
                                text = "User preference: filtering ${if (state.manualDnsPreference) "Enabled" else "Disabled"}",
                                style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = LemonTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    LemonSwitch(
                        checked = isChecked,
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
                        enabled = !isLockedOn
                    )
                }
            }
        }

        // 3. Permanent Daily DNS Protection Schedule Card (11:00 PM – 6:00 AM)
        item(key = "daily_dns_schedule_card") {
            val isInside = state.isInsideNightSchedule

            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(LemonTheme.colors.primaryAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.textAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Daily DNS Protection",
                                style = LemonTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "11:00 PM – 6:00 AM",
                            style = LemonTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = LemonTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Repeats every day. Protection cannot be paused or edited.",
                            style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = LemonTheme.colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    LemonBadge(
                        text = if (isInside) "Active" else "Scheduled",
                        style = if (isInside) LemonBadgeStyle.SUCCESS else LemonBadgeStyle.SCHEDULED,
                        showDot = true
                    )
                }
            }
        }

        // 4. Built-in Feeds Card (HaGeZi Feeds: TIF Mini, NSFW, Multi PRO Mini)
        item(key = "builtin_feeds_card") {
            val config = state.dnsConfig

            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Built-in Protection",
                        style = LemonTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Comprehensive feeds maintained by HaGeZi",
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = LemonTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feed 1: Threat Intelligence (TIF Mini)
                    LemonFeedRow(
                        title = "Threat Intelligence",
                        subtitle = "HaGeZi TIF Mini • Malware & malicious C2",
                        checked = config?.blockThreats ?: true,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockThreats = it) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = LemonTheme.colors.separator
                    )

                    // Feed 2: Adult Content (NSFW)
                    LemonFeedRow(
                        title = "Adult Content",
                        subtitle = "HaGeZi NSFW • Pornography & adult sites",
                        checked = config?.blockAdultContent ?: false,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockAdultContent = it) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = LemonTheme.colors.separator
                    )

                    // Feed 3: Ads & Trackers (Multi PRO Mini)
                    LemonFeedRow(
                        title = "Ads & Trackers",
                        subtitle = "HaGeZi Multi PRO Mini • Behavioral trackers & ads",
                        checked = config?.blockAdsTrackers ?: false,
                        onCheckedChange = { viewModel.updateDnsConfiguration(blockAds = it) }
                    )
                }
            }
        }

        // 5. Upstream DNS Provider Card (Requirement 7 / Part 9)
        item(key = "upstream_dns_card") {
            val currentUpstreamId = state.dnsConfig?.primaryUpstream ?: "SYSTEM"
            val preset = DnsUpstreamPresets.getPreset(currentUpstreamId)
            val customEndpoint = state.dnsConfig?.customUpstreamEndpoint ?: ""

            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(LemonTheme.colors.primaryAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.textAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Upstream DNS",
                                style = LemonTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                        }

                        Button(
                            onClick = { showUpstreamDialog = true },
                            shape = LemonTheme.shapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LemonTheme.colors.primaryAccent,
                                contentColor = LemonTheme.colors.onPrimaryAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Configure",
                                style = LemonTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (currentUpstreamId == "CUSTOM") "Custom: $customEndpoint" else preset.displayName,
                        style = LemonTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (currentUpstreamId == "CUSTOM") "Custom encrypted/standard upstream resolver" else preset.description,
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = LemonTheme.colors.textSecondary
                    )
                }
            }
        }

        // 6. Pending Changes Queue Card (Exact 30-Day Delay)
        if (state.pendingDomainRequests.isNotEmpty()) {
            item(key = "pending_changes_card") {
                Surface(
                    shape = LemonTheme.shapes.card,
                    color = LemonTheme.colors.warningContainer,
                    border = BorderStroke(1.dp, LemonTheme.colors.warning.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.warning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pending Changes",
                                    style = LemonTheme.typography.titleMedium.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = LemonTheme.colors.onWarningContainer
                                )
                            }
                            LemonBadge(
                                text = "${state.pendingDomainRequests.size} queued",
                                style = LemonBadgeStyle.WARNING,
                                showDot = false
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        state.pendingDomainRequests.forEachIndexed { index, request ->
                            LemonPendingRequestRow(
                                request = request,
                                onCancel = { viewModel.cancelPendingRequest(request.id) }
                            )
                            if (index < state.pendingDomainRequests.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
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
                    style = LemonTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LemonTheme.colors.textPrimary
                )

                Button(
                    onClick = { showAddDomainDialog = true },
                    shape = LemonTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LemonTheme.colors.primaryAccent,
                        contentColor = LemonTheme.colors.onPrimaryAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add domain",
                        style = LemonTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // 7. Tabs: Blocklist / Allowlist via Segmented Control
        item(key = "domain_tabs") {
            val tabs = listOf(
                LemonSegmentItem(
                    id = "BLOCK",
                    title = "Blocklist",
                    count = blockedRules.size
                ),
                LemonSegmentItem(
                    id = "ALLOW",
                    title = "Allowlist",
                    count = allowedRules.size
                )
            )
            LemonSegmentedControl(
                items = tabs,
                selectedId = state.selectedRuleTab,
                onItemSelected = { viewModel.setSelectedRuleTab(it) }
            )
        }

        // 8. Domain Search Field
        item(key = "domain_search") {
            LemonSearchField(
                query = state.domainFilterQuery,
                onQueryChange = viewModel::setDomainFilterQuery,
                placeholder = "Filter domain rules..."
            )
        }

        // 9. Domain Rows or Empty State
        if (filteredRules.isEmpty()) {
            item(key = "empty_domains") {
                LemonEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = if (state.selectedRuleTab == "BLOCK") "No blocked domains" else "No allowed domains",
                    description = if (state.domainFilterQuery.isNotEmpty())
                        "No rules match '${state.domainFilterQuery}'."
                    else
                        "You haven't added any custom domains to this list yet.",
                    actionLabel = if (state.domainFilterQuery.isNotEmpty()) "Clear filter" else "Add domain",
                    onActionClick = {
                        if (state.domainFilterQuery.isNotEmpty()) {
                            viewModel.setDomainFilterQuery("")
                        } else {
                            showAddDomainDialog = true
                        }
                    }
                )
            }
        } else {
            items(
                items = filteredRules,
                key = { it.domain }
            ) { rule ->
                LemonDomainRuleRow(
                    rule = rule,
                    onRemove = {
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
        val isBlock = state.selectedRuleTab == "BLOCK"
        AlertDialog(
            onDismissRequest = {
                showAddDomainDialog = false
                domainInputText = ""
            },
            shape = LemonTheme.shapes.dialog,
            containerColor = LemonTheme.colors.cardBackground,
            title = {
                Text(
                    text = if (isBlock) "Add to Blocklist" else "Add to Allowlist",
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
                        text = if (isBlock)
                            "Biometric verification required. Takes effect immediately."
                        else
                            "Biometric verification required. 30-day delay applies before domain is allowed.",
                        style = LemonTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = LemonTheme.colors.textSecondary
                    )

                    OutlinedTextField(
                        value = domainInputText,
                        onValueChange = { domainInputText = it },
                        placeholder = { Text("example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = !isBulkInput,
                        maxLines = if (isBulkInput) 6 else 1,
                        shape = LemonTheme.shapes.button,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            autoCorrectEnabled = false,
                            imeAction = if (isBulkInput) ImeAction.Default else ImeAction.Done
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Multiple lines (bulk import)",
                            style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = LemonTheme.colors.textPrimary
                        )
                        LemonSwitch(
                            checked = isBulkInput,
                            onCheckedChange = { isBulkInput = it }
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
                    shape = LemonTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LemonTheme.colors.primaryAccent,
                        contentColor = LemonTheme.colors.onPrimaryAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Save",
                        style = LemonTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDomainDialog = false
                        domainInputText = ""
                    },
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

    // Upstream DNS Configuration Dialog
    if (showUpstreamDialog) {
        val currentPresetId = state.dnsConfig?.primaryUpstream ?: "SYSTEM"
        var selectedPresetId by remember { mutableStateOf(currentPresetId) }
        var customEndpointInput by remember { mutableStateOf(state.dnsConfig?.customUpstreamEndpoint ?: "") }

        AlertDialog(
            onDismissRequest = { showUpstreamDialog = false },
            shape = LemonTheme.shapes.dialog,
            containerColor = LemonTheme.colors.cardBackground,
            title = {
                Text(
                    text = "Select Upstream DNS",
                    style = LemonTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = LemonTheme.colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DnsUpstreamPresets.PRESETS.forEach { preset ->
                        val isSelected = selectedPresetId == preset.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedPresetId = preset.id }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPresetId = preset.id },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = LemonTheme.colors.primaryAccent
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preset.displayName,
                                    style = LemonTheme.typography.titleSmall.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = LemonTheme.colors.textPrimary
                                )
                                Text(
                                    text = preset.description,
                                    style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = LemonTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    if (selectedPresetId == "CUSTOM") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customEndpointInput,
                            onValueChange = { customEndpointInput = it },
                            label = { Text("Server IP address") },
                            placeholder = { Text("e.g. 94.140.14.14") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LemonTheme.colors.primaryAccent,
                                cursorColor = LemonTheme.colors.primaryAccent
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val preset = DnsUpstreamPresets.getPreset(selectedPresetId)
                        viewModel.updateUpstreamConfig(
                            primary = selectedPresetId,
                            secondary = if (selectedPresetId != "CUSTOM") preset.secondaryIp ?: "" else "",
                            custom = customEndpointInput.trim(),
                            protocol = "SYSTEM"
                        )
                        showUpstreamDialog = false
                    },
                    shape = LemonTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LemonTheme.colors.primaryAccent,
                        contentColor = LemonTheme.colors.onPrimaryAccent
                    )
                ) {
                    Text("Apply Provider", style = LemonTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpstreamDialog = false },
                    shape = LemonTheme.shapes.button
                ) {
                    Text("Cancel", style = LemonTheme.typography.labelMedium, color = LemonTheme.colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun LemonFeedRow(
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
                style = LemonTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = LemonTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = LemonTheme.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        LemonSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun LemonPendingRequestRow(
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
        color = LemonTheme.colors.cardBackground.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionLabel,
                    style = LemonTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = LemonTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Enforces in $countdownText",
                    style = LemonTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = LemonTheme.colors.textAccent
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onCancel,
                shape = LemonTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LemonTheme.colors.cardSecondaryBackground,
                    contentColor = LemonTheme.colors.textPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Cancel",
                    style = LemonTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * Domain rule row with respectful "Remove..." action button instead of destructive immediate trash icon.
 * Reflects that removal incurs a 30-day delay for user protection.
 */
@Composable
private fun LemonDomainRuleRow(
    rule: DomainRuleEntity,
    onRemove: () -> Unit
) {
    Surface(
        shape = LemonTheme.shapes.card,
        color = LemonTheme.colors.cardBackground,
        border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
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
                    style = LemonTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = LemonTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (rule.source.equals("CUSTOM", ignoreCase = true)) "Custom rule" else rule.source,
                    style = LemonTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = LemonTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Subdued "Remove..." button communicating delayed security workflow
            Button(
                onClick = onRemove,
                shape = LemonTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LemonTheme.colors.cardSecondaryBackground,
                    contentColor = LemonTheme.colors.danger
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Remove...",
                    style = LemonTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
