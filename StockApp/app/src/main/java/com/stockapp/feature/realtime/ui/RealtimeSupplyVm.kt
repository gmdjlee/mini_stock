package com.stockapp.feature.realtime.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.core.config.FeatureFlagRepo
import com.stockapp.core.config.FeatureFlags
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.realtime.domain.model.RealtimeSupplySummary
import com.stockapp.feature.realtime.domain.model.TradingHours
import com.stockapp.feature.realtime.domain.usecase.GetRealtimeSupplySummaryUC
import com.stockapp.feature.realtime.domain.usecase.RefreshRealtimeSupplyUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "RealtimeSupplyVm"

/**
 * Realtime supply screen state.
 */
sealed class RealtimeSupplyState {
    data object NoStock : RealtimeSupplyState()
    data object Loading : RealtimeSupplyState()
    data object FeatureDisabled : RealtimeSupplyState()
    data class Success(val summary: RealtimeSupplySummary) : RealtimeSupplyState()
    data class Error(val code: String, val msg: String) : RealtimeSupplyState()
}

/**
 * Auto-refresh settings.
 */
data class AutoRefreshSettings(
    val enabled: Boolean = false,
    val intervalMs: Long = DEFAULT_REFRESH_INTERVAL_MS
) {
    companion object {
        const val DEFAULT_REFRESH_INTERVAL_MS = 60_000L  // 1 minute
        const val MIN_REFRESH_INTERVAL_MS = 30_000L     // 30 seconds
        const val MAX_REFRESH_INTERVAL_MS = 300_000L   // 5 minutes
    }
}

