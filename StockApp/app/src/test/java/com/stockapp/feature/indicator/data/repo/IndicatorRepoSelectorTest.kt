package com.stockapp.feature.indicator.data.repo

import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.config.FeatureFlags
import com.stockapp.feature.indicator.domain.model.DemarkSetup
import com.stockapp.feature.indicator.domain.model.ElderImpulse
import com.stockapp.feature.indicator.domain.model.TrendSignal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for IndicatorRepoSelector.
 * Verifies correct delegation based on feature flags.
 */
class IndicatorRepoSelectorTest {

    private lateinit var nativeRepo: NativeIndicatorRepoImpl
    private lateinit var pyRepo: IndicatorRepoImpl
    private lateinit var featureFlagRepo: FeatureFlagRepo
    private lateinit var selector: IndicatorRepoSelector

    private val testDates = listOf("2024-01-15", "2024-01-14", "2024-01-13")

    private val testTrendSignal = TrendSignal(
        ticker = "005930",
        timeframe = "daily",
        dates = testDates,
        maSignal = listOf(1, 0, -1),
        cmf = listOf(0.15, 0.10, 0.05),
        fearGreed = listOf(0.6, 0.5, 0.4),
        trend = listOf("bullish", "neutral", "neutral"),
        ma5 = listOf(55000, 54500, 54000),
        ma10 = listOf(54000, 53500, 53000),
        ma20 = listOf(53000, 52500, 52000)
    )

    private val testElderImpulse = ElderImpulse(
        ticker = "005930",
        timeframe = "daily",
        dates = testDates,
        color = listOf("green", "blue", "red"),
        ema13 = listOf(55000.0, 54800.0, 54600.0),
        macdLine = listOf(500.0, 450.0, 400.0),
        signalLine = listOf(480.0, 440.0, 410.0),
        macdHist = listOf(20.0, 10.0, -10.0),
        close = listOf(55500.0, 55000.0, 54500.0)
    )

    private val testDemarkSetup = DemarkSetup(
        ticker = "005930",
        timeframe = "daily",
        dates = testDates,
        close = listOf(55500, 55000, 54500),
        sellSetup = listOf(5, 4, 3),
        buySetup = listOf(0, 0, 0)
    )

    @Before
    fun setUp() {
        nativeRepo = mock()
        pyRepo = mock()
        featureFlagRepo = mock()
        selector = IndicatorRepoSelector(nativeRepo, pyRepo, featureFlagRepo)
    }

    // ========================================================================
    // getTrend Tests
    // ========================================================================

    @Test
    fun `getTrend delegates to native repo when flag is enabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getTrend("005930", 180, "daily", true))
            .thenReturn(Result.success(testTrendSignal))

        // When
        val result = selector.getTrend("005930", 180, "daily", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testTrendSignal, result.getOrNull())
        verify(nativeRepo).getTrend("005930", 180, "daily", true)
    }

    @Test
    fun `getTrend delegates to python repo when flag is disabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(false)
        whenever(pyRepo.getTrend("005930", 180, "daily", true))
            .thenReturn(Result.success(testTrendSignal))

