package com.stockapp.core.error

import com.stockapp.core.api.ApiError
import com.stockapp.core.py.PyApiException
import com.stockapp.core.py.PyError
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

    // === PyError conversions ===

    @Test
    fun `PyError InitError converts to AppError PythonInitError`() {
        val pyError = PyError.InitError("Python failed to start")
        val appError = pyError.toAppError()

        assertTrue(appError is AppError.PythonInitError)
        assertEquals(ErrorCode.PYTHON_INIT, appError.code)
    }

    @Test
    fun `PyError NotInitialized converts to AppError PythonNotInitializedError`() {
        val pyError = PyError.NotInitialized("Not initialized")
        val appError = pyError.toAppError()

        assertTrue(appError is AppError.PythonNotInitializedError)
        assertEquals(ErrorCode.PYTHON_NOT_INIT, appError.code)
    }

    @Test
    fun `PyError CallError converts to AppError PythonCallError`() {
        val pyError = PyError.CallError("Function not found")
        val appError = pyError.toAppError()

        assertTrue(appError is AppError.PythonCallError)
        assertEquals(ErrorCode.PYTHON_CALL, appError.code)
    }

    @Test
    fun `PyError Timeout converts to AppError TimeoutError`() {
        val pyError = PyError.Timeout("Timed out after 30s")
        val appError = pyError.toAppError()

        assertTrue(appError is AppError.TimeoutError)
        assertEquals(ErrorCode.TIMEOUT, appError.code)
    }

    @Test
    fun `PyError ParseError converts to AppError ParseError`() {
        val pyError = PyError.ParseError("JSON parse error")
        val appError = pyError.toAppError()

        assertTrue(appError is AppError.ParseError)
        assertEquals(ErrorCode.PARSE_ERROR, appError.code)
    }

    // === PyApiException conversions ===

    @Test
    fun `PyApiException INVALID_ARG converts to AppError InvalidArgumentError`() {
        val exception = PyApiException(PyApiException.INVALID_ARG, "Invalid ticker")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.InvalidArgumentError)
        assertEquals(ErrorCode.INVALID_ARG, appError.code)
    }

    @Test
    fun `PyApiException TICKER_NOT_FOUND converts to AppError NotFoundError`() {
        val exception = PyApiException(PyApiException.TICKER_NOT_FOUND, "005930")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.NotFoundError)
        assertEquals(ErrorCode.NOT_FOUND, appError.code)
    }

    @Test
    fun `PyApiException NO_DATA converts to AppError NoDataError`() {
        val exception = PyApiException(PyApiException.NO_DATA, "No data available")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.NoDataError)
        assertEquals(ErrorCode.NO_DATA, appError.code)
    }

    @Test
    fun `PyApiException AUTH_ERROR converts to AppError AuthError`() {
        val exception = PyApiException(PyApiException.AUTH_ERROR, "Auth failed")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.AuthError)
        assertEquals(ErrorCode.AUTH_ERROR, appError.code)
    }

    @Test
    fun `PyApiException NETWORK_ERROR converts to AppError NetworkError`() {
        val exception = PyApiException(PyApiException.NETWORK_ERROR, "Network error")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.NetworkError)
        assertEquals(ErrorCode.NETWORK_ERROR, appError.code)
    }

    @Test
    fun `PyApiException INSUFFICIENT_DATA converts to AppError InsufficientDataError`() {
        val exception = PyApiException(PyApiException.INSUFFICIENT_DATA, "Insufficient")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.InsufficientDataError)
        assertEquals(ErrorCode.INSUFFICIENT_DATA, appError.code)
    }

    @Test
    fun `PyApiException unknown code converts to AppError UnknownError`() {
        val exception = PyApiException("CUSTOM_ERROR", "Custom error")
        val appError = exception.toAppError()

        assertTrue(appError is AppError.UnknownError)
        assertEquals(ErrorCode.UNKNOWN, appError.code)
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
    fun `getErrorCode returns correct code for PyError`() {
        val error = PyError.InitError("Init failed")
        assertEquals(ErrorCode.PYTHON_INIT, error.getErrorCode())
    }

    @Test
    fun `getErrorCode returns UNKNOWN for generic exception`() {
        val error = RuntimeException("Generic error")
        assertEquals(ErrorCode.UNKNOWN, error.getErrorCode())
    }
}
