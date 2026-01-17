package com.stockapp.feature.indicator.di

import com.stockapp.core.db.dao.IndicatorCacheDao
import com.stockapp.core.py.PyClient
import com.stockapp.feature.indicator.data.repo.IndicatorRepoImpl
import com.stockapp.feature.indicator.domain.repo.IndicatorRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IndicatorModule {

    @Provides
    @Singleton
    fun provideIndicatorRepo(
        pyClient: PyClient,
        indicatorCacheDao: IndicatorCacheDao
    ): IndicatorRepo {
        return IndicatorRepoImpl(pyClient, indicatorCacheDao)
    }
}
