package com.coconut.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SnapRepository(context: Context) {
    private val repo = CoconutRepository(context)

    fun observeSnapped(): Flow<List<SnappedAppEntity>> = repo.observeSnappedApps()

    suspend fun snapApp(
        packageName: String,
        applicationName: String,
        category: String
    ): Result<Unit> = repo.snapApp(packageName, applicationName, category)

    suspend fun removeSnap(packageName: String): Result<Unit> =
        repo.removeSnap(packageName)

    fun isAccessibilityPermissionGranted(): Boolean =
        repo.isAccessibilityPermissionGranted()
}
