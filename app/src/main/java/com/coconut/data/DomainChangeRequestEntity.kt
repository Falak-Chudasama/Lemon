package com.coconut.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

object DomainChangeOperation {
    const val REMOVE_BLOCK = "REMOVE_BLOCK"
    const val ADD_ALLOW = "ADD_ALLOW"
}

object DomainChangeStatus {
    const val PENDING = "PENDING"
    const val CANCELLED = "CANCELLED"
    const val APPLIED = "APPLIED"
    const val REJECTED = "REJECTED"
}

@Entity(
    tableName = "domain_change_requests",
    indices = [
        Index(value = ["normalizedDomain", "status"]),
        Index(value = ["status", "effectiveAtEpochMillis"])
    ]
)
data class DomainChangeRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requestId: String = UUID.randomUUID().toString(),
    val normalizedDomain: String,
    val operation: String, // REMOVE_BLOCK or ADD_ALLOW
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val effectiveAtEpochMillis: Long,
    val status: String = DomainChangeStatus.PENDING,
    val createdTimeZoneId: String = java.util.TimeZone.getDefault().id,
    val lastEvaluatedAtEpochMillis: Long = System.currentTimeMillis()
)

@Dao
interface DomainChangeRequestDao {
    @Query("SELECT * FROM domain_change_requests WHERE status = 'PENDING' ORDER BY effectiveAtEpochMillis ASC")
    fun getPendingRequestsFlow(): Flow<List<DomainChangeRequestEntity>>

    @Query("SELECT * FROM domain_change_requests WHERE status = 'PENDING' ORDER BY effectiveAtEpochMillis ASC")
    suspend fun getPendingRequests(): List<DomainChangeRequestEntity>

    @Query("SELECT * FROM domain_change_requests WHERE normalizedDomain = :domain AND status = 'PENDING' LIMIT 1")
    suspend fun getPendingRequestForDomain(domain: String): DomainChangeRequestEntity?

    @Query("SELECT * FROM domain_change_requests WHERE status = 'PENDING' AND effectiveAtEpochMillis <= :nowMillis")
    suspend fun getMaturedRequests(nowMillis: Long): List<DomainChangeRequestEntity>

    @Query("SELECT * FROM domain_change_requests ORDER BY createdAtEpochMillis DESC LIMIT 100")
    fun getAllRequestsFlow(): Flow<List<DomainChangeRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: DomainChangeRequestEntity): Long

    @Update
    suspend fun update(request: DomainChangeRequestEntity)

    @Query("UPDATE domain_change_requests SET status = 'CANCELLED', lastEvaluatedAtEpochMillis = :nowMillis WHERE id = :id AND status = 'PENDING'")
    suspend fun cancelRequest(id: Long, nowMillis: Long = System.currentTimeMillis()): Int

    @Query("UPDATE domain_change_requests SET status = 'CANCELLED', lastEvaluatedAtEpochMillis = :nowMillis WHERE normalizedDomain = :domain AND status = 'PENDING'")
    suspend fun cancelPendingForDomain(domain: String, nowMillis: Long = System.currentTimeMillis()): Int

    @Query("UPDATE domain_change_requests SET status = 'APPLIED', lastEvaluatedAtEpochMillis = :nowMillis WHERE id = :id")
    suspend fun markApplied(id: Long, nowMillis: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM domain_change_requests WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>
}

