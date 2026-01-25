package com.stockapp.feature.etf.domain.usecase

import com.stockapp.feature.etf.domain.model.EtfCollectionResult
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import javax.inject.Inject

/**
 * Use case for collecting data for a single ETF.
 */
class CollectEtfDataUC @Inject constructor(
    private val repo: EtfCollectorRepo
) {
    /**
     * Collect constituent data for a single ETF.
     *
     * @param etfCode ETF code (6 digits)
     * @param etfName ETF name
     * @return Result containing collection result or error
     */
    suspend operator fun invoke(
        etfCode: String,
        etfName: String
    ): Result<EtfCollectionResult> {
        return repo.collectEtfConstituents(etfCode, etfName)
    }
}
