package com.stockapp.feature.condition.domain.repo

import com.stockapp.feature.condition.domain.model.Condition
import com.stockapp.feature.condition.domain.model.ConditionResult

/**
 * Repository interface for condition search.
 */
interface ConditionRepo {
    /**
     * Get list of available conditions.
     *
     * @param useCache Whether to use cached data if available
     * @return Result with list of conditions or error
     */
    suspend fun getConditionList(useCache: Boolean = true): Result<List<Condition>>

    /**
     * Execute condition search.
     *
     * @param condIdx Condition index
     * @param condName Condition name
     * @return Result with condition search result or error
     */
    suspend fun search(condIdx: String, condName: String): Result<ConditionResult>

    /**
     * Execute condition search by index (auto-fetch condition name).
     *
     * @param condIdx Condition index
     * @return Result with condition search result or error
     */
    suspend fun searchByIdx(condIdx: String): Result<ConditionResult>

    /**
     * Clear cached condition list.
     */
    suspend fun clearCache()
}
