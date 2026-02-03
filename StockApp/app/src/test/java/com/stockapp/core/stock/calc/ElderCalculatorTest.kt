package com.stockapp.core.stock.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ElderCalculator.
 * Verifies calculations match Python reference implementations (indicator/elder.py).
 */
class ElderCalculatorTest {

    private val DELTA = 0.001 // Tolerance for floating point comparisons

    // ========================================================================
    // calcEmaNoSma Tests
    // ========================================================================

    @Test
    fun `calcEmaNoSma calculates correctly with simple data`() {
        // Data in newest-first order: 130, 120, 110, 100, 90
        val prices = listOf(130, 120, 110, 100, 90)

        // Chronological order: 90, 100, 110, 120, 130
        // alpha = 2 / (3 + 1) = 0.5
        // EMA[0] = 90
        // EMA[1] = 0.5 * 100 + 0.5 * 90 = 95
        // EMA[2] = 0.5 * 110 + 0.5 * 95 = 102.5
        // EMA[3] = 0.5 * 120 + 0.5 * 102.5 = 111.25
        // EMA[4] = 0.5 * 130 + 0.5 * 111.25 = 120.625

        val result = ElderCalculator.calcEmaNoSma(prices, 3)

        assertEquals(5, result.size)
        assertEquals(120.625, result[0], DELTA) // Newest
        assertEquals(111.25, result[1], DELTA)
        assertEquals(102.5, result[2], DELTA)
        assertEquals(95.0, result[3], DELTA)
        assertEquals(90.0, result[4], DELTA) // Oldest
    }

    @Test
    fun `calcEmaNoSma with single value returns that value`() {
        val prices = listOf(100)
        val result = ElderCalculator.calcEmaNoSma(prices, 13)
        assertEquals(1, result.size)
        assertEquals(100.0, result[0], DELTA)
    }

