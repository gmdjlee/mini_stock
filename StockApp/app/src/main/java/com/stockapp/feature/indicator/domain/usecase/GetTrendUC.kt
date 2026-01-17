package com.stockapp.feature.indicator.domain.usecase

import com.stockapp.feature.indicator.domain.model.TrendSignal
import com.stockapp.feature.indicator.domain.repo.IndicatorRepo
import javax.inject.Inject

/**
 * Use case for getting Trend Signal indicator.
 */
class GetTrendUC @Inject constructor(
    private val repo: IndicatorRepo
) {
    suspend operator fun invoke(
        ticker: String,
        days: Int = 180,
        timeframe: String = "daily",
        useCache: Boolean = true
    ): Result<TrendSignal> {
        if (ticker.isBlank()) {
            return Result.failure(IllegalArgumentException("종목코드가 필요합니다"))
        }
        return repo.getTrend(ticker, days, timeframe, useCache)
    }
}
