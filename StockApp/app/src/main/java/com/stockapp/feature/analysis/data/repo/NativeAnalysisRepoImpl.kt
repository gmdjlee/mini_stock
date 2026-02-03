package com.stockapp.feature.analysis.data.repo

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.entity.AnalysisCacheEntity
import com.stockapp.core.stock.api.InvestorTrendRequest
import com.stockapp.core.stock.api.InvestorTrendResponse
import com.stockapp.core.stock.api.StockApiEndpoints
import com.stockapp.core.stock.api.StockApiIds
import com.stockapp.core.stock.api.StockInfoRequest
import com.stockapp.core.stock.api.StockInfoResponse
import com.stockapp.core.stock.calc.MathUtil
import com.stockapp.feature.analysis.domain.model.StockData
import com.stockapp.feature.analysis.domain.repo.AnalysisRepo
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NativeAnalysisRepoImpl"

/**
 * Native Kotlin implementation of AnalysisRepo.
 * Uses KiwoomApiClient directly instead of Python/Chaquopy.
 *
 * API calls:
 * - ka10059: Investor trend (투자자별 매매) - foreign, institution, individual net buying
 * - ka10001: Stock basic info (주식 기본정보) - name, market cap
 */
@Singleton
class NativeAnalysisRepoImpl @Inject constructor(
    private val apiClient: KiwoomApiClient,
    private val settingsRepo: SettingsRepo,
    private val cacheDao: AnalysisCacheDao,
    private val json: Json
) : AnalysisRepo {

    /**
     * Internal API config holder.
     */
    private data class ApiConfig(
        val appKey: String,
        val secretKey: String,
        val baseUrl: String
    )

    /**
     * Get API configuration from settings.
     */
    private suspend fun getApiConfig(): ApiConfig {
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

    override suspend fun getAnalysis(
        ticker: String,
        days: Int,
        useCache: Boolean
    ): Result<StockData> {
        Log.d(TAG, "getAnalysis() ticker=$ticker, days=$days, useCache=$useCache")

        // Check cache first if enabled
        if (useCache) {
            getCachedAnalysis(ticker)?.let { cached ->
                Log.d(TAG, "getAnalysis() returning cached data for ticker=$ticker")
                return Result.success(cached)
            }
        }

        return try {
            val config = getApiConfig()

            // Step 1: Fetch stock basic info (ka10001) for name
            val stockInfoResult = fetchStockInfo(ticker, config)
            val stockInfo = stockInfoResult.getOrElse { error ->
                return Result.failure(error)
            }

            // Step 2: Fetch investor trend data (ka10059)
            val investorTrendResult = fetchInvestorTrend(ticker, days, config)
            val investorTrend = investorTrendResult.getOrElse { error ->
                return Result.failure(error)
            }

            // Step 3: Build StockData from responses
            val stockData = buildStockData(ticker, stockInfo, investorTrend)

            // Step 4: Cache the result
            cacheAnalysis(ticker, stockData)

            Log.d(TAG, "getAnalysis() success for ticker=$ticker, dates=${stockData.dates.size}")
            Result.success(stockData)
        } catch (e: ApiError) {
            Log.e(TAG, "getAnalysis() failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "getAnalysis() unexpected error: ${e.message}", e)
            Result.failure(ApiError.ApiCallError(0, e.message ?: "알 수 없는 오류"))
        }
    }

    override suspend fun getCachedAnalysis(ticker: String): StockData? {
        val cached = cacheDao.get(ticker) ?: return null

        // Check if cache is expired (24 hours)
        val now = System.currentTimeMillis()
        if (now - cached.cachedAt > AppDb.ANALYSIS_CACHE_TTL) {
            val ageMinutes = (now - cached.cachedAt) / 1000 / 60
            Log.d(TAG, "Cache expired for ticker=$ticker, age=${ageMinutes}min")
            cacheDao.delete(ticker)
            return null
        }

        return try {
            json.decodeFromString<CachedStockData>(cached.data).toDomain()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse cached analysis for ticker=$ticker", e)
            cacheDao.delete(ticker)
            null
        }
    }

    override suspend fun clearCache(ticker: String) {
        cacheDao.delete(ticker)
    }

    override suspend fun clearAllCache() {
        cacheDao.deleteAll()
    }

    /**
     * Fetch stock basic info using ka10001 API.
     */
    private suspend fun fetchStockInfo(
        ticker: String,
        config: ApiConfig
    ): Result<StockInfo> {
        val request = StockInfoRequest(stkCd = ticker)

        return apiClient.call(
            apiId = StockApiIds.STOCK_INFO,
            url = StockApiEndpoints.STOCK_INFO,
            body = request.toRequestBody(),
            appKey = config.appKey,
            secretKey = config.secretKey,
            baseUrl = config.baseUrl
        ) { responseJson ->
            val response = json.decodeFromString<StockInfoResponse>(responseJson)
            StockInfo(
                name = response.stkNm ?: ticker,
                marketCap = response.mac ?: 0L // mac is in 억원
            )
        }
    }

    /**
     * Fetch investor trend data using ka10059 API.
     */
    private suspend fun fetchInvestorTrend(
        ticker: String,
        days: Int,
        config: ApiConfig
    ): Result<List<InvestorTrendData>> {
        // Calculate date range
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong())
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        val request = InvestorTrendRequest(
            stkCd = ticker,
            inqrStrtDt = startDate.format(dateFormatter),
            inqrEndDt = endDate.format(dateFormatter)
        )

        return apiClient.call(
            apiId = StockApiIds.INVESTOR_TREND,
            url = StockApiEndpoints.INVESTOR_TREND,
            body = request.toRequestBody(),
            appKey = config.appKey,
            secretKey = config.secretKey,
            baseUrl = config.baseUrl
        ) { responseJson ->
            val response = json.decodeFromString<InvestorTrendResponse>(responseJson)

            response.data?.mapNotNull { item ->
                if (item.date == null) return@mapNotNull null

                InvestorTrendData(
                    date = item.date,
                    foreignNet = item.foreignNet ?: 0L,
                    institutionNet = item.institutionNet ?: 0L,
                    individualNet = item.individualNet ?: 0L,
                    marketCap = item.marketCap ?: 0L // marketCap is in 백만원
                )
            }?.sortedByDescending { it.date } ?: emptyList()
        }
    }

    /**
     * Build StockData from API responses.
     *
     * Key calculations:
     * - for5d: 5-day rolling sum of foreign net buying
     * - ins5d: 5-day rolling sum of institution net buying
     * - mcap: Market cap from investor trend (preferred) or stock info
     */
    private fun buildStockData(
        ticker: String,
        stockInfo: StockInfo,
        investorTrend: List<InvestorTrendData>
    ): StockData {
        if (investorTrend.isEmpty()) {
            return StockData(
                ticker = ticker,
                name = stockInfo.name,
                dates = emptyList(),
                mcap = emptyList(),
                for5d = emptyList(),
                ins5d = emptyList()
            )
        }

        // Extract data from investor trend (newest-first order)
        val dates = investorTrend.map { it.date }
        val foreignNet = investorTrend.map { it.foreignNet }
        val institutionNet = investorTrend.map { it.institutionNet }

        // Market cap: use from investor trend (in 백만원), convert to 원 for consistency
        // Note: ka10059 returns marketCap in 백만원, so multiply by 1,000,000
        val mcap = investorTrend.map { trendItem ->
            if (trendItem.marketCap > 0) {
                trendItem.marketCap * 1_000_000 // 백만원 → 원
            } else {
                // Fallback to stock info mac (in 억원)
                stockInfo.marketCap * 100_000_000 // 억원 → 원
            }
        }

        // Calculate 5-day rolling sums (newest-first order maintained)
        val for5d = MathUtil.rollingSum(foreignNet, 5)
        val ins5d = MathUtil.rollingSum(institutionNet, 5)

        Log.d(TAG, "buildStockData() ticker=$ticker, dates=${dates.size}, " +
            "latestMcap=${mcap.firstOrNull()}, latestFor5d=${for5d.firstOrNull()}")

        return StockData(
            ticker = ticker,
            name = stockInfo.name,
            dates = dates,
            mcap = mcap,
            for5d = for5d,
            ins5d = ins5d
        )
    }

    /**
     * Cache analysis data.
     */
    private suspend fun cacheAnalysis(ticker: String, data: StockData) {
        try {
            val cachedData = CachedStockData.fromDomain(data)
            val entity = AnalysisCacheEntity(
                ticker = ticker,
                data = json.encodeToString(cachedData),
                startDate = data.dates.lastOrNull() ?: "", // Oldest date (dates are newest-first)
                endDate = data.dates.firstOrNull() ?: "", // Newest date
                cachedAt = System.currentTimeMillis()
            )
            cacheDao.insert(entity)
            Log.d(TAG, "cacheAnalysis() cached data for ticker=$ticker")
        } catch (e: Exception) {
            Log.w(TAG, "cacheAnalysis() failed to cache: ${e.message}", e)
        }
    }

    /**
     * Internal data class for stock info.
     */
    private data class StockInfo(
        val name: String,
        val marketCap: Long // in 억원
    )

    /**
     * Internal data class for investor trend data.
     */
    private data class InvestorTrendData(
        val date: String,
        val foreignNet: Long,
        val institutionNet: Long,
        val individualNet: Long,
        val marketCap: Long // in 백만원
    )
}
