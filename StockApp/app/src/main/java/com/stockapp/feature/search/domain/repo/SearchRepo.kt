package com.stockapp.feature.search.domain.repo

import com.stockapp.feature.search.domain.model.Stock
import kotlinx.coroutines.flow.Flow

/**
 * Search repository interface.
 */
interface SearchRepo {
    /**
     * Search stocks by query (name or ticker).
     * Uses local cache if available.
     */
    suspend fun search(query: String): Result<List<Stock>>

    /**
     * Get all stocks (for autocomplete cache).
     */
    suspend fun getAll(): Result<List<Stock>>

    /**
     * Get search history.
     */
    fun getHistory(): Flow<List<Stock>>

    /**
     * Save to search history.
     */
    suspend fun saveHistory(stock: Stock)

    /**
     * Clear search history.
     */
    suspend fun clearHistory()

    /**
     * Search for suggestions (State용 검색 함수).
     * Returns empty list on error.
     */
    suspend fun searchForSuggestions(query: String): List<Stock>

    /**
     * Check if stock cache is available.
     */
    suspend fun isCacheAvailable(): Boolean

    /**
     * Get stock cache count.
     */
    suspend fun getCacheCount(): Int
}
