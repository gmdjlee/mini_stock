package com.stockapp.feature.condition.domain.usecase

import com.stockapp.feature.condition.domain.model.ConditionResult
import com.stockapp.feature.condition.domain.repo.ConditionRepo
import javax.inject.Inject

/**
 * Use case for executing condition search.
 */
class SearchConditionUC @Inject constructor(
    private val repo: ConditionRepo
) {
    /**
     * Execute condition search.
     *
     * @param condIdx Condition index
     * @param condName Condition name (optional, if empty will auto-fetch)
     */
    suspend operator fun invoke(
        condIdx: String,
        condName: String = ""
    ): Result<ConditionResult> {
        if (condIdx.isBlank()) {
            return Result.failure(IllegalArgumentException("조건검색 인덱스가 필요합니다"))
        }

        return if (condName.isBlank()) {
            repo.searchByIdx(condIdx)
        } else {
            repo.search(condIdx, condName)
        }
    }
}
