package com.stockapp.feature.etf.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for ka40004 (ETF전체시세요청).
 * Returns list of all ETFs with their current market data.
 */
@Serializable
data class EtfListResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("etfall_mrpr") val items: List<EtfListItemDto>? = null
)

/**
 * ETF item from ka40004 response.
 */
@Serializable
data class EtfListItemDto(
    @SerialName("stk_cd") val stkCd: String? = null,
    @SerialName("stk_nm") val stkNm: String? = null,
    @SerialName("stk_cls") val stkCls: String? = null,          // 종목분류
    @SerialName("close_pric") val closePric: String? = null,    // 종가
    @SerialName("pre_sig") val preSig: String? = null,          // 대비기호
    @SerialName("pred_pre") val predPre: String? = null,        // 전일대비
    @SerialName("pre_rt") val preRt: String? = null,            // 대비율
    @SerialName("trde_qty") val trdeQty: String? = null,        // 거래량
    @SerialName("nav") val nav: String? = null,                 // NAV
    @SerialName("trace_eor_rt") val traceEorRt: String? = null, // 추적오차율
    @SerialName("txbs") val txbs: String? = null,               // 과표기준
    @SerialName("trace_idex_nm") val traceIdexNm: String? = null, // 추적지수명
    @SerialName("drng") val drng: String? = null,               // 배수
    @SerialName("trace_idex_cd") val traceIdexCd: String? = null, // 추적지수코드
    @SerialName("mngmcomp") val mngmcomp: String? = null        // 운용사
)

/**
 * Request parameters for ka40004.
 */
data class EtfListParams(
    val taxType: String = "0",       // 과세유형: 0=전체
    val navPre: String = "0",        // NAV대비: 0=전체
    val managementCompany: String = "0000", // 운용사: 0000=전체
    val taxYn: String = "0",         // 과세여부: 0=전체
    val trackingIndex: String = "0", // 추적지수: 0=전체
    val exchangeType: String = "0"   // 거래소구분: 0=전체, 1=KRX, 2=NXT
) {
    fun toRequestBody(): Map<String, String> = mapOf(
        "txon_type" to taxType,
        "navpre" to navPre,
        "mngmcomp" to managementCompany,
        "txon_yn" to taxYn,
        "trace_idex" to trackingIndex,
        "stex_tp" to exchangeType
    )
}

// ============================================================
// KIS API DTOs for ETF Constituent
// ============================================================

/**
 * Response for KIS API FHKST121600C0 (ETF 구성종목 조회).
 * Returns constituent stocks of an ETF.
 */
@Serializable
data class EtfConstituentResponse(
    @SerialName("rt_cd") val rtCd: String? = null,        // 성공: "0"
    @SerialName("msg_cd") val msgCd: String? = null,
    @SerialName("msg1") val msg1: String? = null,
    @SerialName("output1") val output1: EtfConstituentInfo? = null,
    @SerialName("output2") val output2: List<EtfConstituentItemDto>? = null
)

/**
 * ETF basic info from output1.
 */
@Serializable
data class EtfConstituentInfo(
    @SerialName("etf_nm") val etfNm: String? = null,           // ETF명
    @SerialName("nv_bf_dv_upr_qty") val nvBfDvUprQty: String? = null, // 배당전기준수량
    @SerialName("cmp_sty") val cmpSty: String? = null,         // 구성종목수
    @SerialName("acc_trde_prica") val accTrdePrica: String? = null    // 누적거래대금
)

/**
 * ETF constituent item from output2.
 */
@Serializable
data class EtfConstituentItemDto(
    @SerialName("stck_shrn_iscd") val stckShrnIscd: String? = null,    // 종목단축코드
    @SerialName("stck_bsop_date") val stckBsopDate: String? = null,    // 기준일자
    @SerialName("hts_kor_isnm") val htsKorIsnm: String? = null,        // 종목명
    @SerialName("stck_prpr") val stckPrpr: String? = null,             // 현재가
    @SerialName("prdy_vrss") val prdyVrss: String? = null,             // 전일대비
    @SerialName("prdy_vrss_sign") val prdyVrssSign: String? = null,    // 전일대비기호
    @SerialName("prdy_ctrt") val prdyCtrt: String? = null,             // 전일대비율
    @SerialName("acml_vol") val acmlVol: String? = null,               // 누적거래량
    @SerialName("acml_tr_pbmn") val acmlTrPbmn: String? = null,        // 누적거래대금
    @SerialName("hts_avls") val htsAvls: String? = null,               // 시가총액
    @SerialName("etf_cnfg_issu_rlim") val etfCnfgIssuRlim: String? = null, // 구성비중 (weight %)
    @SerialName("etf_vltn_amt") val etfVltnAmt: String? = null         // ETF평가금액
)

/**
 * Request parameters for KIS API FHKST121600C0.
 */
data class EtfConstituentParams(
    val etfCode: String
) {
    fun toQueryParams(): Map<String, String> = mapOf(
        "FID_COND_MRKT_DIV_CODE" to "J",
        "FID_INPUT_ISCD" to etfCode,
        "FID_COND_SCR_DIV_CODE" to "11216"
    )
}
