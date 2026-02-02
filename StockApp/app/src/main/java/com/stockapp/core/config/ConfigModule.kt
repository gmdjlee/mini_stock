package com.stockapp.core.config

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for configuration-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConfigModule {

    /**
     * Binds [FeatureFlagRepoImpl] to [FeatureFlagRepo] interface.
     */
    @Binds
    @Singleton
    abstract fun bindFeatureFlagRepo(impl: FeatureFlagRepoImpl): FeatureFlagRepo
}
