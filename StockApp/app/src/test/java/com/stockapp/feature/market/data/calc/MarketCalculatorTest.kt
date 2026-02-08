package com.stockapp.feature.market.data.calc

import com.stockapp.feature.market.domain.model.FearGreedSignal
import com.stockapp.feature.market.domain.model.OscillatorSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MarketCalculator.
 * Verifies oscillator and fear/greed calculations with pure data logic.
 */
class MarketCalculatorTest {

    private val DELTA = 0.01

    // ========================================================================
    // calculateOscillator Tests
    // ========================================================================

    @Test
    fun `calculateOscillator with empty input returns empty history`() {
        val result = MarketCalculator.calculateOscillator(
            dates = emptyList(),
            advances = emptyList(),
            declines = emptyList(),
            unchanges = emptyList(),
            totals = emptyList()
        )

        assertTrue(result.dates.isEmpty())
        assertTrue(result.values.isEmpty())
        assertTrue(result.signals.isEmpty())
        assertTrue(result.advanceRatios.isEmpty())
        assertTrue(result.declineRatios.isEmpty())
    }

    @Test
    fun `calculateOscillator with single day data returns one entry`() {
        val result = MarketCalculator.calculateOscillator(
            dates = listOf("2024-01-02"),
            advances = listOf(600),
            declines = listOf(300),
            unchanges = listOf(100),
            totals = listOf(1000)
        )

        assertEquals(1, result.dates.size)
        assertEquals(1, result.values.size)
        assertEquals(1, result.signals.size)
        assertEquals("2024-01-02", result.dates[0])
        // advance ratio = 600/1000 = 0.6
        assertEquals(0.6, result.advanceRatios[0], DELTA)
        // decline ratio = 300/1000 = 0.3
        assertEquals(0.3, result.declineRatios[0], DELTA)
        // EMA with single value is the value itself
        assertEquals(0.6, result.values[0], DELTA)
    }

    @Test
    fun `calculateOscillator with multiple days produces correct ratios`() {
        val result = MarketCalculator.calculateOscillator(
            dates = listOf("2024-01-02", "2024-01-03", "2024-01-04"),
            advances = listOf(800, 500, 200),
            declines = listOf(100, 400, 700),
            unchanges = listOf(100, 100, 100),
            totals = listOf(1000, 1000, 1000)
        )

        assertEquals(3, result.dates.size)
        assertEquals(3, result.advanceRatios.size)
        assertEquals(3, result.declineRatios.size)

        // Day 1: advance ratio = 800/1000 = 0.8
        assertEquals(0.8, result.advanceRatios[0], DELTA)
        // Day 2: advance ratio = 500/1000 = 0.5
        assertEquals(0.5, result.advanceRatios[1], DELTA)
        // Day 3: advance ratio = 200/1000 = 0.2
        assertEquals(0.2, result.advanceRatios[2], DELTA)
    }

    @Test
    fun `calculateOscillator with zero total returns zero ratios`() {
        val result = MarketCalculator.calculateOscillator(
            dates = listOf("2024-01-02"),
            advances = listOf(0),
            declines = listOf(0),
            unchanges = listOf(0),
            totals = listOf(0)
        )

        assertEquals(1, result.dates.size)
        assertEquals(0.0, result.advanceRatios[0], DELTA)
        assertEquals(0.0, result.declineRatios[0], DELTA)
        assertEquals(0.0, result.values[0], DELTA)
    }

    @Test
    fun `calculateOscillator extreme greed signal for high advance ratio`() {
        // Single day with 80% advance ratio -> extreme greed (>= 0.7)
        val result = MarketCalculator.calculateOscillator(
            dates = listOf("2024-01-02"),
            advances = listOf(800),
            declines = listOf(100),
            unchanges = listOf(100),
            totals = listOf(1000)
        )

        // 0.8 >= 0.7 threshold -> EXTREME_GREED
        assertEquals(OscillatorSignal.EXTREME_GREED, result.signals[0])
    }

