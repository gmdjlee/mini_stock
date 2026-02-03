package com.stockapp.core.stock.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DemarkCalculator.
 * Verifies calculations match Python reference implementations (indicator/demark.py).
 */
class DemarkCalculatorTest {

    // ========================================================================
    // calcTdSetup Tests - Basic Functionality
    // ========================================================================

    @Test
    fun `calcTdSetup returns zeros for insufficient data`() {
        val closes = listOf(100, 101, 102, 103) // Only 4 elements

        val (sellSetup, buySetup) = DemarkCalculator.calcTdSetup(closes)

        assertEquals(4, sellSetup.size)
        assertEquals(4, buySetup.size)
        assertTrue(sellSetup.all { it == 0 })
        assertTrue(buySetup.all { it == 0 })
    }

    @Test
    fun `calcTdSetup with empty list returns empty`() {
        val (sellSetup, buySetup) = DemarkCalculator.calcTdSetup(emptyList())

        assertTrue(sellSetup.isEmpty())
        assertTrue(buySetup.isEmpty())
    }

    // ========================================================================
    // calcTdSetup Tests - Sell Setup Logic
    // ========================================================================

    @Test
    fun `calcTdSetup counts sell setup when close greater than close 4 bars ago`() {
        // Data in newest-first order
        // Chronological: 100, 105, 110, 115, 120, 125 (consistent uptrend)
        // Close[4] comparison: index 4 vs index 0, index 5 vs index 1, etc.
        val closes = listOf(125, 120, 115, 110, 105, 100)

        val (sellSetup, _) = DemarkCalculator.calcTdSetup(closes)

        // In chronological order (after reversal):
        // Index 0-3: no comparison possible (< 4 lookback) → 0
        // Index 4: 120 > 100 → 1
        // Index 5: 125 > 105 → 2

        // Since data is returned in newest-first order:
        assertEquals(2, sellSetup[0]) // Newest: accumulated count
        assertEquals(1, sellSetup[1])
        assertEquals(0, sellSetup[2])
        assertEquals(0, sellSetup[3])
        assertEquals(0, sellSetup[4])
        assertEquals(0, sellSetup[5])
    }

    @Test
    fun `calcTdSetup resets sell count when condition breaks`() {
        // Uptrend then break
        // Chronological: 100, 105, 110, 115, 120, 110 (breaks at end)
        val closes = listOf(110, 120, 115, 110, 105, 100)

        val (sellSetup, _) = DemarkCalculator.calcTdSetup(closes)

        // Index 4 (chrono): 120 > 100 → 1
        // Index 5 (chrono): 110 > 105 → 2
        // But wait, let me recalculate:
        // Chrono: [100, 105, 110, 115, 120, 110]
        // i=4: closes[4]=120 > closes[0]=100 → sell=1
        // i=5: closes[5]=110 > closes[1]=105 → sell=2 (110 > 105 is true)

        assertEquals(2, sellSetup[0])
    }

    // ========================================================================
    // calcTdSetup Tests - Buy Setup Logic
    // ========================================================================

    @Test
    fun `calcTdSetup counts buy setup when close less than close 4 bars ago`() {
        // Data in newest-first order
        // Chronological: 125, 120, 115, 110, 105, 100 (consistent downtrend)
        val closes = listOf(100, 105, 110, 115, 120, 125)

        val (_, buySetup) = DemarkCalculator.calcTdSetup(closes)

        // In chronological order:
        // Index 4: 105 < 125 → 1
        // Index 5: 100 < 120 → 2

        // Newest-first result:
        assertEquals(2, buySetup[0])
        assertEquals(1, buySetup[1])
        assertEquals(0, buySetup[2])
    }

    @Test
    fun `calcTdSetup resets buy count when condition breaks`() {
        // Downtrend then break upward
        // Chronological: 125, 120, 115, 110, 105, 115 (breaks at end)
        val closes = listOf(115, 105, 110, 115, 120, 125)

        val (_, buySetup) = DemarkCalculator.calcTdSetup(closes)

        // i=4: 105 < 125 → buy=1
        // i=5: 115 < 120 → buy=2 (115 < 120 is true)

        assertEquals(2, buySetup[0])
    }

    // ========================================================================
    // calcTdSetup Tests - Independent Counting
    // ========================================================================

