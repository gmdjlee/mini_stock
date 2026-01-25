package com.stockapp.feature.etf.domain.repo

import com.stockapp.feature.etf.domain.model.AmountHistory
import com.stockapp.feature.etf.domain.model.CashDepositTrend
import com.stockapp.feature.etf.domain.model.CollectionHistory
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.ComparisonResult
import com.stockapp.feature.etf.domain.model.DailyEtfStatistics
import com.stockapp.feature.etf.domain.model.EtfCashDetail
import com.stockapp.feature.etf.domain.model.EtfConstituent
import com.stockapp.feature.etf.domain.model.EtfDateRange
import com.stockapp.feature.etf.domain.model.EtfInfo
import com.stockapp.feature.etf.domain.model.EtfKeyword
import com.stockapp.feature.etf.domain.model.FilterType
import com.stockapp.feature.etf.domain.model.StockAnalysisResult
import com.stockapp.feature.etf.domain.model.StockChange
import com.stockapp.feature.etf.domain.model.StockRanking
import com.stockapp.feature.etf.domain.model.WeightHistory
import kotlinx.coroutines.flow.Flow

/**
 * ETF Repository interface for managing ETF data, constituents, keywords, and statistics.
 */
interface EtfRepository {

    // ==================== ETF Info ====================

    /**
     * Get all ETFs.
     */
    suspend fun getAllEtfs(): Result<List<EtfInfo>>

    /**
     * Observe all ETFs as a Flow.
     */
    fun observeAllEtfs(): Flow<List<EtfInfo>>

    /**
     * Get filtered ETFs (marked for collection).
     */
    suspend fun getFilteredEtfs(): Result<List<EtfInfo>>

    /**
     * Observe filtered ETFs as a Flow.
     */
    fun observeFilteredEtfs(): Flow<List<EtfInfo>>

    /**
     * Get ETF by code.
     */
    suspend fun getEtfByCode(etfCode: String): Result<EtfInfo?>

    /**
     * Save ETFs (insert or update).
     */
    suspend fun saveEtfs(etfs: List<EtfInfo>): Result<Unit>

    /**
     * Update ETF filter status.
     */
    suspend fun updateEtfFilterStatus(etfCode: String, isFiltered: Boolean): Result<Unit>

    /**
     * Delete all ETFs.
     */
    suspend fun deleteAllEtfs(): Result<Unit>

    // ==================== ETF Constituents ====================

    /**
     * Get constituents for a specific date.
     */
    suspend fun getConstituentsByDate(date: String): Result<List<EtfConstituent>>

    /**
     * Get constituents for a specific ETF and date.
     */
    suspend fun getConstituentsByEtfAndDate(
        etfCode: String,
        date: String
    ): Result<List<EtfConstituent>>

    /**
     * Save constituents (insert or update).
     */
    suspend fun saveConstituents(constituents: List<EtfConstituent>): Result<Unit>

    /**
     * Get available collection dates.
     */
    suspend fun getCollectionDates(): Result<List<String>>

    /**
     * Get the latest collection date.
     */
    suspend fun getLatestDate(): Result<String?>

    /**
     * Get the previous collection date before the given date.
     */
    suspend fun getPreviousDate(date: String): Result<String?>

    /**
     * Get the data date range.
     */
    suspend fun getDataDateRange(): Result<EtfDateRange>

    /**
     * Delete old constituent data before cutoff date.
     */
    suspend fun deleteOldConstituents(cutoffDate: String): Result<Unit>

    /**
     * Delete all constituents.
     */
    suspend fun deleteAllConstituents(): Result<Unit>

    // ==================== ETF Keywords ====================

    /**
     * Get all keywords.
     */
    suspend fun getAllKeywords(): Result<List<EtfKeyword>>

    /**
     * Observe all keywords as a Flow.
     */
    fun observeAllKeywords(): Flow<List<EtfKeyword>>

    /**
     * Get enabled keywords.
     */
    suspend fun getEnabledKeywords(): Result<List<EtfKeyword>>

    /**
     * Observe enabled keywords as a Flow.
     */
    fun observeEnabledKeywords(): Flow<List<EtfKeyword>>

    /**
     * Get keywords by filter type.
     */
    suspend fun getKeywordsByType(filterType: FilterType): Result<List<EtfKeyword>>

    /**
     * Observe keywords by filter type as a Flow.
     */
    fun observeKeywordsByType(filterType: FilterType): Flow<List<EtfKeyword>>

    /**
     * Add a new keyword.
     */
    suspend fun addKeyword(keyword: String, filterType: FilterType): Result<Long>

    /**
     * Delete a keyword by ID.
     */
    suspend fun deleteKeyword(id: Long): Result<Unit>

    /**
     * Update keyword enabled status.
     */
    suspend fun updateKeywordEnabled(id: Long, enabled: Boolean): Result<Unit>

