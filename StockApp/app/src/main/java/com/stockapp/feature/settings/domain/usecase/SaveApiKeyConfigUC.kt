package com.stockapp.feature.settings.domain.usecase

import com.stockapp.feature.settings.domain.model.ApiKeyConfig
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import javax.inject.Inject

/**
 * Use case to save API key configuration.
 */
class SaveApiKeyConfigUC @Inject constructor(
    private val repo: SettingsRepo
) {
    suspend operator fun invoke(config: ApiKeyConfig) = repo.saveApiKeyConfig(config)
}
