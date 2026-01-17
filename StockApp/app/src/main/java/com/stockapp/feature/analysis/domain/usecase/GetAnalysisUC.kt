package com.stockapp.feature.analysis.domain.usecase

import com.stockapp.feature.analysis.domain.model.AnalysisSummary
import com.stockapp.feature.analysis.domain.model.StockData
import com.stockapp.feature.analysis.domain.model.toSummary
import com.stockapp.feature.analysis.domain.repo.AnalysisRepo
import javax.inject.Inject

/**
 * Get stock analysis use case.
 */
class GetAnalysisUC @Inject constructor(
    private val repo: AnalysisRepo
) {
    /**
     * Get analysis data for a stock.
     *
     * @param ticker Stock ticker code
     * @param days Number of days (default 180)
     * @param useCache Whether to use cached data
     * @return Result containing StockData
     */
    suspend operator fun invoke(
        ticker: String,
        days: Int = DEFAULT_DAYS,
        useCache: Boolean = true
    ): Result<StockData> {
        if (ticker.isBlank()) {
            return Result.failure(IllegalArgumentException("종목코드가 필요합니다"))
        }

        return repo.getAnalysis(
            ticker = ticker.trim(),
            days = days,
            useCache = useCache
        )
    }

    companion object {
        const val DEFAULT_DAYS = 180
    }
}

/**
 * Get analysis summary use case.
 * Returns processed summary for UI display.
 */
class GetAnalysisSummaryUC @Inject constructor(
    private val getAnalysisUC: GetAnalysisUC
) {
    suspend operator fun invoke(
        ticker: String,
        days: Int = GetAnalysisUC.DEFAULT_DAYS,
        useCache: Boolean = true
    ): Result<AnalysisSummary> {
        return getAnalysisUC(ticker, days, useCache).map { it.toSummary() }
    }
}

/**
 * Refresh analysis use case.
 * Forces fresh data fetch.
 */
class RefreshAnalysisUC @Inject constructor(
    private val repo: AnalysisRepo
) {
    suspend operator fun invoke(ticker: String): Result<StockData> {
        if (ticker.isBlank()) {
            return Result.failure(IllegalArgumentException("종목코드가 필요합니다"))
        }

        repo.clearCache(ticker.trim())
        return repo.getAnalysis(ticker.trim(), useCache = false)
    }
}
