package com.stockapp.feature.market.di

import com.stockapp.feature.market.data.repo.MarketRepoImpl
import com.stockapp.feature.market.domain.repo.MarketRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MarketModule {

    @Binds
    @Singleton
    abstract fun bindMarketRepo(impl: MarketRepoImpl): MarketRepo
}
