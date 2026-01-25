package com.stockapp.feature.scheduling.domain.model

/**
 * Scheduling configuration domain model.
 */
data class SchedulingConfig(
    val isEnabled: Boolean = true,
    val syncHour: Int = 1,
    val syncMinute: Int = 0,
    val lastSyncAt: Long = 0L,
    val lastSyncStatus: SyncStatus = SyncStatus.NEVER,
    val lastSyncMessage: String? = null
) {
    val syncTimeDisplay: String
        get() = String.format("%02d:%02d", syncHour, syncMinute)
}

/**
 * Sync status enum.
 */
enum class SyncStatus {
    NEVER,
    SUCCESS,
    FAILED,
    IN_PROGRESS;

    companion object {
        fun fromString(value: String): SyncStatus = try {
            valueOf(value)
        } catch (e: Exception) {
            NEVER
        }
    }
}

/**
 * Sync type enum.
 */
enum class SyncType {
    SCHEDULED,
    MANUAL
}

/**
 * Sync history domain model.
 */
data class SyncHistory(
    val id: Long = 0,
    val syncType: SyncType,
    val status: SyncStatus,
    val stockCount: Int = 0,
    val analysisCount: Int = 0,
    val indicatorCount: Int = 0,
    val etfCount: Int = 0,
    val etfConstituentCount: Int = 0,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val syncedAt: Long = System.currentTimeMillis()
) {
    val durationDisplay: String
        get() {
            val seconds = durationMs / 1000
            return if (seconds < 60) {
                "${seconds}초"
            } else {
                "${seconds / 60}분 ${seconds % 60}초"
            }
        }
}

/**
 * Sync result for a single sync operation.
 */
data class SyncResult(
    val success: Boolean,
    val stockCount: Int = 0,
    val analysisCount: Int = 0,
    val indicatorCount: Int = 0,
    val etfCount: Int = 0,
    val etfConstituentCount: Int = 0,
    val errorMessage: String? = null,
    val durationMs: Long = 0
)
