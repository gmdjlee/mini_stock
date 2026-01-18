package com.stockapp.feature.settings.di

import com.stockapp.feature.settings.data.repo.SettingsRepoImpl
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepo(impl: SettingsRepoImpl): SettingsRepo
}