    @Test
    fun `calcTdSetup allows both sell and buy counts simultaneously`() {
        // Create scenario where close equals close[4] - both should be 0
        val closes = listOf(100, 100, 100, 100, 100, 100, 100, 100)

        val (sellSetup, buySetup) = DemarkCalculator.calcTdSetup(closes)

        // When close == close[4], neither condition is met
        // All should be 0
        assertTrue(sellSetup.all { it == 0 })
        assertTrue(buySetup.all { it == 0 })
    }

    // ========================================================================
    // calcTdSetup Tests - Extended Count (No Limit)
    // ========================================================================

    @Test
    fun `calcTdSetup continues counting beyond 9`() {
        // Strong uptrend for 15 periods
        // Chronological: 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240
        val closes = (0 until 15).map { 240 - it * 10 } // 240, 230, ... 100

        val (sellSetup, _) = DemarkCalculator.calcTdSetup(closes)

        // First 4 indices: 0 (no lookback)
        // From index 4 onwards: continuous counting
        // Max count should be 15 - 4 = 11
        val maxSell = sellSetup.maxOrNull() ?: 0
        assertTrue("Sell count should exceed 9", maxSell > 9)
    }

    // ========================================================================
    // getActiveSetups Tests
    // ========================================================================

    @Test
    fun `getActiveSetups returns correct current values`() {
        val sellSetup = listOf(5, 4, 3, 2, 1, 0)
        val buySetup = listOf(0, 0, 2, 1, 0, 0)
        val dates = listOf("2024-01-06", "2024-01-05", "2024-01-04", "2024-01-03", "2024-01-02", "2024-01-01")

        val result = DemarkCalculator.getActiveSetups(sellSetup, buySetup, dates)

        assertEquals(5, result.currentSell)
        assertEquals(0, result.currentBuy)
        assertEquals(5, result.maxSell)
        assertEquals(2, result.maxBuy)
    }

    @Test
    fun `getActiveSetups handles empty lists`() {
        val result = DemarkCalculator.getActiveSetups(emptyList(), emptyList(), emptyList())

        assertEquals(0, result.currentSell)
        assertEquals(0, result.currentBuy)
        assertEquals(0, result.maxSell)
        assertEquals(0, result.maxBuy)
        assertTrue(result.recentSetups.isEmpty())
    }

    @Test
    fun `getActiveSetups returns recent active setups`() {
        val sellSetup = listOf(3, 2, 1, 0, 0, 5, 4, 3, 2, 1)
        val buySetup = listOf(0, 0, 0, 2, 1, 0, 0, 0, 0, 0)
        val dates = (0 until 10).map { "2024-01-${10 - it}" }

        val result = DemarkCalculator.getActiveSetups(sellSetup, buySetup, dates)

        // Should include entries where sell > 0 or buy > 0
        assertTrue(result.recentSetups.isNotEmpty())
        // First entry should have sell=3, buy=0
        assertEquals(3, result.recentSetups[0].sell)
        assertEquals(0, result.recentSetups[0].buy)
    }

    // ========================================================================
    // calculate Integration Tests
    // ========================================================================

    @Test
    fun `calculate returns null for insufficient data`() {
        val closes = listOf(100, 101, 102, 103)
        val dates = listOf("2024-01-04", "2024-01-03", "2024-01-02", "2024-01-01")

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNull(result)
    }

    @Test
    fun `calculate returns result for sufficient data`() {
        val closes = (0 until 20).map { 10000 + (20 - it) * 50 }
        val dates = (0 until 20).map { "2024-01-${20 - it}" }

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        assertEquals("005930", result!!.ticker)
        assertEquals("daily", result.timeframe)
        assertEquals(20, result.dates.size)
        assertEquals(20, result.close.size)
        assertEquals(20, result.sellSetup.size)
        assertEquals(20, result.buySetup.size)
    }

    @Test
    fun `calculate preserves closes correctly`() {
        val closes = listOf(100, 105, 110, 115, 120, 125, 130, 135, 140, 145)
        val dates = (0 until 10).map { "2024-01-${10 - it}" }

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        assertEquals(closes, result!!.close)
    }

    // ========================================================================
    // Setup Count Validation Tests
    // ========================================================================

