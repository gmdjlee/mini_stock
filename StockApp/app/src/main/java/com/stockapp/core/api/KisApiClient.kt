package com.stockapp.core.api

import android.util.Log
import com.stockapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KIS API configuration for ETF constituent collection.
 */
data class KisApiConfig(
    val appKey: String,
    val appSecret: String,
    val baseUrl: String = "https://openapi.koreainvestment.com:9443"
) {
    fun isValid(): Boolean = appKey.isNotBlank() && appSecret.isNotBlank()
}

/**
 * KIS API token response.
 */
@Serializable
private data class KisTokenResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val access_token_token_expired: String? = null
)

/**
 * KIS API token info.
 */
data class KisTokenInfo(
    val token: String,
    val expiresAt: LocalDateTime,
    val tokenType: String = "Bearer"
) {
    val bearer: String get() = "$tokenType $token"

    fun isExpired(): Boolean {
        return LocalDateTime.now() >= expiresAt.minusMinutes(5)
    }
}

/**
 * Direct Kotlin client for KIS (Korea Investment & Securities) REST API.
 * Used for ETF constituent data collection.
 */
@Singleton
class KisApiClient @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    // Rate limiting: minimum 500ms between API calls
    private val minInterval = 500L
    private var lastCallTime = 0L
    private val rateLimitMutex = Mutex()

    // Token cache
    private var tokenCache: KisTokenInfo? = null
    private val tokenMutex = Mutex()

    /**
     * Get valid token, fetching new one if needed.
     */
    suspend fun getToken(config: KisApiConfig): Result<KisTokenInfo> = tokenMutex.withLock {
        val cached = tokenCache
        if (cached != null && !cached.isExpired()) {
            return@withLock Result.success(cached)
        }

        return@withLock fetchToken(config).also { result ->
            result.onSuccess { token ->
                tokenCache = token
            }
        }
    }

    /**
     * Fetch new token from KIS API.
     */
    private suspend fun fetchToken(config: KisApiConfig): Result<KisTokenInfo> = withContext(Dispatchers.IO) {
        try {
            val requestBody = """
                {
                    "grant_type": "client_credentials",
                    "appkey": "${config.appKey}",
                    "appsecret": "${config.appSecret}"
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("${config.baseUrl}/oauth2/tokenP")
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(
                    ApiError.AuthError("KIS 토큰 발급 실패: HTTP ${response.code}")
                )
            }

            val tokenResponse = json.decodeFromString<KisTokenResponse>(responseBody)

            if (tokenResponse.access_token == null) {
                return@withContext Result.failure(
                    ApiError.AuthError("KIS 토큰 발급 실패: access_token is null")
                )
            }

            // Parse expiration datetime (format: yyyy-MM-dd HH:mm:ss)
            val expiresAt = try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                LocalDateTime.parse(tokenResponse.access_token_token_expired, formatter)
            } catch (e: Exception) {
                // Default to 24 hours from now
                LocalDateTime.now().plusHours(24)
            }

            Result.success(
                KisTokenInfo(
                    token = tokenResponse.access_token,
                    expiresAt = expiresAt,
                    tokenType = tokenResponse.token_type ?: "Bearer"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "KIS token fetch exception", e)
            Result.failure(
                when (e) {
                    is java.net.UnknownHostException -> ApiError.NetworkError("네트워크 연결 오류")
                    is java.net.SocketTimeoutException -> ApiError.TimeoutError("요청 시간 초과")
                    else -> ApiError.AuthError("KIS 토큰 발급 오류: ${e.message}")
                }
            )
        }
    }

    /**
     * Make a GET request to KIS REST API.
     *
     * @param trId Transaction ID (e.g., "FHKST121600C0")
     * @param url API endpoint path
     * @param queryParams Query parameters
     * @param config KIS API configuration
     * @param parser Function to parse response JSON string
     */
    suspend fun <T> get(
        trId: String,
        url: String,
        queryParams: Map<String, String>,
        config: KisApiConfig,
        parser: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            // Rate limiting
            waitForRateLimit()

            // Get token
            val tokenResult = getToken(config)
            val token = tokenResult.getOrElse { error ->
                return@withContext Result.failure(error)
            }

            // Build URL with query params
            val urlBuilder = StringBuilder("${config.baseUrl}$url")
            if (queryParams.isNotEmpty()) {
                urlBuilder.append("?")
                urlBuilder.append(queryParams.entries.joinToString("&") { "${it.key}=${it.value}" })
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("authorization", token.bearer)
                .addHeader("appkey", config.appKey)
                .addHeader("appsecret", config.appSecret)
                .addHeader("tr_id", trId)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .get()
                .build()

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "KIS API call: $trId -> $url")
            }

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "KIS API call failed: ${response.code}")
                return@withContext Result.failure(
                    ApiError.ApiCallError(response.code, "HTTP ${response.code}")
                )
            }

            // Parse the response
            val parsed = parser(responseBody)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "KIS API call exception: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(
                when (e) {
                    is java.net.UnknownHostException -> ApiError.NetworkError("네트워크 연결을 확인해주세요")
                    is java.net.SocketTimeoutException -> ApiError.TimeoutError("요청 시간이 초과되었습니다")
                    is kotlinx.serialization.SerializationException -> ApiError.ParseError("응답 파싱 오류: ${e.message}")
                    is ApiError -> e
                    else -> ApiError.ApiCallError(0, e.message ?: "알 수 없는 오류")
                }
            )
        }
    }

    /**
     * Wait for rate limit interval.
     */
    private suspend fun waitForRateLimit() {
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastCallTime
            if (elapsed < minInterval) {
                delay(minInterval - elapsed)
            }
            lastCallTime = System.currentTimeMillis()
        }
    }

    /**
     * Clear cached token.
     */
    suspend fun clearToken() = tokenMutex.withLock {
        tokenCache = null
    }

    companion object {
        private const val TAG = "KisApiClient"
    }
}
