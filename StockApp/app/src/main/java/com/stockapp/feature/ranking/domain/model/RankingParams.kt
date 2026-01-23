package com.stockapp.feature.ranking.domain.model

/**
 * Base interface for ranking API request parameters.
 */
interface RankingParams {
    val marketType: MarketType
    val exchangeType: ExchangeType

    fun toRequestBody(): Map<String, String>
}

/**
 * Parameters for 호가잔량급증요청 (ka10021).
 */
data class OrderBookSurgeParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val tradeType: String = "1",     // 1: Buy (매수), 2: Sell (매도)
    val sortType: String = "1",      // 1: Surge rate desc
    val timeType: String = "30",     // Time interval (분)
    val volumeType: String = "1",    // 거래량구분
    val stockCondition: String = "0" // 0: All stocks
) : RankingParams {

    override fun toRequestBody(): Map<String, String> = mapOf(
        "mrkt_tp" to marketType.code,
        "trde_tp" to tradeType,
        "sort_tp" to sortType,
        "tm_tp" to timeType,
        "trde_qty_tp" to volumeType,
        "stk_cnd" to stockCondition,
        "stex_tp" to exchangeType.code
    )
}

/**
 * Parameters for 거래량급증요청 (ka10023).
 */
data class VolumeSurgeParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val sortType: String = "1",       // 1: Surge rate desc
    val timeType: String = "2",       // Time interval
    val volumeType: String = "5",     // 거래량구분
    val time: String = "",            // 시간 (optional)
    val stockCondition: String = "0", // 0: All stocks
    val priceType: String = "0"       // 가격구분
) : RankingParams {

    override fun toRequestBody(): Map<String, String> = mapOf(
        "mrkt_tp" to marketType.code,
        "sort_tp" to sortType,
        "tm_tp" to timeType,
        "trde_qty_tp" to volumeType,
        "tm" to time,
        "stk_cnd" to stockCondition,
        "pric_tp" to priceType,
        "stex_tp" to exchangeType.code
    )
}

/**
 * Parameters for 당일거래량상위요청 (ka10030).
 */
data class DailyVolumeTopParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val sortType: String = "1",           // 1: Volume desc
    val managedStockInclude: String = "0", // 관리종목포함 (0: 미포함)
    val creditType: String = "0",          // 신용구분
    val volumeType: String = "0",          // 거래량구분
    val priceType: String = "0",           // 가격구분
    val amountType: String = "0",          // 거래대금구분
    val marketOpenType: String = "0"       // 장운영구분 (0: 전체)
) : RankingParams {

    override fun toRequestBody(): Map<String, String> = mapOf(
        "mrkt_tp" to marketType.code,
        "sort_tp" to sortType,
        "mang_stk_incls" to managedStockInclude,
        "crd_tp" to creditType,
        "trde_qty_tp" to volumeType,
        "pric_tp" to priceType,
        "trde_prica_tp" to amountType,
        "mrkt_open_tp" to marketOpenType,
        "stex_tp" to exchangeType.code
    )
}

/**
 * Parameters for 신용비율상위요청 (ka10033).
 */
data class CreditRatioTopParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val volumeType: String = "0",          // 거래량구분
    val stockCondition: String = "0",      // 종목조건
    val upDownInclude: String = "1",       // 상하한포함
    val creditCondition: String = "0"      // 신용조건
) : RankingParams {

    override fun toRequestBody(): Map<String, String> = mapOf(
        "mrkt_tp" to marketType.code,
        "trde_qty_tp" to volumeType,
        "stk_cnd" to stockCondition,
        "updown_incls" to upDownInclude,
        "crd_cnd" to creditCondition,
        "stex_tp" to exchangeType.code
    )
}

/**
 * Parameters for 외국인기관매매상위요청 (ka90009).
 */
data class ForeignInstitutionTopParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val amountQtyType: String = "1",   // 1: 금액, 2: 수량
    val queryDateType: String = "1",   // 1: 당일
    val date: String? = null,          // 날짜 (YYYYMMDD), null이면 당일
    // Filter parameters (used for parsing, not API)
    val investorType: InvestorType = InvestorType.FOREIGN,
    val tradeDirection: TradeDirection = TradeDirection.NET_BUY
) : RankingParams {

    override fun toRequestBody(): Map<String, String> = buildMap {
        put("mrkt_tp", marketType.code)
        put("amt_qty_tp", amountQtyType)
        put("qry_dt_tp", queryDateType)
        date?.let { put("date", it) }
        put("stex_tp", exchangeType.code)
    }
}
