package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching realtime supply data.
 * Uses shorter TTL than analysis cache (1 minute vs 24 hours)
 * because realtime data is time-sensitive.
 */
@Entity(tableName = "realtime_supply_cache")
data class RealtimeSupplyCacheEntity(
    @PrimaryKey
    val ticker: String,
    val name: String,
    val data: String,      // JSON serialized RealtimeSupplyData
    val cachedAt: Long     // Timestamp for TTL calculation
)
