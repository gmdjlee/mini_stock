package com.stockapp.feature.etf.domain.usecase

import android.util.Log
import com.stockapp.core.util.TradingDayUtil
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.DayCollectionResult
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.model.FullCollectionResult
import com.stockapp.feature.etf.domain.model.MultiDayCollectionResult
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import com.stockapp.feature.etf.domain.repo.EtfRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "CollectAllEtfDataUC"
private const val INTER_DAY_DELAY_MS = 3_000L // 3 seconds between trading days
private const val MAX_CONSECUTIVE_FAILURES = 5 // Abort after N consecutive failed days

/**
 * Use case for collecting all filtered ETF data.
 */
class CollectAllEtfDataUC @Inject constructor(
    private val repo: EtfCollectorRepo,
    private val etfRepository: EtfRepository
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Collect constituent data for all filtered ETFs.
     *
     * @param filterConfig Optional filter configuration to apply before collection
     * @param cleanupDays Number of days to keep data (older data will be deleted)
     * @param progressCallback Optional callback for progress updates (current, total)
     * @return Full collection result
     */
    suspend operator fun invoke(
        filterConfig: EtfFilterConfig? = null,
        cleanupDays: Int = 30,
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): FullCollectionResult {
        // First, refresh ETF list from API to ensure database has ETF data
        refreshEtfList()

        // Apply filter if provided
        filterConfig?.let {
            repo.applyKeywordFilter(it)
        }

        // Collect all filtered ETFs
        val result = repo.collectAllFilteredEtfs(progressCallback)

        // Calculate daily statistics after successful collection
        if (result.status == CollectionStatus.SUCCESS || result.status == CollectionStatus.PARTIAL) {
            val collectedDateStr = result.collectedDate.format(dateFormat)
            Log.d(TAG, "Calculating daily statistics for date: $collectedDateStr")
            etfRepository.calculateDailyStatistics(collectedDateStr).fold(
                onSuccess = {
                    Log.d(TAG, "Daily statistics calculated successfully")
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to calculate daily statistics: ${error.message}")
                }
            )
        }

        // Cleanup old data
        if (cleanupDays > 0) {
            val cutoffDate = LocalDate.now().minusDays(cleanupDays.toLong()).format(dateFormat)
            repo.deleteOldData(cutoffDate)
        }

        return result
    }

    /**
     * Collect ETF data across a date range (startDate to today).
     * Skips dates that already have collected data.
     *
     * @param startDate Start date for collection range
     * @param filterConfig Optional filter configuration
     * @param cleanupDays Number of days to keep data
     * @param dayProgressCallback Callback for day-level progress (dayIndex, totalDays, dateStr)
     * @param etfProgressCallback Callback for ETF-level progress within a day (current, total)
     * @return Multi-day collection result
     */
    suspend fun invokeWithDateRange(
        startDate: LocalDate,
        filterConfig: EtfFilterConfig? = null,
        cleanupDays: Int = 30,
        dayProgressCallback: ((dayIndex: Int, totalDays: Int, dateStr: String) -> Unit)? = null,
        etfProgressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): MultiDayCollectionResult {
        // Refresh ETF list and apply filter
        refreshEtfList()
        filterConfig?.let { repo.applyKeywordFilter(it) }

        val endDate = LocalDate.now()
        val tradingDays = TradingDayUtil.getTradingDaysBetween(startDate, endDate)
        val collectedDates = repo.getCollectedDatesSet()

        // Partition into to-collect and to-skip
        val dayResults = mutableListOf<DayCollectionResult>()
        var totalEtfs = 0
        var totalConstituents = 0
        var successDays = 0
        var skippedDays = 0
        var failedDays = 0

        val tradingDateStrings = tradingDays.map { it.format(dateFormat) }
        val toCollect = tradingDays.filterIndexed { index, _ ->
            tradingDateStrings[index] !in collectedDates
        }
        val totalDays = tradingDays.size

        Log.d(TAG, "Date range: $startDate ~ $endDate, trading days: $totalDays, " +
            "to collect: ${toCollect.size}, already collected: ${totalDays - toCollect.size}")

        // Add skipped days
        tradingDays.forEachIndexed { index, date ->
            val dateStr = tradingDateStrings[index]
            if (dateStr in collectedDates) {
                dayResults.add(
                    DayCollectionResult(
                        date = dateStr,
                        skipped = true,
                        etfCount = 0,
                        constituentCount = 0,
                        errorMessage = null
                    )
                )
                skippedDays++
            }
        }

        // Collect each uncollected day with inter-day delay to avoid KRX rate limiting
        var consecutiveFailures = 0
        for ((collectIndex, date) in toCollect.withIndex()) {
            val dateStr = date.format(dateFormat)
            dayProgressCallback?.invoke(collectIndex + 1, toCollect.size, dateStr)

            // Inter-day delay (skip for the first day)
            if (collectIndex > 0) {
                val delayMs = if (consecutiveFailures > 0) {
                    // Exponential backoff on consecutive failures (3s, 6s, 12s, max 30s)
                    minOf(INTER_DAY_DELAY_MS * (1L shl consecutiveFailures), 30_000L)
                } else {
                    INTER_DAY_DELAY_MS
                }
                Log.d(TAG, "Inter-day delay: ${delayMs}ms before collecting $dateStr")
                delay(delayMs)
            }

            try {
                val result = repo.collectAllFilteredEtfsForDate(date, etfProgressCallback)

                dayResults.add(
                    DayCollectionResult(
                        date = dateStr,
                        skipped = false,
                        etfCount = result.successCount,
                        constituentCount = result.totalConstituents,
                        errorMessage = result.errorMessage
                    )
                )

                totalEtfs += result.successCount
                totalConstituents += result.totalConstituents

                when (result.status) {
                    CollectionStatus.SUCCESS, CollectionStatus.PARTIAL -> {
                        successDays++
                        consecutiveFailures = 0
                        // Calculate daily statistics
                        etfRepository.calculateDailyStatistics(dateStr).fold(
                            onSuccess = { Log.d(TAG, "Statistics calculated for $dateStr") },
                            onFailure = { Log.e(TAG, "Statistics failed for $dateStr: ${it.message}") }
                        )
                    }
                    CollectionStatus.FAILED -> {
                        failedDays++
                        consecutiveFailures++
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Collection failed for date $dateStr: ${e.message}")
                dayResults.add(
                    DayCollectionResult(
                        date = dateStr,
                        skipped = false,
                        etfCount = 0,
                        constituentCount = 0,
                        errorMessage = e.message
                    )
                )
                failedDays++
                consecutiveFailures++
            }

            // Abort if too many consecutive failures (likely rate limited)
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                Log.e(TAG, "Aborting: $MAX_CONSECUTIVE_FAILURES consecutive failures, likely rate limited")
                break
            }
        }

        // Cleanup old data
        if (cleanupDays > 0) {
            val cutoffDate = LocalDate.now().minusDays(cleanupDays.toLong()).format(dateFormat)
            repo.deleteOldData(cutoffDate)
        }

        return MultiDayCollectionResult(
            totalDays = totalDays,
            successDays = successDays,
            skippedDays = skippedDays,
            failedDays = failedDays,
            totalEtfs = totalEtfs,
            totalConstituents = totalConstituents,
            dayResults = dayResults.sortedBy { it.date }
        )
    }

    /**
     * Refresh ETF list from API and save to database.
     *
     * @return Number of ETFs saved
     */
    suspend fun refreshEtfList(): Result<Int> {
        return repo.fetchEtfList().map { etfList ->
            // Save ETFs to database
            val entities = etfList.map { info ->
                com.stockapp.core.db.entity.EtfEntity(
                    etfCode = info.etfCode,
                    etfName = info.etfName,
                    etfType = info.etfType.value,
                    managementCompany = info.managementCompany,
                    trackingIndex = info.trackingIndex,
                    assetClass = info.assetClass,
                    totalAssets = info.totalAssets,
                    isFiltered = false, // Will be updated by filter
                    updatedAt = System.currentTimeMillis()
                )
            }
            repo.saveEtfs(entities)
            etfList.size
        }
    }
}
