package com.coconut.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SnapState {
    const val SNAPPED = "SNAPPED"
    const val NOT_INSTALLED = "NOT_INSTALLED"
    const val REINSTALL_PENDING = "REINSTALL_PENDING"
    const val BLOCKING = "BLOCKING"
    const val ACCESSIBILITY_INACTIVE = "ACCESSIBILITY_INACTIVE"
    const val ENFORCEMENT_ERROR = "ENFORCEMENT_ERROR"

    // Backward compatibility aliases
    const val SNAP_ACTIVE = "SNAPPED"
    const val PENDING_ENFORCEMENT = "REINSTALL_PENDING"
    const val PARTIALLY_ENFORCED = "SNAPPED"
    const val SNAP_FAILED = "ENFORCEMENT_ERROR"
    const val AWAITING_INSTALLATION = "NOT_INSTALLED"
    const val RECOVERY_REQUIRED = "ENFORCEMENT_ERROR"
    const val PROTECTED = "SNAPPED"
    const val RECONCILIATION_REQUIRED = "REINSTALL_PENDING"
}

object InstallationState {
    const val INSTALLED = "INSTALLED"
    const val NOT_INSTALLED = "NOT_INSTALLED"
}

@Entity(
    tableName = "snapped_apps",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class SnappedAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val applicationName: String,
    val category: String = "Other",
    val firstSnappedAt: Long = System.currentTimeMillis(),
    val lastAppliedAt: Long = System.currentTimeMillis(),
    val desiredState: String = "SUSPENDED",
    val actualState: String = SnapState.SNAP_ACTIVE,
    val uninstallBlocked: Boolean = false,
    val installationState: String = InstallationState.INSTALLED,
    val lastVerifiedAt: Long = 0L,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
