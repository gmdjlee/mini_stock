package com.stockapp.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for TokenInfo data class.
 */
class TokenInfoTest {

    @Test
    fun `bearer property formats correctly`() {
        val tokenInfo = TokenInfo(
            token = "test_token_123",
            expiresAt = LocalDateTime.now().plusHours(1),
            tokenType = "bearer"
        )

        assertEquals("Bearer test_token_123", tokenInfo.bearer)
    }

    @Test
    fun `isExpired returns false for token expiring far in future`() {
        val tokenInfo = TokenInfo(
            token = "test_token",
            expiresAt = LocalDateTime.now().plusHours(1)
        )

        assertFalse(tokenInfo.isExpired())
    }

    @Test
    fun `isExpired returns true for token already expired`() {
        val tokenInfo = TokenInfo(
            token = "test_token",
            expiresAt = LocalDateTime.now().minusMinutes(5)
        )

        assertTrue(tokenInfo.isExpired())
    }

    @Test
    fun `isExpired returns true within 1 minute buffer`() {
        // Token expires in 30 seconds - should be considered expired (within 1 min buffer)
        val tokenInfo = TokenInfo(
            token = "test_token",
            expiresAt = LocalDateTime.now().plusSeconds(30)
        )

        assertTrue(tokenInfo.isExpired())
    }

    @Test
    fun `isExpired returns false when just outside buffer`() {
        // Token expires in 2 minutes - should NOT be expired (outside 1 min buffer)
        val tokenInfo = TokenInfo(
            token = "test_token",
            expiresAt = LocalDateTime.now().plusMinutes(2)
        )

        assertFalse(tokenInfo.isExpired())
    }

    @Test
    fun `default token type is bearer`() {
        val tokenInfo = TokenInfo(
            token = "test_token",
            expiresAt = LocalDateTime.now().plusHours(1)
        )

        assertEquals("bearer", tokenInfo.tokenType)
    }
}
