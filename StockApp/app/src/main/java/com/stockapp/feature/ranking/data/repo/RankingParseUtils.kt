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

    /**
     * Clean ticker code by removing common suffixes.
     */
    fun cleanTicker(value: String?): String {
        if (value == null) return ""
        // Remove "_AL" suffix and any other common suffixes from ticker codes
        return value.replace("_AL", "").replace("_KS", "").replace("_KQ", "").trim()
    }

    /**
     * Parse Long value from API response string.
     */
    fun parseLong(value: String?): Long {
        if (value == null) return 0
        return value.replace(",", "").replace("+", "").trim().toLongOrNull() ?: 0
    }

    /**
     * Parse Double value from API response string.
     */
    fun parseDouble(value: String?): Double {
        if (value == null) return 0.0
        return value.replace(",", "").replace("+", "").replace("%", "").trim().toDoubleOrNull() ?: 0.0
    }

    /**
     * Parse price change sign from API response.
     * 1, 2, + -> "+"
     * 4, 5, - -> "-"
     */
    fun parseSign(value: String?): String {
        return when (value?.trim()) {
            "1", "2", "+" -> "+"
            "4", "5", "-" -> "-"
            else -> ""
        }
    }
}
