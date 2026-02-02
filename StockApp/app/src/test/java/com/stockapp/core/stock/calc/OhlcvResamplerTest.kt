package com.stockapp.core.stock.calc

import com.stockapp.core.stock.calc.OhlcvResampler.OhlcvBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for OhlcvResampler.
 */
class OhlcvResamplerTest {

    // ========================================================================
    // Weekly Resampling Tests
    // ========================================================================

    @Test
    fun `toWeekly aggregates daily bars into weekly`() {
        // Create 5 daily bars for one week (Mon-Fri)
        // Dates: 2026-01-05 (Mon) to 2026-01-09 (Fri)
        val dailyBars = listOf(
            OhlcvBar("20260109", 105, 110, 104, 108, 5000L), // Friday (newest)
            OhlcvBar("20260108", 104, 108, 103, 105, 4000L), // Thursday
            OhlcvBar("20260107", 103, 106, 102, 104, 3500L), // Wednesday
            OhlcvBar("20260106", 102, 105, 101, 103, 3000L), // Tuesday
            OhlcvBar("20260105", 100, 103, 99, 102, 2500L),  // Monday (oldest)
        )

        val weeklyBars = OhlcvResampler.toWeekly(dailyBars)

        assertEquals(1, weeklyBars.size)

        val weekBar = weeklyBars[0]
        assertEquals(100, weekBar.open)  // Monday's open
        assertEquals(110, weekBar.high)  // Week's high (Friday)
        assertEquals(99, weekBar.low)    // Week's low (Monday)
        assertEquals(108, weekBar.close) // Friday's close
        assertEquals(18000L, weekBar.volume) // Sum of all volumes
    }

    @Test
    fun `toWeekly handles multiple weeks`() {
        // Two different weeks
        val dailyBars = listOf(
            // Week 2 (Jan 12-16)
            OhlcvBar("20260116", 115, 120, 114, 118, 5000L),
            OhlcvBar("20260115", 114, 118, 113, 115, 4000L),
            // Week 1 (Jan 5-9)
            OhlcvBar("20260109", 105, 110, 104, 108, 5000L),
            OhlcvBar("20260108", 104, 108, 103, 105, 4000L),
        )

        val weeklyBars = OhlcvResampler.toWeekly(dailyBars)

        // Should produce 2 weekly bars, newest first
        assertEquals(2, weeklyBars.size)
        assertTrue(weeklyBars[0].date >= weeklyBars[1].date)
    }

    @Test
    fun `toWeekly handles empty list`() {
        val result = OhlcvResampler.toWeekly(emptyList())
        assertTrue(result.isEmpty())
    }

    // ========================================================================
    // Monthly Resampling Tests
    // ========================================================================

    @Test
    fun `toMonthly aggregates daily bars into monthly`() {
        // Create bars spanning one month
        val dailyBars = listOf(
            OhlcvBar("20260131", 115, 120, 114, 118, 5000L), // End of Jan
            OhlcvBar("20260115", 108, 112, 107, 110, 4000L), // Mid Jan
            OhlcvBar("20260102", 100, 105, 99, 104, 3000L),  // Start of Jan
        )

        val monthlyBars = OhlcvResampler.toMonthly(dailyBars)

        assertEquals(1, monthlyBars.size)

        val monthBar = monthlyBars[0]
        assertEquals(100, monthBar.open)  // First day's open
        assertEquals(120, monthBar.high)  // Month's high
        assertEquals(99, monthBar.low)    // Month's low
        assertEquals(118, monthBar.close) // Last day's close
        assertEquals(12000L, monthBar.volume) // Sum of all volumes
    }

    @Test
    fun `toMonthly handles multiple months`() {
        val dailyBars = listOf(
            OhlcvBar("20260215", 125, 130, 124, 128, 5000L), // February
            OhlcvBar("20260131", 115, 120, 114, 118, 5000L), // January
            OhlcvBar("20260115", 108, 112, 107, 110, 4000L), // January
        )

        val monthlyBars = OhlcvResampler.toMonthly(dailyBars)

        assertEquals(2, monthlyBars.size)
        assertTrue(monthlyBars[0].date >= monthlyBars[1].date)
    }

