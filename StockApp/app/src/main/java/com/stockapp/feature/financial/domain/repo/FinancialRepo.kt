package com.stockapp.feature.financial.domain.repo

import com.stockapp.feature.financial.domain.model.FinancialData

/**
 * Repository interface for financial data operations.
 */
interface FinancialRepo {
    /**
     * Get financial data for a stock.
     * Fetches from cache if available and not expired, otherwise from API.
     *
     * @param ticker Stock ticker code (6 digits)
     * @param name Stock name (for caching)
     * @param useCache Whether to use cached data if available
     * @return Result containing FinancialData or error
     */
    suspend fun getFinancialData(
        ticker: String,
        name: String,
        useCache: Boolean = true
    ): Result<FinancialData>

    /**
     * Refresh financial data from API, bypassing cache.
     *
     * @param ticker Stock ticker code (6 digits)
     * @param name Stock name (for caching)
     * @return Result containing FinancialData or error
     */
    suspend fun refreshFinancialData(
        ticker: String,
        name: String
    ): Result<FinancialData>

    /**
     * Clear cached financial data for a stock.
     *
     * @param ticker Stock ticker code
     */
    suspend fun clearCache(ticker: String)

    /**
     * Clear all expired cache entries.
     */
    suspend fun clearExpiredCache()
}
