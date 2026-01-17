package com.stockapp.feature.market.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.feature.market.domain.model.MarketSummary
import com.stockapp.feature.market.domain.model.toSummary
import com.stockapp.feature.market.domain.usecase.GetMarketIndicatorsUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sealed class for market UI state.
 */
sealed class MarketState {
    data object Loading : MarketState()
    data class Success(val summary: MarketSummary) : MarketState()
    data class Error(val code: String, val msg: String) : MarketState()
}

@HiltViewModel
class MarketVm @Inject constructor(
    private val getMarketIndicatorsUC: GetMarketIndicatorsUC
) : ViewModel() {

    private val _state = MutableStateFlow<MarketState>(MarketState.Loading)
    val state: StateFlow<MarketState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedDays = MutableStateFlow(DEFAULT_DAYS)
    val selectedDays: StateFlow<Int> = _selectedDays.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadData(useCache = false)
    }

    fun retry() {
        _state.value = MarketState.Loading
        loadData(useCache = false)
    }

    fun selectDays(days: Int) {
        if (days != _selectedDays.value) {
            _selectedDays.value = days
            _state.value = MarketState.Loading
            loadData(useCache = false, days = days)
        }
    }

    private fun loadData(useCache: Boolean = true, days: Int = _selectedDays.value) {
        viewModelScope.launch {
            try {
                getMarketIndicatorsUC(days, useCache).fold(
                    onSuccess = { indicators ->
                        _state.value = MarketState.Success(indicators.toSummary())
                    },
                    onFailure = { e ->
                        _state.value = MarketState.Error(
                            code = "ERROR",
                            msg = e.message ?: "시장 지표를 가져오는데 실패했습니다"
                        )
                    }
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    companion object {
        const val DEFAULT_DAYS = 30
        val DAYS_OPTIONS = listOf(7, 14, 30, 60, 90)
    }
}
