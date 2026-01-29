package com.stockapp.core.error

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for ErrorCode enum.
 */
class ErrorCodeTest {

    @Test
    fun `fromString returns correct enum for valid code`() {
        assertEquals(ErrorCode.NETWORK_ERROR, ErrorCode.fromString("NETWORK_ERROR"))
        assertEquals(ErrorCode.TIMEOUT, ErrorCode.fromString("TIMEOUT"))
        assertEquals(ErrorCode.AUTH_ERROR, ErrorCode.fromString("AUTH_ERROR"))
        assertEquals(ErrorCode.NO_API_KEY, ErrorCode.fromString("NO_API_KEY"))
        assertEquals(ErrorCode.API_ERROR, ErrorCode.fromString("API_ERROR"))
        assertEquals(ErrorCode.RATE_LIMIT, ErrorCode.fromString("RATE_LIMIT"))
        assertEquals(ErrorCode.PARSE_ERROR, ErrorCode.fromString("PARSE_ERROR"))
        assertEquals(ErrorCode.NOT_FOUND, ErrorCode.fromString("NOT_FOUND"))
        assertEquals(ErrorCode.NO_DATA, ErrorCode.fromString("NO_DATA"))
        assertEquals(ErrorCode.INSUFFICIENT_DATA, ErrorCode.fromString("INSUFFICIENT_DATA"))
        assertEquals(ErrorCode.INVALID_ARG, ErrorCode.fromString("INVALID_ARG"))
        assertEquals(ErrorCode.PYTHON_INIT, ErrorCode.fromString("PYTHON_INIT"))
        assertEquals(ErrorCode.PYTHON_NOT_INIT, ErrorCode.fromString("PYTHON_NOT_INIT"))
        assertEquals(ErrorCode.PYTHON_CALL, ErrorCode.fromString("PYTHON_CALL"))
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromString("UNKNOWN"))
    }

    @Test
    fun `fromString returns UNKNOWN for invalid code`() {
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromString("INVALID_CODE"))
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromString(""))
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromString("random_string"))
    }

    @Test
    fun `code property matches enum name`() {
        for (errorCode in ErrorCode.entries) {
            assertEquals(errorCode.name, errorCode.code)
        }
    }

    @Test
    fun `displayName is not empty`() {
        for (errorCode in ErrorCode.entries) {
            assert(errorCode.displayName.isNotBlank()) {
                "ErrorCode ${errorCode.name} has empty displayName"
            }
        }
    }
}
