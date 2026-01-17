package com.stockapp.feature.indicator.domain.usecase

import com.stockapp.feature.indicator.domain.model.ElderImpulse
import com.stockapp.feature.indicator.domain.repo.IndicatorRepo
import javax.inject.Inject

/**
 * Use case for getting Elder Impulse indicator.
 */
class GetElderUC @Inject constructor(
    private val repo: IndicatorRepo
) {
    suspend operator fun invoke(
        ticker: String,
        days: Int = 180,
        timeframe: String = "daily",
        useCache: Boolean = true
    ): Result<ElderImpulse> {
        if (ticker.isBlank()) {
            return Result.failure(IllegalArgumentException("종목코드가 필요합니다"))
        }
        return repo.getElder(ticker, days, timeframe, useCache)
    }
}
