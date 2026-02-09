package com.stockapp.core.stock.data

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.core.config.AppConfig
import com.stockapp.core.db.dao.OhlcvCacheDao
import com.stockapp.core.db.entity.OhlcvCacheEntity
import com.stockapp.core.krx.KrxDataSource
import com.stockapp.core.stock.api.DailyOhlcvResponse
import com.stockapp.core.stock.api.MonthlyOhlcvResponse
import com.stockapp.core.stock.api.OhlcvData
import com.stockapp.core.stock.api.OhlcvItem
import com.stockapp.core.stock.api.OhlcvRequest
import com.stockapp.core.stock.api.StockApiEndpoints
import com.stockapp.core.stock.api.StockApiIds
import com.stockapp.core.stock.api.WeeklyOhlcvResponse
import com.stockapp.core.stock.calc.OhlcvResampler
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OhlcvService"

/**
 * Service for fetching OHLCV (Open, High, Low, Close, Volume) data.
 * Uses DB cache first, then KRX/Kiwoom API for missing data (incremental fetch).
 * Shared across Analysis, Indicator, and Market features.
 */
@Singleton
class OhlcvService @Inject constructor(
    private val apiClient: KiwoomApiClient,
    private val krxDataSource: KrxDataSource,
    private val settingsRepo: SettingsRepo,
    private val json: Json,
    private val ohlcvCacheDao: OhlcvCacheDao
) {
    /**
     * OHLCV period type.
     */
    enum class Period {
        DAILY,  // ka10081
        WEEKLY, // ka10082
        MONTHLY // ka10083
    }

    /**
     * Internal API config holder.
     */
    private data class ApiConfig(
        val appKey: String,
        val secretKey: String,
        val baseUrl: String
    )

    // Per-ticker mutex to prevent concurrent duplicate API calls
    private val tickerMutexes = ConcurrentHashMap<String, Mutex>()

    private fun getMutex(ticker: String): Mutex =
        tickerMutexes.getOrPut(ticker) { Mutex() }

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

    /**
     * Get OHLCV data for a stock.
     * Uses DB cache first, fetches only missing date ranges from API (incremental).
     *
     * @param ticker Stock ticker code
     * @param days Number of days to fetch
     * @param period OHLCV period (DAILY, WEEKLY, MONTHLY)
     * @return Result containing OhlcvData or error
     */
    suspend fun getOhlcv(
        ticker: String,
        days: Int = 180,
        period: Period = Period.DAILY
    ): Result<OhlcvData> {
        Log.d(TAG, "getOhlcv() ticker=$ticker, days=$days, period=$period")

        // For weekly/monthly: get daily data from cache, then resample
        if (period != Period.DAILY) {
            val multiplier = if (period == Period.WEEKLY) 7 else 31
            val dailyResult = getOhlcv(ticker, days * multiplier, Period.DAILY)
            return dailyResult.map { dailyData ->
                when (period) {
                    Period.WEEKLY -> resampleToWeekly(dailyData)
                    Period.MONTHLY -> resampleToMonthly(dailyData)
                    else -> dailyData
                }
            }
        }

        // Daily data: use DB cache with incremental fetch
        return getOhlcvWithCache(ticker, days)
    }

    /**
     * DB cache-first OHLCV fetch with incremental API call.
     * Uses per-ticker mutex to prevent concurrent duplicate API calls.
     */
    private suspend fun getOhlcvWithCache(ticker: String, days: Int): Result<OhlcvData> {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() + 30) // extra buffer for holidays
        val startDateStr = startDate.format(DATE_FORMAT_YYYYMMDD)
        val endDateStr = today.format(DATE_FORMAT_YYYYMMDD)

        // Step 1: Check DB cache
        val cachedCount = ohlcvCacheDao.countInRange(ticker, startDateStr, endDateStr)
        val expectedTradingDays = (days * AppConfig.OHLCV_CACHE_SUFFICIENCY_RATIO).toInt()

        if (cachedCount >= expectedTradingDays) {
            // Cache is sufficient
            val cached = ohlcvCacheDao.getByTickerAndDateRange(ticker, startDateStr, endDateStr)
            if (cached.isNotEmpty()) {
                Log.d(TAG, "getOhlcvWithCache() cache hit for $ticker ($cachedCount bars)")
                return Result.success(entitiesToOhlcvData(ticker, cached))
            }
        }

        // Step 2: Need API fetch - use mutex to prevent concurrent calls for same ticker
        return getMutex(ticker).withLock {
            // Re-check cache (another coroutine may have populated it)
            val recheckCount = ohlcvCacheDao.countInRange(ticker, startDateStr, endDateStr)
            if (recheckCount >= expectedTradingDays) {
                val cached = ohlcvCacheDao.getByTickerAndDateRange(ticker, startDateStr, endDateStr)
                if (cached.isNotEmpty()) {
                    Log.d(TAG, "getOhlcvWithCache() cache hit after mutex for $ticker")
                    return@withLock Result.success(entitiesToOhlcvData(ticker, cached))
                }
            }

            // Step 3: Determine fetch range (incremental)
            val latestCached = ohlcvCacheDao.getLatestDate(ticker)
            val fetchStartDate: String
            val fetchEndDate = endDateStr

            if (latestCached != null && latestCached >= startDateStr) {
                // Incremental: fetch from day after latest cached
                val nextDay = LocalDate.parse(latestCached, DATE_FORMAT_YYYYMMDD)
                    .plusDays(1)
                fetchStartDate = nextDay.format(DATE_FORMAT_YYYYMMDD)

                if (fetchStartDate > fetchEndDate) {
                    // All data up to today is already cached
                    val cached = ohlcvCacheDao.getByTickerAndDateRange(ticker, startDateStr, endDateStr)
                    Log.d(TAG, "getOhlcvWithCache() fully cached for $ticker")
                    return@withLock Result.success(entitiesToOhlcvData(ticker, cached))
                }

                Log.d(TAG, "getOhlcvWithCache() incremental fetch $ticker: $fetchStartDate~$fetchEndDate")
            } else {
                // Full fetch
                fetchStartDate = startDateStr
                Log.d(TAG, "getOhlcvWithCache() full fetch $ticker: $fetchStartDate~$fetchEndDate")
            }

            // Step 4: Fetch from KRX/Kiwoom
            val fetchResult = fetchDailyFromApi(ticker, fetchStartDate, fetchEndDate)

            if (fetchResult != null && fetchResult.dates.isNotEmpty()) {
                // Step 5: Save to DB cache
                val entities = ohlcvDataToEntities(fetchResult)
                if (entities.isNotEmpty()) {
                    ohlcvCacheDao.insertAll(entities)
                }
            }

            // Step 6: Read full range from DB
            val allData = ohlcvCacheDao.getByTickerAndDateRange(ticker, startDateStr, endDateStr)
            if (allData.isNotEmpty()) {
                Result.success(entitiesToOhlcvData(ticker, allData))
            } else if (fetchResult != null) {
                // DB might be empty if fetch returned data but couldn't save
                Result.success(fetchResult)
            } else {
                Result.failure(Exception("No OHLCV data available for $ticker"))
            }
        }
    }

    /**
     * Fetch daily OHLCV from API (KRX first, Kiwoom fallback).
     * Used internally by cache layer.
     */
    private suspend fun fetchDailyFromApi(
        ticker: String,
        startDate: String,
        endDate: String
    ): OhlcvData? {
        // Try KRX first
        val krxResult = fetchOhlcvFromKrx(ticker, startDate, endDate)
        if (krxResult != null && krxResult.dates.isNotEmpty()) {
            return krxResult
        }

        // Fallback to Kiwoom API
        return try {
            val config = getApiConfig()
            val request = OhlcvRequest(
                stkCd = ticker,
                baseDt = endDate
            )
            val result = fetchDailyOhlcv(ticker, request, config)
            result.getOrNull()
        } catch (e: ApiError) {
            Log.e(TAG, "fetchDailyFromApi() Kiwoom fallback failed: ${e.message}", e)
            null
        }
    }

    /**
     * Fetch OHLCV data from KRX (primary data source).
     * Returns null if KRX fails (allows fallback to Kiwoom API).
     */
    private suspend fun fetchOhlcvFromKrx(
        ticker: String,
        startDate: String,
        endDate: String
    ): OhlcvData? {
        return try {
            val result = krxDataSource.getOhlcvByTicker(startDate, endDate, ticker)
            result.getOrNull()?.let { history ->
                if (history.isEmpty()) return null
                OhlcvData(
                    ticker = ticker,
                    dates = history.map { it.date },
                    opens = history.map { it.open.toInt() },
                    highs = history.map { it.high.toInt() },
                    lows = history.map { it.low.toInt() },
                    closes = history.map { it.close.toInt() },
                    volumes = history.map { it.volume }
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "fetchOhlcvFromKrx() failed for $ticker: ${e.message}")
            null
        }
    }

    /**
     * Fetch daily OHLCV using ka10081 API.
     */
    private suspend fun fetchDailyOhlcv(
        ticker: String,
        request: OhlcvRequest,
        config: ApiConfig
    ): Result<OhlcvData> {
        return apiClient.call(
            apiId = StockApiIds.DAILY_CHART,
            url = StockApiEndpoints.DAILY_CHART,
            body = request.toRequestBody(),
            appKey = config.appKey,
            secretKey = config.secretKey,
            baseUrl = config.baseUrl
        ) { responseJson ->
            val response = json.decodeFromString<DailyOhlcvResponse>(responseJson)
            parseOhlcvResponse(ticker, response.data)
        }
    }

    /**
     * Parse OHLCV response items to OhlcvData.
     */
    private fun parseOhlcvResponse(ticker: String, items: List<OhlcvItem>?): OhlcvData {
        if (items.isNullOrEmpty()) {
            Log.w(TAG, "parseOhlcvResponse() empty data for ticker=$ticker")
            return OhlcvData(
                ticker = ticker,
                dates = emptyList(),
                opens = emptyList(),
                highs = emptyList(),
                lows = emptyList(),
                closes = emptyList(),
                volumes = emptyList()
            )
        }

        // Filter out invalid items and sort by date (newest first)
        val validItems = items
            .filter { item ->
                item.date != null &&
                    item.open != null && item.open > 0 &&
                    item.high != null && item.high > 0 &&
                    item.low != null && item.low > 0 &&
                    item.close != null && item.close > 0
            }
            .sortedByDescending { it.date }

        Log.d(TAG, "parseOhlcvResponse() ticker=$ticker, validItems=${validItems.size}")

        return OhlcvData(
            ticker = ticker,
            dates = validItems.map { it.date!! },
            opens = validItems.map { it.open!! },
            highs = validItems.map { it.high!! },
            lows = validItems.map { it.low!! },
            closes = validItems.map { it.close!! },
            volumes = validItems.map { it.volume ?: 0L }
        )
    }

    // ===== Conversion helpers =====

    private fun entitiesToOhlcvData(ticker: String, entities: List<OhlcvCacheEntity>): OhlcvData {
        return OhlcvData(
            ticker = ticker,
            dates = entities.map { it.date },
            opens = entities.map { it.open },
            highs = entities.map { it.high },
            lows = entities.map { it.low },
            closes = entities.map { it.close },
            volumes = entities.map { it.volume }
        )
    }

    private fun ohlcvDataToEntities(data: OhlcvData): List<OhlcvCacheEntity> {
        val now = System.currentTimeMillis()
        return data.dates.indices.map { i ->
            OhlcvCacheEntity(
                ticker = data.ticker,
                date = data.dates[i],
                open = data.opens[i],
                high = data.highs[i],
                low = data.lows[i],
                close = data.closes[i],
                volume = data.volumes[i],
                cachedAt = now
            )
        }
    }

    // ===== Resampling =====

    /**
     * Convert daily OHLCV data to weekly bars using resampling.
     */
    fun resampleToWeekly(dailyData: OhlcvData): OhlcvData {
        val bars = dailyData.toOhlcvBars()
        val weeklyBars = OhlcvResampler.toWeekly(bars)
        return ohlcvBarsToData(dailyData.ticker, weeklyBars)
    }

    /**
     * Convert daily OHLCV data to monthly bars using resampling.
     */
    fun resampleToMonthly(dailyData: OhlcvData): OhlcvData {
        val bars = dailyData.toOhlcvBars()
        val monthlyBars = OhlcvResampler.toMonthly(bars)
        return ohlcvBarsToData(dailyData.ticker, monthlyBars)
    }

    private fun OhlcvData.toOhlcvBars(): List<OhlcvResampler.OhlcvBar> {
        return dates.indices.map { i ->
            OhlcvResampler.OhlcvBar(
                date = dates[i],
                open = opens[i],
                high = highs[i],
                low = lows[i],
                close = closes[i],
                volume = volumes[i]
            )
        }
    }

    private fun ohlcvBarsToData(ticker: String, bars: List<OhlcvResampler.OhlcvBar>): OhlcvData {
        return OhlcvData(
            ticker = ticker,
            dates = bars.map { it.date },
            opens = bars.map { it.open },
            highs = bars.map { it.high },
            lows = bars.map { it.low },
            closes = bars.map { it.close },
            volumes = bars.map { it.volume }
        )
    }

    companion object {
        private val DATE_FORMAT_YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
