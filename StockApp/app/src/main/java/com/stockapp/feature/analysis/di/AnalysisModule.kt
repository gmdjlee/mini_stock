package com.stockapp.feature.analysis.di

import com.stockapp.feature.analysis.data.repo.AnalysisRepoImpl
import com.stockapp.feature.analysis.domain.repo.AnalysisRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisModule {

    @Binds
    @Singleton
    abstract fun bindAnalysisRepo(impl: AnalysisRepoImpl): AnalysisRepo
}
