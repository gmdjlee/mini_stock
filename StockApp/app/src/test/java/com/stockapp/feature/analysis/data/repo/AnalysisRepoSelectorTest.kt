package com.stockapp.feature.analysis.data.repo

import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.config.FeatureFlags
import com.stockapp.feature.analysis.domain.model.StockData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for AnalysisRepoSelector.
 * Verifies correct delegation based on feature flags.
 */
class AnalysisRepoSelectorTest {

    private lateinit var nativeRepo: NativeAnalysisRepoImpl
    private lateinit var pyRepo: AnalysisRepoImpl
    private lateinit var featureFlagRepo: FeatureFlagRepo
    private lateinit var selector: AnalysisRepoSelector

    private val testStockData = StockData(
        ticker = "005930",
        name = "삼성전자",
        dates = listOf("2024-01-15", "2024-01-14", "2024-01-13"),
        mcap = listOf(400_000_000_000_000L, 398_000_000_000_000L, 395_000_000_000_000L),
        for5d = listOf(50000L, 48000L, 45000L),
        ins5d = listOf(30000L, 28000L, 25000L)
    )

    @Before
    fun setUp() {
        nativeRepo = mock()
        pyRepo = mock()
        featureFlagRepo = mock()
        selector = AnalysisRepoSelector(nativeRepo, pyRepo, featureFlagRepo)
    }

    // ========================================================================
    // Feature Flag Selection Tests
    // ========================================================================

    @Test
    fun `getAnalysis delegates to native repo when flag is enabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(true)
        whenever(nativeRepo.getAnalysis("005930", 180, true)).thenReturn(Result.success(testStockData))

        // When
        val result = selector.getAnalysis("005930", 180, true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testStockData, result.getOrNull())
        verify(nativeRepo).getAnalysis("005930", 180, true)
    }

    @Test
    fun `getAnalysis delegates to python repo when flag is disabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(false)
        whenever(pyRepo.getAnalysis("005930", 180, true)).thenReturn(Result.success(testStockData))

        // When
        val result = selector.getAnalysis("005930", 180, true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testStockData, result.getOrNull())
        verify(pyRepo).getAnalysis("005930", 180, true)
    }

    @Test
    fun `getAnalysis respects useCache parameter`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(true)
        whenever(nativeRepo.getAnalysis("005930", 180, false)).thenReturn(Result.success(testStockData))

        // When
        val result = selector.getAnalysis("005930", 180, false)

        // Then
        assertTrue(result.isSuccess)
        verify(nativeRepo).getAnalysis("005930", 180, false)
    }

    @Test
    fun `getAnalysis respects days parameter`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(true)
        whenever(nativeRepo.getAnalysis("005930", 90, true)).thenReturn(Result.success(testStockData))

        // When
        val result = selector.getAnalysis("005930", 90, true)

        // Then
        assertTrue(result.isSuccess)
        verify(nativeRepo).getAnalysis("005930", 90, true)
    }

    // ========================================================================
    // Cache Operations Tests (Always use native repo)
    // ========================================================================

    @Test
    fun `getCachedAnalysis always uses native repo`() = runTest {
        // Given
        whenever(nativeRepo.getCachedAnalysis("005930")).thenReturn(testStockData)

        // When
        val result = selector.getCachedAnalysis("005930")

        // Then
        assertEquals(testStockData, result)
        verify(nativeRepo).getCachedAnalysis("005930")
    }

    @Test
    fun `getCachedAnalysis returns null when no cache`() = runTest {
        // Given
        whenever(nativeRepo.getCachedAnalysis("005930")).thenReturn(null)

        // When
        val result = selector.getCachedAnalysis("005930")

        // Then
        assertNull(result)
        verify(nativeRepo).getCachedAnalysis("005930")
    }

    @Test
    fun `clearCache always uses native repo`() = runTest {
        // When
        selector.clearCache("005930")

        // Then
        verify(nativeRepo).clearCache("005930")
    }

    @Test
    fun `clearAllCache always uses native repo`() = runTest {
        // When
        selector.clearAllCache()

        // Then
        verify(nativeRepo).clearAllCache()
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test
    fun `getAnalysis returns failure when native repo fails`() = runTest {
        // Given
        val error = RuntimeException("Network error")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(true)
        whenever(nativeRepo.getAnalysis("005930", 180, true)).thenReturn(Result.failure(error))

        // When
        val result = selector.getAnalysis("005930", 180, true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `getAnalysis returns failure when python repo fails`() = runTest {
        // Given
        val error = RuntimeException("Python error")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(false)
        whenever(pyRepo.getAnalysis("005930", 180, true)).thenReturn(Result.failure(error))

        // When
        val result = selector.getAnalysis("005930", 180, true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    // ========================================================================
    // Different Ticker Tests
    // ========================================================================

    @Test
    fun `getAnalysis works with different tickers`() = runTest {
        // Given
        val skData = testStockData.copy(ticker = "000660", name = "SK하이닉스")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(true)
        whenever(nativeRepo.getAnalysis("000660", 180, true)).thenReturn(Result.success(skData))

        // When
        val result = selector.getAnalysis("000660", 180, true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("000660", result.getOrNull()?.ticker)
        assertEquals("SK하이닉스", result.getOrNull()?.name)
    }

    // ========================================================================
    // Flag Toggle Tests
    // ========================================================================

    @Test
    fun `getAnalysis switches repos when flag changes`() = runTest {
        // First call with native
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(true)
        whenever(nativeRepo.getAnalysis("005930", 180, true)).thenReturn(Result.success(testStockData))

        val result1 = selector.getAnalysis("005930", 180, true)
        assertTrue(result1.isSuccess)
        verify(nativeRepo).getAnalysis("005930", 180, true)

        // Second call with python
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_ANALYSIS)).thenReturn(false)
        whenever(pyRepo.getAnalysis("005930", 180, true)).thenReturn(Result.success(testStockData))

        val result2 = selector.getAnalysis("005930", 180, true)
        assertTrue(result2.isSuccess)
        verify(pyRepo).getAnalysis("005930", 180, true)
    }
}
