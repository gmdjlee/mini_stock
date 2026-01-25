package com.stockapp.feature.etf.data.repo

import com.stockapp.core.db.dao.EtfCollectionHistoryDao
import com.stockapp.core.db.dao.EtfConstituentDao
import com.stockapp.core.db.dao.EtfDao
import com.stockapp.core.db.dao.EtfKeywordDao
import com.stockapp.core.db.entity.EtfCollectionHistoryEntity
import com.stockapp.core.db.entity.EtfConstituentEntity
import com.stockapp.core.db.entity.EtfEntity
import com.stockapp.core.db.entity.EtfKeywordEntity
import com.stockapp.feature.etf.domain.model.AmountHistory
import com.stockapp.feature.etf.domain.model.CollectionHistory
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfConstituent
import com.stockapp.feature.etf.domain.model.EtfDateRange
import com.stockapp.feature.etf.domain.model.EtfInfo
import com.stockapp.feature.etf.domain.model.EtfKeyword
import com.stockapp.feature.etf.domain.model.EtfType
import com.stockapp.feature.etf.domain.model.FilterType
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
    private val historyDao: EtfCollectionHistoryDao
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
}
