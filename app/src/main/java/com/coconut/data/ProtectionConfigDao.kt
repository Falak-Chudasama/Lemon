package com.coconut.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectionConfigDao {
    @Query("SELECT * FROM protection_config WHERE id = 1 LIMIT 1")
    fun observeConfig(): Flow<ProtectionConfigurationEntity?>

    @Query("SELECT * FROM protection_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): ProtectionConfigurationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: ProtectionConfigurationEntity): Long

    @Update
    suspend fun update(config: ProtectionConfigurationEntity): Int
}

