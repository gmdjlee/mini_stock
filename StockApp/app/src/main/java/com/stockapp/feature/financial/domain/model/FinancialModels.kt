package com.stockapp.feature.financial.domain.model

import kotlinx.serialization.Serializable

/**
 * Tab type for Financial Info screen.
 */
enum class FinancialTab(val label: String) {
    PROFITABILITY("수익성"),
    STABILITY("안정성")
}

/**
 * Settlement period data (결산년월).
 */
data class FinancialPeriod(
    val yearMonth: String,  // YYYYMM format (e.g., "202312")
    val year: Int,
    val quarter: Int        // 1-4 for quarters, 0 for annual
) {
    /**
     * Format for display (e.g., "2023.12" or "23.12")
     */
    fun toDisplayString(short: Boolean = false): String {
        val y = if (short) yearMonth.substring(2, 4) else yearMonth.substring(0, 4)
        val m = yearMonth.substring(4, 6)
        return "$y.$m"
    }

    companion object {
        fun fromYearMonth(ym: String): FinancialPeriod {
            val year = ym.substring(0, 4).toIntOrNull() ?: 0
            val month = ym.substring(4, 6).toIntOrNull() ?: 0
            val quarter = when (month) {
                3 -> 1
                6 -> 2
                9 -> 3
                12 -> 4
                else -> 0
            }
            return FinancialPeriod(ym, year, quarter)
        }
    }
}

/**
 * Balance Sheet data (대차대조표).
 */
data class BalanceSheet(
    val period: FinancialPeriod,
    val currentAssets: Long?,        // 유동자산 (cras)
    val fixedAssets: Long?,          // 고정자산 (fxas)
    val totalAssets: Long?,          // 자산총계 (total_aset)
    val currentLiabilities: Long?,   // 유동부채 (flow_lblt)
    val fixedLiabilities: Long?,     // 고정부채 (fix_lblt)
    val totalLiabilities: Long?,     // 부채총계 (total_lblt)
    val capital: Long?,              // 자본금 (cpfn)
    val capitalSurplus: Long?,       // 자본잉여금 (cfp_surp)
    val retainedEarnings: Long?,     // 이익잉여금 (rere)
    val totalEquity: Long?           // 자본총계 (total_cptl)
)

/**
 * Income Statement data (손익계산서).
 */
data class IncomeStatement(
    val period: FinancialPeriod,
    val revenue: Long?,              // 매출액 (sale_account)
    val costOfSales: Long?,          // 매출원가 (sale_cost)
    val grossProfit: Long?,          // 매출총이익 (sale_totl_prfi)
    val operatingProfit: Long?,      // 영업이익 (bsop_prti)
    val ordinaryProfit: Long?,       // 경상이익 (op_prfi)
    val netIncome: Long?             // 당기순이익 (thtr_ntin)
)

/**
 * Profitability Ratios (수익성비율).
 */
data class ProfitabilityRatios(
    val period: FinancialPeriod,
    val operatingMargin: Double?,    // 영업이익률 (bsop_prfi_rate)
    val netMargin: Double?,          // 순이익률 (ntin_rate)
    val roe: Double?,                // 자기자본이익률 ROE (roe_val)
    val roa: Double?                 // 총자산이익률 ROA (roa_val)
)

/**
 * Stability Ratios (안정성비율).
 */
data class StabilityRatios(
    val period: FinancialPeriod,
    val debtRatio: Double?,          // 부채비율 (lblt_rate)
    val currentRatio: Double?,       // 유동비율 (crnt_rate)
    val quickRatio: Double?,         // 당좌비율 (quck_rate)
    val borrowingDependency: Double?, // 차입금의존도 (bram_depn)
    val interestCoverageRatio: Double? // 이자보상비율 (inte_cvrg_rate)
)

/**
 * Growth Ratios (성장성비율).
 */
data class GrowthRatios(
    val period: FinancialPeriod,
    val revenueGrowth: Double?,           // 매출액증가율 (grs)
    val operatingProfitGrowth: Double?,   // 영업이익증가율 (bsop_prfi_inrt)
    val netIncomeGrowth: Double?,         // 순이익증가율 (ntin_inrt)
    val equityGrowth: Double?,            // 자기자본증가율 (cptl_ntin_rate)
    val totalAssetsGrowth: Double?        // 총자산증가율 (total_aset_inrt)
)

/**
 * Financial Ratios (재무비율) - Combined view.
 */
