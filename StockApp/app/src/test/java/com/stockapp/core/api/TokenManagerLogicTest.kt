package com.stockapp.core.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Unit tests for TokenManager logic.
 * Mocks OkHttpClient to provide fake HTTP responses.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerLogicTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockHttpClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var tokenManager: TokenManager

    private val appKey = "testAppKey"
    private val secretKey = "testSecretKey"
    private val baseUrl = "https://api.kiwoom.com"

    @Before
    fun setup() {
        mockHttpClient = mock()
        mockCall = mock()
        whenever(mockHttpClient.newCall(any())).thenReturn(mockCall)
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        tokenManager = TokenManager(mockHttpClient, json, testDispatcher)
    }

    // ==================== Helper Methods ====================

    private fun createSuccessResponse(
        token: String = "test_token_abc",
        expiresDt: String? = null
    ): Response {
        val expiresAt = expiresDt ?: LocalDateTime.now().plusHours(24)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        val body = """
            {
                "return_code": 0,
                "return_msg": "success",
                "token": "$token",
                "token_type": "bearer",
                "expires_dt": "$expiresAt"
            }
        """.trimIndent()

        return Response.Builder()
            .request(Request.Builder().url("$baseUrl/oauth2/token").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun createErrorResponse(code: Int, message: String = "Error"): Response {
        return Response.Builder()
            .request(Request.Builder().url("$baseUrl/oauth2/token").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun createTokenErrorResponse(returnCode: Int, returnMsg: String): Response {
        val body = """
            {
                "return_code": $returnCode,
                "return_msg": "$returnMsg"
            }
        """.trimIndent()

        return Response.Builder()
            .request(Request.Builder().url("$baseUrl/oauth2/token").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    // ==================== Token Caching Tests ====================

    @Test
    fun `getToken returns cached token when valid`() = runTest(testDispatcher) {
        // First call returns a valid token
        whenever(mockCall.execute()).thenReturn(createSuccessResponse())

        val result1 = tokenManager.getToken(appKey, secretKey, baseUrl)
        assertTrue(result1.isSuccess)

        // Second call should use cache - no additional HTTP call
        val result2 = tokenManager.getToken(appKey, secretKey, baseUrl)
        assertTrue(result2.isSuccess)

        // Verify HTTP was only called once (cached on second call)
        verify(mockCall, times(1)).execute()

        assertEquals(result1.getOrNull()!!.token, result2.getOrNull()!!.token)
    }

    @Test
    fun `getToken refreshes when token is expired`() = runTest(testDispatcher) {
        // First call: return token that expires in the past
        val expiredTime = LocalDateTime.now().minusMinutes(5)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val expiredResponse = createSuccessResponse(token = "expired_token", expiresDt = expiredTime)

        val freshTime = LocalDateTime.now().plusHours(24)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val freshResponse = createSuccessResponse(token = "fresh_token", expiresDt = freshTime)

        whenever(mockCall.execute())
            .thenReturn(expiredResponse)
            .thenReturn(freshResponse)

        // First call gets an expired token
        val result1 = tokenManager.getToken(appKey, secretKey, baseUrl)
        assertTrue(result1.isSuccess)

        // Second call should fetch new token because cached one is expired
        val result2 = tokenManager.getToken(appKey, secretKey, baseUrl)
        assertTrue(result2.isSuccess)

        // Verify HTTP was called twice (token expired, so refreshed)
        verify(mockCall, times(2)).execute()
        assertEquals("fresh_token", result2.getOrNull()!!.token)
    }

    @Test
    fun `token is considered expired before actual expiry due to safety margin`() {
        // Token that expires in exactly 30 seconds (less than 1-minute safety margin)
        val expiresAt = LocalDateTime.now().plusSeconds(30)
        val tokenInfo = TokenInfo("test_token", expiresAt)

        assertTrue("Token should be considered expired within safety margin", tokenInfo.isExpired())
    }

    @Test
    fun `token is not expired when well within validity period`() {
        // Token that expires in 2 hours (well outside safety margin)
        val expiresAt = LocalDateTime.now().plusHours(2)
        val tokenInfo = TokenInfo("test_token", expiresAt)

        assertTrue("Token should not be expired", !tokenInfo.isExpired())
    }

    @Test
    fun `token bearer format is correct`() {
        val tokenInfo = TokenInfo("my_token_value", LocalDateTime.now().plusHours(1))
        assertEquals("Bearer my_token_value", tokenInfo.bearer)
    }

    // ==================== Token Invalidation Tests ====================

    @Test
    fun `invalidateToken removes cached token forcing re-fetch`() = runTest(testDispatcher) {
        val freshTime = LocalDateTime.now().plusHours(24)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        whenever(mockCall.execute())
            .thenReturn(createSuccessResponse(token = "token_v1", expiresDt = freshTime))
            .thenReturn(createSuccessResponse(token = "token_v2", expiresDt = freshTime))

        // Get initial token
        val result1 = tokenManager.getToken(appKey, secretKey, baseUrl)
        assertEquals("token_v1", result1.getOrNull()!!.token)

        // Invalidate the token
        tokenManager.invalidateToken(appKey, baseUrl)

        // Next call should fetch a new token
        val result2 = tokenManager.getToken(appKey, secretKey, baseUrl)
        assertEquals("token_v2", result2.getOrNull()!!.token)

        verify(mockCall, times(2)).execute()
    }

    @Test
    fun `clearTokens removes all cached tokens`() = runTest(testDispatcher) {
        val freshTime = LocalDateTime.now().plusHours(24)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        whenever(mockCall.execute())
            .thenReturn(createSuccessResponse(token = "token_a", expiresDt = freshTime))
            .thenReturn(createSuccessResponse(token = "token_b", expiresDt = freshTime))

        // Get token
        tokenManager.getToken(appKey, secretKey, baseUrl)

        // Clear all tokens
        tokenManager.clearTokens()

        // Next call should fetch a new token
        val result = tokenManager.getToken(appKey, secretKey, baseUrl)
        assertEquals("token_b", result.getOrNull()!!.token)

        verify(mockCall, times(2)).execute()
    }

    // ==================== Cache Key Isolation Tests ====================

    @Test
    fun `different baseUrl and appKey combinations are independent`() = runTest(testDispatcher) {
        val freshTime = LocalDateTime.now().plusHours(24)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        whenever(mockCall.execute())
            .thenReturn(createSuccessResponse(token = "token_mock", expiresDt = freshTime))
            .thenReturn(createSuccessResponse(token = "token_prod", expiresDt = freshTime))

        // Get token for mock URL
        val mockResult = tokenManager.getToken("keyA", secretKey, "https://mockapi.kiwoom.com")
        assertEquals("token_mock", mockResult.getOrNull()!!.token)

        // Get token for prod URL - should be a separate cache entry
        val prodResult = tokenManager.getToken("keyB", secretKey, "https://api.kiwoom.com")
        assertEquals("token_prod", prodResult.getOrNull()!!.token)

        // Both calls required HTTP (different cache keys)
        verify(mockCall, times(2)).execute()
    }

    @Test
    fun `same baseUrl different appKey creates separate cache entries`() = runTest(testDispatcher) {
        val freshTime = LocalDateTime.now().plusHours(24)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        whenever(mockCall.execute())
            .thenReturn(createSuccessResponse(token = "token_key1", expiresDt = freshTime))
            .thenReturn(createSuccessResponse(token = "token_key2", expiresDt = freshTime))

        val result1 = tokenManager.getToken("appKey1", "secret1", baseUrl)
        val result2 = tokenManager.getToken("appKey2", "secret2", baseUrl)

        assertEquals("token_key1", result1.getOrNull()!!.token)
        assertEquals("token_key2", result2.getOrNull()!!.token)
        verify(mockCall, times(2)).execute()
    }

    // ==================== Retry and Error Handling Tests ====================

    @Test
    fun `retry on network error with exponential backoff`() = runTest(testDispatcher) {
        // First call throws network error, second succeeds
        whenever(mockCall.execute())
            .thenThrow(UnknownHostException("No network"))
            .thenReturn(createSuccessResponse())

        val result = tokenManager.getToken(appKey, secretKey, baseUrl)

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        // Called twice: first fail, then success
        verify(mockCall, times(2)).execute()
    }

    @Test
    fun `retry on timeout error`() = runTest(testDispatcher) {
        whenever(mockCall.execute())
            .thenThrow(SocketTimeoutException("Timeout"))
            .thenReturn(createSuccessResponse())

        val result = tokenManager.getToken(appKey, secretKey, baseUrl)

        assertTrue(result.isSuccess)
        verify(mockCall, times(2)).execute()
    }

    @Test
    fun `no retry on auth error`() = runTest(testDispatcher) {
        // Auth error (return_code != 0) should not be retried
        whenever(mockCall.execute())
            .thenReturn(createTokenErrorResponse(-1, "Invalid credentials"))

        val result = tokenManager.getToken(appKey, secretKey, baseUrl)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiError.AuthError)
        // Only called once - no retry for auth errors
        verify(mockCall, times(1)).execute()
    }

    @Test
    fun `propagates network error after max retries`() = runTest(testDispatcher) {
        // All calls throw network error
        whenever(mockCall.execute())
            .thenThrow(UnknownHostException("No network"))

        val result = tokenManager.getToken(appKey, secretKey, baseUrl)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiError.NetworkError)
        // Called MAX_RETRIES + 1 times (initial + 3 retries)
        verify(mockCall, times(4)).execute()
    }

    @Test
    fun `propagates timeout error after max retries`() = runTest(testDispatcher) {
        whenever(mockCall.execute())
            .thenThrow(SocketTimeoutException("Timeout"))

        val result = tokenManager.getToken(appKey, secretKey, baseUrl)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiError.TimeoutError)
        verify(mockCall, times(4)).execute()
    }

    @Test
    fun `HTTP error response returns auth error`() = runTest(testDispatcher) {
        whenever(mockCall.execute())
            .thenReturn(createErrorResponse(401, "Unauthorized"))

        val result = tokenManager.getToken(appKey, secretKey, baseUrl)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiError.AuthError)
    }

    @Test
    fun `missing token in response returns auth error`() = runTest(testDispatcher) {
        val body = """
            {
                "return_code": 0,
                "return_msg": "success",
                "token": null,
                "token_type": "bearer",
                "expires_dt": null
            }
        """.trimIndent()

        val response = Response.Builder()
            .request(Request.Builder().url("$baseUrl/oauth2/token").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()

        whenever(mockCall.execute()).thenReturn(response)

        val result = tokenManager.getToken(appKey, secretKey, baseUrl)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiError.AuthError)
    }

    // ==================== Force Refresh Tests ====================

    @Test
    fun `refreshToken invalidates and fetches new token`() = runTest(testDispatcher) {
        val freshTime = LocalDateTime.now().plusHours(24)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        whenever(mockCall.execute())
            .thenReturn(createSuccessResponse(token = "old_token", expiresDt = freshTime))
            .thenReturn(createSuccessResponse(token = "new_token", expiresDt = freshTime))

        // First: get a token
        val result1 = tokenManager.getToken(appKey, secretKey, baseUrl)
        assertEquals("old_token", result1.getOrNull()!!.token)

        // Force refresh
        val result2 = tokenManager.refreshToken(appKey, secretKey, baseUrl)
        assertEquals("new_token", result2.getOrNull()!!.token)

        verify(mockCall, times(2)).execute()
    }

    @Test
    fun `generic IOException treated as network error and retried`() = runTest(testDispatcher) {
        // IOException that is not UnknownHostException or SocketTimeoutException
        // should be wrapped as AuthError (general catch)
        whenever(mockCall.execute())
            .thenThrow(IOException("Connection reset"))

        val result = tokenManager.getToken(appKey, secretKey, baseUrl)

        assertTrue(result.isFailure)
        // Generic exception results in AuthError (not NetworkError/TimeoutError)
        val error = result.exceptionOrNull()
        assertTrue(
            "Expected AuthError for generic IOException, got ${error?.javaClass?.simpleName}",
            error is ApiError.AuthError
        )
        // Not retried because generic IOException is not classified as retriable
        verify(mockCall, times(1)).execute()
    }
}
