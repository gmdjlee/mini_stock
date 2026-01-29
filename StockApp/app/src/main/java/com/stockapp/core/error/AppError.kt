package com.stockapp.core.error

/**
 * Unified error hierarchy for StockApp.
 * P1 fix: Consolidates ApiError, PyError, and PyApiException into a single type system.
 *
 * This provides:
 * - Consistent error handling across the app
 * - Structured error codes for logging and display
 * - Type-safe error pattern matching
 */
sealed class AppError(
    val code: ErrorCode,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    // === Network/Transport Errors ===
    class NetworkError(
        message: String = "네트워크 연결을 확인해주세요",
        cause: Throwable? = null
    ) : AppError(ErrorCode.NETWORK_ERROR, message, cause)

    class TimeoutError(
        message: String = "요청 시간이 초과되었습니다",
        cause: Throwable? = null
    ) : AppError(ErrorCode.TIMEOUT, message, cause)

    // === Authentication Errors ===
    class AuthError(
        message: String,
        cause: Throwable? = null
    ) : AppError(ErrorCode.AUTH_ERROR, message, cause)

    class NoApiKeyError(
        message: String = "API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요."
    ) : AppError(ErrorCode.NO_API_KEY, message)

    // === API Response Errors ===
    class ApiCallError(
        val httpCode: Int,
        message: String,
        cause: Throwable? = null
    ) : AppError(ErrorCode.API_ERROR, "[$httpCode] $message", cause)

    class RateLimitError(
        message: String = "요청 한도 초과. 잠시 후 다시 시도해주세요."
    ) : AppError(ErrorCode.RATE_LIMIT, message)

    class ParseError(
        message: String,
        cause: Throwable? = null
    ) : AppError(ErrorCode.PARSE_ERROR, "응답 파싱 오류: $message", cause)

    // === Data Errors ===
    class NotFoundError(
        val entityType: String,
        val identifier: String
    ) : AppError(ErrorCode.NOT_FOUND, "$entityType 없음: $identifier")

    class NoDataError(
        message: String = "데이터가 없습니다"
    ) : AppError(ErrorCode.NO_DATA, message)

    class InsufficientDataError(
        message: String = "계산에 필요한 데이터가 부족합니다"
    ) : AppError(ErrorCode.INSUFFICIENT_DATA, message)

    class InvalidArgumentError(
        message: String
    ) : AppError(ErrorCode.INVALID_ARG, message)

    // === Python Bridge Errors ===
    class PythonInitError(
        message: String,
        cause: Throwable? = null
    ) : AppError(ErrorCode.PYTHON_INIT, message, cause)

    class PythonNotInitializedError(
        message: String = "PyClient not initialized. Call initialize() first."
    ) : AppError(ErrorCode.PYTHON_NOT_INIT, message)

    class PythonCallError(
        message: String,
        cause: Throwable? = null
    ) : AppError(ErrorCode.PYTHON_CALL, message, cause)

    // === Generic Error ===
    class UnknownError(
        message: String,
        cause: Throwable? = null
    ) : AppError(ErrorCode.UNKNOWN, message, cause)

    /**
     * Get user-friendly display message.
     */
    fun getDisplayMessage(): String = when (this) {
        is NetworkError -> "네트워크 연결을 확인해주세요"
        is TimeoutError -> "요청 시간이 초과되었습니다. 다시 시도해주세요."
        is AuthError -> "인증에 실패했습니다. API 키를 확인해주세요."
        is NoApiKeyError -> message
        is ApiCallError -> "서버 오류가 발생했습니다. (코드: $httpCode)"
        is RateLimitError -> message
        is ParseError -> "데이터 처리 중 오류가 발생했습니다"
        is NotFoundError -> "$entityType 을(를) 찾을 수 없습니다"
        is NoDataError -> message
        is InsufficientDataError -> message
        is InvalidArgumentError -> message
        is PythonInitError -> "앱 초기화 실패: $message"
        is PythonNotInitializedError -> "앱이 초기화되지 않았습니다"
        is PythonCallError -> "처리 중 오류가 발생했습니다"
        is UnknownError -> "알 수 없는 오류가 발생했습니다"
    }
}

/**
 * Error codes for consistent identification across the app.
 */
enum class ErrorCode(val code: String, val displayName: String) {
    // Network
    NETWORK_ERROR("NETWORK_ERROR", "네트워크 오류"),
    TIMEOUT("TIMEOUT", "시간 초과"),

    // Auth
    AUTH_ERROR("AUTH_ERROR", "인증 오류"),
    NO_API_KEY("NO_API_KEY", "API 키 없음"),

    // API
    API_ERROR("API_ERROR", "API 오류"),
    RATE_LIMIT("RATE_LIMIT", "요청 제한"),
    PARSE_ERROR("PARSE_ERROR", "파싱 오류"),

    // Data
    NOT_FOUND("NOT_FOUND", "찾을 수 없음"),
    NO_DATA("NO_DATA", "데이터 없음"),
    INSUFFICIENT_DATA("INSUFFICIENT_DATA", "데이터 부족"),
    INVALID_ARG("INVALID_ARG", "잘못된 인자"),

    // Python
    PYTHON_INIT("PYTHON_INIT", "Python 초기화 오류"),
    PYTHON_NOT_INIT("PYTHON_NOT_INIT", "Python 미초기화"),
    PYTHON_CALL("PYTHON_CALL", "Python 호출 오류"),

    // Other
    UNKNOWN("UNKNOWN", "알 수 없는 오류");

    companion object {
        fun fromString(code: String): ErrorCode =
            entries.find { it.code == code } ?: UNKNOWN
    }
}