@HiltViewModel
class RealtimeSupplyVm @Inject constructor(
    private val selectedStockManager: SelectedStockManager,
    private val getRealtimeSupplySummaryUC: GetRealtimeSupplySummaryUC,
    private val refreshRealtimeSupplyUC: RefreshRealtimeSupplyUC,
    private val featureFlagRepo: FeatureFlagRepo
) : ViewModel() {

    private val _state = MutableStateFlow<RealtimeSupplyState>(RealtimeSupplyState.NoStock)
    val state: StateFlow<RealtimeSupplyState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _autoRefreshSettings = MutableStateFlow(AutoRefreshSettings())
    val autoRefreshSettings: StateFlow<AutoRefreshSettings> = _autoRefreshSettings.asStateFlow()

    private val _isTradingHours = MutableStateFlow(TradingHours.isTradingHours())
    val isTradingHours: StateFlow<Boolean> = _isTradingHours.asStateFlow()

    private var currentTicker: String? = null
    private var autoRefreshJob: Job? = null

    init {
        // Observe selected stock changes
        viewModelScope.launch {
            selectedStockManager.selectedTicker.collect { ticker ->
                if (ticker != null && ticker != currentTicker) {
                    currentTicker = ticker
                    loadRealtimeSupply(ticker)
                } else if (ticker == null) {
                    currentTicker = null
                    _state.value = RealtimeSupplyState.NoStock
                    stopAutoRefresh()
                }
            }
        }

        // Update trading hours status periodically
        viewModelScope.launch {
            while (isActive) {
                _isTradingHours.value = TradingHours.isTradingHours()
                delay(60_000) // Check every minute
            }
        }
    }

    /**
     * Get current ticker.
     */
    fun getTicker(): String? = currentTicker

    /**
     * Load realtime supply data.
     */
    private fun loadRealtimeSupply(ticker: String) {
        viewModelScope.launch {
            // Check if feature is enabled
            val isEnabled = featureFlagRepo.isEnabled(FeatureFlags.ENABLE_REALTIME_SUPPLY)
            if (!isEnabled) {
                _state.value = RealtimeSupplyState.FeatureDisabled
                return@launch
            }

            _state.value = RealtimeSupplyState.Loading

            getRealtimeSupplySummaryUC(ticker, useCache = true)
                .onSuccess { summary ->
                    _state.value = RealtimeSupplyState.Success(summary)
                    Log.d(TAG, "loadRealtimeSupply() success: ticker=$ticker, " +
                        "netBuyAmount=${summary.netBuyAmountBillion}억, " +
                        "ratio=${summary.netBuyRatio}")
                }
                .onFailure { e ->
                    _state.value = RealtimeSupplyState.Error(
                        code = extractErrorCode(e),
                        msg = e.message ?: "실시간 수급 조회 실패"
                    )
                    Log.e(TAG, "loadRealtimeSupply() failed: ${e.message}", e)
                }
        }
    }

    /**
     * Refresh realtime supply data (force fetch from API).
     */
    fun refresh() {
        val ticker = currentTicker ?: return

        viewModelScope.launch {
            _isRefreshing.value = true

            refreshRealtimeSupplyUC(ticker)
                .onSuccess { summary ->
                    _state.value = RealtimeSupplyState.Success(summary)
                    Log.d(TAG, "refresh() success: ticker=$ticker")
                }
                .onFailure { e ->
                    _state.value = RealtimeSupplyState.Error(
                        code = extractErrorCode(e),
                        msg = e.message ?: "새로고침 실패"
                    )
                    Log.e(TAG, "refresh() failed: ${e.message}", e)
                }

            _isRefreshing.value = false
        }
    }

    /**
     * Retry after error.
     */
    fun retry() {
        currentTicker?.let { loadRealtimeSupply(it) }
    }

    /**
     * Enable/disable auto-refresh.
     */
    fun setAutoRefreshEnabled(enabled: Boolean) {
        _autoRefreshSettings.value = _autoRefreshSettings.value.copy(enabled = enabled)
        if (enabled) {
            startAutoRefresh()
        } else {
            stopAutoRefresh()
        }
    }

    /**
     * Set auto-refresh interval.
     */
    fun setAutoRefreshInterval(intervalMs: Long) {
        val clampedInterval = intervalMs.coerceIn(
            AutoRefreshSettings.MIN_REFRESH_INTERVAL_MS,
            AutoRefreshSettings.MAX_REFRESH_INTERVAL_MS
        )
        _autoRefreshSettings.value = _autoRefreshSettings.value.copy(intervalMs = clampedInterval)

        // Restart auto-refresh if enabled
        if (_autoRefreshSettings.value.enabled) {
            startAutoRefresh()
        }
    }

    /**
     * Start auto-refresh.
     */
    private fun startAutoRefresh() {
        stopAutoRefresh()

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(_autoRefreshSettings.value.intervalMs)

                // Only refresh during trading hours
                if (TradingHours.isTradingHours() && currentTicker != null) {
                    Log.d(TAG, "Auto-refresh triggered for ticker=$currentTicker")
                    refresh()
                }
            }
        }

        Log.d(TAG, "Auto-refresh started with interval=${_autoRefreshSettings.value.intervalMs}ms")
    }

    /**
     * Stop auto-refresh.
     */
    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        Log.d(TAG, "Auto-refresh stopped")
    }

    /**
     * Get trading hours string.
     */
    fun getTradingHoursString(): String {
        return TradingHours.getTradingHoursString()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }

    private fun extractErrorCode(e: Throwable): String {
        val message = e.message ?: return "UNKNOWN"

        // Try to extract error code from bracket format: [ERROR_CODE]
        val bracketRegex = """\[([A-Z_]+)]""".toRegex()
        bracketRegex.find(message)?.groupValues?.getOrNull(1)?.let {
            return it
        }

        // Map known exception types to error codes
        return when (e) {
            is java.net.SocketTimeoutException -> "TIMEOUT"
            is java.net.UnknownHostException -> "NETWORK_ERROR"
            is kotlinx.coroutines.TimeoutCancellationException -> "TIMEOUT"
            else -> "UNKNOWN"
        }
    }
}
