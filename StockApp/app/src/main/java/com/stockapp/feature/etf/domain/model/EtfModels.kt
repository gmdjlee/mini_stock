package com.stockapp.feature.etf.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ETF information model.
 */
data class EtfInfo(
    val etfCode: String,
    val etfName: String,
    val etfType: String,              // "Active" / "Passive"
    val managementCompany: String,
    val trackingIndex: String,
    val assetClass: String,
    val totalAssets: Double,          // In units of 억원
    val currentPrice: Long,
    val priceChange: Long,
    val priceChangeSign: String,
    val priceChangeRate: Double
)

/**
 * ETF constituent stock model.
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
    val weight: Double,               // %
    val evaluationAmount: Long
)

/**
 * ETF collection result.
 */
data class EtfCollectionResult(
    val etfCode: String,
    val etfName: String,
    val constituents: List<ConstituentStock>,
    val collectedAt: LocalDateTime
)

/**
 * Collection status enum.
 */
enum class CollectionStatus {
    SUCCESS,
    FAILED,
    PARTIAL
}

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
 * Stock ranking item for display.
 */
data class StockRankingItem(
    val rank: Int,
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,           // 합산 평가금액
    val etfCount: Int,               // 포함된 ETF 수
    val previousDayAmount: Long?,    // 전일 금액 (비교용)
    val amountChange: Long?          // 금액 변동
)

/**
 * Stock change type.
 */
enum class StockChangeType {
    NEWLY_INCLUDED,     // 신규 편입
    REMOVED,            // 제외
    WEIGHT_INCREASED,   // 비중 증가
    WEIGHT_DECREASED    // 비중 감소
}

/**
 * Stock change info for display.
 */
data class StockChangeItem(
    val stockCode: String,
    val stockName: String,
    val changeType: StockChangeType,
    val totalAmount: Long,
    val etfNames: List<String>,      // 관련 ETF 목록
    val weightChange: Double?        // 비중 변화량 (증감 시)
)

/**
 * ETF filter configuration.
 */
data class EtfFilterConfig(
    val activeOnly: Boolean = true,
    val includeKeywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = listOf("레버리지", "인버스", "2X", "3X")
)
