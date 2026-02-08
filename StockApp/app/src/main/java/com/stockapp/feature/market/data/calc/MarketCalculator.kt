package com.stockapp.feature.market.data.calc

import com.stockapp.feature.market.domain.model.FearGreedSignal
import com.stockapp.feature.market.domain.model.IndicatorComponent
import com.stockapp.feature.market.domain.model.MarketFearGreed
import com.stockapp.feature.market.domain.model.OscillatorHistory
import com.stockapp.feature.market.domain.model.OscillatorSignal
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Market-level indicator calculator.
 * Computes oscillator and fear/greed from raw KRX data.
 */
object MarketCalculator {

    // Oscillator thresholds
    private const val EXTREME_GREED_THRESHOLD = 0.7
    private const val GREED_THRESHOLD = 0.55
    private const val FEAR_THRESHOLD = 0.45
    private const val EXTREME_FEAR_THRESHOLD = 0.3

    // Fear & Greed RSI period
    private const val RSI_PERIOD = 14
    private const val MOMENTUM_PERIOD = 20
    private const val VOLATILITY_PERIOD = 20

    /**
     * Calculate oscillator from daily advance/decline data.
     *
     * @param dates Date list (oldest first)
     * @param advances Number of advancing stocks per day
     * @param declines Number of declining stocks per day
     * @param unchanges Number of unchanged stocks per day
     * @param totals Total stocks per day
     * @return OscillatorHistory
     */
    fun calculateOscillator(
        dates: List<String>,
        advances: List<Int>,
        declines: List<Int>,
        unchanges: List<Int>,
        totals: List<Int>
    ): OscillatorHistory {
        if (dates.isEmpty()) {
            return OscillatorHistory(
                dates = emptyList(),
                values = emptyList(),
                signals = emptyList(),
                advanceRatios = emptyList(),
                declineRatios = emptyList()
            )
        }

        val advanceRatios = mutableListOf<Double>()
        val declineRatios = mutableListOf<Double>()
        val values = mutableListOf<Double>()
        val signals = mutableListOf<OscillatorSignal>()

        for (i in dates.indices) {
            val total = totals[i]
            val advRatio = if (total > 0) advances[i].toDouble() / total else 0.0
            val decRatio = if (total > 0) declines[i].toDouble() / total else 0.0

            advanceRatios.add(advRatio)
            declineRatios.add(decRatio)

            // Oscillator value: advance ratio (0.0 to 1.0)
            values.add(advRatio)
            signals.add(classifyOscillator(advRatio))
        }

        // Apply 5-day EMA smoothing to oscillator values
        val smoothed = emaSmooth(values, 5)
        val smoothedSignals = smoothed.map { classifyOscillator(it) }

        return OscillatorHistory(
            dates = dates,
            values = smoothed,
            signals = smoothedSignals,
            advanceRatios = advanceRatios,
            declineRatios = declineRatios
        )
    }

    /**
     * Calculate Fear & Greed index from market data.
     *
     * @param date Current date
     * @param indexCloses KOSPI close prices (oldest first, at least 60 days)
     * @param indexVolumes KOSPI volumes (oldest first)
     * @param foreignNetBuy Foreign net buy amount (recent days)
     * @param institutionNetBuy Institution net buy amount (recent days)
     * @param totalTradingValue Total market trading value (for net buy ratio)
     * @param shortSellingRatio Short selling ratio (0.0 to 1.0)
     * @return MarketFearGreed
     */
    fun calculateFearGreed(
        date: String,
        indexCloses: List<Double>,
        indexVolumes: List<Long>,
        foreignNetBuy: Long,
        institutionNetBuy: Long,
        totalTradingValue: Long,
        shortSellingRatio: Double
    ): MarketFearGreed {
        // 1. Momentum: 20-day return normalized to 0-100
        val momentum = calcMomentumScore(indexCloses)

        // 2. RSI: 14-day RSI (already 0-100 range)
        val rsi = calcRsiScore(indexCloses)

        // 3. Volatility: 20-day HV (inverse - high volatility = fear)
        val volatility = calcVolatilityScore(indexCloses)

        // 4. Foreign/Institution flow: net buy ratio
        val investorFlow = calcInvestorFlowScore(
            foreignNetBuy, institutionNetBuy, totalTradingValue
        )

        // 5. Short selling: inverse indicator (high short = fear)
        val shortSelling = calcShortSellingScore(shortSellingRatio)

        // Overall score: weighted average (equal 20% weights)
        val overallScore = momentum.normalizedScore * momentum.weight +
            rsi.normalizedScore * rsi.weight +
            volatility.normalizedScore * volatility.weight +
            investorFlow.normalizedScore * investorFlow.weight +
            shortSelling.normalizedScore * shortSelling.weight

        return MarketFearGreed(
            date = date,
            overallScore = overallScore.coerceIn(0.0, 100.0),
            signal = FearGreedSignal.fromScore(overallScore),
            momentum = momentum,
            rsi = rsi,
            volatility = volatility,
            investorFlow = investorFlow,
            shortSelling = shortSelling
        )
    }

    private fun calcMomentumScore(closes: List<Double>): IndicatorComponent {
        if (closes.size < MOMENTUM_PERIOD + 1) {
            return IndicatorComponent("모멘텀", 0.0, 50.0, description = "데이터 부족")
        }

        val current = closes.last()
        val past = closes[closes.size - MOMENTUM_PERIOD - 1]
        val returnRate = if (past > 0) (current - past) / past * 100 else 0.0

        // Normalize: -10% ~ +10% → 0 ~ 100
        val normalized = ((returnRate + 10.0) / 20.0 * 100).coerceIn(0.0, 100.0)

        return IndicatorComponent(
            name = "모멘텀",
            rawValue = returnRate,
            normalizedScore = normalized,
            description = "KOSPI ${MOMENTUM_PERIOD}일 수익률: ${"%.1f".format(returnRate)}%"
        )
    }

