package com.stockapp.core.stock.calc

/**
 * Elder Impulse System calculator.
 * Kotlin Native implementation of Python indicator/elder.py
 *
 * Elder Impulse System uses:
 * - EMA13 (13-period Exponential Moving Average)
 * - MACD Histogram (MACD Line - Signal Line)
 *
 * Color determination (based on slope/direction):
 * - Green (bull): EMA13 slope > 0 AND MACD Histogram slope > 0
 * - Red (bear): EMA13 slope < 0 AND MACD Histogram slope < 0
 * - Blue (neutral): Otherwise (mixed signals)
 */
object ElderCalculator {

    // MACD default parameters
    private const val MACD_FAST_PERIOD = 12
    private const val MACD_SLOW_PERIOD = 26
    private const val MACD_SIGNAL_PERIOD = 9
    private const val EMA_PERIOD = 13

    // Minimum required periods
    private const val MIN_PERIODS = 35

    /**
     * Elder Impulse result.
     */
    data class ElderResult(
        val ticker: String,
        val timeframe: String,
        val dates: List<String>,
        val color: List<String>,        // "green", "red", "blue"
        val ema13: List<Double>,
        val macdLine: List<Double>,
        val signalLine: List<Double>,
        val macdHist: List<Double>,
        val ema13Slope: List<Double>,
        val histSlope: List<Double>,
        val close: List<Double>
    )

    /**
     * Calculate Elder Impulse from OHLCV data.
     *
     * @param ticker Stock code
     * @param dates Date list (newest-first)
     * @param closes Close prices (newest-first)
     * @param timeframe "daily" or "weekly"
     * @return ElderResult or null if insufficient data
     */
    fun calculate(
        ticker: String,
        dates: List<String>,
        closes: List<Int>,
        timeframe: String = "daily"
    ): ElderResult? {
        if (closes.size < MIN_PERIODS) {
            return null
        }

        // Calculate EMA13 using reference-style EMA (no SMA initialization)
        val ema13 = calcEmaNoSma(closes, EMA_PERIOD)

        // Calculate MACD
        val (macdLine, signalLine, macdHist) = calcMacdNoSma(closes)

        // Calculate slopes (diff from previous value)
        val ema13Slope = calcSlope(ema13)
        val histSlope = calcSlope(macdHist)

        // Determine impulse colors based on slopes
        val colors = calcImpulseColorBySlope(ema13Slope, histSlope)

        return ElderResult(
            ticker = ticker,
            timeframe = timeframe,
            dates = dates,
            color = colors,
            ema13 = ema13,
            macdLine = macdLine,
            signalLine = signalLine,
            macdHist = macdHist,
            ema13Slope = ema13Slope,
            histSlope = histSlope,
            close = closes.map { it.toDouble() }
        )
    }

    /**
     * Calculate EMA using reference formula (ewm with adjust=False).
     * Python reference: elder.py _calc_ema_no_sma()
     *
     * This matches pandas ewm(alpha=2/(period+1), adjust=False).mean()
     * - Starts from first value (no SMA initialization)
     * - Formula: ema[0] = price[0], ema[t] = alpha * price[t] + (1-alpha) * ema[t-1]
     *
     * Note: prices are in reverse order (newest first)
     *
     * @param prices Price list (newest-first)
     * @param period EMA period
     * @return EMA values as doubles
     */
    fun calcEmaNoSma(prices: List<Int>, period: Int): List<Double> {
        if (prices.isEmpty()) return emptyList()

        // Reverse to chronological order for calculation
        val pricesChrono = prices.reversed()
        val alpha = 2.0 / (period + 1)

        // Start from first value (no SMA initialization)
        val emaChrono = mutableListOf(pricesChrono[0].toDouble())

        for (i in 1 until pricesChrono.size) {
            val emaValue = alpha * pricesChrono[i] + (1 - alpha) * emaChrono.last()
            emaChrono.add(emaValue)
        }

        // Reverse back to newest-first order
        return emaChrono.reversed()
    }

