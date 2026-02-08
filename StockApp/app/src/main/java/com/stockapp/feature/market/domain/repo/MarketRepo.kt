package com.stockapp.feature.market.domain.repo

import com.stockapp.feature.market.domain.model.BloodIndicatorHistory
import com.stockapp.feature.market.domain.model.FearGreedHistory
import com.stockapp.feature.market.domain.model.FundFlowHistory
import com.stockapp.feature.market.domain.model.MarketDateRange
import com.stockapp.feature.market.domain.model.MarketFearGreed
import com.stockapp.feature.market.domain.model.OscillatorHistory

/**
 * Repository interface for market indicator data.
 * 4 tabs: 공포/탐욕, 과매수/과매도, 자금 동향, Blood Indicator
 */
interface MarketRepo {

    /**
     * Get market-level Fear & Greed index.
     * Combines 5 KRX-based indicators.
     */
    suspend fun getFearGreedIndex(): Result<MarketFearGreed>

    /**
     * Get Fear & Greed historical data for chart.
     */
    suspend fun getFearGreedHistory(
        dateRange: MarketDateRange = MarketDateRange.THREE_MONTHS
    ): Result<FearGreedHistory>

    /**
     * Get market oscillator data (advance/decline ratio).
     * Calculates from all-stock OHLCV data.
     */
    suspend fun getOscillatorHistory(
        dateRange: MarketDateRange = MarketDateRange.THREE_MONTHS
    ): Result<OscillatorHistory>

    /**
     * Get fund flow data (investor trading trends).
     * Uses KRX investor trading data.
     */
    suspend fun getFundFlowHistory(
        dateRange: MarketDateRange = MarketDateRange.THREE_MONTHS
    ): Result<FundFlowHistory>

    /**
     * Get Blood Indicator data.
     * Requires FRED API key for High Yield Spread.
     */
    suspend fun getBloodIndicatorHistory(
        dateRange: MarketDateRange = MarketDateRange.THREE_MONTHS
    ): Result<BloodIndicatorHistory>
}
