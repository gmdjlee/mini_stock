package com.stockapp.feature.condition.data.repo

import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.ConditionCacheDao
import com.stockapp.core.db.entity.ConditionCacheEntity
import com.stockapp.core.py.PyClient
import com.stockapp.feature.condition.domain.model.Condition
import com.stockapp.feature.condition.domain.model.ConditionListResponse
import com.stockapp.feature.condition.domain.model.ConditionResult
import com.stockapp.feature.condition.domain.model.ConditionSearchResponse
import com.stockapp.feature.condition.domain.repo.ConditionRepo
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Implementation of ConditionRepo.
 */
class ConditionRepoImpl @Inject constructor(
    private val pyClient: PyClient,
    private val conditionCacheDao: ConditionCacheDao
) : ConditionRepo {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getConditionList(useCache: Boolean): Result<List<Condition>> {
        // Check cache first
        if (useCache) {
            val cached = conditionCacheDao.getCache(CACHE_KEY_LIST)
            if (cached != null && !isCacheExpired(cached.cachedAt)) {
                return try {
                    val conditions = parseCacheList(cached.data)
                    Result.success(conditions)
                } catch (e: Exception) {
                    fetchConditionList()
                }
            }
        }

        return fetchConditionList()
    }

    override suspend fun search(condIdx: String, condName: String): Result<ConditionResult> {
        return pyClient.call(
            module = "stock_analyzer.search.condition",
            func = "search",
            args = listOf(condIdx, condName),
            timeoutMs = PyClient.DEFAULT_TIMEOUT_MS
        ) { jsonStr ->
            val response = json.decodeFromString<ConditionSearchResponse>(jsonStr)
            if (response.ok && response.data != null) {
                response.data
            } else {
                throw Exception(response.error?.msg ?: "조건검색에 실패했습니다")
            }
        }.map { dto ->
            dto.toDomain()
        }
    }

    override suspend fun searchByIdx(condIdx: String): Result<ConditionResult> {
        return pyClient.call(
            module = "stock_analyzer.search.condition",
            func = "search_by_idx",
            args = listOf(condIdx),
            timeoutMs = PyClient.DEFAULT_TIMEOUT_MS
        ) { jsonStr ->
            val response = json.decodeFromString<ConditionSearchResponse>(jsonStr)
            if (response.ok && response.data != null) {
                response.data
            } else {
                throw Exception(response.error?.msg ?: "조건검색에 실패했습니다")
            }
        }.map { dto ->
            dto.toDomain()
        }
    }

    override suspend fun clearCache() {
        conditionCacheDao.deleteAll()
    }

    private suspend fun fetchConditionList(): Result<List<Condition>> {
        return pyClient.call(
            module = "stock_analyzer.search.condition",
            func = "get_list",
            args = emptyList(),
            timeoutMs = PyClient.DEFAULT_TIMEOUT_MS
        ) { jsonStr ->
            val response = json.decodeFromString<ConditionListResponse>(jsonStr)
            if (response.ok && response.data != null) {
                response.data
            } else {
                throw Exception(response.error?.msg ?: "조건검색 목록을 가져오는데 실패했습니다")
            }
        }.map { dtoList ->
            val conditions = dtoList.map { it.toDomain() }
            // Save to cache
            saveConditionListToCache(conditions)
            conditions
        }
    }

    private suspend fun saveConditionListToCache(conditions: List<Condition>) {
        val cacheData = conditions.joinToString(";") { "${it.idx}:${it.name}" }
        conditionCacheDao.insertOrUpdate(
            ConditionCacheEntity(
                cacheKey = CACHE_KEY_LIST,
                data = cacheData,
                cachedAt = System.currentTimeMillis()
            )
        )
    }

    private fun parseCacheList(data: String): List<Condition> {
        if (data.isBlank()) return emptyList()
        return data.split(";").map { item ->
            val parts = item.split(":")
            Condition(
                idx = parts[0],
                name = if (parts.size > 1) parts[1] else ""
            )
        }
    }

    private fun isCacheExpired(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt > AppDb.STOCK_CACHE_TTL
    }

    companion object {
        private const val CACHE_KEY_LIST = "condition_list"
    }
}
