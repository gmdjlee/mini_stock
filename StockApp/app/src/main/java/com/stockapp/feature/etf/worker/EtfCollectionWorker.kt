package com.stockapp.feature.etf.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException
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

        // Promote to foreground service for long-running multi-day collections
        val startDateStr = inputData.getString(KEY_START_DATE)
        if (startDateStr != null) {
            try {
                setForeground(createForegroundInfo("ETF 데이터 수집 준비 중..."))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set foreground: ${e.message}")
            }
        }

        // Get collection type from input data
        val collectionTypeStr = inputData.getString(KEY_COLLECTION_TYPE) ?: EtfCollectionType.SCHEDULED.name
        val collectionType = try {
            EtfCollectionType.valueOf(collectionTypeStr)
        } catch (e: Exception) {
            EtfCollectionType.SCHEDULED
        }

        // Get filter config from input data
        val activeOnly = inputData.getBoolean(KEY_ACTIVE_ONLY, false)
        val includeKeywords = inputData.getStringArray(KEY_INCLUDE_KEYWORDS)?.toList() ?: emptyList()
        val excludeKeywords = inputData.getStringArray(KEY_EXCLUDE_KEYWORDS)?.toList()
            ?: listOf("레버리지", "인버스", "2X", "3X")

        val filterConfig = EtfFilterConfig(
            activeOnly = activeOnly,
            includeKeywords = includeKeywords,
            excludeKeywords = excludeKeywords
        )

        Log.d(TAG, "Executing ETF collection, type=$collectionType, startDate=$startDateStr, filterConfig=$filterConfig")

        return try {
            if (startDateStr != null) {
                // Multi-day collection
                val startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                var lastDayIndex = 0
                var lastDayTotal = 0
                val multiResult = collectAllEtfDataUC.invokeWithDateRange(
                    startDate = startDate,
                    filterConfig = filterConfig,
                    cleanupDays = CLEANUP_DAYS,
                    dayProgressCallback = { dayIndex, totalDays, dateStr ->
                        Log.d(TAG, "Day progress: $dayIndex/$totalDays ($dateStr)")
                        lastDayIndex = dayIndex
                        lastDayTotal = totalDays
                        setProgressAsync(workDataOf(
                            KEY_PROGRESS_DAY_CURRENT to dayIndex,
                            KEY_PROGRESS_DAY_TOTAL to totalDays,
                            KEY_PROGRESS_DAY_DATE to dateStr
                        ))
                        // Update foreground notification
                        try {
                            setForegroundAsync(createForegroundInfo(
                                "날짜 $dayIndex/$totalDays ($dateStr)"
                            ))
                        } catch (_: Exception) {}
                    },
                    etfProgressCallback = { current, total ->
                        // Throttle: report every 5 ETFs or at first/last
                        if (current == 1 || current == total || current % PROGRESS_THROTTLE == 0) {
                            setProgressAsync(workDataOf(
                                KEY_PROGRESS_CURRENT to current,
                                KEY_PROGRESS_TOTAL to total,
                                KEY_PROGRESS_DAY_CURRENT to lastDayIndex,
                                KEY_PROGRESS_DAY_TOTAL to lastDayTotal
                            ))
                        }
                    }
                )

                when (multiResult.status) {
                    CollectionStatus.SUCCESS, CollectionStatus.PARTIAL -> {
                        Log.d(TAG, "Multi-day collection: ${multiResult.successDays} success, ${multiResult.skippedDays} skipped, ${multiResult.failedDays} failed")
                        Result.success(workDataOf(
                            KEY_RESULT_ETF_COUNT to multiResult.totalEtfs,
                            KEY_RESULT_CONSTITUENT_COUNT to multiResult.totalConstituents,
                            KEY_RESULT_STATUS to multiResult.status.name,
                            KEY_RESULT_SUCCESS_DAYS to multiResult.successDays,
                            KEY_RESULT_SKIPPED_DAYS to multiResult.skippedDays,
                            KEY_RESULT_FAILED_DAYS to multiResult.failedDays,
                            KEY_RESULT_TOTAL_DAYS to multiResult.totalDays
                        ))
                    }
                    CollectionStatus.FAILED -> {
                        Log.e(TAG, "Multi-day collection failed")
                        if (runAttemptCount < MAX_RETRY_COUNT) {
                            Result.retry()
                        } else {
                            Result.failure(workDataOf(
                                KEY_RESULT_STATUS to multiResult.status.name,
                                KEY_RESULT_ERROR to "전체 날짜 수집 실패"
                            ))
                        }
                    }
                    else -> Result.retry()
                }
            } else {
                // Single-day collection (existing behavior)
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
            }
        } catch (e: CancellationException) {
            throw e
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
        const val KEY_START_DATE = "start_date" // YYYY-MM-DD format for multi-day collection

        // Progress keys (ETF-level within a day)
        const val KEY_PROGRESS_CURRENT = "progress_current"
        const val KEY_PROGRESS_TOTAL = "progress_total"

        // Progress keys (day-level for multi-day collection)
        const val KEY_PROGRESS_DAY_CURRENT = "progress_day_current"
        const val KEY_PROGRESS_DAY_TOTAL = "progress_day_total"
        const val KEY_PROGRESS_DAY_DATE = "progress_day_date"

        // Result keys
        const val KEY_RESULT_ETF_COUNT = "result_etf_count"
        const val KEY_RESULT_CONSTITUENT_COUNT = "result_constituent_count"
        const val KEY_RESULT_STATUS = "result_status"
        const val KEY_RESULT_ERROR = "result_error"
        const val KEY_RESULT_SUCCESS_DAYS = "result_success_days"
        const val KEY_RESULT_SKIPPED_DAYS = "result_skipped_days"
        const val KEY_RESULT_FAILED_DAYS = "result_failed_days"
        const val KEY_RESULT_TOTAL_DAYS = "result_total_days"

        private const val MAX_RETRY_COUNT = 3
        private const val CLEANUP_DAYS = 30
        private const val PROGRESS_THROTTLE = 5 // Report progress every N ETFs
        private const val NOTIFICATION_CHANNEL_ID = "etf_collection"
        private const val NOTIFICATION_ID = 1001

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
         * @param startDate Optional start date for multi-day collection (YYYY-MM-DD).
         *                  If null, collects today only. If set, collects from startDate to today.
         */
        fun collectNow(
            context: Context,
            filterConfig: EtfFilterConfig = EtfFilterConfig(),
            startDate: String? = null
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(
                KEY_COLLECTION_TYPE to EtfCollectionType.MANUAL.name,
                KEY_ACTIVE_ONLY to filterConfig.activeOnly,
                KEY_INCLUDE_KEYWORDS to filterConfig.includeKeywords.toTypedArray(),
                KEY_EXCLUDE_KEYWORDS to filterConfig.excludeKeywords.toTypedArray(),
                KEY_START_DATE to startDate
            )

            val workRequest = OneTimeWorkRequestBuilder<EtfCollectionWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONCE,
                ExistingWorkPolicy.KEEP,
                workRequest
            )

            Log.d(TAG, "Started one-time collection, startDate=$startDate")
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

    private var channelCreated = false

    private fun createForegroundInfo(progressText: String): ForegroundInfo {
        val context = applicationContext

        // Create notification channel once (required for Android 8.0+)
        if (!channelCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "ETF 데이터 수집",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ETF 구성종목 데이터 수집 진행 상태"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            channelCreated = true
        }

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ETF 데이터 수집 중")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
