package com.coconut.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DevicePolicyStateDao {
    @Query("SELECT * FROM device_policy_state WHERE id = 1 LIMIT 1")
    fun observeState(): Flow<DevicePolicyStateEntity?>

    @Query("SELECT * FROM device_policy_state WHERE id = 1 LIMIT 1")
    suspend fun getState(): DevicePolicyStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: DevicePolicyStateEntity): Long

    @Update
    suspend fun update(state: DevicePolicyStateEntity): Int
}

