package com.stockapp.feature.etf.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

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
    val isFiltered: Boolean = false,
    val updatedAt: Long = 0L,
    val currentPrice: Long = 0L,
    val priceChange: Long = 0L,
    val priceChangeSign: String = "",
    val priceChangeRate: Double = 0.0
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
 * Simplified constituent stock for collection result.
 */
data class ConstituentStock(
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
    /** API's business date (stck_bsop_date) in DB format (YYYY-MM-DD) */
    val businessDate: String? = null
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
 * ETF collection result for single ETF.
 */
data class EtfCollectionResult(
    val etfCode: String,
    val etfName: String,
    val constituents: List<ConstituentStock>,
    val collectedAt: LocalDateTime,
    /** API's business date (stck_bsop_date) in DB format (YYYY-MM-DD) */
    val businessDate: String? = null
)

/**
 * Full collection result for multiple ETFs.
 */
data class FullCollectionResult(
    val collectedDate: LocalDate,
    val totalEtfs: Int,
    val totalConstituents: Int,
    val successCount: Int,
    val failedCount: Int,
    val status: CollectionStatus,
    val errorMessage: String?,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime
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
    val totalAmountInEok: Double
        get() = totalAmount / 100_000_000.0

    val totalAmountInJo: Double
        get() = totalAmount / 1_000_000_000_000.0
}

/**
 * Stock ranking item for display (with change data).
 */
data class StockRankingItem(
    val rank: Int,
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfCount: Int,
    val previousDayAmount: Long?,
    val amountChange: Long?
)

/**
 * Stock change type.
 */
enum class StockChangeType {
    NEWLY_INCLUDED,
    REMOVED,
    WEIGHT_INCREASED,
    WEIGHT_DECREASED
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
    val totalAmountInEok: Double
        get() = totalAmount / 100_000_000.0

    val etfNamesFormatted: String
        get() = etfNames.joinToString(", ")
}

/**
 * Stock change info for display (with type).
 */
data class StockChangeItem(
    val stockCode: String,
    val stockName: String,
    val changeType: StockChangeType,
    val totalAmount: Long,
    val etfNames: List<String>,
    val weightChange: Double?
)

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

/**
 * ETF filter configuration.
 */
data class EtfFilterConfig(
    val activeOnly: Boolean = true,
    val includeKeywords: List<String> = DEFAULT_INCLUDE_KEYWORDS,
    val excludeKeywords: List<String> = DEFAULT_EXCLUDE_KEYWORDS
) {
    companion object {
        val DEFAULT_INCLUDE_KEYWORDS = listOf(
            "반도체", "수급", "배당", "신재생", "2차전지", "이노베이션",
            "AI", "인프라", "소비", "코스피", "친환경", "테크",
            "수출", "로봇", "컬처", "밸류업", "바이오", "헬스케어"
        )
        val DEFAULT_EXCLUDE_KEYWORDS = listOf(
            "인버스", "레버리지", "곱버스", "2X", "3X",
            "차이나", "채권", "달러", "아시아", "미국", "일본",
            "금리", "금융채", "회사채", "China"
        )
    }
}

// ==================== ETF Statistics Models (Phase 2) ====================

/**
 * Date range option for statistics queries.
 */
enum class DateRangeOption(val days: Int, val label: String) {
    DAY(1, "1일"),
    WEEK(7, "1주"),
    MONTH(30, "1개월"),
    THREE_MONTHS(90, "3개월"),
    SIX_MONTHS(180, "6개월"),
    YEAR(365, "1년"),
    ALL(-1, "전체")
}

/**
 * Holding status for stock changes.
 */
enum class HoldingStatus(val displayName: String) {
    NEW("신규"),
    INCREASE("증가"),
    DECREASE("감소"),
    REMOVED("편출"),
    MAINTAIN("유지")
}

/**
 * Daily ETF statistics domain model.
 */
data class DailyEtfStatistics(
    val date: String,
    val newStockCount: Int,
    val newStockAmount: Long,
    val removedStockCount: Int,
    val removedStockAmount: Long,
    val increasedStockCount: Int,
    val increasedStockAmount: Long,
    val decreasedStockCount: Int,
    val decreasedStockAmount: Long,
    val cashDepositAmount: Long,
    val cashDepositChange: Long,
    val cashDepositChangeRate: Double,
    val totalEtfCount: Int,
    val totalHoldingAmount: Long,
    val calculatedAt: Long
)

/**
 * Comparison result for period-based analysis.
 */
data class ComparisonResult(
    val currentDate: String,
    val previousDate: String,
    val items: List<HoldingWithComparison>,
    val summary: ComparisonSummary
)

/**
 * Holding comparison data with status.
 */
data class HoldingWithComparison(
    val stockCode: String,
    val stockName: String,
    val currentWeight: Double,
    val previousWeight: Double,
    val currentAmount: Long,
    val previousAmount: Long,
    val status: HoldingStatus,
    val etfNames: List<String>
) {
    val weightChange: Double
        get() = currentWeight - previousWeight

    val amountChange: Long
        get() = currentAmount - previousAmount
}

/**
 * Summary of comparison results.
 */
data class ComparisonSummary(
    val newCount: Int,
    val removedCount: Int,
    val increasedCount: Int,
    val decreasedCount: Int,
    val maintainCount: Int
)

/**
 * Cash deposit trend data point.
 */
data class CashDepositTrend(
    val date: String,
    val totalAmount: Long,
    val changeAmount: Long,
    val changeRate: Double
)

/**
 * ETF cash detail for individual ETF.
 */
data class EtfCashDetail(
    val etfCode: String,
    val etfName: String,
    val cashAmount: Long,
    val cashWeight: Double,
    val cashName: String
)

/**
 * Stock analysis result for individual stock.
 */
data class StockAnalysisResult(
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfCount: Int,
    val amountHistory: List<AmountHistory>,
    val weightHistory: List<WeightHistory>,
    val containingEtfs: List<ContainingEtfInfo>
)

/**
 * ETF information containing a specific stock.
 */
data class ContainingEtfInfo(
    val etfCode: String,
    val etfName: String,
    val weight: Double,
    val amount: Long,
    val collectedDate: String
)

// ==================== Theme List Models (Phase 3) ====================

/**
 * Active ETF summary for Theme List display.
 */
data class ActiveEtfSummary(
    val etfCode: String,
    val etfName: String,
    val etfType: EtfType,
    val managementCompany: String,
    val constituentCount: Int,
    val totalEvaluationAmount: Long,
    val latestCollectedDate: String
) {
    val totalAmountInEok: Double
        get() = totalEvaluationAmount / 100_000_000.0

    val totalAmountInJo: Double
        get() = totalEvaluationAmount / 1_000_000_000_000.0
}

/**
 * ETF detail with constituent list for Theme List detail view.
 */
data class EtfDetailInfo(
    val etfCode: String,
    val etfName: String,
    val etfType: EtfType,
    val managementCompany: String,
    val constituents: List<EtfConstituent>,
    val totalEvaluationAmount: Long,
    val collectedDate: String
) {
    val constituentCount: Int
        get() = constituents.size

    val totalAmountInEok: Double
        get() = totalEvaluationAmount / 100_000_000.0
}

// ==================== Missing Date Analysis Models ====================

/**
 * Result of missing collection date analysis.
 * Used to identify gaps in ETF data collection history.
 */
data class MissingDatesResult(
    /** First date in collected data range (YYYY-MM-DD) */
    val dataStartDate: String?,
    /** Last date in collected data range (YYYY-MM-DD) */
    val dataEndDate: String?,
    /** List of missing trading dates in YYYY-MM-DD format */
    val missingDates: List<String>,
    /** Total number of trading days in the range */
    val totalTradingDays: Int,
    /** Number of days with collected data */
    val collectedDays: Int
) {
    /** Data collection coverage percentage (0-100) */
    val coveragePercent: Double
        get() = if (totalTradingDays > 0) {
            (collectedDays.toDouble() / totalTradingDays) * 100
        } else 0.0

    /** True if there are missing dates */
    val hasMissingDates: Boolean
        get() = missingDates.isNotEmpty()

    /** Number of missing days */
    val missingDaysCount: Int
        get() = missingDates.size
}

// ==================== Ranking Sort Models ====================

/**
 * Sorting column options for stock ranking table.
 */
enum class RankingSortColumn(val displayName: String) {
    TOTAL_AMOUNT("합산금액"),
    ETF_COUNT("ETF수"),
    AMOUNT_CHANGE("변동")
}

/**
 * Sorting direction.
 */
enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun toggle(): SortDirection = when (this) {
        ASCENDING -> DESCENDING
        DESCENDING -> ASCENDING
    }
}

