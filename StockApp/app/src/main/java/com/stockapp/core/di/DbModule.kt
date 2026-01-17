package com.stockapp.core.di

import android.content.Context
import androidx.room.Room
import com.stockapp.core.db.AppDb
import com.stockapp.core.db.dao.AnalysisCacheDao
import com.stockapp.core.db.dao.IndicatorCacheDao
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

    @Provides
    @Singleton
    fun provideAppDb(@ApplicationContext context: Context): AppDb {
        return Room.databaseBuilder(
            context,
            AppDb::class.java,
            AppDb.DB_NAME
        ).build()
    }

    @Provides
    fun provideStockDao(db: AppDb): StockDao = db.stockDao()

    @Provides
    fun provideAnalysisCacheDao(db: AppDb): AnalysisCacheDao = db.analysisCacheDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDb): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideIndicatorCacheDao(db: AppDb): IndicatorCacheDao = db.indicatorCacheDao()
}
