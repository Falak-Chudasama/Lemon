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
import androidx.compose.material3.HorizontalDivider
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
import com.coconut.ui.components.LemonSectionEyebrow
import com.coconut.ui.components.LemonSegmentItem
import com.coconut.ui.components.LemonSegmentedControl
import com.coconut.ui.components.LemonSwitch
import kotlinx.coroutines.launch

/**
 * Redesigned Settings Screen conforming to the Fruit Family design system.
 * Features:
 * - Unbounded geometric header with Lemon brand icon
 * - Inset grouped cards with clean 1px dividers
 * - Responsive 3-mode theme selector (System, Light, Dark)
 * - Security section (App Lock, instant lock, Accessibility permissions)
 * - Networking & Rule data backup (plain text export / import)
 * - Official Lemon branding block with version badge and privacy statement
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
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status bar top spacing
        item(key = "status_bar_spacing") {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }

        // 1. Header (Fruit Family PageShell pattern with Unbounded display title)
        item(key = "settings_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = LemonTheme.shapes.iconSquircle,
                        color = LemonTheme.colors.cardSecondaryBackground,
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
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Preferences, on-device security, and rules management.",
                    style = LemonTheme.typography.bodyMedium,
                    color = LemonTheme.colors.textSecondary
                )
            }
        }

        // 2. Security Section
        item(key = "security_section") {
            LemonSectionEyebrow(title = "SECURITY")
            Spacer(modifier = Modifier.height(4.dp))

            LemonCard {
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = LemonTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Require biometric or PIN when opening Lemon",
                            style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
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
                        .clip(LemonTheme.shapes.iconSquircle)
                        .clickable(onClick = viewModel::lockLemon)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(LemonTheme.shapes.iconSquircle)
                                .background(LemonTheme.colors.brandYellow.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = LemonTheme.colors.brandAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Lock Lemon",
                                style = LemonTheme.typography.titleSmall.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = LemonTheme.colors.textPrimary
                            )
                            Text(
                                text = "Instantly trigger the biometric lock screen",
                                style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = LemonTheme.colors.textSecondary
                            )
                        }
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
                        .clip(LemonTheme.shapes.iconSquircle)
                        .clickable {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Accessibility Service",
                            style = LemonTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = LemonTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Required for immediate app launch interception",
                            style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = LemonTheme.colors.textSecondary
                        )
                    }
                    LemonBadge(
                        text = if (state.isAccessibilityGranted) "Active" else "Inactive",
                        style = if (state.isAccessibilityGranted) LemonBadgeStyle.SUCCESS else LemonBadgeStyle.DANGER,
                        showDot = true
                    )
                }
            }
        }

        // 3. Appearance Section
        item(key = "appearance_section") {
            LemonSectionEyebrow(title = "APPEARANCE")
            Spacer(modifier = Modifier.height(4.dp))

            LemonCard {
                Text(
                    text = "Theme Appearance",
                    style = LemonTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LemonTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Switch between quiet light paper canvas and obsidian warm-black",
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

        // 4. Updates & Networking Section
        item(key = "updates_section") {
            LemonSectionEyebrow(title = "NETWORKING")
            Spacer(modifier = Modifier.height(4.dp))

            LemonCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wi-Fi Only Updates",
                            style = LemonTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = LemonTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Prevent feed and blocklist updates over cellular data",
                            style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
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

        // 5. DNS Rules Backup Section
        item(key = "dns_rules_section") {
            LemonSectionEyebrow(title = "DATA & RULES")
            Spacer(modifier = Modifier.height(4.dp))

            LemonCard {
                Text(
                    text = "Manage Custom Rules",
                    style = LemonTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LemonTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export your rules to clipboard or import rules from a text backup.",
                    style = LemonTheme.typography.bodySmall.copy(fontSize = 12.sp),
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
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

        // 6. Application Details & Fruit Family About Section
        item(key = "app_details_section") {
            LemonCard(
                backgroundColor = LemonTheme.colors.cardBackground
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = LemonTheme.shapes.cardLarge,
                        color = LemonTheme.colors.cardSecondaryBackground,
                        border = BorderStroke(1.dp, LemonTheme.colors.cardBorder),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_lemon_mark),
                                contentDescription = "Lemon Mark",
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Lemon",
                        style = LemonTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = LemonTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} • Fruits Family",
                        style = LemonTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = LemonTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Standard uninstallable application. Lemon never uses Device Owner or Device Administrator to restrict device management.",
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
