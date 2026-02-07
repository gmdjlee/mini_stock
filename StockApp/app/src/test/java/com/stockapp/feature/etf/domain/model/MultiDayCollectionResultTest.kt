package com.stockapp.feature.etf.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for MultiDayCollectionResult status computation.
 */
class MultiDayCollectionResultTest {

    @Test
    fun `status is SUCCESS when no failed days and some success days`() {
        val result = MultiDayCollectionResult(
            totalDays = 5,
            successDays = 5,
            skippedDays = 0,
            failedDays = 0,
            totalEtfs = 50,
            totalConstituents = 500,
            dayResults = emptyList()
        )

        assertEquals(CollectionStatus.SUCCESS, result.status)
    }

    @Test
    fun `status is FAILED when no success days and some failed days`() {
        val result = MultiDayCollectionResult(
            totalDays = 3,
            successDays = 0,
            skippedDays = 0,
            failedDays = 3,
            totalEtfs = 0,
            totalConstituents = 0,
            dayResults = emptyList()
        )

        assertEquals(CollectionStatus.FAILED, result.status)
    }

    @Test
    fun `status is PARTIAL when both success and failed days exist`() {
        val result = MultiDayCollectionResult(
            totalDays = 5,
            successDays = 3,
            skippedDays = 0,
            failedDays = 2,
            totalEtfs = 30,
            totalConstituents = 300,
            dayResults = emptyList()
        )

        assertEquals(CollectionStatus.PARTIAL, result.status)
    }

    @Test
    fun `status is SUCCESS when all days are skipped`() {
        val result = MultiDayCollectionResult(
            totalDays = 5,
            successDays = 0,
            skippedDays = 5,
            failedDays = 0,
            totalEtfs = 0,
            totalConstituents = 0,
            dayResults = emptyList()
        )

        assertEquals(CollectionStatus.SUCCESS, result.status)
    }

    @Test
    fun `status is SUCCESS when no days at all`() {
        val result = MultiDayCollectionResult(
            totalDays = 0,
            successDays = 0,
            skippedDays = 0,
            failedDays = 0,
            totalEtfs = 0,
            totalConstituents = 0,
            dayResults = emptyList()
        )

        assertEquals(CollectionStatus.SUCCESS, result.status)
    }

    @Test
    fun `status is PARTIAL when successDays greater than zero and failedDays greater than zero with skipped`() {
        val result = MultiDayCollectionResult(
            totalDays = 10,
            successDays = 5,
            skippedDays = 3,
            failedDays = 2,
            totalEtfs = 50,
            totalConstituents = 500,
            dayResults = emptyList()
        )

        assertEquals(CollectionStatus.PARTIAL, result.status)
    }

    @Test
    fun `status is FAILED when only failed days exist with skipped`() {
        val result = MultiDayCollectionResult(
            totalDays = 5,
            successDays = 0,
            skippedDays = 3,
            failedDays = 2,
            totalEtfs = 0,
            totalConstituents = 0,
            dayResults = emptyList()
        )

        assertEquals(CollectionStatus.FAILED, result.status)
    }

    @Test
    fun `status is SUCCESS with one success day and no failures`() {
        val result = MultiDayCollectionResult(
            totalDays = 1,
            successDays = 1,
            skippedDays = 0,
            failedDays = 0,
            totalEtfs = 10,
            totalConstituents = 100,
            dayResults = listOf(
                DayCollectionResult(
                    date = "2025-01-15",
                    skipped = false,
                    etfCount = 10,
                    constituentCount = 100,
                    errorMessage = null
                )
            )
        )

        assertEquals(CollectionStatus.SUCCESS, result.status)
    }
}
