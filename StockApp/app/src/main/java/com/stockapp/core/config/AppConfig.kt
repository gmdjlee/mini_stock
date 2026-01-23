package com.stockapp.core.config

/**
 * Centralized application configuration constants.
 * All magic numbers and configurable values should be defined here.
 */
object AppConfig {

    // ========== Cache Configuration ==========

    /** Maximum number of stocks to cache locally */
    const val MAX_STOCK_CACHE_SIZE = 10_000

    /** Maximum search history entries to retain */
    const val MAX_HISTORY_COUNT = 50

    /** Batch size for scheduling sync operations */
    const val SCHEDULING_BATCH_SIZE = 50

    // ========== Timeout Configuration (milliseconds) ==========

    /** Default timeout for Python API calls */
    const val DEFAULT_TIMEOUT_MS = 30_000L

    /** Extended timeout for analysis operations */
    const val ANALYSIS_TIMEOUT_MS = 60_000L

    /** Timeout for loading full stock list */
    const val STOCK_LIST_TIMEOUT_MS = 120_000L

    /** Timeout for scheduling sync operations */
    const val SYNC_TIMEOUT_MS = 120_000L

    // ========== UI Configuration (milliseconds) ==========

    /** Debounce delay for search input */
    const val SEARCH_DEBOUNCE_MS = 300L

    /** Delay between retry attempts */
    const val RETRY_DELAY_MS = 500L

    // ========== Cache TTL Configuration (milliseconds) ==========

    /** Stock cache time-to-live: 24 hours */
    const val STOCK_CACHE_TTL_MS = 24 * 60 * 60 * 1000L

    /** Analysis cache time-to-live: 24 hours */
    const val ANALYSIS_CACHE_TTL_MS = 24 * 60 * 60 * 1000L

    /** Indicator cache time-to-live: 24 hours */
    const val INDICATOR_CACHE_TTL_MS = 24 * 60 * 60 * 1000L

    // ========== API Rate Limiting ==========

    /** Minimum interval between API calls (milliseconds) */
    const val API_RATE_LIMIT_MS = 500L

    // ========== Indicator Configuration ==========

    /** Default number of days for indicator calculations */
    const val DEFAULT_INDICATOR_DAYS = 180

    // ========== Chart Configuration ==========

    /** Maximum days to display in indicator charts */
    const val CHART_MAX_DISPLAY_DAYS = 180

    // ========== DeMark TD Thresholds ==========

    /** Threshold for DeMark TD active signal (shows warning) */
    const val DEMARK_ACTIVE_THRESHOLD = 5

    /** Threshold for DeMark TD strong signal (reversal likely) */
    const val DEMARK_STRONG_THRESHOLD = 9

    // ========== Fear/Greed Thresholds ==========

    /** Fear/Greed index greed threshold (market overheated) */
    const val FEAR_GREED_GREED_THRESHOLD = 0.5

    /** Fear/Greed index fear threshold (market oversold) */
    const val FEAR_GREED_FEAR_THRESHOLD = -0.5
}
