package com.stockapp.feature.settings.domain.repo

import com.stockapp.feature.settings.domain.model.ApiKeyConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for settings storage.
 */
interface SettingsRepo {
    /**
     * Get API key configuration as a Flow.
     */
    fun getApiKeyConfig(): Flow<ApiKeyConfig>

    /**
     * Save API key configuration.
     */
    suspend fun saveApiKeyConfig(config: ApiKeyConfig)

    /**
     * Test API key by attempting to initialize PyClient.
     * Returns true if connection is successful.
     */
    suspend fun testApiKey(config: ApiKeyConfig): Result<Boolean>

    /**
     * Initialize PyClient with saved API keys if available.
     * Should be called at app startup.
     * Returns true if initialization was successful, false if no saved keys.
     */
    suspend fun initializeWithSavedKeys(): Result<Boolean>

    /**
     * Clear all settings.
     */
    suspend fun clearAll()
}
