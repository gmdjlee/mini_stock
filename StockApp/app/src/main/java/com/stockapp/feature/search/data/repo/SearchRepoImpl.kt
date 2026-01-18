package com.stockapp.feature.search.data.repo

import android.util.Log
import com.stockapp.core.db.dao.SearchHistoryDao
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.entity.SearchHistoryEntity
import com.stockapp.core.db.entity.StockEntity
import com.stockapp.core.py.PyClient
import com.stockapp.core.py.PyApiException
import com.stockapp.feature.search.domain.model.Market
import com.stockapp.feature.search.domain.model.SearchResponse
import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.repo.SearchRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SearchRepoImpl"

@Singleton
class SearchRepoImpl @Inject constructor(
    private val pyClient: PyClient,
    private val stockDao: StockDao,
    private val historyDao: SearchHistoryDao,
    private val json: Json
) : SearchRepo {

    override suspend fun search(query: String): Result<List<Stock>> {
        Log.d(TAG, "search() called with query: $query")

        // First try local cache
        val cached = stockDao.search(query)
        Log.d(TAG, "search() cached results: ${cached.size}")

        if (cached.isNotEmpty()) {
            Log.d(TAG, "search() returning cached results")
            return Result.success(cached.map { it.toDomain() })
        }

        Log.d(TAG, "search() calling Python API")

        // Fall back to Python API
        val result = pyClient.call(
            module = "stock_analyzer.stock.search",
            func = "search",
            args = listOf(query),
            timeoutMs = 30_000
        ) { jsonStr ->
            parseSearchResponse(jsonStr)
        }

        result.onSuccess { stocks ->
            Log.d(TAG, "search() API returned ${stocks.size} stocks")
        }.onFailure { e ->
            Log.e(TAG, "search() API failed: ${e.message}", e)
        }

        return result
    }

    override suspend fun getAll(): Result<List<Stock>> {
        val result = pyClient.call(
            module = "stock_analyzer.stock.search",
            func = "get_all",
            args = emptyList(),
            timeoutMs = 60_000
        ) { jsonStr ->
            parseSearchResponse(jsonStr)
        }

        // Cache results after the call completes
        result.onSuccess { stocks ->
            stockDao.insertAll(stocks.map { it.toEntity() })
        }

        return result
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

    private fun parseSearchResponse(jsonStr: String): List<Stock> {
        Log.d(TAG, "parseSearchResponse() JSON (first 500 chars): ${jsonStr.take(500)}")

        val response = json.decodeFromString<SearchResponse>(jsonStr)
        Log.d(TAG, "parseSearchResponse() ok=${response.ok}, data=${response.data?.size ?: 0}, error=${response.error}")

        if (response.ok && response.data != null) {
            val stocks = response.data.map { it.toDomain() }
            Log.d(TAG, "parseSearchResponse() parsed ${stocks.size} stocks")
            return stocks
        } else {
            Log.e(TAG, "parseSearchResponse() API error: code=${response.error?.code}, msg=${response.error?.msg}")
            throw PyApiException(
                response.error?.code ?: "UNKNOWN",
                response.error?.msg ?: "검색 실패"
            )
        }
    }

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
