package com.stockapp.core.py

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PyResponseParser.
 */
class PyResponseParserTest {

    @Serializable
    data class TestData(val value: String)

    @Test
    fun `parse returns data for successful response`() {
        val json = """{"ok": true, "data": {"value": "test"}}"""
        val result = PyResponseParser.parse<TestData>(json)

        assertEquals("test", result.value)
    }

    @Test(expected = PyApiException::class)
    fun `parse throws PyApiException for error response`() {
        val json = """{"ok": false, "error": {"code": "TEST_ERROR", "msg": "Test error message"}}"""
        PyResponseParser.parse<TestData>(json)
    }

    @Test
    fun `parse throws PyApiException with correct code`() {
        val json = """{"ok": false, "error": {"code": "TICKER_NOT_FOUND", "msg": "Stock not found"}}"""
        try {
            PyResponseParser.parse<TestData>(json)
        } catch (e: PyApiException) {
            assertEquals("TICKER_NOT_FOUND", e.code)
            assertEquals("Stock not found", e.message)
        }
    }

    @Test
    fun `parseResult returns success for successful response`() {
        val json = """{"ok": true, "data": {"value": "test"}}"""
        val result = PyResponseParser.parseResult<TestData>(json)

        assertTrue(result.isSuccess)
        assertEquals("test", result.getOrNull()?.value)
    }

    @Test
    fun `parseResult returns failure for error response`() {
        val json = """{"ok": false, "error": {"code": "NO_DATA", "msg": "No data"}}"""
        val result = PyResponseParser.parseResult<TestData>(json)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is PyApiException)
        assertEquals("NO_DATA", (exception as PyApiException).code)
    }

    @Test
    fun `parseResult returns failure for invalid JSON`() {
        val json = """{"invalid json"""
        val result = PyResponseParser.parseResult<TestData>(json)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is PyApiException)
        assertEquals("PARSE_ERROR", (exception as PyApiException).code)
    }

    @Test
    fun `isSuccess returns true for successful response`() {
        val json = """{"ok": true, "data": {"value": "test"}}"""
        assertTrue(PyResponseParser.isSuccess(json))
    }

    @Test
    fun `isSuccess returns false for error response`() {
        val json = """{"ok": false, "error": {"code": "ERROR", "msg": "Error"}}"""
        assertFalse(PyResponseParser.isSuccess(json))
    }

    @Test
    fun `isSuccess returns false for invalid JSON`() {
        val json = """{"invalid json"""
        assertFalse(PyResponseParser.isSuccess(json))
    }

    @Test
    fun `parse handles response with null error code gracefully`() {
        val json = """{"ok": false, "error": {"msg": "No code error"}}"""
        try {
            PyResponseParser.parse<TestData>(json)
        } catch (e: PyApiException) {
            assertEquals("UNKNOWN", e.code)
        }
    }

    @Test
    fun `PyApiException contains all error code constants`() {
        assertEquals("INVALID_ARG", PyApiException.INVALID_ARG)
        assertEquals("TICKER_NOT_FOUND", PyApiException.TICKER_NOT_FOUND)
        assertEquals("NO_DATA", PyApiException.NO_DATA)
        assertEquals("API_ERROR", PyApiException.API_ERROR)
        assertEquals("AUTH_ERROR", PyApiException.AUTH_ERROR)
        assertEquals("NETWORK_ERROR", PyApiException.NETWORK_ERROR)
        assertEquals("CHART_ERROR", PyApiException.CHART_ERROR)
        assertEquals("CONDITION_NOT_FOUND", PyApiException.CONDITION_NOT_FOUND)
        assertEquals("INSUFFICIENT_DATA", PyApiException.INSUFFICIENT_DATA)
    }

    @Test
    fun `PyApiException message includes code`() {
        val exception = PyApiException("TEST_CODE", "Test message")
        assertTrue(exception.toString().contains("TEST_CODE"))
        assertTrue(exception.toString().contains("Test message"))
    }
}
