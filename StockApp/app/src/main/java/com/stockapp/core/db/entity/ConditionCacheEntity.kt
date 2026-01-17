package com.stockapp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache entity for condition search list.
 */
@Entity(tableName = "condition_cache")
data class ConditionCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,

    @ColumnInfo(name = "data")
    val data: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long
)
