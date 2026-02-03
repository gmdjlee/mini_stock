package com.stockapp.feature.realtime.domain.usecase

import com.stockapp.feature.realtime.domain.model.RealtimeSupplySummary
import com.stockapp.feature.realtime.domain.model.toSummary
import com.stockapp.feature.realtime.domain.repo.RealtimeSupplyRepo
import javax.inject.Inject

/**
 * Use case for getting realtime supply summary (for UI).
 */
class GetRealtimeSupplySummaryUC @Inject constructor(
    private val repo: RealtimeSupplyRepo
) {
    /**
     * Get realtime supply summary for UI display.
     */
    suspend operator fun invoke(
        ticker: String,
        useCache: Boolean = true
    ): Result<RealtimeSupplySummary> {
        return repo.getRealtimeSupply(ticker, useCache).map { it.toSummary() }
    }
}

/**
 * Use case for refreshing realtime supply data (bypasses cache).
 */
class RefreshRealtimeSupplyUC @Inject constructor(
    private val repo: RealtimeSupplyRepo
) {
    /**
     * Refresh realtime supply data (bypasses cache).
     */
    suspend operator fun invoke(ticker: String): Result<RealtimeSupplySummary> {
        return repo.getRealtimeSupply(ticker, useCache = false).map { it.toSummary() }
    }
}
