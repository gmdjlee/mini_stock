package com.stockapp.feature.indicator.di

import com.stockapp.feature.indicator.data.repo.NativeIndicatorRepoImpl
import com.stockapp.feature.indicator.domain.repo.IndicatorRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IndicatorModule {

    @Binds
    @Singleton
    abstract fun bindIndicatorRepo(impl: NativeIndicatorRepoImpl): IndicatorRepo
}
