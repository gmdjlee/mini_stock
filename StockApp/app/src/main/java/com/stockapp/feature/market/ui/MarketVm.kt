package com.stockapp.feature.market.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.feature.market.domain.model.BloodIndicatorHistory
import com.stockapp.feature.market.domain.model.FearGreedHistory
import com.stockapp.feature.market.domain.model.FundFlowHistory
import com.stockapp.feature.market.domain.model.MarketDateRange
import com.stockapp.feature.market.domain.model.MarketFearGreed
import com.stockapp.feature.market.domain.model.MarketTab
import com.stockapp.feature.market.domain.model.OscillatorHistory
import com.stockapp.feature.market.domain.usecase.GetBloodIndicatorUC
import com.stockapp.feature.market.domain.usecase.GetFearGreedUC
import com.stockapp.feature.market.domain.usecase.GetFundFlowUC
import com.stockapp.feature.market.domain.usecase.GetOscillatorUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketVm @Inject constructor(
    private val getOscillatorUC: GetOscillatorUC,
    private val getFearGreedUC: GetFearGreedUC,
    private val getFundFlowUC: GetFundFlowUC,
    private val getBloodIndicatorUC: GetBloodIndicatorUC
) : ViewModel() {

    // Tab selection - default to Fear/Greed (first tab)
    private val _selectedTab = MutableStateFlow(MarketTab.FEAR_GREED)
    val selectedTab: StateFlow<MarketTab> = _selectedTab.asStateFlow()

    // Date range selection
    private val _dateRange = MutableStateFlow(MarketDateRange.THREE_MONTHS)
    val dateRange: StateFlow<MarketDateRange> = _dateRange.asStateFlow()

    // Fear & Greed state
    private val _fearGreedState = MutableStateFlow<FearGreedState>(FearGreedState.Idle)
    val fearGreedState: StateFlow<FearGreedState> = _fearGreedState.asStateFlow()

    private val _fearGreedHistoryState = MutableStateFlow<FearGreedHistoryState>(FearGreedHistoryState.Idle)
    val fearGreedHistoryState: StateFlow<FearGreedHistoryState> = _fearGreedHistoryState.asStateFlow()

    // Oscillator state
    private val _oscillatorState = MutableStateFlow<OscillatorState>(OscillatorState.Idle)
    val oscillatorState: StateFlow<OscillatorState> = _oscillatorState.asStateFlow()

    // Fund Flow state
    private val _fundFlowState = MutableStateFlow<FundFlowState>(FundFlowState.Idle)
    val fundFlowState: StateFlow<FundFlowState> = _fundFlowState.asStateFlow()

    // Blood Indicator state
    private val _bloodState = MutableStateFlow<BloodState>(BloodState.Idle)
    val bloodState: StateFlow<BloodState> = _bloodState.asStateFlow()

    // Job tracking
    private var fearGreedJob: Job? = null
    private var fearGreedHistoryJob: Job? = null
    private var oscillatorJob: Job? = null
    private var fundFlowJob: Job? = null
    private var bloodJob: Job? = null

    init {
        loadFearGreed()
    }

    fun selectTab(tab: MarketTab) {
        _selectedTab.value = tab
        when (tab) {
            MarketTab.FEAR_GREED -> {
                if (_fearGreedState.value !is FearGreedState.Success) loadFearGreed()
            }
            MarketTab.OSCILLATOR -> {
                if (_oscillatorState.value !is OscillatorState.Success) loadOscillator()
            }
            MarketTab.FUND_FLOW -> {
                if (_fundFlowState.value !is FundFlowState.Success) loadFundFlow()
            }
            MarketTab.BLOOD -> {
                if (_bloodState.value !is BloodState.Success) loadBlood()
            }
        }
    }

    fun setDateRange(range: MarketDateRange) {
        _dateRange.value = range
        when (_selectedTab.value) {
            MarketTab.FEAR_GREED -> loadFearGreedHistory()
            MarketTab.OSCILLATOR -> loadOscillator()
            MarketTab.FUND_FLOW -> loadFundFlow()
            MarketTab.BLOOD -> loadBlood()
        }
    }

    fun refresh() {
        when (_selectedTab.value) {
            MarketTab.FEAR_GREED -> loadFearGreed()
            MarketTab.OSCILLATOR -> loadOscillator()
            MarketTab.FUND_FLOW -> loadFundFlow()
            MarketTab.BLOOD -> loadBlood()
        }
    }

    private fun loadFearGreed() {
        fearGreedJob?.cancel()
        fearGreedJob = viewModelScope.launch {
            _fearGreedState.value = FearGreedState.Loading
            getFearGreedUC.getLatest()
                .onSuccess { _fearGreedState.value = FearGreedState.Success(it) }
                .onFailure {
                    Log.w(TAG, "Fear & Greed load failed: ${it.message}")
                    _fearGreedState.value = FearGreedState.Error(it.message ?: "공포/탐욕 지수 로드 실패")
                }
        }
        loadFearGreedHistory()
    }

    private fun loadFearGreedHistory() {
        fearGreedHistoryJob?.cancel()
        fearGreedHistoryJob = viewModelScope.launch {
            _fearGreedHistoryState.value = FearGreedHistoryState.Loading
            getFearGreedUC.getHistory(_dateRange.value)
                .onSuccess { _fearGreedHistoryState.value = FearGreedHistoryState.Success(it) }
                .onFailure {
                    _fearGreedHistoryState.value = FearGreedHistoryState.Error(
                        it.message ?: "공포/탐욕 이력 로드 실패"
                    )
                }
        }
    }

    private fun loadOscillator() {
        oscillatorJob?.cancel()
        oscillatorJob = viewModelScope.launch {
            _oscillatorState.value = OscillatorState.Loading
            getOscillatorUC(_dateRange.value)
                .onSuccess { _oscillatorState.value = OscillatorState.Success(it) }
                .onFailure {
                    Log.w(TAG, "Oscillator load failed: ${it.message}")
                    _oscillatorState.value = OscillatorState.Error(it.message ?: "과매수/과매도 데이터 로드 실패")
                }
        }
    }

    private fun loadFundFlow() {
        fundFlowJob?.cancel()
        fundFlowJob = viewModelScope.launch {
            _fundFlowState.value = FundFlowState.Loading
            getFundFlowUC(_dateRange.value)
                .onSuccess { _fundFlowState.value = FundFlowState.Success(it) }
                .onFailure {
                    Log.w(TAG, "Fund flow load failed: ${it.message}")
                    _fundFlowState.value = FundFlowState.Error(it.message ?: "자금 동향 데이터 로드 실패")
                }
        }
    }

    private fun loadBlood() {
        bloodJob?.cancel()
        bloodJob = viewModelScope.launch {
            _bloodState.value = BloodState.Loading
            getBloodIndicatorUC(_dateRange.value)
                .onSuccess { _bloodState.value = BloodState.Success(it) }
                .onFailure {
                    Log.w(TAG, "Blood indicator load failed: ${it.message}")
                    _bloodState.value = BloodState.Error(it.message ?: "Blood Indicator 로드 실패")
                }
        }
    }

    // UI State sealed classes
    sealed class FearGreedState {
        data object Idle : FearGreedState()
        data object Loading : FearGreedState()
        data class Success(val data: MarketFearGreed) : FearGreedState()
        data class Error(val message: String) : FearGreedState()
    }

    sealed class FearGreedHistoryState {
        data object Idle : FearGreedHistoryState()
        data object Loading : FearGreedHistoryState()
        data class Success(val data: FearGreedHistory) : FearGreedHistoryState()
        data class Error(val message: String) : FearGreedHistoryState()
    }

    sealed class OscillatorState {
        data object Idle : OscillatorState()
        data object Loading : OscillatorState()
        data class Success(val data: OscillatorHistory) : OscillatorState()
        data class Error(val message: String) : OscillatorState()
    }

    sealed class FundFlowState {
        data object Idle : FundFlowState()
        data object Loading : FundFlowState()
        data class Success(val data: FundFlowHistory) : FundFlowState()
        data class Error(val message: String) : FundFlowState()
    }

    sealed class BloodState {
        data object Idle : BloodState()
        data object Loading : BloodState()
        data class Success(val data: BloodIndicatorHistory) : BloodState()
        data class Error(val message: String) : BloodState()
    }

    companion object {
        private const val TAG = "MarketVm"
    }
}
