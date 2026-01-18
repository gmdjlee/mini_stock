package com.stockapp.feature.settings.domain.usecase

import com.stockapp.feature.settings.domain.model.ApiKeyConfig
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get API key configuration.
 */
class GetApiKeyConfigUC @Inject constructor(
    private val repo: SettingsRepo
) {
    operator fun invoke(): Flow<ApiKeyConfig> = repo.getApiKeyConfig()
}
