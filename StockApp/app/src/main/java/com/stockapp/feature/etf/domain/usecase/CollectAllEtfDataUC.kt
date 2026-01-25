package com.stockapp.feature.etf.domain.usecase

import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.model.FullCollectionResult
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for collecting all filtered ETF data.
 */
class CollectAllEtfDataUC @Inject constructor(
    private val repo: EtfCollectorRepo
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
        // Apply filter if provided
        filterConfig?.let {
            repo.applyKeywordFilter(it)
        }

        // Collect all filtered ETFs
        val result = repo.collectAllFilteredEtfs(progressCallback)

        // Cleanup old data
        if (cleanupDays > 0) {
            val cutoffDate = LocalDate.now().minusDays(cleanupDays.toLong()).format(dateFormat)
            repo.deleteOldData(cutoffDate)
        }

        return result
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
                    etfType = info.etfType,
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
