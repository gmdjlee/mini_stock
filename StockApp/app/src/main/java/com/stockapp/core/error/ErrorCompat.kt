package com.stockapp.core.error

import com.stockapp.core.api.ApiError

/**
 * Error compatibility layer for gradual migration.
 * P1 fix: Provides conversion functions between legacy error types and AppError.
 *
 * Migration strategy:
 * 1. Keep existing error types unchanged (backward compatible)
 * 2. Use toAppError() extension to convert at boundary layers
 * 3. ViewModels can use AppError for consistent handling
 * 4. Deprecate old types after full migration
 */

/**
 * Convert ApiError to AppError.
 */
fun ApiError.toAppError(): AppError = when (this) {
    is ApiError.AuthError -> AppError.AuthError(message)
    is ApiError.NetworkError -> AppError.NetworkError(message)
    is ApiError.RateLimitError -> AppError.RateLimitError(message)
    is ApiError.ApiCallError -> AppError.ApiCallError(code, message.removePrefix("[$code] "))
    is ApiError.ParseError -> AppError.ParseError(message)
    is ApiError.TimeoutError -> AppError.TimeoutError(message)
    is ApiError.NoApiKeyError -> AppError.NoApiKeyError(message)
}

/**
 * Convert any Throwable to AppError.
 * Use this at the boundary between data and UI layers.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is ApiError -> this.toAppError()
    is java.net.UnknownHostException -> AppError.NetworkError(cause = this)
    is java.net.SocketTimeoutException -> AppError.TimeoutError(cause = this)
    is java.net.ConnectException -> AppError.NetworkError("서버에 연결할 수 없습니다", this)
    is kotlinx.coroutines.TimeoutCancellationException -> AppError.TimeoutError(cause = this)
    is kotlinx.serialization.SerializationException ->
        AppError.ParseError(message ?: "Serialization error", this)
    is IllegalArgumentException -> AppError.InvalidArgumentError(message ?: "Invalid argument")
    else -> AppError.UnknownError(message ?: "Unknown error", this)
}

/**
 * Extension for Result to map failures to AppError.
 */
fun <T> Result<T>.mapErrorToAppError(): Result<T> = this.fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(it.toAppError()) }
)

/**
 * Extract error code from any exception.
 * Useful for logging and analytics.
 * Delegates to toAppError() to avoid duplicate pattern matching.
 */
fun Throwable.getErrorCode(): ErrorCode = toAppError().code

/**
 * Get user-friendly message from any exception.
 */
fun Throwable.getDisplayMessage(): String = when (this) {
    is AppError -> getDisplayMessage()
    else -> toAppError().getDisplayMessage()
}
