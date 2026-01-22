package com.stockapp.feature.indicator.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========== Trend Signal ==========

/**
 * Trend Signal data.
 */
data class TrendSignal(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    val maSignal: List<Int>,        // 1: bullish, 0: neutral, -1: bearish
    val cmf: List<Double>,          // -1 to 1
    val fearGreed: List<Double>,    // approximately -1 to 1.5
    val trend: List<String>,        // "bullish", "neutral", "bearish"
    val ma5: List<Int?>,
    val ma10: List<Int?>,
    val ma20: List<Int?>
)

@Serializable
data class TrendResponse(
    val ok: Boolean,
    val data: TrendDataDto? = null,
    val error: IndicatorError? = null
)

@Serializable
data class TrendDataDto(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    @SerialName("ma_signal") val maSignal: List<Int>,
    val cmf: List<Double>,
    @SerialName("fear_greed") val fearGreed: List<Double>,
    val trend: List<String>,
    val ma5: List<Int?>,
    val ma10: List<Int?>,
    val ma20: List<Int?>
) {
    fun toDomain(): TrendSignal = TrendSignal(
        ticker = ticker,
        timeframe = timeframe,
        dates = dates,
        maSignal = maSignal,
        cmf = cmf,
        fearGreed = fearGreed,
        trend = trend,
        ma5 = ma5,
        ma10 = ma10,
        ma20 = ma20
    )
}

/**
 * Trend Signal summary for UI display.
 */
data class TrendSummary(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    val currentTrend: String,
    val currentMaSignal: Int,
    val currentCmf: Double,
    val currentFearGreed: Double,
    // Chart data
    val cmfHistory: List<Double>,
    val fearGreedHistory: List<Double>,
    val ma5History: List<Int?>,
    val ma10History: List<Int?>,
    val ma20History: List<Int?>,
    // Price history for chart (using MA10 as close proxy for weekly data)
    val priceHistory: List<Double> = emptyList(),
    // MA Signal history for signal generation
    val maSignalHistory: List<Int> = emptyList(),
    // Trend history for signal generation
    val trendHistory: List<String> = emptyList()
) {
    val trendLabel: String
        get() = when (currentTrend) {
            "bullish" -> "상승 추세"
            "bearish" -> "하락 추세"
            else -> "중립"
        }

    val cmfLabel: String
        get() = when {
            currentCmf > 0.1 -> "자금 유입"
            currentCmf < -0.1 -> "자금 유출"
            else -> "중립"
        }

    val fearGreedLabel: String
        get() = when {
            currentFearGreed > 0.5 -> "탐욕 (과열)"
            currentFearGreed < -0.5 -> "공포 (침체)"
            else -> "중립"
        }

    /**
     * Calculate primary buy signal indices.
     * Primary Buy: trend == "bullish" AND maSignal == 1
     */
    fun getPrimaryBuySignals(): List<Int> {
        return trendHistory.indices.filter { i ->
            i < maSignalHistory.size &&
            trendHistory[i] == "bullish" &&
            maSignalHistory[i] == 1
        }
    }

    /**
     * Calculate additional buy signal indices.
     * Additional Buy: maSignal == 1 AND trend != "bullish"
     */
    fun getAdditionalBuySignals(): List<Int> {
        return maSignalHistory.indices.filter { i ->
            i < trendHistory.size &&
            maSignalHistory[i] == 1 &&
            trendHistory[i] != "bullish"
        }
    }

    /**
     * Calculate primary sell signal indices.
     * Primary Sell: trend == "bearish" AND maSignal == -1
     */
    fun getPrimarySellSignals(): List<Int> {
        return trendHistory.indices.filter { i ->
            i < maSignalHistory.size &&
            trendHistory[i] == "bearish" &&
            maSignalHistory[i] == -1
        }
    }

    /**
     * Calculate additional sell signal indices.
     * Additional Sell: maSignal == -1 AND trend != "bearish"
     */
    fun getAdditionalSellSignals(): List<Int> {
        return maSignalHistory.indices.filter { i ->
            i < trendHistory.size &&
            maSignalHistory[i] == -1 &&
            trendHistory[i] != "bearish"
        }
    }
}

fun TrendSignal.toSummary(): TrendSummary = TrendSummary(
    ticker = ticker,
    timeframe = timeframe,
    dates = dates,
    currentTrend = trend.firstOrNull() ?: "neutral",
    currentMaSignal = maSignal.firstOrNull() ?: 0,
    currentCmf = cmf.firstOrNull() ?: 0.0,
    currentFearGreed = fearGreed.firstOrNull() ?: 0.0,
    cmfHistory = cmf,
    fearGreedHistory = fearGreed,
    ma5History = ma5,
    ma10History = ma10,
    ma20History = ma20,
    // Use MA10 as close price proxy for weekly data (more responsive than MA20)
    priceHistory = ma10.mapNotNull { it?.toDouble() }.ifEmpty {
        ma20.mapNotNull { it?.toDouble() }
    },
    // Include signal data for buy/sell marker generation
    maSignalHistory = maSignal,
    trendHistory = trend
)

// ========== Elder Impulse ==========

/**
 * Elder Impulse data.
 */
data class ElderImpulse(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    val color: List<String>,        // "green", "red", "blue"
    val ema13: List<Double>,
    val macdLine: List<Double>,
    val signalLine: List<Double>,
    val macdHist: List<Double>,
    val close: List<Double> = emptyList()  // Close prices for chart display
)

@Serializable
data class ElderResponse(
    val ok: Boolean,
    val data: ElderDataDto? = null,
    val error: IndicatorError? = null
)

