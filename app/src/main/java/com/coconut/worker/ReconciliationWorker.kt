package com.coconut.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.coconut.policy.PolicyReconciliationManager
import java.util.concurrent.TimeUnit

class ReconciliationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val reconciliationManager = PolicyReconciliationManager(applicationContext)
            reconciliationManager.reconcile("WORKMANAGER_PERIODIC")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "coconut_policy_reconciliation"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ReconciliationWorker>(
                6, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

