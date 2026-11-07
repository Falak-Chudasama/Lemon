package com.coconut.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.UserManager
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coconut.R
import com.coconut.ui.CoconutColors
import com.coconut.ui.MainUiState
import com.coconut.ui.components.GlassCard
import com.coconut.ui.components.GlassStatusBadge

@Composable
fun SettingsScreen(
    state: MainUiState,
    onToggleRestriction: (String, Boolean) -> Unit,
    onOpenEmergencyRecovery: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val config = state.protectionConfig
    val adbCommand = "adb shell dpm set-device-owner com.coconut/.policy.CoconutDeviceAdminReceiver"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. DEVICE OWNER PRIVILEGE CARD ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (state.isDeviceOwner) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Device Owner Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    GlassStatusBadge(
                        text = if (state.isDeviceOwner) "Enforced" else "Pending Setup",
                        statusColor = if (state.isDeviceOwner) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (state.isDeviceOwner) {
                        "Coconut is actively provisioned as Device Owner. Full OS-level application suspension and Private DNS locking are enforced by the Android framework."
                    } else {
                        "Standard Android installation. Connect your device via USB and run the ADB command below to enable native package suspension."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (!state.isDeviceOwner) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Provisioning Command", adbCommand))
                            Toast.makeText(context, "ADB command copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy ADB Provisioning Command", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // --- 2. OPTIONAL REVERSIBLE DEVICE HARDENING ---
        Text(
            text = "Optional Device Hardening",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HardeningRow(
                    title = "Disallow USB Debugging",
                    subtitle = "Blocks ADB once device provisioning is complete",
                    checked = config?.disallowDebugging == true,
                    isEnforced = state.isDeviceOwner && config?.disallowDebugging == true,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES, it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                HardeningRow(
                    title = "Disallow Unknown Sources",
                    subtitle = "Prevents sideloading unauthorized APK files",
                    checked = config?.disallowUnknownSources == true,
                    isEnforced = state.isDeviceOwner && config?.disallowUnknownSources == true,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, it) }
                )
            }
        }

        // --- 3. EXCEPTIONAL EMERGENCY RECOVERY VALVE ---
        Text(
            text = "Safety & Emergency Recovery",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = CoconutColors.AwaitingAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Emergency Recovery Valve",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A deliberately authenticated safety mechanism to restore a snapped app in an emergency without factory resetting or erasing any device data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenEmergencyRecovery,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Access Emergency Recovery", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // --- 4. BRAND FOOTER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_coconut_logo),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Coconut • 100% Offline • Zero Tracking • Data-Safe",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun HardeningRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    isEnforced: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (checked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassStatusBadge(
                        text = if (isEnforced) "Enforced" else "Pending DO",
                        statusColor = if (isEnforced) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber,
                        showDot = false
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
