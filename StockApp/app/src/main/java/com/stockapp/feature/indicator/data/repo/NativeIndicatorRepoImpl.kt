package com.stockapp.feature.indicator.data.repo

import android.util.Log
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.db.entity.IndicatorCacheEntity
import com.stockapp.core.stock.calc.DemarkCalculator
import com.stockapp.core.stock.calc.ElderCalculator
import com.stockapp.core.stock.calc.TrendCalculator
import com.stockapp.core.stock.data.OhlcvService
import com.stockapp.feature.indicator.domain.model.DemarkDataDto
import com.stockapp.feature.indicator.domain.model.DemarkSetup
import com.stockapp.feature.indicator.domain.model.ElderDataDto
import com.stockapp.feature.indicator.domain.model.ElderImpulse
import com.stockapp.feature.indicator.domain.model.IndicatorType
import com.stockapp.feature.indicator.domain.model.TrendDataDto
import com.stockapp.feature.indicator.domain.model.TrendSignal
import com.stockapp.feature.indicator.domain.repo.IndicatorRepo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NativeIndicatorRepo"

/**
 * Native Kotlin implementation of IndicatorRepo.
 * Uses OhlcvService and native calculators instead of Python/Chaquopy.
 */
@Singleton
class NativeIndicatorRepoImpl @Inject constructor(
    private val ohlcvService: OhlcvService,
    private val indicatorCacheDao: IndicatorCacheDao
) : IndicatorRepo {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Extra periods for indicator warmup
    private companion object {
        const val TREND_EXTRA_PERIODS = 60
        const val ELDER_EXTRA_PERIODS = 50
        const val DEMARK_EXTRA_PERIODS = 10
    }

    override suspend fun getTrend(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<TrendSignal> {
        Log.d(TAG, "getTrend() ticker=$ticker, days=$days, timeframe=$timeframe, useCache=$useCache")

        val cacheKey = IndicatorCacheDao.buildKey(ticker, IndicatorType.TREND.key, days)

        // Check cache
        if (useCache) {
            getCachedTrend(cacheKey)?.let {
                Log.d(TAG, "getTrend() returning cached data for $ticker")
                return Result.success(it)
            }
        }

        return try {
            // Fetch OHLCV data
            val fetchDays = if (timeframe == "weekly") {
                (days + TREND_EXTRA_PERIODS) * 7  // ~7 days per week
            } else {
                days + TREND_EXTRA_PERIODS
            }

            val period = if (timeframe == "weekly") {
                OhlcvService.Period.DAILY  // Fetch daily and resample
            } else {
                OhlcvService.Period.DAILY
            }

            val ohlcvResult = ohlcvService.getOhlcv(ticker, fetchDays, period)
            if (ohlcvResult.isFailure) {
                return Result.failure(ohlcvResult.exceptionOrNull()!!)
            }

            var ohlcvData = ohlcvResult.getOrThrow()

            // Resample to weekly if needed
            if (timeframe == "weekly") {
                ohlcvData = ohlcvService.resampleToWeekly(ohlcvData)
            }

            // Validate data size
            val minPeriods = if (timeframe == "weekly") 52 else 60
            if (ohlcvData.closes.size < minPeriods) {
                return Result.failure(
                    IllegalStateException("데이터가 충분하지 않습니다 (최소 $minPeriods 필요, 현재 ${ohlcvData.closes.size})")
                )
            }

            // Calculate Trend Signal
            val trendResult = TrendCalculator.calculate(
                ticker = ticker,
                dates = ohlcvData.dates,
                closes = ohlcvData.closes,
                highs = ohlcvData.highs,
                lows = ohlcvData.lows,
                volumes = ohlcvData.volumes,
                timeframe = timeframe
            ) ?: return Result.failure(
                IllegalStateException("Trend calculation failed")
            )

            // Trim to requested days
            val trimLen = minOf(days, trendResult.dates.size - (minPeriods - 1))

            val trendSignal = TrendSignal(
                ticker = trendResult.ticker,
                timeframe = trendResult.timeframe,
                dates = trendResult.dates.take(trimLen),
                maSignal = trendResult.maSignal.take(trimLen),
                cmf = trendResult.cmf.take(trimLen),
                fearGreed = trendResult.fearGreed.take(trimLen),
                trend = trendResult.trend.take(trimLen),
                ma5 = trendResult.ma5.take(trimLen),
                ma10 = trendResult.ma10.take(trimLen),
                ma20 = trendResult.ma20.take(trimLen)
            )

            // Cache the result
            cacheTrend(cacheKey, ticker, trendSignal)

            Log.d(TAG, "getTrend() completed for $ticker, periods=${trendSignal.dates.size}")
            Result.success(trendSignal)
        } catch (e: Exception) {
            Log.e(TAG, "getTrend() failed for $ticker: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getElder(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<ElderImpulse> {
        Log.d(TAG, "getElder() ticker=$ticker, days=$days, timeframe=$timeframe, useCache=$useCache")

        val cacheKey = IndicatorCacheDao.buildKey(ticker, IndicatorType.ELDER.key, days)

        // Check cache
        if (useCache) {
            getCachedElder(cacheKey)?.let { cached ->
                // Only use cache if it has close prices
                if (cached.close.isNotEmpty()) {
                    Log.d(TAG, "getElder() returning cached data for $ticker")
                    return Result.success(cached)
                }
            }
        }

        return try {
            // Fetch OHLCV data
            val fetchDays = if (timeframe == "weekly") {
                (days + ELDER_EXTRA_PERIODS) * 7
            } else {
                days + ELDER_EXTRA_PERIODS
            }

            val ohlcvResult = ohlcvService.getOhlcv(ticker, fetchDays, OhlcvService.Period.DAILY)
            if (ohlcvResult.isFailure) {
                return Result.failure(ohlcvResult.exceptionOrNull()!!)
            }

            var ohlcvData = ohlcvResult.getOrThrow()

            // Resample to weekly if needed
            if (timeframe == "weekly") {
                ohlcvData = ohlcvService.resampleToWeekly(ohlcvData)
            }

            // Validate data size
            val minPeriods = 35
            if (ohlcvData.closes.size < minPeriods) {
                return Result.failure(
                    IllegalStateException("데이터가 충분하지 않습니다 (최소 $minPeriods 필요, 현재 ${ohlcvData.closes.size})")
                )
            }

            // Calculate Elder Impulse
            val elderResult = ElderCalculator.calculate(
                ticker = ticker,
                dates = ohlcvData.dates,
                closes = ohlcvData.closes,
                timeframe = timeframe
            ) ?: return Result.failure(
                IllegalStateException("Elder calculation failed")
            )

            // Trim to requested days
            val trimLen = minOf(days, elderResult.dates.size - 34)

            val elderImpulse = ElderImpulse(
                ticker = elderResult.ticker,
                timeframe = elderResult.timeframe,
                dates = elderResult.dates.take(trimLen),
                color = elderResult.color.take(trimLen),
                ema13 = elderResult.ema13.take(trimLen),
                macdLine = elderResult.macdLine.take(trimLen),
                signalLine = elderResult.signalLine.take(trimLen),
                macdHist = elderResult.macdHist.take(trimLen),
                close = elderResult.close.take(trimLen)
            )

            // Cache the result
            cacheElder(cacheKey, ticker, elderImpulse)

            Log.d(TAG, "getElder() completed for $ticker, periods=${elderImpulse.dates.size}")
            Result.success(elderImpulse)
        } catch (e: Exception) {
            Log.e(TAG, "getElder() failed for $ticker: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getDemark(
        ticker: String,
        days: Int,
        timeframe: String,
        useCache: Boolean
    ): Result<DemarkSetup> {
        Log.d(TAG, "getDemark() ticker=$ticker, days=$days, timeframe=$timeframe, useCache=$useCache")

        val cacheKey = IndicatorCacheDao.buildKey(ticker, IndicatorType.DEMARK.key, days)

        // Check cache
        if (useCache) {
            getCachedDemark(cacheKey)?.let {
                Log.d(TAG, "getDemark() returning cached data for $ticker")
                return Result.success(it)
            }
        }

        return try {
            // Fetch OHLCV data
            val fetchDays = when (timeframe) {
                "weekly" -> (days + DEMARK_EXTRA_PERIODS) * 7
                "monthly" -> (days + DEMARK_EXTRA_PERIODS) * 22
                else -> days + DEMARK_EXTRA_PERIODS
            }

            val ohlcvResult = ohlcvService.getOhlcv(ticker, fetchDays, OhlcvService.Period.DAILY)
            if (ohlcvResult.isFailure) {
                return Result.failure(ohlcvResult.exceptionOrNull()!!)
            }

            var ohlcvData = ohlcvResult.getOrThrow()

            // Resample if needed
            when (timeframe) {
                "weekly" -> ohlcvData = ohlcvService.resampleToWeekly(ohlcvData)
                "monthly" -> ohlcvData = ohlcvService.resampleToMonthly(ohlcvData)
            }

            // Validate data size
            if (ohlcvData.closes.size < 5) {
                return Result.failure(
                    IllegalStateException("데이터가 충분하지 않습니다 (최소 5 필요, 현재 ${ohlcvData.closes.size})")
                )
            }

            // Calculate DeMark TD Setup
            val demarkResult = DemarkCalculator.calculate(
                ticker = ticker,
                dates = ohlcvData.dates,
                closes = ohlcvData.closes,
                timeframe = timeframe
            ) ?: return Result.failure(
                IllegalStateException("DeMark calculation failed")
            )

            // Trim to requested days
            val trimLen = minOf(days, demarkResult.dates.size - 4)

            val demarkSetup = DemarkSetup(
                ticker = demarkResult.ticker,
                timeframe = demarkResult.timeframe,
                dates = demarkResult.dates.take(trimLen),
                close = demarkResult.close.take(trimLen),
                sellSetup = demarkResult.sellSetup.take(trimLen),
                buySetup = demarkResult.buySetup.take(trimLen)
            )

            // Cache the result
            cacheDemark(cacheKey, ticker, demarkSetup)

            Log.d(TAG, "getDemark() completed for $ticker, periods=${demarkSetup.dates.size}")
            Result.success(demarkSetup)
        } catch (e: Exception) {
            Log.e(TAG, "getDemark() failed for $ticker: ${e.message}", e)
            Result.failure(e)
        }
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
            Log.w(TAG, "Failed to parse cached trend: ${e.message}")
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
            Log.w(TAG, "Failed to parse cached elder: ${e.message}")
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
            Log.w(TAG, "Failed to parse cached demark: ${e.message}")
            indicatorCacheDao.delete(key)
            null
        }
    }

    private suspend fun cacheTrend(key: String, ticker: String, signal: TrendSignal) {
        val dto = TrendDataDto(
            ticker = signal.ticker,
            timeframe = signal.timeframe,
            dates = signal.dates,
            maSignal = signal.maSignal,
            cmf = signal.cmf,
            fearGreed = signal.fearGreed,
            trend = signal.trend,
            ma5 = signal.ma5,
            ma10 = signal.ma10,
            ma20 = signal.ma20
        )
        val entity = IndicatorCacheEntity(
            key = key,
            ticker = ticker,
            type = IndicatorType.TREND.key,
            data = json.encodeToString(dto)
        )
        indicatorCacheDao.insert(entity)
    }

    private suspend fun cacheElder(key: String, ticker: String, elder: ElderImpulse) {
        val dto = ElderDataDto(
            ticker = elder.ticker,
            timeframe = elder.timeframe,
            dates = elder.dates,
            color = elder.color,
            ema13 = elder.ema13,
            macdLine = elder.macdLine,
            signalLine = elder.signalLine,
            macdHist = elder.macdHist,
            close = elder.close
        )
        val entity = IndicatorCacheEntity(
            key = key,
            ticker = ticker,
            type = IndicatorType.ELDER.key,
            data = json.encodeToString(dto)
        )
        indicatorCacheDao.insert(entity)
    }

    private suspend fun cacheDemark(key: String, ticker: String, demark: DemarkSetup) {
        val dto = DemarkDataDto(
            ticker = demark.ticker,
            timeframe = demark.timeframe,
            dates = demark.dates,
            close = demark.close,
            sellSetup = demark.sellSetup,
            buySetup = demark.buySetup
        )
        val entity = IndicatorCacheEntity(
            key = key,
            ticker = ticker,
            type = IndicatorType.DEMARK.key,
            data = json.encodeToString(dto)
        )
        indicatorCacheDao.insert(entity)
    }

    private fun isCacheExpired(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt > AppDb.INDICATOR_CACHE_TTL
    }
}
