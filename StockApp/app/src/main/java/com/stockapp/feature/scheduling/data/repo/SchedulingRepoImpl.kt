package com.stockapp.feature.scheduling.data.repo

import android.util.Log
import com.stockapp.core.db.dao.SchedulingConfigDao
import com.stockapp.core.db.dao.StockAnalysisDataDao
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.dao.SyncHistoryDao
import com.stockapp.core.db.entity.SchedulingConfigEntity
import com.stockapp.core.db.entity.StockAnalysisDataEntity
import com.stockapp.core.db.entity.StockEntity
import com.stockapp.core.db.entity.SyncHistoryEntity
import com.stockapp.core.py.PyClient
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.repo.EtfRepository
import com.stockapp.feature.etf.domain.usecase.CollectAllEtfDataUC
import com.stockapp.feature.scheduling.domain.model.SchedulingConfig
import com.stockapp.feature.scheduling.domain.model.SyncHistory
import com.stockapp.feature.scheduling.domain.model.SyncResult
import com.stockapp.feature.scheduling.domain.model.SyncStatus
import com.stockapp.feature.scheduling.domain.model.SyncType
import com.stockapp.feature.scheduling.domain.repo.SchedulingRepo
import com.stockapp.feature.search.domain.model.SearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    private val pyClient: PyClient,
    private val json: Json,
    private val collectAllEtfDataUC: CollectAllEtfDataUC,
    private val etfRepository: EtfRepository
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

        if (!pyClient.isReady()) {
            Log.w(TAG, "PyClient not ready")
            return@withContext Result.failure(Exception("API 키가 설정되지 않았습니다."))
        }

        try {
            val result = pyClient.call(
                module = "stock_analyzer.stock.search",
                func = "get_all",
                args = emptyList(),
                timeoutMs = 120_000
            ) { jsonStr ->
                parseStockList(jsonStr)
            }

            result.fold(
                onSuccess = { stocks ->
                    Log.d(TAG, "Fetched ${stocks.size} stocks")

                    // Limit stock count
                    val limitedStocks = if (stocks.size > MAX_STOCKS_PER_BATCH) {
                        stocks.sortedWith(
                            compareBy<StockEntity> {
                                when (it.market) {
                                    "KOSPI" -> 0
                                    "KOSDAQ" -> 1
                                    else -> 2
                                }
                            }.thenBy { it.name }
                        ).take(MAX_STOCKS_PER_BATCH)
                    } else {
                        stocks
                    }

                    // Clear and insert
                    stockDao.deleteAll()
                    stockDao.insertAll(limitedStocks)

                    val count = stockDao.count()
                    Log.d(TAG, "Stock list synced: $count stocks")
                    Result.success(count)
                },
                onFailure = { e ->
                    Log.e(TAG, "syncStockList failed: ${e.message}", e)
                    Result.failure(e)
                }
            )
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

        if (!pyClient.isReady()) {
            return@withContext Result.failure(Exception("API 키가 설정되지 않았습니다."))
        }

        var syncedCount = 0

        try {
            tickers.chunked(ANALYSIS_BATCH_SIZE).forEach { batch ->
                batch.forEach { ticker ->
                    try {
                        val lastDate = analysisDataDao.getLastAnalyzedDate(ticker)
                        val result = fetchAndSaveAnalysis(ticker, lastDate)
                        if (result.isSuccess) {
                            syncedCount++
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync analysis for $ticker: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "Analysis data synced: $syncedCount tickers")
            Result.success(syncedCount)
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

    private suspend fun syncTopStocksAnalysis(): Result<Int> {
        // Get recently searched stocks or top KOSPI/KOSDAQ stocks
        val stocks = stockDao.getAllOnce(100) // Limit to top 100

        if (stocks.isEmpty()) {
            return Result.success(0)
        }

        return syncAnalysisData(stocks.map { it.ticker })
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
        } catch (e: Exception) {
            Log.e(TAG, "syncEtfData exception: ${e.message}", e)
            Pair(0, 0)
        }
    }

    private suspend fun fetchAndSaveAnalysis(ticker: String, lastDate: String?): Result<Unit> {
        try {
            val result = pyClient.call(
                module = "stock_analyzer.stock.analysis",
                func = "analyze",
                args = listOf(ticker, 30), // Fetch 30 days
                timeoutMs = 60_000
            ) { jsonStr -> jsonStr }

            return result.fold(
                onSuccess = { jsonStr ->
                    val analysisData = parseAnalysisData(ticker, jsonStr)
                    if (analysisData != null) {
                        analysisDataDao.insertOrUpdate(analysisData)
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to parse analysis data"))
                    }
                },
                onFailure = { e -> Result.failure(e) }
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun parseAnalysisData(ticker: String, jsonStr: String): StockAnalysisDataEntity? {
        return try {
            val response = json.decodeFromString<AnalysisApiResponse>(jsonStr)
            if (response.ok && response.data != null) {
                val data = response.data
                val lastDate = data.dates.lastOrNull() ?: ""
                val latestMcap = data.mcap.lastOrNull() ?: 0L
                val latestFor5d = data.for5d.lastOrNull() ?: 0L
                val latestIns5d = data.ins5d.lastOrNull() ?: 0L

                // Calculate supply ratio
                val supplyRatio = if (latestMcap > 0) {
                    ((latestFor5d + latestIns5d).toDouble() / latestMcap) * 100
                } else 0.0

                // Determine signal type
                val signalType = when {
                    supplyRatio > 0.5 -> "STRONG_BUY"
                    supplyRatio > 0.2 -> "BUY"
                    supplyRatio < -0.5 -> "STRONG_SELL"
                    supplyRatio < -0.2 -> "SELL"
                    else -> "NEUTRAL"
                }

                StockAnalysisDataEntity(
                    ticker = ticker,
                    name = data.name,
                    market = data.market ?: "UNKNOWN",
                    marketCap = latestMcap,
                    foreignNet5d = latestFor5d,
                    institutionNet5d = latestIns5d,
                    supplyRatio = supplyRatio,
                    signalType = signalType,
                    lastAnalyzedDate = lastDate,
                    detailDataJson = jsonStr,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseAnalysisData failed: ${e.message}")
            null
        }
    }

    private fun parseStockList(jsonStr: String): List<StockEntity> {
        val response = json.decodeFromString<SearchResponse>(jsonStr)
        if (response.ok && response.data != null) {
            val now = System.currentTimeMillis()
            return response.data.map { stock ->
                StockEntity(
                    ticker = stock.ticker,
                    name = stock.name,
                    market = stock.market,
                    updatedAt = now
                )
            }
        }
        throw Exception(response.error?.msg ?: "Failed to parse stock list")
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
        lastSyncMessage = lastSyncMessage
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

@Serializable
private data class AnalysisApiResponse(
    val ok: Boolean,
    val data: AnalysisData? = null,
    val error: ErrorInfo? = null
)

@Serializable
private data class AnalysisData(
    val ticker: String,
    val name: String,
    val market: String? = null,
    val dates: List<String> = emptyList(),
    val mcap: List<Long> = emptyList(),
    val for5d: List<Long> = emptyList(),
    val ins5d: List<Long> = emptyList()
)

@Serializable
private data class ErrorInfo(
    val code: String? = null,
    val msg: String? = null
)
