package com.stockapp

import android.app.Application
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SettingsRepoEntryPoint {
        fun settingsRepo(): SettingsRepo
    }

    override fun onCreate() {
        super.onCreate()
        initializePyClientWithSavedKeys()
    }

    private fun initializePyClientWithSavedKeys() {
        applicationScope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@App,
                    SettingsRepoEntryPoint::class.java
                )
                val settingsRepo = entryPoint.settingsRepo()
                settingsRepo.initializeWithSavedKeys()
            } catch (e: Exception) {
                // Silently fail - user will need to configure API keys in settings
            }
        }
    }
}
