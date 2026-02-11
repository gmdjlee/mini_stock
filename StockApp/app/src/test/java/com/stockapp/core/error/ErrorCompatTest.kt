package com.stockapp.core.error

import com.stockapp.core.api.ApiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for error compatibility layer.
 */
class ErrorCompatTest {

    // === ApiError conversions ===

    @Test
    fun `ApiError AuthError converts to AppError AuthError`() {
        val apiError = ApiError.AuthError("Invalid credentials")
        val appError = apiError.toAppError()

        assertTrue(appError is AppError.AuthError)
        assertEquals(ErrorCode.AUTH_ERROR, appError.code)
    }

    @Test
    fun `ApiError NetworkError converts to AppError NetworkError`() {
        val apiError = ApiError.NetworkError("Connection failed")
        val appError = apiError.toAppError()

        assertTrue(appError is AppError.NetworkError)
        assertEquals(ErrorCode.NETWORK_ERROR, appError.code)
    }

    @Test
    fun `ApiError RateLimitError converts to AppError RateLimitError`() {
        val apiError = ApiError.RateLimitError("Too many requests")
        val appError = apiError.toAppError()

        assertTrue(appError is AppError.RateLimitError)
        assertEquals(ErrorCode.RATE_LIMIT, appError.code)
    }

    @Test
    fun `ApiError ApiCallError converts to AppError ApiCallError`() {
        val apiError = ApiError.ApiCallError(500, "Internal Server Error")
        val appError = apiError.toAppError()

        assertTrue(appError is AppError.ApiCallError)
        assertEquals(ErrorCode.API_ERROR, appError.code)
    }

    @Test
    fun `ApiError ParseError converts to AppError ParseError`() {
        val apiError = ApiError.ParseError("Invalid JSON")
        val appError = apiError.toAppError()

        assertTrue(appError is AppError.ParseError)
        assertEquals(ErrorCode.PARSE_ERROR, appError.code)
    }

    @Test
    fun `ApiError TimeoutError converts to AppError TimeoutError`() {
        val apiError = ApiError.TimeoutError("Request timed out")
        val appError = apiError.toAppError()

        assertTrue(appError is AppError.TimeoutError)
        assertEquals(ErrorCode.TIMEOUT, appError.code)
    }

    @Test
    fun `ApiError NoApiKeyError converts to AppError NoApiKeyError`() {
        val apiError = ApiError.NoApiKeyError()
        val appError = apiError.toAppError()

        assertTrue(appError is AppError.NoApiKeyError)
        assertEquals(ErrorCode.NO_API_KEY, appError.code)
    }

    // === Throwable.toAppError() ===

    @Test
    fun `Throwable extension handles UnknownHostException`() {
        val exception = java.net.UnknownHostException("Host not found")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.NetworkError)
        assertEquals(ErrorCode.NETWORK_ERROR, appError.code)
    }

    @Test
    fun `Throwable extension handles SocketTimeoutException`() {
        val exception = java.net.SocketTimeoutException("Socket timed out")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.TimeoutError)
        assertEquals(ErrorCode.TIMEOUT, appError.code)
    }

    @Test
    fun `Throwable extension handles ConnectException`() {
        val exception = java.net.ConnectException("Connection refused")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.NetworkError)
        assertEquals(ErrorCode.NETWORK_ERROR, appError.code)
    }

    @Test
    fun `Throwable extension handles IllegalArgumentException`() {
        val exception = IllegalArgumentException("Invalid argument")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.InvalidArgumentError)
        assertEquals(ErrorCode.INVALID_ARG, appError.code)
    }

    @Test
    fun `Throwable extension handles unknown exception`() {
        val exception = RuntimeException("Something went wrong")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.UnknownError)
        assertEquals("Something went wrong", appError.message)
    }

    @Test
    fun `Throwable extension returns same AppError when already AppError`() {
        val originalError = AppError.NetworkError("Original error")
        val result = originalError.toAppError()

        assertTrue(result === originalError)
    }

    // === Result extension ===

    @Test
    fun `mapErrorToAppError preserves successful result`() {
        val result = Result.success("data")
        val mapped = result.mapErrorToAppError()

        assertTrue(mapped.isSuccess)
        assertEquals("data", mapped.getOrNull())
    }

    @Test
    fun `mapErrorToAppError converts failure to AppError`() {
        val result = Result.failure<String>(ApiError.NetworkError("Failed"))
        val mapped = result.mapErrorToAppError()

        assertTrue(mapped.isFailure)
        val error = mapped.exceptionOrNull()
        assertTrue(error is AppError.NetworkError)
    }

    // === getErrorCode extension ===

    @Test
    fun `getErrorCode returns correct code for AppError`() {
        val error = AppError.NetworkError()
        assertEquals(ErrorCode.NETWORK_ERROR, error.getErrorCode())
    }

    @Test
    fun `getErrorCode returns correct code for ApiError`() {
        val error = ApiError.TimeoutError("Timeout")
        assertEquals(ErrorCode.TIMEOUT, error.getErrorCode())
    }

    @Test
    fun `getErrorCode returns UNKNOWN for generic exception`() {
        val error = RuntimeException("Generic error")
        assertEquals(ErrorCode.UNKNOWN, error.getErrorCode())
    }
}
