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
 * Feature flag keys for runtime feature toggling.
 */
object FeatureFlags {
    // Realtime supply (장중 실시간 투자자별 매매 데이터)
    const val ENABLE_REALTIME_SUPPLY = "enable_realtime_supply"

    // KRX direct data source (primary for batch data)
    // Disable if no Korean network/VPN access
    const val USE_KRX_DATA_SOURCE = "use_krx_data_source"

    /**
     * Default values for feature flags.
     */
    val DEFAULTS = mapOf(
        ENABLE_REALTIME_SUPPLY to true,
        USE_KRX_DATA_SOURCE to true
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
}
