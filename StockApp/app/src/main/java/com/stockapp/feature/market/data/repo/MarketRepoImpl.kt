package com.stockapp.feature.market.data.repo

import android.util.Log
import com.krxkt.model.AskBidType
import com.krxkt.model.Market
import com.krxkt.model.TradingValueType
import com.stockapp.core.di.IoDispatcher
import com.stockapp.core.krx.KrxDataSource
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MarketRepo {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    override suspend fun getFearGreedIndex(): Result<MarketFearGreed> =
        withContext(ioDispatcher) {
            try {
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

                // 2. Get investor trading data (recent)
                val investorResult = krxDataSource.getMarketTradingByInvestor(
                    startDate = endDate.minusDays(5).format(dateFormatter),
                    endDate = endDateStr,
                    market = Market.ALL,
                    valueType = TradingValueType.VALUE,
                    askBidType = AskBidType.NET_BUY
                )

                var foreignNetBuy = 0L
                var institutionNetBuy = 0L
                var totalTradingValue = 1L

                investorResult.onSuccess { tradingList ->
                    for (trading in tradingList) {
                        foreignNetBuy += trading.foreigner
                        institutionNetBuy += trading.institutionalTotal
                        totalTradingValue += kotlin.math.abs(trading.total)
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

            Result.success(
                FearGreedHistory(
                    dates = resultDates.subList(startIdx, resultDates.size).toList(),
                    scores = resultScores.subList(startIdx, resultScores.size).toList(),
                    signals = resultSignals.subList(startIdx, resultSignals.size).toList(),
                    indexValues = resultIndexValues.subList(startIdx, resultIndexValues.size).toList()
                )
            )
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
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(dateRange.days.toLong())
            val endDateStr = endDate.format(dateFormatter)
            val startDateStr = startDate.format(dateFormatter)

            val investorResult = krxDataSource.getMarketTradingByInvestor(
                startDate = startDateStr,
                endDate = endDateStr,
                market = Market.ALL,
                valueType = TradingValueType.VALUE,
                askBidType = AskBidType.NET_BUY
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
                foreignNetBuys.add(trading.foreigner)
                institutionNetBuys.add(trading.institutionalTotal)
                individualNetBuys.add(trading.individual)
                totalTradingValues.add(kotlin.math.abs(trading.total))
            }

            Result.success(
                FundFlowHistory(
                    dates = dates,
                    foreignNetBuys = foreignNetBuys,
                    institutionNetBuys = institutionNetBuys,
                    individualNetBuys = individualNetBuys,
                    totalTradingValues = totalTradingValues
                )
            )
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

    companion object {
        private const val TAG = "MarketRepoImpl"
        const val KOSPI_TICKER = "1001"
        const val KOSDAQ_TICKER = "2001"

        private const val MAX_OSCILLATOR_SAMPLE_DAYS = 20
        private const val KRX_CALL_DELAY_MS = 300L
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
