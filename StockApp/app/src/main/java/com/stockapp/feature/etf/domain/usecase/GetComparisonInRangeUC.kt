package com.stockapp.feature.etf.domain.usecase

import com.stockapp.feature.etf.domain.model.ComparisonResult
import com.stockapp.feature.etf.domain.model.DateRangeOption
import com.stockapp.feature.etf.domain.repo.EtfRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for getting comparison results within a specified date range.
 * Compares holdings between the start and end dates of the selected range.
 */
class GetComparisonInRangeUC @Inject constructor(
    private val repository: EtfRepository
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Get comparison results for the specified date range.
     *
     * @param dateRange Date range option to compare
     * @return Result containing comparison data or error
     */
    suspend operator fun invoke(dateRange: DateRangeOption): Result<ComparisonResult> {
        return try {
            // Get the latest date as end date
            val endDate = repository.getLatestDate().getOrNull()
                ?: return Result.failure(NoDataException("수집된 데이터가 없습니다"))

            // Calculate start date based on range
            val startDate = calculateStartDate(dateRange, endDate)

            // Get comparison results
            repository.getComparisonInRange(startDate, endDate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get comparison results for specific dates.
     *
     * @param startDate Start date (YYYY-MM-DD)
     * @param endDate End date (YYYY-MM-DD)
     * @return Result containing comparison data or error
     */
    suspend fun forDates(startDate: String, endDate: String): Result<ComparisonResult> {
        return try {
            repository.getComparisonInRange(startDate, endDate)
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
