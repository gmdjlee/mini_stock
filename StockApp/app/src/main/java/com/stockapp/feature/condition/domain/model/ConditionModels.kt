package com.stockapp.feature.condition.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========== Condition Domain Models ==========

/**
 * Condition search item.
 */
data class Condition(
    val idx: String,
    val name: String
)

/**
 * Condition search result.
 */
data class ConditionResult(
    val condition: Condition,
    val stocks: List<ConditionStock>
)

/**
 * Stock from condition search result.
 */
data class ConditionStock(
    val ticker: String,
    val name: String,
    val price: Int,
    val change: Double
)

// ========== DTO Classes ==========

@Serializable
data class ConditionListResponse(
    val ok: Boolean,
    val data: List<ConditionDto>? = null,
    val error: ConditionError? = null
)

@Serializable
data class ConditionDto(
    val idx: String,
    val name: String
) {
    fun toDomain(): Condition = Condition(
        idx = idx,
        name = name
    )
}

@Serializable
data class ConditionSearchResponse(
    val ok: Boolean,
    val data: ConditionResultDto? = null,
    val error: ConditionError? = null
)

@Serializable
data class ConditionResultDto(
    val condition: ConditionDto,
    val stocks: List<ConditionStockDto>
) {
    fun toDomain(): ConditionResult = ConditionResult(
        condition = condition.toDomain(),
        stocks = stocks.map { it.toDomain() }
    )
}

@Serializable
data class ConditionStockDto(
    val ticker: String,
    val name: String,
    val price: Int,
    val change: Double
) {
    fun toDomain(): ConditionStock = ConditionStock(
        ticker = ticker,
        name = name,
        price = price,
        change = change
    )
}

@Serializable
data class ConditionError(
    val code: String,
    val msg: String
)

// ========== Extension Functions ==========

/**
 * Format price with comma separator.
 */
fun ConditionStock.formattedPrice(): String {
    return "%,d원".format(price)
}

/**
 * Format change percentage.
 */
fun ConditionStock.formattedChange(): String {
    val prefix = if (change > 0) "+" else ""
    return "$prefix%.2f%%".format(change)
}

/**
 * Check if change is positive.
 */
fun ConditionStock.isPositiveChange(): Boolean = change > 0

/**
 * Check if change is negative.
 */
fun ConditionStock.isNegativeChange(): Boolean = change < 0
