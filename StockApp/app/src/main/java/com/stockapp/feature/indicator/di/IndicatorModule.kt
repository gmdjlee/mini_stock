package com.stockapp.feature.indicator.di

import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.py.PyClient
import com.stockapp.core.stock.data.OhlcvService
import com.stockapp.feature.indicator.data.repo.IndicatorRepoImpl
import com.stockapp.feature.indicator.data.repo.IndicatorRepoSelector
import com.stockapp.feature.indicator.data.repo.NativeIndicatorRepoImpl
import com.stockapp.feature.indicator.domain.repo.IndicatorRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IndicatorModule {

    /**
     * Provides the Python-based indicator repository.
     * Used as fallback when native implementation is disabled.
     */
    @Provides
    @Singleton
    fun provideIndicatorRepoImpl(
        pyClient: PyClient,
        indicatorCacheDao: IndicatorCacheDao
    ): IndicatorRepoImpl {
        return IndicatorRepoImpl(pyClient, indicatorCacheDao)
    }

    /**
     * Provides the native Kotlin indicator repository.
     * Uses OhlcvService and native calculators.
     */
    @Provides
    @Singleton
    fun provideNativeIndicatorRepoImpl(
        ohlcvService: OhlcvService,
        indicatorCacheDao: IndicatorCacheDao
    ): NativeIndicatorRepoImpl {
        return NativeIndicatorRepoImpl(ohlcvService, indicatorCacheDao)
    }

    /**
     * Provides the indicator repository selector.
     * Switches between Python and Kotlin implementations based on feature flag.
     */
    @Provides
    @Singleton
    fun provideIndicatorRepo(
        nativeRepo: NativeIndicatorRepoImpl,
        pyRepo: IndicatorRepoImpl,
        featureFlagRepo: FeatureFlagRepo
    ): IndicatorRepo {
        return IndicatorRepoSelector(nativeRepo, pyRepo, featureFlagRepo)
    }
}
