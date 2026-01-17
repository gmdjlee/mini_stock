package com.stockapp.feature.search.di

import com.stockapp.feature.search.data.repo.SearchRepoImpl
import com.stockapp.feature.search.domain.repo.SearchRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {

    @Binds
    @Singleton
    abstract fun bindSearchRepo(impl: SearchRepoImpl): SearchRepo
}
