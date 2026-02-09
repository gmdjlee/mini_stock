package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Normalized OHLCV cache entity.
 * Stores individual daily bars so any date range query can be served from cache.
 * Shared across Analysis, Indicator, and Market features to eliminate redundant API calls.
 */
@Entity(
    tableName = "ohlcv_cache",
    primaryKeys = ["ticker", "date"],
    indices = [
        Index(value = ["ticker"]),
        Index(value = ["cachedAt"])
    ]
)
data class OhlcvCacheEntity(
    val ticker: String,
    val date: String,       // yyyyMMdd format
    val open: Int,
    val high: Int,
    val low: Int,
    val close: Int,
    val volume: Long,
    val cachedAt: Long = System.currentTimeMillis()
)
