package com.stockapp.feature.search.di

import com.stockapp.feature.search.data.repo.SearchRepoSelector
import com.stockapp.feature.search.domain.repo.SearchRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Search feature.
 *
 * Uses SearchRepoSelector to enable gradual migration from Python to Kotlin.
 * The selector delegates to NativeSearchRepoImpl or SearchRepoImpl (Python)
 * based on the USE_NATIVE_SEARCH feature flag.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {

    @Binds
    @Singleton
    abstract fun bindSearchRepo(impl: SearchRepoSelector): SearchRepo
}
