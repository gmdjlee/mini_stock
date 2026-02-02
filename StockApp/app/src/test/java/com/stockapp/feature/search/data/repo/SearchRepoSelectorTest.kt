package com.stockapp.feature.search.data.repo

import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.config.FeatureFlags
import com.stockapp.feature.search.domain.model.Market
import com.stockapp.feature.search.domain.model.Stock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for SearchRepoSelector.
 * Verifies correct delegation based on feature flags.
 */
class SearchRepoSelectorTest {

    private lateinit var nativeRepo: NativeSearchRepoImpl
    private lateinit var pyRepo: SearchRepoImpl
    private lateinit var featureFlagRepo: FeatureFlagRepo
    private lateinit var selector: SearchRepoSelector

    private val testStocks = listOf(
        Stock("005930", "삼성전자", Market.KOSPI),
        Stock("000660", "SK하이닉스", Market.KOSPI)
    )

    @Before
    fun setUp() {
        nativeRepo = mock()
        pyRepo = mock()
        featureFlagRepo = mock()
        selector = SearchRepoSelector(nativeRepo, pyRepo, featureFlagRepo)
    }

    // ========================================================================
    // Feature Flag Selection Tests
    // ========================================================================

    @Test
    fun `search delegates to native repo when flag is enabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)).thenReturn(true)
        whenever(nativeRepo.search("삼성")).thenReturn(Result.success(testStocks))

        // When
        val result = selector.search("삼성")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testStocks, result.getOrNull())
        verify(nativeRepo).search("삼성")
    }

    @Test
    fun `search delegates to python repo when flag is disabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)).thenReturn(false)
        whenever(pyRepo.search("삼성")).thenReturn(Result.success(testStocks))

        // When
        val result = selector.search("삼성")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testStocks, result.getOrNull())
        verify(pyRepo).search("삼성")
    }

    @Test
    fun `getAll delegates to native repo when flag is enabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)).thenReturn(true)
        whenever(nativeRepo.getAll()).thenReturn(Result.success(testStocks))

        // When
        val result = selector.getAll()

        // Then
        assertTrue(result.isSuccess)
        verify(nativeRepo).getAll()
    }

    @Test
    fun `getAll delegates to python repo when flag is disabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)).thenReturn(false)
        whenever(pyRepo.getAll()).thenReturn(Result.success(testStocks))

        // When
        val result = selector.getAll()

        // Then
        assertTrue(result.isSuccess)
        verify(pyRepo).getAll()
    }

    // ========================================================================
    // History Tests (Always uses native)
    // ========================================================================

    @Test
    fun `getHistory always uses native repo`() = runTest {
        // Given
        whenever(nativeRepo.getHistory()).thenReturn(flowOf(testStocks))

        // When
        val flow = selector.getHistory()

        // Then
        verify(nativeRepo).getHistory()
    }

    @Test
    fun `saveHistory always uses native repo`() = runTest {
        // Given
        val stock = Stock("005930", "삼성전자", Market.KOSPI)

        // When
        selector.saveHistory(stock)

        // Then
        verify(nativeRepo).saveHistory(stock)
    }

    @Test
    fun `clearHistory always uses native repo`() = runTest {
        // When
        selector.clearHistory()

        // Then
        verify(nativeRepo).clearHistory()
    }

    // ========================================================================
    // Cache Tests
    // ========================================================================

    @Test
    fun `isCacheAvailable delegates based on flag`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)).thenReturn(true)
        whenever(nativeRepo.isCacheAvailable()).thenReturn(true)

        // When
        val result = selector.isCacheAvailable()

        // Then
        assertTrue(result)
        verify(nativeRepo).isCacheAvailable()
    }

    @Test
    fun `getCacheCount delegates based on flag`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)).thenReturn(false)
        whenever(pyRepo.getCacheCount()).thenReturn(100)

        // When
        val result = selector.getCacheCount()

        // Then
        assertEquals(100, result)
        verify(pyRepo).getCacheCount()
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test
    fun `search returns failure when native repo fails`() = runTest {
        // Given
        val error = RuntimeException("Network error")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)).thenReturn(true)
        whenever(nativeRepo.search("삼성")).thenReturn(Result.failure(error))

        // When
        val result = selector.search("삼성")

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `searchForSuggestions delegates based on flag`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)).thenReturn(true)
        whenever(nativeRepo.searchForSuggestions("삼성")).thenReturn(testStocks)

        // When
        val result = selector.searchForSuggestions("삼성")

        // Then
        assertEquals(testStocks, result)
        verify(nativeRepo).searchForSuggestions("삼성")
    }
}
