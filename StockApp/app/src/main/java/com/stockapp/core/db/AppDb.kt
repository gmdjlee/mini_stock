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
import com.stockapp.core.db.dao.InvestorTradingCacheDao
import com.stockapp.core.db.dao.MarketIndicatorCacheDao
import com.stockapp.core.db.dao.OhlcvCacheDao
import com.stockapp.core.db.dao.RealtimeSupplyCacheDao
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
import com.stockapp.core.db.entity.InvestorTradingCacheEntity
import com.stockapp.core.db.entity.MarketIndicatorCacheEntity
import com.stockapp.core.db.entity.OhlcvCacheEntity
import com.stockapp.core.db.entity.RealtimeSupplyCacheEntity
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
        FinancialCacheEntity::class,
        // Realtime supply cache entity (Kotlin Migration Phase 5)
        RealtimeSupplyCacheEntity::class,
        // Market indicator cache entity
        MarketIndicatorCacheEntity::class,
        // Normalized raw data cache entities (data collection optimization)
        OhlcvCacheEntity::class,
        InvestorTradingCacheEntity::class
    ],
    version = 14,
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

    // Realtime supply cache DAO (Kotlin Migration Phase 5)
    abstract fun realtimeSupplyCacheDao(): RealtimeSupplyCacheDao

    // Market indicator cache DAO
    abstract fun marketIndicatorCacheDao(): MarketIndicatorCacheDao

    // Normalized raw data cache DAOs (data collection optimization)
    abstract fun ohlcvCacheDao(): OhlcvCacheDao
    abstract fun investorTradingCacheDao(): InvestorTradingCacheDao

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

        /**
         * Migration from version 8 to 9: Add isErrorStopped column to scheduling_config
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE scheduling_config ADD COLUMN isErrorStopped INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migration from version 9 to 10: Add realtime_supply_cache table
         * For Kotlin Native Migration Phase 5 - Realtime Supply feature
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `realtime_supply_cache` (
                        `ticker` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `data` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`ticker`)
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from version 10 to 11: Add composite index (stockCode, collectedDate)
         * on etf_constituents for faster stock history queries.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_etf_constituents_stockCode_collectedDate` ON `etf_constituents` (`stockCode`, `collectedDate`)"
                )
            }
        }

        /**
         * Migration from version 11 to 12: Add market_indicator_cache table
         * For market indicator feature renewal (Fear/Greed, Oscillator, Fund Flow, Blood)
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `market_indicator_cache` (
                        `key` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `data` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from version 12 to 13: Add normalized raw data cache tables
         * - ohlcv_cache: Shared OHLCV cache across Analysis, Indicator, Market features
         * - investor_trading_cache: Shared investor trading cache across Analysis, Market features
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create ohlcv_cache table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ohlcv_cache` (
                        `ticker` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `open` INTEGER NOT NULL,
                        `high` INTEGER NOT NULL,
                        `low` INTEGER NOT NULL,
                        `close` INTEGER NOT NULL,
                        `volume` INTEGER NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`ticker`, `date`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ohlcv_cache_ticker` ON `ohlcv_cache` (`ticker`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ohlcv_cache_cachedAt` ON `ohlcv_cache` (`cachedAt`)")

                // Create investor_trading_cache table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `investor_trading_cache` (
                        `ticker` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `foreignNet` INTEGER NOT NULL,
                        `institutionNet` INTEGER NOT NULL,
                        `individualNet` INTEGER NOT NULL,
                        `totalTrading` INTEGER NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`ticker`, `date`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investor_trading_cache_ticker` ON `investor_trading_cache` (`ticker`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investor_trading_cache_cachedAt` ON `investor_trading_cache` (`cachedAt`)")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Clear investor trading cache: unit changed from 원 to 백만원
                db.execSQL("DELETE FROM investor_trading_cache")
            }
        }
    }
}
