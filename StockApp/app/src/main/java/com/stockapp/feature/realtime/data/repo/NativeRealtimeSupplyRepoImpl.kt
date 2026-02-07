package com.stockapp.feature.realtime.data.repo

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.core.db.dao.RealtimeSupplyCacheDao
import com.stockapp.core.db.entity.RealtimeSupplyCacheEntity
import com.stockapp.core.stock.api.RealtimeSupplyRequest
import com.stockapp.core.stock.api.RealtimeSupplyResponse
import com.stockapp.core.stock.api.StockApiEndpoints
import com.stockapp.core.stock.api.StockApiIds
import com.stockapp.core.stock.api.StockInfoRequest
import com.stockapp.core.stock.api.StockInfoResponse
import com.stockapp.feature.realtime.domain.model.CachedRealtimeSupplyData
import com.stockapp.feature.realtime.domain.model.RealtimeSupplyData
import com.stockapp.feature.realtime.domain.repo.RealtimeSupplyRepo
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NativeRealtimeSupplyRepo"

/**
 * Native Kotlin implementation of RealtimeSupplyRepo.
 * Uses KiwoomApiClient directly to call ka10063 API for realtime investor trend.
 *
 * API calls:
 * - ka10063: Realtime investor trend (장중 투자자별 매매)
 * - ka10001: Stock basic info (for stock name)
 */
