package com.stockapp.core.stock.calc

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Trend Signal calculator.
 * Kotlin Native implementation of Python indicator/trend.py
 *
 * Components:
 * - MA Signal: MA crossover signal (daily: MA5 > MA20 > MA60, weekly: reference 3-condition)
 * - CMF (Chaikin Money Flow): -1 to 1
 * - Fear/Greed Index: approximately -1 to 1.5
 * - Trend: combined signal ("bullish", "neutral", "bearish")
 */
object TrendCalculator {

    // Fear/Greed calculation constants (matching Python)
    private const val FG_MOMENTUM_SMOOTHING_PERIOD = 7
    private const val FG_MOMENTUM_DIVISOR = 10
    private const val FG_VOLUME_SMOOTHING_PERIOD = 10
    private const val FG_POSITION_SMOOTHING_PERIOD = 7
    private const val FG_MOMENTUM_LOOKBACK = 5
    private const val FG_POSITION_LOOKBACK = 52
    private const val FG_VOLUME_LOOKBACK = 20
    private const val FG_MIN_CALC_PERIOD = 10

    // Fear/Greed component weights
    private const val FG_WEIGHT_MOMENTUM = 0.45
    private const val FG_WEIGHT_POSITION = 0.45
    private const val FG_WEIGHT_VOLUME_SURGE = 0.05
    private const val FG_WEIGHT_VOLUME_SPIKE = 0.05

    // Fear/Greed clipping bounds
    private const val FG_MOMENTUM_MIN = -1.0
    private const val FG_MOMENTUM_MAX = 1.5
    private const val FG_POSITION_MIN = -1.0
    private const val FG_POSITION_MAX = 1.5
    private const val FG_VOLUME_MIN = -0.5
    private const val FG_VOLUME_MAX = 1.2

    /**
     * Trend Signal result.
     */
    data class TrendResult(
        val ticker: String,
        val timeframe: String,
        val dates: List<String>,
        val maSignal: List<Int>,      // 1: bullish, 0: neutral, -1: bearish
        val cmf: List<Double>,         // -1 to 1
        val fearGreed: List<Double>,   // approximately -1 to 1.5
        val trend: List<String>,       // "bullish", "neutral", "bearish"
        val ma5: List<Int?>,
        val ma10: List<Int?>,
        val ma20: List<Int?>,
        val ma60: List<Int?>? = null   // Only for daily
    )

    /**
     * Calculate Trend Signal from OHLCV data.
     *
     * @param ticker Stock code
     * @param dates Date list (newest-first)
     * @param closes Close prices (newest-first)
     * @param highs High prices (newest-first)
     * @param lows Low prices (newest-first)
     * @param volumes Volume list (newest-first)
     * @param timeframe "daily" or "weekly"
     * @return TrendResult or null if insufficient data
     */
    fun calculate(
        ticker: String,
        dates: List<String>,
        closes: List<Int>,
        highs: List<Int>,
        lows: List<Int>,
        volumes: List<Long>,
        timeframe: String = "daily"
    ): TrendResult? {
        // Determine minimum periods and CMF period based on timeframe
        val minPeriods = if (timeframe == "weekly") 52 else 60
        val cmfPeriod = if (timeframe == "weekly") 4 else 20

        if (closes.size < minPeriods) {
            return null
        }

        // Calculate MAs
        val ma5 = calcMa(closes, 5)
        val ma10 = calcMa(closes, 10)
        val ma20 = calcMa(closes, 20)
        val ma60 = if (timeframe == "daily") calcMa(closes, 60) else null

        // Calculate CMF
        val cmf = MathUtil.cmf(highs, lows, closes, volumes, cmfPeriod)

        // Calculate Fear/Greed
        val fearGreed = if (timeframe == "weekly") {
            calcFearGreedWeekly(closes, volumes)
        } else {
            calcFearGreed(closes, volumes)
        }

        // Calculate MA signal
        val maSignal = if (timeframe == "weekly") {
            calcMaSignalWeeklyReference(closes, highs, lows, ma10, cmf)
        } else {
            calcMaSignal(ma5, ma20, ma60!!)
        }

        // Calculate combined trend
        val trend = calcTrend(maSignal, cmf, fearGreed)

        return TrendResult(
            ticker = ticker,
            timeframe = timeframe,
            dates = dates,
            maSignal = maSignal,
            cmf = cmf,
            fearGreed = fearGreed,
            trend = trend,
            ma5 = ma5,
            ma10 = ma10,
            ma20 = ma20,
            ma60 = ma60
        )
    }

    /**
     * Calculate Simple Moving Average.
     * Python reference: trend.py _calc_ma()
     *
     * @param prices Price list (newest-first)
     * @param period MA period
     * @return MA values (null for insufficient data)
     */
    fun calcMa(prices: List<Int>, period: Int): List<Int?> {
        if (prices.isEmpty() || period <= 0) return emptyList()

        return prices.indices.map { i ->
            if (i + period > prices.size) {
                null
            } else {
                (prices.subList(i, i + period).sum() / period)
            }
        }
    }

