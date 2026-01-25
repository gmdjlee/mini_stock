package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ETF basic information entity.
 * Stores ETF metadata including type, management company, and filter status.
 */
@Entity(tableName = "etfs")
data class EtfEntity(
    @PrimaryKey
    val etfCode: String,              // ETF code (6 digits)
    val etfName: String,              // ETF name
    val etfType: String,              // "Active" / "Passive"
    val managementCompany: String,    // Asset management company
    val trackingIndex: String,        // Tracking index
    val assetClass: String,           // Asset class
    val totalAssets: Double,          // Total assets (억원)
    val isFiltered: Boolean,          // Whether included in filter
    val updatedAt: Long               // Last update timestamp
)
