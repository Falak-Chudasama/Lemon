package com.coconut.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coconut.dns.DnsProviders
import com.coconut.dns.DnsTestResult
import com.coconut.ui.CoconutColors
import com.coconut.ui.MainUiState

@Composable
fun ContentShieldScreen(
    state: MainUiState,
    onToggleShield: (Boolean) -> Unit,
    onSelectProvider: (String) -> Unit,
    onTestDns: () -> Unit
) {
    val scrollState = rememberScrollState()
    val shieldConfig = state.protectionConfig
    val isEnabled = shieldConfig?.contentShieldEnabled == true
    val currentHostname = shieldConfig?.dnsHostname ?: "family-filter-dns.cleanbrowsing.org"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- 1. HERO TOGGLE CARD ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Content Shield",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val isEnforced = state.isDeviceOwner && state.privateDnsStatus?.isEnforced == true
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isEnforced) CoconutColors.ProtectedGreen.copy(alpha = 0.15f) else CoconutColors.AwaitingAmber.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isEnforced) "Enforced" else "Pending DO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnforced) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEnabled) {
                                if (state.isDeviceOwner && state.privateDnsStatus?.isEnforced == true) {
                                    "Adult filtering active & locked"
                                } else {
                                    "Saved locally — Pending Device Owner to lock"
                                }
                            } else "Filtering disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) CoconutColors.ProtectedGreen else MaterialTheme.colorScheme.outline,
                            fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleShield,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // --- 2. PROVIDER SELECTION ---
        Text(
            text = "DNS Filter Provider",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        DnsProviders.ALL.forEach { provider ->
            val isSelected = provider.hostname == currentHostname
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isSelected) onSelectProvider(provider.hostname)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { if (!isSelected) onSelectProvider(provider.hostname) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = provider.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 3. TEST DNS BUTTON & STATUS ---
        OutlinedButton(
            onClick = onTestDns,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !state.isTestingDns
        ) {
            if (state.isTestingDns) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing Resolver...")
            } else {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test DNS Connectivity", style = MaterialTheme.typography.titleSmall)
            }
        }

        state.dnsTestResult?.let { result ->
            val isSuccess = result is DnsTestResult.Success
            val message = when (result) {
                is DnsTestResult.Success -> "Connected! Latency: ${result.latencyMs}ms (Resolved: ${result.resolvedIp})"
                is DnsTestResult.Failure -> "${result.error} ${result.recoveryAdvice}"
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSuccess) CoconutColors.ProtectedGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isSuccess) CoconutColors.ProtectedGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isSuccess) CoconutColors.ProtectedGreen else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
