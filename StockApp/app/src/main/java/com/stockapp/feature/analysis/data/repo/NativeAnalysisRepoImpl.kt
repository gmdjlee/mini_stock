package com.stockapp.feature.analysis.data.repo

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.entity.AnalysisCacheEntity
import com.stockapp.core.stock.api.InvestorTrendRequest
import com.stockapp.core.stock.api.InvestorTrendResponse
import com.stockapp.core.stock.api.OhlcvData
import com.stockapp.core.stock.api.StockApiEndpoints
import com.stockapp.core.stock.api.StockApiIds
import com.stockapp.core.stock.api.StockInfoRequest
import com.stockapp.core.stock.api.StockInfoResponse
import com.stockapp.core.stock.calc.MathUtil
import com.stockapp.core.stock.data.OhlcvService
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
    private val ohlcvService: OhlcvService,
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

            // Step 2.5: Fetch OHLCV data for daily market cap calculation
            // This ensures market cap varies with stock price, avoiding flat lines
            val ohlcvData = fetchOhlcv(ticker, days)

            // Step 3: Build StockData from responses
            val stockData = buildStockData(ticker, stockInfo, investorTrend, ohlcvData)

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
            // flo_stk is in 천주 (thousands of shares), convert to actual shares
            val floStk = response.floStk.toLongSafe()
            val floatingShares = if (floStk > 0) floStk * 1000 else 0L
            StockInfo(
                name = response.stkNm ?: ticker,
                marketCap = response.mac.toLongSafe(), // mac is in 억원, may have sign prefix
                floatingShares = floatingShares // actual number of shares
            )
        }
    }

    /**
     * Fetch OHLCV data for daily market cap calculation.
     * Returns null if fetch fails (allows fallback to API market cap).
     */
    private suspend fun fetchOhlcv(
        ticker: String,
        days: Int
    ): OhlcvData? {
        return try {
            ohlcvService.getOhlcv(ticker, days, OhlcvService.Period.DAILY).getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "fetchOhlcv() failed for ticker=$ticker: ${e.message}")
            null
        }
    }

    /**
     * Safely convert a string value to Long.
     * Handles sign prefixes like "+117500" from Kiwoom API.
     */
    private fun String?.toLongSafe(): Long =
        this?.removePrefix("+")?.toLongOrNull() ?: 0L

    /**
     * Fetch investor trend data using ka10059 API.
     *
     * Note: The ka10059 API requires a single date (dt) parameter and returns
     * historical data in the response. This matches the Python implementation.
     */
    private suspend fun fetchInvestorTrend(
        ticker: String,
        days: Int,
        config: ApiConfig
    ): Result<List<InvestorTrendData>> {
        // Use today's date as the base date (API returns historical data in response)
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        val request = InvestorTrendRequest(
            stkCd = ticker,
            dt = today.format(dateFormatter)
        )

        Log.d(TAG, "fetchInvestorTrend() ticker=$ticker, dt=${request.dt}")

        return apiClient.call(
            apiId = StockApiIds.INVESTOR_TREND,
            url = StockApiEndpoints.INVESTOR_TREND,
            body = request.toRequestBody(),
            appKey = config.appKey,
            secretKey = config.secretKey,
            baseUrl = config.baseUrl
        ) { responseJson ->
            Log.d(TAG, "fetchInvestorTrend() response (first 500): ${responseJson.take(500)}")
            val response = json.decodeFromString<InvestorTrendResponse>(responseJson)

            // API returns list sorted by date (newest first), take only required days
            val allData = response.data?.mapNotNull { item ->
                if (item.date == null) return@mapNotNull null

                InvestorTrendData(
                    date = item.date,
                    foreignNet = item.foreignNet ?: 0L,
                    institutionNet = item.institutionNet ?: 0L,
                    individualNet = item.individualNet ?: 0L,
                    marketCap = item.marketCap ?: 0L // marketCap is in 백만원
                )
            }?.sortedByDescending { it.date } ?: emptyList()

            // Take only the required number of days
            allData.take(days)
        }
    }

    /**
     * Build StockData from API responses.
     *
     * Key calculations:
     * - for5d: 5-day rolling sum of foreign net buying
     * - ins5d: 5-day rolling sum of institution net buying
     * - mcap: Market cap calculated from OHLCV close prices × floating shares (Python parity)
     *
     * Market cap calculation (matching Python analysis.py):
     * - Primary: shares × close_price[date] for each date
     * - Fallback: API's mrkt_tot_amt or stock info's mac
     */
    private fun buildStockData(
        ticker: String,
        stockInfo: StockInfo,
        investorTrend: List<InvestorTrendData>,
        ohlcvData: OhlcvData?
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

        // Build date -> close price map from OHLCV data
        // Normalize OHLCV dates to match investor trend format (YYYYMMDD)
        val dateToClose: Map<String, Int> = ohlcvData?.let { ohlcv ->
            ohlcv.dates.zip(ohlcv.closes).toMap()
        } ?: emptyMap()

        val shares = stockInfo.floatingShares

        Log.d(TAG, "buildStockData() ticker=$ticker, shares=$shares, ohlcvDates=${dateToClose.size}")

        // Extract data from investor trend (newest-first order)
        val dates = investorTrend.map { it.date }
        val foreignNet = investorTrend.map { it.foreignNet }
        val institutionNet = investorTrend.map { it.institutionNet }

        // Calculate daily market cap from OHLCV close prices (matching Python logic)
        // This ensures market cap varies with stock price, avoiding flat lines
        val mcap = investorTrend.mapIndexed { idx, trendItem ->
            val date = trendItem.date
            val closePrice = dateToClose[date]

            if (shares > 0 && closePrice != null && closePrice > 0) {
                // Primary: shares × close_price (matching Python analysis.py line 134-135)
                shares * closePrice.toLong()
            } else if (trendItem.marketCap > 0) {
                // Fallback 1: API's mrkt_tot_amt (in 백만원)
                trendItem.marketCap * 1_000_000
            } else {
                // Fallback 2: stock info mac (in 억원)
                stockInfo.marketCap * 100_000_000
            }
        }

        // Calculate 5-day rolling sums (newest-first order maintained)
        val for5d = MathUtil.rollingSum(foreignNet, 5)
        val ins5d = MathUtil.rollingSum(institutionNet, 5)

        Log.d(TAG, "buildStockData() ticker=$ticker, dates=${dates.size}, " +
            "latestMcap=${mcap.firstOrNull()}, latestFor5d=${for5d.firstOrNull()}, " +
            "usedOhlcv=${shares > 0 && dateToClose.isNotEmpty()}")

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
        val marketCap: Long, // in 억원
        val floatingShares: Long = 0L // actual number of shares (converted from 천주)
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
