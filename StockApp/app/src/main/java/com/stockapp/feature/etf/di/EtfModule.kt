package com.stockapp.feature.etf.di

import com.stockapp.core.api.KisApiClient
import com.stockapp.core.api.KiwoomApiClient
import com.stockapp.core.db.dao.EtfCollectionHistoryDao
import com.stockapp.core.db.dao.EtfConstituentDao
import com.stockapp.core.db.dao.EtfDao
import com.stockapp.core.db.dao.EtfKeywordDao
import com.stockapp.feature.etf.data.repo.EtfCollectorRepoImpl
import com.stockapp.feature.etf.data.repo.EtfRepositoryImpl
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import com.stockapp.feature.etf.domain.repo.EtfRepository
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EtfModule {

    @Binds
    @Singleton
    abstract fun bindEtfRepository(impl: EtfRepositoryImpl): EtfRepository

    companion object {
        @Provides
        @Singleton
        fun provideEtfCollectorRepo(
            kiwoomApiClient: KiwoomApiClient,
            kisApiClient: KisApiClient,
            settingsRepo: SettingsRepo,
            etfDao: EtfDao,
            constituentDao: EtfConstituentDao,
            keywordDao: EtfKeywordDao,
            historyDao: EtfCollectionHistoryDao,
            json: Json
        ): EtfCollectorRepo {
            return EtfCollectorRepoImpl(
                kiwoomApiClient = kiwoomApiClient,
                kisApiClient = kisApiClient,
                settingsRepo = settingsRepo,
                etfDao = etfDao,
                constituentDao = constituentDao,
                keywordDao = keywordDao,
                historyDao = historyDao,
                json = json
            )
        }
    }
}
