package com.stockapp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache entity for market indicators data.
 */
@Entity(tableName = "market_cache")
data class MarketCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,

    @ColumnInfo(name = "data")
    val data: String,

    @ColumnInfo(name = "days")
    val days: Int,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long
)
