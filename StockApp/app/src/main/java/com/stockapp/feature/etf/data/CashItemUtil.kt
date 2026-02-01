package com.stockapp.feature.etf.data

/**
 * Utility for detecting and handling cash/deposit items in ETF constituents.
 *
 * Cash items in KIS API ETF constituent responses may have null or empty stock codes.
 * This utility provides consistent detection logic and synthetic code generation.
 */
object CashItemUtil {

    // Keywords that identify cash/deposit items by name
    private val CASH_NAME_KEYWORDS = listOf(
        "원화예금",
        "현금",
        "cash",
        "예금",
        "krw",
        "원화",
        "예치금",
        "mmf",
        "머니마켓",
        "money market"
    )

    // Stock codes known to represent cash/deposit items
    // 010010: Common code for KRW cash/deposit in ETF PDF
    private val CASH_STOCK_CODES = setOf(
        "010010"
    )

    /**
     * Checks if the stock name indicates a cash/deposit item.
     *
     * @param stockName The name of the constituent item
     * @return true if the item is identified as cash/deposit
     */
    fun isCashItem(stockName: String?): Boolean {
        if (stockName.isNullOrBlank()) return false
        val lowerName = stockName.lowercase()
        return CASH_NAME_KEYWORDS.any { lowerName.contains(it) }
    }

    /**
     * Checks if the stock code represents a known cash/deposit item.
     *
     * @param stockCode The stock code to check
     * @return true if the code is a known cash code
     */
    fun isCashStockCode(stockCode: String?): Boolean {
        if (stockCode.isNullOrBlank()) return false
        return stockCode in CASH_STOCK_CODES
    }

    /**
     * Checks if either stock code or name indicates a cash/deposit item.
     *
     * @param stockCode The stock code
     * @param stockName The stock name
     * @return true if the item is identified as cash/deposit
     */
    fun isCashItemByCodeOrName(stockCode: String?, stockName: String?): Boolean {
        return isCashStockCode(stockCode) || isCashItem(stockName)
    }

    /**
     * Generates a synthetic stock code for cash items.
     *
     * Format: CASH_{etfCode}_{hash}
     * This ensures uniqueness per ETF and per cash name type.
     *
     * @param etfCode The ETF code (e.g., "069500")
     * @param stockName The cash item name (e.g., "원화예금")
     * @return A synthetic stock code (e.g., "CASH_069500_1A2B")
     */
    fun generateCashCode(etfCode: String, stockName: String): String {
        val nameHash = stockName.hashCode().and(0xFFFF).toString(16).uppercase().padStart(4, '0')
        return "CASH_${etfCode}_$nameHash"
    }

    /**
     * Checks if a stock code is a synthetic cash code.
     *
     * @param stockCode The stock code to check
     * @return true if the code is a synthetic cash code
     */
    fun isSyntheticCashCode(stockCode: String): Boolean {
        return stockCode.startsWith("CASH_")
    }
}
