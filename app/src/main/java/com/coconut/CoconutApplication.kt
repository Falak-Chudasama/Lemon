package com.coconut

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.work.WorkManager
import com.coconut.data.AppDatabase
import com.coconut.data.CoconutRepository
import com.coconut.policy.LemonNotificationManager
import com.coconut.policy.LemonPolicyCoordinator
import com.coconut.policy.QuarantineManager
import com.coconut.security.AppLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoconutApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: CoconutRepository by lazy { CoconutRepository(this) }

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize notification channels
        LemonNotificationManager.initializeChannels(this)

        // 2. Lifecycle-aware App Lock: lock session whenever Lemon leaves the foreground
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedActivityCount = 0
            private var isChangingConfigurations = false

            override fun onActivityStarted(activity: Activity) {
                startedActivityCount++
            }

            override fun onActivityStopped(activity: Activity) {
                isChangingConfigurations = activity.isChangingConfigurations
                startedActivityCount--
                if (startedActivityCount <= 0 && !isChangingConfigurations) {
                    // App left foreground: user pressed Home, switched apps, or opened Recents
                    AppLockManager.lockSession()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // 3. Reconcile policies on launch
        CoroutineScope(Dispatchers.IO).launch {
            QuarantineManager.getInstance(this@CoconutApplication).reconcile()
            LemonPolicyCoordinator.getInstance(this@CoconutApplication).reconcile("application_start")
        }

        // Cancel legacy background DPC worker if previously registered
        try {
            WorkManager.getInstance(this).cancelUniqueWork("coconut_policy_reconciliation")
        } catch (_: Exception) {}
    }
}
