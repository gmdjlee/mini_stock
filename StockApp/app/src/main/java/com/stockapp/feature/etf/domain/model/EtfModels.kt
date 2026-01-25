package com.stockapp.feature.etf.domain.model

/**
 * ETF type enumeration.
 */
enum class EtfType(val value: String, val displayName: String) {
    ACTIVE("Active", "액티브"),
    PASSIVE("Passive", "패시브");

    companion object {
        fun fromValue(value: String): EtfType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: PASSIVE
    }
}

/**
 * Filter type enumeration for ETF keywords.
 */
enum class FilterType(val value: String, val displayName: String) {
    INCLUDE("INCLUDE", "포함"),
    EXCLUDE("EXCLUDE", "제외");

    companion object {
        fun fromValue(value: String): FilterType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: INCLUDE
    }
}

/**
 * Collection status enumeration.
 */
enum class CollectionStatus(val value: String, val displayName: String) {
    SUCCESS("SUCCESS", "성공"),
    FAILED("FAILED", "실패"),
    PARTIAL("PARTIAL", "부분 완료"),
    IN_PROGRESS("IN_PROGRESS", "진행 중");

    companion object {
        fun fromValue(value: String): CollectionStatus =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: FAILED
    }
}

/**
 * ETF basic information domain model.
 */
data class EtfInfo(
    val etfCode: String,
    val etfName: String,
    val etfType: EtfType,
    val managementCompany: String,
    val trackingIndex: String,
    val assetClass: String,
    val totalAssets: Double,
    val isFiltered: Boolean,
    val updatedAt: Long
)

/**
 * ETF constituent stock domain model.
 */
data class EtfConstituent(
    val etfCode: String,
    val etfName: String,
    val stockCode: String,
    val stockName: String,
    val currentPrice: Int,
    val priceChange: Int,
    val priceChangeSign: String,
    val priceChangeRate: Double,
    val volume: Long,
    val tradingValue: Long,
    val marketCap: Long,
    val weight: Double,
    val evaluationAmount: Long,
    val collectedDate: String,
    val collectedAt: Long
)

/**
 * ETF filter keyword domain model.
 */
data class EtfKeyword(
    val id: Long,
    val keyword: String,
    val filterType: FilterType,
    val isEnabled: Boolean,
    val createdAt: Long
)

/**
 * ETF collection history domain model.
 */
data class CollectionHistory(
    val id: Long,
    val collectedDate: String,
    val totalEtfs: Int,
    val totalConstituents: Int,
    val status: CollectionStatus,
    val errorMessage: String?,
    val startedAt: Long,
    val completedAt: Long?
)

/**
 * Stock ranking by total evaluation amount across ETFs.
 */
data class StockRanking(
    val rank: Int,
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfCount: Int
) {
    /**
     * Get total amount in 억원 (hundred million KRW).
     */
    val totalAmountInEok: Double
        get() = totalAmount / 100_000_000.0

    /**
     * Get total amount in 조원 (trillion KRW).
     */
    val totalAmountInJo: Double
        get() = totalAmount / 1_000_000_000_000.0
}

/**
 * Stock change information (newly included, removed, weight changed).
 */
data class StockChange(
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfNames: List<String>
) {
    /**
     * Get total amount in 억원 (hundred million KRW).
     */
    val totalAmountInEok: Double
        get() = totalAmount / 100_000_000.0

    /**
     * Get ETF names as comma-separated string.
     */
    val etfNamesFormatted: String
        get() = etfNames.joinToString(", ")
}

/**
 * Date range for data availability.
 */
data class EtfDateRange(
    val startDate: String?,
    val endDate: String?
) {
    val isValid: Boolean
        get() = startDate != null && endDate != null
}

/**
 * Amount history data point for chart visualization.
 */
data class AmountHistory(
    val date: String,
    val totalAmount: Long
) {
    val totalAmountInEok: Double
        get() = totalAmount / 100_000_000.0
}

/**
 * Weight history data point for chart visualization.
 */
data class WeightHistory(
    val date: String,
    val avgWeight: Double
)