    @Test
    fun `toMonthly handles empty list`() {
        val result = OhlcvResampler.toMonthly(emptyList())
        assertTrue(result.isEmpty())
    }

    // ========================================================================
    // Validation Tests
    // ========================================================================

    @Test
    fun `isValidBar returns true for valid bar`() {
        val bar = OhlcvBar("20260115", 100, 110, 95, 105, 1000L)
        assertTrue(OhlcvResampler.isValidBar(bar))
    }

    @Test
    fun `isValidBar returns false when high lower than close`() {
        val bar = OhlcvBar("20260115", 100, 95, 90, 105, 1000L)
        assertFalse(OhlcvResampler.isValidBar(bar))
    }

    @Test
    fun `isValidBar returns false when low higher than close`() {
        val bar = OhlcvBar("20260115", 100, 110, 120, 105, 1000L)
        assertFalse(OhlcvResampler.isValidBar(bar))
    }

    @Test
    fun `isValidBar returns false for invalid date format`() {
        val bar = OhlcvBar("2026-01-15", 100, 110, 95, 105, 1000L)
        assertFalse(OhlcvResampler.isValidBar(bar))
    }

    @Test
    fun `isValidBar returns false for zero prices`() {
        val bar = OhlcvBar("20260115", 0, 110, 95, 105, 1000L)
        assertFalse(OhlcvResampler.isValidBar(bar))
    }

    @Test
    fun `filterValidBars removes invalid bars`() {
        val bars = listOf(
            OhlcvBar("20260115", 100, 110, 95, 105, 1000L),   // Valid
            OhlcvBar("20260114", 0, 110, 95, 105, 1000L),     // Invalid (zero open)
            OhlcvBar("20260113", 100, 110, 95, 105, 1000L),   // Valid
        )

        val filtered = OhlcvResampler.filterValidBars(bars)
        assertEquals(2, filtered.size)
    }

    // ========================================================================
    // Returns Calculation Tests
    // ========================================================================

    @Test
    fun `calculateReturns computes percentage changes`() {
        val bars = listOf(
            OhlcvBar("20260117", 100, 110, 95, 120, 1000L), // 20% up from prev
            OhlcvBar("20260116", 100, 110, 95, 100, 1000L), // 0%
            OhlcvBar("20260115", 100, 110, 95, 100, 1000L), // Base
        )

        val returns = OhlcvResampler.calculateReturns(bars)

        assertEquals(3, returns.size)
        assertEquals(0.0, returns[0], 0.0001) // First element is 0
        assertEquals(0.2, returns[1], 0.0001) // (120-100)/100 = 0.2
        assertEquals(0.0, returns[2], 0.0001) // (100-100)/100 = 0
    }

    @Test
    fun `calculateReturns handles empty list`() {
        val result = OhlcvResampler.calculateReturns(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateReturns handles single bar`() {
        val bars = listOf(OhlcvBar("20260115", 100, 110, 95, 105, 1000L))
        val returns = OhlcvResampler.calculateReturns(bars)
        assertEquals(listOf(0.0), returns)
    }

    // ========================================================================
    // Typical Price Tests
    // ========================================================================

    @Test
    fun `typicalPrices calculates HLC average`() {
        val bars = listOf(
            OhlcvBar("20260115", 100, 120, 90, 110, 1000L), // TP = (120+90+110)/3 = 106.67
            OhlcvBar("20260114", 100, 105, 95, 100, 1000L), // TP = (105+95+100)/3 = 100
        )

        val typicalPrices = OhlcvResampler.typicalPrices(bars)

        assertEquals(2, typicalPrices.size)
        assertEquals(106.666667, typicalPrices[0], 0.001)
        assertEquals(100.0, typicalPrices[1], 0.001)
    }
}
