package com.stockapp.core.stock.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stock API request/response models for Kiwoom REST API.
 * Used by native Kotlin implementations (bypassing Python/Chaquopy).
 */

// ============================================================================
// Common Response
// ============================================================================

/**
 * Common API response wrapper.
 */
@Serializable
data class StockApiResponse<T>(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    val data: T? = null
)

// ============================================================================
// ka10099 - Stock List (종목 리스트)
// ============================================================================

/**
 * Stock list request parameters.
 *
 * @property mrktTp Market type: "0" = All, "1" = KOSPI, "2" = KOSDAQ
 */
data class StockListRequest(
    val mrktTp: String = "0"
) {
    fun toRequestBody(): Map<String, String> = mapOf(
        "mrkt_tp" to mrktTp
    )
}

/**
 * Stock list response.
 * Note: API returns 'list' field (not 'stk_list') with items having 'code', 'name', 'marketName'
 */
@Serializable
data class StockListResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("list") val stkList: List<StockListItem>? = null
)

/**
 * Individual stock item in stock list.
 * Note: API returns 'code', 'name', 'marketName' (not 'stk_cd', 'stk_nm', 'mrkt_nm')
 */
@Serializable
data class StockListItem(
    @SerialName("code") val stkCd: String? = null, // 종목코드
    @SerialName("name") val stkNm: String? = null, // 종목명
    @SerialName("marketName") val mrktNm: String? = null // 시장명 (거래소/코스닥/ETN 등)
)

// ============================================================================
// ka10001 - Stock Basic Info (주식 기본정보)
// ============================================================================

/**
 * Stock basic info request parameters.
 */
data class StockInfoRequest(
    val stkCd: String
) {
    fun toRequestBody(): Map<String, String> = mapOf(
        "stk_cd" to stkCd
    )
}

/**
 * Stock basic info response.
 */
@Serializable
data class StockInfoResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("stk_nm") val stkNm: String? = null,    // 종목명
    @SerialName("cur_prc") val curPrc: Long? = null,    // 현재가
    @SerialName("mac") val mac: Long? = null            // 시가총액 (억원)
)

// ============================================================================
// ka10059 - Investor Trend (투자자별 매매)
// ============================================================================

/**
 * Investor trend request parameters.
 *
 * Required parameters (per Python reference implementation):
 * - dt: Base date (YYYYMMDD) - required!
 * - stk_cd: Stock code
 * - amt_qty_tp: Amount/quantity type (1: Amount, 2: Quantity)
 * - trde_tp: Trade type (0: Net buy, 1: Buy, 2: Sell)
 * - unit_tp: Unit type (1000, etc.)
 */
data class InvestorTrendRequest(
    val stkCd: String,
    val dt: String,              // 기준일 (YYYYMMDD) - required
    val amtQtyTp: String = "1",  // 금액/수량 구분 (1: 금액)
    val trdeTp: String = "0",    // 매매 구분 (0: 순매수)
    val unitTp: String = "1000"  // 단위
) {
    fun toRequestBody(): Map<String, String> = mapOf(
        "dt" to dt,
        "stk_cd" to stkCd,
        "amt_qty_tp" to amtQtyTp,
        "trde_tp" to trdeTp,
        "unit_tp" to unitTp
    )
}

/**
 * Investor trend response.
 */
@Serializable
data class InvestorTrendResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("stk_invsr_orgn") val data: List<InvestorTrendItem>? = null
)

/**
 * Individual investor trend item.
 */
@Serializable
data class InvestorTrendItem(
    @SerialName("dt") val date: String? = null,                // 일자 (YYYYMMDD)
    @SerialName("frgnr_invsr") val foreignNet: Long? = null,   // 외국인 순매수
    @SerialName("orgn") val institutionNet: Long? = null,      // 기관 순매수
    @SerialName("ind_invsr") val individualNet: Long? = null,  // 개인 순매수
    @SerialName("mrkt_tot_amt") val marketCap: Long? = null    // 시가총액 (백만원)
)

// ============================================================================
// ka10081/82/83 - OHLCV Chart (일봉/주봉/월봉)
// ============================================================================

/**
 * OHLCV request parameters.
 */
data class OhlcvRequest(
    val stkCd: String,
    val inqrStrtDt: String? = null, // 조회 시작일
    val inqrEndDt: String? = null,  // 조회 종료일
    val adjPrcYn: String = "1"      // 수정주가 여부: "1" = Yes
) {
    fun toRequestBody(): Map<String, String> = buildMap {
        put("stk_cd", stkCd)
        put("adj_prc_yn", adjPrcYn)
        inqrStrtDt?.let { put("inqr_strt_dt", it) }
        inqrEndDt?.let { put("inqr_end_dt", it) }
    }
}

/**
 * Daily OHLCV response (ka10081).
 */
@Serializable
data class DailyOhlcvResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("stk_dt_pole_chart_qry") val data: List<OhlcvItem>? = null
)

/**
 * Weekly OHLCV response (ka10082).
 */
@Serializable
data class WeeklyOhlcvResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("stk_stk_pole_chart_qry") val data: List<OhlcvItem>? = null
)

/**
 * Monthly OHLCV response (ka10083).
 */
