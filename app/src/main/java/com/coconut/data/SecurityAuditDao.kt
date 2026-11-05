package com.coconut.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityAuditDao {
    @Query("SELECT * FROM security_audit_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = 100): Flow<List<SecurityAuditEventEntity>>

    @Query("SELECT * FROM security_audit_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 100): List<SecurityAuditEventEntity>

    @Insert
    suspend fun insert(event: SecurityAuditEventEntity): Long

    @Query("DELETE FROM security_audit_events WHERE timestamp < :cutoff")
    suspend fun pruneOldEvents(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM security_audit_events")
    suspend fun count(): Int
}

