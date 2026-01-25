package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.EtfConstituentEntity

/**
 * Query result model for stock amount ranking.
 */
data class StockAmountRanking(
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfCount: Int
)

/**
 * Query result model for stock change info (newly included, removed, weight changed).
 */
data class StockChangeInfo(
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfNames: String  // comma-separated ETF names
)

/**
 * Query result model for date-amount pair (chart data).
 */
data class DateAmount(
    val collectedDate: String,
    val totalAmount: Long
)

/**
 * Query result model for date-weight pair (chart data).
 */
data class DateWeight(
    val collectedDate: String,
    val avgWeight: Double
)

/**
 * Query result model for date range.
 */
data class DateRange(
    val startDate: String?,
    val endDate: String?
)

@Dao
interface EtfConstituentDao {
    /**
     * Get all constituents for a specific date ordered by evaluation amount.
     */
    @Query("""
        SELECT * FROM etf_constituents
        WHERE collectedDate = :date
        ORDER BY evaluationAmount DESC
    """)
    suspend fun getByDate(date: String): List<EtfConstituentEntity>

    /**
     * Get constituents for a specific ETF and date.
     */
    @Query("""
        SELECT * FROM etf_constituents
        WHERE etfCode = :etfCode AND collectedDate = :date
        ORDER BY weight DESC
    """)
    suspend fun getByEtfAndDate(etfCode: String, date: String): List<EtfConstituentEntity>

    /**
     * Get all constituents for a specific stock across all ETFs and dates.
     */
    @Query("""
        SELECT * FROM etf_constituents
        WHERE stockCode = :stockCode
        ORDER BY collectedDate DESC, evaluationAmount DESC
    """)
    suspend fun getByStockCode(stockCode: String): List<EtfConstituentEntity>

    /**
     * Get stock amount ranking aggregated across all ETFs for a specific date.
     */
    @Query("""
        SELECT stockCode, stockName,
               SUM(evaluationAmount) as totalAmount,
               COUNT(DISTINCT etfCode) as etfCount
        FROM etf_constituents
        WHERE collectedDate = :date
        GROUP BY stockCode
        ORDER BY totalAmount DESC
        LIMIT :limit
    """)
    suspend fun getStockRankingByAmount(date: String, limit: Int = 100): List<StockAmountRanking>

    /**
     * Get newly included stocks (present today but not yesterday for the same ETF).
     */
    @Query("""
        SELECT t.stockCode, t.stockName,
               SUM(t.evaluationAmount) as totalAmount,
               GROUP_CONCAT(DISTINCT t.etfName) as etfNames
        FROM etf_constituents t
        WHERE t.collectedDate = :today
          AND NOT EXISTS (
              SELECT 1 FROM etf_constituents p
              WHERE p.stockCode = t.stockCode
                AND p.etfCode = t.etfCode
                AND p.collectedDate = :yesterday
          )
        GROUP BY t.stockCode
        ORDER BY totalAmount DESC
    """)
    suspend fun getNewlyIncludedStocks(today: String, yesterday: String): List<StockChangeInfo>

    /**
     * Get removed stocks (present yesterday but not today for the same ETF).
     */
    @Query("""
        SELECT y.stockCode, y.stockName,
               SUM(y.evaluationAmount) as totalAmount,
               GROUP_CONCAT(DISTINCT y.etfName) as etfNames
        FROM etf_constituents y
        WHERE y.collectedDate = :yesterday
          AND NOT EXISTS (
              SELECT 1 FROM etf_constituents t
              WHERE t.stockCode = y.stockCode
                AND t.etfCode = y.etfCode
                AND t.collectedDate = :today
          )
        GROUP BY y.stockCode
        ORDER BY totalAmount DESC
    """)
    suspend fun getRemovedStocks(today: String, yesterday: String): List<StockChangeInfo>

