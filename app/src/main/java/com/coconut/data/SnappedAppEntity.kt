package com.coconut.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SnapState {
    const val PROTECTED = "SNAPPED — PROTECTED"
    const val AWAITING_INSTALLATION = "SNAPPED — AWAITING INSTALLATION"
    const val SNAP_FAILED = "SNAP FAILED"
    const val RECONCILIATION_REQUIRED = "RECONCILIATION REQUIRED"
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
    val actualState: String = SnapState.PROTECTED,
    val installationState: String = InstallationState.INSTALLED,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
