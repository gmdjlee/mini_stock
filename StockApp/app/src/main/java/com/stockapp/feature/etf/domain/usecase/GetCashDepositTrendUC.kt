package com.stockapp.feature.etf.domain.usecase

import com.stockapp.feature.etf.domain.model.CashDepositTrend
import com.stockapp.feature.etf.domain.model.DateRangeOption
import com.stockapp.feature.etf.domain.model.EtfCashDetail
import com.stockapp.feature.etf.domain.repo.EtfRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for getting cash deposit (예금) trend data.
 * Returns cash/deposit holding trends across ETFs over time.
 */
class GetCashDepositTrendUC @Inject constructor(
    private val repository: EtfRepository
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Get cash deposit trend for the specified date range.
     *
     * @param dateRange Date range option
     * @return Result containing list of cash deposit trend data or error
     */
    suspend operator fun invoke(dateRange: DateRangeOption): Result<CashDepositTrendResult> {
        return try {
            // Get the latest date as end date
            val endDate = repository.getLatestDate().getOrNull()
                ?: return Result.failure(NoDataException("수집된 데이터가 없습니다"))

            // Calculate start date based on range
            val startDate = calculateStartDate(dateRange, endDate)

            // Get cash deposit trend
            val trendResult = repository.getCashDepositTrend(startDate, endDate)
            val trend = trendResult.getOrElse { return Result.failure(it) }

            // Get ETF cash details for the latest date
            val detailsResult = repository.getEtfCashDetails(endDate)
            val details = detailsResult.getOrElse { emptyList() }

            Result.success(
                CashDepositTrendResult(
                    startDate = startDate,
                    endDate = endDate,
                    trend = trend,
                    etfCashDetails = details
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get cash deposit trend for specific dates.
     *
     * @param startDate Start date (YYYY-MM-DD)
     * @param endDate End date (YYYY-MM-DD)
     * @return Result containing list of cash deposit trend data or error
     */
    suspend fun forDates(startDate: String, endDate: String): Result<List<CashDepositTrend>> {
        return try {
            repository.getCashDepositTrend(startDate, endDate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get ETF cash details for a specific date.
     *
     * @param date Target date (YYYY-MM-DD)
     * @return Result containing list of ETF cash details or error
     */
    suspend fun getEtfCashDetails(date: String): Result<List<EtfCashDetail>> {
        return try {
            repository.getEtfCashDetails(date)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateStartDate(dateRange: DateRangeOption, endDate: String): String {
        if (dateRange == DateRangeOption.ALL) {
            return "1970-01-01"
        }

        val endLocalDate = LocalDate.parse(endDate, dateFormat)
        val startLocalDate = endLocalDate.minusDays(dateRange.days.toLong())
        return startLocalDate.format(dateFormat)
    }
}

/**
 * Cash deposit trend result with additional ETF details.
 */
data class CashDepositTrendResult(
    val startDate: String,
    val endDate: String,
    val trend: List<CashDepositTrend>,
    val etfCashDetails: List<EtfCashDetail>
) {
    /**
     * Total cash deposit amount for the latest date.
     */
    val latestTotalAmount: Long
        get() = trend.lastOrNull()?.totalAmount ?: 0L

    /**
     * Latest change amount.
     */
    val latestChangeAmount: Long
        get() = trend.lastOrNull()?.changeAmount ?: 0L

    /**
     * Latest change rate.
     */
    val latestChangeRate: Double
        get() = trend.lastOrNull()?.changeRate ?: 0.0
}
