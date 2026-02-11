package com.stockapp.core.cache

import com.stockapp.core.db.dao.StockDao
import com.stockapp.feature.search.domain.model.Market
import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.repo.SearchRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for StockCacheManager.
 * Verifies cache state management, cooldown logic, and refresh behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockCacheManagerTest {

    private lateinit var searchRepo: SearchRepo
    private lateinit var stockDao: StockDao
    private lateinit var cacheManager: StockCacheManager

    @Before
    fun setup() {
        searchRepo = mock()
        stockDao = mock()
        cacheManager = StockCacheManager(searchRepo, stockDao)
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(CacheState.Idle, cacheManager.state.value)
    }

    // ==================== initializeIfNeeded Tests ====================

    @Test
    fun `initializeIfNeeded returns cached count when cache is valid`() = runTest {
        whenever(stockDao.count()).thenReturn(100)
        whenever(stockDao.lastUpdated()).thenReturn(System.currentTimeMillis() - 1000) // Recent

        val result = cacheManager.initializeIfNeeded()

        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull())
        assertEquals(CacheState.Ready(100), cacheManager.state.value)
    }

    // ==================== initializeLazy Tests ====================

    @Test
    fun `initializeLazy returns stale cache stats without API call when cache exists but expired`() = runTest {
        val oldTimestamp = System.currentTimeMillis() - 25 * 60 * 60 * 1000 // 25 hours ago
        whenever(stockDao.count()).thenReturn(50)
        whenever(stockDao.lastUpdated()).thenReturn(oldTimestamp)

        val result = cacheManager.initializeLazy()

        assertTrue(result.isSuccess)
        val stats = result.getOrNull()!!
        assertEquals(50, stats.count)
        assertTrue(stats.isExpired)
        // State should be Stale, not Loading
        assertTrue(cacheManager.state.value is CacheState.Stale)
    }

    @Test
    fun `initializeLazy returns fresh cache stats when cache is valid`() = runTest {
        val recentTimestamp = System.currentTimeMillis() - 1000 // 1 second ago
        whenever(stockDao.count()).thenReturn(200)
        whenever(stockDao.lastUpdated()).thenReturn(recentTimestamp)

        val result = cacheManager.initializeLazy()

        assertTrue(result.isSuccess)
        val stats = result.getOrNull()!!
        assertEquals(200, stats.count)
        assertFalse(stats.isExpired)
        assertEquals(CacheState.Ready(200), cacheManager.state.value)
    }

    // ==================== Cooldown Tests ====================

    @Test
    fun `refreshCache with cooldown prevents rapid refreshes`() = runTest {
        whenever(searchRepo.getAll()).thenReturn(Result.failure(Exception("test")))

        // First call should work
        cacheManager.refreshCache(bypassCooldown = false)

        // Second call within cooldown should fail with RefreshCooldownException
        val result2 = cacheManager.refreshCache(bypassCooldown = false)
        assertTrue(result2.isFailure)
        assertTrue(result2.exceptionOrNull() is RefreshCooldownException)
    }

    @Test
    fun `refreshCache bypassCooldown ignores cooldown`() = runTest {
        whenever(searchRepo.getAll()).thenReturn(Result.failure(Exception("test")))

        // First call to set cooldown
        cacheManager.refreshCache(bypassCooldown = false)

        // Second call with bypass should not get cooldown error
        val result = cacheManager.refreshCache(bypassCooldown = true)
        // It will still fail because of mocked exception, but NOT with RefreshCooldownException
        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is RefreshCooldownException)
    }

    // ==================== isRefreshAvailable Tests ====================

    @Test
    fun `isRefreshAvailable returns true initially`() {
        assertTrue(cacheManager.isRefreshAvailable())
    }

    @Test
    fun `getRemainingCooldownSec returns zero initially`() {
        assertEquals(0, cacheManager.getRemainingCooldownSec())
    }

    // ==================== refreshCache state changes ====================

    @Test
    fun `refreshCache sets Error state on failure`() = runTest {
        whenever(searchRepo.getAll()).thenReturn(Result.failure(Exception("test")))

        cacheManager.refreshCache(bypassCooldown = true)

        // After failure, state should be Error
        assertTrue(cacheManager.state.value is CacheState.Error)
    }

    // ==================== isCacheAvailable Tests ====================

    @Test
    fun `isCacheAvailable returns true when stocks exist`() = runTest {
        whenever(stockDao.count()).thenReturn(100)
        assertTrue(cacheManager.isCacheAvailable())
    }

    @Test
    fun `isCacheAvailable returns false when no stocks`() = runTest {
        whenever(stockDao.count()).thenReturn(0)
        assertFalse(cacheManager.isCacheAvailable())
    }

    // ==================== getCacheStats Tests ====================

    @Test
    fun `getCacheStats returns correct statistics`() = runTest {
        val now = System.currentTimeMillis()
        whenever(stockDao.count()).thenReturn(250)
        whenever(stockDao.lastUpdated()).thenReturn(now - 1000)

        val stats = cacheManager.getCacheStats()

        assertEquals(250, stats.count)
        assertFalse(stats.isExpired) // 1 second old, not expired
    }

    @Test
    fun `getCacheStats marks expired when cache is old`() = runTest {
        val oldTimestamp = System.currentTimeMillis() - 25 * 60 * 60 * 1000 // 25 hours
        whenever(stockDao.count()).thenReturn(100)
        whenever(stockDao.lastUpdated()).thenReturn(oldTimestamp)

        val stats = cacheManager.getCacheStats()

        assertEquals(100, stats.count)
        assertTrue(stats.isExpired)
    }

    @Test
    fun `getCacheStats handles null lastUpdated`() = runTest {
        whenever(stockDao.count()).thenReturn(0)
        whenever(stockDao.lastUpdated()).thenReturn(null)

        val stats = cacheManager.getCacheStats()

        assertEquals(0, stats.count)
        assertTrue(stats.isExpired) // null means very old
    }

    // ==================== Successful Refresh Tests ====================

    @Test
    fun `refreshCache sets Ready state on success`() = runTest {
        val stocks = listOf(
            Stock("005930", "삼성전자", Market.KOSPI),
            Stock("000660", "SK하이닉스", Market.KOSPI)
        )
        whenever(searchRepo.getAll()).thenReturn(Result.success(stocks))
        whenever(stockDao.count()).thenReturn(2)

        val result = cacheManager.refreshCache(bypassCooldown = true)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        assertTrue(cacheManager.state.value is CacheState.Ready)
    }

    // ==================== Error Message Mapping Tests ====================

    @Test
    fun `refreshCache maps auth error to user-friendly message`() = runTest {
        whenever(searchRepo.getAll())
            .thenReturn(Result.failure(Exception("AuthError: Invalid API key")))

        cacheManager.refreshCache(bypassCooldown = true)

        val state = cacheManager.state.value
        assertTrue(state is CacheState.Error)
        val errorMessage = (state as CacheState.Error).message
        assertTrue(
            "Error should mention API key, got: $errorMessage",
            errorMessage.contains("API") || errorMessage.contains("인증")
        )
    }

    @Test
    fun `refreshCache maps network error to user-friendly message`() = runTest {
        whenever(searchRepo.getAll())
            .thenReturn(Result.failure(Exception("Network error: Connection failed")))

        cacheManager.refreshCache(bypassCooldown = true)

        val state = cacheManager.state.value
        assertTrue(state is CacheState.Error)
        val errorMessage = (state as CacheState.Error).message
        assertTrue(
            "Error should mention network, got: $errorMessage",
            errorMessage.contains("네트워크") || errorMessage.contains("Network")
        )
    }
}
