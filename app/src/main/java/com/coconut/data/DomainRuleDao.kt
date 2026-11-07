package com.coconut.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainRuleDao {
    @Query("SELECT * FROM domain_rules ORDER BY updatedAt DESC")
    fun getAllRulesFlow(): Flow<List<DomainRuleEntity>>

    @Query("SELECT * FROM domain_rules WHERE ruleType = :type ORDER BY updatedAt DESC")
    fun getRulesByTypeFlow(type: String): Flow<List<DomainRuleEntity>>

    @Query("SELECT * FROM domain_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<DomainRuleEntity>

    @Query("SELECT * FROM domain_rules WHERE domain = :domain LIMIT 1")
    suspend fun getRuleByDomain(domain: String): DomainRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: DomainRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<DomainRuleEntity>)

    @Update
    suspend fun update(rule: DomainRuleEntity)

    @Delete
    suspend fun delete(rule: DomainRuleEntity)

    @Query("DELETE FROM domain_rules WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("DELETE FROM domain_rules WHERE ruleType = :type")
    suspend fun clearByType(type: String)

    @Query("SELECT COUNT(*) FROM domain_rules WHERE ruleType = 'BLOCK'")
    fun getBlockedDomainCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM domain_rules WHERE ruleType = 'ALLOW'")
    fun getAllowedDomainCountFlow(): Flow<Int>
}

