package com.coconut.data

import android.content.Context
import com.coconut.policy.PolicyResult
import kotlinx.coroutines.flow.Flow

class SnapRepository(context: Context) {
    private val repo = CoconutRepository(context)

    fun observeSnapped(): Flow<List<SnappedAppEntity>> = repo.observeSnappedApps()

    suspend fun snapApp(
        packageName: String,
        applicationName: String,
        category: String
    ): PolicyResult = repo.snapApp(packageName, applicationName, category)

    suspend fun unsnapApp(packageName: String): PolicyResult = repo.unsnapApp(packageName)

    suspend fun reconcile() {
        repo.reconcile("SNAP_REPO_CALL")
    }

    fun isDeviceOwner(): Boolean = repo.isDeviceOwner()
}
