package com.stockapp.core.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

/**
 * Manages app theme (dark/light mode) with DataStore persistence.
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val USE_SYSTEM_THEME = booleanPreferencesKey("use_system_theme")
    }

    /**
     * Flow that emits the current theme mode.
     * null means follow system theme, true means dark, false means light.
     */
    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val useSystemTheme = prefs[Keys.USE_SYSTEM_THEME] ?: true
        if (useSystemTheme) {
            ThemeMode.System
        } else {
            val isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false
            if (isDarkMode) ThemeMode.Dark else ThemeMode.Light
        }
    }

    /**
     * Flow that emits whether dark mode is currently enabled.
     * This is for simpler use cases where you just need the boolean value.
     */
    val isDarkMode: Flow<Boolean?> = context.themeDataStore.data.map { prefs ->
        val useSystemTheme = prefs[Keys.USE_SYSTEM_THEME] ?: true
        if (useSystemTheme) {
            null // Follow system
        } else {
            prefs[Keys.IS_DARK_MODE] ?: false
        }
    }

    /**
     * Set dark mode explicitly.
     */
    suspend fun setDarkMode(isDark: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.USE_SYSTEM_THEME] = false
            prefs[Keys.IS_DARK_MODE] = isDark
        }
    }

    /**
     * Toggle between dark and light mode.
     * If currently following system theme, switches to opposite of provided current state.
     */
    suspend fun toggleDarkMode(currentIsDark: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.USE_SYSTEM_THEME] = false
            prefs[Keys.IS_DARK_MODE] = !currentIsDark
        }
    }

    /**
     * Use system theme setting.
     */
    suspend fun useSystemTheme() {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.USE_SYSTEM_THEME] = true
        }
    }
}

/**
 * Represents the theme mode.
 */
sealed class ThemeMode {
    data object System : ThemeMode()
    data object Light : ThemeMode()
    data object Dark : ThemeMode()
}
