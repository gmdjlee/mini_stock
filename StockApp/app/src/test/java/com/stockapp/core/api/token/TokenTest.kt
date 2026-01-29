package com.stockapp.core.api.token

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for Token data class.
 */
class TokenTest {

    @Test
    fun `bearer property formats correctly`() {
        val token = Token(
            accessToken = "test_token",
            expiresAt = LocalDateTime.now().plusHours(1),
            tokenType = "Bearer"
        )

        assertEquals("Bearer test_token", token.bearer)
    }

    @Test
    fun `bearer property uses custom token type`() {
        val token = Token(
            accessToken = "test_token",
            expiresAt = LocalDateTime.now().plusHours(1),
            tokenType = "CustomType"
        )

        assertEquals("CustomType test_token", token.bearer)
    }

    @Test
    fun `isExpired returns false for future expiry`() {
        val token = Token(
            accessToken = "test_token",
            expiresAt = LocalDateTime.now().plusHours(1),
            tokenType = "Bearer"
        )

        assertFalse(token.isExpired())
    }

    @Test
    fun `isExpired returns true for past expiry`() {
        val token = Token(
            accessToken = "test_token",
            expiresAt = LocalDateTime.now().minusMinutes(1),
            tokenType = "Bearer"
        )

        assertTrue(token.isExpired())
    }

    @Test
    fun `isExpired returns true within buffer period`() {
        val token = Token(
            accessToken = "test_token",
            expiresAt = LocalDateTime.now().plusMinutes(3),
            tokenType = "Bearer"
        )

        // Default buffer is 5 minutes, token expires in 3 minutes
        // So it should be considered expired
        assertTrue(token.isExpired())
    }

    @Test
    fun `isExpired respects custom buffer`() {
        val token = Token(
            accessToken = "test_token",
            expiresAt = LocalDateTime.now().plusMinutes(3),
            tokenType = "Bearer"
        )

        // With 1 minute buffer, token expiring in 3 minutes is NOT expired
        assertFalse(token.isExpired(bufferMinutes = 1))

        // With 5 minute buffer, token expiring in 3 minutes IS expired
        assertTrue(token.isExpired(bufferMinutes = 5))
    }

    @Test
    fun `default token type is Bearer`() {
        val token = Token(
            accessToken = "test_token",
            expiresAt = LocalDateTime.now().plusHours(1)
        )

        assertEquals("Bearer", token.tokenType)
    }
}
