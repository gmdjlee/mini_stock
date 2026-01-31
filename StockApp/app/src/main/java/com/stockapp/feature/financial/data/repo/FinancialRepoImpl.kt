package com.stockapp.feature.financial.data.repo

import android.util.Log
import com.stockapp.core.config.AppConfig
import com.stockapp.core.db.dao.FinancialCacheDao
import com.stockapp.core.db.entity.FinancialCacheEntity
import com.stockapp.core.di.IoDispatcher
import com.stockapp.feature.financial.data.dto.BalanceSheetDto
import com.stockapp.feature.financial.data.dto.GrowthRatiosDto
import com.stockapp.feature.financial.data.dto.IncomeStatementDto
import com.stockapp.feature.financial.data.dto.KisApiResponse
import com.stockapp.feature.financial.data.dto.ProfitabilityRatiosDto
import com.stockapp.feature.financial.data.dto.StabilityRatiosDto
import com.stockapp.feature.financial.domain.model.BalanceSheet
import com.stockapp.feature.financial.domain.model.FinancialData
import com.stockapp.feature.financial.domain.model.FinancialDataCache
import com.stockapp.feature.financial.domain.model.GrowthRatios
import com.stockapp.feature.financial.domain.model.IncomeStatement
import com.stockapp.feature.financial.domain.model.ProfitabilityRatios
import com.stockapp.feature.financial.domain.model.StabilityRatios
import com.stockapp.feature.financial.domain.model.toCache
import com.stockapp.feature.financial.domain.model.toData
import com.stockapp.feature.financial.domain.repo.FinancialRepo
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FinancialRepoImpl"

/**
 * Data class for KIS API configuration.
 */
private data class KisApiConfig(
    val appKey: String,
    val appSecret: String,
    val baseUrl: String,
    val accessToken: String? = null
)

/**
 * Repository implementation for financial data.
 * Fetches data from KIS (Korea Investment & Securities) API.
 */
