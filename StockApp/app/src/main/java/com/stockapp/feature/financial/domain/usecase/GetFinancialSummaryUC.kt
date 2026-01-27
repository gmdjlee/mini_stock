package com.stockapp.feature.financial.domain.usecase

import android.util.Log
import com.stockapp.feature.financial.domain.model.FinancialData
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
        return repo.getFinancialData(ticker, name, useCache).map { data ->
            logFinancialData(data)
            val summary = data.toSummary()
            logFinancialSummary(summary)
            summary
        }
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
        return repo.refreshFinancialData(ticker, name).map { data ->
            logFinancialData(data)
            val summary = data.toSummary()
            logFinancialSummary(summary)
            summary
        }
    }

    private fun logFinancialData(data: FinancialData) {
        Log.d(TAG, "=== FinancialData for ${data.ticker} ===")
        Log.d(TAG, "Periods: ${data.periods}")
        Log.d(TAG, "IncomeStatements count: ${data.incomeStatements.size}, keys: ${data.incomeStatements.keys}")
        Log.d(TAG, "GrowthRatios count: ${data.growthRatios.size}, keys: ${data.growthRatios.keys}")

        // Log sample income statement data
        data.incomeStatements.entries.firstOrNull()?.let { (period, is_) ->
            Log.d(TAG, "Sample IncomeStatement[$period]: revenue=${is_.revenue}, opProfit=${is_.operatingProfit}, netIncome=${is_.netIncome}")
        }

        // Log sample growth data
        data.growthRatios.entries.firstOrNull()?.let { (period, gr) ->
            Log.d(TAG, "Sample GrowthRatios[$period]: revenueGrowth=${gr.revenueGrowth}, equityGrowth=${gr.equityGrowth}, totalAssetsGrowth=${gr.totalAssetsGrowth}")
        }
    }

    private fun logFinancialSummary(summary: FinancialSummary) {
        Log.d(TAG, "=== FinancialSummary for ${summary.ticker} ===")
        Log.d(TAG, "hasProfitabilityData: ${summary.hasProfitabilityData}")
        Log.d(TAG, "hasGrowthData: ${summary.hasGrowthData}")
        Log.d(TAG, "hasAssetGrowthData: ${summary.hasAssetGrowthData}")
        Log.d(TAG, "revenues: ${summary.revenues}")
        Log.d(TAG, "revenueGrowthRates: ${summary.revenueGrowthRates}")
        Log.d(TAG, "equityGrowthRates: ${summary.equityGrowthRates}")
        Log.d(TAG, "totalAssetsGrowthRates: ${summary.totalAssetsGrowthRates}")
    }

    companion object {
        private const val TAG = "GetFinancialSummaryUC"
    }
}