/**
 * Individual sort criterion with column and direction.
 * Used as part of multi-column sorting.
 */
data class SortCriteria(
    val column: RankingSortColumn,
    val direction: SortDirection = SortDirection.DESCENDING
)

/**
 * Multi-column ranking sort state.
 * Supports up to 3 columns with priority order (first = primary sort key).
 */
data class RankingSortState(
    val criteria: List<SortCriteria> = listOf(
        SortCriteria(RankingSortColumn.TOTAL_AMOUNT, SortDirection.DESCENDING)
    )
) {
    companion object {
        const val MAX_SORT_COLUMNS = 3
    }

    /**
     * Get priority (1-based) for a column, null if not in sort list.
     */
    fun getPriority(column: RankingSortColumn): Int? {
        val index = criteria.indexOfFirst { it.column == column }
        return if (index >= 0) index + 1 else null
    }

    /**
     * Get direction for a column, null if not in sort list.
     */
    fun getDirection(column: RankingSortColumn): SortDirection? {
        return criteria.find { it.column == column }?.direction
    }

    /**
     * Check if column is actively sorted.
     */
    fun isActive(column: RankingSortColumn): Boolean {
        return criteria.any { it.column == column }
    }

    /**
     * Handle column click: add to list, toggle direction, remove if ascending, or replace last if at max.
     * Click cycle: OFF → DESCENDING → ASCENDING → OFF (removed)
     */
    fun onColumnClick(clickedColumn: RankingSortColumn): RankingSortState {
        val existingIndex = criteria.indexOfFirst { it.column == clickedColumn }

        return when {
            // Column not in list: add at end (if under max) or replace last
            existingIndex < 0 -> {
                if (criteria.size < MAX_SORT_COLUMNS) {
                    copy(criteria = criteria + SortCriteria(clickedColumn))
                } else {
                    // At max: replace last column
                    copy(criteria = criteria.dropLast(1) + SortCriteria(clickedColumn))
                }
            }
            // Column in list with ASCENDING: remove from list (해제)
            else -> {
                val current = criteria[existingIndex]
                if (current.direction == SortDirection.ASCENDING) {
                    // Remove column - this is the "해제" action
                    removeColumn(clickedColumn)
                } else {
                    // Toggle direction (DESCENDING -> ASCENDING)
                    val updated = current.copy(direction = current.direction.toggle())
                    copy(criteria = criteria.toMutableList().apply {
                        set(existingIndex, updated)
                    })
                }
            }
        }
    }

    /**
     * Remove a column from sort list.
     * If list becomes empty, reset to default.
     */
    fun removeColumn(column: RankingSortColumn): RankingSortState {
        val filtered = criteria.filter { it.column != column }
        return if (filtered.isEmpty()) {
            // Always have at least one sort column - reset to default
            RankingSortState()
        } else {
            copy(criteria = filtered)
        }
    }

    /**
     * Reset to default sort (TOTAL_AMOUNT descending).
     */
    fun reset(): RankingSortState = RankingSortState()

    /**
     * Get formatted sort description for display.
     * Example: "합산금액 > ETF수 > 변동"
     */
    fun getDisplayDescription(): String {
        return criteria.joinToString(" > ") { it.column.displayName }
    }
}
