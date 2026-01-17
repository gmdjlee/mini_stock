package com.stockapp.feature.condition.domain.usecase

import com.stockapp.feature.condition.domain.model.Condition
import com.stockapp.feature.condition.domain.repo.ConditionRepo
import javax.inject.Inject

/**
 * Use case for getting condition list.
 */
class GetConditionListUC @Inject constructor(
    private val repo: ConditionRepo
) {
    /**
     * Get list of available conditions.
     *
     * @param useCache Whether to use cached data (default: true)
     */
    suspend operator fun invoke(useCache: Boolean = true): Result<List<Condition>> {
        return repo.getConditionList(useCache)
    }
}
