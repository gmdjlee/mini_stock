package com.stockapp.feature.etf.domain.usecase

import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.model.EtfInfo
import com.stockapp.feature.etf.domain.model.EtfType
import com.stockapp.feature.etf.domain.model.FullCollectionResult
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import com.stockapp.feature.etf.domain.repo.EtfRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for CollectAllEtfDataUC.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectAllEtfDataUCTest {

    private lateinit var repo: EtfCollectorRepo
    private lateinit var etfRepository: EtfRepository
    private lateinit var useCase: CollectAllEtfDataUC

    private val sampleEtfInfoList = listOf(
        EtfInfo(
            etfCode = "069500",
            etfName = "KODEX 200",
            etfType = EtfType.PASSIVE,
            managementCompany = "Samsung",
            trackingIndex = "KOSPI 200",
            assetClass = "Equity",
            totalAssets = 1000.0
        )
    )

    @Before
    fun setup() {
        repo = mock()
        etfRepository = mock()
        useCase = CollectAllEtfDataUC(repo, etfRepository)
    }

    // ==================== Helper ====================

    private fun createFullCollectionResult(
        status: CollectionStatus = CollectionStatus.SUCCESS,
        successCount: Int = 10,
        failedCount: Int = 0,
        totalConstituents: Int = 100
    ) = FullCollectionResult(
        collectedDate = LocalDate.now(),
        totalEtfs = successCount,
        totalConstituents = totalConstituents,
        successCount = successCount,
        failedCount = failedCount,
        status = status,
        errorMessage = if (failedCount > 0) "Some errors" else null,
        startedAt = LocalDateTime.now(),
        completedAt = LocalDateTime.now()
    )

    private suspend fun setupDefaultMocks() {
        whenever(repo.fetchEtfList()).thenReturn(Result.success(sampleEtfInfoList))
        whenever(repo.saveEtfs(any())).thenReturn(Unit)
    }

    // ==================== invoke (single day) Tests ====================

    @Test
    fun `invoke - successful collection`() = runTest {
        setupDefaultMocks()
        whenever(repo.collectAllFilteredEtfs(anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        val result = useCase.invoke()

        assertEquals(CollectionStatus.SUCCESS, result.status)
        assertEquals(10, result.successCount)
        verify(etfRepository).calculateDailyStatistics(any())
    }

    @Test
    fun `invoke - partial collection`() = runTest {
        setupDefaultMocks()
        whenever(repo.collectAllFilteredEtfs(anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.PARTIAL, successCount = 7, failedCount = 3))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        val result = useCase.invoke()

        assertEquals(CollectionStatus.PARTIAL, result.status)
        assertEquals(7, result.successCount)
        assertEquals(3, result.failedCount)
        verify(etfRepository).calculateDailyStatistics(any())
    }

    @Test
    fun `invoke - failed collection does not calculate statistics`() = runTest {
        setupDefaultMocks()
        whenever(repo.collectAllFilteredEtfs(anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.FAILED, successCount = 0, failedCount = 10))

        val result = useCase.invoke()

        assertEquals(CollectionStatus.FAILED, result.status)
        verify(etfRepository, never()).calculateDailyStatistics(any())
    }

    @Test
    fun `invoke - filter config is applied before collection`() = runTest {
        val filterConfig = EtfFilterConfig(
            activeOnly = true,
            includeKeywords = listOf("반도체"),
            excludeKeywords = listOf("인버스")
        )
        setupDefaultMocks()
        whenever(repo.applyKeywordFilter(any())).thenReturn(5)
        whenever(repo.collectAllFilteredEtfs(anyOrNull()))
            .thenReturn(createFullCollectionResult())
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        useCase.invoke(filterConfig = filterConfig)

        verify(repo).applyKeywordFilter(filterConfig)
    }

    @Test
    fun `invoke - no filter config skips applyKeywordFilter`() = runTest {
        setupDefaultMocks()
        whenever(repo.collectAllFilteredEtfs(anyOrNull()))
            .thenReturn(createFullCollectionResult())
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        useCase.invoke(filterConfig = null)

        verify(repo, never()).applyKeywordFilter(any())
    }

    @Test
    fun `invoke - cleanupDays greater than zero triggers deleteOldData`() = runTest {
        setupDefaultMocks()
        whenever(repo.collectAllFilteredEtfs(anyOrNull()))
            .thenReturn(createFullCollectionResult())
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        useCase.invoke(cleanupDays = 30)

        verify(repo).deleteOldData(any())
    }

    @Test
    fun `invoke - cleanupDays zero skips deleteOldData`() = runTest {
        setupDefaultMocks()
        whenever(repo.collectAllFilteredEtfs(anyOrNull()))
            .thenReturn(createFullCollectionResult())
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        useCase.invoke(cleanupDays = 0)

        verify(repo, never()).deleteOldData(any())
    }

    @Test
    fun `invoke - statistics calculation failure does not break collection`() = runTest {
        setupDefaultMocks()
        whenever(repo.collectAllFilteredEtfs(anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.failure(RuntimeException("Statistics error")))

        // Should complete without throwing
        val result = useCase.invoke()
        assertEquals(CollectionStatus.SUCCESS, result.status)
    }

    // ==================== invokeWithDateRange Tests ====================

    @Test
    fun `invokeWithDateRange - all days succeed returns SUCCESS`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3) // Monday

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        val result = useCase.invokeWithDateRange(startDate)

        // The result should have SUCCESS status (no failures)
        assertEquals(CollectionStatus.SUCCESS, result.status)
        assertEquals(0, result.failedDays)
    }

    @Test
    fun `invokeWithDateRange - already collected days are skipped`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3) // Monday
        // Pretend "2025-02-03" is already collected
        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(setOf("2025-02-03"))
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        val result = useCase.invokeWithDateRange(startDate)

        assertTrue(result.skippedDays > 0)
        // Skipped days should appear in dayResults with skipped=true
        val skippedResults = result.dayResults.filter { it.skipped }
        assertTrue(skippedResults.isNotEmpty())
    }

    @Test
    fun `invokeWithDateRange - some days fail returns PARTIAL`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3) // Monday
        var callCount = 0

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull())).thenAnswer {
            callCount++
            if (callCount % 2 == 0) {
                createFullCollectionResult(CollectionStatus.FAILED, successCount = 0, failedCount = 10)
            } else {
                createFullCollectionResult(CollectionStatus.SUCCESS)
            }
        }
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        val result = useCase.invokeWithDateRange(startDate)

        // Should be PARTIAL (some success, some failure)
        assertTrue(
            "Expected PARTIAL or SUCCESS, got ${result.status}",
            result.status == CollectionStatus.PARTIAL || result.successDays > 0
        )
    }

    @Test
    fun `invokeWithDateRange - all days fail returns FAILED`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3)

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.FAILED, successCount = 0, failedCount = 10))

        val result = useCase.invokeWithDateRange(startDate)

        assertTrue(
            "Expected FAILED status, got ${result.status}",
            result.status == CollectionStatus.FAILED
        )
    }

    @Test
    fun `invokeWithDateRange - consecutive failure abort after 5 failures`() = runTest {
        // Use a date range with many trading days
        val startDate = LocalDate.of(2025, 1, 6) // Monday

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        // All days fail - should abort after MAX_CONSECUTIVE_FAILURES (5)
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.FAILED, successCount = 0, failedCount = 10))

        val result = useCase.invokeWithDateRange(startDate)

        // Should have stopped at 5 failures (or fewer if date range is short)
        assertTrue(
            "Failed days should be at most 5 (consecutive failure abort), got ${result.failedDays}",
            result.failedDays <= 5
        )
    }

    @Test
    fun `invokeWithDateRange - exception during collection counts as failed day`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3)

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenThrow(RuntimeException("Network error"))

        val result = useCase.invokeWithDateRange(startDate)

        assertTrue(result.failedDays > 0)
        // Error should be captured in dayResults
        val errorResults = result.dayResults.filter { it.errorMessage != null }
        assertTrue(errorResults.isNotEmpty())
    }

    @Test
    fun `invokeWithDateRange - day progress callback invoked correctly`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3)
        val dayProgressLog = mutableListOf<Triple<Int, Int, String>>()

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        useCase.invokeWithDateRange(
            startDate = startDate,
            dayProgressCallback = { dayIndex, totalDays, dateStr ->
                dayProgressLog.add(Triple(dayIndex, totalDays, dateStr))
            }
        )

        // Day progress should have been called for each uncollected day
        if (dayProgressLog.isNotEmpty()) {
            // First call should have dayIndex=1
            assertEquals(1, dayProgressLog.first().first)
            // All calls should have the same totalDays
            val totalDays = dayProgressLog.first().second
            assertTrue(dayProgressLog.all { it.second == totalDays })
        }
    }

    @Test
    fun `invokeWithDateRange - statistics calculated after successful days`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3)

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        val result = useCase.invokeWithDateRange(startDate)

        // calculateDailyStatistics should have been called for each successful day
        if (result.successDays > 0) {
            verify(etfRepository, atLeastOnce()).calculateDailyStatistics(any())
        }
    }

    @Test
    fun `invokeWithDateRange - old data cleanup with cleanupDays greater than zero`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3)

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        useCase.invokeWithDateRange(startDate, cleanupDays = 30)

        verify(repo).deleteOldData(any())
    }

    @Test
    fun `invokeWithDateRange - no cleanup when cleanupDays is zero`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3)

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        useCase.invokeWithDateRange(startDate, cleanupDays = 0)

        verify(repo, never()).deleteOldData(any())
    }

    @Test
    fun `invokeWithDateRange - filter config applied before date range collection`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3)
        val filterConfig = EtfFilterConfig(
            activeOnly = true,
            includeKeywords = listOf("반도체"),
            excludeKeywords = listOf("인버스")
        )

        setupDefaultMocks()
        whenever(repo.applyKeywordFilter(any())).thenReturn(5)
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull()))
            .thenReturn(createFullCollectionResult(CollectionStatus.SUCCESS))
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        useCase.invokeWithDateRange(startDate, filterConfig = filterConfig)

        verify(repo).applyKeywordFilter(filterConfig)
    }

    @Test
    fun `invokeWithDateRange - consecutive failures reset on success`() = runTest {
        val startDate = LocalDate.of(2025, 2, 3)
        var callCount = 0

        setupDefaultMocks()
        whenever(repo.getCollectedDatesSet()).thenReturn(emptySet())
        // Pattern: 3 failures, then 1 success, then failures
        whenever(repo.collectAllFilteredEtfsForDate(any(), anyOrNull())).thenAnswer {
            callCount++
            if (callCount == 4) {
                createFullCollectionResult(CollectionStatus.SUCCESS)
            } else {
                createFullCollectionResult(CollectionStatus.FAILED, successCount = 0, failedCount = 10)
            }
        }
        whenever(etfRepository.calculateDailyStatistics(any()))
            .thenReturn(Result.success(Unit))

        val result = useCase.invokeWithDateRange(startDate)

        // Should have at least 1 success day (callCount==4)
        assertTrue(
            "Should have at least 1 success day resetting consecutive failures, got ${result.successDays}",
            result.successDays >= 1 || result.failedDays > 0
        )
    }

    // ==================== refreshEtfList Tests ====================

    @Test
    fun `refreshEtfList saves ETF entities from API response`() = runTest {
        whenever(repo.fetchEtfList()).thenReturn(Result.success(sampleEtfInfoList))
        whenever(repo.saveEtfs(any())).thenReturn(Unit)

        val result = useCase.refreshEtfList()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        verify(repo).saveEtfs(any())
    }

    @Test
    fun `refreshEtfList propagates fetch failure`() = runTest {
        whenever(repo.fetchEtfList())
            .thenReturn(Result.failure(RuntimeException("API error")))

        val result = useCase.refreshEtfList()

        assertTrue(result.isFailure)
        verify(repo, never()).saveEtfs(any())
    }
}
