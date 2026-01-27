package com.stockapp.feature.financial.di

import com.stockapp.feature.financial.data.repo.FinancialRepoImpl
import com.stockapp.feature.financial.domain.repo.FinancialRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FinancialModule {

    @Binds
    @Singleton
    abstract fun bindFinancialRepo(impl: FinancialRepoImpl): FinancialRepo
}
