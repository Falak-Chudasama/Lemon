package com.coconut.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "blocklist_sources")
data class BlocklistSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val version: String = "1.0",
    val lastUpdated: Long = System.currentTimeMillis(),
    val ruleCount: Int = 0,
    val isEnabled: Boolean = true,
    val category: String = "THREATS" // "THREATS", "ADS_TRACKING"
)

@Dao
interface BlocklistSourceDao {
    @Query("SELECT * FROM blocklist_sources")
    fun getAllSourcesFlow(): Flow<List<BlocklistSourceEntity>>

    @Query("SELECT * FROM blocklist_sources WHERE isEnabled = 1")
    suspend fun getActiveSources(): List<BlocklistSourceEntity>

    @Query("SELECT * FROM blocklist_sources WHERE id = :id LIMIT 1")
    suspend fun getSourceById(id: String): BlocklistSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(source: BlocklistSourceEntity)

    @Update
    suspend fun update(source: BlocklistSourceEntity)
}

