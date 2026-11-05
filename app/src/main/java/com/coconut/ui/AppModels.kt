package com.coconut.ui

import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val applicationName: String,
    val category: String,
    val isSystemApp: Boolean,
    val isProtected: Boolean,
    val protectionReason: String?,
    val isSuspended: Boolean,
    val icon: Drawable?
)

enum class AppSortOrder(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    CATEGORY("Category"),
    PACKAGE_NAME("Package Name")
}

enum class NavigationTab(val title: String) {
    HOME("Home"),
    THE_SNAP("The Snap"),
    CONTENT_SHIELD("Content Shield"),
    SETTINGS("Settings")
}
