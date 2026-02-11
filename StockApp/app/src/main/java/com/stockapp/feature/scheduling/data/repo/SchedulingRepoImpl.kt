package com.stockapp.feature.scheduling.data.repo

import android.util.Log
import com.stockapp.core.db.cleanup.DbCleanupManager
import com.stockapp.core.db.dao.SchedulingConfigDao
import com.stockapp.core.db.dao.SearchHistoryDao
import com.stockapp.core.db.dao.StockAnalysisDataDao
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.dao.SyncHistoryDao
import com.stockapp.core.db.entity.SchedulingConfigEntity
import com.stockapp.core.db.entity.StockAnalysisDataEntity
import com.stockapp.core.db.entity.StockEntity
import com.stockapp.core.db.entity.SyncHistoryEntity
import com.stockapp.feature.analysis.domain.repo.AnalysisRepo
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.repo.EtfRepository
import com.stockapp.feature.etf.domain.usecase.CollectAllEtfDataUC
import com.stockapp.feature.market.domain.model.MarketDateRange
import com.stockapp.feature.market.domain.repo.MarketRepo
import com.stockapp.feature.scheduling.domain.model.SchedulingConfig
import com.stockapp.feature.scheduling.domain.model.SyncHistory
import com.stockapp.feature.scheduling.domain.model.SyncResult
import com.stockapp.feature.scheduling.domain.model.SyncStatus
import com.stockapp.feature.scheduling.domain.model.SyncType
import com.stockapp.feature.scheduling.domain.repo.SchedulingRepo
import com.stockapp.feature.search.domain.repo.SearchRepo
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SchedulingRepo"
private const val MAX_STOCKS_PER_BATCH = 10_000
private const val ANALYSIS_BATCH_SIZE = 50
private const val ETF_CLEANUP_DAYS = 30

