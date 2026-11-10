package com.coconut.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.coconut.R
import com.coconut.data.DomainChangeOperation
import com.coconut.data.DomainChangeRequestEntity
import com.coconut.data.DomainRuleEntity
import com.coconut.dns.DnsUpstreamPresets
import com.coconut.policy.VpnRuntimeState
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
import com.coconut.ui.components.LemonSwitch

/**
 * Pristine Redesigned Local DNS Screen conforming to the Fruits Family Quiet Utility Design System.
 * - Geometric Unbounded header with official Lemon brand shield
 * - Unified Engine Master Card with telemetry well
 * - Inset grouped containers for threat feeds, schedules, and custom rules
 * - Safe bottom padding (120dp) preventing bottom dock clipping
 */
@Composable
fun DnsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showAddDomainDialog by remember { mutableStateOf(false) }
    var domainInputText by remember { mutableStateOf("") }
    var isBulkInput by remember { mutableStateOf(false) }
    var showUpstreamDialog by remember { mutableStateOf(false) }

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

    // Determine authoritative high-level status badge
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
        item(key = "dns_header") {
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
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.brandAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "DNS Shield",
                                style = LemonTheme.typography.displayLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                        }
                    }

                    LemonBadge(
                        text = statusLabel,
                        style = statusStyle,
                        showDot = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (state.vpnRuntimeState) {
                        VpnRuntimeState.PROTECTED -> "On-device packet filter operational."
                        VpnRuntimeState.STARTING, VpnRuntimeState.RECOVERING -> "Connecting local DNS tunnel..."
                        VpnRuntimeState.REVOKED -> "VPN consent revoked by Android system."
                        VpnRuntimeState.FAILED -> "Tunnel establishment failed."
                        else -> if (state.isInsideNightSchedule) "Enforced by nightly schedule (23:00–06:00)."
                        else if (state.isQuarantineActive) "Enforced by active Quarantine session."
                        else "On-device packet filter and zero-logging DNS firewall."
                    },
                    style = LemonTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = LemonTheme.colors.textSecondary
                )
            }
        }

        // 2. VPN Revocation Warning Banner (if revoked)
        if (state.vpnRuntimeState == VpnRuntimeState.REVOKED) {
            item(key = "vpn_recovery_card") {
                Surface(
                    shape = LemonTheme.shapes.card,
                    color = LemonTheme.colors.dangerContainer,
                    border = BorderStroke(1.dp, LemonTheme.colors.danger.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                    .background(LemonTheme.colors.danger.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.danger,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "VPN Consent Required",
                                    style = LemonTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = LemonTheme.colors.onDangerContainer
                                )
                                Text(
                                    text = "Re-grant VPN consent in Android settings",
                                    style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = LemonTheme.colors.onDangerContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = LemonTheme.colors.danger,
                            modifier = Modifier.clickable {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnPrepareLauncher.launch(prepareIntent)
                                } else {
                                    viewModel.toggleDns(true)
                                }
                            }
                        ) {
                            Text(
                                text = "Restore",
                                style = LemonTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Compact One-Time Confirmation Banner (only if unconfirmed)
        if (!state.manualDnsConfirmed) {
            item(key = "one_time_confirmation") {
                Surface(
                    shape = LemonTheme.shapes.card,
                    color = LemonTheme.colors.cardSecondaryBackground,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(LemonTheme.shapes.iconSquircle)
                                    .background(LemonTheme.colors.brandYellow.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.brandAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daytime Protection",
                                    style = LemonTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = LemonTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Confirm to activate automatic daytime filtering",
                                    style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = LemonTheme.colors.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = LemonTheme.colors.primaryAccent,
                            modifier = Modifier.clickable { viewModel.confirmManualDnsPreference(keepAlwaysOn = false) }
                        ) {
                            Text(
                                text = "Confirm",
                                style = LemonTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = LemonTheme.colors.onPrimaryAccent,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Master Engine Hero Card
        item(key = "master_card") {
            LemonCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(LemonTheme.shapes.iconSquircle)
                                .background(LemonTheme.colors.brandYellow.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = LemonTheme.colors.brandAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "DNS Protection",
                                style = LemonTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                            Text(
                                text = if (state.isVpnRunning) "Encrypted loopback tunnel active" else "Protection is currently off",
                                style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = LemonTheme.colors.textSecondary
                            )
                        }
                    }

                    LemonSwitch(
                        checked = state.isVpnRunning,
                        onCheckedChange = { enable ->
                            if (enable) {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnPrepareLauncher.launch(prepareIntent)
                                } else {
                                    viewModel.toggleDns(true)
                                }
                            } else {
                                viewModel.toggleDns(false)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Inset Telemetry Strip (Kiwi Metric Delta Well)
                LemonMetricWell(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
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
                                text = "10.255.0.2",
                                style = LemonTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = LemonTheme.colors.textPrimary
                            )
                            Text(
                                text = "LOOPBACK",
                                style = LemonTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = LemonTheme.colors.textTertiary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(22.dp)
                                .width(1.dp)
                                .background(LemonTheme.colors.separator)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "0 ms",
                                style = LemonTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = LemonTheme.colors.success
                            )
                            Text(
                                text = "LATENCY",
                                style = LemonTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = LemonTheme.colors.textTertiary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(22.dp)
                                .width(1.dp)
                                .background(LemonTheme.colors.separator)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Zero Logs",
                                style = LemonTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = LemonTheme.colors.textPrimary
                            )
                            Text(
                                text = "LOCAL ONLY",
                                style = LemonTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = LemonTheme.colors.textTertiary
                            )
                        }
                    }
                }
            }
        }

        // 5. Threat Intelligence Feeds Section
        item(key = "threat_intel_eyebrow") {
            LemonSectionEyebrow(title = "THREAT INTELLIGENCE (HAGEZI)")
        }
        item(key = "threat_intel_card") {
            LemonGroupedContainer {
                LemonGroupedRow(
                    title = "Malicious & Phishing",
                    subtitle = "Block malware, command & control, and phishing",
                    showDivider = true,
                    trailingContent = {
                        LemonSwitch(
                            checked = state.dnsConfig?.blockThreats ?: true,
                            onCheckedChange = { viewModel.updateDnsConfiguration(blockThreats = it) }
                        )
                    }
                )
                LemonGroupedRow(
                    title = "Adult & Explicit Content",
                    subtitle = "Enforce SafeSearch and adult domain restriction",
                    showDivider = true,
                    trailingContent = {
                        LemonSwitch(
                            checked = state.dnsConfig?.blockAdultContent ?: false,
                            onCheckedChange = { viewModel.updateDnsConfiguration(blockAdultContent = it) }
                        )
                    }
                )
                LemonGroupedRow(
                    title = "Trackers & Telemetry",
                    subtitle = "Block telemetry trackers and behavioral profiling",
                    showDivider = false,
                    trailingContent = {
                        LemonSwitch(
                            checked = state.dnsConfig?.blockAdsTrackers ?: false,
                            onCheckedChange = { viewModel.updateDnsConfiguration(blockAds = it) }
                        )
                    }
                )
            }
        }

        // 6. Schedule & Upstream Resolver Section
        item(key = "schedule_eyebrow") {
            LemonSectionEyebrow(title = "SCHEDULE & UPSTREAM")
        }
        item(key = "schedule_card") {
            LemonGroupedContainer {
                LemonGroupedRow(
                    title = "Night Schedule (23:00 – 06:00)",
                    subtitle = "Automatically enforce protection during sleep",
                    showDivider = true,
                    trailingContent = {
                        LemonSwitch(
                            checked = state.dnsSchedule?.enabled ?: false,
                            onCheckedChange = viewModel::setScheduleEnabled
                        )
                    }
                )

                val activePresetName = DnsUpstreamPresets.PRESETS.find {
                    it.id == (state.dnsConfig?.primaryUpstream ?: "SYSTEM")
                }?.displayName ?: "Quad9 (Malware Blocking)"

                LemonGroupedRow(
                    title = "Upstream Resolver",
                    subtitle = activePresetName,
                    showDivider = false,
                    onClick = { showUpstreamDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select resolver",
                            tint = LemonTheme.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }

        // 7. Pending 30-Day Requests (if any)
        if (state.pendingDomainRequests.isNotEmpty()) {
            item(key = "pending_requests_eyebrow") {
                LemonSectionEyebrow(title = "PENDING CHANGES (${state.pendingDomainRequests.size})")
            }
            item(key = "pending_requests_container") {
                LemonGroupedContainer {
                    state.pendingDomainRequests.forEachIndexed { index, req ->
                        PendingChangeRow(
                            request = req,
                            showDivider = index < state.pendingDomainRequests.lastIndex,
                            onCancel = { viewModel.cancelPendingRequest(req.id) }
                        )
                    }
                }
            }
        }

        // 8. Custom Rules Header & Controls
        item(key = "custom_rules_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUSTOM RULES",
                    style = LemonTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = LemonTheme.colors.textTertiary
                )
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = LemonTheme.colors.cardSecondaryBackground,
                    border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                    modifier = Modifier.clickable { showAddDomainDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add rule",
                            tint = LemonTheme.colors.textPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Rule",
                            style = LemonTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = LemonTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        item(key = "rules_controls_block") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LemonSegmentedControl(
                    items = listOf(
                        LemonSegmentItem("BLOCK", "Blocklist", blockedRules.size),
                        LemonSegmentItem("ALLOW", "Allowlist", allowedRules.size)
                    ),
                    selectedId = state.selectedRuleTab,
                    onItemSelected = viewModel::setSelectedRuleTab
                )

                LemonSearchField(
                    query = state.domainFilterQuery,
                    onQueryChange = viewModel::setDomainFilterQuery,
                    placeholder = "Filter ${if (state.selectedRuleTab == "BLOCK") "blocked" else "allowed"} domains..."
                )
            }
        }

        // 9. Grouped Domain Rules List
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
            item(key = "grouped_rules_container") {
                LemonGroupedContainer {
                    filteredRules.forEachIndexed { index, rule ->
                        LemonGroupedRow(
                            title = rule.domain,
                            subtitle = if (rule.source.equals("CUSTOM", ignoreCase = true)) "Custom rule" else rule.source,
                            showDivider = index < filteredRules.lastIndex,
                            trailingContent = {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = LemonTheme.colors.cardSecondaryBackground,
                                    border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                                    modifier = Modifier.clickable {
                                        if (activity != null) {
                                            if (rule.ruleType == "BLOCK") {
                                                viewModel.requestRemoveBlocklistDomain(activity, rule.domain)
                                            } else {
                                                viewModel.requestRemoveAllowlistDomain(activity, rule.domain)
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Remove...",
                                        style = LemonTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = LemonTheme.colors.danger,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        )
                    }
                }
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
                        fontSize = 18.sp,
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
                            focusedBorderColor = LemonTheme.colors.brandAmber,
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
                    style = LemonTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
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
                                .clip(LemonTheme.shapes.iconSquircle)
                                .clickable { selectedPresetId = preset.id }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPresetId = preset.id },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = LemonTheme.colors.brandAmber
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preset.displayName,
                                    style = LemonTheme.typography.titleSmall.copy(
                                        fontSize = 14.sp,
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
                            shape = LemonTheme.shapes.button,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LemonTheme.colors.brandAmber,
                                cursorColor = LemonTheme.colors.brandAmber
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
private fun PendingChangeRow(
    request: DomainChangeRequestEntity,
    showDivider: Boolean,
    onCancel: () -> Unit
) {
    val remainingHours = remember(request.effectiveAtEpochMillis) {
        val diff = request.effectiveAtEpochMillis - System.currentTimeMillis()
        (diff / (1000 * 3600)).coerceAtLeast(0)
    }
    val days = remainingHours / 24
    val hours = remainingHours % 24
    val countdownText = if (days > 0) "${days}d ${hours}h left" else "${hours}h left"

    val actionLabel = if (request.operation == DomainChangeOperation.REMOVE_BLOCK) {
        "Unblock: ${request.normalizedDomain}"
    } else {
        "Allow: ${request.normalizedDomain}"
    }

    LemonGroupedRow(
        title = actionLabel,
        subtitle = "Enforces in $countdownText",
        showDivider = showDivider,
        trailingContent = {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = LemonTheme.colors.cardSecondaryBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.clickable(onClick = onCancel)
            ) {
                Text(
                    text = "Cancel",
                    style = LemonTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = LemonTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    )
}
