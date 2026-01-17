package com.stockapp.feature.market.data.repo

import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.MarketCacheDao
import com.stockapp.core.db.entity.MarketCacheEntity
import com.stockapp.core.py.PyClient
import com.stockapp.feature.market.domain.model.MarketIndicators
import com.stockapp.feature.market.domain.model.MarketIndicatorsResponse
import com.stockapp.feature.market.domain.repo.MarketRepo
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Implementation of MarketRepo.
 */
class MarketRepoImpl @Inject constructor(
    private val pyClient: PyClient,
    private val marketCacheDao: MarketCacheDao
) : MarketRepo {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getMarketIndicators(days: Int, useCache: Boolean): Result<MarketIndicators> {
        // Check cache first
        if (useCache) {
            val cached = marketCacheDao.getCache(CACHE_KEY)
            if (cached != null && !isCacheExpired(cached.cachedAt)) {
                return try {
                    val indicators = parseCacheData(cached.data)
                    Result.success(indicators)
                } catch (e: Exception) {
                    // Cache parse failed, fetch fresh
                    fetchFromPython(days)
                }
            }
        }

        return fetchFromPython(days)
    }

    override suspend fun clearCache() {
        marketCacheDao.deleteAll()
    }

    private suspend fun fetchFromPython(days: Int): Result<MarketIndicators> {
        return pyClient.call(
            module = "stock_analyzer.market.deposit",
            func = "get_market_indicators",
            args = listOf(days),
            timeoutMs = PyClient.DEFAULT_TIMEOUT_MS
        ) { jsonStr ->
            val response = json.decodeFromString<MarketIndicatorsResponse>(jsonStr)
            if (response.ok && response.data != null) {
                response.data
            } else {
                throw Exception(response.error?.msg ?: "시장 지표를 가져오는데 실패했습니다")
            }
        }.map { dto ->
            val indicators = dto.toDomain()
            // Save to cache
            saveToCache(indicators)
            indicators
        }
    }

    private suspend fun saveToCache(indicators: MarketIndicators) {
        val cacheData = buildString {
            append(indicators.dates.joinToString(","))
            append("|")
            append(indicators.deposit.joinToString(","))
            append("|")
            append(indicators.creditLoan.joinToString(","))
            append("|")
            append(indicators.creditBalance.joinToString(","))
            append("|")
            append(indicators.creditRatio.joinToString(","))
        }

        marketCacheDao.insertOrUpdate(
            MarketCacheEntity(
                cacheKey = CACHE_KEY,
                data = cacheData,
                days = indicators.dates.size,
                cachedAt = System.currentTimeMillis()
            )
        )
    }

    private fun parseCacheData(data: String): MarketIndicators {
        val parts = data.split("|")
        return MarketIndicators(
            dates = parts[0].split(","),
            deposit = parts[1].split(",").map { it.toLong() },
            creditLoan = parts[2].split(",").map { it.toLong() },
            creditBalance = parts[3].split(",").map { it.toLong() },
            creditRatio = parts[4].split(",").map { it.toDouble() }
        )
    }

    private fun isCacheExpired(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt > AppDb.STOCK_CACHE_TTL
    }

    companion object {
        private const val CACHE_KEY = "market_indicators"
    }
}
