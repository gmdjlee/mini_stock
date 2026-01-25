package com.stockapp.feature.settings.domain.model

/**
 * Investment mode: Production (실전) or Mock (모의).
 */
enum class InvestmentMode(val displayName: String, val description: String) {
    MOCK("모의투자", "테스트용 모의투자 환경"),
    PRODUCTION("실전투자", "실제 거래가 이루어지는 환경")
}

/**
 * Kiwoom API Key configuration data.
 */
data class ApiKeyConfig(
    val appKey: String = "",
    val secretKey: String = "",
    val investmentMode: InvestmentMode = InvestmentMode.MOCK
) {
    fun isValid(): Boolean = appKey.isNotBlank() && secretKey.isNotBlank()
}

/**
 * KIS (Korea Investment & Securities) API Key configuration data.
 * Used for ETF constituent data collection.
 */
data class KisApiKeyConfig(
    val appKey: String = "",
    val appSecret: String = ""
) {
    fun isValid(): Boolean = appKey.isNotBlank() && appSecret.isNotBlank()
}
