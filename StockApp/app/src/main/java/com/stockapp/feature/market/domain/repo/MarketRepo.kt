package com.stockapp.feature.market.domain.repo

import com.stockapp.feature.market.domain.model.MarketIndicators

/**
 * Repository interface for market indicators.
 */
interface MarketRepo {
    /**
     * Get combined market indicators (deposit + credit).
     *
     * @param days Number of days to retrieve
     * @param useCache Whether to use cached data if available
     * @return Result with MarketIndicators or error
     */
    suspend fun getMarketIndicators(days: Int = 30, useCache: Boolean = true): Result<MarketIndicators>

    /**
     * Clear cached market data.
     */
    suspend fun clearCache()
}
