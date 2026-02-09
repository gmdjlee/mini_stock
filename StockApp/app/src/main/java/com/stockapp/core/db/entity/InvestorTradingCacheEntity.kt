package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Normalized investor trading cache entity.
 * Stores daily investor net buying data per stock or market-wide.
 * Shared across Analysis (per-stock) and Market (market-wide) features.
 *
 * For market-wide data, use ticker = "MARKET_KOSPI" or "MARKET_KOSDAQ".
 */
@Entity(
    tableName = "investor_trading_cache",
    primaryKeys = ["ticker", "date"],
    indices = [
        Index(value = ["ticker"]),
        Index(value = ["cachedAt"])
    ]
)
data class InvestorTradingCacheEntity(
    val ticker: String,         // Stock code or "MARKET_KOSPI" / "MARKET_KOSDAQ"
    val date: String,           // yyyyMMdd format
    val foreignNet: Long,       // Foreign net buying (원)
    val institutionNet: Long,   // Institutional net buying (원)
    val individualNet: Long,    // Individual net buying (원)
    val totalTrading: Long,     // Total trading value (원)
    val cachedAt: Long = System.currentTimeMillis()
)