data class FinancialRatios(
    val period: FinancialPeriod,
    val eps: Double?,               // 주당순이익 EPS
    val bps: Double?,               // 주당순자산 BPS
    val per: Double?,               // 주가수익비율 PER
    val pbr: Double?,               // 주가순자산비율 PBR
    val roe: Double?,               // 자기자본이익률 ROE
    val reserveRatio: Double?       // 유보율
)

/**
 * Other Major Ratios (기타주요비율).
 */
data class OtherMajorRatios(
    val period: FinancialPeriod,
    val per: Double?,
    val pbr: Double?,
    val pcr: Double?,
    val psr: Double?,
    val evEbitda: Double?
)

/**
 * Merged Financial Data by settlement period.
 * All data from different APIs merged by stac_yymm.
 */
data class FinancialData(
    val ticker: String,
    val name: String,
    val periods: List<String>,                    // YYYYMM list
    val balanceSheets: Map<String, BalanceSheet>, // key: YYYYMM
    val incomeStatements: Map<String, IncomeStatement>,
    val profitabilityRatios: Map<String, ProfitabilityRatios>,
    val stabilityRatios: Map<String, StabilityRatios>,
    val growthRatios: Map<String, GrowthRatios>,
    val financialRatios: Map<String, FinancialRatios>,
    val otherMajorRatios: Map<String, OtherMajorRatios>
)

/**
 * Financial Summary for UI display.
 * Pre-processed data ready for charts.
 */
data class FinancialSummary(
    val ticker: String,
    val name: String,
    // Sorted periods (oldest to newest for charts)
    val periods: List<String>,           // ["202303", "202306", "202309", "202312"]
    val displayPeriods: List<String>,    // ["23.03", "23.06", "23.09", "23.12"]

    // ===== 수익성 탭 (Profitability Tab) =====
    // Bar chart: 매출액, 영업이익, 당기순이익
    val revenues: List<Long>,            // 매출액 (억원)
    val operatingProfits: List<Long>,    // 영업이익 (억원)
    val netIncomes: List<Long>,          // 당기순이익 (억원)

    // Line chart: 매출액/영업이익/순이익 증가율
    val revenueGrowthRates: List<Double>,           // 매출액 증가율 (%)
    val operatingProfitGrowthRates: List<Double>,   // 영업이익 증가율 (%)
    val netIncomeGrowthRates: List<Double>,         // 순이익 증가율 (%)

    // Line chart: 자기자본/총자산 증가율
    val equityGrowthRates: List<Double>,            // 자기자본 증가율 (%)
    val totalAssetsGrowthRates: List<Double>,       // 총자산 증가율 (%)

    // ===== 안정성 탭 (Stability Tab) =====
    // 부채비율, 유동비율, 차입금 의존도
    val debtRatios: List<Double>,                   // 부채비율 (%)
    val currentRatios: List<Double>,                // 유동비율 (%)
    val borrowingDependencies: List<Double>         // 차입금 의존도 (%)
) {
    /**
     * Get latest values for summary display.
     */
    val latestRevenue: Long? get() = revenues.lastOrNull()
    val latestOperatingProfit: Long? get() = operatingProfits.lastOrNull()
    val latestNetIncome: Long? get() = netIncomes.lastOrNull()
    val latestDebtRatio: Double? get() = debtRatios.lastOrNull()
    val latestCurrentRatio: Double? get() = currentRatios.lastOrNull()

    /**
     * Check if data is available for charts.
     * Only returns true if there's at least one non-zero value.
     */
    val hasProfitabilityData: Boolean
        get() = revenues.any { it != 0L } ||
                operatingProfits.any { it != 0L } ||
                netIncomes.any { it != 0L }

    val hasGrowthData: Boolean
        get() = revenueGrowthRates.any { it != 0.0 } ||
                operatingProfitGrowthRates.any { it != 0.0 } ||
                netIncomeGrowthRates.any { it != 0.0 }

    val hasAssetGrowthData: Boolean
        get() = equityGrowthRates.any { it != 0.0 } ||
                totalAssetsGrowthRates.any { it != 0.0 }

    val hasStabilityData: Boolean
        get() = debtRatios.any { it != 0.0 } ||
                currentRatios.any { it != 0.0 } ||
                borrowingDependencies.any { it != 0.0 }
}

/**
 * Extension to convert FinancialData to FinancialSummary.
 */