    @Test
    fun `calculateOscillator greed signal for moderately high advance ratio`() {
        // 60% advance ratio -> greed (>= 0.55 and < 0.7)
        val result = MarketCalculator.calculateOscillator(
            dates = listOf("2024-01-02"),
            advances = listOf(600),
            declines = listOf(300),
            unchanges = listOf(100),
            totals = listOf(1000)
        )

        // 0.6 >= 0.55 and < 0.7 -> GREED
        assertEquals(OscillatorSignal.GREED, result.signals[0])
    }

    @Test
    fun `calculateOscillator neutral signal for balanced market`() {
        // 50% advance ratio -> neutral (> 0.3 and < 0.55 but also > 0.45)
        val result = MarketCalculator.calculateOscillator(
            dates = listOf("2024-01-02"),
            advances = listOf(500),
            declines = listOf(400),
            unchanges = listOf(100),
            totals = listOf(1000)
        )

        // 0.5 > 0.45 (not fear) and < 0.55 (not greed) -> NEUTRAL
        assertEquals(OscillatorSignal.NEUTRAL, result.signals[0])
    }

    @Test
    fun `calculateOscillator fear signal for moderately low advance ratio`() {
        // 40% advance ratio -> fear (<= 0.45 and > 0.3)
        val result = MarketCalculator.calculateOscillator(
            dates = listOf("2024-01-02"),
            advances = listOf(400),
            declines = listOf(500),
            unchanges = listOf(100),
            totals = listOf(1000)
        )

        // 0.4 <= 0.45 and > 0.3 -> FEAR
        assertEquals(OscillatorSignal.FEAR, result.signals[0])
    }

    @Test
    fun `calculateOscillator extreme fear signal for low advance ratio`() {
        // 20% advance ratio -> extreme fear (<= 0.3)
        val result = MarketCalculator.calculateOscillator(
            dates = listOf("2024-01-02"),
            advances = listOf(200),
            declines = listOf(700),
            unchanges = listOf(100),
            totals = listOf(1000)
        )

        // 0.2 <= 0.3 -> EXTREME_FEAR
        assertEquals(OscillatorSignal.EXTREME_FEAR, result.signals[0])
    }

    @Test
    fun `calculateOscillator EMA smoothing produces reasonable values`() {
        // 10 days of data with varying advance ratios
        val dates = (1..10).map { "2024-01-${it.toString().padStart(2, '0')}" }
        val advances = listOf(800, 750, 700, 650, 600, 550, 500, 450, 400, 350)
        val declines = listOf(100, 150, 200, 250, 300, 350, 400, 450, 500, 550)
        val unchanges = List(10) { 100 }
        val totals = List(10) { 1000 }

        val result = MarketCalculator.calculateOscillator(
            dates = dates,
            advances = advances,
            declines = declines,
            unchanges = unchanges,
            totals = totals
        )

        assertEquals(10, result.values.size)

        // First EMA value equals first raw value
        assertEquals(0.8, result.values[0], DELTA)

        // EMA smoothing: values should trend downward but lag behind raw values
        // Each successive value should be less than the previous (downward trend)
        for (i in 1 until result.values.size) {
            assertTrue(
                "EMA should be decreasing: index $i",
                result.values[i] < result.values[i - 1]
            )
        }

        // EMA values should be between first and last raw advance ratios
        for (v in result.values) {
            assertTrue("EMA value should be in reasonable range", v in 0.0..1.0)
        }
    }

    @Test
    fun `calculateOscillator EMA smoothed values differ from raw values`() {
        val dates = (1..5).map { "2024-01-${it.toString().padStart(2, '0')}" }
        // Oscillating raw data: 0.8, 0.2, 0.8, 0.2, 0.8
        val advances = listOf(800, 200, 800, 200, 800)
        val declines = listOf(100, 700, 100, 700, 100)
        val unchanges = List(5) { 100 }
        val totals = List(5) { 1000 }

        val result = MarketCalculator.calculateOscillator(
            dates = dates,
            advances = advances,
            declines = declines,
            unchanges = unchanges,
            totals = totals
        )

        // EMA smoothing should dampen the oscillations
        // After first value, EMA should not reach the extremes of 0.2 or 0.8
        for (i in 1 until result.values.size) {
            assertTrue(
                "EMA should dampen oscillations at index $i: ${result.values[i]}",
                result.values[i] > 0.2 && result.values[i] < 0.8
            )
        }
    }

