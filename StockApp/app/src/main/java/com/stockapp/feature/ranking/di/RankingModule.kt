package com.stockapp.feature.ranking.di

import com.stockapp.feature.ranking.data.repo.RankingRepoImpl
import com.stockapp.feature.ranking.domain.repo.RankingRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RankingModule {

    @Binds
    @Singleton
    abstract fun bindRankingRepo(impl: RankingRepoImpl): RankingRepo
}
