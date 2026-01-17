package com.stockapp.feature.analysis.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.feature.analysis.domain.model.AnalysisSummary
import com.stockapp.feature.analysis.domain.usecase.GetAnalysisSummaryUC
import com.stockapp.feature.analysis.domain.usecase.RefreshAnalysisUC
import com.stockapp.feature.analysis.domain.model.toSummary
import com.stockapp.nav.NavArgs
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
    data object Loading : AnalysisState()
    data class Success(val summary: AnalysisSummary) : AnalysisState()
    data class Error(val code: String, val msg: String) : AnalysisState()
}

@HiltViewModel
class AnalysisVm @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnalysisSummaryUC: GetAnalysisSummaryUC,
    private val refreshAnalysisUC: RefreshAnalysisUC
) : ViewModel() {

    val ticker: String = savedStateHandle.get<String>(NavArgs.TICKER) ?: ""

    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Loading)
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        if (ticker.isNotBlank()) {
            loadAnalysis()
        } else {
            _state.value = AnalysisState.Error(
                code = "INVALID_ARG",
                msg = "종목코드가 없습니다"
            )
        }
    }

    /**
     * Load analysis data.
     */
    fun loadAnalysis() {
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
        loadAnalysis()
    }

    private fun extractErrorCode(e: Throwable): String {
        return when {
            e.message?.contains("[") == true -> {
                e.message?.substringAfter("[")?.substringBefore("]") ?: "UNKNOWN"
            }
            else -> "UNKNOWN"
        }
    }
}
