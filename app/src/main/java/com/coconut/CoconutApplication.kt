package com.coconut

import android.app.Application
import com.coconut.data.AppDatabase
import com.coconut.policy.CoconutPolicyManager
import com.coconut.policy.PolicyReconciliationManager
import com.coconut.worker.ReconciliationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoconutApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val policyManager: CoconutPolicyManager by lazy { CoconutPolicyManager(this) }
    val reconciliationManager: PolicyReconciliationManager by lazy { PolicyReconciliationManager(this) }

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic background reconciliation safety net
        ReconciliationWorker.schedule(this)

        // Run startup policy reconciliation
        CoroutineScope(Dispatchers.IO).launch {
            if (policyManager.isDeviceOwner()) {
                // Ensure self-protection is active
                policyManager.setSelfProtection(true)
                reconciliationManager.reconcile("APP_STARTUP")
            }
        }
    }
}

