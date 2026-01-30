package com.stockapp.core.api

import android.util.Log
import com.stockapp.BuildConfig
import com.stockapp.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
 * KIS token fetch retry configuration.
 */
private object KisTokenRetryConfig {
    const val MAX_RETRIES = 3
    val RETRY_DELAYS_MS = listOf(1000L, 2000L, 4000L) // Exponential backoff
}

/**
 * Direct Kotlin client for KIS (Korea Investment & Securities) REST API.
 * Used for ETF constituent data collection.
 */
@Singleton
class KisApiClient @Inject constructor(
    private val httpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Rate limiting: minimum 500ms between API calls
    private val minInterval = 500L
    private var lastCallTime = 0L
    private val rateLimitMutex = Mutex()

    // Token cache per baseUrl (supports mock/production mode switching)
    private val tokenCache = mutableMapOf<String, KisTokenInfo>()
    private val tokenMutex = Mutex()

    /**
     * Get valid token, fetching new one if needed.
     * Token is cached per baseUrl to support mock/production mode switching.
     */
    suspend fun getToken(config: KisApiConfig): Result<KisTokenInfo> = tokenMutex.withLock {
        val cacheKey = config.baseUrl
        val cached = tokenCache[cacheKey]
        if (cached != null && !cached.isExpired()) {
            return@withLock Result.success(cached)
        }

        return@withLock fetchToken(config).also { result ->
            result.onSuccess { token ->
                tokenCache[cacheKey] = token
            }
        }
    }

    /**
     * Fetch new token from KIS API with retry logic.
     * Retries on network/timeout errors with exponential backoff.
     */
    private suspend fun fetchToken(config: KisApiConfig): Result<KisTokenInfo> = withContext(ioDispatcher) {
        var lastError: ApiError? = null

        for (attempt in 0..KisTokenRetryConfig.MAX_RETRIES) {
            val result = fetchTokenOnce(config)

            result.fold(
                onSuccess = { return@withContext Result.success(it) },
                onFailure = { error ->
                    lastError = error as? ApiError ?: ApiError.AuthError(error.message ?: "알 수 없는 오류")

                    // Only retry on network/timeout errors
                    val isRetriable = error is ApiError.NetworkError || error is ApiError.TimeoutError
                    if (isRetriable && attempt < KisTokenRetryConfig.MAX_RETRIES) {
                        val delayMs = KisTokenRetryConfig.RETRY_DELAYS_MS.getOrElse(attempt) { 4000L }
                        Log.w(TAG, "KIS token fetch failed (attempt ${attempt + 1}/${KisTokenRetryConfig.MAX_RETRIES + 1}), retrying in ${delayMs}ms: ${error.message}")
                        delay(delayMs)
                    } else {
                        // Non-retriable error or max retries reached
                        return@withContext Result.failure(error)
                    }
                }
            )
        }

        Result.failure(lastError ?: ApiError.AuthError("KIS 토큰 발급 실패"))
    }

    /**
     * Single attempt to fetch a token from KIS API.
     */
    private suspend fun fetchTokenOnce(config: KisApiConfig): Result<KisTokenInfo> {
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
                return Result.failure(
                    ApiError.AuthError("KIS 토큰 발급 실패: HTTP ${response.code}")
                )
            }

            val tokenResponse = json.decodeFromString<KisTokenResponse>(responseBody)

            if (tokenResponse.access_token == null) {
                return Result.failure(
                    ApiError.AuthError("KIS 토큰 발급 실패: access_token is null")
                )
            }

            // Parse expiration datetime (format: yyyy-MM-dd HH:mm:ss)
            val expiresAt = try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                LocalDateTime.parse(tokenResponse.access_token_token_expired, formatter)
            } catch (e: Exception) {
                // Log warning when parsing fails
                Log.w(TAG, "Failed to parse KIS token expiration, defaulting to 24 hours: ${tokenResponse.access_token_token_expired}")
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
            return Result.failure(
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
     * Automatically retries with token refresh on 401/403 errors.
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
    ): Result<T> = withContext(ioDispatcher) {
        // First attempt
        val result = getOnce(trId, url, queryParams, config, parser)

        result.fold(
            onSuccess = { return@withContext Result.success(it) },
            onFailure = { error ->
                // Check if it's an auth error (401/403)
                val isAuthError = isAuthenticationError(error)
                if (isAuthError) {
                    Log.w(TAG, "KIS auth error detected, refreshing token and retrying: ${error.message}")

                    // Refresh token and retry once
                    val refreshResult = refreshToken(config)
                    if (refreshResult.isFailure) {
                        return@withContext Result.failure(
                            ApiError.AuthError("KIS 토큰 갱신 실패: ${refreshResult.exceptionOrNull()?.message}")
                        )
                    }

                    // Retry with fresh token
                    return@withContext getOnce(trId, url, queryParams, config, parser)
                }

                // Non-auth error, return as-is
                return@withContext Result.failure(error)
            }
        )
    }

    /**
     * Single attempt to make a GET request.
     */
    private suspend fun <T> getOnce(
        trId: String,
        url: String,
        queryParams: Map<String, String>,
        config: KisApiConfig,
        parser: (String) -> T
    ): Result<T> {
        try {
            // Rate limiting
            waitForRateLimit()

            // Get token
            val tokenResult = getToken(config)
            val token = tokenResult.getOrElse { error ->
                return Result.failure(error)
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
                return Result.failure(
                    ApiError.ApiCallError(response.code, "HTTP ${response.code}")
                )
            }

            // Parse the response
            val parsed = parser(responseBody)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "KIS API call exception: ${e.javaClass.simpleName} - ${e.message}", e)
            return Result.failure(
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
     * Check if an error is authentication-related (401/403).
     */
    private fun isAuthenticationError(error: Throwable): Boolean {
        return when {
            error is ApiError.AuthError -> true
            error is ApiError.ApiCallError -> {
                // HTTP 401 (Unauthorized) or 403 (Forbidden)
                error.code == 401 || error.code == 403 ||
                    // KIS API auth-related error messages
                    error.message?.contains("인증", ignoreCase = true) == true ||
                    error.message?.contains("토큰", ignoreCase = true) == true ||
                    error.message?.contains("권한", ignoreCase = true) == true ||
                    error.message?.contains("token", ignoreCase = true) == true
            }
            else -> false
        }
    }

    /**
     * Force refresh token for the given config's baseUrl.
     * Invalidates existing token and fetches a new one.
     */
    suspend fun refreshToken(config: KisApiConfig): Result<KisTokenInfo> = tokenMutex.withLock {
        val cacheKey = config.baseUrl
        tokenCache.remove(cacheKey)

        return@withLock fetchToken(config).also { result ->
            result.onSuccess { token ->
                tokenCache[cacheKey] = token
                Log.d(TAG, "KIS token refreshed successfully for: $cacheKey")
            }
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
     * Clear all cached tokens.
     */
    suspend fun clearTokens() = tokenMutex.withLock {
        tokenCache.clear()
        Log.d(TAG, "All KIS tokens cleared")
    }

    /**
     * Clear cached token for a specific baseUrl.
     */
    suspend fun clearToken(baseUrl: String) = tokenMutex.withLock {
        tokenCache.remove(baseUrl)
        Log.d(TAG, "KIS token cleared for: $baseUrl")
    }

    companion object {
        private const val TAG = "KisApiClient"
    }
}
