package com.coconut

import android.app.Application
import androidx.work.WorkManager
import com.coconut.data.AppDatabase
import com.coconut.data.CoconutRepository

class CoconutApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: CoconutRepository by lazy { CoconutRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Cancel legacy background DPC worker if previously registered
        try {
            WorkManager.getInstance(this).cancelUniqueWork("coconut_policy_reconciliation")
        } catch (_: Exception) {}
    }
}
