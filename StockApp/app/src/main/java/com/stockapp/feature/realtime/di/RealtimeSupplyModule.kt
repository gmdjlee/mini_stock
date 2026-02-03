package com.stockapp.feature.realtime.di

import com.stockapp.feature.realtime.data.repo.RealtimeSupplyRepoSelector
import com.stockapp.feature.realtime.domain.repo.RealtimeSupplyRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Realtime supply module for dependency injection.
 *
 * Uses selector pattern for future migration flexibility.
 * Currently only Native implementation is available (no Python fallback).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeSupplyModule {

    @Binds
    @Singleton
    abstract fun bindRealtimeSupplyRepo(
        impl: RealtimeSupplyRepoSelector
    ): RealtimeSupplyRepo
}
