package com.stockapp.core.cache

import android.util.Log
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.entity.StockEntity
import com.stockapp.core.py.PyClient
import com.stockapp.feature.search.domain.model.SearchResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StockCacheManager"
private const val MAX_CACHE_SIZE = 10_000 // Maximum number of stocks to cache

/**
 * Stock cache state.
 */
sealed class CacheState {
    data object Idle : CacheState()
    data object Loading : CacheState()
    data class Ready(val count: Int) : CacheState()
    data class Error(val message: String) : CacheState()
}

/**
 * Manages stock cache initialization and refresh.
 */
@Singleton
class StockCacheManager @Inject constructor(
    private val pyClient: PyClient,
    private val stockDao: StockDao,
    private val json: Json
) {
    private val _state = MutableStateFlow<CacheState>(CacheState.Idle)
    val state: StateFlow<CacheState> = _state.asStateFlow()

    /**
     * Initialize cache if needed.
     * Call this on app startup.
     */
    suspend fun initializeIfNeeded(): Result<Int> {
        Log.d(TAG, "initializeIfNeeded() called")

        // Check if PyClient is ready
        if (!pyClient.isReady()) {
            Log.w(TAG, "initializeIfNeeded() PyClient not ready, skipping")
            return Result.failure(Exception("PyClient not initialized"))
        }

        // Check current cache state
        val count = stockDao.count()
        val lastUpdated = stockDao.lastUpdated() ?: 0L
        val now = System.currentTimeMillis()
        val cacheAge = now - lastUpdated

        Log.d(TAG, "initializeIfNeeded() cache count=$count, age=${cacheAge / 1000 / 60}min")

        // If cache is valid, return immediately
        if (count > 0 && cacheAge < AppDb.STOCK_CACHE_TTL) {
            Log.d(TAG, "initializeIfNeeded() cache is valid, skipping refresh")
            _state.value = CacheState.Ready(count)
            return Result.success(count)
        }

        // Refresh cache
        return refreshCache()
    }

    /**
     * Force refresh the stock cache.
     */
    suspend fun refreshCache(): Result<Int> {
        Log.d(TAG, "refreshCache() started")

        // Check if PyClient is ready
        if (!pyClient.isReady()) {
            Log.w(TAG, "refreshCache() PyClient not ready, cannot refresh")
            _state.value = CacheState.Error("API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요.")
            return Result.failure(Exception("PyClient not initialized"))
        }

        _state.value = CacheState.Loading

        return try {
            val result = pyClient.call(
                module = "stock_analyzer.stock.search",
                func = "get_all",
                args = emptyList(),
                timeoutMs = 120_000 // 2 minutes for full list
            ) { jsonStr ->
                parseStockList(jsonStr)
            }

            result.fold(
                onSuccess = { stocks ->
                    Log.d(TAG, "refreshCache() fetched ${stocks.size} stocks")

                    // Apply size limit to prevent excessive memory usage
                    val limitedStocks = if (stocks.size > MAX_CACHE_SIZE) {
                        Log.w(TAG, "refreshCache() truncating ${stocks.size} stocks to $MAX_CACHE_SIZE")
                        stocks.take(MAX_CACHE_SIZE)
                    } else {
                        stocks
                    }

                    // Clear old cache and insert new data
                    stockDao.deleteAll()
                    stockDao.insertAll(limitedStocks)

                    val count = stockDao.count()
                    Log.d(TAG, "refreshCache() cache updated with $count stocks")

                    _state.value = CacheState.Ready(count)
                    Result.success(count)
                },
                onFailure = { e ->
                    Log.e(TAG, "refreshCache() failed: ${e.message}", e)
                    _state.value = CacheState.Error(e.message ?: "Failed to refresh cache")
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "refreshCache() exception: ${e.message}", e)
            _state.value = CacheState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Check if cache is available.
     */
    suspend fun isCacheAvailable(): Boolean {
        val count = stockDao.count()
        return count > 0
    }

    /**
     * Get cache statistics.
     */
    suspend fun getCacheStats(): CacheStats {
        val count = stockDao.count()
        val lastUpdated = stockDao.lastUpdated() ?: 0L
        return CacheStats(
            count = count,
            lastUpdatedMs = lastUpdated,
            isExpired = System.currentTimeMillis() - lastUpdated > AppDb.STOCK_CACHE_TTL
        )
    }

    private fun parseStockList(jsonStr: String): List<StockEntity> {
        Log.d(TAG, "parseStockList() JSON length: ${jsonStr.length}")

        val response = json.decodeFromString<SearchResponse>(jsonStr)

        if (response.ok && response.data != null) {
            val now = System.currentTimeMillis()
            return response.data.map { stock ->
                StockEntity(
                    ticker = stock.ticker,
                    name = stock.name,
                    market = stock.market,
                    updatedAt = now
                )
            }
        } else {
            throw Exception(response.error?.msg ?: "Failed to parse stock list")
        }
    }
}

/**
 * Cache statistics.
 */
data class CacheStats(
    val count: Int,
    val lastUpdatedMs: Long,
    val isExpired: Boolean
)
