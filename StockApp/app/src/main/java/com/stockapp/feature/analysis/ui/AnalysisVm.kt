package com.stockapp.feature.analysis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.analysis.domain.model.AnalysisSummary
import com.stockapp.feature.analysis.domain.usecase.GetAnalysisSummaryUC
import com.stockapp.feature.analysis.domain.usecase.RefreshAnalysisUC
import com.stockapp.feature.analysis.domain.model.toSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var currentTicker: String? = null

    init {
        // Observe selected stock changes
        viewModelScope.launch {
            selectedStockManager.selectedTicker.collect { ticker ->
                if (ticker != null && ticker != currentTicker) {
                    currentTicker = ticker
                    loadAnalysis(ticker)
                } else if (ticker == null) {
                    currentTicker = null
                    _state.value = AnalysisState.NoStock
                }
            }
        }
    }

    /**
     * Get current ticker.
     */
    fun getTicker(): String? = currentTicker

    /**
     * Select a ticker from deep link (P3).
     * This sets the ticker in the shared state manager.
     */
    fun selectTickerFromDeepLink(ticker: String) {
        selectedStockManager.selectTicker(ticker)
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

            refreshAnalysisUC(ticker)
                .onSuccess { data ->
                    _state.value = AnalysisState.Success(data.toSummary())
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
