package com.stockapp.feature.search.domain.model

import kotlinx.serialization.Serializable

/**
 * Stock domain model.
 */
data class Stock(
    val ticker: String,
    val name: String,
    val market: Market
)

enum class Market {
    KOSPI, KOSDAQ, OTHER;

    companion object {
        fun fromString(value: String): Market = when (value.uppercase()) {
            "KOSPI" -> KOSPI
            "KOSDAQ" -> KOSDAQ
            else -> OTHER
        }
    }
}

/**
 * Python API response for search.
 */
@Serializable
data class SearchResponse(
    val ok: Boolean,
    val data: List<StockDto>? = null,
    val error: SearchError? = null
)

@Serializable
data class StockDto(
    val ticker: String,
    val name: String,
    val market: String
) {
    fun toDomain(): Stock = Stock(
        ticker = ticker,
        name = name,
        market = Market.fromString(market)
    )
}

@Serializable
data class SearchError(
    val code: String,
    val msg: String
)
