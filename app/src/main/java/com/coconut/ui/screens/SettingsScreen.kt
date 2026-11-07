package com.coconut.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coconut.ui.CoconutColors
import com.coconut.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showPasscodeSetupDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 80.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. SYSTEM PERMISSIONS ---
        Text(
            text = "System Permissions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Accessibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Accessibility Service",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.isAccessibilityGranted) "Active • Enforcing Snap" else "Required for Snap",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isAccessibilityGranted) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber
                        )
                    }
                    if (!state.isAccessibilityGranted) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // VPN Permission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local DNS VPN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.isVpnRunning) "Active • Local filter running" else "Configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isVpnRunning) CoconutColors.ProtectedGreen else MaterialTheme.colorScheme.outline
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "VPN Settings not directly accessible", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("VPN Settings", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. STRICT MODE ---
        Text(
            text = "Strict Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.5.dp),
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
                            text = "Policy Lock",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Requires passcode and delay to modify rules or disable protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = state.strictModeConfig?.isEnabled ?: false,
                        onCheckedChange = { enable ->
                            if (enable) {
                                showPasscodeSetupDialog = true
                            } else {
                                viewModel.setStrictMode(false)
                            }
                        }
                    )
                }

                if (state.strictModeConfig?.isEnabled == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unlock delay: 15 minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        TextButton(onClick = { showUnlockDialog = true }) {
                            Text("Request Unlock", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. DNS UPDATE PREFERENCES ---
        Text(
            text = "Blocklist Updates",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wi-Fi Only Updates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = state.dnsConfig?.wifiOnly ?: true,
                        onCheckedChange = { viewModel.updateDnsConfiguration(wifiOnly = it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automatic Updates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = state.dnsConfig?.autoUpdate ?: true,
                        onCheckedChange = { viewModel.updateDnsConfiguration(autoUpdate = it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. BACKUP & RULES ---
        Text(
            text = "Custom Rules Backup",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val text = viewModel.exportRules()
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Coconut Rules", text))
                            Toast.makeText(context, "Rules copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = "Export", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Import", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. PRIVACY & APP INFO ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Privacy Architecture",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "100% on-device local processing. Zero cloud accounts, zero telemetry, zero analytics tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text("2.0.0 (3)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Passcode Setup Dialog
    if (showPasscodeSetupDialog) {
        var passcode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasscodeSetupDialog = false },
            title = { Text("Set Strict Mode Passcode", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setStrictMode(true, passcode)
                        showPasscodeSetupDialog = false
                    },
                    enabled = passcode.isNotBlank()
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasscodeSetupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unlock Dialog
    if (showUnlockDialog) {
        val config = state.strictModeConfig
        val now = System.currentTimeMillis()
        val requestTime = config?.unlockRequestTimestamp ?: 0L
        val delayMillis = (config?.unlockDelayMinutes ?: 15) * 60 * 1000L
        val isReady = requestTime > 0 && now >= requestTime + delayMillis
        val remainingMinutes = if (requestTime > 0 && !isReady) {
            ((requestTime + delayMillis - now) / 60000L).coerceAtLeast(1)
        } else 0L

        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("Strict Mode Unlock", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    if (requestTime == 0L) {
                        Text(
                            text = "Requesting unlock starts a 15-minute countdown before Settings access is temporarily granted.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else if (!isReady) {
                        Text(
                            text = "Countdown active: $remainingMinutes minutes remaining before temporary access unlocks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoconutColors.AwaitingAmber
                        )
                    } else {
                        Text(
                            text = "Countdown completed. Tap Confirm to open a 15-minute Settings window.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoconutColors.ProtectedGreen
                        )
                    }
                }
            },
            confirmButton = {
                if (requestTime == 0L) {
                    Button(onClick = {
                        viewModel.startUnlockCountdown()
                        showUnlockDialog = false
                    }) {
                        Text("Start 15m Countdown")
                    }
                } else if (isReady) {
                    Button(onClick = {
                        viewModel.completeUnlock()
                        showUnlockDialog = false
                    }) {
                        Text("Open 15m Window")
                    }
                } else {
                    Button(onClick = {
                        viewModel.cancelUnlock()
                        showUnlockDialog = false
                    }) {
                        Text("Cancel Countdown")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Domain Rules", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("Paste exported rules or list of domains...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val (imported, invalid) = viewModel.importRules(importText)
                            Toast.makeText(context, "Imported $imported rules ($invalid invalid)", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        }
                    },
                    enabled = importText.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
