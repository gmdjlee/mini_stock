package com.stockapp.feature.financial.data.dto

import com.stockapp.feature.financial.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Common KIS API response wrapper.
 * Note: Some KIS APIs use "output" while others use "output1" for the data array.
 * This class handles both cases.
 */
@Serializable
data class KisApiResponse<T>(
    @SerialName("rt_cd") val rtCd: String = "",     // 0: success
    @SerialName("msg_cd") val msgCd: String = "",
    @SerialName("msg1") val msg1: String = "",
    val output: T? = null,
    @SerialName("output1") val output1: T? = null
) {
    /**
     * Returns the actual output data, checking both "output" and "output1" fields.
     */
    val actualOutput: T?
        get() = output ?: output1
}

// ========== Balance Sheet (대차대조표) ==========

@Serializable
data class BalanceSheetDto(
    @SerialName("stac_yymm") val stacYymm: String? = null,      // 결산년월
    @SerialName("cras") val cras: String? = null,               // 유동자산
    @SerialName("fxas") val fxas: String? = null,               // 고정자산
    @SerialName("total_aset") val totalAset: String? = null,    // 자산총계
    @SerialName("flow_lblt") val flowLblt: String? = null,      // 유동부채
    @SerialName("fix_lblt") val fixLblt: String? = null,        // 고정부채
    @SerialName("total_lblt") val totalLblt: String? = null,    // 부채총계
    @SerialName("cpfn") val cpfn: String? = null,               // 자본금
    @SerialName("cfp_surp") val cfpSurp: String? = null,        // 자본잉여금
    @SerialName("rere") val rere: String? = null,               // 이익잉여금
    @SerialName("total_cptl") val totalCptl: String? = null     // 자본총계
) {
    fun toDomain(): BalanceSheet? {
        val ym = stacYymm ?: return null
        return BalanceSheet(
            period = FinancialPeriod.fromYearMonth(ym),
            currentAssets = cras?.toLongOrNull(),
            fixedAssets = fxas?.toLongOrNull(),
            totalAssets = totalAset?.toLongOrNull(),
            currentLiabilities = flowLblt?.toLongOrNull(),
            fixedLiabilities = fixLblt?.toLongOrNull(),
            totalLiabilities = totalLblt?.toLongOrNull(),
            capital = cpfn?.toLongOrNull(),
            capitalSurplus = cfpSurp?.toLongOrNull(),
            retainedEarnings = rere?.toLongOrNull(),
            totalEquity = totalCptl?.toLongOrNull()
        )
    }
}

// ========== Income Statement (손익계산서) ==========

@Serializable
data class IncomeStatementDto(
    @SerialName("stac_yymm") val stacYymm: String? = null,        // 결산년월
    @SerialName("sale_account") val saleAccount: String? = null,  // 매출액
    @SerialName("sale_cost") val saleCost: String? = null,        // 매출원가
    @SerialName("sale_totl_prfi") val saleTotlPrfi: String? = null, // 매출총이익
    @SerialName("bsop_prti") val bsopPrti: String? = null,        // 영업이익
    @SerialName("op_prfi") val opPrfi: String? = null,            // 경상이익
    @SerialName("spec_prfi") val specPrfi: String? = null,        // 특별이익
    @SerialName("spec_loss") val specLoss: String? = null,        // 특별손실
    @SerialName("thtr_ntin") val thtrNtin: String? = null         // 당기순이익
) {
    fun toDomain(): IncomeStatement? {
        val ym = stacYymm ?: return null
        return IncomeStatement(
            period = FinancialPeriod.fromYearMonth(ym),
            revenue = saleAccount?.toLongOrNull(),
            costOfSales = saleCost?.toLongOrNull(),
            grossProfit = saleTotlPrfi?.toLongOrNull(),
            operatingProfit = bsopPrti?.toLongOrNull(),
            ordinaryProfit = opPrfi?.toLongOrNull(),
            netIncome = thtrNtin?.toLongOrNull()
        )
    }
}

// ========== Profitability Ratios (수익성비율) ==========

@Serializable
data class ProfitabilityRatiosDto(
    @SerialName("stac_yymm") val stacYymm: String? = null,          // 결산년월
    @SerialName("bsop_prfi_rate") val bsopPrfiRate: String? = null, // 영업이익률
    @SerialName("ntin_rate") val ntinRate: String? = null,          // 순이익률
    @SerialName("roe_val") val roeVal: String? = null,              // ROE
    @SerialName("roa_val") val roaVal: String? = null,              // ROA
    @SerialName("grs") val grs: String? = null                      // 매출총이익률
) {
    fun toDomain(): ProfitabilityRatios? {
        val ym = stacYymm ?: return null
        return ProfitabilityRatios(
            period = FinancialPeriod.fromYearMonth(ym),
            operatingMargin = bsopPrfiRate?.toDoubleOrNull(),
            netMargin = ntinRate?.toDoubleOrNull(),
            roe = roeVal?.toDoubleOrNull(),
            roa = roaVal?.toDoubleOrNull()
        )
    }
}

