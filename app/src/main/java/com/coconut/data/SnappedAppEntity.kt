package com.coconut.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SnapState {
    const val PENDING_ENFORCEMENT = "PENDING_ENFORCEMENT"
    const val SNAP_ACTIVE = "SNAP_ACTIVE"
    const val PARTIALLY_ENFORCED = "PARTIALLY_ENFORCED"
    const val SNAP_FAILED = "SNAP_FAILED"
    const val AWAITING_INSTALLATION = "AWAITING_INSTALLATION"
    const val RECOVERY_REQUIRED = "RECOVERY_REQUIRED"

    // Legacy compatibility constants
    const val PROTECTED = "SNAP_ACTIVE"
    const val RECONCILIATION_REQUIRED = "PENDING_ENFORCEMENT"
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
