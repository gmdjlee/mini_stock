package com.stockapp.core.api.token

import java.time.LocalDateTime

/**
 * Generic token data class.
 * P1 fix: Unified token representation across different API providers.
 */
data class Token(
    val accessToken: String,
    val expiresAt: LocalDateTime,
    val tokenType: String = "Bearer"
) {
    val bearer: String get() = "$tokenType $accessToken"

    /**
     * Check if token is expired (with configurable buffer).
     * @param bufferMinutes Minutes before actual expiry to consider token expired
     */
    fun isExpired(bufferMinutes: Long = 5): Boolean {
        return LocalDateTime.now() >= expiresAt.minusMinutes(bufferMinutes)
    }
}

/**
 * Token provider interface for unified token management.
 * P1 fix: Common interface for different API token providers.
 */
interface TokenProvider {
    /**
     * Get a valid token, refreshing if necessary.
     * Thread-safe implementation required.
     */
    suspend fun getToken(): Result<Token>

    /**
     * Force refresh the token, ignoring cache.
     */
    suspend fun refreshToken(): Result<Token>

    /**
     * Clear cached token.
     */
    suspend fun clearToken()
}

/**
 * Configuration-aware token provider.
 * Implementations should invalidate cached tokens when configuration changes.
 *
 * @param T The configuration type (e.g., API key config)
 */
interface ConfigurableTokenProvider<T> : TokenProvider {
    /**
     * Get a valid token using the provided configuration.
     */
    suspend fun getToken(config: T): Result<Token>
}
