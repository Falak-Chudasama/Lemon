package com.coconut.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protection_config")
data class ProtectionConfigurationEntity(
    @PrimaryKey
    val id: Int = 1,
    val contentShieldEnabled: Boolean = false,
    val dnsProviderName: String = "CleanBrowsing Family",
    val dnsHostname: String = "family-filter-dns.cleanbrowsing.org",
    val selfProtectionEnabled: Boolean = true,
    val disallowConfigPrivateDns: Boolean = true,
    val disallowConfigVpn: Boolean = false,
    val disallowDebugging: Boolean = false,
    val disallowUnknownSources: Boolean = false,
    val disallowSafeBoot: Boolean = false,
    val disallowFactoryReset: Boolean = false,
    val disallowAppsControl: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

