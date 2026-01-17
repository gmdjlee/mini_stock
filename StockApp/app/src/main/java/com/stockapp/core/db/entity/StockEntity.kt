package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stock cache entity for autocomplete and quick lookup.
 */
@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey
    val ticker: String,
    val name: String,
    val market: String,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Analysis cache entity for offline access.
 */
@Entity(tableName = "analysis_cache")
data class AnalysisCacheEntity(
    @PrimaryKey
    val ticker: String,
    val data: String,  // JSON serialized StockData
    val startDate: String,
    val endDate: String,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Search history entity for recent searches.
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ticker: String,
    val name: String,
    val searchedAt: Long = System.currentTimeMillis()
)

/**
 * Indicator cache entity.
 */
@Entity(tableName = "indicator_cache")
data class IndicatorCacheEntity(
    @PrimaryKey
    val key: String,  // format: "ticker:type:days" e.g., "005930:trend:180"
    val ticker: String,
    val type: String,  // trend, elder, demark, oscillator
    val data: String,  // JSON serialized indicator data
    val cachedAt: Long = System.currentTimeMillis()
)
