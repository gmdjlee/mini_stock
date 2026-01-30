package com.stockapp.core.backup

import kotlinx.serialization.Serializable

/**
 * Backup file format version.
 * Increment when making breaking changes to the backup format.
 */
const val BACKUP_FORMAT_VERSION = 1

/**
 * Backup type enum.
 */
@Serializable
enum class BackupType {
    FULL,      // Full database backup
    FILTERED   // Date-filtered backup
}

/**
 * Restore mode enum.
 */
@Serializable
enum class RestoreMode {
    MERGE,    // Add to existing data, update conflicts
    REPLACE   // Clear existing data, then insert
}

/**
 * Backup metadata containing version and summary information.
 */
@Serializable
data class BackupMetadata(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val appVersion: String = "",
    val dbVersion: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val backupType: BackupType = BackupType.FULL,
    val filterStartDate: String? = null,  // YYYY-MM-DD format, null for full backup
    val filterEndDate: String? = null,    // YYYY-MM-DD format, null for full backup
    val entityCounts: Map<String, Int> = emptyMap()
)

/**
 * Complete backup file structure.
 */
@Serializable
data class BackupFile(
    val metadata: BackupMetadata,
    val tables: BackupTables
)

/**
 * Container for all table backup data.
 * All fields are nullable for forward/backward compatibility.
 */
@Serializable
data class BackupTables(
    val stocks: List<StockBackup>? = null,
    val analysisCache: List<AnalysisCacheBackup>? = null,
    val searchHistory: List<SearchHistoryBackup>? = null,
    val indicatorCache: List<IndicatorCacheBackup>? = null,
    val schedulingConfig: SchedulingConfigBackup? = null,
    val syncHistory: List<SyncHistoryBackup>? = null,
    val stockAnalysisData: List<StockAnalysisDataBackup>? = null,
    val indicatorData: List<IndicatorDataBackup>? = null,
    val etfs: List<EtfBackup>? = null,
    val etfConstituents: List<EtfConstituentBackup>? = null,
    val etfKeywords: List<EtfKeywordBackup>? = null,
    val etfCollectionHistory: List<EtfCollectionHistoryBackup>? = null,
    val dailyEtfStatistics: List<DailyEtfStatisticsBackup>? = null,
    val financialCache: List<FinancialCacheBackup>? = null
)

// ============================================================
// Backup data classes for each entity
// These mirror the Room entities but are decoupled for versioning
// ============================================================

@Serializable
data class StockBackup(
    val ticker: String = "",
    val name: String = "",
    val market: String = "",
    val updatedAt: Long = 0L
)

@Serializable
data class AnalysisCacheBackup(
    val ticker: String = "",
    val data: String = "",  // JSON string
    val startDate: String = "",
    val endDate: String = "",
    val cachedAt: Long = 0L
)

@Serializable
data class SearchHistoryBackup(
    val id: Int = 0,
    val ticker: String = "",
    val name: String = "",
    val searchedAt: Long = 0L
)

@Serializable
data class IndicatorCacheBackup(
    val key: String = "",
    val ticker: String = "",
    val type: String = "",
    val data: String = "",  // JSON string
    val cachedAt: Long = 0L
)

@Serializable
data class SchedulingConfigBackup(
    val id: Int = 1,
    val isEnabled: Boolean = true,
    val syncHour: Int = 1,
    val syncMinute: Int = 0,
    val lastSyncAt: Long = 0L,
    val lastSyncStatus: String = "NEVER",
    val lastSyncMessage: String? = null,
    val isErrorStopped: Boolean = false,
    val updatedAt: Long = 0L
)

@Serializable
data class SyncHistoryBackup(
    val id: Long = 0,
    val syncType: String = "",
    val status: String = "",
    val stockCount: Int = 0,
    val analysisCount: Int = 0,
    val indicatorCount: Int = 0,
    val etfCount: Int = 0,
    val etfConstituentCount: Int = 0,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val syncedAt: Long = 0L
)

