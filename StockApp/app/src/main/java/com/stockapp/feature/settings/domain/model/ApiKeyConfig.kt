package com.stockapp.feature.settings.domain.model

/**
 * API Key configuration data.
 */
data class ApiKeyConfig(
    val appKey: String = "",
    val secretKey: String = ""
) {
    fun isValid(): Boolean = appKey.isNotBlank() && secretKey.isNotBlank()
}
