package com.stockapp.core.stock.data

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
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
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OhlcvService"

/**
 * Service for fetching OHLCV (Open, High, Low, Close, Volume) data.
 * Uses Kiwoom REST API directly via KiwoomApiClient.
 */
@Singleton
class OhlcvService @Inject constructor(
    private val apiClient: KiwoomApiClient,
    private val settingsRepo: SettingsRepo,
    private val json: Json
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

        return try {
            val config = getApiConfig()

            // API uses base_dt (base date) - returns data from this date backwards
            // Note: days parameter is not used by API but kept for interface compatibility
            val baseDate = LocalDate.now()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

            val request = OhlcvRequest(
                stkCd = ticker,
                baseDt = baseDate.format(dateFormatter)
            )

            when (period) {
                Period.DAILY -> fetchDailyOhlcv(ticker, request, config)
                Period.WEEKLY -> fetchWeeklyOhlcv(ticker, request, config)
                Period.MONTHLY -> fetchMonthlyOhlcv(ticker, request, config)
            }
        } catch (e: ApiError) {
            Log.e(TAG, "getOhlcv() failed: ${e.message}", e)
            Result.failure(e)
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
     * Fetch weekly OHLCV using ka10082 API.
     */
    private suspend fun fetchWeeklyOhlcv(
        ticker: String,
        request: OhlcvRequest,
        config: ApiConfig
    ): Result<OhlcvData> {
        return apiClient.call(
            apiId = StockApiIds.WEEKLY_CHART,
            url = StockApiEndpoints.WEEKLY_CHART,
            body = request.toRequestBody(),
            appKey = config.appKey,
            secretKey = config.secretKey,
            baseUrl = config.baseUrl
        ) { responseJson ->
            val response = json.decodeFromString<WeeklyOhlcvResponse>(responseJson)
            parseOhlcvResponse(ticker, response.data)
        }
    }

    /**
     * Fetch monthly OHLCV using ka10083 API.
     */
    private suspend fun fetchMonthlyOhlcv(
        ticker: String,
        request: OhlcvRequest,
        config: ApiConfig
    ): Result<OhlcvData> {
        return apiClient.call(
            apiId = StockApiIds.MONTHLY_CHART,
            url = StockApiEndpoints.MONTHLY_CHART,
            body = request.toRequestBody(),
            appKey = config.appKey,
            secretKey = config.secretKey,
            baseUrl = config.baseUrl
        ) { responseJson ->
            val response = json.decodeFromString<MonthlyOhlcvResponse>(responseJson)
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

    /**
     * Convert daily OHLCV data to weekly bars using resampling.
     *
     * @param dailyData Daily OHLCV data
     * @return Weekly OHLCV data
     */
    fun resampleToWeekly(dailyData: OhlcvData): OhlcvData {
        val bars = dailyData.toOhlcvBars()
        val weeklyBars = OhlcvResampler.toWeekly(bars)
        return ohlcvBarsToData(dailyData.ticker, weeklyBars)
    }

    /**
     * Convert daily OHLCV data to monthly bars using resampling.
     *
     * @param dailyData Daily OHLCV data
     * @return Monthly OHLCV data
     */
    fun resampleToMonthly(dailyData: OhlcvData): OhlcvData {
        val bars = dailyData.toOhlcvBars()
        val monthlyBars = OhlcvResampler.toMonthly(bars)
        return ohlcvBarsToData(dailyData.ticker, monthlyBars)
    }

    /**
     * Convert OhlcvData to list of OhlcvBar for resampling.
     */
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

    /**
     * Convert list of OhlcvBar to OhlcvData.
     */
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
}
