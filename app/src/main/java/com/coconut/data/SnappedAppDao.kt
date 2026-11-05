package com.coconut.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SnappedAppDao {
    @Query("SELECT * FROM snapped_apps ORDER BY applicationName ASC")
    fun observeAll(): Flow<List<SnappedAppEntity>>

    @Query("SELECT * FROM snapped_apps ORDER BY applicationName ASC")
    suspend fun getAllOnce(): List<SnappedAppEntity>

    @Query("SELECT * FROM snapped_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): SnappedAppEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM snapped_apps WHERE packageName = :packageName)")
    suspend fun isSnapped(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: SnappedAppEntity): Long

    @Update
    suspend fun update(app: SnappedAppEntity): Int

    @Delete
    suspend fun delete(app: SnappedAppEntity): Int

    @Query("DELETE FROM snapped_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String): Int

    @Query("SELECT COUNT(*) FROM snapped_apps")
    suspend fun count(): Int
}
