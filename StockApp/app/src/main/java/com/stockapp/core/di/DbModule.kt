package com.stockapp.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.dao.ConditionCacheDao
import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.db.dao.MarketCacheDao
import com.stockapp.core.db.dao.SearchHistoryDao
import com.stockapp.core.db.dao.StockDao
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

    @Provides
    @Singleton
    fun provideAppDb(@ApplicationContext context: Context): AppDb {
        return Room.databaseBuilder(
            context,
            AppDb::class.java,
            AppDb.DB_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
    fun provideMarketCacheDao(db: AppDb): MarketCacheDao = db.marketCacheDao()

    @Provides
    fun provideConditionCacheDao(db: AppDb): ConditionCacheDao = db.conditionCacheDao()
}
