package com.coconut.dns

import android.content.Context
import com.coconut.data.AppDatabase
import com.coconut.data.DomainChangeOperation
import com.coconut.data.DomainChangeRequestEntity
import com.coconut.data.DomainChangeStatus
import com.coconut.data.DomainRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DomainQueueManager(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val requestDao = db.domainChangeRequestDao()
    private val ruleDao = db.domainRuleDao()

    companion object {
        // Exact 30 days = 720 hours = 2,592,000,000 milliseconds
        const val WAIT_DURATION_MS = 30L * 24 * 3600 * 1000L
    }

    val pendingRequestsFlow: Flow<List<DomainChangeRequestEntity>> = requestDao.getPendingRequestsFlow()

    /**
     * Queues the removal of an existing blocked domain.
     * Protection remains in full force for exactly 30 days (720 hours).
     */
    suspend fun queueRemoveBlock(domain: String): Result<DomainChangeRequestEntity> = withContext(Dispatchers.IO) {
        val normalized = DomainPolicyEngine.normalizeDomain(domain)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid domain format"))

        val existingRule = ruleDao.getRuleByDomain(normalized)
        if (existingRule == null || existingRule.ruleType != "BLOCK") {
            return@withContext Result.failure(IllegalStateException("$normalized is not currently blocked"))
        }

        val existingPending = requestDao.getPendingRequestForDomain(normalized)
        if (existingPending != null && existingPending.operation == DomainChangeOperation.REMOVE_BLOCK) {
            return@withContext Result.success(existingPending)
        }

        val now = System.currentTimeMillis()
        val effectiveAt = now + WAIT_DURATION_MS
        val entity = DomainChangeRequestEntity(
            normalizedDomain = normalized,
            operation = DomainChangeOperation.REMOVE_BLOCK,
            createdAtEpochMillis = now,
            effectiveAtEpochMillis = effectiveAt,
            status = DomainChangeStatus.PENDING,
            createdTimeZoneId = java.util.TimeZone.getDefault().id,
            lastEvaluatedAtEpochMillis = now
        )

        val id = requestDao.insert(entity)
        Result.success(entity.copy(id = id))
    }

    /**
     * Queues the addition of an allowed domain.
     * The domain is NOT allowed until the 30-day (720 hours) delay period expires.
     */
    suspend fun queueAddAllow(domain: String): Result<DomainChangeRequestEntity> = withContext(Dispatchers.IO) {
        val normalized = DomainPolicyEngine.normalizeDomain(domain)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid domain format"))

        // Database-level exclusivity check: Cannot add allow if actively blocked
        val existingRule = ruleDao.getRuleByDomain(normalized)
        if (existingRule != null && existingRule.ruleType == "BLOCK") {
            return@withContext Result.failure(IllegalStateException("Cannot allow $normalized while actively blocked. Remove the block first."))
        }

        val existingPending = requestDao.getPendingRequestForDomain(normalized)
        if (existingPending != null && existingPending.operation == DomainChangeOperation.ADD_ALLOW) {
            return@withContext Result.success(existingPending)
        }

        val now = System.currentTimeMillis()
        val effectiveAt = now + WAIT_DURATION_MS
        val entity = DomainChangeRequestEntity(
            normalizedDomain = normalized,
            operation = DomainChangeOperation.ADD_ALLOW,
            createdAtEpochMillis = now,
            effectiveAtEpochMillis = effectiveAt,
            status = DomainChangeStatus.PENDING,
            createdTimeZoneId = java.util.TimeZone.getDefault().id,
            lastEvaluatedAtEpochMillis = now
        )

        val id = requestDao.insert(entity)
        Result.success(entity.copy(id = id))
    }

    /**
     * Cancels a pending domain change request at any time.
     * Cancellation aborts the weakening action, preserving strong filtering.
     */
    suspend fun cancelRequest(requestId: Long): Boolean = withContext(Dispatchers.IO) {
        requestDao.cancelRequest(requestId) > 0
    }

    /**
     * Evaluates and applies all matured pending requests whose deadline (720h) has passed.
     */
    suspend fun applyMaturedRequests(): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val matured = requestDao.getMaturedRequests(now)
        var appliedCount = 0

        for (request in matured) {
            when (request.operation) {
                DomainChangeOperation.REMOVE_BLOCK -> {
                    ruleDao.deleteByDomain(request.normalizedDomain)
                    requestDao.markApplied(request.id, now)
                    appliedCount++
                }
                DomainChangeOperation.ADD_ALLOW -> {
                    // Exclusivity check: Ensure no active block rule exists
                    val currentBlock = ruleDao.getRuleByDomain(request.normalizedDomain)
                    if (currentBlock == null || currentBlock.ruleType != "BLOCK") {
                        ruleDao.insertOrUpdate(
                            DomainRuleEntity(
                                domain = request.normalizedDomain,
                                ruleType = "ALLOW",
                                isEnabled = true,
                                source = "USER",
                                createdAt = request.createdAtEpochMillis,
                                updatedAt = now
                            )
                        )
                        requestDao.markApplied(request.id, now)
                        appliedCount++
                    } else {
                        // Conflict: Domain is actively blocked; reject allow
                        requestDao.update(
                            request.copy(
                                status = DomainChangeStatus.REJECTED,
                                lastEvaluatedAtEpochMillis = now
                            )
                        )
                    }
                }
            }
        }

        appliedCount
    }
}

