package com.stockapp.feature.indicator.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.feature.indicator.domain.model.DemarkSummary
import com.stockapp.feature.indicator.domain.model.ElderSummary
import com.stockapp.feature.indicator.domain.model.IndicatorType
import com.stockapp.feature.indicator.domain.model.TrendSummary
import com.stockapp.feature.indicator.domain.model.toSummary
import com.stockapp.feature.indicator.domain.usecase.GetDemarkUC
import com.stockapp.feature.indicator.domain.usecase.GetElderUC
import com.stockapp.feature.indicator.domain.usecase.GetTrendUC
import com.stockapp.nav.NavArgs
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

@HiltViewModel
class IndicatorVm @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTrendUC: GetTrendUC,
    private val getElderUC: GetElderUC,
    private val getDemarkUC: GetDemarkUC
) : ViewModel() {

    private val ticker: String = savedStateHandle[NavArgs.TICKER] ?: ""

    private val _state = MutableStateFlow<IndicatorState>(IndicatorState.Loading)
    val state: StateFlow<IndicatorState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedTab = MutableStateFlow(IndicatorType.TREND)
    val selectedTab: StateFlow<IndicatorType> = _selectedTab.asStateFlow()

    // Store loaded data
    private var trendData: TrendSummary? = null
    private var elderData: ElderSummary? = null
    private var demarkData: DemarkSummary? = null
    private var stockName: String = ""

    init {
        loadInitialData()
    }

    fun selectTab(tab: IndicatorType) {
        _selectedTab.value = tab
        loadTabData(tab, useCache = true)
    }

    fun refresh() {
        _isRefreshing.value = true
        loadTabData(_selectedTab.value, useCache = false)
    }

    fun retry() {
        _state.value = IndicatorState.Loading
        loadTabData(_selectedTab.value, useCache = false)
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            loadTabData(IndicatorType.TREND, useCache = true)
        }
    }

    private fun loadTabData(tab: IndicatorType, useCache: Boolean) {
        viewModelScope.launch {
            try {
                when (tab) {
                    IndicatorType.TREND -> loadTrend(useCache)
                    IndicatorType.ELDER -> loadElder(useCache)
                    IndicatorType.DEMARK -> loadDemark(useCache)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun loadTrend(useCache: Boolean) {
        if (useCache && trendData != null) {
            updateSuccessState()
            return
        }

        getTrendUC(ticker, DAYS, TIMEFRAME, useCache).fold(
            onSuccess = { trend ->
                trendData = trend.toSummary()
                stockName = trend.ticker
                updateSuccessState()
            },
            onFailure = { e ->
                handleError(e)
            }
        )
    }

    private suspend fun loadElder(useCache: Boolean) {
        if (useCache && elderData != null) {
            updateSuccessState()
            return
        }

        getElderUC(ticker, DAYS, TIMEFRAME, useCache).fold(
            onSuccess = { elder ->
                elderData = elder.toSummary()
                if (stockName.isEmpty()) stockName = elder.ticker
                updateSuccessState()
            },
            onFailure = { e ->
                handleError(e)
            }
        )
    }

    private suspend fun loadDemark(useCache: Boolean) {
        if (useCache && demarkData != null) {
            updateSuccessState()
            return
        }

        getDemarkUC(ticker, DAYS, TIMEFRAME, useCache).fold(
            onSuccess = { demark ->
                demarkData = demark.toSummary()
                if (stockName.isEmpty()) stockName = demark.ticker
                updateSuccessState()
            },
            onFailure = { e ->
                handleError(e)
            }
        )
    }

    private fun updateSuccessState() {
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
        private const val TIMEFRAME = "daily"
    }
}