@Serializable
data class StockAnalysisDataBackup(
    val ticker: String = "",
    val name: String = "",
    val market: String = "",
    val marketCap: Long = 0,
    val foreignNet5d: Long = 0,
    val institutionNet5d: Long = 0,
    val supplyRatio: Double = 0.0,
    val signalType: String = "NEUTRAL",
    val lastAnalyzedDate: String = "",
    val detailDataJson: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class IndicatorDataBackup(
    val ticker: String = "",
    val indicatorType: String = "",
    val summaryJson: String = "",
    val detailDataJson: String = "",
    val lastCalculatedDate: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class EtfBackup(
    val etfCode: String = "",
    val etfName: String = "",
    val etfType: String = "",
    val managementCompany: String = "",
    val trackingIndex: String = "",
    val assetClass: String = "",
    val totalAssets: Double = 0.0,
    val isFiltered: Boolean = false,
    val updatedAt: Long = 0L
)

@Serializable
data class EtfConstituentBackup(
    val etfCode: String = "",
    val etfName: String = "",
    val stockCode: String = "",
    val stockName: String = "",
    val currentPrice: Int = 0,
    val priceChange: Int = 0,
    val priceChangeSign: String = "",
    val priceChangeRate: Double = 0.0,
    val volume: Long = 0,
    val tradingValue: Long = 0,
    val marketCap: Long = 0,
    val weight: Double = 0.0,
    val evaluationAmount: Long = 0,
    val collectedDate: String = "",
    val collectedAt: Long = 0L
)

@Serializable
data class EtfKeywordBackup(
    val id: Long = 0,
    val keyword: String = "",
    val filterType: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = 0L
)

@Serializable
data class EtfCollectionHistoryBackup(
    val id: Long = 0,
    val collectedDate: String = "",
    val totalEtfs: Int = 0,
    val totalConstituents: Int = 0,
    val status: String = "",
    val errorMessage: String? = null,
    val startedAt: Long = 0L,
    val completedAt: Long? = null
)

@Serializable
data class DailyEtfStatisticsBackup(
    val date: String = "",
    val newStockCount: Int = 0,
    val newStockAmount: Long = 0,
    val removedStockCount: Int = 0,
    val removedStockAmount: Long = 0,
    val increasedStockCount: Int = 0,
    val increasedStockAmount: Long = 0,
    val decreasedStockCount: Int = 0,
    val decreasedStockAmount: Long = 0,
    val cashDepositAmount: Long = 0,
    val cashDepositChange: Long = 0,
    val cashDepositChangeRate: Double = 0.0,
    val totalEtfCount: Int = 0,
    val totalHoldingAmount: Long = 0,
    val calculatedAt: Long = 0L
)

@Serializable
data class FinancialCacheBackup(
    val ticker: String = "",
    val name: String = "",
    val data: String = "",  // JSON string
    val cachedAt: Long = 0L
)

// ============================================================
// Backup configuration for UI
// ============================================================

/**
 * Backup configuration used when creating a backup.
 */
data class BackupConfig(
    val backupType: BackupType = BackupType.FULL,
    val startDate: String? = null,  // YYYY-MM-DD
    val endDate: String? = null     // YYYY-MM-DD
)

// ============================================================
// Progress and Result types
// ============================================================

/**
 * Backup progress states.
 */
sealed class BackupProgress {
    data class Creating(val progress: Float, val message: String) : BackupProgress()
    data object Saving : BackupProgress()
    data object Complete : BackupProgress()
}

/**
 * Restore progress states.
 */
sealed class RestoreProgress {
    data object Loading : RestoreProgress()
    data object Validating : RestoreProgress()
    data object Migrating : RestoreProgress()
    data class Restoring(val progress: Float, val message: String) : RestoreProgress()
    data object Complete : RestoreProgress()
}

/**
 * Validation result for backup files.
 */
sealed class ValidationResult {
    data class Valid(val metadata: BackupMetadata) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

/**
 * Restore operation result.
 */
data class RestoreResult(
    val success: Boolean,
    val restoredCounts: Map<String, Int> = emptyMap(),
    val skippedTables: List<String> = emptyList(),
    val errorMessage: String? = null
)
