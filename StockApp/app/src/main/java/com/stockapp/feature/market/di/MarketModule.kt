package com.stockapp.feature.market.di

import com.stockapp.core.db.dao.MarketCacheDao
import com.stockapp.core.py.PyClient
import com.stockapp.feature.market.data.repo.MarketRepoImpl
import com.stockapp.feature.market.domain.repo.MarketRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MarketModule {

    @Provides
    @Singleton
    fun provideMarketRepo(
        pyClient: PyClient,
        marketCacheDao: MarketCacheDao
    ): MarketRepo {
        return MarketRepoImpl(pyClient, marketCacheDao)
    }
}
