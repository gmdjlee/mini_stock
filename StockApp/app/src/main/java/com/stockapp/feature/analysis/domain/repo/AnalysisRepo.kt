package com.stockapp.feature.analysis.domain.repo

import com.stockapp.feature.analysis.domain.model.StockData

/**
 * Analysis repository interface.
 */
interface AnalysisRepo {
    /**
     * Get stock supply/demand analysis.
     *
     * @param ticker Stock ticker code
     * @param days Number of days to fetch (default 180)
     * @param useCache Whether to use cached data
     * @return Result containing StockData or error
     */
    suspend fun getAnalysis(
        ticker: String,
        days: Int = 180,
        useCache: Boolean = true
    ): Result<StockData>

    /**
     * Get cached analysis data.
     *
     * @param ticker Stock ticker code
     * @return Cached StockData or null if not found/expired
     */
    suspend fun getCachedAnalysis(ticker: String): StockData?

    /**
     * Clear cached analysis for a ticker.
     */
    suspend fun clearCache(ticker: String)

    /**
     * Clear all analysis cache.
     */
    suspend fun clearAllCache()
}
