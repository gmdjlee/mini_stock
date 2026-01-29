package com.stockapp.core.api.token

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Base token manager with configurable caching and refresh logic.
 * P1 fix: Generic implementation to reduce code duplication in token management.
 *
 * Features:
 * - Thread-safe token caching using Mutex
 * - Configurable expiry buffer
 * - Automatic invalidation on configuration change
 *
 * @param T The configuration type for this token manager
 * @param bufferMinutes Minutes before expiry to consider token expired
 */
abstract class BaseTokenManager<T>(
    private val bufferMinutes: Long = 5L
) : ConfigurableTokenProvider<T> {

    private var cachedToken: Token? = null
    private var lastConfig: T? = null
    private val mutex = Mutex()

    protected abstract val tag: String

    /**
     * Get the current configuration from settings/preferences.
     */
    protected abstract suspend fun getConfig(): T

    /**
     * Fetch a new token using the provided configuration.
     * Called when cache is empty or expired.
     */
    protected abstract suspend fun fetchToken(config: T): Result<Token>

    /**
     * Check if configuration has changed (requires token refresh).
     * Default implementation uses equals().
     */
    protected open fun hasConfigChanged(oldConfig: T?, newConfig: T): Boolean {
        return oldConfig != newConfig
    }

    /**
     * Get token using internally retrieved config.
     */
    override suspend fun getToken(): Result<Token> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val config = getConfig()
                getTokenInternal(config)
            } catch (e: Exception) {
                Log.e(tag, "getToken() failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get token using externally provided config.
     */
    override suspend fun getToken(config: T): Result<Token> = mutex.withLock {
        withContext(Dispatchers.IO) {
            getTokenInternal(config)
        }
    }

    /**
     * Internal implementation - must be called within mutex lock.
     */
    private suspend fun getTokenInternal(config: T): Result<Token> {
        // Check if config changed (e.g., different API keys or base URL)
        if (hasConfigChanged(lastConfig, config)) {
            Log.d(tag, "Config changed, clearing cached token")
            cachedToken = null
        }

        // Check if we have a valid cached token
        val cached = cachedToken
        if (cached != null && !cached.isExpired(bufferMinutes)) {
            Log.d(tag, "Returning cached token (expires at ${cached.expiresAt})")
            return Result.success(cached)
        }

        // Fetch new token
        Log.d(tag, "Fetching new token...")
        return fetchToken(config).also { result ->
            result.onSuccess { token ->
                cachedToken = token
                lastConfig = config
                Log.d(tag, "Token cached (expires at ${token.expiresAt})")
            }
            result.onFailure { error ->
                Log.e(tag, "Token fetch failed: ${error.message}")
            }
        }
    }

    /**
     * Force refresh the token.
     * Clears cache then delegates to getTokenInternal() to avoid duplicating caching logic.
     */
    override suspend fun refreshToken(): Result<Token> = mutex.withLock {
        cachedToken = null  // Force cache miss
        withContext(Dispatchers.IO) {
            getTokenInternal(getConfig())
        }
    }

    /**
     * Clear cached token.
     */
    override suspend fun clearToken() {
        mutex.withLock {
            cachedToken = null
            lastConfig = null
            Log.d(tag, "Token cache cleared")
        }
    }

    /**
     * Check if a valid cached token exists.
     */
    fun hasCachedToken(): Boolean {
        val cached = cachedToken
        return cached != null && !cached.isExpired(bufferMinutes)
    }
}
