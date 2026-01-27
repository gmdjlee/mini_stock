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
 * Used for ETF constituent data collection and financial data.
 */
data class KisApiKeyConfig(
    val appKey: String = "",
    val appSecret: String = "",
    val investmentMode: InvestmentMode = InvestmentMode.MOCK
) {
    fun isValid(): Boolean = appKey.isNotBlank() && appSecret.isNotBlank()

    /**
     * Get the base URL for KIS API based on investment mode.
     */
    fun getBaseUrl(): String = when (investmentMode) {
        InvestmentMode.MOCK -> KIS_BASE_URL_MOCK
        InvestmentMode.PRODUCTION -> KIS_BASE_URL_PROD
    }

    companion object {
        const val KIS_BASE_URL_PROD = "https://openapi.koreainvestment.com:9443"
        const val KIS_BASE_URL_MOCK = "https://openapivts.koreainvestment.com:29443"
    }
}
