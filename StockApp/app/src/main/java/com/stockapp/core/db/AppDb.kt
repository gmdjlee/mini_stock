package com.stockapp.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.db.dao.IndicatorDataDao
import com.stockapp.core.db.dao.SchedulingConfigDao
import com.stockapp.core.db.dao.SearchHistoryDao
import com.stockapp.core.db.dao.StockAnalysisDataDao
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.dao.SyncHistoryDao
import com.stockapp.core.db.entity.AnalysisCacheEntity
import com.stockapp.core.db.entity.IndicatorCacheEntity
import com.stockapp.core.db.entity.IndicatorDataEntity
import com.stockapp.core.db.entity.SchedulingConfigEntity
import com.stockapp.core.db.entity.SearchHistoryEntity
import com.stockapp.core.db.entity.StockAnalysisDataEntity
import com.stockapp.core.db.entity.StockEntity
import com.stockapp.core.db.entity.SyncHistoryEntity

@Database(
    entities = [
        StockEntity::class,
        AnalysisCacheEntity::class,
        SearchHistoryEntity::class,
        IndicatorCacheEntity::class,
        SchedulingConfigEntity::class,
        SyncHistoryEntity::class,
        StockAnalysisDataEntity::class,
        IndicatorDataEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun analysisCacheDao(): AnalysisCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun indicatorCacheDao(): IndicatorCacheDao
    abstract fun schedulingConfigDao(): SchedulingConfigDao
    abstract fun syncHistoryDao(): SyncHistoryDao
    abstract fun stockAnalysisDataDao(): StockAnalysisDataDao
    abstract fun indicatorDataDao(): IndicatorDataDao

    companion object {
        const val DB_NAME = "stock_app.db"

        // Cache TTL constants (milliseconds)
        const val STOCK_CACHE_TTL = 24 * 60 * 60 * 1000L  // 24 hours
        const val ANALYSIS_CACHE_TTL = 24 * 60 * 60 * 1000L  // 24 hours
        const val INDICATOR_CACHE_TTL = 24 * 60 * 60 * 1000L  // 24 hours
        const val MAX_HISTORY_COUNT = 50
    }
}
