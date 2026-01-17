package com.stockapp.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.db.dao.SearchHistoryDao
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.entity.AnalysisCacheEntity
import com.stockapp.core.db.entity.IndicatorCacheEntity
import com.stockapp.core.db.entity.SearchHistoryEntity
import com.stockapp.core.db.entity.StockEntity

@Database(
    entities = [
        StockEntity::class,
        AnalysisCacheEntity::class,
        SearchHistoryEntity::class,
        IndicatorCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDb : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun analysisCacheDao(): AnalysisCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun indicatorCacheDao(): IndicatorCacheDao

    companion object {
        const val DB_NAME = "stock_app.db"

        // Cache TTL constants (milliseconds)
        const val STOCK_CACHE_TTL = 24 * 60 * 60 * 1000L  // 24 hours
        const val ANALYSIS_CACHE_TTL = 24 * 60 * 60 * 1000L  // 24 hours
        const val INDICATOR_CACHE_TTL = 24 * 60 * 60 * 1000L  // 24 hours
        const val MAX_HISTORY_COUNT = 50
    }
}