@Singleton
class FinancialRepoImpl @Inject constructor(
    private val financialCacheDao: FinancialCacheDao,
    private val settingsRepo: SettingsRepo,
    private val json: Json,
    private val httpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FinancialRepo {

    // Token cache with mutex for thread safety (P0 fix: TOCTOU race condition)
    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0
    private var tokenBaseUrl: String? = null
    private val tokenMutex = Mutex()

    override suspend fun getFinancialData(
        ticker: String,
        name: String,
        useCache: Boolean
    ): Result<FinancialData> {
        // Check cache first
        if (useCache) {
            val cached = financialCacheDao.get(ticker)
            if (cached != null && !isCacheExpired(cached.cachedAt)) {
                return try {
                    val cacheData = json.decodeFromString<FinancialDataCache>(cached.data)
                    Result.success(cacheData.toData().copy(name = name))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse cached data for $ticker, fetching from API", e)
                    refreshFinancialData(ticker, name)
                }
            }
        }

        return refreshFinancialData(ticker, name)
    }

    override suspend fun refreshFinancialData(
        ticker: String,
        name: String
    ): Result<FinancialData> = withContext(ioDispatcher) {
        try {
            val config = getKisApiConfig()

            // Fetch all financial data in parallel
            val (balanceSheets, incomeStatements, profitRatios, stabilityRatios, growthRatios) =
                coroutineScope {
                    val balanceSheetDeferred = async { fetchBalanceSheet(ticker, config) }
                    val incomeStatementDeferred = async { fetchIncomeStatement(ticker, config) }
                    val profitRatioDeferred = async { fetchProfitabilityRatios(ticker, config) }
                    val stabilityRatioDeferred = async { fetchStabilityRatios(ticker, config) }
                    val growthRatioDeferred = async { fetchGrowthRatios(ticker, config) }

                    FetchResults(
                        balanceSheetDeferred.await(),
                        incomeStatementDeferred.await(),
                        profitRatioDeferred.await(),
                        stabilityRatioDeferred.await(),
                        growthRatioDeferred.await()
                    )
                }

            // Merge data by settlement year-month
            val data = mergeFinancialData(
                ticker, name,
                balanceSheets, incomeStatements,
                profitRatios, stabilityRatios, growthRatios
            )

            // Save to cache
            val cacheEntity = FinancialCacheEntity(
                ticker = ticker,
                name = name,
                data = json.encodeToString(FinancialDataCache.serializer(), data.toCache())
            )
            financialCacheDao.insert(cacheEntity)

            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch financial data for $ticker", e)
            Result.failure(e)
        }
    }

    override suspend fun clearCache(ticker: String) {
        financialCacheDao.delete(ticker)
    }

    override suspend fun clearExpiredCache() {
        val threshold = System.currentTimeMillis() - AppConfig.FINANCIAL_CACHE_TTL_MS
        financialCacheDao.deleteExpired(threshold)
    }

    private fun isCacheExpired(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt > AppConfig.FINANCIAL_CACHE_TTL_MS
    }

    private suspend fun getKisApiConfig(): KisApiConfig {
        // Use KIS API keys, not Kiwoom API keys
        val config = settingsRepo.getKisApiKeyConfig().first()
        if (!config.isValid()) {
            throw IllegalStateException("KIS API key not configured. 설정에서 KIS API 키를 입력해주세요.")
        }

        val baseUrl = config.getBaseUrl()

        // Get or refresh token
        val token = getAccessToken(config.appKey, config.appSecret, baseUrl)

        return KisApiConfig(
            appKey = config.appKey,
            appSecret = config.appSecret,
            baseUrl = baseUrl,
            accessToken = token
        )
    }

    private suspend fun getAccessToken(appKey: String, appSecret: String, baseUrl: String): String {
        // P0 fix: Use mutex to prevent TOCTOU race condition
        return tokenMutex.withLock {
            // Check if cached token is still valid AND for the same baseUrl
            // Token must be invalidated when investment mode (baseUrl) changes
            val cached = cachedToken
            if (cached != null &&
                tokenBaseUrl == baseUrl &&
                System.currentTimeMillis() < tokenExpiresAt - 60_000
            ) {
                return@withLock cached
            }

            withContext(ioDispatcher) {
                val tokenUrl = "$baseUrl/oauth2/tokenP"
                val requestBody = json.encodeToString(
                    kotlinx.serialization.serializer(),
                    mapOf(
                        "grant_type" to "client_credentials",
                        "appkey" to appKey,
                        "appsecret" to appSecret
                    )
                )

                val request = Request.Builder()
                    .url(tokenUrl)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: throw Exception("Empty token response")

                if (!response.isSuccessful) {
                    throw Exception("Token request failed: ${response.code} - $responseBody")
                }

                // Parse token response
                val tokenResponse = json.decodeFromString<Map<String, String>>(responseBody)
                val token = tokenResponse["access_token"] ?: throw Exception("No access_token in response")

                // Cache token (expires in 24 hours typically)
                cachedToken = token
                tokenExpiresAt = System.currentTimeMillis() + 23 * 60 * 60 * 1000 // 23 hours
                tokenBaseUrl = baseUrl

                token
            }
        }
    }

    /**
     * Generic fetch function for financial data.
     * Extracts common fetch pattern to reduce code duplication (P2 fix).
     *
     * @param D DTO type (e.g., BalanceSheetDto)
     * @param T Domain type (e.g., BalanceSheet)
     * @param ticker Stock ticker
     * @param config KIS API configuration
     * @param endpoint API endpoint path
     * @param trId Transaction ID
     * @param dataTypeLabel Label for logging
     * @param mapper Function to convert DTO to domain model
     */
    private suspend inline fun <reified D, T> fetchFinancialData(
        ticker: String,
        config: KisApiConfig,
        endpoint: String,
        trId: String,
        dataTypeLabel: String,
        crossinline mapper: (D) -> T?
    ): List<T> = withContext(ioDispatcher) {
        try {
            val response = callKisApi<List<D>>(
                config = config,
                endpoint = endpoint,
                trId = trId,
                params = mapOf(
                    "FID_DIV_CLS_CODE" to "1",
                    "FID_COND_MRKT_DIV_CODE" to "J",
                    "FID_INPUT_ISCD" to ticker
                )
            )
            response.mapNotNull { mapper(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch $dataTypeLabel for $ticker", e)
            emptyList()
        }
    }

    private suspend fun fetchBalanceSheet(
        ticker: String,
        config: KisApiConfig
    ): List<BalanceSheet> = fetchFinancialData<BalanceSheetDto, BalanceSheet>(
        ticker = ticker,
        config = config,
        endpoint = "/uapi/domestic-stock/v1/finance/balance-sheet",
        trId = TR_ID_BALANCE_SHEET,
        dataTypeLabel = "balance sheet"
    ) { it.toDomain() }

    private suspend fun fetchIncomeStatement(
        ticker: String,
        config: KisApiConfig
    ): List<IncomeStatement> = fetchFinancialData<IncomeStatementDto, IncomeStatement>(
        ticker = ticker,
        config = config,
        endpoint = "/uapi/domestic-stock/v1/finance/income-statement",
        trId = TR_ID_INCOME_STATEMENT,
        dataTypeLabel = "income statement"
    ) { it.toDomain() }

    private suspend fun fetchProfitabilityRatios(
        ticker: String,
        config: KisApiConfig
    ): List<ProfitabilityRatios> = fetchFinancialData<ProfitabilityRatiosDto, ProfitabilityRatios>(
        ticker = ticker,
        config = config,
        endpoint = "/uapi/domestic-stock/v1/finance/profit-ratio",
        trId = TR_ID_PROFIT_RATIO,
        dataTypeLabel = "profitability ratios"
    ) { it.toDomain() }

    private suspend fun fetchStabilityRatios(
        ticker: String,
        config: KisApiConfig
    ): List<StabilityRatios> = fetchFinancialData<StabilityRatiosDto, StabilityRatios>(
        ticker = ticker,
        config = config,
        endpoint = "/uapi/domestic-stock/v1/finance/stability-ratio",
        trId = TR_ID_STABILITY_RATIO,
        dataTypeLabel = "stability ratios"
    ) { it.toDomain() }

    private suspend fun fetchGrowthRatios(
        ticker: String,
        config: KisApiConfig
    ): List<GrowthRatios> = fetchFinancialData<GrowthRatiosDto, GrowthRatios>(
        ticker = ticker,
        config = config,
        endpoint = "/uapi/domestic-stock/v1/finance/growth-ratio",
        trId = TR_ID_GROWTH_RATIO,
        dataTypeLabel = "growth ratios"
    ) { it.toDomain() }

    private inline fun <reified T> callKisApi(
        config: KisApiConfig,
        endpoint: String,
        trId: String,
        params: Map<String, String>
    ): T {
        val urlBuilder = StringBuilder(config.baseUrl).append(endpoint)
        if (params.isNotEmpty()) {
            urlBuilder.append("?")
            urlBuilder.append(params.entries.joinToString("&") { "${it.key}=${it.value}" })
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .get()
            .addHeader("content-type", "application/json; charset=utf-8")
            .addHeader("authorization", "Bearer ${config.accessToken}")
            .addHeader("appkey", config.appKey)
            .addHeader("appsecret", config.appSecret)
            .addHeader("tr_id", trId)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("API call failed: ${response.code} - $responseBody")
        }

        // Parse response
        val apiResponse = json.decodeFromString<KisApiResponse<T>>(responseBody)
        if (apiResponse.rtCd != "0") {
            throw Exception("API error: ${apiResponse.msgCd} - ${apiResponse.msg1}")
        }

        // Use actualOutput to handle both "output" and "output1" field names
        return apiResponse.actualOutput ?: throw Exception("No output in response")
    }

    private fun mergeFinancialData(
        ticker: String,
        name: String,
        balanceSheets: List<BalanceSheet>,
        incomeStatements: List<IncomeStatement>,
        profitRatios: List<ProfitabilityRatios>,
        stabilityRatios: List<StabilityRatios>,
        growthRatios: List<GrowthRatios>
    ): FinancialData {
        // Collect all periods
        val allPeriods = mutableSetOf<String>()
        balanceSheets.forEach { allPeriods.add(it.period.yearMonth) }
        incomeStatements.forEach { allPeriods.add(it.period.yearMonth) }
        profitRatios.forEach { allPeriods.add(it.period.yearMonth) }
        stabilityRatios.forEach { allPeriods.add(it.period.yearMonth) }
        growthRatios.forEach { allPeriods.add(it.period.yearMonth) }

        return FinancialData(
            ticker = ticker,
            name = name,
            periods = allPeriods.sorted(),
            balanceSheets = balanceSheets.associateBy { it.period.yearMonth },
            incomeStatements = incomeStatements.associateBy { it.period.yearMonth },
            profitabilityRatios = profitRatios.associateBy { it.period.yearMonth },
            stabilityRatios = stabilityRatios.associateBy { it.period.yearMonth },
            growthRatios = growthRatios.associateBy { it.period.yearMonth },
            financialRatios = emptyMap(),
            otherMajorRatios = emptyMap()
        )
    }

    companion object {
        // Transaction IDs (tr_id) - these need to be verified with actual KIS API docs
        private const val TR_ID_BALANCE_SHEET = "FHKST66430100"
        private const val TR_ID_INCOME_STATEMENT = "FHKST66430200"
        private const val TR_ID_FINANCIAL_RATIO = "FHKST66430300"
        private const val TR_ID_PROFIT_RATIO = "FHKST66430400"
        private const val TR_ID_OTHER_MAJOR_RATIO = "FHKST66430500"
        private const val TR_ID_STABILITY_RATIO = "FHKST66430600"
        private const val TR_ID_GROWTH_RATIO = "FHKST66430800"
    }
}

/**
 * Helper data class for parallel fetch results.
 */
private data class FetchResults(
    val balanceSheets: List<BalanceSheet>,
    val incomeStatements: List<IncomeStatement>,
    val profitRatios: List<ProfitabilityRatios>,
    val stabilityRatios: List<StabilityRatios>,
    val growthRatios: List<GrowthRatios>
)
