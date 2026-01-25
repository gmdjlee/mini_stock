package com.stockapp.feature.etf.di

import com.stockapp.feature.etf.data.repo.EtfRepositoryImpl
import com.stockapp.feature.etf.domain.repo.EtfRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EtfModule {

    @Binds
    @Singleton
    abstract fun bindEtfRepository(impl: EtfRepositoryImpl): EtfRepository
}