@Serializable
data class MonthlyOhlcvResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("stk_mth_pole_chart_qry") val data: List<OhlcvItem>? = null
)

/**
 * Individual OHLCV bar.
 */
@Serializable
data class OhlcvItem(
    @SerialName("dt") val date: String? = null,           // 일자 (YYYYMMDD)
    @SerialName("open_pric") val open: Int? = null,       // 시가
    @SerialName("high_pric") val high: Int? = null,       // 고가
    @SerialName("low_pric") val low: Int? = null,         // 저가
    @SerialName("cur_prc") val close: Int? = null,        // 종가 (현재가)
    @SerialName("trde_qty") val volume: Long? = null      // 거래량
)

// ============================================================================
// ka10063 - Realtime Investor Trend (장중 투자자별 매매) - Phase 5
// ============================================================================

/**
 * Realtime supply request parameters.
 * Based on ka10063 API docs: 장중투자자별매매요청
 */
data class RealtimeSupplyRequest(
    val stkCd: String,
    val mrktTp: String = "000",       // 000: 전체
    val invsr: String = "6",          // 6: 전체 투자자
    val stexTp: String = "3",         // 3: KRX (모의)
    val amtQtyTp: String = "1",       // 1: 금액
    val frgnAll: String = "0",        // 0: 외국계 전체 아님
    val smtmNetprpsTp: String = "0"   // 0: 동시순매수 구분 없음
) {
    fun toRequestBody(): Map<String, String> = mapOf(
        "stk_cd" to stkCd,
        "mrkt_tp" to mrktTp,
        "invsr" to invsr,
        "stex_tp" to stexTp,
        "amt_qty_tp" to amtQtyTp,
        "frgn_all" to frgnAll,
        "smtm_netprps_tp" to smtmNetprpsTp
    )
}

/**
 * Realtime supply response.
 */
@Serializable
data class RealtimeSupplyResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("cur_prc") val currentPrice: Long? = null,        // 현재가
    @SerialName("netprps_amt") val netBuyAmount: Long? = null,    // 순매수금액
    @SerialName("buy_amt") val buyAmount: Long? = null,           // 매수금액
    @SerialName("sell_amt") val sellAmount: Long? = null,         // 매도금액
    @SerialName("netprps_qty") val netBuyQuantity: Long? = null,  // 순매수수량
    @SerialName("acc_trde_qty") val accumulatedVolume: Long? = null // 누적거래량
)

// ============================================================================
// Domain Models (Converted from API responses)
// ============================================================================

/**
 * Stock search result.
 */
data class StockSearchResult(
    val ticker: String,
    val name: String,
    val market: String
)

/**
 * Stock analysis data with supply/demand metrics.
 */
data class StockAnalysisData(
    val ticker: String,
    val name: String,
    val dates: List<String>,
    val marketCap: List<Long>,      // 시가총액 (억원)
    val foreignNet5d: List<Long>,   // 외국인 5일 합계
    val institutionNet5d: List<Long>, // 기관 5일 합계
    val supplyRatio: List<Double>   // 수급 비율: (외국인+기관) / 시가총액
)

/**
 * OHLCV data.
 */
data class OhlcvData(
    val ticker: String,
    val dates: List<String>,
    val opens: List<Int>,
    val highs: List<Int>,
    val lows: List<Int>,
    val closes: List<Int>,
    val volumes: List<Long>
)

/**
 * Supply demand signal.
 */
enum class SupplySignal(val label: String, val description: String) {
    STRONG_BUY("강력 매수", "> 0.5%"),
    BUY("매수", "> 0.2%"),
    NEUTRAL("중립", "-0.2% ~ 0.2%"),
    SELL("매도", "< -0.2%"),
    STRONG_SELL("강력 매도", "< -0.5%");

    companion object {
        fun fromRatio(ratio: Double): SupplySignal {
            return when {
                ratio > 0.005 -> STRONG_BUY
                ratio > 0.002 -> BUY
                ratio < -0.005 -> STRONG_SELL
                ratio < -0.002 -> SELL
                else -> NEUTRAL
            }
        }
    }
}

// ============================================================================
// API Endpoints
// ============================================================================

/**
 * Kiwoom API endpoints.
 */
object StockApiEndpoints {
    const val STOCK_LIST = "/api/dostk/stkinfo"       // ka10099
    const val STOCK_INFO = "/api/dostk/stkinfo"       // ka10001
    const val INVESTOR_TREND = "/api/dostk/stkinfo"   // ka10059
    const val DAILY_CHART = "/api/dostk/chart"        // ka10081
    const val WEEKLY_CHART = "/api/dostk/chart"       // ka10082
    const val MONTHLY_CHART = "/api/dostk/chart"      // ka10083
    const val REALTIME_SUPPLY = "/api/dostk/mrkcond"  // ka10063 (장중투자자별매매요청)
}

/**
 * API IDs.
 */
object StockApiIds {
    const val STOCK_LIST = "ka10099"
    const val STOCK_INFO = "ka10001"
    const val INVESTOR_TREND = "ka10059"
    const val DAILY_CHART = "ka10081"
    const val WEEKLY_CHART = "ka10082"
    const val MONTHLY_CHART = "ka10083"
    const val REALTIME_SUPPLY = "ka10063"
}
