package com.stockapp.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stockapp.core.config.AppConfig
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.dao.DailyEtfStatisticsDao
import com.stockapp.core.db.dao.EtfCollectionHistoryDao
import com.stockapp.core.db.dao.EtfConstituentDao
import com.stockapp.core.db.dao.EtfDao
import com.stockapp.core.db.dao.EtfKeywordDao
import com.stockapp.core.db.dao.FinancialCacheDao
import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.db.dao.IndicatorDataDao
import com.stockapp.core.db.dao.SchedulingConfigDao
import com.stockapp.core.db.dao.SearchHistoryDao
import com.stockapp.core.db.dao.StockAnalysisDataDao
import com.stockapp.core.db.dao.StockDao
import com.stockapp.core.db.dao.SyncHistoryDao
import com.stockapp.core.db.entity.AnalysisCacheEntity
import com.stockapp.core.db.entity.DailyEtfStatisticsEntity
import com.stockapp.core.db.entity.EtfCollectionHistoryEntity
import com.stockapp.core.db.entity.EtfConstituentEntity
import com.stockapp.core.db.entity.EtfEntity
import com.stockapp.core.db.entity.EtfKeywordEntity
import com.stockapp.core.db.entity.FinancialCacheEntity
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
        IndicatorDataEntity::class,
        // ETF Collector entities (Phase 1)
        EtfEntity::class,
        EtfConstituentEntity::class,
        EtfKeywordEntity::class,
        EtfCollectionHistoryEntity::class,
        // ETF Statistics entity (Phase 2)
        DailyEtfStatisticsEntity::class,
        // Financial data cache entity
        FinancialCacheEntity::class
    ],
    version = 8,
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

    // ETF Collector DAOs (Phase 1)
    abstract fun etfDao(): EtfDao
    abstract fun etfConstituentDao(): EtfConstituentDao
    abstract fun etfKeywordDao(): EtfKeywordDao
    abstract fun etfCollectionHistoryDao(): EtfCollectionHistoryDao

    // ETF Statistics DAO (Phase 2)
    abstract fun dailyEtfStatisticsDao(): DailyEtfStatisticsDao

    // Financial data cache DAO
    abstract fun financialCacheDao(): FinancialCacheDao

    companion object {
        const val DB_NAME = "stock_app.db"

        // Cache TTL constants - reference centralized config
        val STOCK_CACHE_TTL = AppConfig.STOCK_CACHE_TTL_MS
        val ANALYSIS_CACHE_TTL = AppConfig.ANALYSIS_CACHE_TTL_MS
        val INDICATOR_CACHE_TTL = AppConfig.INDICATOR_CACHE_TTL_MS
        val MAX_HISTORY_COUNT = AppConfig.MAX_HISTORY_COUNT

        /**
         * Migration from version 5 to 6: Add ETF Collector tables
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create etfs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `etfs` (
                        `etfCode` TEXT NOT NULL,
                        `etfName` TEXT NOT NULL,
                        `etfType` TEXT NOT NULL,
                        `managementCompany` TEXT NOT NULL,
                        `trackingIndex` TEXT NOT NULL,
                        `assetClass` TEXT NOT NULL,
                        `totalAssets` REAL NOT NULL,
                        `isFiltered` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`etfCode`)
                    )
                """.trimIndent())

                // Create etf_constituents table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `etf_constituents` (
                        `etfCode` TEXT NOT NULL,
                        `etfName` TEXT NOT NULL,
                        `stockCode` TEXT NOT NULL,
                        `stockName` TEXT NOT NULL,
                        `currentPrice` INTEGER NOT NULL,
                        `priceChange` INTEGER NOT NULL,
                        `priceChangeSign` TEXT NOT NULL,
                        `priceChangeRate` REAL NOT NULL,
                        `volume` INTEGER NOT NULL,
                        `tradingValue` INTEGER NOT NULL,
                        `marketCap` INTEGER NOT NULL,
                        `weight` REAL NOT NULL,
                        `evaluationAmount` INTEGER NOT NULL,
                        `collectedDate` TEXT NOT NULL,
                        `collectedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`etfCode`, `stockCode`, `collectedDate`)
                    )
                """.trimIndent())

                // Create indices for etf_constituents
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_etf_constituents_stockCode` ON `etf_constituents` (`stockCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_etf_constituents_collectedDate` ON `etf_constituents` (`collectedDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_etf_constituents_etfCode_collectedDate` ON `etf_constituents` (`etfCode`, `collectedDate`)")

                // Create etf_keywords table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `etf_keywords` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `keyword` TEXT NOT NULL,
                        `filterType` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create etf_collection_history table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `etf_collection_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `collectedDate` TEXT NOT NULL,
                        `totalEtfs` INTEGER NOT NULL,
                        `totalConstituents` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        `startedAt` INTEGER NOT NULL,
                        `completedAt` INTEGER
                    )
                """.trimIndent())

                // Create index for etf_collection_history
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_etf_collection_history_collectedDate` ON `etf_collection_history` (`collectedDate`)")
            }
        }

        /**
         * Migration from version 6 to 7: Add daily_etf_statistics table
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create daily_etf_statistics table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `daily_etf_statistics` (
                        `date` TEXT NOT NULL,
                        `newStockCount` INTEGER NOT NULL,
                        `newStockAmount` INTEGER NOT NULL,
                        `removedStockCount` INTEGER NOT NULL,
                        `removedStockAmount` INTEGER NOT NULL,
                        `increasedStockCount` INTEGER NOT NULL,
                        `increasedStockAmount` INTEGER NOT NULL,
                        `decreasedStockCount` INTEGER NOT NULL,
                        `decreasedStockAmount` INTEGER NOT NULL,
                        `cashDepositAmount` INTEGER NOT NULL,
                        `cashDepositChange` INTEGER NOT NULL,
                        `cashDepositChangeRate` REAL NOT NULL,
                        `totalEtfCount` INTEGER NOT NULL,
                        `totalHoldingAmount` INTEGER NOT NULL,
                        `calculatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                """.trimIndent())

                // Create unique index on date
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_etf_statistics_date` ON `daily_etf_statistics` (`date`)")
            }
        }

        /**
         * Migration from version 7 to 8: Add financial_cache table
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create financial_cache table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `financial_cache` (
                        `ticker` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `data` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`ticker`)
                    )
                """.trimIndent())
            }
        }
    }
}
