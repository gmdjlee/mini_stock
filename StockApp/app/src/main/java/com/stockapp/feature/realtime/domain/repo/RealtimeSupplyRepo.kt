package com.stockapp.feature.realtime.domain.repo

import com.stockapp.feature.realtime.domain.model.RealtimeSupplyData

/**
 * Repository interface for realtime supply data.
 * Uses ka10063 API for intraday investor trend data.
 */
interface RealtimeSupplyRepo {

    /**
     * Get realtime supply data for a stock.
     *
     * @param ticker Stock ticker code
     * @param useCache Whether to use cached data (default: true)
     * @return Result containing RealtimeSupplyData or error
     */
    suspend fun getRealtimeSupply(
        ticker: String,
        useCache: Boolean = true
    ): Result<RealtimeSupplyData>

    /**
     * Get cached realtime supply data.
     *
     * @param ticker Stock ticker code
     * @return Cached RealtimeSupplyData or null if not found/expired
     */
    suspend fun getCachedSupply(ticker: String): RealtimeSupplyData?

    /**
     * Clear cached supply data for a ticker.
     */
    suspend fun clearCache(ticker: String)

    /**
     * Clear all realtime supply cache.
     */
    suspend fun clearAllCache()

    companion object {
        /**
         * Default cache TTL for realtime data (1 minute).
         * Shorter than analysis cache because data is time-sensitive.
         */
        const val DEFAULT_CACHE_TTL_MS = 60_000L
    }
}
