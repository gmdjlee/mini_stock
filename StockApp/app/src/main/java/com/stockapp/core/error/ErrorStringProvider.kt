package com.stockapp.core.error

import android.content.Context
import com.stockapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides localized error messages from string resources (P3).
 *
 * This class acts as a bridge between the error system and Android string resources,
 * enabling proper localization of error messages.
 */
@Singleton
class ErrorStringProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Network errors
    fun getNetworkError(): String = context.getString(R.string.error_network)
    fun getTimeoutError(): String = context.getString(R.string.error_timeout)
    fun getTimeoutRetryError(): String = context.getString(R.string.error_timeout_retry)
    fun getConnectionError(): String = context.getString(R.string.error_connection)
    fun getSecurityError(): String = context.getString(R.string.error_security)

    // Auth errors
    fun getAuthError(): String = context.getString(R.string.error_auth)
    fun getNoApiKeyError(): String = context.getString(R.string.error_no_api_key)
    fun getTokenIssueError(httpCode: Int): String =
        context.getString(R.string.error_token_issue, httpCode)
    fun getTokenInvalidError(): String = context.getString(R.string.error_token_invalid)
    fun getTokenIssueGeneralError(message: String): String =
        context.getString(R.string.error_token_issue_general, message)

    // API errors
    fun getApiError(): String = context.getString(R.string.error_api)
    fun getApiErrorWithCode(code: Int): String =
        context.getString(R.string.error_api_with_code, code)
    fun getRateLimitError(): String = context.getString(R.string.error_rate_limit)
    fun getParseError(): String = context.getString(R.string.error_parse)
    fun getParseErrorDetail(detail: String): String =
        context.getString(R.string.error_parse_detail, detail)

    // Data errors
    fun getNotFoundError(entityType: String): String =
        context.getString(R.string.error_not_found, entityType)
    fun getNoDataError(): String = context.getString(R.string.error_no_data)
    fun getInsufficientDataError(): String = context.getString(R.string.error_insufficient_data)

    // Python errors
    fun getPythonInitError(message: String): String =
        context.getString(R.string.error_python_init, message)
    fun getPythonNotInitError(): String = context.getString(R.string.error_python_not_init)
    fun getPythonCallError(): String = context.getString(R.string.error_python_call)

    // Generic
    fun getUnknownError(): String = context.getString(R.string.error_unknown)

    // Analysis specific
    fun getAnalysisFailedError(): String = context.getString(R.string.error_analysis_failed)
    fun getRefreshFailedError(): String = context.getString(R.string.error_refresh_failed)
    fun getTickerRequiredError(): String = context.getString(R.string.error_ticker_required)

    // Settings specific
    fun getSettingsLoadError(): String = context.getString(R.string.error_settings_load)
    fun getEnterKeysError(): String = context.getString(R.string.settings_enter_keys)
    fun getEnterKisKeysError(): String = context.getString(R.string.settings_enter_kis_keys)
    fun getTestFailedError(message: String): String =
        context.getString(R.string.settings_test_failed, message)
    fun getSaveFailedError(): String = context.getString(R.string.settings_save_failed)

    // Search
    fun getSearchFailedError(): String = context.getString(R.string.search_failed)
    fun getSearchEnterQueryError(): String = context.getString(R.string.search_enter_query)

    /**
     * Get localized display message for an AppError.
     */
    fun getDisplayMessage(error: AppError): String = when (error) {
        is AppError.NetworkError -> getNetworkError()
        is AppError.TimeoutError -> getTimeoutRetryError()
        is AppError.AuthError -> getAuthError()
        is AppError.NoApiKeyError -> getNoApiKeyError()
        is AppError.ApiCallError -> getApiErrorWithCode(error.httpCode)
        is AppError.RateLimitError -> getRateLimitError()
        is AppError.ParseError -> getParseError()
        is AppError.NotFoundError -> getNotFoundError(error.entityType)
        is AppError.NoDataError -> getNoDataError()
        is AppError.InsufficientDataError -> getInsufficientDataError()
        is AppError.InvalidArgumentError -> error.message
        is AppError.PythonInitError -> getPythonInitError(error.message)
        is AppError.PythonNotInitializedError -> getPythonNotInitError()
        is AppError.PythonCallError -> getPythonCallError()
        is AppError.UnknownError -> getUnknownError()
    }

    /**
     * Get localized display name for an ErrorCode.
     */
    fun getErrorCodeDisplayName(errorCode: ErrorCode): String = when (errorCode) {
        ErrorCode.NETWORK_ERROR -> context.getString(R.string.error_code_network)
        ErrorCode.TIMEOUT -> context.getString(R.string.error_code_timeout)
        ErrorCode.AUTH_ERROR -> context.getString(R.string.error_code_auth)
        ErrorCode.NO_API_KEY -> context.getString(R.string.error_code_auth)
        ErrorCode.API_ERROR -> context.getString(R.string.error_code_api)
        ErrorCode.RATE_LIMIT -> context.getString(R.string.error_code_rate_limit)
        ErrorCode.PARSE_ERROR -> context.getString(R.string.error_code_parse)
        ErrorCode.NOT_FOUND -> context.getString(R.string.error_code_not_found)
        ErrorCode.NO_DATA -> context.getString(R.string.error_code_no_data)
        ErrorCode.INSUFFICIENT_DATA -> context.getString(R.string.error_code_insufficient)
        ErrorCode.INVALID_ARG -> context.getString(R.string.error_code_invalid_arg)
        ErrorCode.PYTHON_INIT -> context.getString(R.string.error_code_unknown)
        ErrorCode.PYTHON_NOT_INIT -> context.getString(R.string.error_code_unknown)
        ErrorCode.PYTHON_CALL -> context.getString(R.string.error_code_unknown)
        ErrorCode.UNKNOWN -> context.getString(R.string.error_code_unknown)
    }
}
