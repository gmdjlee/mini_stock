package com.stockapp.core.py

import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.stockapp.BuildConfig
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.stockapp.core.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Python Bridge Client.
 * All Python calls are performed through this class.
 */
@Singleton
class PyClient @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    // Thread-safe client state: @Volatile ensures visibility across threads
    // Mutex protects initialization sequence, volatile ensures read visibility
    @Volatile
    private var kiwoomClient: PyObject? = null
    @Volatile
    private var initialized: Boolean = false
    private val initMutex = Mutex()

    /**
     * Initialize Python environment.
     * Must be called before any Python calls.
     * Thread-safe: uses Mutex to prevent concurrent initialization.
     */
    suspend fun initialize(
        appKey: String,
        secretKey: String,
        baseUrl: String = "https://api.kiwoom.com"
    ): Result<Unit> = initMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(ctx))
                }

                val py = Python.getInstance()
                val kiwoomModule = py.getModule("stock_analyzer.client.kiwoom")
                val client = kiwoomModule.callAttr(
                    "KiwoomClient",
                    appKey,
                    secretKey,
                    baseUrl
                )
                kiwoomClient = client
                initialized = true
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(PyError.InitError(e.message ?: "Python initialization failed"))
            }
        }
    }

    /**
     * Call Python function with KiwoomClient.
     *
     * @param module Python module path (e.g., "stock_analyzer.stock.search")
     * @param func Function name to call
     * @param args Additional arguments after client
     * @param timeoutMs Timeout in milliseconds
     * @param parser Function to parse JSON response
     */
    suspend fun <T> call(
        module: String,
        func: String,
        args: List<Any> = emptyList(),
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        parser: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "call() started: module=$module, func=$func")
        }

        try {
            val client = kiwoomClient
            if (!initialized || client == null) {
                Log.e(TAG, "call() failed: PyClient not initialized")
                return@withContext Result.failure(
                    PyError.NotInitialized("PyClient not initialized. Call initialize() first.")
                )
            }

            // Safety check: Ensure Python is actually started before proceeding
            // This prevents native crashes if there's a race condition
            if (!Python.isStarted()) {
                Log.e(TAG, "call() failed: Python interpreter not started")
                return@withContext Result.failure(
                    PyError.NotInitialized("Python interpreter not started. Please restart the app.")
                )
            }

            withTimeout(timeoutMs) {
                val py = Python.getInstance()
                val pyModule = py.getModule(module)

                // Build args: [client, ...args]
                val allArgs = listOf(client) + args
                val result = pyModule.callAttr(func, *allArgs.toTypedArray())

                // Convert Python dict to JSON string using json.dumps()
                val jsonModule = py.getModule("json")
                val jsonStr = jsonModule.callAttr("dumps", result).toString()

                if (BuildConfig.DEBUG) {
                    // Only log response length, not content (may contain sensitive data)
                    Log.d(TAG, "call() response received: ${jsonStr.length} chars")
                }

                // Parse response
                val parsed = parser(jsonStr)
                Result.success(parsed)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e(TAG, "call() timeout after ${timeoutMs}ms", e)
            Result.failure(PyError.Timeout("요청 시간이 초과되었습니다 (${timeoutMs / 1000}초)"))
        } catch (e: Exception) {
            Log.e(TAG, "call() exception: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(classifyError(e))
        }
    }

    /**
     * Call Python function without client (for utility functions).
     */
    suspend fun <T> callDirect(
        module: String,
        func: String,
        args: List<Any> = emptyList(),
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        parser: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            // Safety check: Ensure Python is started
            if (!Python.isStarted()) {
                Log.e(TAG, "callDirect() failed: Python interpreter not started")
                return@withContext Result.failure(
                    PyError.NotInitialized("Python interpreter not started. Please restart the app.")
                )
            }

            withTimeout(timeoutMs) {
                val py = Python.getInstance()
                val pyModule = py.getModule(module)

                val result = pyModule.callAttr(func, *args.toTypedArray())

                // Convert Python dict to JSON string using json.dumps()
                val jsonModule = py.getModule("json")
                val jsonStr = jsonModule.callAttr("dumps", result).toString()

                val parsed = parser(jsonStr)
                Result.success(parsed)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(PyError.Timeout("Python call timed out after ${timeoutMs}ms"))
        } catch (e: Exception) {
            Result.failure(PyError.CallError(e.message ?: "Python call failed"))
        }
    }

    /**
     * Raw Python call returning PyObject.
     * Use for complex return types or chained calls.
     */
    suspend fun callRaw(
        module: String,
        func: String,
        args: List<Any> = emptyList(),
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Result<PyObject> = withContext(Dispatchers.IO) {
        try {
            val client = kiwoomClient
            if (!initialized || client == null) {
                return@withContext Result.failure(
                    PyError.NotInitialized("PyClient not initialized")
                )
            }

            // Safety check: Ensure Python is started
            if (!Python.isStarted()) {
                Log.e(TAG, "callRaw() failed: Python interpreter not started")
                return@withContext Result.failure(
                    PyError.NotInitialized("Python interpreter not started. Please restart the app.")
                )
            }

            withTimeout(timeoutMs) {
                val py = Python.getInstance()
                val pyModule = py.getModule(module)

                val allArgs = listOf(client) + args
                val result = pyModule.callAttr(func, *allArgs.toTypedArray())
                Result.success(result)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(PyError.Timeout("Python call timed out"))
        } catch (e: Exception) {
            Result.failure(PyError.CallError(e.message ?: "Python call failed"))
        }
    }

    fun isReady(): Boolean = initialized && kiwoomClient != null

    /**
     * Test API key connection without modifying global state.
     * Use this for testing API keys in settings.
     * Thread-safe: uses separate test client instance.
     *
     * This method actually verifies credentials by calling auth.get_token()
     * which triggers a real API call to the Kiwoom OAuth endpoint.
     */
    suspend fun testConnection(
        appKey: String,
        secretKey: String,
        baseUrl: String = "https://api.kiwoom.com"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Start Python if not already started (with proper synchronization)
            initMutex.withLock {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(ctx))
                }
            }

            val py = Python.getInstance()
            val kiwoomModule = py.getModule("stock_analyzer.client.kiwoom")

            // Create a temporary test client (not stored globally)
            val testClient = kiwoomModule.callAttr(
                "KiwoomClient",
                appKey,
                secretKey,
                baseUrl
            )

            if (testClient == null) {
                return@withContext Result.failure(PyError.InitError("Failed to create test client"))
            }

            // Actually verify credentials by calling auth.get_token()
            // This triggers a real API call to Kiwoom OAuth endpoint
            // If credentials are invalid, AuthError will be thrown
            val authClient = testClient.get("auth")
            authClient?.callAttr("get_token")
                ?: return@withContext Result.failure(PyError.InitError("Failed to get auth client"))

            Result.success(Unit)
        } catch (e: Exception) {
            // Extract meaningful error message from Python exception
            val errorMsg = e.message?.let { msg ->
                when {
                    msg.contains("AuthError") -> {
                        // Extract the Korean message from AuthError
                        val match = Regex("AuthError: (.+)").find(msg)
                        match?.groupValues?.get(1) ?: msg
                    }
                    msg.contains("Network error") -> "네트워크 연결 오류"
                    else -> msg
                }
            } ?: "Connection test failed"

            Result.failure(PyError.InitError(errorMsg))
        }
    }

    /**
     * Classify Python exception into appropriate PyError type.
     */
    private fun classifyError(e: Exception): PyError {
        val msg = e.message ?: "알 수 없는 오류"

        return when {
            // Auth errors (not retriable)
            msg.contains("AuthError", ignoreCase = true) ||
            msg.contains("인증", ignoreCase = true) ||
            msg.contains("권한", ignoreCase = true) ||
            msg.contains("Invalid", ignoreCase = true) && msg.contains("key", ignoreCase = true) -> {
                val cleanMsg = extractPythonErrorMessage(msg) ?: "인증 오류가 발생했습니다"
                PyError.AuthError(cleanMsg)
            }

            // Network errors (retriable)
            msg.contains("Network", ignoreCase = true) ||
            msg.contains("네트워크", ignoreCase = true) ||
            msg.contains("Connection", ignoreCase = true) ||
            msg.contains("UnknownHost", ignoreCase = true) ||
            msg.contains("SocketTimeout", ignoreCase = true) ||
            msg.contains("connect", ignoreCase = true) && msg.contains("fail", ignoreCase = true) -> {
                PyError.NetworkError("네트워크 연결 오류: ${extractPythonErrorMessage(msg) ?: "연결할 수 없습니다"}")
            }

            // Timeout errors (retriable)
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("시간 초과", ignoreCase = true) -> {
                PyError.Timeout("요청 시간이 초과되었습니다")
            }

            // Default: CallError - check if retriable based on error content
            else -> {
                val isRetriable = msg.contains("temporary", ignoreCase = true) ||
                        msg.contains("retry", ignoreCase = true) ||
                        msg.contains("일시적", ignoreCase = true)
                PyError.CallError(extractPythonErrorMessage(msg) ?: msg, isRetriable)
            }
        }
    }

    /**
     * Extract meaningful error message from Python exception string.
     */
    private fun extractPythonErrorMessage(msg: String): String? {
        // Try to extract message from "ErrorType: message" format
        val patterns = listOf(
            Regex("""AuthError:\s*(.+)"""),
            Regex("""ApiError:\s*(.+)"""),
            Regex("""NetworkError:\s*(.+)"""),
            Regex("""Exception:\s*(.+)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(msg)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        // Return null if no pattern matched
        return null
    }

    companion object {
        private const val TAG = "PyClient"
        // Reference centralized config for timeout constants
        val DEFAULT_TIMEOUT_MS = AppConfig.DEFAULT_TIMEOUT_MS
        val ANALYSIS_TIMEOUT_MS = AppConfig.ANALYSIS_TIMEOUT_MS
    }
}

/**
 * Python call errors.
 * @property isRetriable true if this error is transient and the operation can be retried
 */
sealed class PyError(override val message: String, val isRetriable: Boolean = false) : Exception(message) {
    /** Initialization failed - may be retriable if caused by network issues */
    class InitError(msg: String, retriable: Boolean = false) : PyError(msg, retriable)
    /** PyClient not initialized - not retriable, requires app restart or re-initialization */
    class NotInitialized(msg: String) : PyError(msg, false)
    /** API call failed - may be retriable depending on the underlying cause */
    class CallError(msg: String, retriable: Boolean = false) : PyError(msg, retriable)
    /** Timeout - retriable as it may be a transient network issue */
    class Timeout(msg: String) : PyError(msg, true)
    /** Parse error - not retriable as data is malformed */
    class ParseError(msg: String) : PyError(msg, false)
    /** Network error - retriable as it's a transient issue */
    class NetworkError(msg: String) : PyError(msg, true)
    /** Auth error - not retriable without fixing credentials */
    class AuthError(msg: String) : PyError(msg, false)
}
