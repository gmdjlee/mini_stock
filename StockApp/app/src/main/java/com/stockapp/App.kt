package com.stockapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import com.stockapp.core.cache.StockCacheManager
import com.stockapp.feature.scheduling.SchedulingManager
import com.stockapp.feature.scheduling.domain.repo.SchedulingRepo
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "App"

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun settingsRepo(): SettingsRepo
        fun stockCacheManager(): StockCacheManager
        fun schedulingRepo(): SchedulingRepo
        fun schedulingManager(): SchedulingManager
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

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

                            // 3. Initialize scheduling if enabled
                            initializeScheduling(entryPoint)
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

    private suspend fun initializeScheduling(entryPoint: AppEntryPoint) {
        try {
            Log.d(TAG, "Initializing scheduling...")
            val schedulingRepo = entryPoint.schedulingRepo()
            val schedulingManager = entryPoint.schedulingManager()

            val config = schedulingRepo.getConfig()
            if (config.isEnabled) {
                Log.d(TAG, "Scheduling daily sync at ${config.syncHour}:${config.syncMinute}")
                schedulingManager.scheduleDailySync(config.syncHour, config.syncMinute)
            } else {
                Log.d(TAG, "Scheduling is disabled")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Scheduling initialization failed: ${e.message}")
        }
    }
}
