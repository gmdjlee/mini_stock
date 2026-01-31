package com.stockapp.core.di

import com.stockapp.BuildConfig
import com.stockapp.core.network.CertificateHashExtractor
import com.stockapp.core.network.CertificatePinningConfig
import com.stockapp.core.state.SelectedStockManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }

    /**
     * Provides OkHttpClient with security enhancements (P3).
     *
     * Features:
     * - Certificate pinning in release builds (MITM protection)
     * - HTTP logging in debug builds
     * - Standard timeouts (30s)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            // Debug: Enable HTTP logging, skip certificate pinning
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
            // Add certificate hash extractor to log SPKI hashes for pinning
            // Check Logcat with tag "CertHash" to get actual hashes
            builder.addNetworkInterceptor(CertificateHashExtractor())
            // Note: Certificate pinning disabled in debug for proxy tools
        } else {
            // Release: Enable certificate pinning for API security (if configured)
            CertificatePinningConfig.createPinner()?.let { pinner ->
                builder.certificatePinner(pinner)
            }
        }

        return builder.build()
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    fun provideSelectedStockManager(): SelectedStockManager = SelectedStockManager()
}
