package com.coconut.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "quarantine_state")
data class QuarantineEntity(
    @PrimaryKey
    val id: Int = 1,
    val isActive: Boolean = false,
    val startTimeEpochMillis: Long = 0L,
    val durationMinutes: Int = 30,
    val endTimeEpochMillis: Long = 0L,
    val bootCountAtStart: Int = 0,
    val elapsedRealtimeAtStart: Long = 0L,
    val selectedPackages: String = "", // Comma-delimited list of temporarily restricted packages
    val lastEvaluatedEpochMillis: Long = 0L
) {
    fun getPackageList(): Set<String> {
        if (selectedPackages.isBlank()) return emptySet()
        return selectedPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    companion object {
        fun fromPackageList(
            packages: Set<String>,
            durationMinutes: Int,
            startTime: Long,
            endTime: Long,
            bootCount: Int,
            elapsedRealtime: Long
        ): QuarantineEntity {
            return QuarantineEntity(
                id = 1,
                isActive = true,
                startTimeEpochMillis = startTime,
                durationMinutes = durationMinutes,
                endTimeEpochMillis = endTime,
                bootCountAtStart = bootCount,
                elapsedRealtimeAtStart = elapsedRealtime,
                selectedPackages = packages.joinToString(","),
                lastEvaluatedEpochMillis = startTime
            )
        }
    }
}

@Dao
interface QuarantineDao {
    @Query("SELECT * FROM quarantine_state WHERE id = 1 LIMIT 1")
    fun getQuarantineFlow(): Flow<QuarantineEntity?>

    @Query("SELECT * FROM quarantine_state WHERE id = 1 LIMIT 1")
    suspend fun getQuarantine(): QuarantineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(quarantine: QuarantineEntity)

    @Update
    suspend fun update(quarantine: QuarantineEntity)

    @Query("UPDATE quarantine_state SET isActive = 0, lastEvaluatedEpochMillis = :evaluatedAt WHERE id = 1")
    suspend fun deactivate(evaluatedAt: Long)
}