    @Test
    fun `calcEmaNoSma with empty list returns empty`() {
        val result = ElderCalculator.calcEmaNoSma(emptyList(), 13)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calcEmaNoSma with period 13 calculates correctly`() {
        // Generate test data (newest-first)
        val prices = (0 until 20).map { 100 + it * 5 }.reversed()

        val result = ElderCalculator.calcEmaNoSma(prices, 13)

        assertEquals(20, result.size)
        // EMA should smooth the price series
        // First value should be close to starting value
        assertEquals(100.0, result.last(), DELTA)
    }

    // ========================================================================
    // calcEmaNoSmaDouble Tests
    // ========================================================================

    @Test
    fun `calcEmaNoSmaDouble calculates correctly with double values`() {
        val prices = listOf(130.0, 120.0, 110.0, 100.0, 90.0)
        val result = ElderCalculator.calcEmaNoSmaDouble(prices, 3)

        assertEquals(5, result.size)
        assertEquals(120.625, result[0], DELTA)
    }

    // ========================================================================
    // calcMacdNoSma Tests
    // ========================================================================

    @Test
    fun `calcMacdNoSma returns correct structure`() {
        // Generate 40 periods of data (newest-first)
        val prices = (0 until 40).map { 10000 + it * 50 }.reversed()

        val (macdLine, signalLine, histogram) = ElderCalculator.calcMacdNoSma(prices)

        assertEquals(40, macdLine.size)
        assertEquals(40, signalLine.size)
        assertEquals(40, histogram.size)

        // Histogram = MACD Line - Signal Line
        for (i in 0 until 40) {
            assertEquals(macdLine[i] - signalLine[i], histogram[i], DELTA)
        }
    }

    @Test
    fun `calcMacdNoSma with empty list returns empty`() {
        val (macdLine, signalLine, histogram) = ElderCalculator.calcMacdNoSma(emptyList())

        assertTrue(macdLine.isEmpty())
        assertTrue(signalLine.isEmpty())
        assertTrue(histogram.isEmpty())
    }

    @Test
    fun `calcMacdNoSma produces reasonable MACD values`() {
        // Generate uptrend data
        val prices = (0 until 50).map { 10000 + it * 100 }.reversed()

        val (macdLine, signalLine, histogram) = ElderCalculator.calcMacdNoSma(prices)

        // In uptrend, fast EMA should be above slow EMA, so MACD should be positive
        // After warmup period
        val recentMacd = macdLine.take(10)
        assertTrue("MACD should have positive values in uptrend", recentMacd.any { it > 0 })
    }

    // ========================================================================
    // calcSlope Tests
    // ========================================================================

    @Test
    fun `calcSlope calculates difference from previous value`() {
        // Newest-first: 150, 120, 100
        val values = listOf(150.0, 120.0, 100.0)

        val result = ElderCalculator.calcSlope(values)

        assertEquals(3, result.size)
        // Slope = current - next (previous in time)
        assertEquals(30.0, result[0], DELTA)  // 150 - 120
        assertEquals(20.0, result[1], DELTA)  // 120 - 100
        assertEquals(0.0, result[2], DELTA)   // No next value
    }

    @Test
    fun `calcSlope with single value returns zero`() {
        val values = listOf(100.0)
        val result = ElderCalculator.calcSlope(values)
        assertEquals(1, result.size)
        assertEquals(0.0, result[0], DELTA)
    }

    @Test
    fun `calcSlope with empty list returns empty`() {
        val result = ElderCalculator.calcSlope(emptyList())
        assertTrue(result.isEmpty())
    }

    // ========================================================================
    // calcImpulseColorBySlope Tests
    // ========================================================================

    @Test
    fun `calcImpulseColorBySlope returns green when both slopes positive`() {
        val ema13Slope = listOf(10.0, 5.0, 1.0)
        val histSlope = listOf(0.5, 0.3, 0.1)

        val result = ElderCalculator.calcImpulseColorBySlope(ema13Slope, histSlope)

        assertEquals(3, result.size)
        assertEquals("green", result[0])
        assertEquals("green", result[1])
        assertEquals("green", result[2])
    }

    @Test
    fun `calcImpulseColorBySlope returns red when both slopes negative`() {
        val ema13Slope = listOf(-10.0, -5.0, -1.0)
        val histSlope = listOf(-0.5, -0.3, -0.1)

        val result = ElderCalculator.calcImpulseColorBySlope(ema13Slope, histSlope)

        assertEquals(3, result.size)
        assertEquals("red", result[0])
        assertEquals("red", result[1])
        assertEquals("red", result[2])
    }

    @Test
    fun `calcImpulseColorBySlope returns blue for mixed slopes`() {
        val ema13Slope = listOf(10.0, -5.0, 0.0)
        val histSlope = listOf(-0.5, 0.3, 0.0)

        val result = ElderCalculator.calcImpulseColorBySlope(ema13Slope, histSlope)

        assertEquals(3, result.size)
        assertEquals("blue", result[0])  // EMA positive, hist negative
        assertEquals("blue", result[1])  // EMA negative, hist positive
        assertEquals("blue", result[2])  // Both zero
    }

    @Test
    fun `calcImpulseColorBySlope handles edge case with zeros`() {
        val ema13Slope = listOf(0.0, 10.0, -10.0)
        val histSlope = listOf(0.0, 0.0, 0.0)

        val result = ElderCalculator.calcImpulseColorBySlope(ema13Slope, histSlope)

        assertEquals("blue", result[0])  // Both zero
        assertEquals("blue", result[1])  // Hist zero
        assertEquals("blue", result[2])  // Hist zero
    }

    // ========================================================================
    // calculate Integration Tests
    // ========================================================================

    @Test
    fun `calculate returns null for insufficient data`() {
        val closes = List(20) { 100 }
        val dates = List(20) { "2024-01-${it + 1}" }

        val result = ElderCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNull(result)  // Needs 35 periods minimum
    }

    @Test
    fun `calculate returns result for sufficient data`() {
        // Generate 50 days of data (newest-first)
        val closes = (0 until 50).map { 10000 + (50 - it) * 50 }
        val dates = (0 until 50).map { "2024-01-${50 - it}" }

        val result = ElderCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        assertEquals("005930", result!!.ticker)
        assertEquals("daily", result.timeframe)
        assertEquals(50, result.dates.size)
        assertEquals(50, result.color.size)
        assertEquals(50, result.ema13.size)
        assertEquals(50, result.macdLine.size)
        assertEquals(50, result.signalLine.size)
        assertEquals(50, result.macdHist.size)
        assertEquals(50, result.ema13Slope.size)
        assertEquals(50, result.histSlope.size)
        assertEquals(50, result.close.size)
    }

    @Test
    fun `calculate produces valid color values`() {
        val closes = (0 until 50).map { 10000 + (it % 10) * 100 }
        val dates = (0 until 50).map { "2024-01-${50 - it}" }

        val result = ElderCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        result!!.color.forEach { color ->
            assertTrue(
                "Color '$color' should be green, red, or blue",
                color in listOf("green", "red", "blue")
            )
        }
    }

    // ========================================================================
    // EMA13 Consistency Tests
    // ========================================================================

    @Test
    fun `ema13 follows price trend`() {
        // Strong uptrend data
        val closes = (0 until 50).map { 10000 + (50 - it) * 100 }
        val dates = (0 until 50).map { "2024-01-${50 - it}" }

        val result = ElderCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        // In uptrend, EMA should be below close price (lagging)
        // Check recent values
        val recentEma = result!!.ema13.first()
        val recentClose = result.close.first()
        assertTrue("EMA13 should be below close in uptrend", recentEma < recentClose)
    }

    // ========================================================================
    // MACD Histogram Tests
    // ========================================================================

    @Test
    fun `macdHist equals macdLine minus signalLine`() {
        val closes = (0 until 50).map { 10000 + (it % 20) * 50 }
        val dates = (0 until 50).map { "2024-01-${50 - it}" }

        val result = ElderCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        for (i in result!!.macdHist.indices) {
            val expected = result.macdLine[i] - result.signalLine[i]
            assertEquals(expected, result.macdHist[i], DELTA)
        }
    }

    // ========================================================================
    // Slope Consistency Tests
    // ========================================================================

    @Test
    fun `slopes are calculated correctly from EMA and histogram`() {
        val closes = (0 until 50).map { 10000 + (50 - it) * 50 }
        val dates = (0 until 50).map { "2024-01-${50 - it}" }

        val result = ElderCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        // Verify slope calculation for EMA13
        for (i in 0 until result!!.ema13Slope.size - 1) {
            val expectedSlope = result.ema13[i] - result.ema13[i + 1]
            assertEquals(expectedSlope, result.ema13Slope[i], DELTA)
        }

        // Verify slope calculation for histogram
        for (i in 0 until result.histSlope.size - 1) {
            val expectedSlope = result.macdHist[i] - result.macdHist[i + 1]
            assertEquals(expectedSlope, result.histSlope[i], DELTA)
        }
    }

    // ========================================================================
    // Color Logic Verification Tests
    // ========================================================================

    @Test
    fun `color matches slope conditions`() {
        val closes = (0 until 50).map { 10000 + (it % 15) * 80 }
        val dates = (0 until 50).map { "2024-01-${50 - it}" }

        val result = ElderCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        for (i in result!!.color.indices) {
            val emaSlope = result.ema13Slope[i]
            val histSlope = result.histSlope[i]
            val expectedColor = when {
                emaSlope > 0 && histSlope > 0 -> "green"
                emaSlope < 0 && histSlope < 0 -> "red"
                else -> "blue"
            }
            assertEquals(
                "Color at index $i should match slope conditions",
                expectedColor,
                result.color[i]
            )
        }
    }

    // ========================================================================
    // Weekly Timeframe Tests
    // ========================================================================

    @Test
    fun `calculate works for weekly timeframe`() {
        val closes = (0 until 50).map { 10000 + (50 - it) * 200 }
        val dates = (0 until 50).map { "2024-W${50 - it}" }

        val result = ElderCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "weekly"
        )

        assertNotNull(result)
        assertEquals("weekly", result!!.timeframe)
        assertEquals(50, result.dates.size)
    }
}
