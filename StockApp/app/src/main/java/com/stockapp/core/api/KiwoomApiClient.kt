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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct Kotlin client for Kiwoom REST API.
 * Bypasses Python for features that need to be implemented in Kotlin only.
 */
@Singleton
class KiwoomApiClient @Inject constructor(
    private val tokenManager: TokenManager,
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

    /**
     * Make a direct API call to Kiwoom REST API.
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
        try {
            // Rate limiting
            waitForRateLimit()

            // Get token
            val tokenResult = tokenManager.getToken(appKey, secretKey, baseUrl)
            val token = tokenResult.getOrElse { error ->
                return@withContext Result.failure(error)
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

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "API call failed: ${response.code}")
                return@withContext Result.failure(
                    ApiError.ApiCallError(response.code, "HTTP ${response.code}")
                )
            }

            // Check for API error in response
            val apiResponse = json.decodeFromString<ApiResponse>(responseBody)
            if (apiResponse.returnCode != 0) {
                return@withContext Result.failure(
                    ApiError.ApiCallError(apiResponse.returnCode, apiResponse.returnMsg ?: "API 오류")
                )
            }

            // Parse the response
            val parsed = parser(responseBody)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Make a paginated API call to Kiwoom REST API.
     * Supports cont-yn and next-key headers for pagination (연속조회).
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
        try {
            // Rate limiting
            waitForRateLimit()

            // Get token
            val tokenResult = tokenManager.getToken(appKey, secretKey, baseUrl)
            val token = tokenResult.getOrElse { error ->
                return@withContext Result.failure(error)
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

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "API call failed: ${response.code}")
                return@withContext Result.failure(
                    ApiError.ApiCallError(response.code, "HTTP ${response.code}")
                )
            }

            // Check for API error in response
            val apiResponse = json.decodeFromString<ApiResponse>(responseBody)
            if (apiResponse.returnCode != 0) {
                return@withContext Result.failure(
                    ApiError.ApiCallError(apiResponse.returnCode, apiResponse.returnMsg ?: "API 오류")
                )
            }

            // Extract pagination info from response headers
            val hasNext = response.header("cont-yn") == "Y"
            val respNextKey = response.header("next-key") ?: ""

            // Parse the response
            val parsed = parser(responseBody)

            Result.success(
                PaginatedResponse(
                    data = parsed,
                    pagination = PaginationInfo(
                        hasNext = hasNext,
                        nextKey = respNextKey.ifEmpty { null }
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(mapException(e))
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

    companion object {
        private const val TAG = "KiwoomApiClient"

        // Pagination delay between consecutive page fetches (ms)
        private const val PAGINATION_DELAY_MS = 1000L

        // Rate limit retry settings
        private const val MAX_RATE_LIMIT_RETRIES = 3
        private const val RATE_LIMIT_BACKOFF_MS = 2000L
    }
}
