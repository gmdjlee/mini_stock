package com.stockapp.feature.financial.domain.usecase

import com.stockapp.feature.financial.domain.model.FinancialSummary
import com.stockapp.feature.financial.domain.model.toSummary
import com.stockapp.feature.financial.domain.repo.FinancialRepo
import javax.inject.Inject

/**
 * Use case for getting financial summary data for UI display.
 */
class GetFinancialSummaryUC @Inject constructor(
    private val repo: FinancialRepo
) {
    /**
     * Get financial summary for a stock.
     *
     * @param ticker Stock ticker code (6 digits)
     * @param name Stock name
     * @param useCache Whether to use cached data
     * @return Result containing FinancialSummary or error
     */
    suspend operator fun invoke(
        ticker: String,
        name: String,
        useCache: Boolean = true
    ): Result<FinancialSummary> {
        return repo.getFinancialData(ticker, name, useCache).map { it.toSummary() }
    }

    /**
     * Refresh financial summary from API.
     *
     * @param ticker Stock ticker code (6 digits)
     * @param name Stock name
     * @return Result containing FinancialSummary or error
     */
    suspend fun refresh(
        ticker: String,
        name: String
    ): Result<FinancialSummary> {
        return repo.refreshFinancialData(ticker, name).map { it.toSummary() }
    }
}
