package com.stockapp.core.backup

import android.content.Context
import android.provider.Settings
import com.stockapp.BuildConfig
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.entity.AnalysisCacheEntity
import com.stockapp.core.db.entity.DailyEtfStatisticsEntity
import com.stockapp.core.db.entity.EtfCollectionHistoryEntity
import com.stockapp.core.db.entity.EtfConstituentEntity
import com.stockapp.core.db.entity.EtfEntity
import com.stockapp.core.db.entity.EtfKeywordEntity
import com.stockapp.core.db.entity.FinancialCacheEntity
import com.stockapp.core.db.entity.IndicatorCacheEntity
import com.stockapp.core.db.entity.IndicatorDataEntity
import com.stockapp.core.db.entity.SchedulingConfigEntity
import com.stockapp.core.db.entity.SearchHistoryEntity
import com.stockapp.core.db.entity.StockAnalysisDataEntity
import com.stockapp.core.db.entity.StockEntity
import com.stockapp.core.db.entity.SyncHistoryEntity
import com.stockapp.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for database backup and restore operations.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDb,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Create a backup of the database.
     */
    suspend fun createBackup(
        backupType: BackupType,
        startDate: String? = null,
        endDate: String? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): BackupFile = withContext(ioDispatcher) {
        val startMs = startDate?.let { parseDate(it) } ?: 0L
        val endMs = endDate?.let { parseDate(it) + 86400000L } ?: Long.MAX_VALUE // End of day

        // For ETF constituents which use String date
        val startDateStr = startDate ?: "1970-01-01"
        val endDateStr = endDate ?: "2099-12-31"

        val entityCounts = mutableMapOf<String, Int>()
        var progress = 0f
        val totalSteps = 14f

        // 1. Stocks
        onProgress(progress / totalSteps, "종목 데이터 수집 중...")
        val stocks = if (backupType == BackupType.FULL) {
            db.stockDao().getAllOnce()
        } else {
            db.stockDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["stocks"] = stocks.size
        progress++

        // 2. Analysis Cache
        onProgress(progress / totalSteps, "분석 캐시 수집 중...")
        val analysisCache = if (backupType == BackupType.FULL) {
            db.analysisCacheDao().getAllOnce()
        } else {
            db.analysisCacheDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["analysisCache"] = analysisCache.size
        progress++

        // 3. Search History
        onProgress(progress / totalSteps, "검색 기록 수집 중...")
        val searchHistory = if (backupType == BackupType.FULL) {
            db.searchHistoryDao().getAllOnce()
        } else {
            db.searchHistoryDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["searchHistory"] = searchHistory.size
        progress++

        // 4. Indicator Cache
        onProgress(progress / totalSteps, "지표 캐시 수집 중...")
        val indicatorCache = if (backupType == BackupType.FULL) {
            db.indicatorCacheDao().getAllOnce()
        } else {
            db.indicatorCacheDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["indicatorCache"] = indicatorCache.size
        progress++

        // 5. Scheduling Config (always full - singleton)
        onProgress(progress / totalSteps, "스케줄링 설정 수집 중...")
        val schedulingConfig = db.schedulingConfigDao().getConfigOnce()?.toBackup()
        entityCounts["schedulingConfig"] = if (schedulingConfig != null) 1 else 0
        progress++

        // 6. Sync History
        onProgress(progress / totalSteps, "동기화 기록 수집 중...")
        val syncHistory = if (backupType == BackupType.FULL) {
            db.syncHistoryDao().getAllOnce()
        } else {
            db.syncHistoryDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["syncHistory"] = syncHistory.size
        progress++

        // 7. Stock Analysis Data
        onProgress(progress / totalSteps, "분석 데이터 수집 중...")
        val stockAnalysisData = if (backupType == BackupType.FULL) {
            db.stockAnalysisDataDao().getAllOnce()
        } else {
            db.stockAnalysisDataDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["stockAnalysisData"] = stockAnalysisData.size
        progress++

        // 8. Indicator Data
        onProgress(progress / totalSteps, "지표 데이터 수집 중...")
        val indicatorData = if (backupType == BackupType.FULL) {
            db.indicatorDataDao().getAllOnce()
        } else {
            db.indicatorDataDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["indicatorData"] = indicatorData.size
        progress++

        // 9. ETFs
        onProgress(progress / totalSteps, "ETF 데이터 수집 중...")
        val etfs = if (backupType == BackupType.FULL) {
            db.etfDao().getAllEtfs()
        } else {
            db.etfDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["etfs"] = etfs.size
        progress++

        // 10. ETF Constituents
        onProgress(progress / totalSteps, "ETF 구성종목 수집 중...")
        val etfConstituents = if (backupType == BackupType.FULL) {
            db.etfConstituentDao().getAllOnce()
        } else {
            db.etfConstituentDao().getInDateRange(startDateStr, endDateStr)
        }.map { it.toBackup() }
        entityCounts["etfConstituents"] = etfConstituents.size
        progress++

        // 11. ETF Keywords (always full - user settings)
        onProgress(progress / totalSteps, "ETF 키워드 수집 중...")
        val etfKeywords = db.etfKeywordDao().getAllKeywords().map { it.toBackup() }
        entityCounts["etfKeywords"] = etfKeywords.size
        progress++

        // 12. ETF Collection History
        onProgress(progress / totalSteps, "ETF 수집 기록 수집 중...")
        val etfCollectionHistory = if (backupType == BackupType.FULL) {
            db.etfCollectionHistoryDao().getAllOnce()
        } else {
            db.etfCollectionHistoryDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["etfCollectionHistory"] = etfCollectionHistory.size
        progress++

        // 13. Daily ETF Statistics
        onProgress(progress / totalSteps, "ETF 통계 수집 중...")
        val dailyEtfStatistics = if (backupType == BackupType.FULL) {
            db.dailyEtfStatisticsDao().getAllOnce()
        } else {
            db.dailyEtfStatisticsDao().getInRange(startDateStr, endDateStr)
        }.map { it.toBackup() }
        entityCounts["dailyEtfStatistics"] = dailyEtfStatistics.size
        progress++

        // 14. Financial Cache
        onProgress(progress / totalSteps, "재무 캐시 수집 중...")
        val financialCache = if (backupType == BackupType.FULL) {
            db.financialCacheDao().getAllOnce()
        } else {
            db.financialCacheDao().getInDateRange(startMs, endMs)
        }.map { it.toBackup() }
        entityCounts["financialCache"] = financialCache.size
        progress++

        onProgress(1f, "백업 완료")

        BackupFile(
            metadata = BackupMetadata(
                formatVersion = BACKUP_FORMAT_VERSION,
                appVersion = BuildConfig.VERSION_NAME,
                dbVersion = 9, // Current DB version
                createdAt = System.currentTimeMillis(),
                deviceId = getDeviceId(),
                backupType = backupType,
                filterStartDate = startDate,
                filterEndDate = endDate,
                entityCounts = entityCounts
            ),
            tables = BackupTables(
                stocks = stocks,
                analysisCache = analysisCache,
                searchHistory = searchHistory,
                indicatorCache = indicatorCache,
                schedulingConfig = schedulingConfig,
                syncHistory = syncHistory,
                stockAnalysisData = stockAnalysisData,
                indicatorData = indicatorData,
                etfs = etfs,
                etfConstituents = etfConstituents,
                etfKeywords = etfKeywords,
                etfCollectionHistory = etfCollectionHistory,
                dailyEtfStatistics = dailyEtfStatistics,
                financialCache = financialCache
            )
        )
    }

    /**
     * Restore data from a backup.
     */
    suspend fun restoreBackup(
        backup: BackupFile,
        restoreMode: RestoreMode,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): RestoreResult = withContext(ioDispatcher) {
        val restoredCounts = mutableMapOf<String, Int>()
        val skippedTables = mutableListOf<String>()
        var progress = 0f
        val totalSteps = 14f

        try {
            // Clear data if REPLACE mode
            if (restoreMode == RestoreMode.REPLACE) {
                onProgress(0f, "기존 데이터 삭제 중...")
                clearAllTables()
            }

            // 1. Stocks
            onProgress(progress / totalSteps, "종목 데이터 복원 중...")
            backup.tables.stocks?.let { stocks ->
                db.stockDao().insertAll(stocks.map { it.toEntity() })
                restoredCounts["stocks"] = stocks.size
            } ?: skippedTables.add("stocks")
            progress++

            // 2. Analysis Cache
            onProgress(progress / totalSteps, "분석 캐시 복원 중...")
            backup.tables.analysisCache?.forEach { cache ->
                db.analysisCacheDao().insert(cache.toEntity())
            }
            restoredCounts["analysisCache"] = backup.tables.analysisCache?.size ?: 0
            progress++

            // 3. Search History
            onProgress(progress / totalSteps, "검색 기록 복원 중...")
            backup.tables.searchHistory?.forEach { history ->
                db.searchHistoryDao().insert(history.toEntity())
            }
            restoredCounts["searchHistory"] = backup.tables.searchHistory?.size ?: 0
            progress++

            // 4. Indicator Cache
            onProgress(progress / totalSteps, "지표 캐시 복원 중...")
            backup.tables.indicatorCache?.forEach { cache ->
                db.indicatorCacheDao().insert(cache.toEntity())
            }
            restoredCounts["indicatorCache"] = backup.tables.indicatorCache?.size ?: 0
            progress++

            // 5. Scheduling Config
            onProgress(progress / totalSteps, "스케줄링 설정 복원 중...")
            backup.tables.schedulingConfig?.let { config ->
                db.schedulingConfigDao().insertOrUpdate(config.toEntity())
                restoredCounts["schedulingConfig"] = 1
            } ?: skippedTables.add("schedulingConfig")
            progress++

            // 6. Sync History
            onProgress(progress / totalSteps, "동기화 기록 복원 중...")
            backup.tables.syncHistory?.forEach { history ->
                db.syncHistoryDao().insert(history.toEntity())
            }
            restoredCounts["syncHistory"] = backup.tables.syncHistory?.size ?: 0
            progress++

            // 7. Stock Analysis Data
            onProgress(progress / totalSteps, "분석 데이터 복원 중...")
            backup.tables.stockAnalysisData?.let { data ->
                db.stockAnalysisDataDao().insertOrUpdateAll(data.map { it.toEntity() })
                restoredCounts["stockAnalysisData"] = data.size
            } ?: skippedTables.add("stockAnalysisData")
            progress++

            // 8. Indicator Data
            onProgress(progress / totalSteps, "지표 데이터 복원 중...")
            backup.tables.indicatorData?.let { data ->
                db.indicatorDataDao().insertOrUpdateAll(data.map { it.toEntity() })
                restoredCounts["indicatorData"] = data.size
            } ?: skippedTables.add("indicatorData")
            progress++

            // 9. ETFs
            onProgress(progress / totalSteps, "ETF 데이터 복원 중...")
            backup.tables.etfs?.let { etfs ->
                db.etfDao().insertAll(etfs.map { it.toEntity() })
                restoredCounts["etfs"] = etfs.size
            } ?: skippedTables.add("etfs")
            progress++

            // 10. ETF Constituents
            onProgress(progress / totalSteps, "ETF 구성종목 복원 중...")
            backup.tables.etfConstituents?.let { constituents ->
                db.etfConstituentDao().insertAll(constituents.map { it.toEntity() })
                restoredCounts["etfConstituents"] = constituents.size
            } ?: skippedTables.add("etfConstituents")
            progress++

            // 11. ETF Keywords
            onProgress(progress / totalSteps, "ETF 키워드 복원 중...")
            backup.tables.etfKeywords?.forEach { keyword ->
                // Skip ID to let Room auto-generate
                db.etfKeywordDao().insert(keyword.toEntity())
            }
            restoredCounts["etfKeywords"] = backup.tables.etfKeywords?.size ?: 0
            progress++

            // 12. ETF Collection History
            onProgress(progress / totalSteps, "ETF 수집 기록 복원 중...")
            backup.tables.etfCollectionHistory?.forEach { history ->
                db.etfCollectionHistoryDao().insert(history.toEntity())
            }
            restoredCounts["etfCollectionHistory"] = backup.tables.etfCollectionHistory?.size ?: 0
            progress++

            // 13. Daily ETF Statistics
            onProgress(progress / totalSteps, "ETF 통계 복원 중...")
            backup.tables.dailyEtfStatistics?.let { stats ->
                db.dailyEtfStatisticsDao().insertAll(stats.map { it.toEntity() })
                restoredCounts["dailyEtfStatistics"] = stats.size
            } ?: skippedTables.add("dailyEtfStatistics")
            progress++

            // 14. Financial Cache
            onProgress(progress / totalSteps, "재무 캐시 복원 중...")
            backup.tables.financialCache?.forEach { cache ->
                db.financialCacheDao().insert(cache.toEntity())
            }
            restoredCounts["financialCache"] = backup.tables.financialCache?.size ?: 0
            progress++

            onProgress(1f, "복원 완료")

            RestoreResult(
                success = true,
                restoredCounts = restoredCounts,
                skippedTables = skippedTables
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                restoredCounts = restoredCounts,
                skippedTables = skippedTables,
                errorMessage = e.message ?: "복원 중 오류가 발생했습니다"
            )
        }
    }

    private suspend fun clearAllTables() {
        db.stockDao().deleteAll()
        db.analysisCacheDao().deleteAll()
        db.searchHistoryDao().deleteAll()
        db.indicatorCacheDao().deleteAll()
        db.syncHistoryDao().deleteAll()
        db.stockAnalysisDataDao().deleteAll()
        db.indicatorDataDao().deleteAll()
        db.etfDao().deleteAll()
        db.etfConstituentDao().deleteAll()
        db.etfKeywordDao().deleteAll()
        db.etfCollectionHistoryDao().deleteAll()
        db.dailyEtfStatisticsDao().deleteAll()
        db.financialCacheDao().deleteAll()
        // Note: schedulingConfig is not cleared (singleton)
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            dateFormat.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    @Suppress("HardwareIds")
    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // ============================================================
    // Entity to Backup conversion extensions
    // ============================================================

    private fun StockEntity.toBackup() = StockBackup(
        ticker = ticker,
        name = name,
        market = market,
        updatedAt = updatedAt
    )

    private fun AnalysisCacheEntity.toBackup() = AnalysisCacheBackup(
        ticker = ticker,
        data = data,
        startDate = startDate,
        endDate = endDate,
        cachedAt = cachedAt
    )

    private fun SearchHistoryEntity.toBackup() = SearchHistoryBackup(
        id = id,
        ticker = ticker,
        name = name,
        searchedAt = searchedAt
    )

    private fun IndicatorCacheEntity.toBackup() = IndicatorCacheBackup(
        key = key,
        ticker = ticker,
        type = type,
        data = data,
        cachedAt = cachedAt
    )

    private fun SchedulingConfigEntity.toBackup() = SchedulingConfigBackup(
        id = id,
        isEnabled = isEnabled,
        syncHour = syncHour,
        syncMinute = syncMinute,
        lastSyncAt = lastSyncAt,
        lastSyncStatus = lastSyncStatus,
        lastSyncMessage = lastSyncMessage,
        isErrorStopped = isErrorStopped,
        updatedAt = updatedAt
    )

    private fun SyncHistoryEntity.toBackup() = SyncHistoryBackup(
        id = id,
        syncType = syncType,
        status = status,
        stockCount = stockCount,
        analysisCount = analysisCount,
        indicatorCount = indicatorCount,
        etfCount = etfCount,
        etfConstituentCount = etfConstituentCount,
        errorMessage = errorMessage,
        durationMs = durationMs,
        syncedAt = syncedAt
    )

    private fun StockAnalysisDataEntity.toBackup() = StockAnalysisDataBackup(
        ticker = ticker,
        name = name,
        market = market,
        marketCap = marketCap,
        foreignNet5d = foreignNet5d,
        institutionNet5d = institutionNet5d,
        supplyRatio = supplyRatio,
        signalType = signalType,
        lastAnalyzedDate = lastAnalyzedDate,
        detailDataJson = detailDataJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun IndicatorDataEntity.toBackup() = IndicatorDataBackup(
        ticker = ticker,
        indicatorType = indicatorType,
        summaryJson = summaryJson,
        detailDataJson = detailDataJson,
        lastCalculatedDate = lastCalculatedDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun EtfEntity.toBackup() = EtfBackup(
        etfCode = etfCode,
        etfName = etfName,
        etfType = etfType,
        managementCompany = managementCompany,
        trackingIndex = trackingIndex,
        assetClass = assetClass,
        totalAssets = totalAssets,
        isFiltered = isFiltered,
        updatedAt = updatedAt
    )

    private fun EtfConstituentEntity.toBackup() = EtfConstituentBackup(
        etfCode = etfCode,
        etfName = etfName,
        stockCode = stockCode,
        stockName = stockName,
        currentPrice = currentPrice,
        priceChange = priceChange,
        priceChangeSign = priceChangeSign,
        priceChangeRate = priceChangeRate,
        volume = volume,
        tradingValue = tradingValue,
        marketCap = marketCap,
        weight = weight,
        evaluationAmount = evaluationAmount,
        collectedDate = collectedDate,
        collectedAt = collectedAt
    )

    private fun EtfKeywordEntity.toBackup() = EtfKeywordBackup(
        id = id,
        keyword = keyword,
        filterType = filterType,
        isEnabled = isEnabled,
        createdAt = createdAt
    )

    private fun EtfCollectionHistoryEntity.toBackup() = EtfCollectionHistoryBackup(
        id = id,
        collectedDate = collectedDate,
        totalEtfs = totalEtfs,
        totalConstituents = totalConstituents,
        status = status,
        errorMessage = errorMessage,
        startedAt = startedAt,
        completedAt = completedAt
    )

    private fun DailyEtfStatisticsEntity.toBackup() = DailyEtfStatisticsBackup(
        date = date,
        newStockCount = newStockCount,
        newStockAmount = newStockAmount,
        removedStockCount = removedStockCount,
        removedStockAmount = removedStockAmount,
        increasedStockCount = increasedStockCount,
        increasedStockAmount = increasedStockAmount,
        decreasedStockCount = decreasedStockCount,
        decreasedStockAmount = decreasedStockAmount,
        cashDepositAmount = cashDepositAmount,
        cashDepositChange = cashDepositChange,
        cashDepositChangeRate = cashDepositChangeRate,
        totalEtfCount = totalEtfCount,
        totalHoldingAmount = totalHoldingAmount,
        calculatedAt = calculatedAt
    )

    private fun FinancialCacheEntity.toBackup() = FinancialCacheBackup(
        ticker = ticker,
        name = name,
        data = data,
        cachedAt = cachedAt
    )

    // ============================================================
    // Backup to Entity conversion extensions
    // ============================================================

    private fun StockBackup.toEntity() = StockEntity(
        ticker = ticker,
        name = name,
        market = market,
        updatedAt = updatedAt
    )

    private fun AnalysisCacheBackup.toEntity() = AnalysisCacheEntity(
        ticker = ticker,
        data = data,
        startDate = startDate,
        endDate = endDate,
        cachedAt = cachedAt
    )

    private fun SearchHistoryBackup.toEntity() = SearchHistoryEntity(
        id = 0, // Let Room auto-generate
        ticker = ticker,
        name = name,
        searchedAt = searchedAt
    )

    private fun IndicatorCacheBackup.toEntity() = IndicatorCacheEntity(
        key = key,
        ticker = ticker,
        type = type,
        data = data,
        cachedAt = cachedAt
    )

    private fun SchedulingConfigBackup.toEntity() = SchedulingConfigEntity(
        id = id,
        isEnabled = isEnabled,
        syncHour = syncHour,
        syncMinute = syncMinute,
        lastSyncAt = lastSyncAt,
        lastSyncStatus = lastSyncStatus,
        lastSyncMessage = lastSyncMessage,
        isErrorStopped = isErrorStopped,
        updatedAt = updatedAt
    )

    private fun SyncHistoryBackup.toEntity() = SyncHistoryEntity(
        id = 0, // Let Room auto-generate
        syncType = syncType,
        status = status,
        stockCount = stockCount,
        analysisCount = analysisCount,
        indicatorCount = indicatorCount,
        etfCount = etfCount,
        etfConstituentCount = etfConstituentCount,
        errorMessage = errorMessage,
        durationMs = durationMs,
        syncedAt = syncedAt
    )

    private fun StockAnalysisDataBackup.toEntity() = StockAnalysisDataEntity(
        ticker = ticker,
        name = name,
        market = market,
        marketCap = marketCap,
        foreignNet5d = foreignNet5d,
        institutionNet5d = institutionNet5d,
        supplyRatio = supplyRatio,
        signalType = signalType,
        lastAnalyzedDate = lastAnalyzedDate,
        detailDataJson = detailDataJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun IndicatorDataBackup.toEntity() = IndicatorDataEntity(
        ticker = ticker,
        indicatorType = indicatorType,
        summaryJson = summaryJson,
        detailDataJson = detailDataJson,
        lastCalculatedDate = lastCalculatedDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun EtfBackup.toEntity() = EtfEntity(
        etfCode = etfCode,
        etfName = etfName,
        etfType = etfType,
        managementCompany = managementCompany,
        trackingIndex = trackingIndex,
        assetClass = assetClass,
        totalAssets = totalAssets,
        isFiltered = isFiltered,
        updatedAt = updatedAt
    )

    private fun EtfConstituentBackup.toEntity() = EtfConstituentEntity(
        etfCode = etfCode,
        etfName = etfName,
        stockCode = stockCode,
        stockName = stockName,
        currentPrice = currentPrice,
        priceChange = priceChange,
        priceChangeSign = priceChangeSign,
        priceChangeRate = priceChangeRate,
        volume = volume,
        tradingValue = tradingValue,
        marketCap = marketCap,
        weight = weight,
        evaluationAmount = evaluationAmount,
        collectedDate = collectedDate,
        collectedAt = collectedAt
    )

    private fun EtfKeywordBackup.toEntity() = EtfKeywordEntity(
        id = 0, // Let Room auto-generate
        keyword = keyword,
        filterType = filterType,
        isEnabled = isEnabled,
        createdAt = createdAt
    )

    private fun EtfCollectionHistoryBackup.toEntity() = EtfCollectionHistoryEntity(
        id = 0, // Let Room auto-generate
        collectedDate = collectedDate,
        totalEtfs = totalEtfs,
        totalConstituents = totalConstituents,
        status = status,
        errorMessage = errorMessage,
        startedAt = startedAt,
        completedAt = completedAt
    )

    private fun DailyEtfStatisticsBackup.toEntity() = DailyEtfStatisticsEntity(
        date = date,
        newStockCount = newStockCount,
        newStockAmount = newStockAmount,
        removedStockCount = removedStockCount,
        removedStockAmount = removedStockAmount,
        increasedStockCount = increasedStockCount,
        increasedStockAmount = increasedStockAmount,
        decreasedStockCount = decreasedStockCount,
        decreasedStockAmount = decreasedStockAmount,
        cashDepositAmount = cashDepositAmount,
        cashDepositChange = cashDepositChange,
        cashDepositChangeRate = cashDepositChangeRate,
        totalEtfCount = totalEtfCount,
        totalHoldingAmount = totalHoldingAmount,
        calculatedAt = calculatedAt
    )

    private fun FinancialCacheBackup.toEntity() = FinancialCacheEntity(
        ticker = ticker,
        name = name,
        data = data,
        cachedAt = cachedAt
    )
}
