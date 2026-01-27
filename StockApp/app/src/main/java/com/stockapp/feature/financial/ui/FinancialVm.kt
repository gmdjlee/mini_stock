package com.stockapp.feature.financial.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.financial.domain.model.FinancialSummary
import com.stockapp.feature.financial.domain.model.FinancialTab
import com.stockapp.feature.financial.domain.usecase.GetFinancialSummaryUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Financial screen.
 */
sealed class FinancialState {
    data object NoStock : FinancialState()
    data object Loading : FinancialState()
    data object NoApiKey : FinancialState()
    data class Success(val summary: FinancialSummary) : FinancialState()
    data class Error(val message: String) : FinancialState()
}

/**
 * ViewModel for Financial screen.
 */
@HiltViewModel
class FinancialVm @Inject constructor(
    private val selectedStockManager: SelectedStockManager,
    private val getFinancialSummaryUC: GetFinancialSummaryUC
) : ViewModel() {

    private val _state = MutableStateFlow<FinancialState>(FinancialState.NoStock)
    val state: StateFlow<FinancialState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow(FinancialTab.PROFITABILITY)
    val selectedTab: StateFlow<FinancialTab> = _selectedTab.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentTicker: String? = null
    private var currentName: String? = null

    init {
        // Observe selected stock changes
        viewModelScope.launch {
            selectedStockManager.selectedStock.collect { stock ->
                if (stock != null) {
                    currentTicker = stock.ticker
                    currentName = stock.name
                    loadFinancialData(stock.ticker, stock.name, useCache = true)
                } else {
                    currentTicker = null
                    currentName = null
                    _state.value = FinancialState.NoStock
                }
            }
        }
    }

    /**
     * Select a tab.
     */
    fun selectTab(tab: FinancialTab) {
        _selectedTab.value = tab
    }

    /**
     * Refresh financial data.
     */
    fun refresh() {
        val ticker = currentTicker ?: return
        val name = currentName ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            loadFinancialData(ticker, name, useCache = false)
            _isRefreshing.value = false
        }
    }

    /**
     * Retry loading after error.
     */
    fun retry() {
        val ticker = currentTicker ?: return
        val name = currentName ?: return
        loadFinancialData(ticker, name, useCache = false)
    }

    private fun loadFinancialData(ticker: String, name: String, useCache: Boolean) {
        viewModelScope.launch {
            _state.value = FinancialState.Loading

            val result = if (useCache) {
                getFinancialSummaryUC(ticker, name, useCache = true)
            } else {
                getFinancialSummaryUC.refresh(ticker, name)
            }

            _state.value = result.fold(
                onSuccess = { summary ->
                    if (summary.periods.isEmpty()) {
                        FinancialState.Error("재무정보를 찾을 수 없습니다.")
                    } else {
                        FinancialState.Success(summary)
                    }
                },
                onFailure = { error ->
                    val message = when {
                        error.message?.contains("API key") == true -> {
                            return@fold FinancialState.NoApiKey
                        }
                        error.message?.contains("network") == true ||
                        error.message?.contains("Network") == true -> {
                            "네트워크 연결을 확인해주세요."
                        }
                        else -> {
                            error.message ?: "알 수 없는 오류가 발생했습니다."
                        }
                    }
                    FinancialState.Error(message)
                }
            )
        }
    }
}
