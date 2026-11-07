package com.coconut.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "domain_rules",
    indices = [
        Index(value = ["domain"], unique = true),
        Index(value = ["ruleType", "isEnabled"])
    ]
)
data class DomainRuleEntity(
    @PrimaryKey
    val domain: String,
    val ruleType: String, // "BLOCK" or "ALLOW"
    val isEnabled: Boolean = true,
    val source: String = "USER", // "USER", "PRESET"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

