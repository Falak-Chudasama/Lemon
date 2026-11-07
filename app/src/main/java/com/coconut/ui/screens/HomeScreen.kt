package com.coconut.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coconut.R
import com.coconut.data.SnapState
import com.coconut.ui.CoconutColors
import com.coconut.ui.MainUiState
import com.coconut.ui.NavigationTab
import com.coconut.ui.components.GlassCard
import com.coconut.ui.components.GlassStatusBadge
import com.coconut.ui.components.GlassSurface

@Composable
fun HomeScreen(
    state: MainUiState,
    onNavigateTab: (NavigationTab) -> Unit,
    onReconcileNow: () -> Unit,
    onOpenSetup: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val totalSnapped = state.snappedApps.size
    val activeProtected = state.snappedApps.count { it.actualState == SnapState.SNAP_ACTIVE }
    val pendingCount = state.snappedApps.count { it.actualState == SnapState.PENDING_ENFORCEMENT }
    val failedCount = state.snappedApps.count { it.actualState == SnapState.SNAP_FAILED }

    val shieldConfig = state.protectionConfig
    val shieldEnabled = shieldConfig?.contentShieldEnabled == true
    val isDnsEnforced = state.isDeviceOwner && state.privateDnsStatus?.isEnforced == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. BRAND HEADER & STATUS PILL ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_coconut_logo),
                    contentDescription = "Coconut Logo",
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Coconut",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Hardware-Enforced Focus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Clickable status pill to view setup wizard
            val managementPillColor = if (state.isDeviceOwner) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber
            val managementPillText = if (state.isDeviceOwner) "Device Owner" else "Setup Needed"
            GlassStatusBadge(
                text = managementPillText,
                statusColor = managementPillColor,
                modifier = Modifier.clickable(onClick = onOpenSetup)
            )
        }

        // --- 2. SUMMARY HERO CARD ---
        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Protection Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (!state.isDeviceOwner) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = CoconutColors.AwaitingAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pending ADB Provisioning",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CoconutColors.AwaitingAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "$activeProtected",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (totalSnapped == activeProtected) "Apps Enforced" else "$activeProtected of $totalSnapped Enforced",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val shieldStatusColor = if (isDnsEnforced) CoconutColors.ProtectedGreen else if (shieldEnabled) CoconutColors.AwaitingAmber else MaterialTheme.colorScheme.outline
                        val shieldStatusText = if (isDnsEnforced) "DNS Filter Active" else if (shieldEnabled) "DNS Pending DO" else "DNS Filter Off"

                        GlassStatusBadge(
                            text = shieldStatusText,
                            statusColor = shieldStatusColor,
                            showDot = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (shieldEnabled) shieldConfig.dnsProviderName else "Default DNS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // --- 3. RECOVERY OR PRIVILEGE WARNING BANNER ---
        if (!state.isDeviceOwner) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CoconutColors.AwaitingAmber.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSetup)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CoconutColors.AwaitingAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Device Owner Not Active",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to view the ADB provisioning command required for native suspension.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else if (failedCount > 0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CoconutColors.SnappedRed.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CoconutColors.SnappedRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$failedCount Snap Enforcement Issues",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CoconutColors.SnappedRed
                        )
                        Text(
                            text = "Some packages refused suspension. Run policy sync to resolve.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // --- 4. FEATURE NAVIGATION CARDS ---
        FeatureGlassCard(
            icon = Icons.Default.Lock,
            title = "The Snap",
            subtitle = if (totalSnapped == 0) "Permanent app blacklisting" else "$totalSnapped apps in permanent registry",
            badgeText = if (activeProtected > 0) "$activeProtected Active" else null,
            badgeColor = CoconutColors.ProtectedGreen,
            onClick = { onNavigateTab(NavigationTab.THE_SNAP) }
        )

        FeatureGlassCard(
            icon = Icons.Default.Shield,
            title = "Content Shield",
            subtitle = if (shieldEnabled) "DNS filter: ${shieldConfig.dnsProviderName}" else "Device-wide DNS adult content filtering",
            badgeText = if (isDnsEnforced) "Enforced" else if (shieldEnabled) "Pending DO" else "OFF",
            badgeColor = if (isDnsEnforced) CoconutColors.ProtectedGreen else if (shieldEnabled) CoconutColors.AwaitingAmber else MaterialTheme.colorScheme.outline,
            onClick = { onNavigateTab(NavigationTab.CONTENT_SHIELD) }
        )

        FeatureGlassCard(
            icon = Icons.Default.Security,
            title = "Settings & Privilege Management",
            subtitle = if (state.isDeviceOwner) "Device Owner active • Hardware policies" else "Setup instructions • Emergency recovery",
            badgeText = null,
            badgeColor = CoconutColors.ProtectedGreen,
            onClick = { onNavigateTab(NavigationTab.SETTINGS) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // --- 5. RECONCILE ACTION BUTTON ---
        OutlinedButton(
            onClick = onReconcileNow,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verifying Policy Enforcement...")
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync & Verify Policies", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun FeatureGlassCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String?,
    badgeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassStatusBadge(text = badgeText, statusColor = badgeColor, showDot = false)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
