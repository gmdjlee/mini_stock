package com.stockapp.core.stock.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TrendCalculator.
 * Verifies calculations match Python reference implementations (indicator/trend.py).
 */
class TrendCalculatorTest {

    private val DELTA = 0.01 // Tolerance for floating point comparisons

    // ========================================================================
    // calcMa Tests
    // ========================================================================

    @Test
    fun `calcMa with period 5 calculates correctly`() {
        val prices = listOf(100, 102, 104, 106, 108, 110, 112, 114, 116, 118)
        val result = TrendCalculator.calcMa(prices, 5)

        // Index 0: (100+102+104+106+108)/5 = 104
        // Index 1: (102+104+106+108+110)/5 = 106
        assertEquals(10, result.size)
        assertEquals(104, result[0])
        assertEquals(106, result[1])
        assertEquals(108, result[2])
        // Last 4 should be null (insufficient data)
        assertNull(result[6])
        assertNull(result[7])
    }

    @Test
    fun `calcMa with empty list returns empty`() {
        val result = TrendCalculator.calcMa(emptyList(), 5)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calcMa with zero period returns empty`() {
        val result = TrendCalculator.calcMa(listOf(100, 200), 0)
        assertTrue(result.isEmpty())
    }

    // ========================================================================
    // calcMaSignal Tests (Daily)
    // ========================================================================

    @Test
    fun `calcMaSignal returns bullish when MA5 greater than MA20 greater than MA60`() {
        val ma5 = listOf(100, 95, 90)
        val ma20 = listOf(90, 85, 80)
        val ma60 = listOf(80, 75, 70)

        val result = TrendCalculator.calcMaSignal(ma5, ma20, ma60)

        assertEquals(3, result.size)
        assertEquals(1, result[0]) // 100 > 90 > 80 = bullish
        assertEquals(1, result[1]) // 95 > 85 > 75 = bullish
        assertEquals(1, result[2]) // 90 > 80 > 70 = bullish
    }

    @Test
    fun `calcMaSignal returns bearish when MA5 less than MA20 less than MA60`() {
        val ma5 = listOf(70, 75, 80)
        val ma20 = listOf(80, 85, 90)
        val ma60 = listOf(90, 95, 100)

        val result = TrendCalculator.calcMaSignal(ma5, ma20, ma60)

        assertEquals(3, result.size)
        assertEquals(-1, result[0]) // 70 < 80 < 90 = bearish
        assertEquals(-1, result[1]) // 75 < 85 < 95 = bearish
        assertEquals(-1, result[2]) // 80 < 90 < 100 = bearish
    }

    @Test
    fun `calcMaSignal returns neutral for mixed alignment`() {
        val ma5 = listOf(90, 85, 95)
        val ma20 = listOf(85, 90, 90)
        val ma60 = listOf(80, 80, 85)

        val result = TrendCalculator.calcMaSignal(ma5, ma20, ma60)

        // 90 > 85 > 80 = bullish
        assertEquals(1, result[0])
        // 85 < 90 but 90 > 80 = neutral (not aligned)
        assertEquals(0, result[1])
        // 95 > 90 > 85 = bullish
        assertEquals(1, result[2])
    }

    @Test
    fun `calcMaSignal handles null values`() {
        val ma5 = listOf(100, null, 90)
        val ma20 = listOf(90, 85, null)
        val ma60 = listOf(null, 75, 70)

        val result = TrendCalculator.calcMaSignal(ma5, ma20, ma60)

        assertEquals(3, result.size)
        assertEquals(0, result[0]) // null in ma60
        assertEquals(0, result[1]) // null in ma5
        assertEquals(0, result[2]) // null in ma20
    }

    // ========================================================================
    // calcMaSignalWeeklyReference Tests
    // ========================================================================

    @Test
    fun `calcMaSignalWeeklyReference returns buy signal when conditions met`() {
        // Data is newest-first
        val closes = listOf(110, 100, 95, 90, 85)
        val highs = listOf(115, 105, 100, 95, 90)
        val lows = listOf(105, 95, 90, 85, 80)
        val ma10 = listOf(100, 95, 90, 85, 80)
        val cmf = listOf(0.1, -0.1, 0.05, -0.05, 0.0)

        val result = TrendCalculator.calcMaSignalWeeklyReference(closes, highs, lows, ma10, cmf)

        // Index 0: High(115) > PrevHigh(105), Close(110) > MA10(100), CMF(0.1) > 0 → Buy (1)
        assertEquals(1, result[0])
    }

    @Test
    fun `calcMaSignalWeeklyReference returns sell signal when conditions met`() {
        // Data is newest-first
        val closes = listOf(80, 90, 95, 100, 105)
        val highs = listOf(85, 95, 100, 105, 110)
        val lows = listOf(75, 85, 90, 95, 100)
        val ma10 = listOf(90, 90, 90, 90, 90)
        val cmf = listOf(-0.1, 0.1, -0.05, 0.05, 0.0)

        val result = TrendCalculator.calcMaSignalWeeklyReference(closes, highs, lows, ma10, cmf)

        // Index 0: Low(75) < PrevLow(85), Close(80) < MA10(90), CMF(-0.1) < 0 → Sell (-1)
        assertEquals(-1, result[0])
    }

    // ========================================================================
    // calcFearGreed Tests
    // ========================================================================

    @Test
    fun `calcFearGreed returns zeros for insufficient data`() {
        val closes = List(30) { 100 }
        val volumes = List(30) { 1000L }

        val result = TrendCalculator.calcFearGreed(closes, volumes)

        assertEquals(30, result.size)
        // All should be 0.0 since we need 52 periods minimum
        assertTrue(result.all { it == 0.0 })
    }

    @Test
    fun `calcFearGreed calculates correctly with sufficient data`() {
        // Generate 60 days of data (newest-first)
        val basePrice = 10000
        val closes = (0 until 60).map { i ->
            // Simulate upward trend
            basePrice + (60 - i) * 50
        }
        val volumes = (0 until 60).map { 1000000L }

        val result = TrendCalculator.calcFearGreed(closes, volumes)

        assertEquals(60, result.size)
        // Should have some non-zero values after warmup period
        val nonZeroCount = result.count { it != 0.0 }
        assertTrue("Should have non-zero values after warmup", nonZeroCount > 0)
    }

    @Test
    fun `calcFearGreed values are within expected range`() {
        // Generate realistic price data
        val closes = (0 until 100).map { 10000 + (it % 10) * 100 }
        val volumes = (0 until 100).map { 1000000L + (it % 5) * 100000L }

        val result = TrendCalculator.calcFearGreed(closes, volumes)

        // Fear/Greed should be in range approximately -1 to 1.5
        result.filter { it != 0.0 }.forEach { value ->
            assertTrue("FG value $value should be >= -1.5", value >= -1.5)
            assertTrue("FG value $value should be <= 2.0", value <= 2.0)
        }
    }

    // ========================================================================
    // calcTrend Tests
    // ========================================================================

    @Test
    fun `calcTrend returns bullish when two or more indicators are bullish`() {
        val maSignal = listOf(1, 1, 0, 1)
        val cmf = listOf(0.1, 0.2, 0.1, -0.1)  // > 0.05 = bullish
        val fearGreed = listOf(0.6, 0.0, 0.6, 0.6)  // > 0.5 = bullish

        val result = TrendCalculator.calcTrend(maSignal, cmf, fearGreed)

        assertEquals("bullish", result[0])  // MA + CMF = 2 bullish
        assertEquals("bullish", result[1])  // MA + CMF = 2 bullish
        assertEquals("bullish", result[2])  // CMF + FG = 2 bullish
        assertEquals("bullish", result[3])  // MA + FG = 2 bullish
    }

    @Test
    fun `calcTrend returns bearish when two or more indicators are bearish`() {
        val maSignal = listOf(-1, -1, 0, -1)
        val cmf = listOf(-0.1, -0.2, -0.1, 0.1)  // < -0.05 = bearish
        val fearGreed = listOf(-0.6, 0.0, -0.6, -0.6)  // < -0.5 = bearish

        val result = TrendCalculator.calcTrend(maSignal, cmf, fearGreed)

        assertEquals("bearish", result[0])  // MA + CMF = 2 bearish
        assertEquals("bearish", result[1])  // MA + CMF = 2 bearish
        assertEquals("bearish", result[2])  // CMF + FG = 2 bearish
        assertEquals("bearish", result[3])  // MA + FG = 2 bearish
    }

    @Test
    fun `calcTrend returns neutral for mixed signals`() {
        val maSignal = listOf(1, -1, 0)
        val cmf = listOf(-0.1, 0.1, 0.0)
        val fearGreed = listOf(-0.2, 0.2, 0.0)

        val result = TrendCalculator.calcTrend(maSignal, cmf, fearGreed)

        assertEquals("neutral", result[0])  // MA bullish, CMF bearish, FG neutral
        assertEquals("neutral", result[1])  // MA bearish, CMF bullish, FG neutral
        assertEquals("neutral", result[2])  // All neutral
    }

    // ========================================================================
    // calculate Integration Tests
    // ========================================================================

    @Test
    fun `calculate returns null for insufficient data`() {
        val closes = List(30) { 100 }
        val highs = List(30) { 105 }
        val lows = List(30) { 95 }
        val volumes = List(30) { 1000L }
        val dates = List(30) { "2024-01-${it + 1}" }

        val result = TrendCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            highs = highs,
            lows = lows,
            volumes = volumes,
            timeframe = "daily"
        )

        assertNull(result)  // Needs 60 periods for daily
    }

    @Test
    fun `calculate returns result for sufficient daily data`() {
        // Generate 80 days of realistic data (newest-first)
        val closes = (0 until 80).map { 10000 + (80 - it) * 50 }
        val highs = closes.map { it + 100 }
        val lows = closes.map { it - 100 }
        val volumes = (0 until 80).map { 1000000L }
        val dates = (0 until 80).map { "2024-01-${80 - it}" }

        val result = TrendCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            highs = highs,
            lows = lows,
            volumes = volumes,
            timeframe = "daily"
        )

        assertNotNull(result)
        assertEquals("005930", result!!.ticker)
        assertEquals("daily", result.timeframe)
        assertEquals(80, result.dates.size)
        assertEquals(80, result.maSignal.size)
        assertEquals(80, result.cmf.size)
        assertEquals(80, result.fearGreed.size)
        assertEquals(80, result.trend.size)
        assertNotNull(result.ma60)
    }

    @Test
    fun `calculate returns result for sufficient weekly data`() {
        // Generate 60 weeks of data (newest-first)
        val closes = (0 until 60).map { 10000 + (60 - it) * 100 }
        val highs = closes.map { it + 200 }
        val lows = closes.map { it - 200 }
        val volumes = (0 until 60).map { 5000000L }
        val dates = (0 until 60).map { "2024-W${60 - it}" }

        val result = TrendCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            highs = highs,
            lows = lows,
            volumes = volumes,
            timeframe = "weekly"
        )

        assertNotNull(result)
        assertEquals("005930", result!!.ticker)
        assertEquals("weekly", result.timeframe)
        assertEquals(60, result.dates.size)
        assertNull(result.ma60)  // Weekly doesn't use MA60
    }

    // ========================================================================
    // CMF Boundary Tests
    // ========================================================================

    @Test
    fun `cmf values are within -1 to 1 range`() {
        // Generate 80 days of data with varying volume
        val closes = (0 until 80).map { 10000 + (it % 20) * 100 }
        val highs = closes.map { it + 200 }
        val lows = closes.map { it - 200 }
        val volumes = (0 until 80).map { 1000000L + (it % 10) * 500000L }
        val dates = (0 until 80).map { "2024-01-${80 - it}" }

        val result = TrendCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            highs = highs,
            lows = lows,
            volumes = volumes,
            timeframe = "daily"
        )

        assertNotNull(result)
        result!!.cmf.forEach { cmf ->
            assertTrue("CMF $cmf should be >= -1", cmf >= -1.0)
            assertTrue("CMF $cmf should be <= 1", cmf <= 1.0)
        }
    }

    // ========================================================================
    // MA Signal Consistency Tests
    // ========================================================================

    @Test
    fun `maSignal values are within valid range`() {
        val closes = (0 until 80).map { 10000 + (it % 20) * 100 }
        val highs = closes.map { it + 200 }
        val lows = closes.map { it - 200 }
        val volumes = (0 until 80).map { 1000000L }
        val dates = (0 until 80).map { "2024-01-${80 - it}" }

        val result = TrendCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            highs = highs,
            lows = lows,
            volumes = volumes,
            timeframe = "daily"
        )

        assertNotNull(result)
        result!!.maSignal.forEach { signal ->
            assertTrue("MA signal $signal should be -1, 0, or 1", signal in listOf(-1, 0, 1))
        }
    }

    // ========================================================================
    // Trend Consistency Tests
    // ========================================================================

    @Test
    fun `trend values are valid strings`() {
        val closes = (0 until 80).map { 10000 + (it % 20) * 100 }
        val highs = closes.map { it + 200 }
        val lows = closes.map { it - 200 }
        val volumes = (0 until 80).map { 1000000L }
        val dates = (0 until 80).map { "2024-01-${80 - it}" }

        val result = TrendCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            highs = highs,
            lows = lows,
            volumes = volumes,
            timeframe = "daily"
        )

        assertNotNull(result)
        result!!.trend.forEach { trend ->
            assertTrue(
                "Trend '$trend' should be bullish, neutral, or bearish",
                trend in listOf("bullish", "neutral", "bearish")
            )
        }
    }
}
