package com.stockapp.feature.etf.data.repo

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KisApiClient
import com.stockapp.core.api.KisApiConfig
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.core.db.dao.DateRange
import com.stockapp.core.db.dao.EtfCollectionHistoryDao
import com.stockapp.core.db.dao.EtfConstituentDao
import com.stockapp.core.db.dao.EtfDao
import com.stockapp.core.db.dao.EtfKeywordDao
import com.stockapp.core.db.dao.StockAmountRanking
import com.stockapp.core.db.dao.StockChangeInfo
import com.stockapp.core.db.entity.EtfCollectionHistoryEntity
import com.stockapp.core.db.entity.EtfConstituentEntity
import com.stockapp.core.db.entity.EtfEntity
import com.stockapp.feature.etf.data.dto.EtfConstituentParams
import com.stockapp.feature.etf.data.dto.EtfConstituentResponse
import com.stockapp.feature.etf.data.dto.EtfListParams
import com.stockapp.feature.etf.data.dto.EtfListResponse
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.ConstituentStock
import com.stockapp.feature.etf.domain.model.EtfCollectionResult
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.model.EtfInfo
import com.stockapp.feature.etf.domain.model.EtfType
import com.stockapp.feature.etf.domain.model.FullCollectionResult
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EtfCollectorRepoImpl @Inject constructor(
    private val kiwoomApiClient: KiwoomApiClient,
    private val kisApiClient: KisApiClient,
    private val settingsRepo: SettingsRepo,
    private val etfDao: EtfDao,
    private val constituentDao: EtfConstituentDao,
    private val keywordDao: EtfKeywordDao,
    private val historyDao: EtfCollectionHistoryDao,
    private val json: Json
) : EtfCollectorRepo {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ============================================================
    // ETF List Operations
    // ============================================================

    override suspend fun fetchEtfList(): Result<List<EtfInfo>> {
        return try {
            val config = getKiwoomApiConfig()

            // Use callAllPages to fetch all ETFs across pages (연속조회)
            kiwoomApiClient.callAllPages(
                apiId = "ka40004",
                url = "/api/dostk/etf",
                body = EtfListParams().toRequestBody(),
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl,
                maxPages = 20
            ) { responseJson ->
                parseEtfListResponse(responseJson)
            }
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "fetchEtfList error", e)
            Result.failure(ApiError.ApiCallError(0, e.message ?: "알 수 없는 오류"))
        }
    }

    private fun parseEtfListResponse(responseJson: String): List<EtfInfo> {
        val response = json.decodeFromString<EtfListResponse>(responseJson)
        return response.items?.mapNotNull { item ->
            val code = item.stkCd?.trim() ?: return@mapNotNull null
            val name = item.stkNm?.trim() ?: return@mapNotNull null

            EtfInfo(
                etfCode = code,
                etfName = name,
                etfType = determineEtfType(name),
                managementCompany = item.mngmcomp ?: "",
                trackingIndex = item.traceIdexNm ?: "",
                assetClass = item.stkCls ?: "",
                totalAssets = 0.0,
                currentPrice = parsePrice(item.closePric),
                priceChange = parsePrice(item.predPre),
                priceChangeSign = item.preSig ?: "",
                priceChangeRate = parseRate(item.preRt)
            )
        } ?: emptyList()
    }

    private fun determineEtfType(name: String): EtfType {
        // Active ETF keywords
        val activeKeywords = listOf("액티브", "Active", "ACTIVE")
        return if (activeKeywords.any { name.contains(it, ignoreCase = true) }) {
            EtfType.ACTIVE
        } else {
            EtfType.PASSIVE
        }
    }

    override suspend fun getFilteredEtfs(): List<EtfEntity> = etfDao.getFilteredEtfs()

    override suspend fun getAllEtfs(): List<EtfEntity> = etfDao.getAllEtfs()

    override suspend fun updateEtfFilterStatus(etfCode: String, isFiltered: Boolean) {
        etfDao.updateFilterStatus(etfCode, isFiltered)
    }

    override suspend fun applyKeywordFilter(config: EtfFilterConfig): Int {
        val allEtfs = getAllEtfs()
        var filteredCount = 0

        allEtfs.forEach { etf ->
            val shouldInclude = shouldIncludeEtf(etf, config)
            if (shouldInclude != etf.isFiltered) {
                updateEtfFilterStatus(etf.etfCode, shouldInclude)
            }
            if (shouldInclude) filteredCount++
        }

        return filteredCount
    }

    override suspend fun saveEtfs(etfs: List<EtfEntity>) {
        etfDao.insertAll(etfs)
    }

    private fun shouldIncludeEtf(etf: EtfEntity, config: EtfFilterConfig): Boolean {
        val name = etf.etfName

        // Check Active only filter
        if (config.activeOnly && etf.etfType != "Active") {
            return false
        }

        // Check exclude keywords
        if (config.excludeKeywords.any { name.contains(it, ignoreCase = true) }) {
            return false
        }

        // Check include keywords (if specified, at least one must match)
        if (config.includeKeywords.isNotEmpty()) {
            return config.includeKeywords.any { name.contains(it, ignoreCase = true) }
        }

        return true
    }

    // ============================================================
    // Constituent Collection Operations
    // ============================================================

    override suspend fun collectEtfConstituents(
        etfCode: String,
        etfName: String
    ): Result<EtfCollectionResult> {
        return try {
            val kisConfig = getKisApiConfig()
                ?: return Result.failure(ApiError.NoApiKeyError("KIS API 키가 설정되지 않았습니다"))

            val params = EtfConstituentParams(etfCode)

            kisApiClient.get(
                trId = "FHKST121600C0",
                url = "/uapi/etfetn/v1/quotations/inquire-component-stock-price",
                queryParams = params.toQueryParams(),
                config = kisConfig
            ) { responseJson ->
                parseConstituentResponse(responseJson, etfCode, etfName)
            }
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "collectEtfConstituents error: $etfCode", e)
            Result.failure(ApiError.ApiCallError(0, e.message ?: "알 수 없는 오류"))
        }
    }

    private fun parseConstituentResponse(
        responseJson: String,
        etfCode: String,
        etfName: String
    ): EtfCollectionResult {
        val response = json.decodeFromString<EtfConstituentResponse>(responseJson)

        if (response.rtCd != "0") {
            throw ApiError.ApiCallError(
                response.rtCd?.toIntOrNull() ?: -1,
                response.msg1 ?: "API 오류"
            )
        }

        val constituents = response.output2?.mapNotNull { item ->
            val stockCode = item.stckShrnIscd?.trim() ?: return@mapNotNull null
            val stockName = item.htsKorIsnm?.trim() ?: return@mapNotNull null

            ConstituentStock(
                stockCode = stockCode,
                stockName = stockName,
                currentPrice = parsePrice(item.stckPrpr).toInt(),
                priceChange = parsePrice(item.prdyVrss).toInt(),
                priceChangeSign = item.prdyVrssSign ?: "",
                priceChangeRate = parseRate(item.prdyCtrt),
                volume = parseLong(item.acmlVol),
                tradingValue = parseLong(item.acmlTrPbmn),
                marketCap = parseLong(item.htsAvls),
                weight = parseRate(item.cmpWt),
                evaluationAmount = parseLong(item.etfVltnAmt)
            )
        } ?: emptyList()

        return EtfCollectionResult(
            etfCode = etfCode,
            etfName = etfName,
            constituents = constituents,
            collectedAt = LocalDateTime.now()
        )
    }

    override suspend fun collectAllFilteredEtfs(
        progressCallback: ((current: Int, total: Int) -> Unit)?
    ): FullCollectionResult {
        val startedAt = LocalDateTime.now()
        val collectedDate = LocalDate.now()
        val dateStr = collectedDate.format(dateFormat)

        // Start history record
        val historyId = historyDao.insert(
            EtfCollectionHistoryEntity(
                collectedDate = dateStr,
                totalEtfs = 0,
                totalConstituents = 0,
                status = "IN_PROGRESS",
                errorMessage = null,
                startedAt = System.currentTimeMillis(),
                completedAt = null
            )
        )

        val filteredEtfs = getFilteredEtfs()
        val total = filteredEtfs.size
        var successCount = 0
        var failedCount = 0
        var totalConstituents = 0
        val errors = mutableListOf<String>()

        filteredEtfs.forEachIndexed { index, etf ->
            progressCallback?.invoke(index + 1, total)

            val result = collectEtfConstituents(etf.etfCode, etf.etfName)

            result.fold(
                onSuccess = { collectionResult ->
                    // Convert to entities and save
                    val entities = collectionResult.constituents.map { stock ->
                        EtfConstituentEntity(
                            etfCode = etf.etfCode,
                            etfName = etf.etfName,
                            stockCode = stock.stockCode,
                            stockName = stock.stockName,
                            currentPrice = stock.currentPrice,
                            priceChange = stock.priceChange,
                            priceChangeSign = stock.priceChangeSign,
                            priceChangeRate = stock.priceChangeRate,
                            volume = stock.volume,
                            tradingValue = stock.tradingValue,
                            marketCap = stock.marketCap,
                            weight = stock.weight,
                            evaluationAmount = stock.evaluationAmount,
                            collectedDate = dateStr,
                            collectedAt = System.currentTimeMillis()
                        )
                    }
                    saveConstituents(entities)
                    totalConstituents += entities.size
                    successCount++
                },
                onFailure = { error ->
                    failedCount++
                    errors.add("${etf.etfCode}: ${error.message}")
                    Log.w(TAG, "Failed to collect ${etf.etfCode}: ${error.message}")
                }
            )

            // Rate limiting between ETF collections
            delay(COLLECTION_DELAY_MS)
        }

        val completedAt = LocalDateTime.now()
        val status = when {
            failedCount == 0 -> CollectionStatus.SUCCESS
            successCount == 0 -> CollectionStatus.FAILED
            else -> CollectionStatus.PARTIAL
        }

        // Update history
        historyDao.updateCompletion(
            id = historyId,
            status = status.value,
            totalEtfs = successCount,
            totalConstituents = totalConstituents,
            errorMessage = if (errors.isNotEmpty()) errors.joinToString("; ") else null,
            completedAt = System.currentTimeMillis()
        )

        return FullCollectionResult(
            collectedDate = collectedDate,
            totalEtfs = successCount,
            totalConstituents = totalConstituents,
            successCount = successCount,
            failedCount = failedCount,
            status = status,
            errorMessage = if (errors.isNotEmpty()) errors.joinToString("; ") else null,
            startedAt = startedAt,
            completedAt = completedAt
        )
    }

    override suspend fun saveConstituents(constituents: List<EtfConstituentEntity>) {
        constituentDao.insertAll(constituents)
    }

    // ============================================================
    // Statistics Queries
    // ============================================================

    override suspend fun getStockRanking(date: String, limit: Int): List<StockAmountRanking> {
        return constituentDao.getStockRankingByAmount(date, limit)
    }

    override suspend fun getNewlyIncludedStocks(
        today: String,
        yesterday: String
    ): List<StockChangeInfo> {
        return constituentDao.getNewlyIncludedStocks(today, yesterday)
    }

    override suspend fun getRemovedStocks(
        today: String,
        yesterday: String
    ): List<StockChangeInfo> {
        return constituentDao.getRemovedStocks(today, yesterday)
    }

    override suspend fun getWeightIncreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double
    ): List<StockChangeInfo> {
        return constituentDao.getWeightIncreasedStocks(today, yesterday, threshold)
    }

    override suspend fun getWeightDecreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double
    ): List<StockChangeInfo> {
        return constituentDao.getWeightDecreasedStocks(today, yesterday, threshold)
    }

    // ============================================================
    // Data Management
    // ============================================================

    override suspend fun getDataDateRange(): DateRange? {
        return constituentDao.getDataDateRange()
    }

    override suspend fun getLatestCollectionDate(): String? {
        return constituentDao.getLatestDate()
    }

    override suspend fun getPreviousCollectionDate(date: String): String? {
        return constituentDao.getPreviousDate(date)
    }

    override suspend fun deleteOldData(cutoffDate: String) {
        constituentDao.deleteOldData(cutoffDate)
    }

    override fun observeCollectionHistory(limit: Int): Flow<List<EtfCollectionHistoryEntity>> {
        return historyDao.observeRecentHistory(limit)
    }

    // ============================================================
    // Helper Functions
    // ============================================================

    private suspend fun getKiwoomApiConfig(): ApiConfig {
        val config = settingsRepo.getApiKeyConfig().first()
        if (!config.isValid()) {
            throw ApiError.NoApiKeyError()
        }
        val baseUrl = when (config.investmentMode) {
            InvestmentMode.MOCK -> "https://mockapi.kiwoom.com"
            InvestmentMode.PRODUCTION -> "https://api.kiwoom.com"
        }
        return ApiConfig(config.appKey, config.secretKey, baseUrl)
    }

    private suspend fun getKisApiConfig(): KisApiConfig? {
        val config = settingsRepo.getKisApiKeyConfig().first()
        if (!config.isValid()) {
            return null
        }
        return KisApiConfig(
            appKey = config.appKey,
            appSecret = config.appSecret,
            baseUrl = config.getBaseUrl()
        )
    }

    private fun parsePrice(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        return value.replace(",", "").replace("+", "").replace("-", "").trim()
            .toLongOrNull() ?: 0L
    }

    private fun parseLong(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        return value.replace(",", "").trim().toLongOrNull() ?: 0L
    }

    private fun parseRate(value: String?): Double {
        if (value.isNullOrBlank()) return 0.0
        return value.replace(",", "").replace("%", "").replace("+", "").trim()
            .toDoubleOrNull() ?: 0.0
    }

    private data class ApiConfig(
        val appKey: String,
        val secretKey: String,
        val baseUrl: String
    )

    companion object {
        private const val TAG = "EtfCollectorRepo"
        private const val COLLECTION_DELAY_MS = 500L
    }
}
