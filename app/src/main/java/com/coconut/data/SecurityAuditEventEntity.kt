package com.coconut.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_audit_events")
data class SecurityAuditEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val target: String? = null,
    val details: String,
    val severity: String = "INFO"
)

