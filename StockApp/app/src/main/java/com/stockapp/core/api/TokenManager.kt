package com.stockapp.core.api

import android.util.Log
import com.stockapp.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
 * Token information.
 */
data class TokenInfo(
    val token: String,
    val expiresAt: LocalDateTime,
    val tokenType: String = "bearer"
) {
    val bearer: String get() = "Bearer $token"

    fun isExpired(): Boolean {
        // Consider token expired 1 minute before actual expiry
        return LocalDateTime.now() >= expiresAt.minusMinutes(1)
    }
}

/**
 * Token fetch retry configuration.
 */
private object TokenRetryConfig {
    const val MAX_RETRIES = 3
    val RETRY_DELAYS_MS = listOf(1000L, 2000L, 4000L) // Exponential backoff
}

/**
 * Manages OAuth token for Kiwoom API.
 * Thread-safe token management with automatic refresh.
 */
@Singleton
class TokenManager @Inject constructor(
    private val httpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Token cache per base URL + app key combination
    private val tokenCache = mutableMapOf<String, TokenInfo>()
    private val tokenMutex = Mutex()

    /**
     * Get a valid token, refreshing if necessary.
     */
    suspend fun getToken(
        appKey: String,
        secretKey: String,
        baseUrl: String
    ): Result<TokenInfo> = tokenMutex.withLock {
        val cacheKey = "$baseUrl:$appKey"

        // Check if we have a valid cached token
        val cachedToken = tokenCache[cacheKey]
        if (cachedToken != null && !cachedToken.isExpired()) {
            return@withLock Result.success(cachedToken)
        }

        // Fetch new token
        return@withLock fetchToken(appKey, secretKey, baseUrl).also { result ->
            result.onSuccess { token ->
                tokenCache[cacheKey] = token
            }
        }
    }

    /**
     * Fetch a new token from Kiwoom OAuth endpoint (au10001) with retry logic.
     * Retries on network/timeout errors with exponential backoff.
     */
    private suspend fun fetchToken(
        appKey: String,
        secretKey: String,
        baseUrl: String
    ): Result<TokenInfo> = withContext(ioDispatcher) {
        var lastError: ApiError? = null

        for (attempt in 0..TokenRetryConfig.MAX_RETRIES) {
            val result = fetchTokenOnce(appKey, secretKey, baseUrl)

            result.fold(
                onSuccess = { return@withContext Result.success(it) },
                onFailure = { error ->
                    lastError = error as? ApiError ?: ApiError.AuthError(error.message ?: "알 수 없는 오류")

                    // Only retry on network/timeout errors
                    val isRetriable = error is ApiError.NetworkError || error is ApiError.TimeoutError
                    if (isRetriable && attempt < TokenRetryConfig.MAX_RETRIES) {
                        val delayMs = TokenRetryConfig.RETRY_DELAYS_MS.getOrElse(attempt) { 4000L }
                        Log.w(TAG, "Token fetch failed (attempt ${attempt + 1}/${TokenRetryConfig.MAX_RETRIES + 1}), retrying in ${delayMs}ms: ${error.message}")
                        delay(delayMs)
                    } else {
                        // Non-retriable error or max retries reached
                        return@withContext Result.failure(error)
                    }
                }
            )
        }

        Result.failure(lastError ?: ApiError.AuthError("토큰 발급 실패"))
    }

    /**
     * Single attempt to fetch a token from Kiwoom OAuth endpoint.
     */
    private suspend fun fetchTokenOnce(
        appKey: String,
        secretKey: String,
        baseUrl: String
    ): Result<TokenInfo> {
        try {
            val requestBody = """
                {
                    "grant_type": "client_credentials",
                    "appkey": "$appKey",
                    "secretkey": "$secretKey"
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("$baseUrl/oauth2/token")
                .addHeader("api-id", "au10001")
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Token fetch failed: ${response.code}")
                return Result.failure(
                    ApiError.AuthError("토큰 발급 실패: HTTP ${response.code}")
                )
            }

            val tokenResponse = json.decodeFromString<TokenResponse>(responseBody)

            if (tokenResponse.returnCode != 0) {
                return Result.failure(
                    ApiError.AuthError(tokenResponse.returnMsg ?: "토큰 발급 실패")
                )
            }

            val token = tokenResponse.token
            val expiresDt = tokenResponse.expiresDt

            if (token == null || expiresDt == null) {
                return Result.failure(
                    ApiError.AuthError("토큰 응답이 올바르지 않습니다")
                )
            }

            // Parse expiration datetime (format: yyyyMMddHHmmss)
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            val expiresAt = LocalDateTime.parse(expiresDt, formatter)

            return Result.success(TokenInfo(token, expiresAt, tokenResponse.tokenType ?: "bearer"))
        } catch (e: Exception) {
            Log.e(TAG, "Token fetch exception", e)
            return Result.failure(
                when (e) {
                    is java.net.UnknownHostException -> ApiError.NetworkError("네트워크 연결을 확인해주세요")
                    is java.net.SocketTimeoutException -> ApiError.TimeoutError("요청 시간이 초과되었습니다")
                    else -> ApiError.AuthError("토큰 발급 중 오류 발생: ${e.message}")
                }
            )
        }
    }

    /**
     * Clear all cached tokens.
     */
    suspend fun clearTokens() = tokenMutex.withLock {
        tokenCache.clear()
    }

    /**
     * Invalidate token for a specific API configuration.
     * Use this when receiving 401/403 errors to force token refresh on next call.
     */
    suspend fun invalidateToken(appKey: String, baseUrl: String) = tokenMutex.withLock {
        val cacheKey = "$baseUrl:$appKey"
        tokenCache.remove(cacheKey)
        Log.d(TAG, "Token invalidated for: $cacheKey")
    }

    /**
     * Force refresh token for a specific API configuration.
     * Invalidates existing token and fetches a new one.
     */
    suspend fun refreshToken(
        appKey: String,
        secretKey: String,
        baseUrl: String
    ): Result<TokenInfo> = tokenMutex.withLock {
        val cacheKey = "$baseUrl:$appKey"
        tokenCache.remove(cacheKey)

        return@withLock fetchToken(appKey, secretKey, baseUrl).also { result ->
            result.onSuccess { token ->
                tokenCache[cacheKey] = token
                Log.d(TAG, "Token refreshed for: $cacheKey")
            }
        }
    }

    companion object {
        private const val TAG = "TokenManager"
    }
}
