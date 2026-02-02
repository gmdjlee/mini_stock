package com.stockapp.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature flag keys for gradual migration from Python to Kotlin Native.
 * Used to toggle between Python (Chaquopy) and Kotlin implementations.
 */
object FeatureFlags {
    // Search feature
    const val USE_NATIVE_SEARCH = "use_native_search"

    // Analysis feature
    const val USE_NATIVE_ANALYSIS = "use_native_analysis"

    // Indicator feature
    const val USE_NATIVE_INDICATOR = "use_native_indicator"

    // Realtime supply (new feature, Kotlin only)
    const val ENABLE_REALTIME_SUPPLY = "enable_realtime_supply"

    /**
     * Default values for feature flags.
     * Initially all native features are disabled (Python fallback).
     */
    val DEFAULTS = mapOf(
        USE_NATIVE_SEARCH to false,
        USE_NATIVE_ANALYSIS to false,
        USE_NATIVE_INDICATOR to false,
        ENABLE_REALTIME_SUPPLY to false
    )
}

/**
 * Repository interface for feature flag management.
 */
interface FeatureFlagRepo {
    /**
     * Check if a feature flag is enabled.
     *
     * @param flag Flag key from [FeatureFlags]
     * @return true if enabled, false otherwise
     */
    suspend fun isEnabled(flag: String): Boolean

    /**
     * Set a feature flag value.
     *
     * @param flag Flag key from [FeatureFlags]
     * @param enabled true to enable, false to disable
     */
    suspend fun setEnabled(flag: String, enabled: Boolean)

    /**
     * Observe a feature flag value as a Flow.
     *
     * @param flag Flag key from [FeatureFlags]
     * @return Flow emitting boolean values
     */
    fun observeFlag(flag: String): Flow<Boolean>

    /**
     * Get all feature flag values.
     *
     * @return Map of flag key to enabled status
     */
    suspend fun getAllFlags(): Map<String, Boolean>

    /**
     * Reset all flags to default values.
     */
    suspend fun resetToDefaults()

    /**
     * Enable all native features (for testing or full migration).
     */
    suspend fun enableAllNative()

    /**
     * Disable all native features (fallback to Python).
     */
    suspend fun disableAllNative()
}

// DataStore instance for feature flags
private val Context.featureFlagsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feature_flags"
)

/**
 * DataStore-based implementation of [FeatureFlagRepo].
 */
@Singleton
class FeatureFlagRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FeatureFlagRepo {

    private val dataStore = context.featureFlagsDataStore

    override suspend fun isEnabled(flag: String): Boolean {
        val key = booleanPreferencesKey(flag)
        val prefs = dataStore.data.first()
        return prefs[key] ?: (FeatureFlags.DEFAULTS[flag] ?: false)
    }

    override suspend fun setEnabled(flag: String, enabled: Boolean) {
        val key = booleanPreferencesKey(flag)
        dataStore.edit { prefs ->
            prefs[key] = enabled
        }
    }

    override fun observeFlag(flag: String): Flow<Boolean> {
        val key = booleanPreferencesKey(flag)
        return dataStore.data.map { prefs ->
            prefs[key] ?: (FeatureFlags.DEFAULTS[flag] ?: false)
        }
    }

    override suspend fun getAllFlags(): Map<String, Boolean> {
        val prefs = dataStore.data.first()
        return FeatureFlags.DEFAULTS.keys.associateWith { flag ->
            val key = booleanPreferencesKey(flag)
            prefs[key] ?: (FeatureFlags.DEFAULTS[flag] ?: false)
        }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            FeatureFlags.DEFAULTS.forEach { (flag, defaultValue) ->
                val key = booleanPreferencesKey(flag)
                prefs[key] = defaultValue
            }
        }
    }

    override suspend fun enableAllNative() {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(FeatureFlags.USE_NATIVE_SEARCH)] = true
            prefs[booleanPreferencesKey(FeatureFlags.USE_NATIVE_ANALYSIS)] = true
            prefs[booleanPreferencesKey(FeatureFlags.USE_NATIVE_INDICATOR)] = true
            prefs[booleanPreferencesKey(FeatureFlags.ENABLE_REALTIME_SUPPLY)] = true
        }
    }

    override suspend fun disableAllNative() {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(FeatureFlags.USE_NATIVE_SEARCH)] = false
            prefs[booleanPreferencesKey(FeatureFlags.USE_NATIVE_ANALYSIS)] = false
            prefs[booleanPreferencesKey(FeatureFlags.USE_NATIVE_INDICATOR)] = false
            prefs[booleanPreferencesKey(FeatureFlags.ENABLE_REALTIME_SUPPLY)] = false
        }
    }
}
