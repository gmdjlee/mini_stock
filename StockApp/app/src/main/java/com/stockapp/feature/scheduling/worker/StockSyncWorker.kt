package com.stockapp.feature.scheduling.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stockapp.core.py.PyError
import com.stockapp.feature.scheduling.domain.model.SyncType
import com.stockapp.feature.scheduling.domain.repo.SchedulingRepo
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "StockSyncWorker"
private const val MAX_RETRY_COUNT = 3

/**
 * Background worker for syncing stock data.
 * Uses WorkManager to schedule periodic and one-time syncs.
 *
 * Error handling strategy:
 * - Transient errors (network, timeout): Retry up to MAX_RETRY_COUNT times
 * - Permanent errors (auth, config): Set errorStopped flag and fail
 */
@HiltWorker
class StockSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val schedulingRepo: SchedulingRepo,
    private val settingsRepo: SettingsRepo
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() started, attempt=${runAttemptCount + 1}")

        // Ensure PyClient is initialized before sync.
        // When app is killed and WorkManager restarts the process,
        // PyClient may not be initialized yet.
        val initResult = settingsRepo.initializeWithSavedKeys()
        initResult.fold(
            onSuccess = { initialized ->
                if (!initialized) {
                    Log.w(TAG, "No API keys configured, cannot sync")
                    // Don't set error-stopped flag - this is a configuration issue
                    // User needs to configure API keys in Settings
                    schedulingRepo.updateLastSync(
                        System.currentTimeMillis(),
                        false,
                        "API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요."
                    )
                    return Result.failure()
                }
                Log.d(TAG, "PyClient initialized for sync")
            },
            onFailure = { e ->
                Log.e(TAG, "PyClient initialization failed: ${e.message}", e)
                // Check if this is a retriable error
                val isRetriable = isRetriableError(e)
                if (isRetriable && runAttemptCount < MAX_RETRY_COUNT) {
                    Log.d(TAG, "Initialization failed with retriable error, will retry")
                    return Result.retry()
                }
                // Permanent error - stop syncing
                schedulingRepo.setErrorStopped(true)
                schedulingRepo.updateLastSync(
                    System.currentTimeMillis(),
                    false,
                    "초기화 실패: ${e.message}"
                )
                return Result.failure()
            }
        )

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
                handleSyncFailure(result.errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync exception: ${e.message}", e)
            handleSyncFailure(e.message, e)
        }
    }

    /**
     * Handle sync failure based on error type.
     * Transient errors trigger retry, permanent errors set errorStopped.
     */
    private suspend fun handleSyncFailure(errorMessage: String?, exception: Throwable? = null): Result {
        val isRetriable = exception?.let { isRetriableError(it) }
            ?: isRetriableErrorMessage(errorMessage)

        if (isRetriable && runAttemptCount < MAX_RETRY_COUNT) {
            Log.d(TAG, "Sync failed with retriable error, will retry (attempt ${runAttemptCount + 1}/$MAX_RETRY_COUNT)")
            return Result.retry()
        }

        // Permanent error or max retries reached
        val finalMessage = if (runAttemptCount >= MAX_RETRY_COUNT) {
            "동기화 실패 (${MAX_RETRY_COUNT}회 재시도 후): $errorMessage"
        } else {
            errorMessage ?: "알 수 없는 오류"
        }

        Log.w(TAG, "Sync permanently failed: $finalMessage")
        schedulingRepo.setErrorStopped(true)
        schedulingRepo.updateLastSync(System.currentTimeMillis(), false, finalMessage)
        return Result.failure()
    }

    /**
     * Check if an exception represents a retriable error.
     */
    private fun isRetriableError(e: Throwable): Boolean {
        return when (e) {
            is PyError -> e.isRetriable
            is java.net.SocketTimeoutException,
            is java.net.UnknownHostException,
            is java.net.ConnectException -> true
            else -> isRetriableErrorMessage(e.message)
        }
    }

    /**
     * Check if an error message indicates a retriable error.
     */
    private fun isRetriableErrorMessage(message: String?): Boolean {
        if (message == null) return false
        val lowerMsg = message.lowercase()
        return lowerMsg.contains("timeout") ||
                lowerMsg.contains("시간 초과") ||
                lowerMsg.contains("network") ||
                lowerMsg.contains("네트워크") ||
                lowerMsg.contains("connection") ||
                lowerMsg.contains("연결") ||
                lowerMsg.contains("temporary") ||
                lowerMsg.contains("일시적")
    }

    companion object {
        const val WORK_NAME_PERIODIC = "stock_sync_periodic"
        const val WORK_NAME_ONCE = "stock_sync_once"
        const val KEY_SYNC_TYPE = "sync_type"
    }
}