    private fun calcRsiScore(closes: List<Double>): IndicatorComponent {
        if (closes.size < RSI_PERIOD + 1) {
            return IndicatorComponent("RSI", 0.0, 50.0, description = "데이터 부족")
        }

        // PERF: Calculate RSI in-place without allocating separate gains/losses lists.
        // This avoids 2 * N Double object allocations + list overhead.
        var avgGain = 0.0
        var avgLoss = 0.0

        // Initial average over first RSI_PERIOD changes
        for (i in 1..RSI_PERIOD) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= RSI_PERIOD
        avgLoss /= RSI_PERIOD

        // Smoothed RSI over remaining data points
        for (i in RSI_PERIOD + 1 until closes.size) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0
            avgGain = (avgGain * (RSI_PERIOD - 1) + gain) / RSI_PERIOD
            avgLoss = (avgLoss * (RSI_PERIOD - 1) + loss) / RSI_PERIOD
        }

        val rsi = if (avgLoss == 0.0) 100.0
        else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))

        return IndicatorComponent(
            name = "RSI",
            rawValue = rsi,
            normalizedScore = rsi, // RSI is already 0-100
            description = "KOSPI ${RSI_PERIOD}일 RSI: ${"%.1f".format(rsi)}"
        )
    }

    private fun calcVolatilityScore(closes: List<Double>): IndicatorComponent {
        if (closes.size < VOLATILITY_PERIOD + 1) {
            return IndicatorComponent("변동성", 0.0, 50.0, description = "데이터 부족")
        }

        // PERF: Only compute log returns for the last VOLATILITY_PERIOD days,
        // avoiding allocation for the full history. We only need the recent window.
        var sum = 0.0
        var count = 0
        val recentReturns = DoubleArray(VOLATILITY_PERIOD)

        for (i in closes.size - VOLATILITY_PERIOD until closes.size) {
            if (i > 0 && closes[i - 1] > 0) {
                val ret = ln(closes[i] / closes[i - 1])
                recentReturns[count] = ret
                sum += ret
                count++
            }
        }

        if (count == 0) {
            return IndicatorComponent("변동성", 0.0, 50.0, description = "변동성 계산 불가")
        }

        // PERF: Calculate variance in-place without allocating a mapped list
        val mean = sum / count
        var varianceSum = 0.0
        for (j in 0 until count) {
            val diff = recentReturns[j] - mean
            varianceSum += diff * diff
        }
        val variance = varianceSum / count
        val hv = sqrt(variance) * sqrt(252.0) * 100 // Annualized %

        // Inverse normalize: HV 5% ~ 40% → 100 ~ 0
        // Low volatility = high score (greed), high volatility = low score (fear)
        val normalized = ((40.0 - hv) / 35.0 * 100).coerceIn(0.0, 100.0)

        return IndicatorComponent(
            name = "변동성",
            rawValue = hv,
            normalizedScore = normalized,
            description = "KOSPI ${VOLATILITY_PERIOD}일 변동성: ${"%.1f".format(hv)}%"
        )
    }

    private fun calcInvestorFlowScore(
        foreignNetBuy: Long,
        institutionNetBuy: Long,
        totalTradingValue: Long
    ): IndicatorComponent {
        if (totalTradingValue == 0L) {
            return IndicatorComponent("투자자 수급", 0.0, 50.0, description = "데이터 부족")
        }

        val netBuy = foreignNetBuy + institutionNetBuy
        val ratio = netBuy.toDouble() / totalTradingValue * 100

        // Normalize: -2% ~ +2% → 0 ~ 100
        val normalized = ((ratio + 2.0) / 4.0 * 100).coerceIn(0.0, 100.0)

        return IndicatorComponent(
            name = "투자자 수급",
            rawValue = ratio,
            normalizedScore = normalized,
            description = "외인+기관 순매수: ${"%.2f".format(ratio)}%"
        )
    }

    private fun calcShortSellingScore(shortSellingRatio: Double): IndicatorComponent {
        // Inverse indicator: high short selling = fear
        // Typical range: 1% ~ 15%
        val normalized = ((15.0 - shortSellingRatio * 100) / 14.0 * 100).coerceIn(0.0, 100.0)

        return IndicatorComponent(
            name = "공매도",
            rawValue = shortSellingRatio * 100,
            normalizedScore = normalized,
            description = "공매도 비율: ${"%.1f".format(shortSellingRatio * 100)}%"
        )
    }

    private fun classifyOscillator(value: Double): OscillatorSignal = when {
        value >= EXTREME_GREED_THRESHOLD -> OscillatorSignal.EXTREME_GREED
        value >= GREED_THRESHOLD -> OscillatorSignal.GREED
        value <= EXTREME_FEAR_THRESHOLD -> OscillatorSignal.EXTREME_FEAR
        value <= FEAR_THRESHOLD -> OscillatorSignal.FEAR
        else -> OscillatorSignal.NEUTRAL
    }

    /**
     * Simple EMA smoothing.
     * PERF: Uses DoubleArray to avoid Double boxing overhead from MutableList<Double>.
     */
    private fun emaSmooth(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val alpha = 2.0 / (period + 1)
        val oneMinusAlpha = 1.0 - alpha
        val result = DoubleArray(values.size)
        result[0] = values[0]
        for (i in 1 until values.size) {
            result[i] = alpha * values[i] + oneMinusAlpha * result[i - 1]
        }
        return result.toList()
    }
}
