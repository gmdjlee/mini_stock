package com.stockapp.core.stock.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for MathUtil.
 * Verifies calculations match Python reference implementations.
 */
class MathUtilTest {

    private val DELTA = 0.0001 // Tolerance for floating point comparisons

    // ========================================================================
    // Rolling Sum Tests
    // ========================================================================

    @Test
    fun `rollingSum with window 5 calculates correctly`() {
        val values = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L)
        val result = MathUtil.rollingSum(values, 5)

        // Index 0: just 10 = 10
        // Index 1: 10+20 = 30
        // Index 2: 10+20+30 = 60
        // Index 3: 10+20+30+40 = 100
        // Index 4: 10+20+30+40+50 = 150
        // Index 5: 20+30+40+50+60 = 200
        // Index 6: 30+40+50+60+70 = 250
        assertEquals(listOf(10L, 30L, 60L, 100L, 150L, 200L, 250L), result)
    }

    @Test
    fun `rollingSum with window 1 returns original values`() {
        val values = listOf(10L, 20L, 30L)
        val result = MathUtil.rollingSum(values, 1)
        assertEquals(values, result)
    }

    @Test
    fun `rollingSum with empty list returns empty`() {
        val result = MathUtil.rollingSum(emptyList(), 5)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `rollingSum with zero window returns empty`() {
        val result = MathUtil.rollingSum(listOf(1L, 2L, 3L), 0)
        assertTrue(result.isEmpty())
    }

    // ========================================================================
    // SMA Tests
    // ========================================================================

    @Test
    fun `sma with period 3 calculates correctly`() {
        val prices = listOf(100, 110, 120, 130, 140)
        val result = MathUtil.sma(prices, 3)

        // Index 0: (100+110+120)/3 = 110
        // Index 1: (110+120+130)/3 = 120
        // Index 2: (120+130+140)/3 = 130
        // Index 3, 4: insufficient data
        assertEquals(110.0, result[0]!!, DELTA)
        assertEquals(120.0, result[1]!!, DELTA)
        assertEquals(130.0, result[2]!!, DELTA)
        assertEquals(null, result[3])
        assertEquals(null, result[4])
    }

    @Test
    fun `sma with period greater than size returns all nulls`() {
        val prices = listOf(100, 200)
        val result = MathUtil.sma(prices, 5)
        assertTrue(result.all { it == null })
    }

    @Test
    fun `sma with empty list returns empty`() {
        val result = MathUtil.sma(emptyList(), 3)
        assertTrue(result.isEmpty())
    }

    // ========================================================================
    // EMA Tests
    // ========================================================================

    @Test
    fun `ema with period 3 calculates correctly`() {
        // Data in newest-first order
        val prices = listOf(130.0, 120.0, 110.0, 100.0, 90.0)

        // Chronological (oldest-first): 90, 100, 110, 120, 130
        // alpha = 2 / (3 + 1) = 0.5
        // EMA[0] = 90
        // EMA[1] = 0.5 * 100 + 0.5 * 90 = 95
        // EMA[2] = 0.5 * 110 + 0.5 * 95 = 102.5
        // EMA[3] = 0.5 * 120 + 0.5 * 102.5 = 111.25
        // EMA[4] = 0.5 * 130 + 0.5 * 111.25 = 120.625

        val result = MathUtil.ema(prices, 3)

        assertEquals(5, result.size)
        assertEquals(120.625, result[0], DELTA) // Newest
        assertEquals(111.25, result[1], DELTA)
        assertEquals(102.5, result[2], DELTA)
        assertEquals(95.0, result[3], DELTA)
        assertEquals(90.0, result[4], DELTA) // Oldest
    }

    @Test
    fun `ema with single value returns that value`() {
        val prices = listOf(100.0)
        val result = MathUtil.ema(prices, 5)
        assertEquals(1, result.size)
        assertEquals(100.0, result[0], DELTA)
    }

    @Test
    fun `ema with empty list returns empty`() {
        val result = MathUtil.ema(emptyList(), 3)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `emaInt converts int prices and calculates correctly`() {
        val prices = listOf(130, 120, 110, 100, 90)
        val result = MathUtil.emaInt(prices, 3)
        assertEquals(120.625, result[0], DELTA)
    }

    // ========================================================================
    // CMF Tests
    // ========================================================================

    @Test
    fun `cmf calculates correctly with simple data`() {
        // Creating simple test data where CMF should be predictable
        // When close = high, MFM = 1
        // When close = low, MFM = -1
        // When close = midpoint, MFM = 0

        val highs = listOf(110, 120, 130, 140, 150)
        val lows = listOf(90, 100, 110, 120, 130)
        val closes = listOf(110, 120, 130, 140, 150) // All at high
        val volumes = listOf(1000L, 1000L, 1000L, 1000L, 1000L)

        val result = MathUtil.cmf(highs, lows, closes, volumes, 3)

        // MFM = ((close-low) - (high-close)) / (high-low)
        // For close=high: ((high-low) - 0) / (high-low) = 1
        // CMF = Sum(MFM * Vol) / Sum(Vol) = (1000+1000+1000) / 3000 = 1.0
        assertEquals(5, result.size)
        assertEquals(1.0, result[0], DELTA) // Period 0-2
        assertEquals(1.0, result[1], DELTA) // Period 1-3
        assertEquals(1.0, result[2], DELTA) // Period 2-4
    }

    @Test
    fun `cmf returns 0 when range is 0`() {
        val highs = listOf(100, 100, 100)
        val lows = listOf(100, 100, 100)
        val closes = listOf(100, 100, 100)
        val volumes = listOf(1000L, 1000L, 1000L)

        val result = MathUtil.cmf(highs, lows, closes, volumes, 2)
        assertTrue(result.all { it == 0.0 })
    }

    @Test
    fun `cmf with mismatched list sizes returns empty`() {
        val highs = listOf(100, 110)
        val lows = listOf(90)
        val closes = listOf(100, 110)
        val volumes = listOf(1000L, 1000L)

        val result = MathUtil.cmf(highs, lows, closes, volumes, 2)
        assertTrue(result.isEmpty())
    }

    // ========================================================================
    // Standard Deviation Tests
    // ========================================================================

    @Test
    fun `std calculates population standard deviation`() {
        val values = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        // Mean = 5
        // Variance = ((2-5)^2 + (4-5)^2 + ... + (9-5)^2) / 8 = 4
        // Std = sqrt(4) = 2

        val result = MathUtil.std(values)
        assertEquals(2.0, result, DELTA)
    }

    @Test
    fun `std with single value returns 0`() {
        val result = MathUtil.std(listOf(100.0))
        assertEquals(0.0, result, DELTA)
    }

    @Test
    fun `std with empty list returns 0`() {
        val result = MathUtil.std(emptyList())
        assertEquals(0.0, result, DELTA)
    }

    @Test
    fun `std with identical values returns 0`() {
        val values = listOf(5.0, 5.0, 5.0, 5.0)
        val result = MathUtil.std(values)
        assertEquals(0.0, result, DELTA)
    }

    // ========================================================================
    // Rolling Mean Tests
    // ========================================================================

    @Test
    fun `rollingMean with window 3 calculates correctly`() {
        val values = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        val result = MathUtil.rollingMean(values, 3)

        // Index 0: 10 / 1 = 10
        // Index 1: (10+20) / 2 = 15
        // Index 2: (10+20+30) / 3 = 20
        // Index 3: (20+30+40) / 3 = 30
        // Index 4: (30+40+50) / 3 = 40
        assertEquals(10.0, result[0], DELTA)
        assertEquals(15.0, result[1], DELTA)
        assertEquals(20.0, result[2], DELTA)
        assertEquals(30.0, result[3], DELTA)
        assertEquals(40.0, result[4], DELTA)
    }

    // ========================================================================
    // Rolling Min/Max Tests
    // ========================================================================

    @Test
    fun `rollingMin finds minimum in window`() {
        val values = listOf(50, 30, 40, 20, 60)
        val result = MathUtil.rollingMin(values, 3)

        // Index 0: min(50, 30, 40) = 30
        // Index 1: min(30, 40, 20) = 20
        // Index 2: min(40, 20, 60) = 20
        // Index 3, 4: insufficient data
        assertEquals(30, result[0])
        assertEquals(20, result[1])
        assertEquals(20, result[2])
        assertEquals(null, result[3])
        assertEquals(null, result[4])
    }

    @Test
    fun `rollingMax finds maximum in window`() {
        val values = listOf(50, 30, 40, 20, 60)
        val result = MathUtil.rollingMax(values, 3)

        assertEquals(50, result[0])
        assertEquals(40, result[1])
        assertEquals(60, result[2])
        assertEquals(null, result[3])
        assertEquals(null, result[4])
    }

    // ========================================================================
    // Percentage Change Tests
    // ========================================================================

    @Test
    fun `pctChange calculates percentage changes`() {
        // Newest-first: 150, 120, 100
        val values = listOf(150.0, 120.0, 100.0)
        val result = MathUtil.pctChange(values)

        // Index 0: no previous = 0
        // Index 1: (150-120)/120 = 0.25 (newer value - older value)
        // Index 2: (120-100)/100 = 0.2
        assertEquals(3, result.size)
        assertEquals(0.0, result[0], DELTA)
        assertEquals(0.25, result[1], DELTA)
        assertEquals(0.2, result[2], DELTA)
    }

    @Test
    fun `pctChange with single value returns 0`() {
        val result = MathUtil.pctChange(listOf(100.0))
        assertEquals(listOf(0.0), result)
    }

    @Test
    fun `pctChange handles zero division`() {
        val values = listOf(100.0, 0.0)
        val result = MathUtil.pctChange(values)
        assertEquals(0.0, result[1], DELTA) // Division by zero returns 0
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    fun `ln returns 0 for non-positive values`() {
        assertEquals(0.0, MathUtil.ln(0.0), DELTA)
        assertEquals(0.0, MathUtil.ln(-1.0), DELTA)
    }

    @Test
    fun `ln calculates natural log for positive values`() {
        assertEquals(0.0, MathUtil.ln(1.0), DELTA)
        assertTrue(MathUtil.ln(2.718281828) - 1.0 < DELTA)
    }

    // ========================================================================
    // Python Reference Comparison Tests
    // ========================================================================

    @Test
    fun `ema matches Python pandas ewm adjust=False calculation`() {
        // Python reference calculation:
        // import pandas as pd
        // s = pd.Series([90, 100, 110, 120, 130])
        // s.ewm(span=13, adjust=False).mean()
        //
        // For span=13, alpha = 2/(13+1) = 0.142857...

        val prices = listOf(130.0, 120.0, 110.0, 100.0, 90.0) // newest-first
        val result = MathUtil.ema(prices, 13)

        // Manually calculated with alpha = 2/14 ≈ 0.142857:
        // Start with 90, then each step: new = alpha*price + (1-alpha)*prev
        val alpha = 2.0 / 14.0
        val chronoPrices = prices.reversed()
        var ema = chronoPrices[0]
        val expected = mutableListOf(ema)
        for (i in 1 until chronoPrices.size) {
            ema = alpha * chronoPrices[i] + (1 - alpha) * ema
            expected.add(ema)
        }

        assertEquals(expected.last(), result[0], DELTA) // Newest should match
    }

    @Test
    fun `cmf typical range is between -1 and 1`() {
        // Generate realistic OHLCV data
        val highs = listOf(105, 108, 106, 110, 107, 109, 111, 108, 110, 112)
        val lows = listOf(95, 98, 96, 100, 97, 99, 101, 98, 100, 102)
        val closes = listOf(100, 105, 102, 108, 104, 107, 109, 106, 108, 111)
        val volumes = (1..10).map { 1000000L }

        val result = MathUtil.cmf(highs, lows, closes, volumes, 5)

        result.forEach { cmf ->
            assertTrue("CMF $cmf should be between -1 and 1", abs(cmf) <= 1.0)
        }
    }
}
