package com.stockapp.core.stock.calc

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * OHLCV data resampler for converting daily data to weekly/monthly.
 * Python reference: ohlcv.py resampling logic
 */
object OhlcvResampler {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Raw OHLCV bar data.
     *
     * @property date Date string in YYYYMMDD format
     * @property open Opening price
     * @property high High price
     * @property low Low price
     * @property close Closing price
     * @property volume Trading volume
     */
    data class OhlcvBar(
        val date: String,
        val open: Int,
        val high: Int,
        val low: Int,
        val close: Int,
        val volume: Long
    )

    /**
     * Resample daily OHLCV data to weekly bars.
     *
     * Groups by ISO week (Monday-Sunday) and aggregates:
     * - open: First bar's open
     * - high: Maximum high
     * - low: Minimum low
     * - close: Last bar's close
     * - volume: Sum of volumes
     *
     * @param dailyBars List of daily bars (newest-first)
     * @return List of weekly bars (newest-first)
     */
    fun toWeekly(dailyBars: List<OhlcvBar>): List<OhlcvBar> {
        if (dailyBars.isEmpty()) return emptyList()

        // Group by ISO week
        val weekGroups = dailyBars.groupBy { bar ->
            val date = LocalDate.parse(bar.date, dateFormatter)
            val weekFields = WeekFields.of(Locale.getDefault())
            val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
            val year = date.get(weekFields.weekBasedYear())
            "$year-W$weekOfYear"
        }

        // Convert each group to weekly bar
        return weekGroups.map { (_, bars) ->
            aggregateBars(bars, PeriodType.WEEKLY)
        }.sortedByDescending { it.date } // Ensure newest-first
    }

    /**
     * Resample daily OHLCV data to monthly bars.
     *
     * Groups by year-month and aggregates:
     * - open: First bar's open
     * - high: Maximum high
     * - low: Minimum low
     * - close: Last bar's close
     * - volume: Sum of volumes
     *
     * @param dailyBars List of daily bars (newest-first)
     * @return List of monthly bars (newest-first)
     */
    fun toMonthly(dailyBars: List<OhlcvBar>): List<OhlcvBar> {
        if (dailyBars.isEmpty()) return emptyList()

        // Group by year-month
        val monthGroups = dailyBars.groupBy { bar ->
            bar.date.substring(0, 6) // YYYYMM
        }

        // Convert each group to monthly bar
        return monthGroups.map { (_, bars) ->
            aggregateBars(bars, PeriodType.MONTHLY)
        }.sortedByDescending { it.date } // Ensure newest-first
    }

    /**
     * Period type for date representation in aggregated bars.
     */
    enum class PeriodType {
        WEEKLY,  // Use Friday of the week
        MONTHLY  // Use last day of the month
    }

    /**
     * Aggregate multiple bars into a single bar.
     *
     * @param bars List of bars to aggregate (can be in any order)
     * @param periodType How to determine the representative date
     * @return Aggregated bar
     */
    private fun aggregateBars(bars: List<OhlcvBar>, periodType: PeriodType): OhlcvBar {
        require(bars.isNotEmpty()) { "Cannot aggregate empty bars" }

        // Sort chronologically (oldest first) for proper open/close
        val sortedBars = bars.sortedBy { it.date }

        val representativeDate = when (periodType) {
            PeriodType.WEEKLY -> {
                // Use Friday of the week (or the latest trading day)
                val latestDate = LocalDate.parse(sortedBars.last().date, dateFormatter)
                val friday = latestDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                friday.format(dateFormatter)
            }
            PeriodType.MONTHLY -> {
                // Use the last day of the month
                val latestDate = LocalDate.parse(sortedBars.last().date, dateFormatter)
                val lastDay = latestDate.with(TemporalAdjusters.lastDayOfMonth())
                lastDay.format(dateFormatter)
            }
        }

        return OhlcvBar(
            date = representativeDate,
            open = sortedBars.first().open,  // First bar's open
            high = sortedBars.maxOf { it.high },
            low = sortedBars.minOf { it.low },
            close = sortedBars.last().close, // Last bar's close
            volume = sortedBars.sumOf { it.volume }
        )
    }

    /**
     * Validate OHLCV bar data.
     *
     * @param bar Bar to validate
     * @return True if valid
     */
    fun isValidBar(bar: OhlcvBar): Boolean {
        return bar.date.length == 8 &&
            bar.open > 0 &&
            bar.high >= bar.open &&
            bar.high >= bar.close &&
            bar.low <= bar.open &&
            bar.low <= bar.close &&
            bar.low > 0 &&
            bar.volume >= 0
    }

    /**
     * Filter out invalid bars.
     *
     * @param bars List of bars
     * @return List of valid bars only
     */
    fun filterValidBars(bars: List<OhlcvBar>): List<OhlcvBar> {
        return bars.filter { isValidBar(it) }
    }

    /**
     * Calculate returns (percentage changes) from close prices.
     *
     * @param bars List of bars (newest-first)
     * @return List of returns (newest-first), first element is 0
     */
    fun calculateReturns(bars: List<OhlcvBar>): List<Double> {
        if (bars.isEmpty()) return emptyList()
        if (bars.size == 1) return listOf(0.0)

        val returns = mutableListOf(0.0)
        for (i in 1 until bars.size) {
            val prev = bars[i].close.toDouble()
            val curr = bars[i - 1].close.toDouble()
            returns.add(if (prev == 0.0) 0.0 else (curr - prev) / prev)
        }
        return returns
    }

    /**
     * Get typical price for each bar: (High + Low + Close) / 3
     *
     * @param bars List of bars
     * @return List of typical prices
     */
    fun typicalPrices(bars: List<OhlcvBar>): List<Double> {
        return bars.map { (it.high + it.low + it.close) / 3.0 }
    }
}
