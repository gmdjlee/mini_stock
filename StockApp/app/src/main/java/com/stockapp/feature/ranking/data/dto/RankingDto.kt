package com.stockapp.feature.ranking.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generic ranking list item from API response.
 * Fields are optional as different APIs return different fields.
 */
@Serializable
data class RankingItemDto(
    // Common fields
    @SerialName("stk_cd") val stkCd: String? = null,
    @SerialName("stk_nm") val stkNm: String? = null,
    @SerialName("cur_prc") val curPrc: String? = null,
    @SerialName("pred_pre_sig") val predPreSig: String? = null,
    @SerialName("pred_pre") val predPre: String? = null,
    @SerialName("flu_rt") val fluRt: String? = null,

    // Volume fields (ka10023, ka10030)
    @SerialName("trde_qty") val trdeQty: String? = null,
    @SerialName("now_trde_qty") val nowTrdeQty: String? = null,
    @SerialName("prev_trde_qty") val prevTrdeQty: String? = null,
    @SerialName("sdnin_qty") val sdninQty: String? = null,
    @SerialName("sdnin_rt") val sdninRt: String? = null,
    @SerialName("pred_rt") val predRt: String? = null,

    // Order book fields (ka10021)
    @SerialName("tot_buy_qty") val totBuyQty: String? = null,
    @SerialName("tot_sel_req") val totSelReq: String? = null,
    @SerialName("tot_buy_req") val totBuyReq: String? = null,
    @SerialName("now") val now: String? = null,
    @SerialName("int") val baseRate: String? = null,

    // Credit fields (ka10033)
    @SerialName("crd_rt") val crdRt: String? = null,
    @SerialName("sel_req") val selReq: String? = null,
    @SerialName("buy_req") val buyReq: String? = null,

    // Rank fields
    @SerialName("rank") val rank: String? = null,
    @SerialName("now_rank") val nowRank: String? = null
)

/**
 * Response for ka10021 (호가잔량급증요청).
 * API returns data wrapped in "bid_req_sdnin" field as array of objects.
 */
@Serializable
data class OrderBookSurgeResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("bid_req_sdnin") val items: List<RankingItemDto>? = null
)

/**
 * Response for ka10023 (거래량급증요청).
 * API returns data wrapped in "trde_qty_sdnin" field as array of objects.
 */
@Serializable
data class VolumeSurgeResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("trde_qty_sdnin") val items: List<RankingItemDto>? = null
)

/**
 * Response for ka10030 (당일거래량상위요청).
 * API returns data wrapped in "today_trde_qty_top" field as array of objects.
 */
@Serializable
data class DailyVolumeTopResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("today_trde_qty_top") val items: List<RankingItemDto>? = null
)

/**
 * Response for ka10033 (신용비율상위요청).
 * API returns data wrapped in "crd_rt_top" field as array of objects.
 */
@Serializable
data class CreditRatioTopResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("crd_rt_top") val items: List<RankingItemDto>? = null
)

/**
 * Response for ka90009 (외국인기관매매상위요청).
 * Note: This API returns separate lists for foreign buy/sell and institution buy/sell.
 */
@Serializable
data class ForeignInstitutionTopResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    // 외인 순매수 종목
    @SerialName("for_netprps_stk_cd") val forNetprpsStkCdList: List<String>? = null,
    @SerialName("for_netprps_stk_nm") val forNetprpsStkNmList: List<String>? = null,
    @SerialName("for_netprps_amt") val forNetprpsAmtList: List<String>? = null,
    @SerialName("for_netprps_qty") val forNetprpsQtyList: List<String>? = null,
    // 외인 순매도 종목
    @SerialName("for_netslmt_stk_cd") val forNetslmtStkCdList: List<String>? = null,
    @SerialName("for_netslmt_stk_nm") val forNetslmtStkNmList: List<String>? = null,
    @SerialName("for_netslmt_amt") val forNetslmtAmtList: List<String>? = null,
    @SerialName("for_netslmt_qty") val forNetslmtQtyList: List<String>? = null,
    // 기관 순매수 종목
    @SerialName("orgn_netprps_stk_cd") val orgnNetprpsStkCdList: List<String>? = null,
    @SerialName("orgn_netprps_stk_nm") val orgnNetprpsStkNmList: List<String>? = null,
    @SerialName("orgn_netprps_amt") val orgnNetprpsAmtList: List<String>? = null,
    @SerialName("orgn_netprps_qty") val orgnNetprpsQtyList: List<String>? = null,
    // 기관 순매도 종목
    @SerialName("orgn_netslmt_stk_cd") val orgnNetslmtStkCdList: List<String>? = null,
    @SerialName("orgn_netslmt_stk_nm") val orgnNetslmtStkNmList: List<String>? = null,
    @SerialName("orgn_netslmt_amt") val orgnNetslmtAmtList: List<String>? = null,
    @SerialName("orgn_netslmt_qty") val orgnNetslmtQtyList: List<String>? = null
)