    /**
     * Calculate EMA for Double values.
     */
    fun calcEmaNoSmaDouble(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty()) return emptyList()

        // Reverse to chronological order for calculation
        val pricesChrono = prices.reversed()
        val alpha = 2.0 / (period + 1)

        // Start from first value (no SMA initialization)
        val emaChrono = mutableListOf(pricesChrono[0])

        for (i in 1 until pricesChrono.size) {
            val emaValue = alpha * pricesChrono[i] + (1 - alpha) * emaChrono.last()
            emaChrono.add(emaValue)
        }

        // Reverse back to newest-first order
        return emaChrono.reversed()
    }

    /**
     * Calculate MACD using reference formula (no SMA initialization).
     * Python reference: elder.py _calc_macd_no_sma()
     *
     * MACD Line = EMA12 - EMA26
     * Signal Line = EMA9 of MACD Line
     * Histogram = MACD Line - Signal Line
     *
     * @param prices Price list (newest-first)
     * @param fastPeriod Fast EMA period (default 12)
     * @param slowPeriod Slow EMA period (default 26)
     * @param signalPeriod Signal line period (default 9)
     * @return Triple of (macdLine, signalLine, histogram)
     */
    fun calcMacdNoSma(
        prices: List<Int>,
        fastPeriod: Int = MACD_FAST_PERIOD,
        slowPeriod: Int = MACD_SLOW_PERIOD,
        signalPeriod: Int = MACD_SIGNAL_PERIOD
    ): Triple<List<Double>, List<Double>, List<Double>> {
        if (prices.isEmpty()) {
            return Triple(emptyList(), emptyList(), emptyList())
        }

        // Calculate EMAs
        val emaFast = calcEmaNoSma(prices, fastPeriod)
        val emaSlow = calcEmaNoSma(prices, slowPeriod)

        // Calculate MACD Line = EMA(fast) - EMA(slow)
        val macdLine = emaFast.indices.map { i ->
            emaFast[i] - emaSlow[i]
        }

        // Calculate Signal Line = EMA of MACD Line
        val signalLine = calcEmaNoSmaDouble(macdLine, signalPeriod)

        // Calculate Histogram = MACD Line - Signal Line
        val histogram = macdLine.indices.map { i ->
            macdLine[i] - signalLine[i]
        }

        return Triple(macdLine, signalLine, histogram)
    }

    /**
     * Calculate slope (diff from previous value) of a series.
     * Python reference: elder.py _calc_slope()
     *
     * This matches pandas .diff() method.
     * Note: Data is in reverse order (newest first), so slope = current - next (older).
     *
     * @param values Value list (newest-first)
     * @return Slope list (positive = rising, negative = falling)
     */
    fun calcSlope(values: List<Double>): List<Double> {
        return values.indices.map { i ->
            if (i + 1 >= values.size) {
                0.0
            } else {
                // Since data is newest-first, slope = current - previous (which is next in list)
                values[i] - values[i + 1]
            }
        }
    }

    /**
     * Determine Elder Impulse color based on slopes.
     * Python reference: elder.py _calc_impulse_color_by_slope()
     *
     * Reference logic:
     * - "green" (bull): ema_slope > 0 AND hist_slope > 0
     * - "red" (bear): ema_slope < 0 AND hist_slope < 0
     * - "blue" (neutral): Otherwise
     *
     * @param ema13Slope EMA13 slope list
     * @param histSlope MACD Histogram slope list
     * @return Color list ("green", "red", "blue")
     */
    fun calcImpulseColorBySlope(
        ema13Slope: List<Double>,
        histSlope: List<Double>
    ): List<String> {
        return ema13Slope.indices.map { i ->
            val emaSlope = ema13Slope.getOrNull(i) ?: 0.0
            val hSlope = histSlope.getOrNull(i) ?: 0.0

            when {
                emaSlope > 0 && hSlope > 0 -> "green"
                emaSlope < 0 && hSlope < 0 -> "red"
                else -> "blue"
            }
        }
    }
}
