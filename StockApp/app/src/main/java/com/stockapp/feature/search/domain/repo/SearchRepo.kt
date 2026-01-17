package com.stockapp.feature.search.domain.repo

import com.stockapp.feature.search.domain.model.Stock
import kotlinx.coroutines.flow.Flow

/**
 * Search repository interface.
 */
interface SearchRepo {
    /**
     * Search stocks by query (name or ticker).
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
}
