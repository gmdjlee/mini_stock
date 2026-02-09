package com.stockapp.feature.market.data.repo

import android.util.Log
import com.krxkt.model.Market
import com.stockapp.core.config.AppConfig
import com.stockapp.core.db.dao.MarketIndicatorCacheDao
import com.stockapp.core.db.entity.MarketIndicatorCacheEntity
import com.stockapp.core.di.IoDispatcher
import com.stockapp.core.krx.KrxDataSource
import com.stockapp.core.stock.data.InvestorTradingService
import com.stockapp.feature.market.data.calc.MarketCalculator
import com.stockapp.feature.market.domain.model.BloodIndicatorHistory
import com.stockapp.feature.market.domain.model.BloodSignal
import com.stockapp.feature.market.domain.model.FearGreedHistory
import com.stockapp.feature.market.domain.model.FearGreedSignal
import com.stockapp.feature.market.domain.model.FundFlowHistory
import com.stockapp.feature.market.domain.model.MarketDateRange
import com.stockapp.feature.market.domain.model.MarketFearGreed
import com.stockapp.feature.market.domain.model.OscillatorHistory
import com.stockapp.feature.market.domain.repo.MarketRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * KRX-based implementation of MarketRepo.
 * All data sourced from Korean Exchange via kotlin_krx library.
 */
