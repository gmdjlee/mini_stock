package com.stockapp.feature.etf.data.repo

import com.stockapp.core.db.dao.DailyEtfStatisticsDao
import com.stockapp.core.db.dao.EtfCollectionHistoryDao
import com.stockapp.core.db.dao.EtfConstituentDao
import com.stockapp.core.db.dao.EtfDao
import com.stockapp.core.db.dao.EtfKeywordDao
import com.stockapp.core.db.entity.DailyEtfStatisticsEntity
import com.stockapp.core.db.entity.EtfCollectionHistoryEntity
import com.stockapp.core.db.entity.EtfConstituentEntity
import com.stockapp.core.db.entity.EtfEntity
import com.stockapp.core.db.entity.EtfKeywordEntity
import com.stockapp.feature.etf.domain.model.AmountHistory
import com.stockapp.feature.etf.domain.model.CashDepositTrend
import com.stockapp.feature.etf.domain.model.CollectionHistory
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.ComparisonResult
import com.stockapp.feature.etf.domain.model.ComparisonSummary
import com.stockapp.feature.etf.domain.model.ContainingEtfInfo
import com.stockapp.feature.etf.domain.model.DailyEtfStatistics
import com.stockapp.feature.etf.domain.model.EtfCashDetail
import com.stockapp.feature.etf.domain.model.EtfConstituent
import com.stockapp.feature.etf.domain.model.EtfDateRange
import com.stockapp.feature.etf.domain.model.EtfInfo
import com.stockapp.feature.etf.domain.model.EtfKeyword
import com.stockapp.feature.etf.domain.model.EtfType
import com.stockapp.feature.etf.domain.model.FilterType
import com.stockapp.feature.etf.domain.model.HoldingStatus
import com.stockapp.feature.etf.domain.model.HoldingWithComparison
import com.stockapp.feature.etf.domain.model.StockAnalysisResult
import com.stockapp.feature.etf.domain.model.StockChange
import com.stockapp.feature.etf.domain.model.StockRanking
import com.stockapp.feature.etf.domain.model.WeightHistory
import com.stockapp.feature.etf.domain.repo.EtfRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ETF Repository implementation using Room DAOs.
 */
