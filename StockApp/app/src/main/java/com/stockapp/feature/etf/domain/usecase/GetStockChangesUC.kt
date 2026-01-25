package com.stockapp.feature.etf.domain.usecase

import com.stockapp.core.db.dao.StockChangeInfo
import com.stockapp.feature.etf.domain.model.StockChangeType
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import javax.inject.Inject

/**
 * Use case for getting stock changes (newly included, removed, weight changes).
 */
class GetStockChangesUC @Inject constructor(
    private val repo: EtfCollectorRepo
) {

    /**
     * Get all stock changes for the latest date comparison.
     *
     * @param weightThreshold Minimum weight change threshold (default 0.1%)
     * @return Result containing all change types or error
     */
    suspend operator fun invoke(weightThreshold: Double = 0.1): Result<StockChangesResult> {
        return try {
            // Get latest date
            val latestDate = repo.getLatestCollectionDate()
                ?: return Result.failure(NoDataException("수집된 데이터가 없습니다"))

            // Get previous date
            val previousDate = repo.getPreviousCollectionDate(latestDate)
                ?: return Result.failure(NoDataException("비교할 이전 데이터가 없습니다"))

            // Get all changes
            val newlyIncluded = repo.getNewlyIncludedStocks(latestDate, previousDate)
            val removed = repo.getRemovedStocks(latestDate, previousDate)
            val weightIncreased = repo.getWeightIncreasedStocks(latestDate, previousDate, weightThreshold)
            val weightDecreased = repo.getWeightDecreasedStocks(latestDate, previousDate, weightThreshold)

            Result.success(
                StockChangesResult(
                    date = latestDate,
                    previousDate = previousDate,
                    newlyIncluded = newlyIncluded.toEnhanced(StockChangeType.NEWLY_INCLUDED),
                    removed = removed.toEnhanced(StockChangeType.REMOVED),
                    weightIncreased = weightIncreased.toEnhanced(StockChangeType.WEIGHT_INCREASED),
                    weightDecreased = weightDecreased.toEnhanced(StockChangeType.WEIGHT_DECREASED)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get newly included stocks.
     */
    suspend fun getNewlyIncluded(): Result<List<EnhancedStockChange>> {
        return getChangesByType(StockChangeType.NEWLY_INCLUDED)
    }

    /**
     * Get removed stocks.
     */
    suspend fun getRemoved(): Result<List<EnhancedStockChange>> {
        return getChangesByType(StockChangeType.REMOVED)
    }

    /**
     * Get weight increased stocks.
     */
    suspend fun getWeightIncreased(threshold: Double = 0.1): Result<List<EnhancedStockChange>> {
        return try {
            val latestDate = repo.getLatestCollectionDate()
                ?: return Result.failure(NoDataException("수집된 데이터가 없습니다"))
            val previousDate = repo.getPreviousCollectionDate(latestDate)
                ?: return Result.failure(NoDataException("비교할 이전 데이터가 없습니다"))

            val changes = repo.getWeightIncreasedStocks(latestDate, previousDate, threshold)
            Result.success(changes.toEnhanced(StockChangeType.WEIGHT_INCREASED))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get weight decreased stocks.
     */
    suspend fun getWeightDecreased(threshold: Double = 0.1): Result<List<EnhancedStockChange>> {
        return try {
            val latestDate = repo.getLatestCollectionDate()
                ?: return Result.failure(NoDataException("수집된 데이터가 없습니다"))
            val previousDate = repo.getPreviousCollectionDate(latestDate)
                ?: return Result.failure(NoDataException("비교할 이전 데이터가 없습니다"))

            val changes = repo.getWeightDecreasedStocks(latestDate, previousDate, threshold)
            Result.success(changes.toEnhanced(StockChangeType.WEIGHT_DECREASED))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getChangesByType(type: StockChangeType): Result<List<EnhancedStockChange>> {
        return try {
            val latestDate = repo.getLatestCollectionDate()
                ?: return Result.failure(NoDataException("수집된 데이터가 없습니다"))
            val previousDate = repo.getPreviousCollectionDate(latestDate)
                ?: return Result.failure(NoDataException("비교할 이전 데이터가 없습니다"))

            val changes = when (type) {
                StockChangeType.NEWLY_INCLUDED -> repo.getNewlyIncludedStocks(latestDate, previousDate)
                StockChangeType.REMOVED -> repo.getRemovedStocks(latestDate, previousDate)
                StockChangeType.WEIGHT_INCREASED -> repo.getWeightIncreasedStocks(latestDate, previousDate, 0.1)
                StockChangeType.WEIGHT_DECREASED -> repo.getWeightDecreasedStocks(latestDate, previousDate, 0.1)
            }
            Result.success(changes.toEnhanced(type))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun List<StockChangeInfo>.toEnhanced(type: StockChangeType): List<EnhancedStockChange> {
        return mapIndexed { index, info ->
            EnhancedStockChange(
                rank = index + 1,
                stockCode = info.stockCode,
                stockName = info.stockName,
                changeType = type,
                totalAmount = info.totalAmount,
                etfNames = info.etfNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            )
        }
    }
}

/**
 * Enhanced stock change with parsed ETF names.
 */
data class EnhancedStockChange(
    val rank: Int,
    val stockCode: String,
    val stockName: String,
    val changeType: StockChangeType,
    val totalAmount: Long,
    val etfNames: List<String>
)

/**
 * All stock changes result.
 */
data class StockChangesResult(
    val date: String,
    val previousDate: String,
    val newlyIncluded: List<EnhancedStockChange>,
    val removed: List<EnhancedStockChange>,
    val weightIncreased: List<EnhancedStockChange>,
    val weightDecreased: List<EnhancedStockChange>
) {
    val totalChanges: Int
        get() = newlyIncluded.size + removed.size + weightIncreased.size + weightDecreased.size
}
