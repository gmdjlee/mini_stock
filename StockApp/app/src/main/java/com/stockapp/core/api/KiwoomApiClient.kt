package com.stockapp.core.api

import android.util.Log
import com.stockapp.BuildConfig
import com.stockapp.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API categories for separate rate limiting.
 * Allows different feature areas to make calls independently without blocking each other.
 */
enum class ApiCategory {
    SEARCH,      // ka10099 (stock list)
    ANALYSIS,    // ka10059, ka10001, ka10081, ka10082, ka10083
    RANKING,     // ka10021, ka10023, ka10030, ka10033, ka90009
    FINANCIAL,   // FHKST66430xxx
    ETF,         // ka40004 (ETF constituents)
    OTHER        // Default category
}

/**
 * Rate limiter for a specific API category.
 */
private class CategoryRateLimiter(private val minInterval: Long = 500L) {
    private var lastCallTime = 0L
    private val mutex = Mutex()

    suspend fun waitForRateLimit() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastCallTime
            if (elapsed < minInterval) {
                delay(minInterval - elapsed)
            }
            lastCallTime = System.currentTimeMillis()
        }
    }
}

/**
 * Direct Kotlin client for Kiwoom REST API.
 * Bypasses Python for features that need to be implemented in Kotlin only.
 *
 * Uses category-based rate limiting to allow different features (search, analysis, ranking)
 * to make API calls independently without blocking each other.
 */
