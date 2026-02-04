package com.stockapp.feature.analysis.data.repo

import android.util.Log
import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.config.FeatureFlags
import com.stockapp.feature.analysis.domain.model.StockData
import com.stockapp.feature.analysis.domain.repo.AnalysisRepo
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AnalysisRepoSelector"

/**
 * Repository selector that delegates to either Python or Native Kotlin implementation
 * based on feature flag configuration.
 *
 * This enables gradual migration from Python (Chaquopy) to native Kotlin.
 */
@Singleton
class AnalysisRepoSelector @Inject constructor(
    private val nativeRepo: NativeAnalysisRepoImpl,
    private val pyRepo: AnalysisRepoImpl,
    private val featureFlagRepo: FeatureFlagRepo
) : AnalysisRepo {

    /**
     * Select the appropriate repository based on feature flag.
     */
    private suspend fun selectRepo(): AnalysisRepo {
        val useNative = featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)
        Log.d(TAG, "selectRepo() useNative=$useNative")
        return if (useNative) nativeRepo else pyRepo
    }

    override suspend fun getAnalysis(
        ticker: String,
        days: Int,
        useCache: Boolean
    ): Result<StockData> {
        return selectRepo().getAnalysis(ticker, days, useCache)
    }

    override suspend fun getAnalysisWithIntraday(
        ticker: String,
        days: Int,
        useCache: Boolean
    ): Result<StockData> {
        return selectRepo().getAnalysisWithIntraday(ticker, days, useCache)
    }

    override suspend fun getCachedAnalysis(ticker: String): StockData? {
        // Cache is shared between implementations - use native for direct access
        return nativeRepo.getCachedAnalysis(ticker)
    }

    override suspend fun clearCache(ticker: String) {
        // Cache is shared between implementations
        nativeRepo.clearCache(ticker)
    }

    override suspend fun clearAllCache() {
        // Cache is shared between implementations
        nativeRepo.clearAllCache()
    }
}
