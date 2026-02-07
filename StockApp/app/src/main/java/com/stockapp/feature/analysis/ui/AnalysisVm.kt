package com.stockapp.feature.analysis.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.analysis.domain.model.AnalysisSummary
import com.stockapp.feature.analysis.domain.usecase.GetAnalysisSummaryUC
import com.stockapp.feature.analysis.domain.usecase.RefreshAnalysisUC
import com.stockapp.feature.realtime.domain.model.TradingHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Analysis screen state.
 */
sealed class AnalysisState {
    data object NoStock : AnalysisState()
    data object Loading : AnalysisState()
    data class Success(val summary: AnalysisSummary) : AnalysisState()
    data class Error(val code: String, val msg: String) : AnalysisState()
}

@HiltViewModel
class AnalysisVm @Inject constructor(
    private val selectedStockManager: SelectedStockManager,
    private val getAnalysisSummaryUC: GetAnalysisSummaryUC,
    private val refreshAnalysisUC: RefreshAnalysisUC
) : ViewModel() {

    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.NoStock)
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Trading hours state
    private val _isTradingHours = MutableStateFlow(TradingHours.isTradingHours())
    val isTradingHours: StateFlow<Boolean> = _isTradingHours.asStateFlow()

    // Auto refresh settings
    private val _autoRefreshEnabled = MutableStateFlow(false)
    val autoRefreshEnabled: StateFlow<Boolean> = _autoRefreshEnabled.asStateFlow()

    private var currentTicker: String? = null
    private var autoRefreshJob: Job? = null
    private var tradingHoursCheckJob: Job? = null

    init {
        // Observe selected stock changes
        viewModelScope.launch {
            selectedStockManager.selectedTicker.collect { ticker ->
                if (ticker != null && ticker != currentTicker) {
                    currentTicker = ticker
                    loadAnalysis(ticker)
                    // Auto-refresh will start if enabled and in trading hours
                    if (_autoRefreshEnabled.value && _isTradingHours.value) {
                        startAutoRefresh()
                    }
                } else if (ticker == null) {
                    currentTicker = null
                    _state.value = AnalysisState.NoStock
                    stopAutoRefresh()
                }
            }
        }

        // Start trading hours check job
        startTradingHoursCheck()
    }

    /**
     * Get current ticker.
     */
    fun getTicker(): String? = currentTicker

    /**
     * Select a ticker from deep link (P3).
     * This sets the ticker in the shared state manager.
     * Validates that the ticker matches Korean stock format (6-digit number).
     */
    fun selectTickerFromDeepLink(ticker: String) {
        if (!isValidKoreanTicker(ticker)) {
            Log.w(TAG, "Invalid deep link ticker rejected: $ticker")
            return
        }
        selectedStockManager.selectTicker(ticker)
    }

    /**
     * Enable or disable auto refresh during trading hours.
     */
    fun setAutoRefreshEnabled(enabled: Boolean) {
        _autoRefreshEnabled.value = enabled
        if (enabled && _isTradingHours.value && currentTicker != null) {
            startAutoRefresh()
        } else {
            stopAutoRefresh()
        }
    }

    /**
     * Load analysis data.
     */
    private fun loadAnalysis(ticker: String) {
        viewModelScope.launch {
            _state.value = AnalysisState.Loading

            getAnalysisSummaryUC(ticker)
                .onSuccess { summary ->
                    _state.value = AnalysisState.Success(summary)
                }
                .onFailure { e ->
                    _state.value = AnalysisState.Error(
                        code = extractErrorCode(e),
                        msg = e.message ?: "수급 분석 실패"
                    )
                }
        }
    }

    /**
     * Refresh analysis data (force fetch from API).
     */
    fun refresh() {
        val ticker = currentTicker ?: return

        viewModelScope.launch {
            _isRefreshing.value = true

            // Use getAnalysisSummaryUC which now integrates intraday data
            getAnalysisSummaryUC(ticker, useCache = false)
                .onSuccess { summary ->
                    _state.value = AnalysisState.Success(summary)
                }
                .onFailure { e ->
                    _state.value = AnalysisState.Error(
                        code = extractErrorCode(e),
                        msg = e.message ?: "새로고침 실패"
                    )
                }

            _isRefreshing.value = false
        }
    }

    /**
     * Retry after error.
     */
    fun retry() {
        currentTicker?.let { loadAnalysis(it) }
    }

    /**
     * Start periodic trading hours check.
     * Checks every minute and handles trading hours transitions.
     */
    private fun startTradingHoursCheck() {
        tradingHoursCheckJob?.cancel()
        tradingHoursCheckJob = viewModelScope.launch {
            while (isActive) {
                val wasTradingHours = _isTradingHours.value
                val nowTradingHours = TradingHours.isTradingHours()
                _isTradingHours.value = nowTradingHours

                // Handle trading hours transitions
                if (wasTradingHours && !nowTradingHours) {
                    // Market just closed - refresh to get closing data
                    stopAutoRefresh()
                    currentTicker?.let { ticker ->
                        viewModelScope.launch {
                            getAnalysisSummaryUC(ticker, useCache = false)
                                .onSuccess { summary ->
                                    _state.value = AnalysisState.Success(summary)
                                }
                        }
                    }
                } else if (!wasTradingHours && nowTradingHours) {
                    // Market just opened - start auto refresh if enabled
                    if (_autoRefreshEnabled.value && currentTicker != null) {
                        startAutoRefresh()
                    }
                }

                delay(TRADING_HOURS_CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * Start auto refresh job.
     * Refreshes data every minute during trading hours.
     */
    private fun startAutoRefresh() {
        stopAutoRefresh()
        autoRefreshJob = viewModelScope.launch {
            while (isActive && _isTradingHours.value) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                val ticker = currentTicker
                if (_isTradingHours.value && ticker != null) {
                    // Silent refresh (don't show refreshing indicator)
                    getAnalysisSummaryUC(ticker, useCache = false)
                        .onSuccess { summary ->
                            _state.value = AnalysisState.Success(summary)
                        }
                }
            }
        }
    }

    /**
     * Stop auto refresh job.
     */
    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
        tradingHoursCheckJob?.cancel()
    }

    private fun extractErrorCode(e: Throwable): String {
        val message = e.message ?: return "UNKNOWN"

        // Try to extract error code from bracket format: [ERROR_CODE]
        BRACKET_ERROR_REGEX.find(message)?.groupValues?.getOrNull(1)?.let {
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

    companion object {
        private const val TAG = "AnalysisVm"

        /** Auto refresh interval: 1 minute */
        private const val AUTO_REFRESH_INTERVAL_MS = 60_000L

        /** Trading hours check interval: 1 minute */
        private const val TRADING_HOURS_CHECK_INTERVAL_MS = 60_000L

        /** Korean stock tickers are exactly 6 digits (e.g., "005930" for Samsung). */
        private val TICKER_REGEX = Regex("^\\d{6}$")

        /** Pre-compiled regex for extracting error codes from bracket format. */
        private val BRACKET_ERROR_REGEX = """\[([A-Z_]+)]""".toRegex()

        fun isValidKoreanTicker(ticker: String): Boolean = ticker.matches(TICKER_REGEX)
    }
}
