package com.stockapp.feature.scheduling.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stockapp.feature.scheduling.domain.model.SyncType
import com.stockapp.feature.scheduling.domain.repo.SchedulingRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "StockSyncWorker"

/**
 * Background worker for syncing stock data.
 * Uses WorkManager to schedule periodic and one-time syncs.
 */
@HiltWorker
class StockSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val schedulingRepo: SchedulingRepo
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() started")

        // Check if sync is enabled or error-stopped
        val config = schedulingRepo.getConfig()
        if (!config.isEnabled || config.isErrorStopped) {
            Log.d(TAG, "Sync is disabled or error-stopped, skipping")
            return Result.success()
        }

        return try {
            // Determine sync type from input data
            val syncTypeStr = inputData.getString(KEY_SYNC_TYPE) ?: SyncType.SCHEDULED.name
            val syncType = try {
                SyncType.valueOf(syncTypeStr)
            } catch (e: Exception) {
                SyncType.SCHEDULED
            }

            Log.d(TAG, "Executing sync, type=$syncType")

            val result = schedulingRepo.syncAllData(syncType)

            if (result.success) {
                Log.d(TAG, "Sync completed successfully: stocks=${result.stockCount}, analysis=${result.analysisCount}")
                Result.success()
            } else {
                Log.w(TAG, "Sync failed: ${result.errorMessage}")
                // No retry - set error flag and fail immediately
                schedulingRepo.setErrorStopped(true)
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync exception: ${e.message}", e)
            // No retry - set error flag and fail immediately
            schedulingRepo.setErrorStopped(true)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "stock_sync_periodic"
        const val WORK_NAME_ONCE = "stock_sync_once"
        const val KEY_SYNC_TYPE = "sync_type"
    }
}
