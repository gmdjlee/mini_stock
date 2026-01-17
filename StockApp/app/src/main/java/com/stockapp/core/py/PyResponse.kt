package com.stockapp.core.py

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Standard Python API response wrapper.
 *
 * Success: {"ok": true, "data": {...}}
 * Error: {"ok": false, "error": {"code": "...", "msg": "..."}}
 */
@Serializable
data class PyResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: PyApiError? = null
)

@Serializable
data class PyApiError(
    val code: String,
    val msg: String,
    val ctx: Map<String, String>? = null
)

/**
 * Generic response with JsonElement for dynamic parsing.
 */
@Serializable
data class PyRawResponse(
    val ok: Boolean,
    val data: JsonElement? = null,
    val error: PyApiError? = null
)

/**
 * Helper to parse Python responses.
 */
object PyResponseParser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parse response and extract data or throw error.
     */
    inline fun <reified T> parse(jsonStr: String): T {
        val response = json.decodeFromString<PyResponse<T>>(jsonStr)
        if (response.ok && response.data != null) {
            return response.data
        } else {
            throw PyApiException(
                response.error?.code ?: "UNKNOWN",
                response.error?.msg ?: "Unknown error"
            )
        }
    }

    /**
     * Parse response and return Result.
     */
    inline fun <reified T> parseResult(jsonStr: String): Result<T> {
        return try {
            val data = parse<T>(jsonStr)
            Result.success(data)
        } catch (e: PyApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(PyApiException("PARSE_ERROR", e.message ?: "Parse error"))
        }
    }

    /**
     * Check if response is successful.
     */
    fun isSuccess(jsonStr: String): Boolean {
        return try {
            val response = json.decodeFromString<PyRawResponse>(jsonStr)
            response.ok
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Python API exception with error code.
 */
class PyApiException(
    val code: String,
    override val message: String
) : Exception("[$code] $message") {

    companion object {
        // Error codes
        const val INVALID_ARG = "INVALID_ARG"
        const val TICKER_NOT_FOUND = "TICKER_NOT_FOUND"
        const val NO_DATA = "NO_DATA"
        const val API_ERROR = "API_ERROR"
        const val AUTH_ERROR = "AUTH_ERROR"
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val CHART_ERROR = "CHART_ERROR"
        const val CONDITION_NOT_FOUND = "CONDITION_NOT_FOUND"
        const val INSUFFICIENT_DATA = "INSUFFICIENT_DATA"
    }
}