    /**
     * Calculate MA-based signal (daily).
     * Python reference: trend.py _calc_ma_signal()
     *
     * Signal Logic:
     * - 1 (Bullish): MA5 > MA20 > MA60 (uptrend alignment)
     * - -1 (Bearish): MA5 < MA20 < MA60 (downtrend alignment)
     * - 0 (Neutral): Otherwise
     */
    fun calcMaSignal(
        ma5: List<Int?>,
        ma20: List<Int?>,
        ma60: List<Int?>
    ): List<Int> {
        return ma5.indices.map { i ->
            val m5 = ma5.getOrNull(i)
            val m20 = ma20.getOrNull(i)
            val m60 = ma60.getOrNull(i)

            when {
                m5 == null || m20 == null || m60 == null -> 0
                m5 > m20 && m20 > m60 -> 1
                m5 < m20 && m20 < m60 -> -1
                else -> 0
            }
        }
    }

    /**
     * Calculate MA-based signal for weekly data (reference 3-condition logic).
     * Python reference: trend.py _calc_ma_signal_weekly_reference()
     *
     * Reference signal logic (from 추세판별.txt):
     * - Buy Signal (1): High > Prev_High AND Close > MA10 AND CMF > 0
     * - Sell Signal (-1): Low < Prev_Low AND Close < MA10 AND CMF < 0
     * - Neutral (0): Otherwise
     *
     * Note: Data is in reverse order (newest first), so Prev = index + 1.
     */
    fun calcMaSignalWeeklyReference(
        closes: List<Int>,
        highs: List<Int>,
        lows: List<Int>,
        ma10: List<Int?>,
        cmf: List<Double>
    ): List<Int> {
        val n = closes.size
        val result = MutableList(n) { 0 }

        for (i in 0 until n) {
            // Need previous bar data (which is at index i+1 since data is newest-first)
            if (i + 1 >= n || ma10.getOrNull(i) == null) {
                result[i] = 0
                continue
            }

            val prevHigh = highs[i + 1]
            val prevLow = lows[i + 1]
            val currentMa10 = ma10[i]!!

            // Buy Signal: High > Prev_High AND Close > MA10 AND CMF > 0
            if (highs[i] > prevHigh && closes[i] > currentMa10 && cmf[i] > 0) {
                result[i] = 1
            }
            // Sell Signal: Low < Prev_Low AND Close < MA10 AND CMF < 0
            else if (lows[i] < prevLow && closes[i] < currentMa10 && cmf[i] < 0) {
                result[i] = -1
            } else {
                result[i] = 0
            }
        }

        return result
    }

