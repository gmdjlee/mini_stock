package com.stockapp.feature.search.domain.usecase

import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.repo.SearchRepo
import javax.inject.Inject

/**
 * Search stock use case.
 */
class SearchStockUC @Inject constructor(
    private val repo: SearchRepo
) {
    suspend operator fun invoke(query: String): Result<List<Stock>> {
        if (query.isBlank()) {
            return Result.failure(IllegalArgumentException("검색어를 입력하세요"))
        }
        return repo.search(query.trim())
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
