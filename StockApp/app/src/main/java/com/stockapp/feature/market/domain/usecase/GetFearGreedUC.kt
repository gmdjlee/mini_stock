package com.stockapp.feature.market.domain.usecase

import com.stockapp.feature.market.domain.model.FearGreedHistory
import com.stockapp.feature.market.domain.model.MarketDateRange
import com.stockapp.feature.market.domain.model.MarketFearGreed
import com.stockapp.feature.market.domain.repo.MarketRepo
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Use case for fetching market-level Fear & Greed index.
 */
class GetFearGreedUC @Inject constructor(
    private val repo: MarketRepo
) {
    suspend fun getLatest(): Result<MarketFearGreed> {
        return try {
            repo.getFearGreedIndex()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHistory(
        dateRange: MarketDateRange = MarketDateRange.THREE_MONTHS
    ): Result<FearGreedHistory> {
        return try {
            repo.getFearGreedHistory(dateRange)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
