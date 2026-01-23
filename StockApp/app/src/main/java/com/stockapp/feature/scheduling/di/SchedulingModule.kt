package com.stockapp.feature.scheduling.di

import com.stockapp.feature.scheduling.data.repo.SchedulingRepoImpl
import com.stockapp.feature.scheduling.domain.repo.SchedulingRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulingModule {

    @Binds
    @Singleton
    abstract fun bindSchedulingRepo(impl: SchedulingRepoImpl): SchedulingRepo
}
