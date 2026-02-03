package com.stockapp.feature.realtime.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalTime

/**
 * Realtime supply data model.
 * Represents current trading session supply/demand data.
 */
data class RealtimeSupplyData(
    val ticker: String,
    val name: String,
    val currentPrice: Long,           // 현재가
    val netBuyAmount: Long,           // 순매수금액 (백만원)
    val buyAmount: Long,              // 매수금액 (백만원)
    val sellAmount: Long,             // 매도금액 (백만원)
    val netBuyQuantity: Long,         // 순매수수량
    val accumulatedVolume: Long,      // 누적거래량
    val fetchedAt: Long               // 조회 시각 (epoch millis)
) {
    /**
     * Net buy ratio: netBuyAmount / (buyAmount + sellAmount)
     */
    val netBuyRatio: Double
        get() {
            val total = buyAmount + sellAmount
            return if (total > 0) netBuyAmount.toDouble() / total else 0.0
        }

    /**
     * Net buy amount in 억원 (100 million KRW)
     */
    val netBuyAmountBillion: Double
        get() = netBuyAmount / 100.0  // 백만원 → 억원

    /**
     * Buy amount in 억원
     */
    val buyAmountBillion: Double
        get() = buyAmount / 100.0

    /**
     * Sell amount in 억원
     */
    val sellAmountBillion: Double
        get() = sellAmount / 100.0
}

/**
 * Summary for UI display.
 */
data class RealtimeSupplySummary(
    val ticker: String,
    val name: String,
    val currentPrice: Long,
    val netBuyAmountBillion: Double,    // 억원 단위
    val buyAmountBillion: Double,       // 억원 단위
    val sellAmountBillion: Double,      // 억원 단위
    val netBuyQuantity: Long,
    val accumulatedVolume: Long,
    val netBuyRatio: Double,            // -1 ~ 1 범위
    val signal: RealtimeSupplySignal,
    val fetchedAt: Long,
    val isTradingHours: Boolean         // 장중 여부
)

/**
 * Realtime supply signal.
 */
enum class RealtimeSupplySignal(val label: String, val description: String) {
    STRONG_BUY("강력 매수", "순매수 비중 > 30%"),
    BUY("매수", "순매수 비중 > 10%"),
    NEUTRAL("중립", "순매수 비중 -10% ~ 10%"),
    SELL("매도", "순매도 비중 > 10%"),
    STRONG_SELL("강력 매도", "순매도 비중 > 30%");

    companion object {
        fun fromRatio(ratio: Double): RealtimeSupplySignal = when {
            ratio > 0.3 -> STRONG_BUY
            ratio > 0.1 -> BUY
            ratio < -0.3 -> STRONG_SELL
            ratio < -0.1 -> SELL
            else -> NEUTRAL
        }
    }
}

/**
 * Trading hours utility.
 */
object TradingHours {
    private val MARKET_OPEN = LocalTime.of(9, 0)
    private val MARKET_CLOSE = LocalTime.of(15, 30)

    /**
     * Check if current time is within trading hours.
     */
    fun isTradingHours(): Boolean {
        val now = LocalTime.now()
        return now in MARKET_OPEN..MARKET_CLOSE
    }

    /**
     * Get formatted trading hours string.
     */
    fun getTradingHoursString(): String = "09:00 - 15:30"
}

/**
 * Extension function to convert RealtimeSupplyData to Summary.
 */
fun RealtimeSupplyData.toSummary(): RealtimeSupplySummary {
    return RealtimeSupplySummary(
        ticker = ticker,
        name = name,
        currentPrice = currentPrice,
        netBuyAmountBillion = netBuyAmountBillion,
        buyAmountBillion = buyAmountBillion,
        sellAmountBillion = sellAmountBillion,
        netBuyQuantity = netBuyQuantity,
        accumulatedVolume = accumulatedVolume,
        netBuyRatio = netBuyRatio,
        signal = RealtimeSupplySignal.fromRatio(netBuyRatio),
        fetchedAt = fetchedAt,
        isTradingHours = TradingHours.isTradingHours()
    )
}

/**
 * Cache serialization wrapper.
 */
@Serializable
data class CachedRealtimeSupplyData(
    val ticker: String,
    val name: String,
    val currentPrice: Long,
    val netBuyAmount: Long,
    val buyAmount: Long,
    val sellAmount: Long,
    val netBuyQuantity: Long,
    val accumulatedVolume: Long,
    val fetchedAt: Long
) {
    fun toDomain(): RealtimeSupplyData = RealtimeSupplyData(
        ticker = ticker,
        name = name,
        currentPrice = currentPrice,
        netBuyAmount = netBuyAmount,
        buyAmount = buyAmount,
        sellAmount = sellAmount,
        netBuyQuantity = netBuyQuantity,
        accumulatedVolume = accumulatedVolume,
        fetchedAt = fetchedAt
    )

    companion object {
        fun fromDomain(data: RealtimeSupplyData): CachedRealtimeSupplyData =
            CachedRealtimeSupplyData(
                ticker = data.ticker,
                name = data.name,
                currentPrice = data.currentPrice,
                netBuyAmount = data.netBuyAmount,
                buyAmount = data.buyAmount,
                sellAmount = data.sellAmount,
                netBuyQuantity = data.netBuyQuantity,
                accumulatedVolume = data.accumulatedVolume,
                fetchedAt = data.fetchedAt
            )
    }
}
