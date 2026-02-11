package com.stockapp.feature.realtime.data.repo

import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.config.FeatureFlags
import com.stockapp.feature.realtime.domain.model.RealtimeSupplyData
import com.stockapp.feature.realtime.domain.repo.RealtimeSupplyRepo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Selector for RealtimeSupplyRepo implementation.
 *
 * Uses ENABLE_REALTIME_SUPPLY feature flag to control the realtime supply feature.
 */
@Singleton
class RealtimeSupplyRepoSelector @Inject constructor(
    private val nativeRepo: NativeRealtimeSupplyRepoImpl,
    private val featureFlagRepo: FeatureFlagRepo
) : RealtimeSupplyRepo {

    /**
     * Select repository based on feature flag.
     */
    private suspend fun selectRepo(): RealtimeSupplyRepo {
        val useNative = featureFlagRepo.isEnabled(FeatureFlags.ENABLE_REALTIME_SUPPLY)
        // Note: Realtime supply is a new feature, only Native implementation exists
        return nativeRepo
    }

    /**
     * Check if realtime supply feature is enabled.
     */
    suspend fun isEnabled(): Boolean {
        return featureFlagRepo.isEnabled(FeatureFlags.ENABLE_REALTIME_SUPPLY)
    }

    override suspend fun getRealtimeSupply(
        ticker: String,
        useCache: Boolean
    ): Result<RealtimeSupplyData> {
        return selectRepo().getRealtimeSupply(ticker, useCache)
    }

    override suspend fun getCachedSupply(ticker: String): RealtimeSupplyData? {
        return selectRepo().getCachedSupply(ticker)
    }

    override suspend fun clearCache(ticker: String) {
        selectRepo().clearCache(ticker)
    }

    override suspend fun clearAllCache() {
        selectRepo().clearAllCache()
    }
}
