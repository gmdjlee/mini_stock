package com.stockapp.feature.search.data.repo

import android.util.Log
import com.stockapp.core.api.ApiError
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.core.config.AppConfig
import com.stockapp.core.db.dao.SearchHistoryDao
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.entity.SearchHistoryEntity
import com.stockapp.core.db.entity.StockEntity
import com.stockapp.core.stock.api.StockApiIds
import com.stockapp.core.stock.api.StockListResponse
import com.stockapp.feature.search.domain.model.Market
import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.repo.SearchRepo
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NativeSearchRepoImpl"

// Default markets to include (KOSPI and KOSDAQ only, same as Python implementation)
// This filters out ETN, ETF, and other derivative products
private val DEFAULT_MARKETS = setOf("KOSPI", "KOSDAQ")

/**
 * Native Kotlin implementation of SearchRepo.
 * Uses KiwoomApiClient directly instead of Python/Chaquopy.
 */
@Singleton
class NativeSearchRepoImpl @Inject constructor(
    private val apiClient: KiwoomApiClient,
    private val settingsRepo: SettingsRepo,
    private val stockDao: StockDao,
    private val historyDao: SearchHistoryDao,
    private val json: Json
) : SearchRepo {

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

    override suspend fun search(query: String): Result<List<Stock>> {
        Log.d(TAG, "search() called with query: $query")

        // First try local cache with Kotlin filtering (SQLite LIKE has issues with Korean text)
        val cacheCount = stockDao.count()
        Log.d(TAG, "search() cache count: $cacheCount")

        if (cacheCount > 0) {
            val allStocks = stockDao.getAllOnce()
            val queryLower = query.lowercase()
            val filtered = allStocks.filter { stock ->
                // Filter by market (KOSPI/KOSDAQ only)
                stock.market in DEFAULT_MARKETS &&
                // Filter by query
                (stock.name.lowercase().contains(queryLower) ||
                    stock.ticker.lowercase().contains(queryLower))
            }.take(50) // Limit results

            Log.d(TAG, "search() filtered results: ${filtered.size}")

            if (filtered.isNotEmpty()) {
                Log.d(TAG, "search() returning filtered cache results")
                return Result.success(filtered.map { it.toDomain() })
            }
        }

        Log.d(TAG, "search() calling Native API")

        // Fall back to Native API
        return try {
            val config = getApiConfig()

            apiClient.call(
                apiId = StockApiIds.STOCK_LIST,
                url = "/api/dostk/stkinfo",
                body = mapOf("mrkt_tp" to "0"), // All markets
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl
            ) { responseJson ->
                parseStockListResponse(responseJson, query)
            }
        } catch (e: ApiError) {
            Log.e(TAG, "search() API failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getAll(): Result<List<Stock>> {
        Log.d(TAG, "getAll() called")

        return try {
            val config = getApiConfig()

            val result = apiClient.call(
                apiId = StockApiIds.STOCK_LIST,
                url = "/api/dostk/stkinfo",
                body = mapOf("mrkt_tp" to "0"), // All markets
                appKey = config.appKey,
                secretKey = config.secretKey,
                baseUrl = config.baseUrl
            ) { responseJson ->
                parseAllStocksResponse(responseJson)
            }

            // Cache results after the call completes
            result.onSuccess { stocks ->
                Log.d(TAG, "getAll() caching ${stocks.size} stocks")
                stockDao.insertAll(stocks.map { it.toEntity() })
            }.onFailure { e ->
                Log.e(TAG, "getAll() failed: ${e.message}", e)
            }

            result
        } catch (e: ApiError) {
            Log.e(TAG, "getAll() API failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getHistory(): Flow<List<Stock>> {
        return historyDao.getRecent(20).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveHistory(stock: Stock) {
        historyDao.deleteByTicker(stock.ticker)
        historyDao.insert(
            SearchHistoryEntity(
                ticker = stock.ticker,
                name = stock.name,
                searchedAt = System.currentTimeMillis()
            )
        )
        historyDao.trimToSize(50)
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll()
    }

    override suspend fun searchForSuggestions(query: String): List<Stock> {
        return search(query).getOrElse { emptyList() }
    }

    override suspend fun isCacheAvailable(): Boolean {
        val count = stockDao.count()
        Log.d(TAG, "isCacheAvailable() count=$count")
        return count > 0
    }

    override suspend fun getCacheCount(): Int {
        return stockDao.count()
    }

    override suspend fun searchCacheOnly(query: String): Result<List<Stock>> {
        Log.d(TAG, "searchCacheOnly() query: $query")

        val cacheCount = stockDao.count()
        if (cacheCount == 0) {
            Log.d(TAG, "searchCacheOnly() cache empty, returning empty list")
            return Result.success(emptyList())
        }

        val allStocks = stockDao.getAllOnce()
        val queryLower = query.lowercase()
        val filtered = allStocks.filter { stock ->
            stock.market in DEFAULT_MARKETS &&
                (stock.name.lowercase().contains(queryLower) ||
                    stock.ticker.lowercase().contains(queryLower))
        }.take(AppConfig.MAX_SEARCH_RESULTS)

        Log.d(TAG, "searchCacheOnly() found ${filtered.size} results")
        return Result.success(filtered.map { it.toDomain() })
    }

    /**
     * Parse stock list response and filter by query.
     * Also filters by market (KOSPI/KOSDAQ only) to exclude ETN, ETF, etc.
     */
    private fun parseStockListResponse(jsonStr: String, query: String): List<Stock> {
        Log.d(TAG, "parseStockListResponse() JSON (first 500 chars): ${jsonStr.take(500)}")

        val response = json.decodeFromString<StockListResponse>(jsonStr)

        if (response.returnCode != 0) {
            throw ApiError.ApiCallError(response.returnCode, response.returnMsg ?: "API 오류")
        }

        val stocks = response.stkList?.mapNotNull { item ->
            val ticker = item.stkCd ?: return@mapNotNull null
            val name = item.stkNm ?: return@mapNotNull null
            val marketRaw = item.mrktNm ?: "OTHER"

            // Convert market name to standardized format (same logic as Python)
            val market = normalizeMarketName(marketRaw)

            // Filter by market (KOSPI/KOSDAQ only, exclude ETN, ETF, etc.)
            if (market.name !in DEFAULT_MARKETS) {
                return@mapNotNull null
            }

            Stock(
                ticker = ticker,
                name = name,
                market = market
            )
        } ?: emptyList()

        // Filter by query
        val queryLower = query.lowercase()
        val filtered = stocks.filter { stock ->
            stock.name.lowercase().contains(queryLower) ||
                stock.ticker.lowercase().contains(queryLower)
        }.take(50)

        Log.d(TAG, "parseStockListResponse() total=${stocks.size}, filtered=${filtered.size}")

        return filtered
    }

    /**
     * Normalize market name to standardized format.
     * Matches Python's _get_market_name() function.
     */
    private fun normalizeMarketName(marketName: String): Market {
        if (marketName.isEmpty()) return Market.OTHER

        val nameUpper = marketName.uppercase()
        return when {
            // '거래소' = KOSPI (유가증권시장)
            marketName.contains("거래소") || marketName.contains("코스피") ||
                nameUpper.contains("KOSPI") -> Market.KOSPI
            marketName.contains("코스닥") || nameUpper.contains("KOSDAQ") -> Market.KOSDAQ
            else -> Market.OTHER
        }
    }

    /**
     * Parse all stocks response with market filtering (KOSPI/KOSDAQ only).
     */
    private fun parseAllStocksResponse(jsonStr: String): List<Stock> {
        Log.d(TAG, "parseAllStocksResponse()")

        val response = json.decodeFromString<StockListResponse>(jsonStr)

        if (response.returnCode != 0) {
            throw ApiError.ApiCallError(response.returnCode, response.returnMsg ?: "API 오류")
        }

        val stocks = response.stkList?.mapNotNull { item ->
            val ticker = item.stkCd ?: return@mapNotNull null
            val name = item.stkNm ?: return@mapNotNull null
            val marketRaw = item.mrktNm ?: "OTHER"

            // Convert market name to standardized format
            val market = normalizeMarketName(marketRaw)

            // Filter by market (KOSPI/KOSDAQ only, exclude ETN, ETF, etc.)
            if (market.name !in DEFAULT_MARKETS) {
                return@mapNotNull null
            }

            Stock(
                ticker = ticker,
                name = name,
                market = market
            )
        } ?: emptyList()

        Log.d(TAG, "parseAllStocksResponse() parsed ${stocks.size} stocks (filtered to KOSPI/KOSDAQ)")

        return stocks
    }

    // Extension functions for entity conversion

    private fun StockEntity.toDomain(): Stock = Stock(
        ticker = ticker,
        name = name,
        market = Market.fromString(market)
    )

    private fun SearchHistoryEntity.toDomain(): Stock = Stock(
        ticker = ticker,
        name = name,
        market = Market.OTHER
    )

    private fun Stock.toEntity(): StockEntity = StockEntity(
        ticker = ticker,
        name = name,
        market = market.name,
        updatedAt = System.currentTimeMillis()
    )
}