@Serializable
data class ElderDataDto(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    val color: List<String>,
    val ema13: List<Double>,
    @SerialName("macd_line") val macdLine: List<Double>,
    @SerialName("signal_line") val signalLine: List<Double>,
    @SerialName("macd_hist") val macdHist: List<Double>,
    val close: List<Double> = emptyList()  // Close prices (populated from OHLCV)
) {
    fun toDomain(): ElderImpulse = ElderImpulse(
        ticker = ticker,
        timeframe = timeframe,
        dates = dates,
        color = color,
        ema13 = ema13,
        macdLine = macdLine,
        signalLine = signalLine,
        macdHist = macdHist,
        close = close
    )
}

/**
 * Elder Impulse summary for UI display.
 */
data class ElderSummary(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    val currentColor: String,
    val currentMacdHist: Double,
    // Chart data
    val colorHistory: List<String>,
    val ema13History: List<Double>,
    val macdHistHistory: List<Double>,
    // MACD data for MACD chart
    val macdLineHistory: List<Double> = emptyList(),
    val signalLineHistory: List<Double> = emptyList(),
    // Market cap history for Elder Impulse chart
    val mcapHistory: List<Double> = emptyList(),
    // Impulse states for chart markers (1=bullish, 0=neutral, -1=bearish)
    val impulseStates: List<Int> = emptyList()
) {
    val colorLabel: String
        get() = when (currentColor) {
            "green" -> "상승 (Green)"
            "red" -> "하락 (Red)"
            else -> "중립 (Blue)"
        }

    val impulseSignal: String
        get() = when (currentColor) {
            "green" -> "매수 유리"
            "red" -> "매도 유리"
            else -> "관망"
        }
}

fun ElderImpulse.toSummary(): ElderSummary = ElderSummary(
    ticker = ticker,
    timeframe = timeframe,
    dates = dates,
    currentColor = color.firstOrNull() ?: "blue",
    currentMacdHist = macdHist.firstOrNull() ?: 0.0,
    colorHistory = color,
    ema13History = ema13,
    macdHistHistory = macdHist,
    macdLineHistory = macdLine,
    signalLineHistory = signalLine,
    // Use actual close prices for chart display (fallback to EMA13 if not available)
    mcapHistory = close.ifEmpty { ema13 },
    // Convert color strings to impulse states
    impulseStates = color.map { c ->
        when (c) {
            "green" -> 1
            "red" -> -1
            else -> 0
        }
    }
)

// ========== DeMark TD Setup ==========

/**
 * DeMark TD Setup data.
 */
data class DemarkSetup(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    val close: List<Int>,
    val sellSetup: List<Int>,
    val buySetup: List<Int>
)

@Serializable
data class DemarkResponse(
    val ok: Boolean,
    val data: DemarkDataDto? = null,
    val error: IndicatorError? = null
)

@Serializable
data class DemarkDataDto(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    val close: List<Int>,
    @SerialName("sell_setup") val sellSetup: List<Int>,
    @SerialName("buy_setup") val buySetup: List<Int>
) {
    fun toDomain(): DemarkSetup = DemarkSetup(
        ticker = ticker,
        timeframe = timeframe,
        dates = dates,
        close = close,
        sellSetup = sellSetup,
        buySetup = buySetup
    )
}

/**
 * DeMark TD Setup summary for UI display.
 */
data class DemarkSummary(
    val ticker: String,
    val timeframe: String,
    val dates: List<String>,
    val currentSellSetup: Int,
    val currentBuySetup: Int,
    val maxSellSetup: Int,
    val maxBuySetup: Int,
    // Chart data
    val sellSetupHistory: List<Int>,
    val buySetupHistory: List<Int>,
    val closeHistory: List<Int>,
    // Market cap history for chart overlay
    val mcapHistory: List<Double> = emptyList()
) {
    val sellSignal: String
        get() = when {
            currentSellSetup >= 9 -> "매도 신호 (카운트 $currentSellSetup)"
            currentSellSetup >= 5 -> "매도 대기 (카운트 $currentSellSetup)"
            else -> "없음"
        }

    val buySignal: String
        get() = when {
            currentBuySetup >= 9 -> "매수 신호 (카운트 $currentBuySetup)"
            currentBuySetup >= 5 -> "매수 대기 (카운트 $currentBuySetup)"
            else -> "없음"
        }

    val hasActiveSetup: Boolean
        get() = currentSellSetup >= 5 || currentBuySetup >= 5
}

fun DemarkSetup.toSummary(): DemarkSummary = DemarkSummary(
    ticker = ticker,
    timeframe = timeframe,
    dates = dates,
    currentSellSetup = sellSetup.firstOrNull() ?: 0,
    currentBuySetup = buySetup.firstOrNull() ?: 0,
    maxSellSetup = sellSetup.maxOrNull() ?: 0,
    maxBuySetup = buySetup.maxOrNull() ?: 0,
    sellSetupHistory = sellSetup,
    buySetupHistory = buySetup,
    closeHistory = close,
    // Use close price as mcap reference for chart
    mcapHistory = close.map { it.toDouble() }
)

// ========== Common ==========

@Serializable
data class IndicatorError(
    val code: String,
    val msg: String
)

/**
 * Indicator type enum.
 */
enum class IndicatorType(val key: String, val label: String) {
    TREND("trend", "Trend Signal"),
    ELDER("elder", "Elder Impulse"),
    DEMARK("demark", "DeMark TD");

    companion object {
        fun fromKey(key: String): IndicatorType? = entries.find { it.key == key }
    }
}
