package com.coconut.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_policy_state")
data class DevicePolicyStateEntity(
    @PrimaryKey
    val id: Int = 1,
    val isDeviceOwner: Boolean = false,
    val isUninstallBlocked: Boolean = false,
    val activeDnsMode: String = "OFF",
    val activeDnsHost: String? = null,
    val isDnsProtected: Boolean = false,
    val totalSnappedCount: Int = 0,
    val activeSnappedCount: Int = 0,
    val lastReconciledAt: Long = 0L,
    val reconciliationStatus: String = "INITIAL"
)

