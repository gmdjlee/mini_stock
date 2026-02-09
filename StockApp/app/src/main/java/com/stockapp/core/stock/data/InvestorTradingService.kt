package com.stockapp.core.stock.data

import android.util.Log
import com.stockapp.core.config.AppConfig
import com.stockapp.core.db.dao.InvestorTradingCacheDao
import com.stockapp.core.db.entity.InvestorTradingCacheEntity
import com.stockapp.core.krx.KrxDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InvestorTradingService"

/**
 * Investor trading data model returned by this service.
 */
data class InvestorTradingData(
    val date: String,
    val foreignNet: Long,
    val institutionNet: Long,
    val individualNet: Long,
    val totalTrading: Long
)

/**
 * Shared service for investor trading data with DB cache.
 * Used by Analysis (per-stock) and Market (market-wide) features.
 */
@Singleton
class InvestorTradingService @Inject constructor(
    private val krxDataSource: KrxDataSource,
    private val investorTradingCacheDao: InvestorTradingCacheDao
) {
    companion object {
        const val MARKET_KOSPI = "MARKET_KOSPI"
        const val MARKET_KOSDAQ = "MARKET_KOSDAQ"
        const val MARKET_ALL = "MARKET_ALL"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    private val tickerMutexes = ConcurrentHashMap<String, Mutex>()
    private fun getMutex(key: String): Mutex = tickerMutexes.getOrPut(key) { Mutex() }

    /**
     * Get investor trading data for a specific stock.
     *
     * @param ticker Stock ticker code
     * @param days Number of days to fetch
     * @return List of investor trading data sorted by date descending
     */
    suspend fun getInvestorTrading(
        ticker: String,
        days: Int
    ): Result<List<InvestorTradingData>> {
        return getWithCache(ticker, days)
    }

    /**
     * Get market-wide investor trading data.
     *
     * @param days Number of days to fetch
     * @param market Market identifier (MARKET_KOSPI, MARKET_KOSDAQ, MARKET_ALL)
     * @return List of investor trading data sorted by date descending
     */
    suspend fun getMarketInvestorTrading(
        days: Int,
        market: String = MARKET_ALL
    ): Result<List<InvestorTradingData>> {
        return getWithCache(market, days)
    }

    private suspend fun getWithCache(
        cacheKey: String,
        days: Int
    ): Result<List<InvestorTradingData>> {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() + 10)
        val startDateStr = startDate.format(DATE_FORMAT)
        val endDateStr = today.format(DATE_FORMAT)

        // Step 1: Check DB cache
        val cachedCount = investorTradingCacheDao.countInRange(cacheKey, startDateStr, endDateStr)
        val expectedDays = (days * AppConfig.OHLCV_CACHE_SUFFICIENCY_RATIO).toInt()

        if (cachedCount >= expectedDays) {
            val cached = investorTradingCacheDao.getByTickerAndDateRange(cacheKey, startDateStr, endDateStr)
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Cache hit for $cacheKey ($cachedCount records)")
                return Result.success(cached.map { it.toData() })
            }
        }

        // Step 2: Fetch from API with mutex
        return getMutex(cacheKey).withLock {
            // Re-check after acquiring mutex
            val recheckCount = investorTradingCacheDao.countInRange(cacheKey, startDateStr, endDateStr)
            if (recheckCount >= expectedDays) {
                val cached = investorTradingCacheDao.getByTickerAndDateRange(cacheKey, startDateStr, endDateStr)
                if (cached.isNotEmpty()) {
                    return@withLock Result.success(cached.map { it.toData() })
                }
            }

            // Determine incremental fetch range
            val latestCached = investorTradingCacheDao.getLatestDate(cacheKey)
            val fetchStart = if (latestCached != null && latestCached >= startDateStr) {
                val nextDay = LocalDate.parse(latestCached, DATE_FORMAT).plusDays(1)
                val nextDayStr = nextDay.format(DATE_FORMAT)
                if (nextDayStr > endDateStr) {
                    // Already fully cached
                    val cached = investorTradingCacheDao.getByTickerAndDateRange(cacheKey, startDateStr, endDateStr)
                    return@withLock Result.success(cached.map { it.toData() })
                }
                nextDayStr
            } else {
                startDateStr
            }

            // Fetch from KRX
            val fetchResult = fetchFromApi(cacheKey, fetchStart, endDateStr)

            fetchResult.onSuccess { dataList ->
                if (dataList.isNotEmpty()) {
                    val entities = dataList.map { it.toEntity(cacheKey) }
                    investorTradingCacheDao.insertAll(entities)
                }
            }

            // Return full range from DB
            val allData = investorTradingCacheDao.getByTickerAndDateRange(cacheKey, startDateStr, endDateStr)
            if (allData.isNotEmpty()) {
                Result.success(allData.map { it.toData() })
            } else {
                fetchResult
            }
        }
    }

    private suspend fun fetchFromApi(
        cacheKey: String,
        startDate: String,
        endDate: String
    ): Result<List<InvestorTradingData>> {
        return try {
            val result = when {
                cacheKey.startsWith("MARKET_") -> {
                    val market = when (cacheKey) {
                        MARKET_KOSPI -> com.krxkt.model.Market.KOSPI
                        MARKET_KOSDAQ -> com.krxkt.model.Market.KOSDAQ
                        else -> com.krxkt.model.Market.ALL
                    }
                    krxDataSource.getMarketTradingByInvestor(
                        startDate = startDate,
                        endDate = endDate,
                        market = market
                    )
                }
                else -> {
                    krxDataSource.getTradingByInvestor(
                        startDate = startDate,
                        endDate = endDate,
                        ticker = cacheKey
                    )
                }
            }

            result.map { tradingList ->
                tradingList.map { trading ->
                    InvestorTradingData(
                        date = trading.date,
                        foreignNet = trading.foreigner,
                        institutionNet = trading.institutionalTotal,
                        individualNet = trading.individual,
                        totalTrading = trading.total
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "fetchFromApi() failed for $cacheKey: ${e.message}")
            Result.failure(e)
        }
    }

    private fun InvestorTradingCacheEntity.toData() = InvestorTradingData(
        date = date,
        foreignNet = foreignNet,
        institutionNet = institutionNet,
        individualNet = individualNet,
        totalTrading = totalTrading
    )

    private fun InvestorTradingData.toEntity(cacheKey: String) = InvestorTradingCacheEntity(
        ticker = cacheKey,
        date = date,
        foreignNet = foreignNet,
        institutionNet = institutionNet,
        individualNet = individualNet,
        totalTrading = totalTrading
    )
}
