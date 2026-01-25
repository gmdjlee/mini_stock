package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ETF filter keyword entity.
 * Stores include/exclude keywords for ETF filtering.
 */
@Entity(tableName = "etf_keywords")
data class EtfKeywordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,              // Keyword text
    val filterType: String,           // "INCLUDE" / "EXCLUDE"
    val isEnabled: Boolean = true,    // Whether enabled
    val createdAt: Long               // Creation timestamp
)