    @Test
    fun `calculateOscillator result sizes match input size`() {
        val n = 7
        val dates = (1..n).map { "2024-01-${it.toString().padStart(2, '0')}" }
        val advances = List(n) { 500 }
        val declines = List(n) { 400 }
        val unchanges = List(n) { 100 }
        val totals = List(n) { 1000 }

        val result = MarketCalculator.calculateOscillator(
            dates = dates,
            advances = advances,
            declines = declines,
            unchanges = unchanges,
            totals = totals
        )

        assertEquals(n, result.dates.size)
        assertEquals(n, result.values.size)
        assertEquals(n, result.signals.size)
        assertEquals(n, result.advanceRatios.size)
        assertEquals(n, result.declineRatios.size)
    }

    // ========================================================================
    // calculateFearGreed Tests
    // ========================================================================

    /**
     * Generates a list of close prices with a steady upward trend.
     * Starts from a base and increments daily by step.
     */
    private fun generateSteadyCloses(
        days: Int,
        base: Double = 2500.0,
        step: Double = 5.0
    ): List<Double> {
        return (0 until days).map { base + it * step }
    }

    /**
     * Generates a list of close prices with a steady downward trend.
     */
    private fun generateDecliningCloses(
        days: Int,
        base: Double = 2500.0,
        step: Double = 5.0
    ): List<Double> {
        return (0 until days).map { base - it * step }
    }

    /**
     * Generates a flat price series with no change.
     */
    private fun generateFlatCloses(days: Int, price: Double = 2500.0): List<Double> {
        return List(days) { price }
    }

