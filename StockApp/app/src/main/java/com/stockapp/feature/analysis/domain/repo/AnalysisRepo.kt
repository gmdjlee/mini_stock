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
     * Get stock supply/demand analysis with intraday data integration.
     * During trading hours (09:00-15:30), this method fetches real-time
     * intraday data from ka10063 API and merges it with historical data.
     * After market close, it returns the standard analysis data.
     *
     * @param ticker Stock ticker code
     * @param days Number of days to fetch (default 180)
     * @param useCache Whether to use cached data
     * @return Result containing StockData with intraday data merged if applicable
     */
    suspend fun getAnalysisWithIntraday(
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