fun FinancialData.toSummary(): FinancialSummary {
    // Sort periods oldest to newest
    val sortedPeriods = periods.sorted()

    return FinancialSummary(
        ticker = ticker,
        name = name,
        periods = sortedPeriods,
        displayPeriods = sortedPeriods.map { FinancialPeriod.fromYearMonth(it).toDisplayString(short = true) },

        // Income statement data (convert to 억원)
        revenues = sortedPeriods.map { period ->
            incomeStatements[period]?.revenue?.let { it / 100_000_000 } ?: 0L
        },
        operatingProfits = sortedPeriods.map { period ->
            incomeStatements[period]?.operatingProfit?.let { it / 100_000_000 } ?: 0L
        },
        netIncomes = sortedPeriods.map { period ->
            incomeStatements[period]?.netIncome?.let { it / 100_000_000 } ?: 0L
        },

        // Growth rates
        revenueGrowthRates = sortedPeriods.map { period ->
            growthRatios[period]?.revenueGrowth ?: 0.0
        },
        operatingProfitGrowthRates = sortedPeriods.map { period ->
            growthRatios[period]?.operatingProfitGrowth ?: 0.0
        },
        netIncomeGrowthRates = sortedPeriods.map { period ->
            growthRatios[period]?.netIncomeGrowth ?: 0.0
        },
        equityGrowthRates = sortedPeriods.map { period ->
            growthRatios[period]?.equityGrowth ?: 0.0
        },
        totalAssetsGrowthRates = sortedPeriods.map { period ->
            growthRatios[period]?.totalAssetsGrowth ?: 0.0
        },

        // Stability ratios
        debtRatios = sortedPeriods.map { period ->
            stabilityRatios[period]?.debtRatio ?: 0.0
        },
        currentRatios = sortedPeriods.map { period ->
            stabilityRatios[period]?.currentRatio ?: 0.0
        },
        borrowingDependencies = sortedPeriods.map { period ->
            stabilityRatios[period]?.borrowingDependency ?: 0.0
        }
    )
}

/**
 * Serializable version for caching.
 */
@Serializable
data class FinancialDataCache(
    val ticker: String,
    val name: String,
    val periods: List<String>,
    val balanceSheets: List<BalanceSheetCache>,
    val incomeStatements: List<IncomeStatementCache>,
    val profitabilityRatios: List<ProfitabilityRatiosCache>,
    val stabilityRatios: List<StabilityRatiosCache>,
    val growthRatios: List<GrowthRatiosCache>
)

@Serializable
data class BalanceSheetCache(
    val yearMonth: String,
    val currentAssets: Long?,
    val fixedAssets: Long?,
    val totalAssets: Long?,
    val currentLiabilities: Long?,
    val fixedLiabilities: Long?,
    val totalLiabilities: Long?,
    val capital: Long?,
    val capitalSurplus: Long?,
    val retainedEarnings: Long?,
    val totalEquity: Long?
)

@Serializable
data class IncomeStatementCache(
    val yearMonth: String,
    val revenue: Long?,
    val costOfSales: Long?,
    val grossProfit: Long?,
    val operatingProfit: Long?,
    val ordinaryProfit: Long?,
    val netIncome: Long?
)

@Serializable
data class ProfitabilityRatiosCache(
    val yearMonth: String,
    val operatingMargin: Double?,
    val netMargin: Double?,
    val roe: Double?,
    val roa: Double?
)

@Serializable
data class StabilityRatiosCache(
    val yearMonth: String,
    val debtRatio: Double?,
    val currentRatio: Double?,
    val quickRatio: Double?,
    val borrowingDependency: Double?,
    val interestCoverageRatio: Double?
)

@Serializable
data class GrowthRatiosCache(
    val yearMonth: String,
    val revenueGrowth: Double?,
    val operatingProfitGrowth: Double?,
    val netIncomeGrowth: Double?,
    val equityGrowth: Double?,
    val totalAssetsGrowth: Double?
)

/**
 * Convert FinancialData to cache format.
 */
