package com.stockapp.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ApiError hierarchy.
 */
class ApiErrorTest {

    @Test
    fun `AuthError message is preserved`() {
        val error = ApiError.AuthError("Invalid token")
        assertEquals("Invalid token", error.message)
    }

    @Test
    fun `NetworkError message is preserved`() {
        val error = ApiError.NetworkError("No internet connection")
        assertEquals("No internet connection", error.message)
    }

    @Test
    fun `RateLimitError message is preserved`() {
        val error = ApiError.RateLimitError("Rate limit exceeded")
        assertEquals("Rate limit exceeded", error.message)
    }

    @Test
    fun `ApiCallError formats code and message correctly`() {
        val error = ApiError.ApiCallError(404, "Not Found")
        assertTrue(error.message.contains("404"))
        assertTrue(error.message.contains("Not Found"))
        assertEquals(404, error.code)
    }

    @Test
    fun `ParseError message is preserved`() {
        val error = ApiError.ParseError("Invalid JSON format")
        assertEquals("Invalid JSON format", error.message)
    }

    @Test
    fun `TimeoutError message is preserved`() {
        val error = ApiError.TimeoutError("Connection timeout")
        assertEquals("Connection timeout", error.message)
    }

    @Test
    fun `NoApiKeyError has default message`() {
        val error = ApiError.NoApiKeyError()
        assertTrue(error.message.contains("API"))
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `NoApiKeyError accepts custom message`() {
        val customMessage = "Custom API key error"
        val error = ApiError.NoApiKeyError(customMessage)
        assertEquals(customMessage, error.message)
    }

    @Test
    fun `ApiError subtypes are sealed`() {
        // This test verifies that we can use when exhaustively
        val error: ApiError = ApiError.NetworkError("test")

        val result = when (error) {
            is ApiError.AuthError -> "auth"
            is ApiError.NetworkError -> "network"
            is ApiError.RateLimitError -> "rate"
            is ApiError.ApiCallError -> "api"
            is ApiError.ParseError -> "parse"
            is ApiError.TimeoutError -> "timeout"
            is ApiError.NoApiKeyError -> "nokey"
        }

        assertEquals("network", result)
    }
}
