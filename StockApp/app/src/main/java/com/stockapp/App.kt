package com.stockapp

import android.app.Application
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import com.stockapp.core.cache.StockCacheManager
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "App"

@HiltAndroidApp
class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun settingsRepo(): SettingsRepo
        fun stockCacheManager(): StockCacheManager
    }

    override fun onCreate() {
        super.onCreate()
        initializeApp()
    }

    private fun initializeApp() {
        applicationScope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@App,
                    AppEntryPoint::class.java
                )

                // 1. Initialize PyClient with saved API keys
                Log.d(TAG, "Initializing PyClient with saved keys...")
                val settingsRepo = entryPoint.settingsRepo()
                val initResult = settingsRepo.initializeWithSavedKeys()

                initResult.fold(
                    onSuccess = { initialized ->
                        if (initialized) {
                            Log.d(TAG, "PyClient initialized successfully")

                            // 2. Initialize stock cache after PyClient is ready
                            // Small delay to ensure PyClient is fully ready
                            delay(500)

                            Log.d(TAG, "Initializing stock cache...")
                            val cacheManager = entryPoint.stockCacheManager()
                            val cacheResult = cacheManager.initializeIfNeeded()

                            cacheResult.fold(
                                onSuccess = { count ->
                                    Log.d(TAG, "Stock cache initialized with $count stocks")
                                },
                                onFailure = { e ->
                                    Log.w(TAG, "Stock cache initialization failed: ${e.message}")
                                }
                            )
                        } else {
                            Log.w(TAG, "No API keys configured, skipping cache init")
                        }
                    },
                    onFailure = { e ->
                        Log.w(TAG, "PyClient initialization failed: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "App initialization error: ${e.message}", e)
                // Silently fail - user will need to configure API keys in settings
            }
        }
    }
}
