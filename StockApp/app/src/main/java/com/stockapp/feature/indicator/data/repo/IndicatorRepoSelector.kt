package com.stockapp.feature.indicator.data.repo

import android.util.Log
import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.config.FeatureFlags
import com.stockapp.feature.indicator.domain.model.DemarkSetup
import com.stockapp.feature.indicator.domain.model.ElderImpulse
import com.stockapp.feature.indicator.domain.model.TrendSignal
import com.stockapp.feature.indicator.domain.repo.IndicatorRepo
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "IndicatorRepoSelector"

/**
 * Repository selector that switches between Python and Kotlin implementations
 * based on feature flag configuration.
 *
 * This enables gradual rollout of the native Kotlin implementation.
 */
@Singleton
class IndicatorRepoSelector @Inject constructor(
    private val nativeRepo: NativeIndicatorRepoImpl,
    private val pyRepo: IndicatorRepoImpl,
    private val featureFlagRepo: FeatureFlagRepo
) : IndicatorRepo {

    /**
     * Select the appropriate repository based on feature flag.
     */
    private suspend fun selectRepo(): IndicatorRepo {
        val useNative = featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)
        Log.d(TAG, "selectRepo() useNative=$useNative")
        return if (useNative) nativeRepo else pyRepo
    }

    override suspend fun getTrend(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<TrendSignal> {
        return selectRepo().getTrend(ticker, days, timeframe, useCache)
    }

    override suspend fun getElder(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<ElderImpulse> {
        return selectRepo().getElder(ticker, days, timeframe, useCache)
    }

    override suspend fun getDemark(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<DemarkSetup> {
        return selectRepo().getDemark(ticker, days, timeframe, useCache)
    }

    override suspend fun clearCache(ticker: String) {
        // Clear cache in both repositories
        nativeRepo.clearCache(ticker)
        pyRepo.clearCache(ticker)
    }

    override suspend fun clearExpiredCache() {
        // Clear expired cache in both repositories
        nativeRepo.clearExpiredCache()
        pyRepo.clearExpiredCache()
    }
}
