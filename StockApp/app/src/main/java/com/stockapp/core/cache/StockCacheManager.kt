package com.stockapp.core.cache

import android.util.Log
import com.stockapp.core.config.AppConfig
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.entity.StockEntity
import com.stockapp.feature.search.domain.repo.SearchRepo
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StockCacheManager"

/** Refresh cooldown period in milliseconds (30 seconds). */
private const val REFRESH_COOLDOWN_MS = 30_000L

/**
 * Stock cache state.
 */
sealed class CacheState {
    data object Idle : CacheState()
    data object Loading : CacheState()
    data class Ready(val count: Int) : CacheState()
    data class Stale(val count: Int) : CacheState()
    data class Error(val message: String) : CacheState()
}

/**
 * Exception thrown when refresh is called during cooldown period.
 */
class RefreshCooldownException(message: String) : Exception(message)

/**
 * Manages stock cache initialization and refresh.
 */
@Singleton
class StockCacheManager @Inject constructor(
    private val searchRepo: SearchRepo,
    private val stockDao: StockDao
) {
    private val _state = MutableStateFlow<CacheState>(CacheState.Idle)
    val state: StateFlow<CacheState> = _state.asStateFlow()

    /** Last time refresh was attempted (for cooldown). */
    private var lastRefreshAttempt = 0L

    /**
     * Initialize cache if needed.
     * Call this on app startup.
     * Note: This method blocks if cache is expired. For non-blocking init, use initializeLazy().
     */
    suspend fun initializeIfNeeded(): Result<Int> {
        Log.d(TAG, "initializeIfNeeded() called")

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

        // Refresh cache (bypass cooldown for initialization)
        return refreshCache(bypassCooldown = true)
    }

    /**
     * Initialize cache lazily without blocking.
     * Returns immediately with existing cache (even if stale) to improve startup time.
     * Only calls API if no cache exists at all.
     *
     * Use this for app startup to avoid slow initialization when cache is expired.
     */
    suspend fun initializeLazy(): Result<CacheStats> {
        Log.d(TAG, "initializeLazy() called")

        val count = stockDao.count()
        val lastUpdated = stockDao.lastUpdated() ?: 0L
        val now = System.currentTimeMillis()
        val cacheAge = now - lastUpdated
        val isStale = cacheAge > AppDb.STOCK_CACHE_TTL

        Log.d(TAG, "initializeLazy() count=$count, age=${cacheAge / 1000 / 60}min, stale=$isStale")

        val stats = CacheStats(
            count = count,
            lastUpdatedMs = lastUpdated,
            isExpired = isStale
        )

        // If cache exists (even if stale), return immediately without API call
        if (count > 0) {
            _state.value = if (isStale) {
                Log.d(TAG, "initializeLazy() cache is stale but available, using it")
                CacheState.Stale(count)
            } else {
                Log.d(TAG, "initializeLazy() cache is fresh")
                CacheState.Ready(count)
            }
            return Result.success(stats)
        }

        // No cache at all - this is the only case we call API
        Log.d(TAG, "initializeLazy() no cache, calling API")

        return refreshCache(bypassCooldown = true).map {
            CacheStats(
                count = it,
                lastUpdatedMs = System.currentTimeMillis(),
                isExpired = false
            )
        }
    }

    /**
     * Force refresh the stock cache with cooldown protection.
     * @param bypassCooldown If true, bypasses cooldown check (for internal use).
     * @return Result.failure with RefreshCooldownException if cooldown not elapsed.
     */
    suspend fun refreshCache(bypassCooldown: Boolean = false): Result<Int> {
        Log.d(TAG, "refreshCache() started, bypassCooldown=$bypassCooldown")

        // Check cooldown (unless bypassed for internal initialization)
        if (!bypassCooldown) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRefreshAttempt
            if (elapsed < REFRESH_COOLDOWN_MS) {
                val remainingSec = (REFRESH_COOLDOWN_MS - elapsed) / 1000
                Log.d(TAG, "refreshCache() cooldown active, ${remainingSec}s remaining")
                return Result.failure(
                    RefreshCooldownException("잠시 후 다시 시도해주세요 (${remainingSec}초)")
                )
            }
            lastRefreshAttempt = now
        }

        _state.value = CacheState.Loading

        return try {
            val result = searchRepo.getAll()

            result.fold(
                onSuccess = { stocks ->
                    Log.d(TAG, "refreshCache() fetched ${stocks.size} stocks")

                    val now = System.currentTimeMillis()
                    val stockEntities = stocks.map { stock ->
                        StockEntity(
                            ticker = stock.ticker,
                            name = stock.name,
                            market = stock.market.name,
                            updatedAt = now
                        )
                    }

                    // Apply size limit to prevent excessive memory usage
                    val limitedStocks = if (stockEntities.size > AppConfig.MAX_STOCK_CACHE_SIZE) {
                        Log.w(TAG, "refreshCache() truncating ${stockEntities.size} stocks to ${AppConfig.MAX_STOCK_CACHE_SIZE}")
                        stockEntities.sortedWith(
                            compareBy<StockEntity> {
                                when (it.market) {
                                    "KOSPI" -> 0
                                    "KOSDAQ" -> 1
                                    else -> 2
                                }
                            }.thenBy { it.name }
                        ).take(AppConfig.MAX_STOCK_CACHE_SIZE)
                    } else {
                        stockEntities
                    }

                    // Smart sync: upsert active + remove delisted
                    stockDao.smartSync(limitedStocks)

                    val count = stockDao.count()
                    Log.d(TAG, "refreshCache() cache updated with $count stocks")

                    _state.value = CacheState.Ready(count)
                    Result.success(count)
                },
                onFailure = { e ->
                    Log.e(TAG, "refreshCache() failed: ${e.message}", e)
                    val userMessage = mapErrorToUserMessage(e.message)
                    _state.value = CacheState.Error(userMessage)
                    Result.failure(e)
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "refreshCache() exception: ${e.message}", e)
            val userMessage = mapErrorToUserMessage(e.message)
            _state.value = CacheState.Error(userMessage)
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

    /**
     * Check if refresh is available (cooldown elapsed).
     */
    fun isRefreshAvailable(): Boolean {
        return System.currentTimeMillis() - lastRefreshAttempt >= REFRESH_COOLDOWN_MS
    }

    /**
     * Get remaining cooldown time in seconds.
     */
    fun getRemainingCooldownSec(): Int {
        val elapsed = System.currentTimeMillis() - lastRefreshAttempt
        return maxOf(0, ((REFRESH_COOLDOWN_MS - elapsed) / 1000).toInt())
    }

    /**
     * Map technical error messages to user-friendly messages.
     */
    private fun mapErrorToUserMessage(errorMessage: String?): String {
        if (errorMessage == null) return "알 수 없는 오류가 발생했습니다."

        return when {
            // Authentication errors
            errorMessage.contains("AuthError") ||
            errorMessage.contains("인증에 실패") ||
            errorMessage.contains("App Key") ||
            errorMessage.contains("Secret Key") -> {
                "API 키 인증에 실패했습니다. 설정에서 올바른 API 키를 입력해주세요."
            }
            // Network errors
            errorMessage.contains("Network error") ||
            errorMessage.contains("네트워크") ||
            errorMessage.contains("timeout", ignoreCase = true) -> {
                "네트워크 연결에 실패했습니다. 인터넷 연결을 확인해주세요."
            }
            // Not initialized
            errorMessage.contains("not initialized") ||
            errorMessage.contains("NotInitialized") ||
            errorMessage.contains("NoApiKey") -> {
                "API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요."
            }
            // Rate limit
            errorMessage.contains("Rate limit") ||
            errorMessage.contains("429") -> {
                "API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요."
            }
            // Default
            else -> errorMessage
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