        // When
        val result = selector.getTrend("005930", 180, "daily", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testTrendSignal, result.getOrNull())
        verify(pyRepo).getTrend("005930", 180, "daily", true)
    }

    @Test
    fun `getTrend respects timeframe parameter`() = runTest {
        // Given
        val weeklySignal = testTrendSignal.copy(timeframe = "weekly")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getTrend("005930", 180, "weekly", true))
            .thenReturn(Result.success(weeklySignal))

        // When
        val result = selector.getTrend("005930", 180, "weekly", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("weekly", result.getOrNull()?.timeframe)
        verify(nativeRepo).getTrend("005930", 180, "weekly", true)
    }

    @Test
    fun `getTrend returns failure when native repo fails`() = runTest {
        // Given
        val error = RuntimeException("Calculation error")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getTrend("005930", 180, "daily", true))
            .thenReturn(Result.failure(error))

        // When
        val result = selector.getTrend("005930", 180, "daily", true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    // ========================================================================
    // getElder Tests
    // ========================================================================

    @Test
    fun `getElder delegates to native repo when flag is enabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getElder("005930", 180, "daily", true))
            .thenReturn(Result.success(testElderImpulse))

        // When
        val result = selector.getElder("005930", 180, "daily", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testElderImpulse, result.getOrNull())
        verify(nativeRepo).getElder("005930", 180, "daily", true)
    }

    @Test
    fun `getElder delegates to python repo when flag is disabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(false)
        whenever(pyRepo.getElder("005930", 180, "daily", true))
            .thenReturn(Result.success(testElderImpulse))

        // When
        val result = selector.getElder("005930", 180, "daily", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testElderImpulse, result.getOrNull())
        verify(pyRepo).getElder("005930", 180, "daily", true)
    }

    @Test
    fun `getElder respects useCache parameter`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getElder("005930", 180, "daily", false))
            .thenReturn(Result.success(testElderImpulse))

        // When
        val result = selector.getElder("005930", 180, "daily", false)

        // Then
        assertTrue(result.isSuccess)
        verify(nativeRepo).getElder("005930", 180, "daily", false)
    }

    @Test
    fun `getElder returns failure when python repo fails`() = runTest {
        // Given
        val error = RuntimeException("Python error")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(false)
        whenever(pyRepo.getElder("005930", 180, "daily", true))
            .thenReturn(Result.failure(error))

        // When
        val result = selector.getElder("005930", 180, "daily", true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    // ========================================================================
    // getDemark Tests
    // ========================================================================

    @Test
    fun `getDemark delegates to native repo when flag is enabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getDemark("005930", 180, "daily", true))
            .thenReturn(Result.success(testDemarkSetup))

        // When
        val result = selector.getDemark("005930", 180, "daily", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDemarkSetup, result.getOrNull())
        verify(nativeRepo).getDemark("005930", 180, "daily", true)
    }

    @Test
    fun `getDemark delegates to python repo when flag is disabled`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(false)
        whenever(pyRepo.getDemark("005930", 180, "daily", true))
            .thenReturn(Result.success(testDemarkSetup))

        // When
        val result = selector.getDemark("005930", 180, "daily", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDemarkSetup, result.getOrNull())
        verify(pyRepo).getDemark("005930", 180, "daily", true)
    }

    @Test
    fun `getDemark supports monthly timeframe`() = runTest {
        // Given
        val monthlySetup = testDemarkSetup.copy(timeframe = "monthly")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getDemark("005930", 180, "monthly", true))
            .thenReturn(Result.success(monthlySetup))

        // When
        val result = selector.getDemark("005930", 180, "monthly", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("monthly", result.getOrNull()?.timeframe)
        verify(nativeRepo).getDemark("005930", 180, "monthly", true)
    }

    @Test
    fun `getDemark returns failure when native repo fails`() = runTest {
        // Given
        val error = RuntimeException("Insufficient data")
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getDemark("005930", 180, "daily", true))
            .thenReturn(Result.failure(error))

        // When
        val result = selector.getDemark("005930", 180, "daily", true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    // ========================================================================
    // Cache Operations Tests
    // ========================================================================

    @Test
    fun `clearCache clears both repos`() = runTest {
        // When
        selector.clearCache("005930")

        // Then
        verify(nativeRepo).clearCache("005930")
        verify(pyRepo).clearCache("005930")
    }

    @Test
    fun `clearExpiredCache clears both repos`() = runTest {
        // When
        selector.clearExpiredCache()

        // Then
        verify(nativeRepo).clearExpiredCache()
        verify(pyRepo).clearExpiredCache()
    }

    // ========================================================================
    // Different Ticker Tests
    // ========================================================================

    @Test
    fun `all methods work with different tickers`() = runTest {
        // Given
        val skTrend = testTrendSignal.copy(ticker = "000660")
        val skElder = testElderImpulse.copy(ticker = "000660")
        val skDemark = testDemarkSetup.copy(ticker = "000660")

        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getTrend("000660", 180, "daily", true))
            .thenReturn(Result.success(skTrend))
        whenever(nativeRepo.getElder("000660", 180, "daily", true))
            .thenReturn(Result.success(skElder))
        whenever(nativeRepo.getDemark("000660", 180, "daily", true))
            .thenReturn(Result.success(skDemark))

        // When
        val trendResult = selector.getTrend("000660", 180, "daily", true)
        val elderResult = selector.getElder("000660", 180, "daily", true)
        val demarkResult = selector.getDemark("000660", 180, "daily", true)

        // Then
        assertEquals("000660", trendResult.getOrNull()?.ticker)
        assertEquals("000660", elderResult.getOrNull()?.ticker)
        assertEquals("000660", demarkResult.getOrNull()?.ticker)
    }

    // ========================================================================
    // Flag Toggle Tests
    // ========================================================================

    @Test
    fun `methods switch repos when flag changes`() = runTest {
        // First call with native
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getTrend("005930", 180, "daily", true))
            .thenReturn(Result.success(testTrendSignal))

        val result1 = selector.getTrend("005930", 180, "daily", true)
        assertTrue(result1.isSuccess)
        verify(nativeRepo).getTrend("005930", 180, "daily", true)

        // Second call with python
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(false)
        whenever(pyRepo.getTrend("005930", 180, "daily", true))
            .thenReturn(Result.success(testTrendSignal))

        val result2 = selector.getTrend("005930", 180, "daily", true)
        assertTrue(result2.isSuccess)
        verify(pyRepo).getTrend("005930", 180, "daily", true)
    }

    // ========================================================================
    // Days Parameter Tests
    // ========================================================================

    @Test
    fun `all methods respect days parameter`() = runTest {
        // Given
        whenever(featureFlagRepo.isEnabled(FeatureFlags.USE_NATIVE_INDICATOR)).thenReturn(true)
        whenever(nativeRepo.getTrend("005930", 90, "daily", true))
            .thenReturn(Result.success(testTrendSignal))
        whenever(nativeRepo.getElder("005930", 90, "daily", true))
            .thenReturn(Result.success(testElderImpulse))
        whenever(nativeRepo.getDemark("005930", 90, "daily", true))
            .thenReturn(Result.success(testDemarkSetup))

        // When
        selector.getTrend("005930", 90, "daily", true)
        selector.getElder("005930", 90, "daily", true)
        selector.getDemark("005930", 90, "daily", true)

        // Then
        verify(nativeRepo).getTrend("005930", 90, "daily", true)
        verify(nativeRepo).getElder("005930", 90, "daily", true)
        verify(nativeRepo).getDemark("005930", 90, "daily", true)
    }
}
