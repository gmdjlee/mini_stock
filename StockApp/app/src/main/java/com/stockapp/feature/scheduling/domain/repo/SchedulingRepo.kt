package com.stockapp.feature.scheduling.domain.repo

import com.stockapp.feature.scheduling.domain.model.SchedulingConfig
import com.stockapp.feature.scheduling.domain.model.SyncHistory
import com.stockapp.feature.scheduling.domain.model.SyncResult
import com.stockapp.feature.scheduling.domain.model.SyncType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for scheduling configuration and sync operations.
 */
interface SchedulingRepo {
    /**
     * Observe scheduling configuration changes.
     */
    fun observeConfig(): Flow<SchedulingConfig>

    /**
     * Get current scheduling configuration.
     */
    suspend fun getConfig(): SchedulingConfig

    /**
     * Update scheduling enabled state.
     */
    suspend fun setEnabled(enabled: Boolean)

    /**
     * Update sync time.
     */
    suspend fun setSyncTime(hour: Int, minute: Int)

    /**
     * Update last sync status.
     */
    suspend fun updateLastSync(syncedAt: Long, success: Boolean, message: String?)

    /**
     * Observe sync history.
     */
    fun observeSyncHistory(limit: Int = 10): Flow<List<SyncHistory>>

    /**
     * Get latest sync history.
     */
    suspend fun getLatestSync(): SyncHistory?

    /**
     * Execute stock list sync.
     * Returns the number of stocks synced.
     */
    suspend fun syncStockList(): Result<Int>

    /**
     * Execute full data sync (stock list + analysis data).
     */
    suspend fun syncAllData(syncType: SyncType): SyncResult

    /**
     * Execute analysis data sync for specific tickers.
     * Only fetches new data since last analyzed date.
     */
    suspend fun syncAnalysisData(tickers: List<String>): Result<Int>

    /**
     * Check if there's new data available since last sync.
     */
    suspend fun hasNewDataAvailable(): Boolean
}
