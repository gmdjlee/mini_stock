package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * ETF constituent daily snapshot entity.
 * Stores constituent stock data with price, volume, weight, and evaluation amount.
 * Uses composite primary key (etfCode, stockCode, collectedDate) for daily snapshots.
 */
@Entity(
    tableName = "etf_constituents",
    primaryKeys = ["etfCode", "stockCode", "collectedDate"],
    indices = [
        Index("stockCode"),
        Index("collectedDate"),
        Index(value = ["etfCode", "collectedDate"])
    ]
)
data class EtfConstituentEntity(
    val etfCode: String,              // ETF code
    val etfName: String,              // ETF name
    val stockCode: String,            // Stock code
    val stockName: String,            // Stock name
    val currentPrice: Int,            // Current price
    val priceChange: Int,             // Price change from previous day
    val priceChangeSign: String,      // Sign (1-5: 상한/상승/보합/하락/하한)
    val priceChangeRate: Double,      // Change rate (%)
    val volume: Long,                 // Trading volume
    val tradingValue: Long,           // Trading value
    val marketCap: Long,              // Market capitalization
    val weight: Double,               // Weight (%)
    val evaluationAmount: Long,       // Evaluation amount
    val collectedDate: String,        // Collection date (YYYY-MM-DD)
    val collectedAt: Long             // Collection timestamp
)
