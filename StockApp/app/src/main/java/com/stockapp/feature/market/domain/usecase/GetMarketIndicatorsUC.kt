package com.stockapp.feature.market.domain.usecase

import com.stockapp.feature.market.domain.model.MarketIndicators
import com.stockapp.feature.market.domain.repo.MarketRepo
import javax.inject.Inject

/**
 * Use case for getting market indicators.
 */
class GetMarketIndicatorsUC @Inject constructor(
    private val repo: MarketRepo
) {
    /**
     * Get market indicators.
     *
     * @param days Number of days to retrieve (default: 30)
     * @param useCache Whether to use cached data (default: true)
     */
    suspend operator fun invoke(
        days: Int = DEFAULT_DAYS,
        useCache: Boolean = true
    ): Result<MarketIndicators> {
        if (days <= 0) {
            return Result.failure(IllegalArgumentException("조회 기간은 1일 이상이어야 합니다"))
        }
        return repo.getMarketIndicators(days, useCache)
    }

    companion object {
        const val DEFAULT_DAYS = 30
    }
}
