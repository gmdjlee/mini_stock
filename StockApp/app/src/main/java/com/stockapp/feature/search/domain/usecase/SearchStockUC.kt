package com.stockapp.feature.search.domain.usecase

import com.stockapp.core.config.AppConfig
import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.repo.SearchRepo
import javax.inject.Inject

/**
 * Search stock use case.
 * Optimizes API calls by using cache-only search for short queries.
 */
class SearchStockUC @Inject constructor(
    private val repo: SearchRepo
) {
    suspend operator fun invoke(query: String): Result<List<Stock>> {
        val trimmed = query.trim()

        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("검색어를 입력하세요"))
        }

        // For short queries, only search cache (no API call)
        // This minimizes API calls during typing
        if (trimmed.length < AppConfig.MIN_SEARCH_QUERY_LENGTH) {
            return repo.searchCacheOnly(trimmed)
        }

        return repo.search(trimmed)
    }
}

/**
 * Save to history use case.
 */
class SaveHistoryUC @Inject constructor(
    private val repo: SearchRepo
) {
    suspend operator fun invoke(stock: Stock) {
        repo.saveHistory(stock)
    }
}
