package com.stockapp.core.py

import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
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

    private var kiwoomClient: PyObject? = null
    private var isInitialized = false

    /**
     * Initialize Python environment.
     * Must be called before any Python calls.
     */
    suspend fun initialize(
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
            kiwoomClient = kiwoomModule.callAttr(
                "KiwoomClient",
                appKey,
                secretKey,
                baseUrl
            )
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PyError.InitError(e.message ?: "Python initialization failed"))
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
        Log.d(TAG, "call() started: module=$module, func=$func, args=$args")

        try {
            if (!isInitialized || kiwoomClient == null) {
                Log.e(TAG, "call() failed: PyClient not initialized")
                return@withContext Result.failure(
                    PyError.NotInitialized("PyClient not initialized. Call initialize() first.")
                )
            }

            withTimeout(timeoutMs) {
                val py = Python.getInstance()
                Log.d(TAG, "call() getting module: $module")
                val pyModule = py.getModule(module)

                // Build args: [client, ...args]
                val allArgs = mutableListOf<Any>(kiwoomClient!!)
                allArgs.addAll(args)
                Log.d(TAG, "call() invoking $func with ${allArgs.size} args")

                val result = pyModule.callAttr(func, *allArgs.toTypedArray())
                Log.d(TAG, "call() Python returned result type: ${result?.javaClass?.simpleName}")

                // Convert Python dict to JSON string using json.dumps()
                val jsonModule = py.getModule("json")
                val jsonStr = jsonModule.callAttr("dumps", result).toString()
                Log.d(TAG, "call() JSON response (first 500 chars): ${jsonStr.take(500)}")

                // Parse response
                val parsed = parser(jsonStr)
                Log.d(TAG, "call() parsed successfully: ${parsed?.javaClass?.simpleName}")
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
            if (!isInitialized || kiwoomClient == null) {
                return@withContext Result.failure(
                    PyError.NotInitialized("PyClient not initialized")
                )
            }

            withTimeout(timeoutMs) {
                val py = Python.getInstance()
                val pyModule = py.getModule(module)

                val allArgs = mutableListOf<Any>(kiwoomClient!!)
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

    fun isReady(): Boolean = isInitialized && kiwoomClient != null

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
