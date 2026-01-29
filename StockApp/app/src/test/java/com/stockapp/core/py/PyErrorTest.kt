package com.stockapp.core.py

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for PyError hierarchy.
 */
class PyErrorTest {

    @Test
    fun `InitError message is preserved`() {
        val error = PyError.InitError("Python initialization failed")
        assertEquals("Python initialization failed", error.message)
    }

    @Test
    fun `NotInitialized message is preserved`() {
        val error = PyError.NotInitialized("PyClient not initialized")
        assertEquals("PyClient not initialized", error.message)
    }

    @Test
    fun `CallError message is preserved`() {
        val error = PyError.CallError("Function call failed")
        assertEquals("Function call failed", error.message)
    }

    @Test
    fun `Timeout message is preserved`() {
        val error = PyError.Timeout("Operation timed out after 30s")
        assertEquals("Operation timed out after 30s", error.message)
    }

    @Test
    fun `ParseError message is preserved`() {
        val error = PyError.ParseError("JSON parsing failed")
        assertEquals("JSON parsing failed", error.message)
    }

    @Test
    fun `PyError subtypes are sealed`() {
        // Verify exhaustive when matching
        val error: PyError = PyError.CallError("test")

        val result = when (error) {
            is PyError.InitError -> "init"
            is PyError.NotInitialized -> "notinit"
            is PyError.CallError -> "call"
            is PyError.Timeout -> "timeout"
            is PyError.ParseError -> "parse"
        }

        assertEquals("call", result)
    }

    @Test
    fun `PyError is an Exception`() {
        val error: Exception = PyError.CallError("test")
        assertEquals("test", error.message)
    }
}
