package com.stockapp.feature.etf.domain.repo

import com.stockapp.core.db.dao.DateRange
import com.stockapp.core.db.dao.StockAmountRanking
import com.stockapp.core.db.dao.StockChangeInfo
import com.stockapp.core.db.entity.EtfConstituentEntity
import com.stockapp.core.db.entity.EtfEntity
import com.stockapp.feature.etf.domain.model.EtfCollectionResult
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.model.EtfInfo
import com.stockapp.feature.etf.domain.model.FullCollectionResult
import com.stockapp.feature.etf.domain.model.MissingDatesResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ETF data collection and retrieval.
 */
interface EtfCollectorRepo {

    // ============================================================
    // ETF List Operations
    // ============================================================

    /**
     * Fetch all ETFs from Kiwoom API.
     */
    suspend fun fetchEtfList(): Result<List<EtfInfo>>

    /**
     * Get filtered ETFs from local database.
     */
    suspend fun getFilteredEtfs(): List<EtfEntity>

    /**
     * Get all ETFs from local database.
     */
    suspend fun getAllEtfs(): List<EtfEntity>

    /**
     * Update ETF filter status.
     */
    suspend fun updateEtfFilterStatus(etfCode: String, isFiltered: Boolean)

    /**
     * Apply keyword filter to ETFs and update database.
     */
    suspend fun applyKeywordFilter(config: EtfFilterConfig): Int

    /**
     * Save ETFs to database.
     */
    suspend fun saveEtfs(etfs: List<EtfEntity>)

    // ============================================================
    // Constituent Collection Operations
    // ============================================================

    /**
     * Collect constituent data for a single ETF.
     * @param etfCode ETF code
     * @param etfName ETF name
     * @return Collection result with constituent data
     */
    suspend fun collectEtfConstituents(etfCode: String, etfName: String): Result<EtfCollectionResult>

    /**
     * Collect all filtered ETF data.
     * @param progressCallback Optional callback for progress updates (current, total)
     * @return Full collection result
     */
    suspend fun collectAllFilteredEtfs(
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): FullCollectionResult

    /**
     * Save constituents to database.
     */
    suspend fun saveConstituents(constituents: List<EtfConstituentEntity>)

    // ============================================================
    // Statistics Queries
    // ============================================================

    /**
     * Get stock ranking by total evaluation amount.
     * @param date Target date (YYYY-MM-DD)
     * @param limit Maximum number of results
     */
    suspend fun getStockRanking(date: String, limit: Int = 100): List<StockAmountRanking>

    /**
     * Get newly included stocks compared to previous day.
     * @param today Today's date (YYYY-MM-DD)
     * @param yesterday Previous day's date (YYYY-MM-DD)
     */
    suspend fun getNewlyIncludedStocks(today: String, yesterday: String): List<StockChangeInfo>

    /**
     * Get removed stocks compared to previous day.
     */
    suspend fun getRemovedStocks(today: String, yesterday: String): List<StockChangeInfo>

    /**
     * Get stocks with weight increased.
     * @param threshold Minimum weight change threshold
     */
    suspend fun getWeightIncreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double = 0.1
    ): List<StockChangeInfo>

    /**
     * Get stocks with weight decreased.
     */
    suspend fun getWeightDecreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double = 0.1
    ): List<StockChangeInfo>

    // ============================================================
    // Data Management
    // ============================================================

    /**
     * Get available data date range.
     */
    suspend fun getDataDateRange(): DateRange?

    /**
     * Get the latest collection date.
     */
    suspend fun getLatestCollectionDate(): String?

    /**
     * Get the previous collection date before the given date.
     */
    suspend fun getPreviousCollectionDate(date: String): String?

    /**
     * Get all collected dates in ascending order.
     * @return List of dates in YYYY-MM-DD format
     */
    suspend fun getCollectedDates(): List<String>

    /**
     * Find missing trading days within the collected data range.
     * Compares collected dates against expected trading days (excluding weekends and holidays).
     * @return Analysis result with missing dates and coverage statistics
     */
    suspend fun findMissingCollectionDates(): MissingDatesResult

    /**
     * Delete old data beyond retention period.
     * @param cutoffDate Delete data before this date
     */
    suspend fun deleteOldData(cutoffDate: String)

    /**
     * Observe collection history.
     */
    fun observeCollectionHistory(limit: Int = 10): Flow<List<com.stockapp.core.db.entity.EtfCollectionHistoryEntity>>
}
