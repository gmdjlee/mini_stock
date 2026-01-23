package com.stockapp.feature.ranking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.core.api.ApiError
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.ranking.domain.model.ExchangeType
import com.stockapp.feature.ranking.domain.model.InvestorType
import com.stockapp.feature.ranking.domain.model.ItemCount
import com.stockapp.feature.ranking.domain.model.MarketType
import com.stockapp.feature.ranking.domain.model.OrderBookDirection
import com.stockapp.feature.ranking.domain.model.RankingItem
import com.stockapp.feature.ranking.domain.model.RankingResult
import com.stockapp.feature.ranking.domain.model.RankingType
import com.stockapp.feature.ranking.domain.model.TradeDirection
import com.stockapp.feature.ranking.domain.model.ValueType
import com.stockapp.feature.ranking.domain.usecase.GetRankingUC
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Ranking screen.
 */
sealed class RankingState {
    data object Loading : RankingState()
    data object NoApiKey : RankingState()
    data class Success(val result: RankingResult) : RankingState()
    data class Error(val message: String) : RankingState()
}

@HiltViewModel
class RankingVm @Inject constructor(
    private val getRankingUC: GetRankingUC,
    private val settingsRepo: SettingsRepo,
    private val selectedStockManager: SelectedStockManager
) : ViewModel() {

    private val _state = MutableStateFlow<RankingState>(RankingState.Loading)
    val state: StateFlow<RankingState> = _state.asStateFlow()

    private val _rankingType = MutableStateFlow(RankingType.DAILY_VOLUME_TOP)
    val rankingType: StateFlow<RankingType> = _rankingType.asStateFlow()

    private val _marketType = MutableStateFlow(MarketType.KOSPI)
    val marketType: StateFlow<MarketType> = _marketType.asStateFlow()

    private val _exchangeType = MutableStateFlow(ExchangeType.KRX_MOCK)
    val exchangeType: StateFlow<ExchangeType> = _exchangeType.asStateFlow()

    private val _itemCount = MutableStateFlow(ItemCount.TEN)
    val itemCount: StateFlow<ItemCount> = _itemCount.asStateFlow()

    private val _investmentMode = MutableStateFlow(InvestmentMode.MOCK)
    val investmentMode: StateFlow<InvestmentMode> = _investmentMode.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ka10021 (Order Book Surge) specific filter
    private val _orderBookDirection = MutableStateFlow(OrderBookDirection.BUY)
    val orderBookDirection: StateFlow<OrderBookDirection> = _orderBookDirection.asStateFlow()

    // ka90009 (Foreign/Institution Top) specific filters
    private val _investorType = MutableStateFlow(InvestorType.FOREIGN)
    val investorType: StateFlow<InvestorType> = _investorType.asStateFlow()

    private val _tradeDirection = MutableStateFlow(TradeDirection.NET_BUY)
    val tradeDirection: StateFlow<TradeDirection> = _tradeDirection.asStateFlow()

    private val _valueType = MutableStateFlow(ValueType.AMOUNT)
    val valueType: StateFlow<ValueType> = _valueType.asStateFlow()

    init {
        checkApiKeyAndLoad()
    }

    private fun checkApiKeyAndLoad() {
        viewModelScope.launch {
            try {
                val config = settingsRepo.getApiKeyConfig().first()
                if (!config.isValid()) {
                    _state.value = RankingState.NoApiKey
                    return@launch
                }

                _investmentMode.value = config.investmentMode

                // Set default exchange type based on investment mode
                _exchangeType.value = when (config.investmentMode) {
                    InvestmentMode.MOCK -> ExchangeType.KRX_MOCK
                    InvestmentMode.PRODUCTION -> ExchangeType.KRX
                }

                loadRanking()
            } catch (e: Exception) {
                _state.value = RankingState.Error(e.message ?: "설정 로드 오류")
            }
        }
    }

    fun onRankingTypeChange(type: RankingType) {
        _rankingType.value = type
        loadRanking()
    }

    fun onMarketTypeChange(type: MarketType) {
        _marketType.value = type
        loadRanking()
    }

    fun onExchangeTypeChange(type: ExchangeType) {
        _exchangeType.value = type
        loadRanking()
    }

    fun onOrderBookDirectionChange(direction: OrderBookDirection) {
        _orderBookDirection.value = direction
        loadRanking()
    }

    fun onInvestorTypeChange(type: InvestorType) {
        _investorType.value = type
        loadRanking()
    }

    fun onTradeDirectionChange(direction: TradeDirection) {
        _tradeDirection.value = direction
        loadRanking()
    }

    fun onValueTypeChange(type: ValueType) {
        _valueType.value = type
        loadRanking()
    }

    fun onItemCountChange(count: ItemCount) {
        _itemCount.value = count
        // If we already have data, just filter it locally
        val currentState = _state.value
        if (currentState is RankingState.Success) {
            val filteredItems = currentState.result.items.take(count.value)
            _state.value = RankingState.Success(
                currentState.result.copy(items = filteredItems)
            )
        } else {
            loadRanking()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadRanking()
            _isRefreshing.value = false
        }
    }

    private fun loadRanking() {
        viewModelScope.launch {
            _state.value = RankingState.Loading

            val result = getRankingUC(
                rankingType = _rankingType.value,
                marketType = _marketType.value,
                exchangeType = _exchangeType.value,
                itemCount = _itemCount.value,
                orderBookDirection = _orderBookDirection.value,
                investorType = _investorType.value,
                tradeDirection = _tradeDirection.value,
                valueType = _valueType.value
            )

            result.fold(
                onSuccess = { data ->
                    _state.value = RankingState.Success(data)
                },
                onFailure = { error ->
                    val message = when (error) {
                        is ApiError.NoApiKeyError -> {
                            _state.value = RankingState.NoApiKey
                            return@fold
                        }
                        is ApiError.NetworkError -> error.message
                        is ApiError.AuthError -> "인증 오류: ${error.message}"
                        is ApiError.ApiCallError -> "API 오류: ${error.message}"
                        else -> error.message ?: "알 수 없는 오류"
                    }
                    _state.value = RankingState.Error(message)
                }
            )
        }
    }

    fun onStockClick(item: RankingItem) {
        // Select the stock for Analysis screen
        selectedStockManager.selectTicker(item.ticker, item.name)
    }

    /**
     * Get available exchange types based on investment mode.
     */
    fun getAvailableExchangeTypes(): List<ExchangeType> {
        return when (_investmentMode.value) {
            InvestmentMode.MOCK -> listOf(ExchangeType.KRX_MOCK)
            InvestmentMode.PRODUCTION -> listOf(ExchangeType.KRX, ExchangeType.NXT)
        }
    }

    /**
     * Check if current ranking type is Order Book Surge (ka10021).
     */
    fun isOrderBookSurgeType(): Boolean {
        return _rankingType.value == RankingType.ORDER_BOOK_SURGE
    }

    /**
     * Check if current ranking type supports ka90009 filters.
     */
    fun isForeignInstitutionType(): Boolean {
        return _rankingType.value == RankingType.FOREIGN_INSTITUTION_TOP
    }

    /**
     * Get available market types for current ranking type.
     * ka90009 supports ALL market, others only support KOSPI/KOSDAQ.
     */
    fun getAvailableMarketTypes(): List<MarketType> {
        return if (_rankingType.value == RankingType.FOREIGN_INSTITUTION_TOP) {
            listOf(MarketType.KOSPI, MarketType.KOSDAQ, MarketType.ALL)
        } else {
            listOf(MarketType.KOSPI, MarketType.KOSDAQ)
        }
    }
}
