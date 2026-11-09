package com.coconut.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dns_configuration")
data class DnsConfigurationEntity(
    @PrimaryKey
    val id: Int = 1,
    val dnsEnabled: Boolean = false, // Legacy runtime field; preserved for compatibility
    val manualDnsEnabled: Boolean = false, // Authoritative user preference toggle
    val manualDnsConfirmed: Boolean = false, // One-time confirmation state; ensures user intent is explicitly captured
    val blockThreats: Boolean = true,
    val blockAdultContent: Boolean = false,
    val blockAdsTrackers: Boolean = false,
    val autoUpdate: Boolean = true,
    val wifiOnly: Boolean = true,
    val dnsCacheEnabled: Boolean = true,
    val lastUpdateTimestamp: Long = 0L,
    val primaryUpstream: String = "SYSTEM", // "SYSTEM", "ADGUARD_DEFAULT", "ADGUARD_FAMILY", "CLEANBROWSING_FAMILY", "CLOUDFLARE", "GOOGLE", "CUSTOM"
    val secondaryUpstream: String = "",
    val customUpstreamEndpoint: String = "",
    val upstreamProtocol: String = "SYSTEM" // "SYSTEM", "DOT", "DOH"
)

@Dao
interface DnsConfigurationDao {
    @Query("SELECT * FROM dns_configuration WHERE id = 1 LIMIT 1")
    fun getConfigurationFlow(): Flow<DnsConfigurationEntity?>

    @Query("SELECT * FROM dns_configuration WHERE id = 1 LIMIT 1")
    suspend fun getConfiguration(): DnsConfigurationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: DnsConfigurationEntity)

    @Update
    suspend fun update(config: DnsConfigurationEntity)

    @Query("UPDATE dns_configuration SET manualDnsEnabled = :enabled WHERE id = 1")
    suspend fun setManualDnsEnabled(enabled: Boolean)

    @Query("UPDATE dns_configuration SET manualDnsEnabled = :enabled, manualDnsConfirmed = 1 WHERE id = 1")
    suspend fun confirmManualDnsPreference(enabled: Boolean)

    @Query("UPDATE dns_configuration SET primaryUpstream = :primary, secondaryUpstream = :secondary, customUpstreamEndpoint = :custom, upstreamProtocol = :protocol WHERE id = 1")
    suspend fun updateUpstreamConfig(primary: String, secondary: String, custom: String, protocol: String)
}
