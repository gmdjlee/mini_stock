package com.stockapp.core.stock.calc

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Mathematical utility functions for stock analysis calculations.
 * Mirrors Python implementations in stock-analyzer package.
 *
 * Note: Most functions assume data is in newest-first order (descending date)
 * unless otherwise specified.
 */
object MathUtil {

    /**
     * Rolling Sum with min_periods=1.
     * Python reference: analysis.py _rolling_sum()
     *
     * For each position i, calculates sum of elements from max(0, i-window+1) to i.
     *
     * @param values List of values (newest-first)
     * @param window Window size for rolling calculation
     * @return List of rolling sums (newest-first)
     */
    fun rollingSum(values: List<Long>, window: Int): List<Long> {
        if (values.isEmpty() || window <= 0) return emptyList()

        return values.indices.map { i ->
            val start = maxOf(0, i - window + 1)
            values.subList(start, i + 1).sum()
        }
    }

    /**
     * Rolling Sum for Double values.
     */
    fun rollingSumDouble(values: List<Double>, window: Int): List<Double> {
        if (values.isEmpty() || window <= 0) return emptyList()

        return values.indices.map { i ->
            val start = maxOf(0, i - window + 1)
            values.subList(start, i + 1).sum()
        }
    }

    /**
     * Simple Moving Average (SMA).
     * Python reference: trend.py _calc_ma()
     *
     * @param prices List of prices (newest-first)
     * @param period MA period
     * @return List of SMA values, null where insufficient data
     */
    fun sma(prices: List<Int>, period: Int): List<Double?> {
        if (prices.isEmpty() || period <= 0) return emptyList()

        return prices.indices.map { i ->
            if (i + period > prices.size) {
                null
            } else {
                prices.subList(i, i + period).average()
            }
        }
    }

    /**
     * SMA for Double values.
     */
    fun smaDouble(prices: List<Double>, period: Int): List<Double?> {
        if (prices.isEmpty() || period <= 0) return emptyList()

        return prices.indices.map { i ->
            if (i + period > prices.size) {
                null
            } else {
                prices.subList(i, i + period).average()
            }
        }
    }

    /**
     * Exponential Moving Average (EMA) with adjust=false.
     * Python reference: elder.py _calc_ema_no_sma()
     *
     * Uses alpha = 2 / (period + 1).
     * Starts from the oldest value and propagates forward.
     *
     * @param prices List of prices (newest-first)
     * @param period EMA period
     * @return List of EMA values (newest-first)
     */
    fun ema(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty() || period <= 0) return emptyList()

        val alpha = 2.0 / (period + 1)
        val result = mutableListOf<Double>()

        // Start from oldest (last element in newest-first list)
        result.add(prices.last())

        // Process in chronological order (reverse iteration)
        for (i in prices.size - 2 downTo 0) {
            val emaValue = alpha * prices[i] + (1 - alpha) * result.last()
            result.add(emaValue)
        }

        // Reverse back to newest-first order
        return result.reversed()
    }

    /**
     * EMA for Int prices, returns Double.
     */
    fun emaInt(prices: List<Int>, period: Int): List<Double> {
        return ema(prices.map { it.toDouble() }, period)
    }

    /**
     * Chaikin Money Flow (CMF).
     * Python reference: trend.py _calc_cmf()
     *
     * Formula:
     * - MFM = ((Close - Low) - (High - Close)) / (High - Low)
     * - MFV = MFM * Volume
     * - CMF = Sum(MFV, period) / Sum(Volume, period)
     *
     * @param highs High prices (newest-first)
     * @param lows Low prices (newest-first)
     * @param closes Close prices (newest-first)
     * @param volumes Volumes (newest-first)
     * @param period CMF period (default: 20)
     * @return List of CMF values (newest-first), range typically -1 to 1
     */
    fun cmf(
        highs: List<Int>,
        lows: List<Int>,
        closes: List<Int>,
        volumes: List<Long>,
        period: Int = 20
    ): List<Double> {
        if (highs.isEmpty() || highs.size != lows.size ||
            highs.size != closes.size || highs.size != volumes.size
        ) {
            return emptyList()
        }

        // Calculate Money Flow Volume for each bar
        val mfv = highs.indices.map { i ->
            val hlRange = highs[i] - lows[i]
            if (hlRange == 0) {
                0.0
            } else {
                val mfm = ((closes[i] - lows[i]) - (highs[i] - closes[i])).toDouble() / hlRange
                mfm * volumes[i]
            }
        }

        // Calculate CMF: Sum(MFV) / Sum(Volume) over period
        return highs.indices.map { i ->
            if (i + period > highs.size) {
                0.0
            } else {
                val sumMfv = mfv.subList(i, i + period).sum()
                val sumVol = volumes.subList(i, i + period).sum()
                if (sumVol == 0L) 0.0 else sumMfv / sumVol
            }
        }
    }

    /**
     * Standard Deviation.
     * Python reference: trend.py _calc_std()
     *
     * Uses population standard deviation (N, not N-1).
     *
     * @param values List of values
     * @return Standard deviation, or 0.0 if insufficient data
     */
    fun std(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    /**
     * Rolling Standard Deviation.
     *
     * @param values List of values (newest-first)
     * @param window Rolling window size
     * @return List of rolling std values (newest-first)
     */
    fun rollingStd(values: List<Double>, window: Int): List<Double> {
        if (values.isEmpty() || window <= 0) return emptyList()

        return values.indices.map { i ->
            if (i + window > values.size) {
                0.0
            } else {
                std(values.subList(i, i + window))
            }
        }
    }

    /**
     * Rolling Mean/Average.
     *
     * @param values List of values (newest-first)
     * @param window Rolling window size
     * @return List of rolling mean values (newest-first)
     */
    fun rollingMean(values: List<Double>, window: Int): List<Double> {
        if (values.isEmpty() || window <= 0) return emptyList()

        return values.indices.map { i ->
            val start = maxOf(0, i - window + 1)
            values.subList(start, i + 1).average()
        }
    }

    /**
     * Rolling Min.
     */
    fun rollingMin(values: List<Int>, window: Int): List<Int?> {
        if (values.isEmpty() || window <= 0) return emptyList()

        return values.indices.map { i ->
            if (i + window > values.size) {
                null
            } else {
                values.subList(i, i + window).minOrNull()
            }
        }
    }

    /**
     * Rolling Max.
     */
    fun rollingMax(values: List<Int>, window: Int): List<Int?> {
        if (values.isEmpty() || window <= 0) return emptyList()

        return values.indices.map { i ->
            if (i + window > values.size) {
                null
            } else {
                values.subList(i, i + window).maxOrNull()
            }
        }
    }

    /**
     * Calculates percentage change between consecutive values.
     *
     * @param values List of values (newest-first)
     * @return List of percentage changes (newest-first), first element is 0
     */
    fun pctChange(values: List<Double>): List<Double> {
        if (values.isEmpty()) return emptyList()
        if (values.size == 1) return listOf(0.0)

        val result = mutableListOf(0.0) // First element has no previous value
        for (i in 1 until values.size) {
            val prev = values[i] // Older value (index increases = older in newest-first)
            val curr = values[i - 1]
            result.add(if (prev == 0.0) 0.0 else (curr - prev) / prev)
        }
        return result
    }

    /**
     * Natural logarithm for positive values.
     */
    fun ln(value: Double): Double {
        return if (value > 0) kotlin.math.ln(value) else 0.0
    }

    /**
     * Coerce value to range.
     */
    fun Double.coerceToRange(min: Double, max: Double): Double {
        return this.coerceIn(min, max)
    }
}
