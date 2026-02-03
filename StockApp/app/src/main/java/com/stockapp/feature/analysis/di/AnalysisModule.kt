package com.stockapp.feature.analysis.di

import com.stockapp.feature.analysis.data.repo.AnalysisRepoSelector
import com.stockapp.feature.analysis.domain.repo.AnalysisRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Analysis module with selector pattern for gradual Python → Kotlin migration.
 *
 * The AnalysisRepoSelector delegates to either:
 * - NativeAnalysisRepoImpl (Kotlin) when USE_NATIVE_ANALYSIS flag is enabled
 * - AnalysisRepoImpl (Python/Chaquopy) when flag is disabled
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisModule {

    @Binds
    @Singleton
    abstract fun bindAnalysisRepo(impl: AnalysisRepoSelector): AnalysisRepo
}
