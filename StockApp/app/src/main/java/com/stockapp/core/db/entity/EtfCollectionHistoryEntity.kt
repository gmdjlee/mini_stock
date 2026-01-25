package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ETF collection history entity.
 * Tracks ETF data collection operations for monitoring and debugging.
 */
@Entity(
    tableName = "etf_collection_history",
    indices = [Index(value = ["collectedDate"])]
)
data class EtfCollectionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val collectedDate: String,        // Collection date (YYYY-MM-DD)
    val totalEtfs: Int,               // Number of ETFs collected
    val totalConstituents: Int,       // Number of constituents collected
    val status: String,               // SUCCESS / FAILED / PARTIAL
    val errorMessage: String?,        // Error message if failed
    val startedAt: Long,              // Start timestamp
    val completedAt: Long?            // Completion timestamp
)
