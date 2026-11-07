package com.coconut.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coconut.R
import com.coconut.ui.CoconutColors
import com.coconut.ui.MainUiState

@Composable
fun SetupWizardDialog(
    state: MainUiState,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val adbCommand = "adb shell dpm set-device-owner com.coconut/.policy.CoconutDeviceAdminReceiver"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        icon = {
            Image(
                painter = painterResource(id = R.drawable.ic_coconut_logo),
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
        },
        title = {
            Text(
                text = "Privilege & Provisioning Setup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Android OS requires specific privileges for native app suspension and Private DNS management:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Device Administrator (ordinary)
                SetupItemCard(
                    title = "Device Administrator",
                    description = if (state.isDeviceAdmin) "Admin active. Note: Ordinary admin CANNOT suspend apps or set DNS." else "Basic device admin activation.",
                    isComplete = state.isDeviceAdmin,
                    actionLabel = "Activate",
                    onAction = onRequestDeviceAdmin
                )

                // 2. Notifications Permission
                SetupItemCard(
                    title = "System Alerts",
                    description = "Notifies you of background policy verifications.",
                    isComplete = hasNotificationPermission,
                    actionLabel = "Allow",
                    onAction = onRequestNotificationPermission
                )

                // 3. Device Owner Mode (DPC)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isDeviceOwner) CoconutColors.ProtectedGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.isDeviceOwner) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (state.isDeviceOwner) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device Owner (Enterprise DPC)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (state.isDeviceOwner) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CoconutColors.ProtectedGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.isDeviceOwner) {
                                "Full OS-level suspension and Private DNS locking are active."
                            } else {
                                "Mandatory for native package suspension and Private DNS enforcement. Requires ADB command on a device with no accounts added."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        if (!state.isDeviceOwner) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCommand))
                                    Toast.makeText(context, "ADB command copied!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy ADB DPC Command", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun SetupItemCard(
    title: String,
    description: String,
    isComplete: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) CoconutColors.ProtectedGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isComplete) CoconutColors.ProtectedGreen else CoconutColors.AwaitingAmber,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isComplete) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CoconutColors.ProtectedGreen,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}