@Singleton
class MarketRepoImpl @Inject constructor(
    private val krxDataSource: KrxDataSource,
    private val investorTradingService: InvestorTradingService,
    private val marketIndicatorCacheDao: MarketIndicatorCacheDao,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MarketRepo {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    override suspend fun getFearGreedIndex(): Result<MarketFearGreed> =
        withContext(ioDispatcher) {
            try {
                // Check cache first
                val cacheKey = CACHE_KEY_FEAR_GREED_LATEST
                getCachedData(cacheKey)?.let { data ->
                    return@withContext Result.success(json.decodeFromString<MarketFearGreed>(data))
                }

                val endDate = LocalDate.now()
                val startDate = endDate.minusDays(90)
                val endDateStr = endDate.format(dateFormatter)
                val startDateStr = startDate.format(dateFormatter)

                // 1. Get KOSPI index data (60+ days needed)
                val indexResult = krxDataSource.getIndexOhlcv(
                    startDate = startDateStr,
                    endDate = endDateStr,
                    ticker = KOSPI_TICKER
                )

                val indexData = indexResult.getOrNull()
                if (indexData == null || indexData.size < 30) {
                    return@withContext Result.failure(
                        IllegalStateException("Insufficient KOSPI data for Fear & Greed calculation")
                    )
                }

                val closes = indexData.map { it.close.toDouble() }
                val volumes = indexData.map { it.volume }

                // 2. Get investor trading data via shared service (DB cache + KRX)
                var foreignNetBuy = 0L
                var institutionNetBuy = 0L
                var totalTradingValue = 1L

                val investorResult = investorTradingService.getMarketInvestorTrading(
                    days = 5,
                    market = InvestorTradingService.MARKET_ALL
                )
                investorResult.onSuccess { tradingList ->
                    for (trading in tradingList) {
                        foreignNetBuy += trading.foreignNet
                        institutionNetBuy += trading.institutionNet
                        totalTradingValue += kotlin.math.abs(trading.totalTrading)
                    }
                }

                // 3. Get short selling data (recent date)
                var shortSellingRatio = 0.05 // default 5%
                val shortResult = krxDataSource.getShortSellingAll(
                    date = endDateStr,
                    market = Market.KOSPI
                )

                shortResult.onSuccess { shortList ->
                    if (shortList.isNotEmpty()) {
                        val totalVol = shortList.sumOf { it.totalVolume }
                        val shortVol = shortList.sumOf { it.shortVolume }
                        if (totalVol > 0) {
                            shortSellingRatio = shortVol.toDouble() / totalVol
                        }
                    }
                }

                val fearGreed = MarketCalculator.calculateFearGreed(
                    date = endDateStr,
                    indexCloses = closes,
                    indexVolumes = volumes,
                    foreignNetBuy = foreignNetBuy,
                    institutionNetBuy = institutionNetBuy,
                    totalTradingValue = totalTradingValue,
                    shortSellingRatio = shortSellingRatio
                )

                // Cache the result
                cacheData(cacheKey, CACHE_TYPE_FEAR_GREED, json.encodeToString(fearGreed))

                Result.success(fearGreed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get Fear & Greed: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun getFearGreedHistory(
        dateRange: MarketDateRange
    ): Result<FearGreedHistory> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cacheKey = "${CACHE_KEY_FEAR_GREED_HISTORY}_${dateRange.days}d"
            getCachedData(cacheKey)?.let { data ->
                return@withContext Result.success(json.decodeFromString<FearGreedHistory>(data))
            }

            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(dateRange.days.toLong() + 60)
            val endDateStr = endDate.format(dateFormatter)
            val startDateStr = startDate.format(dateFormatter)

            // Get extended KOSPI data for rolling calculations
            val indexResult = krxDataSource.getIndexOhlcv(
                startDate = startDateStr,
                endDate = endDateStr,
                ticker = KOSPI_TICKER
            )

            val indexData = indexResult.getOrNull()
            if (indexData == null || indexData.size < 30) {
                return@withContext Result.failure(
                    IllegalStateException("Insufficient KOSPI data for Fear & Greed history")
                )
            }

            val closes = indexData.map { it.close.toDouble() }
            val resultDates = mutableListOf<String>()
            val resultScores = mutableListOf<Double>()
            val resultSignals = mutableListOf<FearGreedSignal>()
            val resultIndexValues = mutableListOf<Double>()

            // Calculate rolling momentum + RSI for each date window
            val minWindow = 30
            for (i in minWindow until closes.size) {
                val windowCloses = closes.subList(0, i + 1)
                val momentum = calcSimpleMomentum(windowCloses)
                val rsi = calcSimpleRsi(windowCloses)
                val score = (momentum + rsi) / 2.0

                resultDates.add(indexData[i].date)
                resultScores.add(score.coerceIn(0.0, 100.0))
                resultSignals.add(FearGreedSignal.fromScore(score))
                resultIndexValues.add(closes[i])
            }

            if (resultDates.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Fear & Greed history calculation produced no results")
                )
            }

            val targetStartDate = endDate.minusDays(dateRange.days.toLong())
            val targetStartStr = targetStartDate.format(dateFormatter)
            val trimIndex = resultDates.indexOfFirst { it >= targetStartStr }
            val startIdx = if (trimIndex >= 0) trimIndex else 0

            val history = FearGreedHistory(
                dates = resultDates.subList(startIdx, resultDates.size).toList(),
                scores = resultScores.subList(startIdx, resultScores.size).toList(),
                signals = resultSignals.subList(startIdx, resultSignals.size).toList(),
                indexValues = resultIndexValues.subList(startIdx, resultIndexValues.size).toList()
            )

            // Cache the result
            cacheData(cacheKey, CACHE_TYPE_FEAR_GREED, json.encodeToString(history))

            Result.success(history)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Fear & Greed history: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getOscillatorHistory(
        dateRange: MarketDateRange
    ): Result<OscillatorHistory> = withContext(ioDispatcher) {
        try {
            // Check cache first (oscillator is very expensive: ~6s for 20 days)
            val cacheKey = "${CACHE_KEY_OSCILLATOR}_${dateRange.days}d"
            getCachedData(cacheKey)?.let { data ->
                return@withContext Result.success(json.decodeFromString<OscillatorHistory>(data))
            }

            val endDate = LocalDate.now()
            val dates = mutableListOf<String>()
            val advances = mutableListOf<Int>()
            val declines = mutableListOf<Int>()
            val unchanges = mutableListOf<Int>()
            val totals = mutableListOf<Int>()

            val sampleDays = minOf(dateRange.days, MAX_OSCILLATOR_SAMPLE_DAYS)
            var currentDate = endDate
            var fetchedDays = 0
            var consecutiveFailures = 0
            val cutoffDate = endDate.minusDays(dateRange.days.toLong() + 30)

            while (fetchedDays < sampleDays && currentDate.isAfter(cutoffDate)) {
                ensureActive()

                if (currentDate.dayOfWeek.value >= 6) {
                    currentDate = currentDate.minusDays(1)
                    continue
                }

                if (fetchedDays > 0) {
                    delay(KRX_CALL_DELAY_MS)
                }

                val dateStr = currentDate.format(dateFormatter)
                val result = krxDataSource.getMarketOhlcv(dateStr, Market.ALL)

                result.onSuccess { ohlcvList ->
                    if (ohlcvList.isNotEmpty()) {
                        var adv = 0
                        var dec = 0
                        var unch = 0

                        for (stock in ohlcvList) {
                            when {
                                stock.changeRate > 0 -> adv++
                                stock.changeRate < 0 -> dec++
                                else -> unch++
                            }
                        }

                        dates.add(dateStr)
                        advances.add(adv)
                        declines.add(dec)
                        unchanges.add(unch)
                        totals.add(ohlcvList.size)
                        fetchedDays++
                        consecutiveFailures = 0
                    } else {
                        consecutiveFailures++
                    }
                }.onFailure {
                    consecutiveFailures++
                    Log.w(TAG, "KRX oscillator fetch failed for $dateStr: ${it.message}")
                }

                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    Log.w(TAG, "Oscillator: $consecutiveFailures consecutive failures, stopping early (fetched $fetchedDays days)")
                    break
                }

                currentDate = currentDate.minusDays(1)
            }

            if (dates.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("No market OHLCV data available")
                )
            }

            val oscillator = MarketCalculator.calculateOscillator(
                dates = dates.reversed(),
                advances = advances.reversed(),
                declines = declines.reversed(),
                unchanges = unchanges.reversed(),
                totals = totals.reversed()
            )

            // Cache the result
            cacheData(cacheKey, CACHE_TYPE_OSCILLATOR, json.encodeToString(oscillator))

            Result.success(oscillator)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get oscillator: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getFundFlowHistory(
        dateRange: MarketDateRange
    ): Result<FundFlowHistory> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cacheKey = "${CACHE_KEY_FUND_FLOW}_${dateRange.days}d"
            getCachedData(cacheKey)?.let { data ->
                return@withContext Result.success(json.decodeFromString<FundFlowHistory>(data))
            }

            // Use shared InvestorTradingService (DB cache + KRX)
            val investorResult = investorTradingService.getMarketInvestorTrading(
                days = dateRange.days,
                market = InvestorTradingService.MARKET_ALL
            )

            val tradingList = investorResult.getOrNull()
            if (tradingList == null || tradingList.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("No investor trading data available")
                )
            }

            val dates = mutableListOf<String>()
            val foreignNetBuys = mutableListOf<Long>()
            val institutionNetBuys = mutableListOf<Long>()
            val individualNetBuys = mutableListOf<Long>()
            val totalTradingValues = mutableListOf<Long>()

            for (trading in tradingList) {
                dates.add(trading.date)
                foreignNetBuys.add(trading.foreignNet)
                institutionNetBuys.add(trading.institutionNet)
                individualNetBuys.add(trading.individualNet)
                totalTradingValues.add(kotlin.math.abs(trading.totalTrading))
            }

            val history = FundFlowHistory(
                dates = dates,
                foreignNetBuys = foreignNetBuys,
                institutionNetBuys = institutionNetBuys,
                individualNetBuys = individualNetBuys,
                totalTradingValues = totalTradingValues
            )

            // Cache the result
            cacheData(cacheKey, CACHE_TYPE_FUND_FLOW, json.encodeToString(history))

            Result.success(history)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get fund flow: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getBloodIndicatorHistory(
        dateRange: MarketDateRange
    ): Result<BloodIndicatorHistory> = withContext(ioDispatcher) {
        try {
            // Blood Indicator requires external data (Yahoo Finance + FRED API)
            // For now, return a failure indicating that data collection is needed
            // The actual implementation will use the settings-stored FRED API key
            // and fetch ^IRX (3-Month T-Bill) from Yahoo Finance
            Result.failure(
                IllegalStateException(
                    "Blood Indicator 데이터를 수집해 주세요.\n" +
                        "설정 > 시장 지표에서 FRED API 키를 입력하고 데이터를 수집하세요."
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Blood Indicator: ${e.message}")
            Result.failure(e)
        }
    }

    private fun calcSimpleMomentum(closes: List<Double>): Double {
        if (closes.size < 21) return 50.0
        val current = closes.last()
        val past = closes[closes.size - 21]
        val ret = if (past > 0) (current - past) / past * 100 else 0.0
        return ((ret + 10.0) / 20.0 * 100).coerceIn(0.0, 100.0)
    }

    private fun calcSimpleRsi(closes: List<Double>): Double {
        if (closes.size < 15) return 50.0
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in closes.size - 14 until closes.size) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) avgGain += change else avgLoss += kotlin.math.abs(change)
        }
        avgGain /= 14.0
        avgLoss /= 14.0
        return if (avgLoss == 0.0) 100.0
        else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
    }

    // ===== Cache helpers =====

    private suspend fun getCachedData(cacheKey: String): String? {
        return try {
            val minTimestamp = System.currentTimeMillis() - AppConfig.MARKET_CACHE_TTL_MS
            marketIndicatorCacheDao.getIfFresh(cacheKey, minTimestamp)?.data
        } catch (e: Exception) {
            Log.w(TAG, "Cache read failed for $cacheKey: ${e.message}")
            null
        }
    }

    private suspend fun cacheData(cacheKey: String, type: String, data: String) {
        try {
            marketIndicatorCacheDao.upsert(
                MarketIndicatorCacheEntity(
                    key = cacheKey,
                    type = type,
                    data = data,
                    cachedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cache write failed for $cacheKey: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "MarketRepoImpl"
        const val KOSPI_TICKER = "1001"
        const val KOSDAQ_TICKER = "2001"

        private const val MAX_OSCILLATOR_SAMPLE_DAYS = 20
        private const val KRX_CALL_DELAY_MS = 300L
        private const val MAX_CONSECUTIVE_FAILURES = 3

        // Cache keys
        private const val CACHE_KEY_FEAR_GREED_LATEST = "fear_greed_latest"
        private const val CACHE_KEY_FEAR_GREED_HISTORY = "fear_greed_history"
        private const val CACHE_KEY_OSCILLATOR = "oscillator"
        private const val CACHE_KEY_FUND_FLOW = "fund_flow"

        // Cache types
        private const val CACHE_TYPE_FEAR_GREED = "fear_greed"
        private const val CACHE_TYPE_OSCILLATOR = "oscillator"
        private const val CACHE_TYPE_FUND_FLOW = "fund_flow"
    }
}
