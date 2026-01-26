package com.stockapp.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard Kiwoom API response wrapper.
 */
@Serializable
data class ApiResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null
)

/**
 * Token response from OAuth endpoint (au10001).
 */
@Serializable
data class TokenResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_dt") val expiresDt: String? = null
)

/**
 * Pagination information from API response headers.
 * Used for Kiwoom REST API continuation queries.
 */
data class PaginationInfo(
    val hasNext: Boolean,
    val nextKey: String?
)

/**
 * API response with pagination support.
 * @param T The type of parsed data
 */
data class PaginatedResponse<T>(
    val data: T,
    val pagination: PaginationInfo
)

/**
 * API call errors.
 */
sealed class ApiError(override val message: String) : Exception(message) {
    class AuthError(msg: String) : ApiError(msg)
    class NetworkError(msg: String) : ApiError(msg)
    class RateLimitError(msg: String) : ApiError(msg)
    class ApiCallError(code: Int, msg: String) : ApiError("[$code] $msg")
    class ParseError(msg: String) : ApiError(msg)
    class TimeoutError(msg: String) : ApiError(msg)
    class NoApiKeyError(
        msg: String = "API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요."
    ) : ApiError(msg)
}
