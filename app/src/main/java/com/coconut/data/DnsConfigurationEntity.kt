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
    val dnsEnabled: Boolean = false,
    val blockThreats: Boolean = true,
    val blockAdultContent: Boolean = false,
    val blockAdsTrackers: Boolean = false,
    val autoUpdate: Boolean = true,
    val wifiOnly: Boolean = true,
    val dnsCacheEnabled: Boolean = true,
    val lastUpdateTimestamp: Long = 0L
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
}
