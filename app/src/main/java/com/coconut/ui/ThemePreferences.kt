package com.coconut.ui

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("coconut_theme_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
        private val _themeModeFlow = MutableStateFlow(AppThemeMode.SYSTEM)
        val themeModeFlow: StateFlow<AppThemeMode> = _themeModeFlow.asStateFlow()
    }

    init {
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        _themeModeFlow.value = try {
            AppThemeMode.valueOf(saved)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeModeFlow.value = mode
    }

    fun getThemeMode(): AppThemeMode {
        return _themeModeFlow.value
    }
}

