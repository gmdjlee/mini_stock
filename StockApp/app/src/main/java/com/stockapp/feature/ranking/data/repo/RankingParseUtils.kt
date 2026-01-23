package com.stockapp.feature.ranking.data.repo

/**
 * API configuration for Kiwoom REST API calls.
 */
internal data class ApiConfig(
    val appKey: String,
    val secretKey: String,
    val baseUrl: String
)

/**
 * Utility functions for parsing ranking API responses.
 */
internal object RankingParseUtils {

    private val TICKER_SUFFIXES = listOf("_AL", "_KS", "_KQ")

    /** Clean ticker code by removing common suffixes. */
    fun cleanTicker(value: String?): String =
        value?.let { ticker ->
            TICKER_SUFFIXES.fold(ticker) { acc, suffix -> acc.replace(suffix, "") }.trim()
        } ?: ""

    /** Parse Long value from API response string. */
    fun parseLong(value: String?): Long =
        value?.replace(",", "")?.replace("+", "")?.trim()?.toLongOrNull() ?: 0

    /** Parse Double value from API response string. */
    fun parseDouble(value: String?): Double =
        value?.replace(",", "")?.replace("+", "")?.replace("%", "")?.trim()?.toDoubleOrNull() ?: 0.0

    /** Parse price change sign (1,2,+ -> "+", 4,5,- -> "-"). */
    fun parseSign(value: String?): String = when (value?.trim()) {
        "1", "2", "+" -> "+"
        "4", "5", "-" -> "-"
        else -> ""
    }
}
