package com.stockapp.feature.settings.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stockapp.core.py.PyClient
import com.stockapp.feature.settings.domain.model.ApiKeyConfig
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

@Singleton
class SettingsRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pyClient: PyClient
) : SettingsRepo {

    private object Keys {
        val APP_KEY = stringPreferencesKey("api_app_key")
        val SECRET_KEY = stringPreferencesKey("api_secret_key")
        val IS_PRODUCTION = booleanPreferencesKey("is_production")
    }

    override fun getApiKeyConfig(): Flow<ApiKeyConfig> {
        return context.dataStore.data.map { prefs ->
            ApiKeyConfig(
                appKey = prefs[Keys.APP_KEY] ?: "",
                secretKey = prefs[Keys.SECRET_KEY] ?: "",
                investmentMode = if (prefs[Keys.IS_PRODUCTION] == true) {
                    InvestmentMode.PRODUCTION
                } else {
                    InvestmentMode.MOCK
                }
            )
        }
    }

    override suspend fun saveApiKeyConfig(config: ApiKeyConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_KEY] = config.appKey
            prefs[Keys.SECRET_KEY] = config.secretKey
            prefs[Keys.IS_PRODUCTION] = config.investmentMode == InvestmentMode.PRODUCTION
        }
    }

    override suspend fun testApiKey(config: ApiKeyConfig): Result<Boolean> {
        return try {
            if (!config.isValid()) {
                return Result.failure(IllegalArgumentException("API Key와 Secret Key를 입력해주세요"))
            }

            // Select URL based on investment mode
            val baseUrl = when (config.investmentMode) {
                InvestmentMode.MOCK -> MOCK_URL
                InvestmentMode.PRODUCTION -> PROD_URL
            }

            // Try to initialize PyClient with the provided keys
            pyClient.initialize(
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = baseUrl
            ).fold(
                onSuccess = {
                    Result.success(true)
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun initializeWithSavedKeys(): Result<Boolean> {
        return try {
            val config = getApiKeyConfig().first()
            if (config.isValid()) {
                testApiKey(config)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        private const val PROD_URL = "https://api.kiwoom.com"
        private const val MOCK_URL = "https://mockapi.kiwoom.com"
    }
}
