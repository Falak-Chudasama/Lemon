package com.coconut.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "strict_mode_config")
data class StrictModeConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val isEnabled: Boolean = false,
    val passcodeHash: String? = null,
    val passcodeSalt: String? = null,
    val unlockDelayMinutes: Int = 15,
    val unlockRequestTimestamp: Long = 0L,
    val temporaryUnlockUntil: Long = 0L
)

@Dao
interface StrictModeConfigDao {
    @Query("SELECT * FROM strict_mode_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<StrictModeConfigEntity?>

    @Query("SELECT * FROM strict_mode_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): StrictModeConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: StrictModeConfigEntity)

    @Update
    suspend fun update(config: StrictModeConfigEntity)
}

