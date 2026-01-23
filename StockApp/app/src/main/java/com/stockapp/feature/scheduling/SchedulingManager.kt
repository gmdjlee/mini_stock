package com.stockapp.feature.scheduling

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stockapp.feature.scheduling.domain.model.SyncType
import com.stockapp.feature.scheduling.worker.StockSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SchedulingManager"

/**
 * Manager for scheduling stock sync work using WorkManager.
 */
@Singleton
class SchedulingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule daily sync at the specified time.
     */
    fun scheduleDailySync(hour: Int, minute: Int) {
        Log.d(TAG, "scheduleDailySync() at $hour:$minute")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initialDelay = calculateInitialDelay(hour, minute)
        Log.d(TAG, "Initial delay: ${initialDelay / 1000 / 60} minutes")

        val inputData = Data.Builder()
            .putString(StockSyncWorker.KEY_SYNC_TYPE, SyncType.SCHEDULED.name)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<StockSyncWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            StockSyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Daily sync scheduled")
    }

    /**
     * Cancel scheduled daily sync.
     */
    fun cancelDailySync() {
        Log.d(TAG, "cancelDailySync()")
        workManager.cancelUniqueWork(StockSyncWorker.WORK_NAME_PERIODIC)
    }

    /**
     * Trigger immediate sync.
     */
    fun triggerImmediateSync() {
        Log.d(TAG, "triggerImmediateSync()")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(StockSyncWorker.KEY_SYNC_TYPE, SyncType.MANUAL.name)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<StockSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            StockSyncWorker.WORK_NAME_ONCE,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Immediate sync triggered")
    }

    /**
     * Observe sync work state.
     */
    fun observeSyncState(): Flow<SyncWorkState> {
        return workManager.getWorkInfosForUniqueWorkFlow(StockSyncWorker.WORK_NAME_ONCE)
            .map { workInfos ->
                val workInfo = workInfos.firstOrNull()
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> SyncWorkState.RUNNING
                    WorkInfo.State.SUCCEEDED -> SyncWorkState.SUCCEEDED
                    WorkInfo.State.FAILED -> SyncWorkState.FAILED
                    WorkInfo.State.CANCELLED -> SyncWorkState.CANCELLED
                    WorkInfo.State.ENQUEUED -> SyncWorkState.ENQUEUED
                    WorkInfo.State.BLOCKED -> SyncWorkState.BLOCKED
                    null -> SyncWorkState.IDLE
                }
            }
    }

    /**
     * Check if periodic sync is scheduled.
     */
    fun isPeriodicSyncScheduled(): Flow<Boolean> {
        return workManager.getWorkInfosForUniqueWorkFlow(StockSyncWorker.WORK_NAME_PERIODIC)
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            }
    }

    /**
     * Calculate initial delay to reach the target time.
     */
    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time has passed today, schedule for tomorrow
        if (target.before(now) || target == now) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}

/**
 * Sync work state enum.
 */
enum class SyncWorkState {
    IDLE,
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    BLOCKED
}
