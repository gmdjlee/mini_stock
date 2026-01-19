package com.stockapp.feature.analysis.domain.model

import kotlinx.serialization.Serializable

/**
 * Stock analysis data with supply/demand metrics.
 */
data class StockData(
    val ticker: String,
    val name: String,
    val dates: List<String>,      // Reverse chronological order (newest first)
    val mcap: List<Long>,         // Market cap
    val for5d: List<Long>,        // Foreign 5-day net buying
    val ins5d: List<Long>         // Institution 5-day net buying
) {
    /**
     * Get latest (current) market cap in trillion KRW.
     * Data is in reverse chronological order (newest first), so use firstOrNull().
     */
    val latestMcapTrillion: Double
        get() = mcap.firstOrNull()?.let { it / 1_000_000_000_000.0 } ?: 0.0

    /**
     * Get latest (current) foreign net buying in 억원 (100 million KRW).
     * Data is in reverse chronological order (newest first), so use firstOrNull().
     * Note: API returns for_5d in 백만원 (million KRW), so divide by 100 to get 억원.
     */
    val latestFor5dBillion: Double
        get() = for5d.firstOrNull()?.let { it / 100.0 } ?: 0.0

    /**
     * Get latest (current) institution net buying in 억원 (100 million KRW).
     * Data is in reverse chronological order (newest first), so use firstOrNull().
     * Note: API returns ins_5d in 백만원 (million KRW), so divide by 100 to get 억원.
     */
    val latestIns5dBillion: Double
        get() = ins5d.firstOrNull()?.let { it / 100.0 } ?: 0.0

    /**
     * Get total supply (foreign + institution) for latest date.
     * Data is in reverse chronological order (newest first), so use firstOrNull().
     */
    val latestTotalSupply: Long
        get() = (for5d.firstOrNull() ?: 0L) + (ins5d.firstOrNull() ?: 0L)

    /**
     * Get supply ratio for latest date.
     * Data is in reverse chronological order (newest first), so use firstOrNull().
     * Note: for_5d/ins_5d are in 백만원, mcap is in 원, so multiply by 1,000,000 to normalize.
     */
    val latestSupplyRatio: Double
        get() {
            val m = mcap.firstOrNull() ?: return 0.0
            if (m == 0L) return 0.0
            // Convert 백만원 to 원 for proper ratio calculation
            return (latestTotalSupply.toDouble() * 1_000_000) / m
        }
}

/**
 * Python API response for analysis.
 */
@Serializable
data class AnalysisResponse(
    val ok: Boolean,
    val data: AnalysisDataDto? = null,
    val error: AnalysisError? = null
)

@Serializable
data class AnalysisDataDto(
    val ticker: String,
    val name: String,
    val dates: List<String>,
    val mcap: List<Long>,
    val for_5d: List<Long>,
    val ins_5d: List<Long>
) {
    fun toDomain(): StockData = StockData(
        ticker = ticker,
        name = name,
        dates = dates,
        mcap = mcap,
        for5d = for_5d,
        ins5d = ins_5d
    )
}

@Serializable
data class AnalysisError(
    val code: String,
    val msg: String
)

/**
 * Summary for display in UI.
 */
data class AnalysisSummary(
    val ticker: String,
    val name: String,
    val mcapTrillion: Double,
    val for5dBillion: Double,           // In 억원 (100 million KRW)
    val ins5dBillion: Double,           // In 억원 (100 million KRW)
    val supplyRatio: Double,
    val supplySignal: SupplySignal,
    val dates: List<String>,
    val mcapHistory: List<Double>,      // In trillion (조원)
    val for5dHistory: List<Double>,     // In 억원 (100 million KRW)
    val ins5dHistory: List<Double>      // In 억원 (100 million KRW)
)

enum class SupplySignal {
    STRONG_BUY,   // > 0.5%
    BUY,          // > 0.2%
    NEUTRAL,      // -0.2% ~ 0.2%
    SELL,         // < -0.2%
    STRONG_SELL;  // < -0.5%

    companion object {
        fun fromRatio(ratio: Double): SupplySignal = when {
            ratio > 0.005 -> STRONG_BUY
            ratio > 0.002 -> BUY
            ratio < -0.005 -> STRONG_SELL
            ratio < -0.002 -> SELL
            else -> NEUTRAL
        }
    }
}

/**
 * Convert StockData to AnalysisSummary for UI.
 * Note: API returns for_5d/ins_5d in 백만원 (million KRW), convert to 억원 by dividing by 100.
 */
fun StockData.toSummary(): AnalysisSummary {
    return AnalysisSummary(
        ticker = ticker,
        name = name,
        mcapTrillion = latestMcapTrillion,
        for5dBillion = latestFor5dBillion,
        ins5dBillion = latestIns5dBillion,
        supplyRatio = latestSupplyRatio,
        supplySignal = SupplySignal.fromRatio(latestSupplyRatio),
        dates = dates,
        mcapHistory = mcap.map { it / 1_000_000_000_000.0 },
        for5dHistory = for5d.map { it / 100.0 },  // 백만원 → 억원
        ins5dHistory = ins5d.map { it / 100.0 }   // 백만원 → 억원
    )
}