@Singleton
class KiwoomApiClient @Inject constructor(
    private val tokenManager: TokenManager,
    private val httpClient: OkHttpClient,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // Category-based rate limiters (each category can make calls independently)
    private val categoryRateLimiters = mapOf(
        ApiCategory.SEARCH to CategoryRateLimiter(500L),
        ApiCategory.ANALYSIS to CategoryRateLimiter(500L),
        ApiCategory.RANKING to CategoryRateLimiter(500L),
        ApiCategory.FINANCIAL to CategoryRateLimiter(500L),
        ApiCategory.ETF to CategoryRateLimiter(500L),
        ApiCategory.OTHER to CategoryRateLimiter(500L)
    )

    /**
     * Make a direct API call to Kiwoom REST API.
     * Automatically retries with token refresh on 401/403 errors.
     *
     * @param apiId API identifier (e.g., "ka10021")
     * @param url API endpoint path (e.g., "/api/dostk/rkinfo")
     * @param body Request body as Map
     * @param appKey Kiwoom API app key
     * @param secretKey Kiwoom API secret key
     * @param baseUrl API base URL
     * @param parser Function to parse response JSON string
     */
    suspend fun <T> call(
        apiId: String,
        url: String,
        body: Map<String, String>,
        appKey: String,
        secretKey: String,
        baseUrl: String,
        parser: (String) -> T
    ): Result<T> = withContext(ioDispatcher) {
        // First attempt
        val result = callOnce(apiId, url, body, appKey, secretKey, baseUrl, parser)

        result.fold(
            onSuccess = { return@withContext Result.success(it) },
            onFailure = { error ->
                // Check if it's an auth error (401/403 or auth-related API error)
                val isAuthError = isAuthenticationError(error)
                if (isAuthError) {
                    Log.w(TAG, "Auth error detected, refreshing token and retrying: ${error.message}")

                    // Refresh token and retry once
                    val refreshResult = tokenManager.refreshToken(appKey, secretKey, baseUrl)
                    if (refreshResult.isFailure) {
                        return@withContext Result.failure(
                            ApiError.AuthError("토큰 갱신 실패: ${refreshResult.exceptionOrNull()?.message}")
                        )
                    }

                    // Retry with fresh token
                    return@withContext callOnce(apiId, url, body, appKey, secretKey, baseUrl, parser)
                }

                // Non-auth error, return as-is
                return@withContext Result.failure(error)
            }
        )
    }

    /**
     * Single attempt to make an API call.
     */
    private suspend fun <T> callOnce(
        apiId: String,
        url: String,
        body: Map<String, String>,
        appKey: String,
        secretKey: String,
        baseUrl: String,
        parser: (String) -> T
    ): Result<T> {
        try {
            // Rate limiting (category-based)
            waitForRateLimit(apiId)

            // Get token
            val tokenResult = tokenManager.getToken(appKey, secretKey, baseUrl)
            val token = tokenResult.getOrElse { error ->
                return Result.failure(error)
            }

            // Build request body JSON using kotlinx.serialization for proper escaping
            val requestBodyJson = json.encodeToString(body)

            val request = Request.Builder()
                .url("$baseUrl$url")
                .addHeader("api-id", apiId)
                .addHeader("authorization", token.bearer)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "API call: $apiId -> $url")
            }

            val (responseBody, responseCode, isSuccessful) = httpClient.newCall(request).execute().use { response ->
                Triple(response.body?.string(), response.code, response.isSuccessful)
            }

            if (!isSuccessful || responseBody == null) {
                Log.e(TAG, "API call failed: $responseCode")
                return Result.failure(
                    ApiError.ApiCallError(responseCode, "HTTP $responseCode")
                )
            }

            // Preprocess response to handle non-standard JSON (e.g., "+12345" numbers)
            val normalizedBody = normalizeJsonNumbers(responseBody)

            // Check for API error in response
            val apiResponse = json.decodeFromString<ApiResponse>(normalizedBody)
            if (apiResponse.returnCode != 0) {
                return Result.failure(
                    ApiError.ApiCallError(apiResponse.returnCode, apiResponse.returnMsg ?: "API 오류")
                )
            }

            // Parse the response
            val parsed = parser(normalizedBody)
            return Result.success(parsed)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return Result.failure(mapException(e))
        }
    }

    /**
     * Check if an error is authentication-related (401/403 or auth API error codes).
     */
    private fun isAuthenticationError(error: Throwable): Boolean {
        return when {
            error is ApiError.AuthError -> true
            error is ApiError.ApiCallError -> {
                // HTTP 401 (Unauthorized) or 403 (Forbidden)
                error.code == 401 || error.code == 403 ||
                    // Kiwoom API auth-related error codes
                    error.message?.contains("인증", ignoreCase = true) == true ||
                    error.message?.contains("토큰", ignoreCase = true) == true ||
                    error.message?.contains("권한", ignoreCase = true) == true
            }
            else -> false
        }
    }

    /**
     * Make a paginated API call to Kiwoom REST API.
     * Supports cont-yn and next-key headers for pagination (연속조회).
     * Automatically retries with token refresh on 401/403 errors.
     *
     * @param apiId API identifier (e.g., "ka40004")
     * @param url API endpoint path
     * @param body Request body as Map
     * @param appKey Kiwoom API app key
     * @param secretKey Kiwoom API secret key
     * @param baseUrl API base URL
     * @param contYn Continuation flag ("Y" to fetch next page, empty for first page)
     * @param nextKey Next key from previous response (empty for first page)
     * @param parser Function to parse response JSON string
     */
    suspend fun <T> callPaginated(
        apiId: String,
        url: String,
        body: Map<String, String>,
        appKey: String,
        secretKey: String,
        baseUrl: String,
        contYn: String = "",
        nextKey: String = "",
        parser: (String) -> T
    ): Result<PaginatedResponse<T>> = withContext(ioDispatcher) {
        // First attempt
        val result = callPaginatedOnce(apiId, url, body, appKey, secretKey, baseUrl, contYn, nextKey, parser)

        result.fold(
            onSuccess = { return@withContext Result.success(it) },
            onFailure = { error ->
                // Check if it's an auth error
                val isAuthError = isAuthenticationError(error)
                if (isAuthError) {
                    Log.w(TAG, "Auth error in paginated call, refreshing token and retrying: ${error.message}")

                    // Refresh token and retry once
                    val refreshResult = tokenManager.refreshToken(appKey, secretKey, baseUrl)
                    if (refreshResult.isFailure) {
                        return@withContext Result.failure(
                            ApiError.AuthError("토큰 갱신 실패: ${refreshResult.exceptionOrNull()?.message}")
                        )
                    }

                    // Retry with fresh token
                    return@withContext callPaginatedOnce(apiId, url, body, appKey, secretKey, baseUrl, contYn, nextKey, parser)
                }

                // Non-auth error, return as-is
                return@withContext Result.failure(error)
            }
        )
    }

    /**
     * Single attempt to make a paginated API call.
     */
    private suspend fun <T> callPaginatedOnce(
        apiId: String,
        url: String,
        body: Map<String, String>,
        appKey: String,
        secretKey: String,
        baseUrl: String,
        contYn: String,
        nextKey: String,
        parser: (String) -> T
    ): Result<PaginatedResponse<T>> {
        try {
            // Rate limiting (category-based)
            waitForRateLimit(apiId)

            // Get token
            val tokenResult = tokenManager.getToken(appKey, secretKey, baseUrl)
            val token = tokenResult.getOrElse { error ->
                return Result.failure(error)
            }

            // Build request body JSON
            val requestBodyJson = json.encodeToString(body)

            // Build request with pagination headers
            val requestBuilder = Request.Builder()
                .url("$baseUrl$url")
                .addHeader("api-id", apiId)
                .addHeader("authorization", token.bearer)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))

            // Add pagination headers if provided
            if (contYn.isNotEmpty()) {
                requestBuilder.addHeader("cont-yn", contYn)
            }
            if (nextKey.isNotEmpty()) {
                requestBuilder.addHeader("next-key", nextKey)
            }

            val request = requestBuilder.build()

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "API call (paginated): $apiId -> $url, contYn=$contYn")
            }

            // Extract all needed data inside use {} to ensure response is closed
            data class RawResponse(
                val body: String?,
                val code: Int,
                val isSuccessful: Boolean,
                val contYnHeader: String?,
                val nextKeyHeader: String?
            )

            val raw = httpClient.newCall(request).execute().use { response ->
                RawResponse(
                    body = response.body?.string(),
                    code = response.code,
                    isSuccessful = response.isSuccessful,
                    contYnHeader = response.header("cont-yn"),
                    nextKeyHeader = response.header("next-key")
                )
            }

            if (!raw.isSuccessful || raw.body == null) {
                Log.e(TAG, "API call failed: ${raw.code}")
                return Result.failure(
                    ApiError.ApiCallError(raw.code, "HTTP ${raw.code}")
                )
            }

            // Preprocess response to handle non-standard JSON (e.g., "+12345" numbers)
            val normalizedBody = normalizeJsonNumbers(raw.body)

            // Check for API error in response
            val apiResponse = json.decodeFromString<ApiResponse>(normalizedBody)
            if (apiResponse.returnCode != 0) {
                return Result.failure(
                    ApiError.ApiCallError(apiResponse.returnCode, apiResponse.returnMsg ?: "API 오류")
                )
            }

            // Extract pagination info from response headers
            val hasNext = raw.contYnHeader == "Y"
            val respNextKey = raw.nextKeyHeader ?: ""

            // Parse the response
            val parsed = parser(normalizedBody)

            return Result.success(
                PaginatedResponse(
                    data = parsed,
                    pagination = PaginationInfo(
                        hasNext = hasNext,
                        nextKey = respNextKey.ifEmpty { null }
                    )
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return Result.failure(mapException(e))
        }
    }

    /**
     * Fetch all pages of a paginated API call.
     * Automatically continues fetching while hasNext is true.
     * Returns partial results if a page fetch fails after collecting some data.
     *
     * @param apiId API identifier
     * @param url API endpoint path
     * @param body Request body as Map
     * @param appKey Kiwoom API app key
     * @param secretKey Kiwoom API secret key
     * @param baseUrl API base URL
     * @param maxPages Maximum number of pages to fetch (default: 10, for safety)
     * @param paginationDelayMs Extra delay between pagination calls (default: 1000ms)
     * @param parser Function to parse response JSON string to a List
     */
    suspend fun <T> callAllPages(
        apiId: String,
        url: String,
        body: Map<String, String>,
        appKey: String,
        secretKey: String,
        baseUrl: String,
        maxPages: Int = 10,
        paginationDelayMs: Long = PAGINATION_DELAY_MS,
        parser: (String) -> List<T>
    ): Result<List<T>> = withContext(ioDispatcher) {
        val allItems = mutableListOf<T>()
        var contYn = ""
        var nextKey = ""
        var pageCount = 0

        while (pageCount < maxPages) {
            // Fetch page with retry for rate limit errors
            val result = fetchPageWithRetry(
                apiId = apiId,
                url = url,
                body = body,
                appKey = appKey,
                secretKey = secretKey,
                baseUrl = baseUrl,
                contYn = contYn,
                nextKey = nextKey,
                parser = parser
            )

            val paginatedResponse = result.getOrElse { error ->
                // On failure, return partial results if we have any data
                if (allItems.isNotEmpty()) {
                    Log.w(TAG, "Pagination failed after $pageCount pages, returning ${allItems.size} partial results. Error: ${error.message}")
                    return@withContext Result.success(allItems)
                }
                // If no data collected yet, return the failure
                return@withContext Result.failure(error)
            }

            allItems.addAll(paginatedResponse.data)
            pageCount++

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Fetched page $pageCount, items: ${paginatedResponse.data.size}, hasNext: ${paginatedResponse.pagination.hasNext}")
            }

            if (!paginatedResponse.pagination.hasNext) {
                break
            }

            contYn = "Y"
            nextKey = paginatedResponse.pagination.nextKey ?: ""

            // Extra delay between pagination calls to avoid rate limiting
            delay(paginationDelayMs)
        }

        if (pageCount >= maxPages) {
            Log.w(TAG, "Reached max pages limit: $maxPages")
        }

        Result.success(allItems)
    }

    /**
     * Fetch a single page with retry logic for rate limit errors (429).
     */
    private suspend fun <T> fetchPageWithRetry(
        apiId: String,
        url: String,
        body: Map<String, String>,
        appKey: String,
        secretKey: String,
        baseUrl: String,
        contYn: String,
        nextKey: String,
        maxRetries: Int = MAX_RATE_LIMIT_RETRIES,
        parser: (String) -> T
    ): Result<PaginatedResponse<T>> {
        var lastError: Throwable? = null
        var retryCount = 0

        while (retryCount <= maxRetries) {
            val result = callPaginated(
                apiId = apiId,
                url = url,
                body = body,
                appKey = appKey,
                secretKey = secretKey,
                baseUrl = baseUrl,
                contYn = contYn,
                nextKey = nextKey,
                parser = parser
            )

            result.fold(
                onSuccess = { return Result.success(it) },
                onFailure = { error ->
                    lastError = error
                    // Check if it's a rate limit error (429)
                    val isRateLimitError = error is ApiError.ApiCallError &&
                        (error.code == 429 || error.code == 5)

                    if (isRateLimitError && retryCount < maxRetries) {
                        val backoffMs = RATE_LIMIT_BACKOFF_MS * (retryCount + 1)
                        Log.w(TAG, "Rate limit hit, retrying in ${backoffMs}ms (attempt ${retryCount + 1}/$maxRetries)")
                        delay(backoffMs)
                        retryCount++
                    } else {
                        // Non-retriable error or max retries reached
                        return Result.failure(error)
                    }
                }
            )
        }

        return Result.failure(lastError ?: ApiError.ApiCallError(0, "Unknown error after retries"))
    }

    /**
     * Get API category from API ID.
     * Categories allow different features to make calls independently.
     */
    private fun getCategory(apiId: String): ApiCategory {
        return when {
            apiId == "ka10099" -> ApiCategory.SEARCH
            apiId in listOf("ka10059", "ka10001", "ka10081", "ka10082", "ka10083", "ka10063") ->
                ApiCategory.ANALYSIS
            apiId in listOf("ka10021", "ka10023", "ka10030", "ka10033", "ka90009") ->
                ApiCategory.RANKING
            apiId.startsWith("FHKST") -> ApiCategory.FINANCIAL
            apiId == "ka40004" -> ApiCategory.ETF
            else -> ApiCategory.OTHER
        }
    }

    /**
     * Wait for rate limit interval based on API category.
     * Different categories have separate rate limiters, allowing parallel calls across features.
     */
    private suspend fun waitForRateLimit(apiId: String) {
        val category = getCategory(apiId)
        val rateLimiter = categoryRateLimiters[category]
            ?: categoryRateLimiters.getValue(ApiCategory.OTHER)
        rateLimiter.waitForRateLimit()
    }

    /**
     * Map exceptions to appropriate ApiError types.
     */
    private fun mapException(e: Exception): ApiError {
        Log.e(TAG, "API call exception: ${e.javaClass.simpleName} - ${e.message}", e)
        return when (e) {
            is java.net.UnknownHostException -> ApiError.NetworkError("네트워크 연결을 확인해주세요")
            is java.net.SocketTimeoutException -> ApiError.TimeoutError("요청 시간이 초과되었습니다")
            is kotlinx.serialization.SerializationException -> ApiError.ParseError("응답 파싱 오류: ${e.message}")
            is ApiError -> e
            else -> ApiError.ApiCallError(0, e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * Normalize JSON by removing leading '+' signs from numeric values.
     * Kiwoom API sometimes returns numbers like "+12345" which is invalid JSON.
     */
    private fun normalizeJsonNumbers(json: String): String {
        // Remove + from quoted values: "+117500" -> "117500"
        var result = QUOTED_PLUS_REGEX.replace(json) { "\"${it.groupValues[1]}\"" }

        // Remove + from unquoted values after : or , (e.g., ":+123" -> ":123")
        result = UNQUOTED_PLUS_REGEX.replace(result) { "${it.groupValues[1]}${it.groupValues[2]}" }

        return result
    }

    companion object {
        private const val TAG = "KiwoomApiClient"

        // Pre-compiled regex patterns for JSON normalization
        private val QUOTED_PLUS_REGEX = Regex("\"\\+(\\d+)\"")
        private val UNQUOTED_PLUS_REGEX = Regex("([,:])\\s*\\+(\\d+)")

        // Pagination delay between consecutive page fetches (ms)
        private const val PAGINATION_DELAY_MS = 1000L

        // Rate limit retry settings
        private const val MAX_RATE_LIMIT_RETRIES = 3
        private const val RATE_LIMIT_BACKOFF_MS = 2000L
    }
}
