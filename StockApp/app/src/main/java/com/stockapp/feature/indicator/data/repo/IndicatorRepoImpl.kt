package com.stockapp.feature.indicator.data.repo

import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.db.entity.IndicatorCacheEntity
import com.stockapp.core.py.PyClient
import com.stockapp.core.py.PyError
import com.stockapp.feature.indicator.domain.model.DemarkDataDto
import com.stockapp.feature.indicator.domain.model.DemarkResponse
import com.stockapp.feature.indicator.domain.model.DemarkSetup
import com.stockapp.feature.indicator.domain.model.ElderDataDto
import com.stockapp.feature.indicator.domain.model.ElderImpulse
import com.stockapp.feature.indicator.domain.model.ElderResponse
import com.stockapp.feature.indicator.domain.model.IndicatorType
import com.stockapp.feature.indicator.domain.model.TrendDataDto
import com.stockapp.feature.indicator.domain.model.TrendResponse
import com.stockapp.feature.indicator.domain.model.TrendSignal
import com.stockapp.feature.indicator.domain.repo.IndicatorRepo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndicatorRepoImpl @Inject constructor(
    private val pyClient: PyClient,
    private val indicatorCacheDao: IndicatorCacheDao
) : IndicatorRepo {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getTrend(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<TrendSignal> {
        val cacheKey = IndicatorCacheDao.buildKey(ticker, IndicatorType.TREND.key, days)

        // Check cache
        if (useCache) {
            getCachedTrend(cacheKey)?.let { return Result.success(it) }
        }

        // Fetch from Python
        val result = pyClient.call(
            module = "stock_analyzer.indicator.trend",
            func = "calc",
            args = listOf(ticker, days, timeframe),
            timeoutMs = PyClient.DEFAULT_TIMEOUT_MS
        ) { jsonStr ->
            val response = json.decodeFromString<TrendResponse>(jsonStr)
            if (response.ok && response.data != null) {
                Pair(response.data.toDomain(), response.data)
            } else {
                throw PyError.CallError(response.error?.msg ?: "Failed to get trend signal")
            }
        }

        // Cache the result after the call completes
        result.onSuccess { (_, dto) ->
            cacheTrend(cacheKey, ticker, dto)
        }

        return result.map { it.first }
    }

    override suspend fun getElder(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<ElderImpulse> {
        val cacheKey = IndicatorCacheDao.buildKey(ticker, IndicatorType.ELDER.key, days)

        // Check cache
        if (useCache) {
            getCachedElder(cacheKey)?.let { return Result.success(it) }
        }

        // Fetch from Python
        val result = pyClient.call(
            module = "stock_analyzer.indicator.elder",
            func = "calc",
            args = listOf(ticker, days, timeframe),
            timeoutMs = PyClient.DEFAULT_TIMEOUT_MS
        ) { jsonStr ->
            val response = json.decodeFromString<ElderResponse>(jsonStr)
            if (response.ok && response.data != null) {
                Pair(response.data.toDomain(), response.data)
            } else {
                throw PyError.CallError(response.error?.msg ?: "Failed to get elder impulse")
            }
        }

        // Cache the result after the call completes
        result.onSuccess { (_, dto) ->
            cacheElder(cacheKey, ticker, dto)
        }

        return result.map { it.first }
    }

    override suspend fun getDemark(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<DemarkSetup> {
        val cacheKey = IndicatorCacheDao.buildKey(ticker, IndicatorType.DEMARK.key, days)

        // Check cache
        if (useCache) {
            getCachedDemark(cacheKey)?.let { return Result.success(it) }
        }

        // Fetch from Python
        val result = pyClient.call(
            module = "stock_analyzer.indicator.demark",
            func = "calc",
            args = listOf(ticker, days, timeframe),
            timeoutMs = PyClient.DEFAULT_TIMEOUT_MS
        ) { jsonStr ->
            val response = json.decodeFromString<DemarkResponse>(jsonStr)
            if (response.ok && response.data != null) {
                Pair(response.data.toDomain(), response.data)
            } else {
                throw PyError.CallError(response.error?.msg ?: "Failed to get demark setup")
            }
        }

        // Cache the result after the call completes
        result.onSuccess { (_, dto) ->
            cacheDemark(cacheKey, ticker, dto)
        }

        return result.map { it.first }
    }

    override suspend fun clearCache(ticker: String) {
        indicatorCacheDao.deleteByTicker(ticker)
    }

    override suspend fun clearExpiredCache() {
        val threshold = System.currentTimeMillis() - AppDb.INDICATOR_CACHE_TTL
        indicatorCacheDao.deleteExpired(threshold)
    }

    // ========== Cache Helpers ==========

    private suspend fun getCachedTrend(key: String): TrendSignal? {
        val cached = indicatorCacheDao.get(key) ?: return null
        if (isCacheExpired(cached.cachedAt)) {
            indicatorCacheDao.delete(key)
            return null
        }
        return try {
            val dto = json.decodeFromString<TrendDataDto>(cached.data)
            dto.toDomain()
        } catch (e: Exception) {
            indicatorCacheDao.delete(key)
            null
        }
    }

    private suspend fun getCachedElder(key: String): ElderImpulse? {
        val cached = indicatorCacheDao.get(key) ?: return null
        if (isCacheExpired(cached.cachedAt)) {
            indicatorCacheDao.delete(key)
            return null
        }
        return try {
            val dto = json.decodeFromString<ElderDataDto>(cached.data)
            dto.toDomain()
        } catch (e: Exception) {
            indicatorCacheDao.delete(key)
            null
        }
    }

    private suspend fun getCachedDemark(key: String): DemarkSetup? {
        val cached = indicatorCacheDao.get(key) ?: return null
        if (isCacheExpired(cached.cachedAt)) {
            indicatorCacheDao.delete(key)
            return null
        }
        return try {
            val dto = json.decodeFromString<DemarkDataDto>(cached.data)
            dto.toDomain()
        } catch (e: Exception) {
            indicatorCacheDao.delete(key)
            null
        }
    }

    private suspend fun cacheTrend(key: String, ticker: String, data: TrendDataDto) {
        val entity = IndicatorCacheEntity(
            key = key,
            ticker = ticker,
            type = IndicatorType.TREND.key,
            data = json.encodeToString(data)
        )
        indicatorCacheDao.insert(entity)
    }

    private suspend fun cacheElder(key: String, ticker: String, data: ElderDataDto) {
        val entity = IndicatorCacheEntity(
            key = key,
            ticker = ticker,
            type = IndicatorType.ELDER.key,
            data = json.encodeToString(data)
        )
        indicatorCacheDao.insert(entity)
    }

    private suspend fun cacheDemark(key: String, ticker: String, data: DemarkDataDto) {
        val entity = IndicatorCacheEntity(
            key = key,
            ticker = ticker,
            type = IndicatorType.DEMARK.key,
            data = json.encodeToString(data)
        )
        indicatorCacheDao.insert(entity)
    }

    private fun isCacheExpired(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt > AppDb.INDICATOR_CACHE_TTL
    }
}
