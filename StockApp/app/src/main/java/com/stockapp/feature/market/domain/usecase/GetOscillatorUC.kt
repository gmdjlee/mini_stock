package com.stockapp.feature.market.domain.usecase

import com.stockapp.feature.market.domain.model.MarketDateRange
import com.stockapp.feature.market.domain.model.OscillatorHistory
import com.stockapp.feature.market.domain.repo.MarketRepo
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Use case for fetching market oscillator (advance/decline) data.
 */
class GetOscillatorUC @Inject constructor(
    private val repo: MarketRepo
) {
    suspend operator fun invoke(
        dateRange: MarketDateRange = MarketDateRange.THREE_MONTHS
    ): Result<OscillatorHistory> {
        return try {
            repo.getOscillatorHistory(dateRange)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
