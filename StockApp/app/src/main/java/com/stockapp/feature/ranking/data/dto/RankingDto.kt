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
 * API returns data wrapped in "tdy_trde_qty_top" field as array of objects.
 * Note: Field name uses "tdy" abbreviation for "today" per Kiwoom API convention.
 */
@Serializable
data class DailyVolumeTopResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("tdy_trde_qty_top") val items: List<RankingItemDto>? = null
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
 * API returns data wrapped in "frgnr_orgn_trde_upper" field.
 * Each row contains 4 different rankings side by side:
 * - 외인 순매도 (for_netslmt_*)
 * - 외인 순매수 (for_netprps_*)
 * - 기관 순매도 (orgn_netslmt_*)
 * - 기관 순매수 (orgn_netprps_*)
 */
@Serializable
data class ForeignInstitutionTopResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("frgnr_orgn_trde_upper") val items: List<ForeignInstitutionItemDto>? = null
)

/**
 * Item DTO for ka90009 (외국인기관매매상위요청).
 * Each row contains 4 different stock entries for different categories.
 */
@Serializable
data class ForeignInstitutionItemDto(
    // 외인 순매도 종목
    @SerialName("for_netslmt_stk_cd") val forNetslmtStkCd: String? = null,
    @SerialName("for_netslmt_stk_nm") val forNetslmtStkNm: String? = null,
    @SerialName("for_netslmt_amt") val forNetslmtAmt: String? = null,
    @SerialName("for_netslmt_qty") val forNetslmtQty: String? = null,
    // 외인 순매수 종목
    @SerialName("for_netprps_stk_cd") val forNetprpsStkCd: String? = null,
    @SerialName("for_netprps_stk_nm") val forNetprpsStkNm: String? = null,
    @SerialName("for_netprps_amt") val forNetprpsAmt: String? = null,
    @SerialName("for_netprps_qty") val forNetprpsQty: String? = null,
    // 기관 순매도 종목
    @SerialName("orgn_netslmt_stk_cd") val orgnNetslmtStkCd: String? = null,
    @SerialName("orgn_netslmt_stk_nm") val orgnNetslmtStkNm: String? = null,
    @SerialName("orgn_netslmt_amt") val orgnNetslmtAmt: String? = null,
    @SerialName("orgn_netslmt_qty") val orgnNetslmtQty: String? = null,
    // 기관 순매수 종목
    @SerialName("orgn_netprps_stk_cd") val orgnNetprpsStkCd: String? = null,
    @SerialName("orgn_netprps_stk_nm") val orgnNetprpsStkNm: String? = null,
    @SerialName("orgn_netprps_amt") val orgnNetprpsAmt: String? = null,
    @SerialName("orgn_netprps_qty") val orgnNetprpsQty: String? = null
)