    /**
     * Get stocks with weight increased above threshold.
     */
    @Query("""
        SELECT t.stockCode, t.stockName,
               SUM(t.evaluationAmount) as totalAmount,
               GROUP_CONCAT(DISTINCT t.etfName) as etfNames
        FROM etf_constituents t
        JOIN etf_constituents y ON t.stockCode = y.stockCode
                                AND t.etfCode = y.etfCode
                                AND y.collectedDate = :yesterday
        WHERE t.collectedDate = :today
          AND t.weight > y.weight + :threshold
        GROUP BY t.stockCode
        ORDER BY totalAmount DESC
    """)
    suspend fun getWeightIncreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double = 0.1
    ): List<StockChangeInfo>

    /**
     * Get stocks with weight decreased below threshold.
     */
    @Query("""
        SELECT t.stockCode, t.stockName,
               SUM(t.evaluationAmount) as totalAmount,
               GROUP_CONCAT(DISTINCT t.etfName) as etfNames
        FROM etf_constituents t
        JOIN etf_constituents y ON t.stockCode = y.stockCode
                                AND t.etfCode = y.etfCode
                                AND y.collectedDate = :yesterday
        WHERE t.collectedDate = :today
          AND t.weight < y.weight - :threshold
        GROUP BY t.stockCode
        ORDER BY totalAmount DESC
    """)
    suspend fun getWeightDecreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double = 0.1
    ): List<StockChangeInfo>

    /**
     * Get stock amount history for chart visualization.
     */
    @Query("""
        SELECT collectedDate, SUM(evaluationAmount) as totalAmount
        FROM etf_constituents
        WHERE stockCode = :stockCode
        GROUP BY collectedDate
        ORDER BY collectedDate
    """)
    suspend fun getStockAmountHistory(stockCode: String): List<DateAmount>

    /**
     * Get stock average weight history for chart visualization.
     */
    @Query("""
        SELECT collectedDate, AVG(weight) as avgWeight
        FROM etf_constituents
        WHERE stockCode = :stockCode
        GROUP BY collectedDate
        ORDER BY collectedDate
    """)
    suspend fun getStockWeightHistory(stockCode: String): List<DateWeight>

    /**
     * Get the date range of available data.
     */
    @Query("""
        SELECT MIN(collectedDate) as startDate, MAX(collectedDate) as endDate
        FROM etf_constituents
    """)
    suspend fun getDataDateRange(): DateRange?

    /**
     * Get distinct collection dates ordered descending.
     */
    @Query("SELECT DISTINCT collectedDate FROM etf_constituents ORDER BY collectedDate DESC")
    suspend fun getCollectionDates(): List<String>

    /**
     * Get the latest collection date.
     */
    @Query("SELECT MAX(collectedDate) FROM etf_constituents")
    suspend fun getLatestDate(): String?

    /**
     * Get the previous collection date before the given date.
     */
    @Query("""
        SELECT MAX(collectedDate) FROM etf_constituents
        WHERE collectedDate < :date
    """)
    suspend fun getPreviousDate(date: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(constituent: EtfConstituentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(constituents: List<EtfConstituentEntity>)

    /**
     * Delete old data beyond retention period.
     */
    @Query("DELETE FROM etf_constituents WHERE collectedDate < :cutoffDate")
    suspend fun deleteOldData(cutoffDate: String)

    /**
     * Delete all constituents for a specific date.
     */
    @Query("DELETE FROM etf_constituents WHERE collectedDate = :date")
    suspend fun deleteByDate(date: String)

    /**
     * Delete all constituents for a specific ETF.
     */
    @Query("DELETE FROM etf_constituents WHERE etfCode = :etfCode")
    suspend fun deleteByEtf(etfCode: String)

    @Query("DELETE FROM etf_constituents")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM etf_constituents")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM etf_constituents WHERE collectedDate = :date")
    suspend fun countByDate(date: String): Int

    @Query("SELECT COUNT(DISTINCT stockCode) FROM etf_constituents WHERE collectedDate = :date")
    suspend fun countDistinctStocksByDate(date: String): Int
}
