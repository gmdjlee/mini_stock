package com.stockapp.core.py

import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.stockapp.BuildConfig
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Thread-safe client state using AtomicReference and Mutex
    private val kiwoomClientRef = AtomicReference<PyObject?>(null)
    private val initializedFlag = AtomicBoolean(false)
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
                kiwoomClientRef.set(client)
                initializedFlag.set(true)
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
            val client = kiwoomClientRef.get()
            if (!initializedFlag.get() || client == null) {
                Log.e(TAG, "call() failed: PyClient not initialized")
                return@withContext Result.failure(
                    PyError.NotInitialized("PyClient not initialized. Call initialize() first.")
                )
            }

            withTimeout(timeoutMs) {
                val py = Python.getInstance()
                val pyModule = py.getModule(module)

                // Build args: [client, ...args]
                val allArgs = mutableListOf<Any>(client)
                allArgs.addAll(args)

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
            Result.failure(PyError.Timeout("Python call timed out after ${timeoutMs}ms"))
        } catch (e: Exception) {
            Log.e(TAG, "call() exception: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(PyError.CallError(e.message ?: "Python call failed"))
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
            val client = kiwoomClientRef.get()
            if (!initializedFlag.get() || client == null) {
                return@withContext Result.failure(
                    PyError.NotInitialized("PyClient not initialized")
                )
            }

            withTimeout(timeoutMs) {
                val py = Python.getInstance()
                val pyModule = py.getModule(module)

                val allArgs = mutableListOf<Any>(client)
                allArgs.addAll(args)

                val result = pyModule.callAttr(func, *allArgs.toTypedArray())
                Result.success(result)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(PyError.Timeout("Python call timed out"))
        } catch (e: Exception) {
            Result.failure(PyError.CallError(e.message ?: "Python call failed"))
        }
    }

    fun isReady(): Boolean = initializedFlag.get() && kiwoomClientRef.get() != null

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
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(ctx))
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

    companion object {
        private const val TAG = "PyClient"
        const val DEFAULT_TIMEOUT_MS = 30_000L
        const val ANALYSIS_TIMEOUT_MS = 60_000L
    }
}

/**
 * Python call errors.
 */
sealed class PyError(override val message: String) : Exception(message) {
    class InitError(msg: String) : PyError(msg)
    class NotInitialized(msg: String) : PyError(msg)
    class CallError(msg: String) : PyError(msg)
    class Timeout(msg: String) : PyError(msg)
    class ParseError(msg: String) : PyError(msg)
}
