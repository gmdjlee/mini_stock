package com.stockapp.feature.etf.domain.usecase

import com.stockapp.feature.etf.domain.model.StockAnalysisResult
import com.stockapp.feature.etf.domain.repo.EtfRepository
import javax.inject.Inject

/**
 * Use case for analyzing a specific stock's ETF holdings.
 * Returns detailed information about which ETFs hold the stock,
 * along with historical amount and weight data.
 */
class GetStockAnalysisUC @Inject constructor(
    private val repository: EtfRepository
) {

    /**
     * Get stock analysis for a specific stock code.
     *
     * @param stockCode Stock code to analyze (e.g., "005930")
     * @return Result containing stock analysis data or error
     */
    suspend operator fun invoke(stockCode: String): Result<StockAnalysisResult> {
        return try {
            if (stockCode.isBlank()) {
                return Result.failure(IllegalArgumentException("종목코드를 입력해주세요"))
            }

            // Normalize stock code (remove leading zeros if needed, or pad if needed)
            val normalizedCode = normalizeStockCode(stockCode)

            repository.getStockAnalysis(normalizedCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search and analyze a stock by name or code.
     * This is a convenience method that handles both code and name searches.
     *
     * @param query Stock name or code
     * @return Result containing stock analysis data or error
     */
    suspend fun search(query: String): Result<StockAnalysisResult> {
        return try {
            if (query.isBlank()) {
                return Result.failure(IllegalArgumentException("검색어를 입력해주세요"))
            }

            // If query looks like a stock code (all digits), use it directly
            val stockCode = if (query.all { it.isDigit() }) {
                normalizeStockCode(query)
            } else {
                // For name-based search, we need to find the stock code first
                // This would require a search in the constituents table
                // For now, assume the query is a code
                query
            }

            repository.getStockAnalysis(stockCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Normalize stock code to standard format (6 digits with leading zeros).
     */
    private fun normalizeStockCode(code: String): String {
        val cleaned = code.trim().filter { it.isDigit() }
        return cleaned.padStart(6, '0')
    }
}
