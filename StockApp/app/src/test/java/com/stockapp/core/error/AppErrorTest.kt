package com.stockapp.core.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppError hierarchy.
 */
class AppErrorTest {

    @Test
    fun `NetworkError has correct error code`() {
        val error = AppError.NetworkError()
        assertEquals(ErrorCode.NETWORK_ERROR, error.code)
    }

    @Test
    fun `NetworkError uses default message when not provided`() {
        val error = AppError.NetworkError()
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `NetworkError uses custom message when provided`() {
        val customMessage = "Connection refused"
        val error = AppError.NetworkError(customMessage)
        assertEquals(customMessage, error.message)
    }

    @Test
    fun `TimeoutError has correct error code`() {
        val error = AppError.TimeoutError()
        assertEquals(ErrorCode.TIMEOUT, error.code)
    }

    @Test
    fun `AuthError has correct error code`() {
        val error = AppError.AuthError("Authentication failed")
        assertEquals(ErrorCode.AUTH_ERROR, error.code)
    }

    @Test
    fun `NoApiKeyError has correct error code and default message`() {
        val error = AppError.NoApiKeyError()
        assertEquals(ErrorCode.NO_API_KEY, error.code)
        assertTrue(error.message.contains("API"))
    }

    @Test
    fun `ApiCallError includes HTTP code in message`() {
        val error = AppError.ApiCallError(404, "Not Found")
        assertEquals(ErrorCode.API_ERROR, error.code)
        assertTrue(error.message.contains("404"))
        assertEquals(404, error.httpCode)
    }

    @Test
    fun `RateLimitError has correct error code`() {
        val error = AppError.RateLimitError()
        assertEquals(ErrorCode.RATE_LIMIT, error.code)
    }

    @Test
    fun `ParseError has correct error code`() {
        val error = AppError.ParseError("Invalid JSON")
        assertEquals(ErrorCode.PARSE_ERROR, error.code)
        assertTrue(error.message.contains("Invalid JSON"))
    }

    @Test
    fun `NotFoundError formats entity and identifier correctly`() {
        val error = AppError.NotFoundError("Stock", "005930")
        assertEquals(ErrorCode.NOT_FOUND, error.code)
        assertEquals("Stock 없음: 005930", error.message)
        assertEquals("Stock", error.entityType)
        assertEquals("005930", error.identifier)
    }

    @Test
    fun `NoDataError has correct error code`() {
        val error = AppError.NoDataError()
        assertEquals(ErrorCode.NO_DATA, error.code)
    }

    @Test
    fun `InsufficientDataError has correct error code`() {
        val error = AppError.InsufficientDataError()
        assertEquals(ErrorCode.INSUFFICIENT_DATA, error.code)
    }

    @Test
    fun `InvalidArgumentError has correct error code`() {
        val error = AppError.InvalidArgumentError("Invalid ticker")
        assertEquals(ErrorCode.INVALID_ARG, error.code)
    }

    @Test
    fun `PythonInitError has correct error code`() {
        val error = AppError.PythonInitError("Python failed to start")
        assertEquals(ErrorCode.PYTHON_INIT, error.code)
    }

    @Test
    fun `PythonNotInitializedError has correct error code`() {
        val error = AppError.PythonNotInitializedError()
        assertEquals(ErrorCode.PYTHON_NOT_INIT, error.code)
    }

    @Test
    fun `PythonCallError has correct error code`() {
        val error = AppError.PythonCallError("Module not found")
        assertEquals(ErrorCode.PYTHON_CALL, error.code)
    }

    @Test
    fun `UnknownError has correct error code`() {
        val error = AppError.UnknownError("Something went wrong")
        assertEquals(ErrorCode.UNKNOWN, error.code)
    }

    @Test
    fun `cause is preserved when provided`() {
        val originalCause = RuntimeException("Original cause")
        val error = AppError.NetworkError("Connection failed", originalCause)
        assertNotNull(error.cause)
        assertEquals(originalCause, error.cause)
    }

    @Test
    fun `getDisplayMessage returns user-friendly message`() {
        val error = AppError.NetworkError()
        val displayMsg = error.getDisplayMessage()
        assertTrue(displayMsg.isNotBlank())
        // Should be user-friendly (Korean message)
        assertTrue(displayMsg.contains("네트워크"))
    }
}