@Singleton
class EtfRepositoryImpl @Inject constructor(
    private val etfDao: EtfDao,
    private val constituentDao: EtfConstituentDao,
    private val keywordDao: EtfKeywordDao,
    private val historyDao: EtfCollectionHistoryDao,
    private val statisticsDao: DailyEtfStatisticsDao
) : EtfRepository {

    // ==================== ETF Info ====================

    override suspend fun getAllEtfs(): Result<List<EtfInfo>> = runCatching {
        etfDao.getAllEtfs().map { it.toDomain() }
    }

    override fun observeAllEtfs(): Flow<List<EtfInfo>> =
        etfDao.observeAllEtfs().map { list -> list.map { it.toDomain() } }

    override suspend fun getFilteredEtfs(): Result<List<EtfInfo>> = runCatching {
        etfDao.getFilteredEtfs().map { it.toDomain() }
    }

    override fun observeFilteredEtfs(): Flow<List<EtfInfo>> =
        etfDao.observeFilteredEtfs().map { list -> list.map { it.toDomain() } }

    override suspend fun getEtfByCode(etfCode: String): Result<EtfInfo?> = runCatching {
        etfDao.getByCode(etfCode)?.toDomain()
    }

    override suspend fun saveEtfs(etfs: List<EtfInfo>): Result<Unit> = runCatching {
        etfDao.insertAll(etfs.map { it.toEntity() })
    }

    override suspend fun updateEtfFilterStatus(
        etfCode: String,
        isFiltered: Boolean
    ): Result<Unit> = runCatching {
        etfDao.updateFilterStatus(etfCode, isFiltered)
    }

    override suspend fun deleteAllEtfs(): Result<Unit> = runCatching {
        etfDao.deleteAll()
    }

    // ==================== ETF Constituents ====================

    override suspend fun getConstituentsByDate(date: String): Result<List<EtfConstituent>> =
        runCatching {
            constituentDao.getByDate(date).map { it.toDomain() }
        }

    override suspend fun getConstituentsByEtfAndDate(
        etfCode: String,
        date: String
    ): Result<List<EtfConstituent>> = runCatching {
        constituentDao.getByEtfAndDate(etfCode, date).map { it.toDomain() }
    }

    override suspend fun saveConstituents(constituents: List<EtfConstituent>): Result<Unit> =
        runCatching {
            constituentDao.insertAll(constituents.map { it.toEntity() })
        }

    override suspend fun getCollectionDates(): Result<List<String>> = runCatching {
        constituentDao.getCollectionDates()
    }

    override suspend fun getLatestDate(): Result<String?> = runCatching {
        constituentDao.getLatestDate()
    }

    override suspend fun getPreviousDate(date: String): Result<String?> = runCatching {
        constituentDao.getPreviousDate(date)
    }

    override suspend fun getDataDateRange(): Result<EtfDateRange> = runCatching {
        val range = constituentDao.getDataDateRange()
        EtfDateRange(range?.startDate, range?.endDate)
    }

    override suspend fun deleteOldConstituents(cutoffDate: String): Result<Unit> = runCatching {
        constituentDao.deleteOldData(cutoffDate)
    }

    override suspend fun deleteAllConstituents(): Result<Unit> = runCatching {
        constituentDao.deleteAll()
    }

    // ==================== ETF Keywords ====================

    override suspend fun getAllKeywords(): Result<List<EtfKeyword>> = runCatching {
        keywordDao.getAllKeywords().map { it.toDomain() }
    }

    override fun observeAllKeywords(): Flow<List<EtfKeyword>> =
        keywordDao.observeAllKeywords().map { list -> list.map { it.toDomain() } }

    override suspend fun getEnabledKeywords(): Result<List<EtfKeyword>> = runCatching {
        keywordDao.getEnabledKeywords().map { it.toDomain() }
    }

    override fun observeEnabledKeywords(): Flow<List<EtfKeyword>> =
        keywordDao.observeEnabledKeywords().map { list -> list.map { it.toDomain() } }

    override suspend fun getKeywordsByType(filterType: FilterType): Result<List<EtfKeyword>> =
        runCatching {
            keywordDao.getKeywordsByType(filterType.value).map { it.toDomain() }
        }

    override fun observeKeywordsByType(filterType: FilterType): Flow<List<EtfKeyword>> =
        keywordDao.observeKeywordsByType(filterType.value).map { list -> list.map { it.toDomain() } }

    override suspend fun addKeyword(keyword: String, filterType: FilterType): Result<Long> =
        runCatching {
            val entity = EtfKeywordEntity(
                keyword = keyword,
                filterType = filterType.value,
                isEnabled = true,
                createdAt = System.currentTimeMillis()
            )
            keywordDao.insert(entity)
        }

    override suspend fun deleteKeyword(id: Long): Result<Unit> = runCatching {
        keywordDao.deleteById(id)
    }

    override suspend fun updateKeywordEnabled(id: Long, enabled: Boolean): Result<Unit> =
        runCatching {
            keywordDao.updateEnabled(id, enabled)
        }

    override suspend fun keywordExists(keyword: String, filterType: FilterType): Result<Boolean> =
        runCatching {
            keywordDao.exists(keyword, filterType.value)
        }

    override suspend fun deleteAllKeywords(): Result<Unit> = runCatching {
        keywordDao.deleteAll()
    }

    // ==================== Collection History ====================

    override suspend fun getRecentHistory(limit: Int): Result<List<CollectionHistory>> =
        runCatching {
            historyDao.getRecentHistory(limit).map { it.toDomain() }
        }

    override fun observeRecentHistory(limit: Int): Flow<List<CollectionHistory>> =
        historyDao.observeRecentHistory(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getLatestHistory(): Result<CollectionHistory?> = runCatching {
        historyDao.getLatest()?.toDomain()
    }

    override fun observeLatestHistory(): Flow<CollectionHistory?> =
        historyDao.observeLatest().map { it?.toDomain() }

    override suspend fun startCollection(collectedDate: String): Result<Long> = runCatching {
        val entity = EtfCollectionHistoryEntity(
            collectedDate = collectedDate,
            totalEtfs = 0,
            totalConstituents = 0,
            status = CollectionStatus.IN_PROGRESS.value,
            errorMessage = null,
            startedAt = System.currentTimeMillis(),
            completedAt = null
        )
        historyDao.insert(entity)
    }

    override suspend fun completeCollection(
        id: Long,
        status: CollectionStatus,
        totalEtfs: Int,
        totalConstituents: Int,
        errorMessage: String?
    ): Result<Unit> = runCatching {
        historyDao.updateCompletion(
            id = id,
            status = status.value,
            totalEtfs = totalEtfs,
            totalConstituents = totalConstituents,
            errorMessage = errorMessage,
            completedAt = System.currentTimeMillis()
        )
    }

    override suspend fun trimHistory(keepCount: Int): Result<Unit> = runCatching {
        historyDao.trimHistory(keepCount)
    }

    // ==================== Statistics Queries ====================

    override suspend fun getStockRanking(date: String, limit: Int): Result<List<StockRanking>> =
        runCatching {
            constituentDao.getStockRankingByAmount(date, limit).mapIndexed { index, ranking ->
                StockRanking(
                    rank = index + 1,
                    stockCode = ranking.stockCode,
                    stockName = ranking.stockName,
                    totalAmount = ranking.totalAmount,
                    etfCount = ranking.etfCount
                )
            }
        }

    override suspend fun getNewlyIncludedStocks(
        today: String,
        yesterday: String
    ): Result<List<StockChange>> = runCatching {
        constituentDao.getNewlyIncludedStocks(today, yesterday).map { it.toDomain() }
    }

    override suspend fun getRemovedStocks(
        today: String,
        yesterday: String
    ): Result<List<StockChange>> = runCatching {
        constituentDao.getRemovedStocks(today, yesterday).map { it.toDomain() }
    }

    override suspend fun getWeightIncreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double
    ): Result<List<StockChange>> = runCatching {
        constituentDao.getWeightIncreasedStocks(today, yesterday, threshold).map { it.toDomain() }
    }

    override suspend fun getWeightDecreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double
    ): Result<List<StockChange>> = runCatching {
        constituentDao.getWeightDecreasedStocks(today, yesterday, threshold).map { it.toDomain() }
    }

    // ==================== Chart Data ====================

    override suspend fun getStockAmountHistory(stockCode: String): Result<List<AmountHistory>> =
        runCatching {
            constituentDao.getStockAmountHistory(stockCode).map { dateAmount ->
                AmountHistory(
                    date = dateAmount.collectedDate,
                    totalAmount = dateAmount.totalAmount
                )
            }
        }

    override suspend fun getStockWeightHistory(stockCode: String): Result<List<WeightHistory>> =
        runCatching {
            constituentDao.getStockWeightHistory(stockCode).map { dateWeight ->
                WeightHistory(
                    date = dateWeight.collectedDate,
                    avgWeight = dateWeight.avgWeight
                )
            }
        }

    // ==================== Entity <-> Domain Mappers ====================

    private fun EtfEntity.toDomain() = EtfInfo(
        etfCode = etfCode,
        etfName = etfName,
        etfType = EtfType.fromValue(etfType),
        managementCompany = managementCompany,
        trackingIndex = trackingIndex,
        assetClass = assetClass,
        totalAssets = totalAssets,
        isFiltered = isFiltered,
        updatedAt = updatedAt
    )

    private fun EtfInfo.toEntity() = EtfEntity(
        etfCode = etfCode,
        etfName = etfName,
        etfType = etfType.value,
        managementCompany = managementCompany,
        trackingIndex = trackingIndex,
        assetClass = assetClass,
        totalAssets = totalAssets,
        isFiltered = isFiltered,
        updatedAt = updatedAt
    )

    private fun EtfConstituentEntity.toDomain() = EtfConstituent(
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

    private fun EtfConstituent.toEntity() = EtfConstituentEntity(
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

    private fun EtfKeywordEntity.toDomain() = EtfKeyword(
        id = id,
        keyword = keyword,
        filterType = FilterType.fromValue(filterType),
        isEnabled = isEnabled,
        createdAt = createdAt
    )

    private fun EtfCollectionHistoryEntity.toDomain() = CollectionHistory(
        id = id,
        collectedDate = collectedDate,
        totalEtfs = totalEtfs,
        totalConstituents = totalConstituents,
        status = CollectionStatus.fromValue(status),
        errorMessage = errorMessage,
        startedAt = startedAt,
        completedAt = completedAt
    )

    private fun com.stockapp.core.db.dao.StockChangeInfo.toDomain() = StockChange(
        stockCode = stockCode,
        stockName = stockName,
        totalAmount = totalAmount,
        etfNames = etfNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    )

    // ==================== ETF Statistics (Phase 2) ====================

    override suspend fun getComparisonInRange(
        startDate: String,
        endDate: String
    ): Result<ComparisonResult> = runCatching {
        val currentConstituents = constituentDao.getByDate(endDate)
        val previousConstituents = constituentDao.getByDate(startDate)

        val currentMap = currentConstituents.groupBy { it.stockCode }
        val previousMap = previousConstituents.groupBy { it.stockCode }

        val allStockCodes = (currentMap.keys + previousMap.keys).distinct()

        val items = allStockCodes.map { stockCode ->
            val currentList = currentMap[stockCode] ?: emptyList()
            val previousList = previousMap[stockCode] ?: emptyList()

            val currentWeight = currentList.sumOf { it.weight }
            val previousWeight = previousList.sumOf { it.weight }
            val currentAmount = currentList.sumOf { it.evaluationAmount }
            val previousAmount = previousList.sumOf { it.evaluationAmount }

            val status = determineHoldingStatus(currentWeight, previousWeight)
            val stockName = currentList.firstOrNull()?.stockName
                ?: previousList.firstOrNull()?.stockName ?: ""
            val etfNames = currentList.map { it.etfName }.distinct()

            HoldingWithComparison(
                stockCode = stockCode,
                stockName = stockName,
                currentWeight = currentWeight,
                previousWeight = previousWeight,
                currentAmount = currentAmount,
                previousAmount = previousAmount,
                status = status,
                etfNames = etfNames
            )
        }

        val summary = ComparisonSummary(
            newCount = items.count { it.status == HoldingStatus.NEW },
            removedCount = items.count { it.status == HoldingStatus.REMOVED },
            increasedCount = items.count { it.status == HoldingStatus.INCREASE },
            decreasedCount = items.count { it.status == HoldingStatus.DECREASE },
            maintainCount = items.count { it.status == HoldingStatus.MAINTAIN }
        )

        ComparisonResult(
            currentDate = endDate,
            previousDate = startDate,
            items = items,
            summary = summary
        )
    }

    override suspend fun getCashDepositTrend(
        startDate: String,
        endDate: String
    ): Result<List<CashDepositTrend>> = runCatching {
        val statistics = statisticsDao.getInRange(startDate, endDate)
        statistics.map { entity ->
            CashDepositTrend(
                date = entity.date,
                totalAmount = entity.cashDepositAmount,
                changeAmount = entity.cashDepositChange,
                changeRate = entity.cashDepositChangeRate
            )
        }
    }

    override suspend fun getEtfCashDetails(date: String): Result<List<EtfCashDetail>> = runCatching {
        val constituents = constituentDao.getByDate(date)
        constituents
            .filter { isCashDeposit(it.stockName) }
            .map { entity ->
                EtfCashDetail(
                    etfCode = entity.etfCode,
                    etfName = entity.etfName,
                    cashAmount = entity.evaluationAmount,
                    cashWeight = entity.weight,
                    cashName = entity.stockName
                )
            }
    }

    override suspend fun getStockAnalysis(stockCode: String): Result<StockAnalysisResult> = runCatching {
        val latestDate = constituentDao.getLatestDate()
            ?: throw IllegalStateException("No data available")

        val constituents = constituentDao.getByStockCode(stockCode)
        if (constituents.isEmpty()) {
            throw IllegalStateException("Stock not found: $stockCode")
        }

        val latestConstituents = constituents.filter { it.collectedDate == latestDate }
        val stockName = latestConstituents.firstOrNull()?.stockName ?: constituents.first().stockName
        val totalAmount = latestConstituents.sumOf { it.evaluationAmount }
        val etfCount = latestConstituents.map { it.etfCode }.distinct().size

        val amountHistory = constituentDao.getStockAmountHistory(stockCode).map { dateAmount ->
            AmountHistory(
                date = dateAmount.collectedDate,
                totalAmount = dateAmount.totalAmount
            )
        }

        val weightHistory = constituentDao.getStockWeightHistory(stockCode).map { dateWeight ->
            WeightHistory(
                date = dateWeight.collectedDate,
                avgWeight = dateWeight.avgWeight
            )
        }

        val containingEtfs = latestConstituents.map { entity ->
            ContainingEtfInfo(
                etfCode = entity.etfCode,
                etfName = entity.etfName,
                weight = entity.weight,
                amount = entity.evaluationAmount,
                collectedDate = entity.collectedDate
            )
        }

        StockAnalysisResult(
            stockCode = stockCode,
            stockName = stockName,
            totalAmount = totalAmount,
            etfCount = etfCount,
            amountHistory = amountHistory,
            weightHistory = weightHistory,
            containingEtfs = containingEtfs
        )
    }

    override suspend fun calculateDailyStatistics(date: String): Result<Unit> = runCatching {
        val previousDate = constituentDao.getPreviousDate(date)
        val currentConstituents = constituentDao.getByDate(date)

        if (currentConstituents.isEmpty()) {
            throw IllegalStateException("No data for date: $date")
        }

        val previousConstituents = previousDate?.let { constituentDao.getByDate(it) } ?: emptyList()

        val currentMap = currentConstituents.groupBy { it.stockCode }
        val previousMap = previousConstituents.groupBy { it.stockCode }

        // Calculate new stocks
        val newStocks = currentMap.keys - previousMap.keys
        val newStockCount = newStocks.size
        val newStockAmount = newStocks.sumOf { code ->
            currentMap[code]?.sumOf { it.evaluationAmount } ?: 0L
        }

        // Calculate removed stocks
        val removedStocks = previousMap.keys - currentMap.keys
        val removedStockCount = removedStocks.size
        val removedStockAmount = removedStocks.sumOf { code ->
            previousMap[code]?.sumOf { it.evaluationAmount } ?: 0L
        }

        // Calculate weight changes
        val commonStocks = currentMap.keys.intersect(previousMap.keys)
        var increasedCount = 0
        var increasedAmount = 0L
        var decreasedCount = 0
        var decreasedAmount = 0L

        for (code in commonStocks) {
            val currentWeight = currentMap[code]?.sumOf { it.weight } ?: 0.0
            val previousWeight = previousMap[code]?.sumOf { it.weight } ?: 0.0
            val currentAmt = currentMap[code]?.sumOf { it.evaluationAmount } ?: 0L

            when {
                currentWeight - previousWeight > WEIGHT_CHANGE_THRESHOLD -> {
                    increasedCount++
                    increasedAmount += currentAmt
                }
                previousWeight - currentWeight > WEIGHT_CHANGE_THRESHOLD -> {
                    decreasedCount++
                    decreasedAmount += currentAmt
                }
            }
        }

        // Calculate cash deposit
        val currentCash = currentConstituents
            .filter { isCashDeposit(it.stockName) }
            .sumOf { it.evaluationAmount }

        val previousCash = previousConstituents
            .filter { isCashDeposit(it.stockName) }
            .sumOf { it.evaluationAmount }

        val cashChange = currentCash - previousCash
        val cashChangeRate = if (previousCash > 0) {
            (cashChange.toDouble() / previousCash) * 100
        } else {
            0.0
        }

        // Calculate totals
        val totalEtfCount = currentConstituents.map { it.etfCode }.distinct().size
        val totalHoldingAmount = currentConstituents.sumOf { it.evaluationAmount }

        val entity = DailyEtfStatisticsEntity(
            date = date,
            newStockCount = newStockCount,
            newStockAmount = newStockAmount,
            removedStockCount = removedStockCount,
            removedStockAmount = removedStockAmount,
            increasedStockCount = increasedCount,
            increasedStockAmount = increasedAmount,
            decreasedStockCount = decreasedCount,
            decreasedStockAmount = decreasedAmount,
            cashDepositAmount = currentCash,
            cashDepositChange = cashChange,
            cashDepositChangeRate = cashChangeRate,
            totalEtfCount = totalEtfCount,
            totalHoldingAmount = totalHoldingAmount,
            calculatedAt = System.currentTimeMillis()
        )

        statisticsDao.insert(entity)
    }

    override suspend fun getDailyStatistics(date: String): Result<DailyEtfStatistics?> = runCatching {
        statisticsDao.getByDate(date)?.toDomain()
    }

    override suspend fun getDailyStatisticsInRange(
        startDate: String,
        endDate: String
    ): Result<List<DailyEtfStatistics>> = runCatching {
        statisticsDao.getInRange(startDate, endDate).map { it.toDomain() }
    }

    // ==================== Statistics Helper Methods ====================

    private fun determineHoldingStatus(currentWeight: Double, previousWeight: Double): HoldingStatus {
        return when {
            previousWeight == 0.0 && currentWeight > 0.0 -> HoldingStatus.NEW
            currentWeight == 0.0 && previousWeight > 0.0 -> HoldingStatus.REMOVED
            currentWeight - previousWeight > WEIGHT_CHANGE_THRESHOLD -> HoldingStatus.INCREASE
            previousWeight - currentWeight > WEIGHT_CHANGE_THRESHOLD -> HoldingStatus.DECREASE
            else -> HoldingStatus.MAINTAIN
        }
    }

    private fun isCashDeposit(stockName: String): Boolean {
        val lowerName = stockName.lowercase()
        return lowerName.contains("원화예금") ||
            lowerName.contains("현금") ||
            lowerName.contains("cash") ||
            lowerName.contains("예금") ||
            lowerName.contains("krw")
    }

    private fun DailyEtfStatisticsEntity.toDomain() = DailyEtfStatistics(
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

    // ==================== Theme List (Phase 3) ====================

    override suspend fun getActiveEtfSummaries(): Result<List<com.stockapp.feature.etf.domain.model.ActiveEtfSummary>> =
        runCatching {
            val latestDate = constituentDao.getLatestDate()
                ?: throw IllegalStateException("No data available")

            val summaries = constituentDao.getActiveEtfSummaries(latestDate)

            // Get ETF info for additional details
            summaries.map { summary ->
                val etfInfo = etfDao.getByCode(summary.etfCode)
                com.stockapp.feature.etf.domain.model.ActiveEtfSummary(
                    etfCode = summary.etfCode,
                    etfName = summary.etfName,
                    etfType = etfInfo?.let { EtfType.fromValue(it.etfType) } ?: EtfType.ACTIVE,
                    managementCompany = etfInfo?.managementCompany ?: "",
                    constituentCount = summary.constituentCount,
                    totalEvaluationAmount = summary.totalAmount,
                    latestCollectedDate = latestDate
                )
            }
        }

    override suspend fun searchActiveEtfs(query: String): Result<List<com.stockapp.feature.etf.domain.model.ActiveEtfSummary>> =
        runCatching {
            val latestDate = constituentDao.getLatestDate()
                ?: throw IllegalStateException("No data available")

            val summaries = constituentDao.searchActiveEtfs(latestDate, query)

            summaries.map { summary ->
                val etfInfo = etfDao.getByCode(summary.etfCode)
                com.stockapp.feature.etf.domain.model.ActiveEtfSummary(
                    etfCode = summary.etfCode,
                    etfName = summary.etfName,
                    etfType = etfInfo?.let { EtfType.fromValue(it.etfType) } ?: EtfType.ACTIVE,
                    managementCompany = etfInfo?.managementCompany ?: "",
                    constituentCount = summary.constituentCount,
                    totalEvaluationAmount = summary.totalAmount,
                    latestCollectedDate = latestDate
                )
            }
        }

    override suspend fun getEtfDetail(etfCode: String): Result<com.stockapp.feature.etf.domain.model.EtfDetailInfo> =
        runCatching {
            val latestDate = constituentDao.getLatestDate()
                ?: throw IllegalStateException("No data available")

            val constituents = constituentDao.getByEtfAndDate(etfCode, latestDate)
            if (constituents.isEmpty()) {
                throw IllegalStateException("ETF not found: $etfCode")
            }

            val etfInfo = etfDao.getByCode(etfCode)
            val totalAmount = constituents.sumOf { it.evaluationAmount }

            com.stockapp.feature.etf.domain.model.EtfDetailInfo(
                etfCode = etfCode,
                etfName = constituents.first().etfName,
                etfType = etfInfo?.let { EtfType.fromValue(it.etfType) } ?: EtfType.ACTIVE,
                managementCompany = etfInfo?.managementCompany ?: "",
                constituents = constituents.map { it.toDomain() },
                totalEvaluationAmount = totalAmount,
                collectedDate = latestDate
            )
        }

    companion object {
        private const val WEIGHT_CHANGE_THRESHOLD = 0.01
    }
}
