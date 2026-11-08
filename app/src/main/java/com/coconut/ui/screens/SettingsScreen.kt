package com.coconut.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coconut.BuildConfig
import com.coconut.R
import com.coconut.ui.AppThemeMode
import com.coconut.ui.LemonTheme
import com.coconut.ui.MainViewModel
import com.coconut.ui.ThemePreferences
import com.coconut.ui.components.LemonBadge
import com.coconut.ui.components.LemonBadgeStyle
import com.coconut.ui.components.LemonSegmentItem
import com.coconut.ui.components.LemonSegmentedControl
import com.coconut.ui.components.LemonSwitch
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentThemeMode by ThemePreferences.themeModeFlow.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status bar top spacing
        item(key = "status_bar_spacing") {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }

        // 1. Header (Large 34sp Title)
        item(key = "settings_header") {
            Text(
                text = "Settings",
                style = LemonTheme.typography.displayLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = LemonTheme.colors.textPrimary
            )
        }

        // 2. Security Section (Inset Grouped)
        item(key = "security_section") {
            LemonSectionLabel(title = "Security", icon = Icons.Default.Security)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // App Lock Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Lock",
                                style = LemonTheme.typography.titleSmall.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Require biometric or PIN when opening Lemon",
                                style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = LemonTheme.colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        LemonSwitch(
                            checked = state.isAppLockEnabled,
                            onCheckedChange = viewModel::toggleAppLockEnabled
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = LemonTheme.colors.separator
                    )

                    // Lock Lemon Now Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(LemonTheme.shapes.button)
                            .clickable(onClick = viewModel::lockLemon)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(LemonTheme.colors.primaryAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = LemonTheme.colors.textAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Lock Lemon",
                                style = LemonTheme.typography.titleSmall.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Lock",
                            tint = LemonTheme.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = LemonTheme.colors.separator
                    )

                    // Accessibility Status & Direct Navigation Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(LemonTheme.shapes.button)
                            .clickable {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accessibility",
                                style = LemonTheme.typography.titleSmall.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Required to enforce app restrictions",
                                style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = LemonTheme.colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        LemonBadge(
                            text = if (state.isAccessibilityGranted) "Active" else "Inactive",
                            style = if (state.isAccessibilityGranted) LemonBadgeStyle.SUCCESS else LemonBadgeStyle.DANGER,
                            showDot = true
                        )
                    }
                }
            }
        }

        // 3. Appearance Section (3-Option Segmented Control)
        item(key = "appearance_section") {
            LemonSectionLabel(title = "Appearance", icon = Icons.Default.Palette)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Theme",
                        style = LemonTheme.typography.titleSmall.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select your preferred visual appearance",
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = LemonTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val themeSegments = listOf(
                        LemonSegmentItem(id = AppThemeMode.SYSTEM.name, title = "System"),
                        LemonSegmentItem(id = AppThemeMode.LIGHT.name, title = "Light"),
                        LemonSegmentItem(id = AppThemeMode.DARK.name, title = "Dark")
                    )

                    LemonSegmentedControl(
                        items = themeSegments,
                        selectedId = currentThemeMode.name,
                        onItemSelected = { selectedName ->
                            val selectedMode = AppThemeMode.valueOf(selectedName)
                            viewModel.themePreferences.setThemeMode(selectedMode)
                        }
                    )
                }
            }
        }

        // 4. Updates & Networking Section
        item(key = "updates_section") {
            LemonSectionLabel(title = "Updates", icon = Icons.Default.Wifi)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wi-Fi Only Updates",
                                style = LemonTheme.typography.titleSmall.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Prevent list updates over mobile data",
                                style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = LemonTheme.colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        LemonSwitch(
                            checked = state.dnsConfig?.wifiOnly ?: true,
                            onCheckedChange = { viewModel.updateDnsConfiguration(wifiOnly = it) }
                        )
                    }
                }
            }
        }

        // 5. DNS Rules Backup Section
        item(key = "dns_rules_section") {
            LemonSectionLabel(title = "Rules & Backup", icon = Icons.Default.Shield)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Manage Custom Rules",
                        style = LemonTheme.typography.titleSmall.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Export your rules for safekeeping or import rules from a text backup.",
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = LemonTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Export Rules Button
                        Button(
                            onClick = {
                                scope.launch {
                                    val text = viewModel.exportRules()
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Lemon Rules", text))
                                    Toast.makeText(context, "Rules copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = LemonTheme.shapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LemonTheme.colors.cardSecondaryBackground,
                                contentColor = LemonTheme.colors.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export",
                                modifier = Modifier.size(16.dp),
                                tint = LemonTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export",
                                style = LemonTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Import Rules Button
                        Button(
                            onClick = { showImportDialog = true },
                            shape = LemonTheme.shapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LemonTheme.colors.cardSecondaryBackground,
                                contentColor = LemonTheme.colors.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Import",
                                modifier = Modifier.size(16.dp),
                                tint = LemonTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import",
                                style = LemonTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Application Details & About Section
        item(key = "app_details_section") {
            Surface(
                shape = LemonTheme.shapes.card,
                color = LemonTheme.colors.cardSecondaryBackground,
                border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_lemon_mark),
                        contentDescription = "Lemon Mark",
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Lemon",
                        style = LemonTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • Target Android 16 (API 36)",
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = LemonTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Local-first self-control. Zero remote telemetry.",
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = LemonTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ordinary uninstallable app. Lemon does not use Device Owner or Device Administrator to prevent removal.",
                        style = LemonTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = LemonTheme.colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importText = ""
            },
            shape = LemonTheme.shapes.dialog,
            containerColor = LemonTheme.colors.cardBackground,
            title = {
                Text(
                    text = "Import Domain Rules",
                    style = LemonTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LemonTheme.colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste plain-text rules with format:\nBLOCK: example.com\nALLOW: domain.org",
                        style = LemonTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = LemonTheme.colors.textSecondary
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LemonTheme.colors.primaryAccent,
                            unfocusedBorderColor = LemonTheme.colors.cardBorder,
                            focusedContainerColor = LemonTheme.colors.cardSecondaryBackground,
                            unfocusedContainerColor = LemonTheme.colors.cardSecondaryBackground,
                            focusedTextColor = LemonTheme.colors.textPrimary,
                            unfocusedTextColor = LemonTheme.colors.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val (imported, invalid) = viewModel.importRules(importText)
                            Toast.makeText(context, "Imported $imported rules ($invalid skipped)", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                            importText = ""
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
                        text = "Import",
                        style = LemonTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        importText = ""
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
}

@Composable
private fun LemonSectionLabel(
    title: String,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(LemonTheme.colors.primaryAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LemonTheme.colors.textAccent,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = LemonTheme.typography.titleSmall.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            color = LemonTheme.colors.textPrimary
        )
    }
}
