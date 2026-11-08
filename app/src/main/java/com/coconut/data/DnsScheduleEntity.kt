package com.coconut.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dns_schedule")
data class DnsScheduleEntity(
    @PrimaryKey
    val id: Int = 1,
    val enabled: Boolean = true,
    val startHour: Int = 23,
    val startMinute: Int = 0,
    val endHour: Int = 6,
    val endMinute: Int = 0,
    val lastEvaluatedAtEpochMillis: Long = 0L
)

@Dao
interface DnsScheduleDao {
    @Query("SELECT * FROM dns_schedule WHERE id = 1 LIMIT 1")
    fun getScheduleFlow(): Flow<DnsScheduleEntity?>

    @Query("SELECT * FROM dns_schedule WHERE id = 1 LIMIT 1")
    suspend fun getSchedule(): DnsScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(schedule: DnsScheduleEntity)

    @Update
    suspend fun update(schedule: DnsScheduleEntity)

    @Query("UPDATE dns_schedule SET enabled = :enabled WHERE id = 1")
    suspend fun setEnabled(enabled: Boolean)

    @Query("UPDATE dns_schedule SET lastEvaluatedAtEpochMillis = :nowMillis WHERE id = 1")
    suspend fun updateLastEvaluated(nowMillis: Long)
}

