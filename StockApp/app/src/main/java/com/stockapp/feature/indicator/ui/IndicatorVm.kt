package com.stockapp.feature.indicator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.indicator.domain.model.DemarkSummary
import com.stockapp.feature.indicator.domain.model.ElderSummary
import com.stockapp.feature.indicator.domain.model.IndicatorType
import com.stockapp.feature.indicator.domain.model.TrendSummary
import com.stockapp.feature.indicator.domain.model.toSummary
import com.stockapp.feature.indicator.domain.usecase.GetDemarkUC
import com.stockapp.feature.indicator.domain.usecase.GetElderUC
import com.stockapp.feature.indicator.domain.usecase.GetTrendUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sealed class for indicator UI state.
 */
sealed class IndicatorState {
    data object NoStock : IndicatorState()
    data object Loading : IndicatorState()
    data class Success(
        val stockName: String,
        val ticker: String,
        val selectedTab: IndicatorType,
        val trend: TrendSummary? = null,
        val elder: ElderSummary? = null,
        val demark: DemarkSummary? = null
    ) : IndicatorState()
    data class Error(val code: String, val msg: String) : IndicatorState()
}

/**
 * Timeframe for indicator data.
 * Reference recommends WEEKLY for Trend and Elder indicators.
 */
enum class Timeframe(val label: String, val apiValue: String) {
    DAILY("일봉", "daily"),
    WEEKLY("주봉", "weekly"),
    MONTHLY("월봉", "monthly")
}

@HiltViewModel
class IndicatorVm @Inject constructor(
    private val selectedStockManager: SelectedStockManager,
    private val getTrendUC: GetTrendUC,
    private val getElderUC: GetElderUC,
    private val getDemarkUC: GetDemarkUC
) : ViewModel() {

    private val _state = MutableStateFlow<IndicatorState>(IndicatorState.NoStock)
    val state: StateFlow<IndicatorState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedTab = MutableStateFlow(IndicatorType.TREND)
    val selectedTab: StateFlow<IndicatorType> = _selectedTab.asStateFlow()

    // Default to WEEKLY as recommended by reference (Trend/Elder use weekly data)
    private val _selectedTimeframe = MutableStateFlow(Timeframe.WEEKLY)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    // Store loaded data
    private var trendData: TrendSummary? = null
    private var elderData: ElderSummary? = null
    private var demarkData: DemarkSummary? = null
    private var stockName: String = ""
    private var currentTicker: String? = null

    init {
        // Observe selected stock changes
        viewModelScope.launch {
            selectedStockManager.selectedStock.collect { stock ->
                if (stock != null && stock.ticker != currentTicker) {
                    currentTicker = stock.ticker
                    // Update stock name from selected stock
                    stockName = stock.name
                    // Clear cached data for new stock
                    clearCachedData()
                    loadInitialData(stock.ticker)
                } else if (stock == null) {
                    currentTicker = null
                    clearCachedData()
                    _state.value = IndicatorState.NoStock
                }
            }
        }
    }

    private fun clearCachedData() {
        trendData = null
        elderData = null
        demarkData = null
        stockName = ""
    }

    fun selectTab(tab: IndicatorType) {
        if (_selectedTab.value == tab) return // Skip if same tab

        _selectedTab.value = tab

        // Check if we already have cached data for this tab
        val hasCachedData = when (tab) {
            IndicatorType.TREND -> trendData != null
            IndicatorType.ELDER -> elderData != null
            IndicatorType.DEMARK -> demarkData != null
        }

        currentTicker?.let { ticker ->
            if (hasCachedData) {
                // Immediately update state with cached data (no loading)
                updateSuccessState(ticker)
            } else {
                // Need to load data
                loadTabData(ticker, tab, useCache = true)
            }
        }
    }

    fun selectTimeframe(timeframe: Timeframe) {
        _selectedTimeframe.value = timeframe
        // Clear cached data and reload with new timeframe
        clearCachedData()
        currentTicker?.let { ticker ->
            loadTabData(ticker, _selectedTab.value, useCache = false)
        }
    }

    fun refresh() {
        val ticker = currentTicker ?: return
        _isRefreshing.value = true
        loadTabData(ticker, _selectedTab.value, useCache = false)
    }

    fun retry() {
        val ticker = currentTicker ?: return
        _state.value = IndicatorState.Loading
        loadTabData(ticker, _selectedTab.value, useCache = false)
    }

    private fun loadInitialData(ticker: String) {
        // Load data for currently selected tab (not always TREND)
        // This fixes the infinite loading issue when user had selected a different tab
        loadTabData(ticker, _selectedTab.value, useCache = true)
    }

    private fun loadTabData(ticker: String, tab: IndicatorType, useCache: Boolean) {
        viewModelScope.launch {
            try {
                when (tab) {
                    IndicatorType.TREND -> loadTrend(ticker, useCache)
                    IndicatorType.ELDER -> loadElder(ticker, useCache)
                    IndicatorType.DEMARK -> loadDemark(ticker, useCache)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun loadTrend(ticker: String, useCache: Boolean) {
        if (useCache && trendData != null) {
            updateSuccessState(ticker)
            return
        }

        val timeframe = _selectedTimeframe.value.apiValue
        getTrendUC(ticker, DAYS, timeframe, useCache).fold(
            onSuccess = { trend ->
                trendData = trend.toSummary()
                // Only update stockName if empty (prefer name from SelectedStockManager)
                if (stockName.isEmpty()) stockName = trend.ticker
                updateSuccessState(ticker)
            },
            onFailure = { e ->
                handleError(e)
            }
        )
    }

    private suspend fun loadElder(ticker: String, useCache: Boolean) {
        if (useCache && elderData != null) {
            updateSuccessState(ticker)
            return
        }

        val timeframe = _selectedTimeframe.value.apiValue
        getElderUC(ticker, DAYS, timeframe, useCache).fold(
            onSuccess = { elder ->
                elderData = elder.toSummary()
                if (stockName.isEmpty()) stockName = elder.ticker
                updateSuccessState(ticker)
            },
            onFailure = { e ->
                handleError(e)
            }
        )
    }

    private suspend fun loadDemark(ticker: String, useCache: Boolean) {
        if (useCache && demarkData != null) {
            updateSuccessState(ticker)
            return
        }

        val timeframe = _selectedTimeframe.value.apiValue
        getDemarkUC(ticker, DAYS, timeframe, useCache).fold(
            onSuccess = { demark ->
                demarkData = demark.toSummary()
                if (stockName.isEmpty()) stockName = demark.ticker
                updateSuccessState(ticker)
            },
            onFailure = { e ->
                handleError(e)
            }
        )
    }

    private fun updateSuccessState(ticker: String) {
        _state.value = IndicatorState.Success(
            stockName = stockName.ifEmpty { ticker },
            ticker = ticker,
            selectedTab = _selectedTab.value,
            trend = trendData,
            elder = elderData,
            demark = demarkData
        )
    }

    private fun handleError(e: Throwable) {
        _state.value = IndicatorState.Error(
            code = "ERROR",
            msg = e.message ?: "알 수 없는 오류가 발생했습니다"
        )
    }

    companion object {
        private const val DAYS = 180
    }
}
