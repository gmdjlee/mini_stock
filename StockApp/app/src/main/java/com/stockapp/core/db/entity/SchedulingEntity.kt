package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Scheduling configuration entity.
 * Stores user's scheduling preferences for stock list sync.
 */
@Entity(tableName = "scheduling_config")
data class SchedulingConfigEntity(
    @PrimaryKey
    val id: Int = 1, // Singleton - only one config
    val isEnabled: Boolean = true,
    val syncHour: Int = 1, // Default: 1 AM
    val syncMinute: Int = 0,
    val lastSyncAt: Long = 0L,
    val lastSyncStatus: String = "NEVER", // NEVER, SUCCESS, FAILED
    val lastSyncMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Stock sync history entity.
 * Tracks individual sync operations for debugging and monitoring.
 */
@Entity(
    tableName = "sync_history",
    indices = [Index(value = ["syncedAt"])]
)
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncType: String, // SCHEDULED, MANUAL
    val status: String, // SUCCESS, FAILED, IN_PROGRESS
    val stockCount: Int = 0,
    val analysisCount: Int = 0,
    val indicatorCount: Int = 0,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val syncedAt: Long = System.currentTimeMillis()
)

/**
 * Stock analysis data entity for incremental updates.
 * Stores detailed analysis data per stock with date tracking.
 */
@Entity(
    tableName = "stock_analysis_data",
    indices = [
        Index(value = ["ticker"]),
        Index(value = ["lastAnalyzedDate"])
    ]
)
data class StockAnalysisDataEntity(
    @PrimaryKey
    val ticker: String,
    val name: String,
    val market: String,
    val marketCap: Long = 0, // Latest market cap (억원)
    val foreignNet5d: Long = 0, // 5-day foreign net (억원)
    val institutionNet5d: Long = 0, // 5-day institution net (억원)
    val supplyRatio: Double = 0.0, // Supply ratio
    val signalType: String = "NEUTRAL", // STRONG_BUY, BUY, NEUTRAL, SELL, STRONG_SELL
    val lastAnalyzedDate: String = "", // Last date of analysis data (YYYY-MM-DD)
    val detailDataJson: String = "", // Full analysis JSON for detailed view
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Indicator data entity for incremental updates.
 * Stores technical indicator data per stock.
 */
@Entity(
    tableName = "indicator_data",
    primaryKeys = ["ticker", "indicatorType"],
    indices = [
        Index(value = ["ticker"]),
        Index(value = ["indicatorType"]),
        Index(value = ["lastCalculatedDate"])
    ]
)
data class IndicatorDataEntity(
    val ticker: String,
    val indicatorType: String, // TREND, ELDER, DEMARK, OSCILLATOR
    val summaryJson: String = "", // Summary data JSON
    val detailDataJson: String = "", // Full indicator JSON for detailed view
    val lastCalculatedDate: String = "", // Last date of calculated data (YYYY-MM-DD)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
