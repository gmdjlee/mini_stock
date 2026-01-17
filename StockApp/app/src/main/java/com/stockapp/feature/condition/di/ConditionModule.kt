package com.stockapp.feature.condition.di

import com.stockapp.core.db.dao.ConditionCacheDao
import com.stockapp.core.py.PyClient
import com.stockapp.feature.condition.data.repo.ConditionRepoImpl
import com.stockapp.feature.condition.domain.repo.ConditionRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConditionModule {

    @Provides
    @Singleton
    fun provideConditionRepo(
        pyClient: PyClient,
        conditionCacheDao: ConditionCacheDao
    ): ConditionRepo {
        return ConditionRepoImpl(pyClient, conditionCacheDao)
    }
}
