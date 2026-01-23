package com.stockapp.feature.analysis.data.repo

import android.util.Log
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.entity.AnalysisCacheEntity
import com.stockapp.core.py.PyApiException
import com.stockapp.core.py.PyClient
import com.stockapp.feature.analysis.domain.model.AnalysisResponse
import com.stockapp.feature.analysis.domain.model.StockData
import com.stockapp.feature.analysis.domain.repo.AnalysisRepo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AnalysisRepoImpl"

@Singleton
class AnalysisRepoImpl @Inject constructor(
    private val pyClient: PyClient,
    private val cacheDao: AnalysisCacheDao,
    private val json: Json
) : AnalysisRepo {

    override suspend fun getAnalysis(
        ticker: String,
        days: Int,
        useCache: Boolean
    ): Result<StockData> {
        // Check cache first if enabled
        if (useCache) {
            getCachedAnalysis(ticker)?.let { cached ->
                return Result.success(cached)
            }
        }

        // Fetch from Python API
        val result = pyClient.call(
            module = "stock_analyzer.stock.analysis",
            func = "analyze",
            args = listOf(ticker, days),
            timeoutMs = PyClient.ANALYSIS_TIMEOUT_MS
        ) { jsonStr ->
            parseAnalysisResponse(jsonStr)
        }

        // Cache the result after the call completes
        result.onSuccess { data ->
            cacheAnalysis(ticker, data)
        }

        return result
    }

    override suspend fun getCachedAnalysis(ticker: String): StockData? {
        val cached = cacheDao.get(ticker) ?: return null

        // Check if cache is expired
        val now = System.currentTimeMillis()
        if (now - cached.cachedAt > AppDb.ANALYSIS_CACHE_TTL) {
            val ageMinutes = (now - cached.cachedAt) / 1000 / 60
            Log.d(TAG, "Cache expired for ticker=$ticker, age=${ageMinutes}min, TTL=${AppDb.ANALYSIS_CACHE_TTL / 1000 / 60}min")
            cacheDao.delete(ticker)
            return null
        }

        return try {
            json.decodeFromString<CachedStockData>(cached.data).toDomain()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse cached analysis for ticker=$ticker, deleting invalid cache", e)
            cacheDao.delete(ticker)
            null
        }
    }

    override suspend fun clearCache(ticker: String) {
        cacheDao.delete(ticker)
    }

    override suspend fun clearAllCache() {
        cacheDao.deleteAll()
    }

    private suspend fun cacheAnalysis(ticker: String, data: StockData) {
        val cachedData = CachedStockData.fromDomain(data)
        val entity = AnalysisCacheEntity(
            ticker = ticker,
            data = json.encodeToString(cachedData),
            startDate = data.dates.firstOrNull() ?: "",
            endDate = data.dates.lastOrNull() ?: "",
            cachedAt = System.currentTimeMillis()
        )
        cacheDao.insert(entity)
    }

    private fun parseAnalysisResponse(jsonStr: String): StockData {
        val response = json.decodeFromString<AnalysisResponse>(jsonStr)
        if (response.ok && response.data != null) {
            return response.data.toDomain()
        } else {
            throw PyApiException(
                response.error?.code ?: "UNKNOWN",
                response.error?.msg ?: "수급 분석 실패"
            )
        }
    }
}

/**
 * Cache serialization wrapper.
 */
@kotlinx.serialization.Serializable
private data class CachedStockData(
    val ticker: String,
    val name: String,
    val dates: List<String>,
    val mcap: List<Long>,
    val for5d: List<Long>,
    val ins5d: List<Long>
) {
    fun toDomain(): StockData = StockData(
        ticker = ticker,
        name = name,
        dates = dates,
        mcap = mcap,
        for5d = for5d,
        ins5d = ins5d
    )

    companion object {
        fun fromDomain(data: StockData): CachedStockData = CachedStockData(
            ticker = data.ticker,
            name = data.name,
            dates = data.dates,
            mcap = data.mcap,
            for5d = data.for5d,
            ins5d = data.ins5d
        )
    }
}