@Singleton
class NativeRealtimeSupplyRepoImpl @Inject constructor(
    private val apiClient: KiwoomApiClient,
    private val settingsRepo: SettingsRepo,
    private val cacheDao: RealtimeSupplyCacheDao,
    private val json: Json
) : RealtimeSupplyRepo {

    /**
     * Internal API config holder.
     */
    private data class ApiConfig(
        val appKey: String,
        val secretKey: String,
        val baseUrl: String,
        val investmentMode: InvestmentMode
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
        return ApiConfig(config.appKey, config.secretKey, baseUrl, config.investmentMode)
    }

    override suspend fun getRealtimeSupply(
        ticker: String,
        useCache: Boolean
    ): Result<RealtimeSupplyData> {
        Log.d(TAG, "getRealtimeSupply() ticker=$ticker, useCache=$useCache")

        // Check cache first if enabled
        if (useCache) {
            getCachedSupply(ticker)?.let { cached ->
                Log.d(TAG, "getRealtimeSupply() returning cached data for ticker=$ticker")
                return Result.success(cached)
            }
        }

        return try {
            val config = getApiConfig()

            // Step 1: Fetch stock name (ka10001)
            val stockNameResult = fetchStockName(ticker, config)
            val stockName = stockNameResult.getOrElse { error ->
                Log.w(TAG, "Failed to fetch stock name, using ticker: $error")
                ticker // Fallback to ticker if name fetch fails
            }

            // Step 2: Fetch realtime supply data (ka10063)
            val realtimeResult = fetchRealtimeSupply(ticker, config)
            val realtimeData = realtimeResult.getOrElse { error ->
                return Result.failure(error)
            }

            // Step 3: Build domain model
            val supplyData = buildRealtimeSupplyData(ticker, stockName, realtimeData)

            // Step 4: Cache the result
            cacheSupply(supplyData)

            Log.d(TAG, "getRealtimeSupply() success for ticker=$ticker, " +
                "netBuyAmount=${supplyData.netBuyAmount}, volume=${supplyData.accumulatedVolume}")
            Result.success(supplyData)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiError) {
            Log.e(TAG, "getRealtimeSupply() failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "getRealtimeSupply() unexpected error: ${e.message}", e)
            Result.failure(ApiError.ApiCallError(0, e.message ?: "알 수 없는 오류"))
        }
    }

    override suspend fun getCachedSupply(ticker: String): RealtimeSupplyData? {
        val cached = cacheDao.get(ticker) ?: return null

        // Check if cache is expired (using shorter TTL for realtime data)
        val now = System.currentTimeMillis()
        if (now - cached.cachedAt > RealtimeSupplyRepo.DEFAULT_CACHE_TTL_MS) {
            val ageSeconds = (now - cached.cachedAt) / 1000
            Log.d(TAG, "Cache expired for ticker=$ticker, age=${ageSeconds}s")
            cacheDao.delete(ticker)
            return null
        }

        return try {
            json.decodeFromString<CachedRealtimeSupplyData>(cached.data).toDomain()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse cached realtime supply for ticker=$ticker", e)
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
     * Fetch stock name using ka10001 API.
     */
    private suspend fun fetchStockName(
        ticker: String,
        config: ApiConfig
    ): Result<String> {
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
            response.stkNm ?: ticker
        }
    }

    /**
     * Fetch realtime supply data using ka10063 API.
     */
    private suspend fun fetchRealtimeSupply(
        ticker: String,
        config: ApiConfig
    ): Result<RealtimeSupplyResponse> {
        // Determine stexTp based on investment mode
        val stexTp = when (config.investmentMode) {
            InvestmentMode.MOCK -> "3"    // KRX (모의)
            InvestmentMode.PRODUCTION -> "1" // KRX (실전)
        }

        val request = RealtimeSupplyRequest(
            stkCd = ticker,
            mrktTp = "000",    // 전체
            invsr = "6",       // 전체 투자자
            stexTp = stexTp,
            amtQtyTp = "1"     // 금액
        )

        return apiClient.call(
            apiId = StockApiIds.REALTIME_SUPPLY,
            url = StockApiEndpoints.REALTIME_SUPPLY,
            body = request.toRequestBody(),
            appKey = config.appKey,
            secretKey = config.secretKey,
            baseUrl = config.baseUrl
        ) { responseJson ->
            json.decodeFromString<RealtimeSupplyResponse>(responseJson)
        }
    }

    /**
     * Build RealtimeSupplyData from API response.
     * API returns data as LIST under 'opmr_invsr_trde' key.
     * We need to find the matching item by ticker or use the first item if only one exists.
     */
    private fun buildRealtimeSupplyData(
        ticker: String,
        stockName: String,
        response: RealtimeSupplyResponse
    ): RealtimeSupplyData {
        val items = response.items
        if (items.isNullOrEmpty()) {
            Log.w(TAG, "buildRealtimeSupplyData() no items in response for ticker=$ticker")
            // Return empty data if no items found
            return RealtimeSupplyData(
                ticker = ticker,
                name = stockName,
                currentPrice = 0L,
                netBuyAmount = 0L,
                buyAmount = 0L,
                sellAmount = 0L,
                netBuyQuantity = 0L,
                accumulatedVolume = 0L,
                fetchedAt = System.currentTimeMillis()
            )
        }

        // Find matching item by ticker, or use first item if only one
        val item = items.find { it.stkCd == ticker } ?: items.firstOrNull()
        if (item == null) {
            Log.w(TAG, "buildRealtimeSupplyData() no matching item found for ticker=$ticker")
            return RealtimeSupplyData(
                ticker = ticker,
                name = stockName,
                currentPrice = 0L,
                netBuyAmount = 0L,
                buyAmount = 0L,
                sellAmount = 0L,
                netBuyQuantity = 0L,
                accumulatedVolume = 0L,
                fetchedAt = System.currentTimeMillis()
            )
        }

        Log.d(TAG, "buildRealtimeSupplyData() found item: stkCd=${item.stkCd}, stkNm=${item.stkNm}")

        return RealtimeSupplyData(
            ticker = ticker,
            name = item.stkNm ?: stockName,
            currentPrice = parseSignedLong(item.currentPrice),
            netBuyAmount = parseSignedLong(item.netBuyAmount),
            buyAmount = parseSignedLong(item.buyAmount),
            sellAmount = parseSignedLong(item.sellAmount),
            netBuyQuantity = parseSignedLong(item.netBuyQuantity),
            accumulatedVolume = parseSignedLong(item.accumulatedVolume),
            fetchedAt = System.currentTimeMillis()
        )
    }

    /**
     * Parse a string value that may have sign prefix (e.g., "+1234", "-5678") to Long.
     * toLongOrNull() handles sign prefixes and returns null on parse failure, so no try-catch needed.
     */
    private fun parseSignedLong(value: String?): Long =
        value?.replace(",", "")?.trim()?.toLongOrNull() ?: 0L

    /**
     * Cache realtime supply data.
     */
    private suspend fun cacheSupply(data: RealtimeSupplyData) {
        try {
            val cachedData = CachedRealtimeSupplyData.fromDomain(data)
            val entity = RealtimeSupplyCacheEntity(
                ticker = data.ticker,
                name = data.name,
                data = json.encodeToString(cachedData),
                cachedAt = System.currentTimeMillis()
            )
            cacheDao.insert(entity)
            Log.d(TAG, "cacheSupply() cached data for ticker=${data.ticker}")
        } catch (e: Exception) {
            Log.w(TAG, "cacheSupply() failed to cache: ${e.message}", e)
        }
    }
}
