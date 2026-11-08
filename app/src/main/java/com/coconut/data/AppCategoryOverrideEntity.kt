package com.coconut.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_category_overrides")
data class AppCategoryOverrideEntity(
    @PrimaryKey
    val packageName: String,
    val category: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface AppCategoryOverrideDao {
    @Query("SELECT * FROM app_category_overrides")
    fun getAllOverridesFlow(): Flow<List<AppCategoryOverrideEntity>>

    @Query("SELECT * FROM app_category_overrides")
    suspend fun getAllOverrides(): List<AppCategoryOverrideEntity>

    @Query("SELECT category FROM app_category_overrides WHERE packageName = :packageName LIMIT 1")
    suspend fun getCategoryForPackage(packageName: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(override: AppCategoryOverrideEntity)

    @Query("DELETE FROM app_category_overrides WHERE packageName = :packageName")
    suspend fun deleteForPackage(packageName: String)
}

