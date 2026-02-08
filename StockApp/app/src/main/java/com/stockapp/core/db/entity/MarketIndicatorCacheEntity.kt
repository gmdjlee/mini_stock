package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching market indicator data.
 * Stores JSON-serialized indicator data by type and date.
 */
@Entity(tableName = "market_indicator_cache")
data class MarketIndicatorCacheEntity(
    @PrimaryKey
    val key: String,        // e.g., "fear_greed_latest", "oscillator_90d", "fund_flow_90d", "blood_90d"
    val type: String,       // "fear_greed", "oscillator", "fund_flow", "blood"
    val data: String,       // JSON serialized indicator data
    val cachedAt: Long      // Timestamp for TTL calculation
)