    /**
     * Calculate Fear/Greed Index (daily).
     * Python reference: trend.py _calc_fear_greed()
     *
     * Components:
     * - Momentum5 (45%): 5-day log return
     * - Pos52 (45%): Position within 52-day high/low range
     * - VolSurge (5%): Recent volume surge vs past volume
     * - VolSpike (5%): Recent volatility vs past volatility (negative)
     */
    fun calcFearGreed(
        closes: List<Int>,
        volumes: List<Long>
    ): List<Double> {
        val n = closes.size
        if (n < FG_POSITION_LOOKBACK) {
            return List(n) { 0.0 }
        }

        // Process in chronological order
        val closesChrono = closes.reversed()
        val volumesChrono = volumes.reversed()

        // Initialize component arrays
        val momentum5 = DoubleArray(n) { 0.0 }
        val pos52 = DoubleArray(n) { 0.0 }
        val volSurge = DoubleArray(n) { 1.0 }
        val volSpike = DoubleArray(n) { 1.0 }
        val returns = DoubleArray(n) { 0.0 }

        for (i in 0 until n) {
            // Momentum5: log return over 5 periods * 100
            if (i >= FG_MOMENTUM_LOOKBACK && closesChrono[i] > 0 && closesChrono[i - FG_MOMENTUM_LOOKBACK] > 0) {
                momentum5[i] = (ln(closesChrono[i].toDouble()) - ln(closesChrono[i - FG_MOMENTUM_LOOKBACK].toDouble())) * 100
            }

            // Pos52: Position within 52-period range
            if (i >= FG_POSITION_LOOKBACK - 1) {
                val window = closesChrono.subList(max(0, i - FG_POSITION_LOOKBACK + 1), i + 1)
                val low52 = window.minOrNull() ?: 0
                val high52 = window.maxOrNull() ?: 0
                pos52[i] = if (high52 > low52) {
                    (closesChrono[i] - low52).toDouble() / (high52 - low52)
                } else {
                    0.5
                }
            } else {
                // Use available data
                val window = closesChrono.subList(0, i + 1)
                if (window.isNotEmpty()) {
                    val lowVal = window.minOrNull() ?: 0
                    val highVal = window.maxOrNull() ?: 0
                    pos52[i] = if (highVal > lowVal) {
                        (closesChrono[i] - lowVal).toDouble() / (highVal - lowVal)
                    } else {
                        0.5
                    }
                }
            }

            // Returns for volatility calculation
            if (i >= 1 && closesChrono[i - 1] > 0) {
                returns[i] = (closesChrono[i] - closesChrono[i - 1]).toDouble() / closesChrono[i - 1]
            }
        }

        // VolSurge: recent 5-day avg volume / past 20-day avg volume
        for (i in 0 until n) {
            if (i >= FG_VOLUME_LOOKBACK) {
                val recentVol = volumesChrono.subList(i - 4, i + 1).average()
                val pastVol = volumesChrono.subList(i - 19, i + 1).average()
                if (pastVol > 0) {
                    volSurge[i] = max(0.0, min(3.0, recentVol / pastVol))
                }
            } else if (i >= 5) {
                volSurge[i] = 1.0
            }
        }

        // VolSpike: recent 5-day volatility / past 20-day volatility
        for (i in 0 until n) {
            if (i >= FG_VOLUME_LOOKBACK) {
                val recentReturns = returns.slice(i - 4..i)
                val pastReturns = returns.slice(i - 19..i)

                val recentStd = MathUtil.std(recentReturns)
                val pastStd = MathUtil.std(pastReturns)

                if (pastStd > 0) {
                    volSpike[i] = max(0.0, min(3.0, recentStd / pastStd))
                }
            } else if (i >= 5) {
                volSpike[i] = 1.0
            }
        }

        // Calculate FG with smoothing
        val fgChrono = DoubleArray(n) { 0.0 }
        val momentumWindowOffset = FG_MOMENTUM_SMOOTHING_PERIOD - 1
        val volumeWindowOffset = FG_VOLUME_SMOOTHING_PERIOD - 1

        for (i in 0 until n) {
            if (i < FG_MIN_CALC_PERIOD) {
                fgChrono[i] = 0.0
                continue
            }

            // m = (Momentum5.rolling(7).mean() / 10).clip(-1, 1.5)
            val mWindowStart = max(0, i - momentumWindowOffset)
            val mWindow = momentum5.slice(mWindowStart..i)
            val m = (mWindow.average() / FG_MOMENTUM_DIVISOR).coerceIn(FG_MOMENTUM_MIN, FG_MOMENTUM_MAX)

            // p = (2 * Pos52.rolling(7).mean() - 1).clip(-1, 1.5)
            val pWindow = pos52.slice(mWindowStart..i)
            val p = (2 * pWindow.average() - 1).coerceIn(FG_POSITION_MIN, FG_POSITION_MAX)

            // v = (VolSurge.rolling(10).mean() - 1).clip(-0.5, 1.2)
            val vWindowStart = max(0, i - volumeWindowOffset)
            val vWindow = volSurge.slice(vWindowStart..i)
            val v = (vWindow.average() - 1).coerceIn(FG_VOLUME_MIN, FG_VOLUME_MAX)

            // vs = -(VolSpike.rolling(10).mean() - 1).clip(-0.5, 1.2)
            val vsWindow = volSpike.slice(vWindowStart..i)
            val vs = (-(vsWindow.average() - 1)).coerceIn(FG_VOLUME_MIN, FG_VOLUME_MAX)

            // FG = weighted sum of components
            fgChrono[i] = FG_WEIGHT_MOMENTUM * m +
                FG_WEIGHT_POSITION * p +
                FG_WEIGHT_VOLUME_SURGE * v +
                FG_WEIGHT_VOLUME_SPIKE * vs
        }

        // Reverse back to newest-first order
        return fgChrono.toList().reversed()
    }

    /**
     * Calculate Fear/Greed Index for weekly data.
     * Python reference: trend.py _calc_fear_greed_weekly()
     *
     * Same algorithm as daily, using weekly periods.
     */
    fun calcFearGreedWeekly(
        closes: List<Int>,
        volumes: List<Long>
    ): List<Double> {
        // Same implementation as daily - periods are already in weeks
        return calcFearGreed(closes, volumes)
    }

    /**
     * Determine overall trend.
     * Python reference: trend.py _calc_trend()
     *
     * Logic:
     * - "bullish": Strong uptrend signals (>=2 bullish indicators)
     * - "bearish": Strong downtrend signals (>=2 bearish indicators)
     * - "neutral": Mixed signals
     */
    fun calcTrend(
        maSignal: List<Int>,
        cmf: List<Double>,
        fearGreed: List<Double>
    ): List<String> {
        return maSignal.indices.map { i ->
            var bullCount = 0
            var bearCount = 0

            // MA Signal
            when (maSignal.getOrNull(i)) {
                1 -> bullCount++
                -1 -> bearCount++
            }

            // CMF
            val cmfValue = cmf.getOrNull(i) ?: 0.0
            when {
                cmfValue > 0.05 -> bullCount++
                cmfValue < -0.05 -> bearCount++
            }

            // Fear/Greed (range: -1 ~ 1.5)
            // > 0.5: Greed (bullish momentum)
            // < -0.5: Fear (bearish momentum)
            val fgValue = fearGreed.getOrNull(i) ?: 0.0
            when {
                fgValue > 0.5 -> bullCount++
                fgValue < -0.5 -> bearCount++
            }

            when {
                bullCount >= 2 -> "bullish"
                bearCount >= 2 -> "bearish"
                else -> "neutral"
            }
        }
    }
}
