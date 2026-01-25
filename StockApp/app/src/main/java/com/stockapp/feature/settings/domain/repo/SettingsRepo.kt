package com.stockapp.feature.settings.domain.repo

import com.stockapp.feature.settings.domain.model.ApiKeyConfig
import com.stockapp.feature.settings.domain.model.KisApiKeyConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for settings storage.
 */
interface SettingsRepo {
    /**
     * Get Kiwoom API key configuration as a Flow.
     */
    fun getApiKeyConfig(): Flow<ApiKeyConfig>

    /**
     * Save Kiwoom API key configuration.
     */
    suspend fun saveApiKeyConfig(config: ApiKeyConfig)

    /**
     * Test Kiwoom API key by attempting to initialize PyClient.
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
     * Get KIS API key configuration as a Flow.
     */
    fun getKisApiKeyConfig(): Flow<KisApiKeyConfig>

    /**
     * Save KIS API key configuration.
     */
    suspend fun saveKisApiKeyConfig(config: KisApiKeyConfig)

    /**
     * Test KIS API key by attempting to get a token.
     * Returns true if connection is successful.
     */
    suspend fun testKisApiKey(config: KisApiKeyConfig): Result<Boolean>

    /**
     * Clear all settings.
     */
    suspend fun clearAll()
}
