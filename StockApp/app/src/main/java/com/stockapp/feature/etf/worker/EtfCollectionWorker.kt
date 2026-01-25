package com.stockapp.feature.etf.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.usecase.CollectAllEtfDataUC
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val TAG = "EtfCollectionWorker"

/**
 * Collection type for ETF data.
 */
enum class EtfCollectionType {
    SCHEDULED,  // Scheduled daily collection
    MANUAL      // Manual one-time collection
}

/**
 * Background worker for collecting ETF constituent data.
 * Runs daily to collect data from all filtered ETFs.
 */
@HiltWorker
class EtfCollectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val collectAllEtfDataUC: CollectAllEtfDataUC
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() started, runAttemptCount=$runAttemptCount")

        // Get collection type from input data
        val collectionTypeStr = inputData.getString(KEY_COLLECTION_TYPE) ?: EtfCollectionType.SCHEDULED.name
        val collectionType = try {
            EtfCollectionType.valueOf(collectionTypeStr)
        } catch (e: Exception) {
            EtfCollectionType.SCHEDULED
        }

        // Get filter config from input data
        val activeOnly = inputData.getBoolean(KEY_ACTIVE_ONLY, true)
        val includeKeywords = inputData.getStringArray(KEY_INCLUDE_KEYWORDS)?.toList() ?: emptyList()
        val excludeKeywords = inputData.getStringArray(KEY_EXCLUDE_KEYWORDS)?.toList()
            ?: listOf("레버리지", "인버스", "2X", "3X")

        val filterConfig = EtfFilterConfig(
            activeOnly = activeOnly,
            includeKeywords = includeKeywords,
            excludeKeywords = excludeKeywords
        )

        Log.d(TAG, "Executing ETF collection, type=$collectionType, filterConfig=$filterConfig")

        return try {
            val result = collectAllEtfDataUC(
                filterConfig = filterConfig,
                cleanupDays = CLEANUP_DAYS,
                progressCallback = { current, total ->
                    Log.d(TAG, "Progress: $current/$total")
                    setProgressAsync(workDataOf(
                        KEY_PROGRESS_CURRENT to current,
                        KEY_PROGRESS_TOTAL to total
                    ))
                }
            )

            when (result.status) {
                CollectionStatus.SUCCESS -> {
                    Log.d(TAG, "Collection completed: ${result.totalEtfs} ETFs, ${result.totalConstituents} constituents")
                    Result.success(workDataOf(
                        KEY_RESULT_ETF_COUNT to result.totalEtfs,
                        KEY_RESULT_CONSTITUENT_COUNT to result.totalConstituents,
                        KEY_RESULT_STATUS to result.status.name
                    ))
                }

                CollectionStatus.PARTIAL -> {
                    Log.w(TAG, "Collection partially succeeded: ${result.successCount} success, ${result.failedCount} failed")
                    // Consider partial success as success, but include error info
                    Result.success(workDataOf(
                        KEY_RESULT_ETF_COUNT to result.totalEtfs,
                        KEY_RESULT_CONSTITUENT_COUNT to result.totalConstituents,
                        KEY_RESULT_STATUS to result.status.name,
                        KEY_RESULT_ERROR to result.errorMessage
                    ))
                }

                CollectionStatus.FAILED -> {
                    Log.e(TAG, "Collection failed: ${result.errorMessage}")
                    if (runAttemptCount < MAX_RETRY_COUNT) {
                        Result.retry()
                    } else {
                        Result.failure(workDataOf(
                            KEY_RESULT_STATUS to result.status.name,
                            KEY_RESULT_ERROR to result.errorMessage
                        ))
                    }
                }

                CollectionStatus.IN_PROGRESS -> {
                    Log.w(TAG, "Collection still in progress, retrying...")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Collection exception: ${e.message}", e)
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure(workDataOf(
                    KEY_RESULT_ERROR to (e.message ?: "Unknown error")
                ))
            }
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "etf_collection_periodic"
        const val WORK_NAME_ONCE = "etf_collection_once"

        // Input keys
        const val KEY_COLLECTION_TYPE = "collection_type"
        const val KEY_ACTIVE_ONLY = "active_only"
        const val KEY_INCLUDE_KEYWORDS = "include_keywords"
        const val KEY_EXCLUDE_KEYWORDS = "exclude_keywords"

        // Progress keys
        const val KEY_PROGRESS_CURRENT = "progress_current"
        const val KEY_PROGRESS_TOTAL = "progress_total"

        // Result keys
        const val KEY_RESULT_ETF_COUNT = "result_etf_count"
        const val KEY_RESULT_CONSTITUENT_COUNT = "result_constituent_count"
        const val KEY_RESULT_STATUS = "result_status"
        const val KEY_RESULT_ERROR = "result_error"

        private const val MAX_RETRY_COUNT = 3
        private const val CLEANUP_DAYS = 30

        /**
         * Schedule daily ETF collection at specified hour.
         *
         * @param context Application context
         * @param hour Hour of day to run (0-23)
         * @param minute Minute of hour (0-59)
         * @param filterConfig Filter configuration
         */
        fun scheduleDailyCollection(
            context: Context,
            hour: Int = 6,
            minute: Int = 0,
            filterConfig: EtfFilterConfig = EtfFilterConfig()
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Calculate initial delay to target time
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If target time has passed today, schedule for tomorrow
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }

            val initialDelay = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<EtfCollectionWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .setInputData(workDataOf(
                    KEY_COLLECTION_TYPE to EtfCollectionType.SCHEDULED.name,
                    KEY_ACTIVE_ONLY to filterConfig.activeOnly,
                    KEY_INCLUDE_KEYWORDS to filterConfig.includeKeywords.toTypedArray(),
                    KEY_EXCLUDE_KEYWORDS to filterConfig.excludeKeywords.toTypedArray()
                ))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Log.d(TAG, "Scheduled daily collection at $hour:$minute (initial delay: ${initialDelay / 1000 / 60} minutes)")
        }

        /**
         * Run one-time ETF collection immediately.
         *
         * @param context Application context
         * @param filterConfig Filter configuration
         */
        fun collectNow(
            context: Context,
            filterConfig: EtfFilterConfig = EtfFilterConfig()
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<EtfCollectionWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInputData(workDataOf(
                    KEY_COLLECTION_TYPE to EtfCollectionType.MANUAL.name,
                    KEY_ACTIVE_ONLY to filterConfig.activeOnly,
                    KEY_INCLUDE_KEYWORDS to filterConfig.includeKeywords.toTypedArray(),
                    KEY_EXCLUDE_KEYWORDS to filterConfig.excludeKeywords.toTypedArray()
                ))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONCE,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Log.d(TAG, "Started one-time collection")
        }

        /**
         * Cancel scheduled ETF collection.
         *
         * @param context Application context
         */
        fun cancelScheduledCollection(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
            Log.d(TAG, "Cancelled scheduled collection")
        }

        /**
         * Cancel ongoing one-time collection.
         *
         * @param context Application context
         */
        fun cancelOngoingCollection(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_ONCE)
            Log.d(TAG, "Cancelled one-time collection")
        }
    }
}
