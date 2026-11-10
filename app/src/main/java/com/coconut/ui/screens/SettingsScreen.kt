package com.coconut.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.coconut.ui.components.LemonCard
import com.coconut.ui.components.LemonGroupedContainer
import com.coconut.ui.components.LemonGroupedRow
import com.coconut.ui.components.LemonSectionEyebrow
import com.coconut.ui.components.LemonSegmentItem
import com.coconut.ui.components.LemonSegmentedControl
import com.coconut.ui.components.LemonSwitch
import kotlinx.coroutines.launch

/**
 * Pristine Redesigned Settings Screen conforming to the Fruits Family Quiet Utility Design System.
 * - Grouped container sections for Security, Appearance, Networking, and Data
 * - Tactile switches and chevron drill-down navigation
 * - Responsive 3-mode theme switcher (System, Light, Dark)
 * - Centered ecosystem branding card
 * - Safe bottom padding (120dp) preventing bottom dock clipping
 */
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
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status bar top spacing
        item(key = "status_bar_spacing") {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }

        // 1. Page Header (Fruit Family PageShell pattern with Unbounded display title)
        item(key = "settings_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = LemonTheme.colors.brandAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = "Settings",
                        style = LemonTheme.typography.displayLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Preferences, on-device security, and rules management.",
                    style = LemonTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = LemonTheme.colors.textSecondary
                )
            }
        }

        // 2. Security Section
        item(key = "security_eyebrow") {
            LemonSectionEyebrow(title = "SECURITY")
        }
        item(key = "security_card") {
            LemonGroupedContainer {
                LemonGroupedRow(
                    title = "App Lock",
                    subtitle = "Require biometric or PIN when opening Lemon",
                    showDivider = true,
                    trailingContent = {
                        LemonSwitch(
                            checked = state.isAppLockEnabled,
                            onCheckedChange = viewModel::toggleAppLockEnabled
                        )
                    }
                )

                LemonGroupedRow(
                    title = "Lock Lemon",
                    subtitle = "Instantly return to the secure lock screen",
                    showDivider = true,
                    onClick = viewModel::lockLemon,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Lock",
                            tint = LemonTheme.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                LemonGroupedRow(
                    title = "Accessibility Service",
                    subtitle = "Required for instant app launch interception",
                    showDivider = false,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LemonBadge(
                                text = if (state.isAccessibilityGranted) "Active" else "Inactive",
                                style = if (state.isAccessibilityGranted) LemonBadgeStyle.SUCCESS else LemonBadgeStyle.DANGER,
                                showDot = true
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Accessibility",
                                tint = LemonTheme.colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
        }

        // 3. Appearance Section
        item(key = "appearance_eyebrow") {
            LemonSectionEyebrow(title = "APPEARANCE")
        }
        item(key = "appearance_card") {
            LemonGroupedContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Theme Appearance",
                        style = LemonTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Switch between light paper canvas and obsidian warm-black",
                        style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
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

        // 4. Networking Section
        item(key = "networking_eyebrow") {
            LemonSectionEyebrow(title = "NETWORKING")
        }
        item(key = "networking_card") {
            LemonGroupedContainer {
                LemonGroupedRow(
                    title = "Wi-Fi Only Updates",
                    subtitle = "Prevent feed and blocklist updates over cellular data",
                    showDivider = false,
                    trailingContent = {
                        LemonSwitch(
                            checked = state.dnsConfig?.wifiOnly ?: true,
                            onCheckedChange = { viewModel.updateDnsConfiguration(wifiOnly = it) }
                        )
                    }
                )
            }
        }

        // 5. Data & Rules Backup Section
        item(key = "data_eyebrow") {
            LemonSectionEyebrow(title = "DATA & RULES")
        }
        item(key = "data_card") {
            LemonGroupedContainer {
                LemonGroupedRow(
                    title = "Export Rules",
                    subtitle = "Copy all custom domain rules to clipboard",
                    showDivider = true,
                    onClick = {
                        scope.launch {
                            val text = viewModel.exportRules()
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Lemon Rules", text))
                            Toast.makeText(context, "Rules copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export",
                            tint = LemonTheme.colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                LemonGroupedRow(
                    title = "Import Rules",
                    subtitle = "Restore domain rules from clipboard text backup",
                    showDivider = false,
                    onClick = { showImportDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Import",
                            tint = LemonTheme.colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // 6. Application Details & Fruit Family Ecosystem Card
        item(key = "app_details_section") {
            LemonCard(
                backgroundColor = LemonTheme.colors.cardBackground,
                contentPadding = PaddingValues(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = LemonTheme.shapes.cardLarge,
                        color = LemonTheme.colors.cardSecondaryBackground,
                        border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_lemon_mark),
                                contentDescription = "Lemon Mark",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Lemon",
                        style = LemonTheme.typography.displaySmall.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} • Fruits Family",
                        style = LemonTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = LemonTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "100% On-Device • Zero Telemetry • Open Privacy",
                        style = LemonTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = LemonTheme.colors.brandAmber,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Standard uninstallable application. Lemon never uses Device Owner or Device Administrator to restrict device management.",
                        style = LemonTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        color = LemonTheme.colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Import Rules Dialog
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
                        fontSize = 18.sp,
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
                        shape = LemonTheme.shapes.card,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LemonTheme.colors.brandAmber,
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