// ========== Stability Ratios (안정성비율) ==========

@Serializable
data class StabilityRatiosDto(
    @SerialName("stac_yymm") val stacYymm: String? = null,          // 결산년월
    @SerialName("lblt_rate") val lbltRate: String? = null,          // 부채비율
    @SerialName("crnt_rate") val crntRate: String? = null,          // 유동비율
    @SerialName("quck_rate") val quckRate: String? = null,          // 당좌비율
    @SerialName("bram_depn") val bramDepn: String? = null,          // 차입금의존도
    @SerialName("rsrv_rate") val rsrvRate: String? = null,          // 유보율
    @SerialName("inte_cvrg_rate") val inteCvrgRate: String? = null  // 이자보상비율
) {
    fun toDomain(): StabilityRatios? {
        val ym = stacYymm ?: return null
        return StabilityRatios(
            period = FinancialPeriod.fromYearMonth(ym),
            debtRatio = lbltRate?.toDoubleOrNull(),
            currentRatio = crntRate?.toDoubleOrNull(),
            quickRatio = quckRate?.toDoubleOrNull(),
            borrowingDependency = bramDepn?.toDoubleOrNull(),
            interestCoverageRatio = inteCvrgRate?.toDoubleOrNull()
        )
    }
}

// ========== Growth Ratios (성장성비율) ==========

@Serializable
data class GrowthRatiosDto(
    @SerialName("stac_yymm") val stacYymm: String? = null,            // 결산년월
    @SerialName("grs") val grs: String? = null,                       // 매출액증가율
    @SerialName("bsop_prfi_inrt") val bsopPrfiInrt: String? = null,   // 영업이익증가율
    @SerialName("ntin_inrt") val ntinInrt: String? = null,            // 순이익증가율
    @SerialName("cptl_ntin_rate") val cptlNtinRate: String? = null,   // 자기자본증가율
    @SerialName("total_aset_inrt") val totalAsetInrt: String? = null  // 총자산증가율
) {
    fun toDomain(): GrowthRatios? {
        val ym = stacYymm ?: return null
        return GrowthRatios(
            period = FinancialPeriod.fromYearMonth(ym),
            revenueGrowth = grs?.toDoubleOrNull(),
            operatingProfitGrowth = bsopPrfiInrt?.toDoubleOrNull(),
            netIncomeGrowth = ntinInrt?.toDoubleOrNull(),
            equityGrowth = cptlNtinRate?.toDoubleOrNull(),
            totalAssetsGrowth = totalAsetInrt?.toDoubleOrNull()
        )
    }
}

// ========== Financial Ratios (재무비율) ==========

@Serializable
data class FinancialRatiosDto(
    @SerialName("stac_yymm") val stacYymm: String? = null,
    @SerialName("grs") val grs: String? = null,                       // 매출액증가율
    @SerialName("bsop_prfi_inrt") val bsopPrfiInrt: String? = null,   // 영업이익증가율
    @SerialName("ntin_inrt") val ntinInrt: String? = null,            // 순이익증가율
    @SerialName("roe_val") val roeVal: String? = null,                // ROE
    @SerialName("eps") val eps: String? = null,                       // EPS
    @SerialName("sps") val sps: String? = null,                       // 주당매출액
    @SerialName("bps") val bps: String? = null,                       // BPS
    @SerialName("rsrv_rate") val rsrvRate: String? = null,            // 유보율
    @SerialName("lblt_rate") val lbltRate: String? = null             // 부채비율
) {
    fun toDomain(): FinancialRatios? {
        val ym = stacYymm ?: return null
        return FinancialRatios(
            period = FinancialPeriod.fromYearMonth(ym),
            eps = eps?.toDoubleOrNull(),
            bps = bps?.toDoubleOrNull(),
            per = null,
            pbr = null,
            roe = roeVal?.toDoubleOrNull(),
            reserveRatio = rsrvRate?.toDoubleOrNull()
        )
    }
}

// ========== Other Major Ratios (기타주요비율) ==========

@Serializable
data class OtherMajorRatiosDto(
    @SerialName("stac_yymm") val stacYymm: String? = null,
    @SerialName("per") val per: String? = null,
    @SerialName("pbr") val pbr: String? = null,
    @SerialName("pcr") val pcr: String? = null,
    @SerialName("psr") val psr: String? = null,
    @SerialName("ev_ebitda") val evEbitda: String? = null
) {
    fun toDomain(): OtherMajorRatios? {
        val ym = stacYymm ?: return null
        return OtherMajorRatios(
            period = FinancialPeriod.fromYearMonth(ym),
            per = per?.toDoubleOrNull(),
            pbr = pbr?.toDoubleOrNull(),
            pcr = pcr?.toDoubleOrNull(),
            psr = psr?.toDoubleOrNull(),
            evEbitda = evEbitda?.toDoubleOrNull()
        )
    }
}