@Singleton
class SchedulingRepoImpl @Inject constructor(
    private val configDao: SchedulingConfigDao,
    private val syncHistoryDao: SyncHistoryDao,
    private val stockDao: StockDao,
    private val analysisDataDao: StockAnalysisDataDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val searchRepo: SearchRepo,
    private val analysisRepo: AnalysisRepo,
    private val collectAllEtfDataUC: CollectAllEtfDataUC,
    private val etfRepository: EtfRepository,
    private val marketRepo: MarketRepo,
    private val dbCleanupManager: DbCleanupManager
) : SchedulingRepo {

    override fun observeConfig(): Flow<SchedulingConfig> {
        return configDao.getConfig().map { entity ->
            entity?.toDomain() ?: SchedulingConfig()
        }
    }

    override suspend fun getConfig(): SchedulingConfig {
        return configDao.getConfigOnce()?.toDomain() ?: run {
            // Initialize default config if not exists
            val defaultConfig = SchedulingConfigEntity()
            configDao.insertOrUpdate(defaultConfig)
            defaultConfig.toDomain()
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        ensureConfigExists()
        configDao.setEnabled(enabled)
    }

    override suspend fun setSyncTime(hour: Int, minute: Int) {
        ensureConfigExists()
        configDao.setSyncTime(hour, minute)
    }

    override suspend fun updateLastSync(syncedAt: Long, success: Boolean, message: String?) {
        ensureConfigExists()
        val status = if (success) SyncStatus.SUCCESS.name else SyncStatus.FAILED.name
        configDao.updateLastSync(syncedAt, status, message)
    }

    override fun observeSyncHistory(limit: Int): Flow<List<SyncHistory>> {
        return syncHistoryDao.getRecentHistory(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLatestSync(): SyncHistory? {
        return syncHistoryDao.getLatestSync()?.toDomain()
    }

    override suspend fun syncStockList(): Result<Int> = withContext(Dispatchers.IO) {
        Log.d(TAG, "syncStockList() started")

        try {
            val result = searchRepo.getAll()

            result.fold(
                onSuccess = { stocks ->
                    Log.d(TAG, "Fetched ${stocks.size} stocks")

                    val now = System.currentTimeMillis()
                    val stockEntities = stocks.map { stock ->
                        StockEntity(
                            ticker = stock.ticker,
                            name = stock.name,
                            market = stock.market.name,
                            updatedAt = now
                        )
                    }

                    // Limit stock count
                    val limitedStocks = if (stockEntities.size > MAX_STOCKS_PER_BATCH) {
                        stockEntities.sortedWith(
                            compareBy<StockEntity> {
                                when (it.market) {
                                    "KOSPI" -> 0
                                    "KOSDAQ" -> 1
                                    else -> 2
                                }
                            }.thenBy { it.name }
                        ).take(MAX_STOCKS_PER_BATCH)
                    } else {
                        stockEntities
                    }

                    // Smart sync: upsert active + remove delisted
                    stockDao.smartSync(limitedStocks)

                    val count = stockDao.count()
                    Log.d(TAG, "Stock list synced: $count stocks")
                    Result.success(count)
                },
                onFailure = { e ->
                    Log.e(TAG, "syncStockList failed: ${e.message}", e)
                    Result.failure(e)
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "syncStockList exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun syncAllData(syncType: SyncType): SyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "syncAllData() started, type=$syncType")
        val startTime = System.currentTimeMillis()

        // Create sync history entry
        val historyId = syncHistoryDao.insert(
            SyncHistoryEntity(
                syncType = syncType.name,
                status = SyncStatus.IN_PROGRESS.name,
                syncedAt = startTime
            )
        )

        try {
            // 1. Sync stock list
            val stockResult = syncStockList()
            if (stockResult.isFailure) {
                val error = stockResult.exceptionOrNull()?.message ?: "Stock list sync failed"
                finishSync(historyId, false, 0, 0, 0, 0, 0, error, startTime)
                return@withContext SyncResult(
                    success = false,
                    errorMessage = error,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            val stockCount = stockResult.getOrDefault(0)

            // 2. Sync analysis data for top stocks (by market cap or recent searches)
            val analysisResult = syncTopStocksAnalysis()
            val analysisCount = analysisResult.getOrDefault(0)

            // 3. Sync ETF data
            val (etfCount, etfConstituentCount) = syncEtfData()

            // 4. Pre-fetch market indicator data (Fear/Greed, Fund Flow)
            try {
                syncMarketData()
            } catch (e: Exception) {
                Log.w(TAG, "Market data pre-fetch failed (non-fatal): ${e.message}")
            }

            // 5. Run DB cleanup (expired caches, old OHLCV data)
            try {
                dbCleanupManager.runCleanup()
            } catch (e: Exception) {
                Log.w(TAG, "DB cleanup failed (non-fatal): ${e.message}")
            }

            val durationMs = System.currentTimeMillis() - startTime
            finishSync(historyId, true, stockCount, analysisCount, 0, etfCount, etfConstituentCount, null, startTime)

            // Update last sync status
            updateLastSync(System.currentTimeMillis(), true, null)

            SyncResult(
                success = true,
                stockCount = stockCount,
                analysisCount = analysisCount,
                etfCount = etfCount,
                etfConstituentCount = etfConstituentCount,
                durationMs = durationMs
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "syncAllData failed: ${e.message}", e)
            val durationMs = System.currentTimeMillis() - startTime
            finishSync(historyId, false, 0, 0, 0, 0, 0, e.message, startTime)
            updateLastSync(System.currentTimeMillis(), false, e.message)

            SyncResult(
                success = false,
                errorMessage = e.message,
                durationMs = durationMs
            )
        }
    }

    override suspend fun syncAnalysisData(tickers: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        Log.d(TAG, "syncAnalysisData() for ${tickers.size} tickers")

        var syncedCount = 0

        try {
            tickers.chunked(ANALYSIS_BATCH_SIZE).forEach { batch ->
                batch.forEach { ticker ->
                    try {
                        val result = analysisRepo.getAnalysis(ticker, 30, useCache = false)
                        if (result.isSuccess) {
                            val data = result.getOrNull()
                            if (data != null) {
                                val latestMcap = data.mcap.lastOrNull() ?: 0L
                                val latestFor5d = data.for5d.lastOrNull() ?: 0L
                                val latestIns5d = data.ins5d.lastOrNull() ?: 0L

                                val supplyRatio = if (latestMcap > 0) {
                                    ((latestFor5d + latestIns5d).toDouble() / latestMcap) * 100
                                } else 0.0

                                val signalType = when {
                                    supplyRatio > 0.5 -> "STRONG_BUY"
                                    supplyRatio > 0.2 -> "BUY"
                                    supplyRatio < -0.5 -> "STRONG_SELL"
                                    supplyRatio < -0.2 -> "SELL"
                                    else -> "NEUTRAL"
                                }

                                val market = stockDao.getByTicker(ticker)?.market ?: "UNKNOWN"
                                val entity = StockAnalysisDataEntity(
                                    ticker = ticker,
                                    name = data.name,
                                    market = market,
                                    marketCap = latestMcap,
                                    foreignNet5d = latestFor5d,
                                    institutionNet5d = latestIns5d,
                                    supplyRatio = supplyRatio,
                                    signalType = signalType,
                                    lastAnalyzedDate = data.dates.lastOrNull() ?: "",
                                    detailDataJson = "",
                                    updatedAt = System.currentTimeMillis()
                                )
                                analysisDataDao.insertOrUpdate(entity)
                                syncedCount++
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync analysis for $ticker: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "Analysis data synced: $syncedCount tickers")
            Result.success(syncedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "syncAnalysisData failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun hasNewDataAvailable(): Boolean {
        // Check if stock list cache is expired
        val lastStockUpdate = stockDao.lastUpdated() ?: 0L
        val stockCacheExpired = System.currentTimeMillis() - lastStockUpdate > 24 * 60 * 60 * 1000L
        return stockCacheExpired
    }

    override suspend fun deleteSyncHistory(id: Long) {
        syncHistoryDao.deleteById(id)
    }

    override suspend fun setErrorStopped(stopped: Boolean) {
        ensureConfigExists()
        configDao.setErrorStopped(stopped)
    }

    override suspend fun clearErrorAndResume() {
        ensureConfigExists()
        configDao.setErrorStopped(false)
    }

    private suspend fun syncTopStocksAnalysis(): Result<Int> {
        // Priority 1: Recently searched stocks (most likely to be viewed again)
        val recentSearchTickers = searchHistoryDao.getRecentList(30).map { it.ticker }

        // Priority 2: Stocks with existing analysis data (already tracked)
        val trackedTickers = analysisDataDao.getAllOnce()
            .sortedByDescending { it.updatedAt }
            .take(50)
            .map { it.ticker }

        // Combine, deduplicate, limit to 100
        val prioritizedTickers = (recentSearchTickers + trackedTickers)
            .distinct()
            .take(100)

        if (prioritizedTickers.isEmpty()) {
            // Fallback to first 100 stocks
            val stocks = stockDao.getAllOnce(100)
            if (stocks.isEmpty()) return Result.success(0)
            return syncAnalysisData(stocks.map { it.ticker })
        }

        return syncAnalysisData(prioritizedTickers)
    }

    /**
     * Pre-fetch market indicator data to populate cache.
     * Fear/Greed and Fund Flow are fast; Oscillator is skipped (expensive per-day calls).
     */
    private suspend fun syncMarketData() {
        Log.d(TAG, "syncMarketData() started")
        val dateRange = MarketDateRange.THREE_MONTHS

        // Fear & Greed index (latest)
        marketRepo.getFearGreedIndex().onFailure {
            Log.w(TAG, "syncMarketData() Fear/Greed index failed: ${it.message}")
        }

        // Fear & Greed history
        marketRepo.getFearGreedHistory(dateRange).onFailure {
            Log.w(TAG, "syncMarketData() Fear/Greed history failed: ${it.message}")
        }

        // Fund Flow history
        marketRepo.getFundFlowHistory(dateRange).onFailure {
            Log.w(TAG, "syncMarketData() Fund Flow failed: ${it.message}")
        }

        // Note: Oscillator is skipped due to high cost (20 individual KRX calls)
        Log.d(TAG, "syncMarketData() completed")
    }

    /**
     * Sync ETF data using the ETF filter configuration from the repository.
     * @return Pair of (etfCount, constituentCount)
     */
    private suspend fun syncEtfData(): Pair<Int, Int> {
        Log.d(TAG, "syncEtfData() started")

        // Build filter config from enabled keywords
        val keywords = etfRepository.getEnabledKeywords().getOrDefault(emptyList())
        val includeKeywords = keywords
            .filter { it.filterType.value == "INCLUDE" }
            .map { it.keyword }
        val excludeKeywords = keywords
            .filter { it.filterType.value == "EXCLUDE" }
            .map { it.keyword }

        // Use defaults if no keywords are set
        val filterConfig = EtfFilterConfig(
            activeOnly = true, // Default: active ETFs only
            includeKeywords = includeKeywords.ifEmpty { EtfFilterConfig.DEFAULT_INCLUDE_KEYWORDS },
            excludeKeywords = excludeKeywords.ifEmpty { EtfFilterConfig.DEFAULT_EXCLUDE_KEYWORDS }
        )

        Log.d(TAG, "ETF filter config: activeOnly=${filterConfig.activeOnly}, " +
                "include=${filterConfig.includeKeywords.size}, exclude=${filterConfig.excludeKeywords.size}")

        return try {
            val result = collectAllEtfDataUC(
                filterConfig = filterConfig,
                cleanupDays = ETF_CLEANUP_DAYS,
                progressCallback = { current, total ->
                    Log.d(TAG, "ETF collection progress: $current/$total")
                }
            )

            when (result.status) {
                CollectionStatus.SUCCESS, CollectionStatus.PARTIAL -> {
                    Log.d(TAG, "ETF collection completed: ${result.totalEtfs} ETFs, ${result.totalConstituents} constituents")
                    Pair(result.totalEtfs, result.totalConstituents)
                }
                else -> {
                    Log.w(TAG, "ETF collection failed: ${result.errorMessage}")
                    Pair(0, 0)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "syncEtfData exception: ${e.message}", e)
            Pair(0, 0)
        }
    }

    private suspend fun finishSync(
        historyId: Long,
        success: Boolean,
        stockCount: Int,
        analysisCount: Int,
        indicatorCount: Int,
        etfCount: Int,
        etfConstituentCount: Int,
        errorMessage: String?,
        startTime: Long
    ) {
        val durationMs = System.currentTimeMillis() - startTime
        syncHistoryDao.updateSync(
            id = historyId,
            status = if (success) SyncStatus.SUCCESS.name else SyncStatus.FAILED.name,
            stockCount = stockCount,
            analysisCount = analysisCount,
            indicatorCount = indicatorCount,
            etfCount = etfCount,
            etfConstituentCount = etfConstituentCount,
            errorMessage = errorMessage,
            durationMs = durationMs
        )
        syncHistoryDao.trimHistory()
    }

    private suspend fun ensureConfigExists() {
        if (configDao.getConfigOnce() == null) {
            configDao.insertOrUpdate(SchedulingConfigEntity())
        }
    }

    private fun SchedulingConfigEntity.toDomain() = SchedulingConfig(
        isEnabled = isEnabled,
        syncHour = syncHour,
        syncMinute = syncMinute,
        lastSyncAt = lastSyncAt,
        lastSyncStatus = SyncStatus.fromString(lastSyncStatus),
        lastSyncMessage = lastSyncMessage,
        isErrorStopped = isErrorStopped
    )

    private fun SyncHistoryEntity.toDomain() = SyncHistory(
        id = id,
        syncType = try { SyncType.valueOf(syncType) } catch (e: Exception) { SyncType.MANUAL },
        status = SyncStatus.fromString(status),
        stockCount = stockCount,
        analysisCount = analysisCount,
        indicatorCount = indicatorCount,
        etfCount = etfCount,
        etfConstituentCount = etfConstituentCount,
        errorMessage = errorMessage,
        durationMs = durationMs,
        syncedAt = syncedAt
    )
}