    @Test
    fun `calculateFearGreed with sufficient data returns valid result`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 1_000_000L,
            institutionNetBuy = 500_000L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        assertNotNull(result)
        assertEquals("2024-03-01", result.date)
        assertTrue("Score should be in 0-100 range", result.overallScore in 0.0..100.0)
        assertNotNull(result.signal)
        assertNotNull(result.momentum)
        assertNotNull(result.rsi)
        assertNotNull(result.volatility)
        assertNotNull(result.investorFlow)
        assertNotNull(result.shortSelling)
    }

    @Test
    fun `calculateFearGreed overall score is weighted average of components`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 1_000_000L,
            institutionNetBuy = 500_000L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Each component has weight 0.2, overall should be sum of (score * weight)
        val expectedScore = result.momentum.normalizedScore * result.momentum.weight +
            result.rsi.normalizedScore * result.rsi.weight +
            result.volatility.normalizedScore * result.volatility.weight +
            result.investorFlow.normalizedScore * result.investorFlow.weight +
            result.shortSelling.normalizedScore * result.shortSelling.weight

        assertEquals(
            expectedScore.coerceIn(0.0, 100.0),
            result.overallScore,
            DELTA
        )
    }

    @Test
    fun `calculateFearGreed component weights sum to 1`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        val totalWeight = result.momentum.weight +
            result.rsi.weight +
            result.volatility.weight +
            result.investorFlow.weight +
            result.shortSelling.weight

        assertEquals(1.0, totalWeight, DELTA)
    }

    @Test
    fun `calculateFearGreed all component scores are in 0 to 100 range`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 5_000_000L,
            institutionNetBuy = 3_000_000L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.08
        )

        assertTrue("Momentum score in range", result.momentum.normalizedScore in 0.0..100.0)
        assertTrue("RSI score in range", result.rsi.normalizedScore in 0.0..100.0)
        assertTrue("Volatility score in range", result.volatility.normalizedScore in 0.0..100.0)
        assertTrue("Investor flow score in range", result.investorFlow.normalizedScore in 0.0..100.0)
        assertTrue("Short selling score in range", result.shortSelling.normalizedScore in 0.0..100.0)
    }

    // ========================================================================
    // Momentum calculation tests
    // ========================================================================

    @Test
    fun `calculateFearGreed momentum is high for strong uptrend`() {
        // Strong uptrend: +10% over 20 days from base 2500
        val closes = (0 until 60).map { 2500.0 + it * (250.0 / 59) }
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Uptrend should produce positive momentum
        assertTrue(
            "Momentum should be above 50 for uptrend: ${result.momentum.normalizedScore}",
            result.momentum.normalizedScore > 50.0
        )
    }

    @Test
    fun `calculateFearGreed momentum is low for strong downtrend`() {
        // Strong downtrend: -8% over 20 days
        val closes = (0 until 60).map { 2500.0 - it * (200.0 / 59) }
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        assertTrue(
            "Momentum should be below 50 for downtrend: ${result.momentum.normalizedScore}",
            result.momentum.normalizedScore < 50.0
        )
    }

    @Test
    fun `calculateFearGreed momentum returns default for insufficient data`() {
        // Only 15 days of data, need MOMENTUM_PERIOD(20) + 1 = 21
        val closes = generateSteadyCloses(15)
        val volumes = List(15) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Insufficient data defaults to normalizedScore=50.0
        assertEquals(50.0, result.momentum.normalizedScore, DELTA)
    }

    // ========================================================================
    // RSI calculation tests
    // ========================================================================

    @Test
    fun `calculateFearGreed RSI is high for consistent uptrend`() {
        // Consistently rising prices: RSI should be high
        val closes = generateSteadyCloses(60, step = 10.0)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Consistent uptrend -> all gains, zero losses -> RSI near 100
        assertTrue(
            "RSI should be very high for consistent uptrend: ${result.rsi.normalizedScore}",
            result.rsi.normalizedScore > 80.0
        )
    }

    @Test
    fun `calculateFearGreed RSI is low for consistent downtrend`() {
        // Consistently declining prices: RSI should be low
        val closes = generateDecliningCloses(60, base = 3000.0, step = 5.0)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Consistent downtrend -> all losses, zero gains -> RSI near 0
        assertTrue(
            "RSI should be very low for consistent downtrend: ${result.rsi.normalizedScore}",
            result.rsi.normalizedScore < 20.0
        )
    }

    @Test
    fun `calculateFearGreed RSI returns default for insufficient data`() {
        // Only 10 days, need RSI_PERIOD(14) + 1 = 15
        val closes = generateSteadyCloses(10)
        val volumes = List(10) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        assertEquals(50.0, result.rsi.normalizedScore, DELTA)
    }

    // ========================================================================
    // Volatility calculation tests
    // ========================================================================

    @Test
    fun `calculateFearGreed volatility score is high for low volatility`() {
        // Flat prices = low volatility = high score (greed)
        val closes = generateFlatCloses(60, price = 2500.0)
        // Add tiny fluctuations to avoid zero-division in ln
        val adjustedCloses = closes.mapIndexed { i, p -> p + (i % 2) * 0.01 }
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = adjustedCloses,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Very low volatility -> inverse normalized -> high score
        assertTrue(
            "Volatility score should be high for stable prices: ${result.volatility.normalizedScore}",
            result.volatility.normalizedScore > 80.0
        )
    }

    @Test
    fun `calculateFearGreed volatility score is low for high volatility`() {
        // Wildly oscillating prices = high volatility = low score (fear)
        val closes = (0 until 60).map { i ->
            if (i % 2 == 0) 2500.0 + 200.0 else 2500.0 - 200.0
        }
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // High volatility -> inverse normalized -> low score
        assertTrue(
            "Volatility score should be low for volatile prices: ${result.volatility.normalizedScore}",
            result.volatility.normalizedScore < 30.0
        )
    }

    @Test
    fun `calculateFearGreed volatility returns default for insufficient data`() {
        // Only 15 days, need VOLATILITY_PERIOD(20) + 1 = 21
        val closes = generateSteadyCloses(15)
        val volumes = List(15) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        assertEquals(50.0, result.volatility.normalizedScore, DELTA)
    }

    // ========================================================================
    // InvestorFlow calculation tests
    // ========================================================================

    @Test
    fun `calculateFearGreed investor flow high for strong net buying`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 1_500_000L,   // strong foreign buying
            institutionNetBuy = 1_000_000L, // strong institution buying
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Net buy ratio = (1.5M + 1M) / 100M * 100 = 2.5%
        // Normalized = (2.5 + 2) / 4 * 100 = 112.5 -> clamped to 100
        assertTrue(
            "Investor flow should be high for net buying: ${result.investorFlow.normalizedScore}",
            result.investorFlow.normalizedScore > 70.0
        )
    }

    @Test
    fun `calculateFearGreed investor flow low for strong net selling`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = -1_500_000L,    // strong foreign selling
            institutionNetBuy = -1_000_000L, // strong institution selling
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Net buy ratio = (-1.5M + -1M) / 100M * 100 = -2.5%
        // Normalized = (-2.5 + 2) / 4 * 100 = -12.5 -> clamped to 0
        assertTrue(
            "Investor flow should be low for net selling: ${result.investorFlow.normalizedScore}",
            result.investorFlow.normalizedScore < 30.0
        )
    }

    @Test
    fun `calculateFearGreed investor flow neutral for zero net flow`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Net buy ratio = 0, Normalized = (0 + 2) / 4 * 100 = 50
        assertEquals(50.0, result.investorFlow.normalizedScore, DELTA)
    }

    @Test
    fun `calculateFearGreed investor flow returns default for zero trading value`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 100L,
            institutionNetBuy = 200L,
            totalTradingValue = 0L,
            shortSellingRatio = 0.05
        )

        assertEquals(50.0, result.investorFlow.normalizedScore, DELTA)
    }

    // ========================================================================
    // ShortSelling calculation tests
    // ========================================================================

    @Test
    fun `calculateFearGreed short selling score high for low short ratio`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.01 // 1% short selling - very low
        )

        // Inverse: (15 - 1) / 14 * 100 = 100.0
        assertEquals(100.0, result.shortSelling.normalizedScore, DELTA)
    }

    @Test
    fun `calculateFearGreed short selling score low for high short ratio`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.15 // 15% short selling - very high
        )

        // Inverse: (15 - 15) / 14 * 100 = 0.0
        assertEquals(0.0, result.shortSelling.normalizedScore, DELTA)
    }

    @Test
    fun `calculateFearGreed short selling score mid for typical ratio`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.08 // 8% is mid-range
        )

        // Inverse: (15 - 8) / 14 * 100 = 50.0
        assertEquals(50.0, result.shortSelling.normalizedScore, DELTA)
    }

    @Test
    fun `calculateFearGreed short selling raw value is percentage`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Raw value should be shortSellingRatio * 100 = 5.0
        assertEquals(5.0, result.shortSelling.rawValue, DELTA)
    }

    // ========================================================================
    // Signal classification from overall score
    // ========================================================================

    @Test
    fun `calculateFearGreed signal matches overall score`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 0L,
            institutionNetBuy = 0L,
            totalTradingValue = 100_000_000L,
            shortSellingRatio = 0.05
        )

        // Signal should match FearGreedSignal.fromScore(overallScore)
        val expectedSignal = FearGreedSignal.fromScore(result.overallScore)
        assertEquals(expectedSignal, result.signal)
    }

    @Test
    fun `calculateFearGreed overall score is clamped to 0 to 100`() {
        val closes = generateSteadyCloses(60)
        val volumes = List(60) { 1_000_000L }

        // Even with extreme inputs, score should be clamped
        val result = MarketCalculator.calculateFearGreed(
            date = "2024-03-01",
            indexCloses = closes,
            indexVolumes = volumes,
            foreignNetBuy = 999_999_999L,
            institutionNetBuy = 999_999_999L,
            totalTradingValue = 1L, // extreme ratio
            shortSellingRatio = 0.0
        )

        assertTrue("Score clamped to max 100", result.overallScore <= 100.0)
        assertTrue("Score clamped to min 0", result.overallScore >= 0.0)
    }
}