    /**
     * Check if a keyword exists.
     */
    suspend fun keywordExists(keyword: String, filterType: FilterType): Result<Boolean>

    /**
     * Delete all keywords.
     */
    suspend fun deleteAllKeywords(): Result<Unit>

    /**
     * Initialize default keywords if no keywords exist.
     * Inserts default include and exclude keywords from EtfFilterConfig.
     * @return true if keywords were initialized, false if keywords already exist.
     */
    suspend fun initializeDefaultKeywords(): Result<Boolean>

    // ==================== Collection History ====================

    /**
     * Get recent collection history.
     */
    suspend fun getRecentHistory(limit: Int = 10): Result<List<CollectionHistory>>

    /**
     * Observe recent collection history as a Flow.
     */
    fun observeRecentHistory(limit: Int = 10): Flow<List<CollectionHistory>>

    /**
     * Get the latest collection history.
     */
    suspend fun getLatestHistory(): Result<CollectionHistory?>

    /**
     * Observe the latest collection history as a Flow.
     */
    fun observeLatestHistory(): Flow<CollectionHistory?>

    /**
     * Start a new collection (create IN_PROGRESS record).
     */
    suspend fun startCollection(collectedDate: String): Result<Long>

    /**
     * Complete a collection (update status and counts).
     */
    suspend fun completeCollection(
        id: Long,
        status: CollectionStatus,
        totalEtfs: Int,
        totalConstituents: Int,
        errorMessage: String? = null
    ): Result<Unit>

    /**
     * Trim history to keep only recent entries.
     */
    suspend fun trimHistory(keepCount: Int = 30): Result<Unit>

    // ==================== Statistics Queries ====================

    /**
     * Get stock ranking by total evaluation amount.
     */
    suspend fun getStockRanking(date: String, limit: Int = 100): Result<List<StockRanking>>

    /**
     * Get newly included stocks (present today but not yesterday).
     */
    suspend fun getNewlyIncludedStocks(
        today: String,
        yesterday: String
    ): Result<List<StockChange>>

    /**
     * Get removed stocks (present yesterday but not today).
     */
    suspend fun getRemovedStocks(
        today: String,
        yesterday: String
    ): Result<List<StockChange>>

    /**
     * Get stocks with weight increased above threshold.
     */
    suspend fun getWeightIncreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double = 0.1
    ): Result<List<StockChange>>

    /**
     * Get stocks with weight decreased below threshold.
     */
    suspend fun getWeightDecreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double = 0.1
    ): Result<List<StockChange>>

    // ==================== Chart Data ====================

    /**
     * Get stock amount history for chart visualization.
     */
    suspend fun getStockAmountHistory(stockCode: String): Result<List<AmountHistory>>

    /**
     * Get stock average weight history for chart visualization.
     */
    suspend fun getStockWeightHistory(stockCode: String): Result<List<WeightHistory>>

    // ==================== ETF Statistics (Phase 2) ====================

    /**
     * Get comparison results within date range.
     */
    suspend fun getComparisonInRange(
        startDate: String,
        endDate: String
    ): Result<ComparisonResult>

    /**
     * Get cash deposit trend within date range.
     */
    suspend fun getCashDepositTrend(
        startDate: String,
        endDate: String
    ): Result<List<CashDepositTrend>>

    /**
     * Get ETF cash details for a specific date.
     */
    suspend fun getEtfCashDetails(date: String): Result<List<EtfCashDetail>>

    /**
     * Get stock analysis result for a specific stock.
     */
    suspend fun getStockAnalysis(stockCode: String): Result<StockAnalysisResult>

    /**
     * Calculate and save daily statistics for a specific date.
     */
    suspend fun calculateDailyStatistics(date: String): Result<Unit>

    /**
     * Get daily statistics for a specific date.
     */
    suspend fun getDailyStatistics(date: String): Result<DailyEtfStatistics?>

    /**
     * Get daily statistics within date range.
     */
    suspend fun getDailyStatisticsInRange(
        startDate: String,
        endDate: String
    ): Result<List<DailyEtfStatistics>>

    // ==================== Theme List (Phase 3) ====================

    /**
     * Get active ETFs (ETFs with constituent data) for the latest date.
     */
    suspend fun getActiveEtfSummaries(): Result<List<com.stockapp.feature.etf.domain.model.ActiveEtfSummary>>

    /**
     * Search active ETFs by name.
     */
    suspend fun searchActiveEtfs(query: String): Result<List<com.stockapp.feature.etf.domain.model.ActiveEtfSummary>>

    /**
     * Get ETF detail with constituents.
     */
    suspend fun getEtfDetail(etfCode: String): Result<com.stockapp.feature.etf.domain.model.EtfDetailInfo>
}
