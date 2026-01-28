package com.stockapp.feature.settings.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.stockapp.core.api.KisApiClient
import com.stockapp.core.api.KisApiConfig
import com.stockapp.core.py.PyClient
import com.stockapp.feature.settings.domain.model.ApiKeyConfig
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.model.KisApiKeyConfig
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

@Singleton
class SettingsRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pyClient: PyClient,
    private val kisApiClient: KisApiClient
) : SettingsRepo {

    private object Keys {
        // Non-sensitive settings in DataStore
        val IS_PRODUCTION = booleanPreferencesKey("is_production")
        val KIS_IS_PRODUCTION = booleanPreferencesKey("kis_is_production")

        // Sensitive Kiwoom API keys stored in EncryptedSharedPreferences
        const val APP_KEY = "api_app_key"
        const val SECRET_KEY = "api_secret_key"

        // Sensitive KIS API keys stored in EncryptedSharedPreferences
        const val KIS_APP_KEY = "kis_api_app_key"
        const val KIS_APP_SECRET = "kis_api_app_secret"

        const val ENCRYPTED_PREFS_NAME = "secure_api_prefs"
    }

    // Lazy initialization of encrypted shared preferences
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            Keys.ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getApiKeyConfig(): Flow<ApiKeyConfig> = flow {
        // Read sensitive keys from encrypted storage
        val appKey = encryptedPrefs.getString(Keys.APP_KEY, "") ?: ""
        val secretKey = encryptedPrefs.getString(Keys.SECRET_KEY, "") ?: ""

        // Read non-sensitive settings from DataStore
        val prefs = context.dataStore.data.first()
        val investmentMode = if (prefs[Keys.IS_PRODUCTION] == true) {
            InvestmentMode.PRODUCTION
        } else {
            InvestmentMode.MOCK
        }

        emit(ApiKeyConfig(
            appKey = appKey,
            secretKey = secretKey,
            investmentMode = investmentMode
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun saveApiKeyConfig(config: ApiKeyConfig) {
        withContext(Dispatchers.IO) {
            // Save sensitive API keys to encrypted storage
            encryptedPrefs.edit()
                .putString(Keys.APP_KEY, config.appKey)
                .putString(Keys.SECRET_KEY, config.secretKey)
                .apply()

            // Save non-sensitive settings to DataStore
            context.dataStore.edit { prefs ->
                prefs[Keys.IS_PRODUCTION] = config.investmentMode == InvestmentMode.PRODUCTION
            }
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

            // Test connection without modifying global PyClient state
            // This prevents race conditions with concurrent API calls
            pyClient.testConnection(
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = baseUrl
            ).fold(
                onSuccess = {
                    // Test passed, now initialize the actual client
                    pyClient.initialize(
                        appKey = config.appKey,
                        secretKey = config.secretKey,
                        baseUrl = baseUrl
                    ).fold(
                        onSuccess = { Result.success(true) },
                        onFailure = { e -> Result.failure(e) }
                    )
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
                // Initialize without network test to avoid startup failures
                // when network is unavailable
                val baseUrl = when (config.investmentMode) {
                    InvestmentMode.MOCK -> MOCK_URL
                    InvestmentMode.PRODUCTION -> PROD_URL
                }

                pyClient.initialize(
                    appKey = config.appKey,
                    secretKey = config.secretKey,
                    baseUrl = baseUrl
                ).fold(
                    onSuccess = { Result.success(true) },
                    onFailure = { e -> Result.failure(e) }
                )
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            // Clear encrypted API keys (both Kiwoom and KIS)
            encryptedPrefs.edit()
                .remove(Keys.APP_KEY)
                .remove(Keys.SECRET_KEY)
                .remove(Keys.KIS_APP_KEY)
                .remove(Keys.KIS_APP_SECRET)
                .apply()

            // Clear non-sensitive settings
            context.dataStore.edit { prefs ->
                prefs.clear()
            }

            // Clear KIS token cache
            kisApiClient.clearToken()
        }
    }

    // ============================================================
    // KIS API Key Configuration
    // ============================================================

    override fun getKisApiKeyConfig(): Flow<KisApiKeyConfig> = flow {
        val appKey = encryptedPrefs.getString(Keys.KIS_APP_KEY, "") ?: ""
        val appSecret = encryptedPrefs.getString(Keys.KIS_APP_SECRET, "") ?: ""

        // Read KIS investment mode from DataStore
        val prefs = context.dataStore.data.first()
        val investmentMode = if (prefs[Keys.KIS_IS_PRODUCTION] == true) {
            InvestmentMode.PRODUCTION
        } else {
            InvestmentMode.MOCK
        }

        emit(KisApiKeyConfig(
            appKey = appKey,
            appSecret = appSecret,
            investmentMode = investmentMode
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun saveKisApiKeyConfig(config: KisApiKeyConfig) {
        withContext(Dispatchers.IO) {
            // Save sensitive API keys to encrypted storage
            encryptedPrefs.edit()
                .putString(Keys.KIS_APP_KEY, config.appKey)
                .putString(Keys.KIS_APP_SECRET, config.appSecret)
                .apply()

            // Save KIS investment mode to DataStore
            context.dataStore.edit { prefs ->
                prefs[Keys.KIS_IS_PRODUCTION] = config.investmentMode == InvestmentMode.PRODUCTION
            }

            // Clear token cache when config changes
            kisApiClient.clearToken()
        }
    }

    override suspend fun testKisApiKey(config: KisApiKeyConfig): Result<Boolean> {
        return try {
            if (!config.isValid()) {
                return Result.failure(IllegalArgumentException("KIS API App Key와 App Secret을 입력해주세요"))
            }

            val kisConfig = KisApiConfig(
                appKey = config.appKey,
                appSecret = config.appSecret,
                baseUrl = config.getBaseUrl()
            )

            // Test by fetching a token
            kisApiClient.getToken(kisConfig).fold(
                onSuccess = { Result.success(true) },
                onFailure = { e -> Result.failure(e) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val PROD_URL = "https://api.kiwoom.com"
        private const val MOCK_URL = "https://mockapi.kiwoom.com"
    }
}