fun FinancialData.toCache(): FinancialDataCache = FinancialDataCache(
    ticker = ticker,
    name = name,
    periods = periods,
    balanceSheets = balanceSheets.map { (ym, bs) ->
        BalanceSheetCache(
            yearMonth = ym,
            currentAssets = bs.currentAssets,
            fixedAssets = bs.fixedAssets,
            totalAssets = bs.totalAssets,
            currentLiabilities = bs.currentLiabilities,
            fixedLiabilities = bs.fixedLiabilities,
            totalLiabilities = bs.totalLiabilities,
            capital = bs.capital,
            capitalSurplus = bs.capitalSurplus,
            retainedEarnings = bs.retainedEarnings,
            totalEquity = bs.totalEquity
        )
    },
    incomeStatements = incomeStatements.map { (ym, is_) ->
        IncomeStatementCache(
            yearMonth = ym,
            revenue = is_.revenue,
            costOfSales = is_.costOfSales,
            grossProfit = is_.grossProfit,
            operatingProfit = is_.operatingProfit,
            ordinaryProfit = is_.ordinaryProfit,
            netIncome = is_.netIncome
        )
    },
    profitabilityRatios = profitabilityRatios.map { (ym, pr) ->
        ProfitabilityRatiosCache(
            yearMonth = ym,
            operatingMargin = pr.operatingMargin,
            netMargin = pr.netMargin,
            roe = pr.roe,
            roa = pr.roa
        )
    },
    stabilityRatios = stabilityRatios.map { (ym, sr) ->
        StabilityRatiosCache(
            yearMonth = ym,
            debtRatio = sr.debtRatio,
            currentRatio = sr.currentRatio,
            quickRatio = sr.quickRatio,
            borrowingDependency = sr.borrowingDependency,
            interestCoverageRatio = sr.interestCoverageRatio
        )
    },
    growthRatios = growthRatios.map { (ym, gr) ->
        GrowthRatiosCache(
            yearMonth = ym,
            revenueGrowth = gr.revenueGrowth,
            operatingProfitGrowth = gr.operatingProfitGrowth,
            netIncomeGrowth = gr.netIncomeGrowth,
            equityGrowth = gr.equityGrowth,
            totalAssetsGrowth = gr.totalAssetsGrowth
        )
    }
)

/**
 * Convert cache back to FinancialData.
 */
fun FinancialDataCache.toData(): FinancialData = FinancialData(
    ticker = ticker,
    name = name,
    periods = periods,
    balanceSheets = balanceSheets.associate { bs ->
        bs.yearMonth to BalanceSheet(
            period = FinancialPeriod.fromYearMonth(bs.yearMonth),
            currentAssets = bs.currentAssets,
            fixedAssets = bs.fixedAssets,
            totalAssets = bs.totalAssets,
            currentLiabilities = bs.currentLiabilities,
            fixedLiabilities = bs.fixedLiabilities,
            totalLiabilities = bs.totalLiabilities,
            capital = bs.capital,
            capitalSurplus = bs.capitalSurplus,
            retainedEarnings = bs.retainedEarnings,
            totalEquity = bs.totalEquity
        )
    },
    incomeStatements = incomeStatements.associate { is_ ->
        is_.yearMonth to IncomeStatement(
            period = FinancialPeriod.fromYearMonth(is_.yearMonth),
            revenue = is_.revenue,
            costOfSales = is_.costOfSales,
            grossProfit = is_.grossProfit,
            operatingProfit = is_.operatingProfit,
            ordinaryProfit = is_.ordinaryProfit,
            netIncome = is_.netIncome
        )
    },
    profitabilityRatios = profitabilityRatios.associate { pr ->
        pr.yearMonth to ProfitabilityRatios(
            period = FinancialPeriod.fromYearMonth(pr.yearMonth),
            operatingMargin = pr.operatingMargin,
            netMargin = pr.netMargin,
            roe = pr.roe,
            roa = pr.roa
        )
    },
    stabilityRatios = stabilityRatios.associate { sr ->
        sr.yearMonth to StabilityRatios(
            period = FinancialPeriod.fromYearMonth(sr.yearMonth),
            debtRatio = sr.debtRatio,
            currentRatio = sr.currentRatio,
            quickRatio = sr.quickRatio,
            borrowingDependency = sr.borrowingDependency,
            interestCoverageRatio = sr.interestCoverageRatio
        )
    },
    growthRatios = growthRatios.associate { gr ->
        gr.yearMonth to GrowthRatios(
            period = FinancialPeriod.fromYearMonth(gr.yearMonth),
            revenueGrowth = gr.revenueGrowth,
            operatingProfitGrowth = gr.operatingProfitGrowth,
            netIncomeGrowth = gr.netIncomeGrowth,
            equityGrowth = gr.equityGrowth,
            totalAssetsGrowth = gr.totalAssetsGrowth
        )
    },
    financialRatios = emptyMap(),
    otherMajorRatios = emptyMap()
)
