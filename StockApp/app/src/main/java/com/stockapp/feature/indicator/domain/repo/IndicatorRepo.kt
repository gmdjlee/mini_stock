package com.stockapp.feature.indicator.domain.repo

import com.stockapp.feature.indicator.domain.model.DemarkSetup
import com.stockapp.feature.indicator.domain.model.ElderImpulse
import com.stockapp.feature.indicator.domain.model.TrendSignal

/**
 * Repository interface for technical indicators.
 */
interface IndicatorRepo {

    /**
     * Get Trend Signal indicator.
     *
     * @param ticker Stock code
     * @param days Number of periods
     * @param timeframe "daily" or "weekly"
     * @param useCache Whether to use cached data
     */
    suspend fun getTrend(
        ticker: String,
        days: Int = 180,
        timeframe: String = "daily",
        useCache: Boolean = true
    ): Result<TrendSignal>

    /**
     * Get Elder Impulse indicator.
     *
     * @param ticker Stock code
     * @param days Number of periods
     * @param timeframe "daily" or "weekly"
     * @param useCache Whether to use cached data
     */
    suspend fun getElder(
        ticker: String,
        days: Int = 180,
        timeframe: String = "daily",
        useCache: Boolean = true
    ): Result<ElderImpulse>

    /**
     * Get DeMark TD Setup indicator.
     *
     * @param ticker Stock code
     * @param days Number of periods
     * @param timeframe "daily", "weekly", or "monthly"
     * @param useCache Whether to use cached data
     */
    suspend fun getDemark(
        ticker: String,
        days: Int = 180,
        timeframe: String = "daily",
        useCache: Boolean = true
    ): Result<DemarkSetup>

    /**
     * Clear cached indicator data for a ticker.
     */
    suspend fun clearCache(ticker: String)

    /**
     * Clear all expired cache entries.
     */
    suspend fun clearExpiredCache()
}
