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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

@Composable
fun SettingsScreen(
    state: MainUiState,
    onToggleSelfProtection: (Boolean) -> Unit,
    onToggleRestriction: (String, Boolean) -> Unit
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
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- 1. DEVICE OWNER CARD ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isDeviceOwner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (state.isDeviceOwner) CoconutColors.ProtectedGreen.copy(alpha = 0.15f) else CoconutColors.AwaitingAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (state.isDeviceOwner) "Enforced" else "Pending Setup",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isDeviceOwner) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (state.isDeviceOwner) {
                        "Coconut has full Device Owner privileges. System-level app suspension and DNS locking are actively enforced by Android."
                    } else {
                        "Ordinary installation. Policy choices are saved locally. Connect via ADB to activate hardware-level enforcement."
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

        // --- 2. SECURITY HARDENING TOGGLES ---
        Text(
            text = "Device Hardening Policies",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HardeningRow(
                    title = "Prevent Coconut Uninstallation",
                    subtitle = "Locks Coconut against removal from device",
                    checked = config?.selfProtectionEnabled ?: true,
                    isEnforced = state.isDeviceOwner && state.isSelfProtectionActive,
                    isDeviceOwner = state.isDeviceOwner,
                    onCheckedChange = onToggleSelfProtection
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                HardeningRow(
                    title = "Lock Private DNS Configuration",
                    subtitle = "Prevents tampering with Content Shield in Settings",
                    checked = config?.disallowConfigPrivateDns ?: true,
                    isEnforced = state.isDeviceOwner && state.privateDnsStatus?.isConfigBlocked == true,
                    isDeviceOwner = state.isDeviceOwner,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_CONFIG_PRIVATE_DNS, it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                HardeningRow(
                    title = "Disallow Factory Reset",
                    subtitle = "Prevents wiping device to bypass restrictions",
                    checked = config?.disallowFactoryReset == true,
                    isEnforced = state.isDeviceOwner && config?.disallowFactoryReset == true,
                    isDeviceOwner = state.isDeviceOwner,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_FACTORY_RESET, it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                HardeningRow(
                    title = "Disallow Safe Mode Boot",
                    subtitle = "Prevents booting device into Android Safe Mode",
                    checked = config?.disallowSafeBoot == true,
                    isEnforced = state.isDeviceOwner && config?.disallowSafeBoot == true,
                    isDeviceOwner = state.isDeviceOwner,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_SAFE_BOOT, it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                HardeningRow(
                    title = "Disallow VPN Configuration",
                    subtitle = "Prevents VPN connections from bypassing DNS filter",
                    checked = config?.disallowConfigVpn == true,
                    isEnforced = state.isDeviceOwner && config?.disallowConfigVpn == true,
                    isDeviceOwner = state.isDeviceOwner,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_CONFIG_VPN, it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                HardeningRow(
                    title = "Disallow USB Debugging",
                    subtitle = "Prevents ADB debugging once fully provisioned",
                    checked = config?.disallowDebugging == true,
                    isEnforced = state.isDeviceOwner && config?.disallowDebugging == true,
                    isDeviceOwner = state.isDeviceOwner,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES, it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                HardeningRow(
                    title = "Disallow Unknown Sources",
                    subtitle = "Blocks sideloading untrusted apps",
                    checked = config?.disallowUnknownSources == true,
                    isEnforced = state.isDeviceOwner && config?.disallowUnknownSources == true,
                    isDeviceOwner = state.isDeviceOwner,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                HardeningRow(
                    title = "Disallow App Control",
                    subtitle = "Prevents clearing app storage or force stopping apps",
                    checked = config?.disallowAppsControl == true,
                    isEnforced = state.isDeviceOwner && config?.disallowAppsControl == true,
                    isDeviceOwner = state.isDeviceOwner,
                    onCheckedChange = { onToggleRestriction(UserManager.DISALLOW_APPS_CONTROL, it) }
                )
            }
        }

        // --- 3. MINIMAL BRAND FOOTER ---
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
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Coconut • 100% Offline • Zero Tracking",
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
    isDeviceOwner: Boolean,
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
                Spacer(modifier = Modifier.width(8.dp))
                if (checked) {
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
