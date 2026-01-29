package com.stockapp.core.error

import com.stockapp.core.api.ApiError
import com.stockapp.core.py.PyApiException
import com.stockapp.core.py.PyError

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
 * Convert PyError to AppError.
 */
fun PyError.toAppError(): AppError = when (this) {
    is PyError.InitError -> AppError.PythonInitError(message)
    is PyError.NotInitialized -> AppError.PythonNotInitializedError(message)
    is PyError.CallError -> AppError.PythonCallError(message)
    is PyError.Timeout -> AppError.TimeoutError(message)
    is PyError.ParseError -> AppError.ParseError(message)
}

/**
 * Convert PyApiException to AppError.
 */
fun PyApiException.toAppError(): AppError = when (code) {
    PyApiException.INVALID_ARG -> AppError.InvalidArgumentError(message)
    PyApiException.TICKER_NOT_FOUND -> AppError.NotFoundError("종목", message)
    PyApiException.NO_DATA -> AppError.NoDataError(message)
    PyApiException.API_ERROR -> AppError.ApiCallError(0, message)
    PyApiException.AUTH_ERROR -> AppError.AuthError(message)
    PyApiException.NETWORK_ERROR -> AppError.NetworkError(message)
    PyApiException.INSUFFICIENT_DATA -> AppError.InsufficientDataError(message)
    PyApiException.CHART_ERROR -> AppError.PythonCallError(message)
    PyApiException.CONDITION_NOT_FOUND -> AppError.NotFoundError("조건검색", message)
    else -> AppError.UnknownError(message)
}

/**
 * Convert any Throwable to AppError.
 * Use this at the boundary between data and UI layers.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is ApiError -> this.toAppError()
    is PyError -> this.toAppError()
    is PyApiException -> this.toAppError()
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
