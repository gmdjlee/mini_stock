package com.stockapp.feature.ranking.domain.model

/**
 * Market type for ranking queries.
 */
enum class MarketType(val code: String, val displayName: String) {
    ALL("000", "전체"),
    KOSPI("001", "KOSPI"),
    KOSDAQ("101", "KOSDAQ")
}

/**
 * Exchange type for ranking queries.
 * - KRX: 정규 거래소 (실전투자)
 * - NXT: 대체 거래소 (실전투자)
 * - KRX_MOCK: KRX 모의투자용
 */
enum class ExchangeType(val code: String, val displayName: String) {
    KRX("1", "KRX"),
    NXT("2", "NXT"),
    KRX_MOCK("3", "KRX")  // Mock mode only supports KRX (stex_tp: 3)
}

/**
 * Ranking type identifiers.
 */
enum class RankingType(val displayName: String, val apiId: String) {
    ORDER_BOOK_SURGE_BUY("호가잔량급증(매수)", "ka10021"),
    ORDER_BOOK_SURGE_SELL("호가잔량급증(매도)", "ka10021"),
    VOLUME_SURGE("거래량급증", "ka10023"),
    DAILY_VOLUME_TOP("당일거래량상위", "ka10030"),
    CREDIT_RATIO_TOP("신용비율상위", "ka10033"),
    FOREIGN_INSTITUTION_TOP("외국인/기관상위", "ka90009")
}

/**
 * Item count options for display.
 */
enum class ItemCount(val value: Int) {
    FIVE(5),
    TEN(10),
    TWENTY(20),
    THIRTY(30)
}

/**
 * Individual ranking item.
 */
data class RankingItem(
    val rank: Int,
    val ticker: String,
    val name: String,
    val currentPrice: Long,
    val priceChange: Long,
    val priceChangeSign: String,  // "+", "-", or ""
    val changeRate: Double,
    // Optional fields depending on ranking type
    val volume: Long? = null,
    val surgeRate: Double? = null,
    val surgeQuantity: Long? = null,
    val creditRatio: Double? = null,
    val foreignNetBuy: Long? = null,
    val institutionNetBuy: Long? = null,
    val totalBuyQuantity: Long? = null,
    val totalSellQuantity: Long? = null
)

/**
 * Ranking query result.
 */
data class RankingResult(
    val rankingType: RankingType,
    val marketType: MarketType,
    val exchangeType: ExchangeType,
    val items: List<RankingItem>,
    val fetchedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)
