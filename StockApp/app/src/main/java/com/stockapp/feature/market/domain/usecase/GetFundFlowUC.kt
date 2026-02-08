package com.stockapp.feature.market.domain.usecase

import com.stockapp.feature.market.domain.model.FundFlowHistory
import com.stockapp.feature.market.domain.model.MarketDateRange
import com.stockapp.feature.market.domain.repo.MarketRepo
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Use case for fetching fund flow (investor trading) data.
 */
class GetFundFlowUC @Inject constructor(
    private val repo: MarketRepo
) {
    suspend operator fun invoke(
        dateRange: MarketDateRange = MarketDateRange.THREE_MONTHS
    ): Result<FundFlowHistory> {
        return try {
            repo.getFundFlowHistory(dateRange)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