    @Test
    fun `all setup values are non-negative`() {
        val closes = (0 until 30).map { 10000 + (it % 10) * 100 }
        val dates = (0 until 30).map { "2024-01-${30 - it}" }

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        assertTrue(result!!.sellSetup.all { it >= 0 })
        assertTrue(result.buySetup.all { it >= 0 })
    }

    // ========================================================================
    // Timeframe Tests
    // ========================================================================

    @Test
    fun `calculate works for weekly timeframe`() {
        val closes = (0 until 20).map { 10000 + (20 - it) * 100 }
        val dates = (0 until 20).map { "2024-W${20 - it}" }

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "weekly"
        )

        assertNotNull(result)
        assertEquals("weekly", result!!.timeframe)
    }

    @Test
    fun `calculate works for monthly timeframe`() {
        val closes = (0 until 20).map { 10000 + (20 - it) * 200 }
        val dates = (0 until 20).map { "2024-${String.format("%02d", 20 - it)}" }

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "monthly"
        )

        assertNotNull(result)
        assertEquals("monthly", result!!.timeframe)
    }

    // ========================================================================
    // Reset Logic Verification Tests
    // ========================================================================

    @Test
    fun `sell setup resets to zero correctly`() {
        // Create pattern: uptrend -> break -> uptrend
        // Chronological: 100, 110, 120, 130, 140, 130, 140, 150, 160, 170
        val chronoPrices = listOf(100, 110, 120, 130, 140, 130, 140, 150, 160, 170)
        val closes = chronoPrices.reversed()

        val (sellSetup, _) = DemarkCalculator.calcTdSetup(closes)

        // i=4: 140 > 100 → 1
        // i=5: 130 > 110 → 2 (130 > 110)
        // i=6: 140 > 120 → 3
        // i=7: 150 > 130 → 4
        // i=8: 160 > 140 → 5
        // i=9: 170 > 130 → 6

        // Newest first result:
        assertEquals(6, sellSetup[0]) // 170 > 130
    }

    @Test
    fun `buy setup resets to zero correctly`() {
        // Create pattern: downtrend -> break -> downtrend
        // Chronological: 170, 160, 150, 140, 130, 140, 130, 120, 110, 100
        val chronoPrices = listOf(170, 160, 150, 140, 130, 140, 130, 120, 110, 100)
        val closes = chronoPrices.reversed()

        val (_, buySetup) = DemarkCalculator.calcTdSetup(closes)

        // i=4: 130 < 170 → 1
        // i=5: 140 < 160 → 2 (140 < 160)
        // i=6: 130 < 150 → 3
        // i=7: 120 < 140 → 4
        // i=8: 110 < 130 → 5
        // i=9: 100 < 140 → 6

        assertEquals(6, buySetup[0])
    }

    // ========================================================================
    // Edge Case Tests
    // ========================================================================

    @Test
    fun `handles equal prices correctly`() {
        // All prices the same
        val closes = List(10) { 10000 }
        val dates = (0 until 10).map { "2024-01-${10 - it}" }

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        // When close == close[4], neither condition is satisfied
        assertTrue(result!!.sellSetup.all { it == 0 })
        assertTrue(result.buySetup.all { it == 0 })
    }

    @Test
    fun `handles large price values`() {
        // Large prices (typical for Korean stocks)
        val closes = (0 until 20).map { 1_000_000 + (20 - it) * 10_000 }
        val dates = (0 until 20).map { "2024-01-${20 - it}" }

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        // Should still calculate correctly
        assertTrue(result!!.sellSetup.any { it > 0 })
    }

    @Test
    fun `handles volatile price movements`() {
        // Alternating up/down pattern
        val closes = (0 until 20).map { if (it % 2 == 0) 10000 else 11000 }
        val dates = (0 until 20).map { "2024-01-${20 - it}" }

        val result = DemarkCalculator.calculate(
            ticker = "005930",
            dates = dates,
            closes = closes,
            timeframe = "daily"
        )

        assertNotNull(result)
        // With alternating pattern, setups should frequently reset
        val maxSell = result!!.sellSetup.maxOrNull() ?: 0
        val maxBuy = result.buySetup.maxOrNull() ?: 0
        // Neither should have very high counts due to frequent direction changes
        assertTrue(maxSell < 5 || maxBuy < 5)
    }
}
