package com.stockapp.feature.search.data.repo

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

@Singleton
class SearchRepoImpl @Inject constructor(
    private val pyClient: PyClient,
    private val stockDao: StockDao,
    private val historyDao: SearchHistoryDao,
    private val json: Json
) : SearchRepo {

    override suspend fun search(query: String): Result<List<Stock>> {
        // First try local cache
        val cached = stockDao.search(query)
        if (cached.isNotEmpty()) {
            return Result.success(cached.map { it.toDomain() })
        }

        // Fall back to Python API
        return pyClient.call(
            module = "stock_analyzer.stock.search",
            func = "search",
            args = listOf(query),
            timeoutMs = 30_000
        ) { jsonStr ->
            parseSearchResponse(jsonStr)
        }
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
        val response = json.decodeFromString<SearchResponse>(jsonStr)
        if (response.ok && response.data != null) {
            return response.data.map { it.toDomain() }
        } else {
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
