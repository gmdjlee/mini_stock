package com.stockapp.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stockapp.core.db.AppDb
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    /**
     * Migration from version 1 to 2: Added market and condition cache tables
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create market_cache table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS market_cache (
                    cacheKey TEXT NOT NULL PRIMARY KEY,
                    data TEXT NOT NULL,
                    days INTEGER NOT NULL,
                    cachedAt INTEGER NOT NULL
                )
            """.trimIndent())

            // Create condition_cache table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS condition_cache (
                    cacheKey TEXT NOT NULL PRIMARY KEY,
                    data TEXT NOT NULL,
                    cachedAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    /**
     * Migration from version 2 to 3: Added indexes on stocks table for faster search
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create index on name column
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stocks_name ON stocks (name)")
            // Create index on market column
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stocks_market ON stocks (market)")
        }
    }

    /**
     * Migration from version 3 to 4: Removed market and condition cache tables
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Drop market_cache table
            db.execSQL("DROP TABLE IF EXISTS market_cache")
            // Drop condition_cache table
            db.execSQL("DROP TABLE IF EXISTS condition_cache")
        }
    }

    /**
     * Migration from version 4 to 5: Added scheduling and stock data tables
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create scheduling_config table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS scheduling_config (
                    id INTEGER NOT NULL PRIMARY KEY,
                    isEnabled INTEGER NOT NULL DEFAULT 1,
                    syncHour INTEGER NOT NULL DEFAULT 1,
                    syncMinute INTEGER NOT NULL DEFAULT 0,
                    lastSyncAt INTEGER NOT NULL DEFAULT 0,
                    lastSyncStatus TEXT NOT NULL DEFAULT 'NEVER',
                    lastSyncMessage TEXT,
                    updatedAt INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // Create sync_history table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    syncType TEXT NOT NULL,
                    status TEXT NOT NULL,
                    stockCount INTEGER NOT NULL DEFAULT 0,
                    analysisCount INTEGER NOT NULL DEFAULT 0,
                    indicatorCount INTEGER NOT NULL DEFAULT 0,
                    errorMessage TEXT,
                    durationMs INTEGER NOT NULL DEFAULT 0,
                    syncedAt INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_history_syncedAt ON sync_history (syncedAt)")

            // Create stock_analysis_data table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS stock_analysis_data (
                    ticker TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    market TEXT NOT NULL,
                    marketCap INTEGER NOT NULL DEFAULT 0,
                    foreignNet5d INTEGER NOT NULL DEFAULT 0,
                    institutionNet5d INTEGER NOT NULL DEFAULT 0,
                    supplyRatio REAL NOT NULL DEFAULT 0.0,
                    signalType TEXT NOT NULL DEFAULT 'NEUTRAL',
                    lastAnalyzedDate TEXT NOT NULL DEFAULT '',
                    detailDataJson TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_analysis_data_ticker ON stock_analysis_data (ticker)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_analysis_data_lastAnalyzedDate ON stock_analysis_data (lastAnalyzedDate)")

            // Create indicator_data table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS indicator_data (
                    ticker TEXT NOT NULL,
                    indicatorType TEXT NOT NULL,
                    summaryJson TEXT NOT NULL DEFAULT '',
                    detailDataJson TEXT NOT NULL DEFAULT '',
                    lastCalculatedDate TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (ticker, indicatorType)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_indicator_data_ticker ON indicator_data (ticker)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_indicator_data_indicatorType ON indicator_data (indicatorType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_indicator_data_lastCalculatedDate ON indicator_data (lastCalculatedDate)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDb(@ApplicationContext context: Context): AppDb {
        return Room.databaseBuilder(
            context,
            AppDb::class.java,
            AppDb.DB_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, AppDb.MIGRATION_5_6, AppDb.MIGRATION_6_7, AppDb.MIGRATION_7_8)
            // Note: Removed destructive fallback to prevent silent data loss.
            // All future schema changes should have explicit migrations.
            // If migration fails, the app will crash with a clear error message,
            // prompting developers to add the required migration.
            .build()
    }

    @Provides
    fun provideStockDao(db: AppDb): StockDao = db.stockDao()

    @Provides
    fun provideAnalysisCacheDao(db: AppDb): AnalysisCacheDao = db.analysisCacheDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDb): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideIndicatorCacheDao(db: AppDb): IndicatorCacheDao = db.indicatorCacheDao()

    @Provides
    fun provideSchedulingConfigDao(db: AppDb): SchedulingConfigDao = db.schedulingConfigDao()

    @Provides
    fun provideSyncHistoryDao(db: AppDb): SyncHistoryDao = db.syncHistoryDao()

    @Provides
    fun provideStockAnalysisDataDao(db: AppDb): StockAnalysisDataDao = db.stockAnalysisDataDao()

    @Provides
    fun provideIndicatorDataDao(db: AppDb): IndicatorDataDao = db.indicatorDataDao()

    // ETF Collector DAOs (Phase 1)
    @Provides
    fun provideEtfDao(db: AppDb): EtfDao = db.etfDao()

    @Provides
    fun provideEtfConstituentDao(db: AppDb): EtfConstituentDao = db.etfConstituentDao()

    @Provides
    fun provideEtfKeywordDao(db: AppDb): EtfKeywordDao = db.etfKeywordDao()

    @Provides
    fun provideEtfCollectionHistoryDao(db: AppDb): EtfCollectionHistoryDao = db.etfCollectionHistoryDao()

    // ETF Statistics DAO (Phase 2)
    @Provides
    fun provideDailyEtfStatisticsDao(db: AppDb): DailyEtfStatisticsDao = db.dailyEtfStatisticsDao()

    // Financial data cache DAO
    @Provides
    fun provideFinancialCacheDao(db: AppDb): FinancialCacheDao = db.financialCacheDao()
}
