package com.coconut.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("coconut_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"

        // In-memory process session unlock state. Resets automatically on process death/restart.
        private val _isSessionUnlocked = MutableStateFlow(false)
        val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

        fun unlockSession() {
            _isSessionUnlocked.value = true
        }

        fun lockSession() {
            _isSessionUnlocked.value = false
        }
    }

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    fun isLocked(): Boolean {
        return isAppLockEnabled && !_isSessionUnlocked.value
    }
}

