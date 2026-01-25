package com.stockapp.feature.etf.domain.usecase

import com.stockapp.core.db.dao.StockAmountRanking
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for getting stock ranking by evaluation amount.
 */
class GetStockRankingUC @Inject constructor(
    private val repo: EtfCollectorRepo
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Get stock ranking for the latest collection date.
     *
     * @param limit Maximum number of results (default 100)
     * @return Result containing ranking list or error
     */
    suspend operator fun invoke(limit: Int = 100): Result<StockRankingResult> {
        return try {
            // Get latest date
            val latestDate = repo.getLatestCollectionDate()
                ?: return Result.failure(NoDataException("수집된 데이터가 없습니다"))

            // Get ranking for that date
            val ranking = repo.getStockRanking(latestDate, limit)

            // Get previous date for comparison
            val previousDate = repo.getPreviousCollectionDate(latestDate)
            val previousRanking = previousDate?.let {
                repo.getStockRanking(it, limit * 2) // Get more for comparison
            } ?: emptyList()

            // Create lookup map for previous day amounts
            val previousAmountMap = previousRanking.associateBy { it.stockCode }

            // Enhance ranking with change data
            val enhancedRanking = ranking.mapIndexed { index, item ->
                val prevItem = previousAmountMap[item.stockCode]
                EnhancedStockRanking(
                    rank = index + 1,
                    stockCode = item.stockCode,
                    stockName = item.stockName,
                    totalAmount = item.totalAmount,
                    etfCount = item.etfCount,
                    previousAmount = prevItem?.totalAmount,
                    amountChange = prevItem?.let { item.totalAmount - it.totalAmount },
                    isNew = prevItem == null
                )
            }

            Result.success(
                StockRankingResult(
                    date = latestDate,
                    previousDate = previousDate,
                    rankings = enhancedRanking
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get stock ranking for a specific date.
     *
     * @param date Target date (YYYY-MM-DD)
     * @param limit Maximum number of results
     */
    suspend fun forDate(date: String, limit: Int = 100): Result<List<StockAmountRanking>> {
        return try {
            val ranking = repo.getStockRanking(date, limit)
            Result.success(ranking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Enhanced stock ranking with comparison data.
 */
data class EnhancedStockRanking(
    val rank: Int,
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfCount: Int,
    val previousAmount: Long?,
    val amountChange: Long?,
    val isNew: Boolean
)

/**
 * Stock ranking result with date info.
 */
data class StockRankingResult(
    val date: String,
    val previousDate: String?,
    val rankings: List<EnhancedStockRanking>
)

/**
 * Exception when no data is available.
 */
class NoDataException(message: String) : Exception(message)
