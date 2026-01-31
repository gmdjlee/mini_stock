package com.stockapp.feature.etf.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stockapp.core.db.entity.EtfCollectionHistoryEntity
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfConstituent
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.model.EtfKeyword
import com.stockapp.feature.etf.domain.model.FilterType
import com.stockapp.feature.etf.domain.model.MissingDatesResult
import com.stockapp.feature.etf.domain.model.RankingSortColumn
import com.stockapp.feature.etf.domain.model.RankingSortState
import com.stockapp.feature.etf.ui.detail.EtfDetailState
import com.stockapp.feature.etf.ui.detail.StockDetailData
import com.stockapp.feature.etf.ui.detail.StockDetailState
import com.stockapp.feature.etf.ui.tabs.ThemeListState
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import com.stockapp.feature.etf.domain.repo.EtfRepository
import com.stockapp.feature.etf.domain.model.DateRangeOption
import com.stockapp.feature.etf.domain.usecase.CashDepositTrendResult
import com.stockapp.feature.etf.domain.usecase.EnhancedStockRanking
import com.stockapp.feature.etf.domain.usecase.GetCashDepositTrendUC
import com.stockapp.feature.etf.domain.usecase.GetStockAnalysisUC
import com.stockapp.feature.etf.domain.usecase.GetStockChangesUC
import com.stockapp.feature.etf.domain.usecase.GetStockRankingUC
import com.stockapp.feature.etf.domain.usecase.StockChangesResult
import com.stockapp.feature.etf.domain.usecase.StockRankingResult
import com.stockapp.feature.etf.ui.tabs.statistics.CashDepositState
import com.stockapp.feature.etf.ui.tabs.statistics.StatisticsSubTab
import com.stockapp.feature.etf.ui.tabs.statistics.StockAnalysisState
import com.stockapp.feature.etf.worker.EtfCollectionWorker
import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.repo.SearchRepo
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tab definitions for ETF screen.
 */
enum class EtfTab(val title: String) {
    STATISTICS("통계"),
    THEME_LIST("테마 목록")
}

/**
 * Collection status UI state.
 */
sealed class CollectionState {
    data object Idle : CollectionState()
    data class Collecting(val current: Int, val total: Int) : CollectionState()
    data class Success(val etfCount: Int, val constituentCount: Int) : CollectionState()
    data class Error(val message: String) : CollectionState()
}

/**
 * Stock ranking UI state.
 */
sealed class RankingState {
    data object Loading : RankingState()
    data object NoData : RankingState()
    data class Success(val result: StockRankingResult) : RankingState()
    data class Error(val message: String) : RankingState()
}

/**
 * Stock changes UI state.
 */
sealed class ChangesState {
    data object Loading : ChangesState()
    data object NoData : ChangesState()
    data class Success(val result: StockChangesResult) : ChangesState()
    data class Error(val message: String) : ChangesState()
}

/**
 * ETF settings configuration.
 */
data class EtfSettingsConfig(
    val isAutoCollectionEnabled: Boolean = false,
    val collectionHour: Int = 6,
    val collectionMinute: Int = 0,
    val activeOnly: Boolean = false,
    val dataRetentionDays: Int = 30,
    val includeKeywords: List<EtfKeyword> = emptyList(),
    val excludeKeywords: List<EtfKeyword> = emptyList()
) {
    val collectionTimeDisplay: String
        get() = String.format("%02d:%02d", collectionHour, collectionMinute)
}

/**
 * Collection history item for display.
 */
data class CollectionHistoryItem(
    val id: Long,
    val date: String,
    val etfCount: Int,
    val constituentCount: Int,
    val status: CollectionStatus,
    val errorMessage: String?,
    val startedAt: Long,
    val completedAt: Long?,
    val durationSeconds: Long?
) {
    val durationDisplay: String
        get() = durationSeconds?.let {
            if (it >= 60) "${it / 60}분 ${it % 60}초" else "${it}초"
        } ?: ""
}

@HiltViewModel
class EtfVm @Inject constructor(
    @ApplicationContext private val context: Context,
    private val etfRepository: EtfRepository,
    private val etfCollectorRepo: EtfCollectorRepo,
    private val getStockRankingUC: GetStockRankingUC,
    private val getStockChangesUC: GetStockChangesUC,
    private val getCashDepositTrendUC: GetCashDepositTrendUC,
    private val getStockAnalysisUC: GetStockAnalysisUC,
    private val settingsRepo: SettingsRepo,
    private val selectedStockManager: SelectedStockManager,
    private val searchRepo: SearchRepo
) : ViewModel() {

    // Tab selection
    private val _selectedTab = MutableStateFlow(EtfTab.STATISTICS)
    val selectedTab: StateFlow<EtfTab> = _selectedTab.asStateFlow()

    // Collection state
    private val _collectionState = MutableStateFlow<CollectionState>(CollectionState.Idle)
    val collectionState: StateFlow<CollectionState> = _collectionState.asStateFlow()

    // Collection history
    val collectionHistory: StateFlow<List<CollectionHistoryItem>> = etfCollectorRepo
        .observeCollectionHistory(10)
        .map { entities -> entities.map { it.toHistoryItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Missing dates analysis result
    private val _missingDatesResult = MutableStateFlow<MissingDatesResult?>(null)
    val missingDatesResult: StateFlow<MissingDatesResult?> = _missingDatesResult.asStateFlow()

    // Stock ranking state
    private val _rankingState = MutableStateFlow<RankingState>(RankingState.Loading)
    val rankingState: StateFlow<RankingState> = _rankingState.asStateFlow()

    // Stock ranking sort state
    private val _rankingSortState = MutableStateFlow(RankingSortState())
    val rankingSortState: StateFlow<RankingSortState> = _rankingSortState.asStateFlow()

    // Stock changes state
    private val _changesState = MutableStateFlow<ChangesState>(ChangesState.Loading)
    val changesState: StateFlow<ChangesState> = _changesState.asStateFlow()

    // Settings state
    private val _settingsConfig = MutableStateFlow(EtfSettingsConfig())
    val settingsConfig: StateFlow<EtfSettingsConfig> = _settingsConfig.asStateFlow()

    // Keywords from repository
    val keywords: StateFlow<List<EtfKeyword>> = etfRepository
        .observeEnabledKeywords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Refreshing state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Time picker dialog
    private val _showTimePicker = MutableStateFlow(false)
    val showTimePicker: StateFlow<Boolean> = _showTimePicker.asStateFlow()

    // Add keyword dialog
    private val _showAddKeywordDialog = MutableStateFlow(false)
    val showAddKeywordDialog: StateFlow<Boolean> = _showAddKeywordDialog.asStateFlow()

    private val _addKeywordType = MutableStateFlow(FilterType.INCLUDE)
    val addKeywordType: StateFlow<FilterType> = _addKeywordType.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Stock detail bottom sheet state
    private val _showStockDetail = MutableStateFlow(false)
    val showStockDetail: StateFlow<Boolean> = _showStockDetail.asStateFlow()

    private val _stockDetailState = MutableStateFlow<StockDetailState>(StockDetailState.Loading)
    val stockDetailState: StateFlow<StockDetailState> = _stockDetailState.asStateFlow()

    // ==================== Statistics Tab States ====================

    // Statistics sub-tab selection
    private val _selectedSubTab = MutableStateFlow(StatisticsSubTab.AMOUNT_RANKING)
    val selectedSubTab: StateFlow<StatisticsSubTab> = _selectedSubTab.asStateFlow()

    // Date range selection
    private val _selectedDateRange = MutableStateFlow(DateRangeOption.WEEK)
    val selectedDateRange: StateFlow<DateRangeOption> = _selectedDateRange.asStateFlow()

    // Date info for display
    private val _currentDate = MutableStateFlow<String?>(null)
    val currentDate: StateFlow<String?> = _currentDate.asStateFlow()

    private val _previousDate = MutableStateFlow<String?>(null)
    val previousDate: StateFlow<String?> = _previousDate.asStateFlow()

    // Cash deposit state
    private val _cashDepositState = MutableStateFlow<CashDepositState>(CashDepositState.Loading)
    val cashDepositState: StateFlow<CashDepositState> = _cashDepositState.asStateFlow()

    private val _isCashDepositRefreshing = MutableStateFlow(false)
    val isCashDepositRefreshing: StateFlow<Boolean> = _isCashDepositRefreshing.asStateFlow()

    // Stock analysis state
    private val _stockAnalysisState = MutableStateFlow<StockAnalysisState>(StockAnalysisState.Initial)
    val stockAnalysisState: StateFlow<StockAnalysisState> = _stockAnalysisState.asStateFlow()

    private val _stockSearchQuery = MutableStateFlow("")
    val stockSearchQuery: StateFlow<String> = _stockSearchQuery.asStateFlow()

    // Stock suggestions for autocomplete
    private val _stockSuggestions = MutableStateFlow<List<Stock>>(emptyList())
    val stockSuggestions: StateFlow<List<Stock>> = _stockSuggestions.asStateFlow()

    private val _isSuggestionsLoading = MutableStateFlow(false)
    val isSuggestionsLoading: StateFlow<Boolean> = _isSuggestionsLoading.asStateFlow()

    private var suggestionSearchJob: Job? = null
    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }

    // ==================== Theme List Tab States ====================

    // Theme list state
    private val _themeListState = MutableStateFlow<ThemeListState>(ThemeListState.Loading)
    val themeListState: StateFlow<ThemeListState> = _themeListState.asStateFlow()

    // ETF search query for theme list
    private val _etfSearchQuery = MutableStateFlow("")
    val etfSearchQuery: StateFlow<String> = _etfSearchQuery.asStateFlow()

    // ETF detail bottom sheet state
    private val _showEtfDetail = MutableStateFlow(false)
    val showEtfDetail: StateFlow<Boolean> = _showEtfDetail.asStateFlow()

    private val _etfDetailState = MutableStateFlow<EtfDetailState>(EtfDetailState.Loading)
    val etfDetailState: StateFlow<EtfDetailState> = _etfDetailState.asStateFlow()

    init {
        loadInitialData()
        observeWorkProgress()
    }

    private fun loadInitialData() {
        loadSettings()
        loadRankingData()
        loadChangesData()
        loadDateInfo()
        loadMissingDates()
    }

    private fun loadDateInfo() {
        viewModelScope.launch {
            try {
                val latestDate = etfRepository.getLatestDate().getOrNull()
                _currentDate.value = latestDate

                if (latestDate != null) {
                    val previousDate = etfRepository.getPreviousDate(latestDate).getOrNull()
                    _previousDate.value = previousDate
                }
            } catch (e: Exception) {
                // Date info load error is not critical
            }
        }
    }

    /**
     * Load missing collection dates analysis.
     */
    fun loadMissingDates() {
        viewModelScope.launch {
            try {
                val result = etfCollectorRepo.findMissingCollectionDates()
                _missingDatesResult.value = result
            } catch (e: Exception) {
                // Missing dates analysis error is not critical
                _missingDatesResult.value = null
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val enabledKeywords = etfRepository.getEnabledKeywords().getOrNull() ?: emptyList()
                val includeKeywords = enabledKeywords.filter { it.filterType == FilterType.INCLUDE }
                val excludeKeywords = enabledKeywords.filter { it.filterType == FilterType.EXCLUDE }

                _settingsConfig.value = _settingsConfig.value.copy(
                    includeKeywords = includeKeywords,
                    excludeKeywords = excludeKeywords
                )
            } catch (e: Exception) {
                // Settings load error is not critical
            }
        }
    }

    fun selectTab(tab: EtfTab) {
        _selectedTab.value = tab
        when (tab) {
            EtfTab.STATISTICS -> {
                // Load data based on current sub-tab
                loadStatisticsData()
            }
            EtfTab.THEME_LIST -> {
                loadActiveEtfs()
            }
        }
    }

    // ==================== Statistics Tab ====================

    /**
     * Select a statistics sub-tab.
     */
    fun selectSubTab(subTab: StatisticsSubTab) {
        _selectedSubTab.value = subTab
        loadSubTabData(subTab)
    }

    /**
     * Select a date range for statistics.
     */
    fun selectDateRange(range: DateRangeOption) {
        _selectedDateRange.value = range
        loadSubTabData(_selectedSubTab.value)
    }

    private fun loadStatisticsData() {
        // Load ranking and changes data for statistics
        if (_rankingState.value is RankingState.Loading) loadRankingData()
        if (_changesState.value is ChangesState.Loading) loadChangesData()
        loadSubTabData(_selectedSubTab.value)
    }

    private fun loadSubTabData(subTab: StatisticsSubTab) {
        when (subTab) {
            StatisticsSubTab.AMOUNT_RANKING -> {
                if (_rankingState.value is RankingState.Loading) loadRankingData()
            }
            StatisticsSubTab.NEWLY_INCLUDED,
            StatisticsSubTab.REMOVED,
            StatisticsSubTab.WEIGHT_INCREASED,
            StatisticsSubTab.WEIGHT_DECREASED -> {
                if (_changesState.value is ChangesState.Loading) loadChangesData()
            }
            StatisticsSubTab.CASH_DEPOSIT -> loadCashDepositData()
            StatisticsSubTab.STOCK_ANALYSIS -> {
                // Stock analysis is loaded on search, not on tab selection
            }
        }
    }

    /**
     * Load cash deposit trend data.
     */
    private fun loadCashDepositData() {
        viewModelScope.launch {
            _cashDepositState.value = CashDepositState.Loading

            getCashDepositTrendUC(_selectedDateRange.value).fold(
                onSuccess = { result ->
                    _currentDate.value = result.endDate
                    _previousDate.value = result.startDate
                    _cashDepositState.value = if (result.trend.isEmpty()) {
                        CashDepositState.NoData
                    } else {
                        CashDepositState.Success(result)
                    }
                },
                onFailure = { error ->
                    _cashDepositState.value = if (error.message?.contains("데이터가 없습니다") == true) {
                        CashDepositState.NoData
                    } else {
                        CashDepositState.Error(error.message ?: "데이터 로드 실패")
                    }
                }
            )
        }
    }

    /**
     * Refresh cash deposit data.
     */
    fun refreshCashDeposit() {
        viewModelScope.launch {
            _isCashDepositRefreshing.value = true
            loadCashDepositData()
            _isCashDepositRefreshing.value = false
        }
    }

    /**
     * Update stock search query for analysis with debounced autocomplete.
     */
    fun updateStockSearchQuery(query: String) {
        _stockSearchQuery.value = query

        // Clear suggestions if query is blank
        if (query.isBlank()) {
            _stockSuggestions.value = emptyList()
            _isSuggestionsLoading.value = false
            suggestionSearchJob?.cancel()
            return
        }

        // Debounce autocomplete search
        suggestionSearchJob?.cancel()
        suggestionSearchJob = viewModelScope.launch {
            _isSuggestionsLoading.value = true
            delay(SEARCH_DEBOUNCE_MS)
            val suggestions = searchRepo.searchForSuggestions(query)
            _stockSuggestions.value = suggestions
            _isSuggestionsLoading.value = false
        }
    }

    /**
     * Search and analyze a stock.
     */
    fun searchStock() {
        val query = _stockSearchQuery.value.trim()
        if (query.length < 2) {
            _stockAnalysisState.value = StockAnalysisState.Initial
            return
        }

        viewModelScope.launch {
            _stockAnalysisState.value = StockAnalysisState.Loading
            _stockSuggestions.value = emptyList() // Clear suggestions when searching

            getStockAnalysisUC.search(query).fold(
                onSuccess = { result ->
                    _stockAnalysisState.value = StockAnalysisState.Success(result)
                },
                onFailure = { error ->
                    _stockAnalysisState.value = if (error.message?.contains("찾을 수 없습니다") == true ||
                        error.message?.contains("없습니다") == true) {
                        StockAnalysisState.NotFound(query)
                    } else {
                        StockAnalysisState.Error(error.message ?: "분석 실패")
                    }
                }
            )
        }
    }

    /**
     * Handle stock selection from autocomplete suggestions.
     */
    fun onStockSuggestionSelected(stock: Stock) {
        // Update query with selected stock name
        _stockSearchQuery.value = stock.name
        // Clear suggestions
        _stockSuggestions.value = emptyList()
        suggestionSearchJob?.cancel()

        // Trigger analysis with the selected stock code
        viewModelScope.launch {
            _stockAnalysisState.value = StockAnalysisState.Loading

            getStockAnalysisUC(stock.ticker).fold(
                onSuccess = { result ->
                    _stockAnalysisState.value = StockAnalysisState.Success(result)
                },
                onFailure = { error ->
                    _stockAnalysisState.value = if (error.message?.contains("찾을 수 없습니다") == true ||
                        error.message?.contains("없습니다") == true) {
                        StockAnalysisState.NotFound(stock.name)
                    } else {
                        StockAnalysisState.Error(error.message ?: "분석 실패")
                    }
                }
            )
        }
    }

    /**
     * Navigate to analysis screen from stock analysis tab.
     */
    fun navigateToAnalysisFromStock(stockCode: String, stockName: String) {
        selectedStockManager.selectTicker(stockCode, stockName)
    }

    // ==================== Collection Tab ====================

    fun startCollection() {
        viewModelScope.launch {
            _collectionState.value = CollectionState.Collecting(0, 0)

            val config = _settingsConfig.value
            val filterConfig = EtfFilterConfig(
                activeOnly = config.activeOnly,
                includeKeywords = config.includeKeywords.map { it.keyword },
                excludeKeywords = config.excludeKeywords.map { it.keyword }
            )

            // Start background worker
            EtfCollectionWorker.collectNow(context, filterConfig)
        }
    }

    private fun observeWorkProgress() {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(EtfCollectionWorker.WORK_NAME_ONCE)
                .collect { workInfos ->
                    val workInfo = workInfos.firstOrNull() ?: return@collect

                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val current = workInfo.progress.getInt(EtfCollectionWorker.KEY_PROGRESS_CURRENT, 0)
                            val total = workInfo.progress.getInt(EtfCollectionWorker.KEY_PROGRESS_TOTAL, 0)
                            _collectionState.value = CollectionState.Collecting(current, total)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val etfCount = workInfo.outputData.getInt(EtfCollectionWorker.KEY_RESULT_ETF_COUNT, 0)
                            val constituentCount = workInfo.outputData.getInt(EtfCollectionWorker.KEY_RESULT_CONSTITUENT_COUNT, 0)
                            _collectionState.value = CollectionState.Success(etfCount, constituentCount)
                            // Reload data after successful collection
                            loadRankingData()
                            loadChangesData()
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(EtfCollectionWorker.KEY_RESULT_ERROR)
                                ?: "수집 실패"
                            _collectionState.value = CollectionState.Error(error)
                        }
                        WorkInfo.State.CANCELLED -> {
                            _collectionState.value = CollectionState.Idle
                        }
                        else -> {}
                    }
                }
        }
    }

    // ==================== Ranking Tab ====================

    fun loadRankingData() {
        viewModelScope.launch {
            _rankingState.value = RankingState.Loading

            getStockRankingUC(limit = 100).fold(
                onSuccess = { result ->
                    _rankingState.value = if (result.rankings.isEmpty()) {
                        RankingState.NoData
                    } else {
                        RankingState.Success(result)
                    }
                },
                onFailure = { error ->
                    _rankingState.value = if (error.message?.contains("데이터가 없습니다") == true) {
                        RankingState.NoData
                    } else {
                        RankingState.Error(error.message ?: "데이터 로드 실패")
                    }
                }
            )
        }
    }

    fun refreshRanking() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadRankingData()
            _isRefreshing.value = false
        }
    }

    /**
     * Handle ranking column header click for sorting.
     */
    fun onRankingSortColumnClick(column: RankingSortColumn) {
        _rankingSortState.value = _rankingSortState.value.onColumnClick(column)
    }

    fun onRankingItemClick(item: EnhancedStockRanking) {
        // Navigate to Analysis screen
        selectedStockManager.selectTicker(item.stockCode, item.stockName)
    }

    // ==================== Changes Tab ====================

    fun loadChangesData() {
        viewModelScope.launch {
            _changesState.value = ChangesState.Loading

            getStockChangesUC(weightThreshold = 0.1).fold(
                onSuccess = { result ->
                    _changesState.value = if (result.totalChanges == 0) {
                        ChangesState.NoData
                    } else {
                        ChangesState.Success(result)
                    }
                },
                onFailure = { error ->
                    _changesState.value = if (error.message?.contains("데이터가 없습니다") == true) {
                        ChangesState.NoData
                    } else {
                        ChangesState.Error(error.message ?: "데이터 로드 실패")
                    }
                }
            )
        }
    }

    fun refreshChanges() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadChangesData()
            _isRefreshing.value = false
        }
    }

    // ==================== Settings Tab ====================

    fun setAutoCollectionEnabled(enabled: Boolean) {
        _settingsConfig.value = _settingsConfig.value.copy(isAutoCollectionEnabled = enabled)

        if (enabled) {
            val config = _settingsConfig.value
            EtfCollectionWorker.scheduleDailyCollection(
                context = context,
                hour = config.collectionHour,
                minute = config.collectionMinute,
                filterConfig = EtfFilterConfig(
                    activeOnly = config.activeOnly,
                    includeKeywords = config.includeKeywords.map { it.keyword },
                    excludeKeywords = config.excludeKeywords.map { it.keyword }
                )
            )
        } else {
            EtfCollectionWorker.cancelScheduledCollection(context)
        }
    }

    fun showTimePicker() {
        _showTimePicker.value = true
    }

    fun hideTimePicker() {
        _showTimePicker.value = false
    }

    fun setCollectionTime(hour: Int, minute: Int) {
        _settingsConfig.value = _settingsConfig.value.copy(
            collectionHour = hour,
            collectionMinute = minute
        )
        hideTimePicker()

        // Reschedule if enabled
        if (_settingsConfig.value.isAutoCollectionEnabled) {
            setAutoCollectionEnabled(true)
        }
    }

    fun setActiveOnly(activeOnly: Boolean) {
        _settingsConfig.value = _settingsConfig.value.copy(activeOnly = activeOnly)
    }

    fun setDataRetentionDays(days: Int) {
        _settingsConfig.value = _settingsConfig.value.copy(dataRetentionDays = days)
    }

    fun showAddKeywordDialog(type: FilterType) {
        _addKeywordType.value = type
        _showAddKeywordDialog.value = true
    }

    fun hideAddKeywordDialog() {
        _showAddKeywordDialog.value = false
    }

    fun addKeyword(keyword: String) {
        viewModelScope.launch {
            val type = _addKeywordType.value
            etfRepository.addKeyword(keyword.trim(), type).fold(
                onSuccess = {
                    hideAddKeywordDialog()
                    loadSettings()
                },
                onFailure = { error ->
                    _error.value = error.message ?: "키워드 추가 실패"
                }
            )
        }
    }

    fun deleteKeyword(id: Long) {
        viewModelScope.launch {
            etfRepository.deleteKeyword(id).fold(
                onSuccess = { loadSettings() },
                onFailure = { error ->
                    _error.value = error.message ?: "키워드 삭제 실패"
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }

    // ==================== Stock Detail ====================

    /**
     * Show stock detail bottom sheet and load data.
     */
    fun showStockDetail(stockCode: String, stockName: String) {
        _showStockDetail.value = true
        loadStockDetailData(stockCode, stockName)
    }

    /**
     * Hide stock detail bottom sheet.
     */
    fun hideStockDetail() {
        _showStockDetail.value = false
    }

    private fun loadStockDetailData(stockCode: String, stockName: String) {
        viewModelScope.launch {
            _stockDetailState.value = StockDetailState.Loading

            try {
                // Load amount history
                val amountHistoryResult = etfRepository.getStockAmountHistory(stockCode)
                val amountHistory = amountHistoryResult.getOrNull() ?: emptyList()

                // Load weight history
                val weightHistoryResult = etfRepository.getStockWeightHistory(stockCode)
                val weightHistory = weightHistoryResult.getOrNull() ?: emptyList()

                // Load containing ETFs (from latest date)
                val latestDate = etfRepository.getLatestDate().getOrNull()
                val containingEtfs: List<EtfConstituent> = if (latestDate != null) {
                    val constituents = etfRepository.getConstituentsByDate(latestDate).getOrNull() ?: emptyList()
                    constituents.filter { it.stockCode == stockCode }
                } else {
                    emptyList()
                }

                _stockDetailState.value = StockDetailState.Success(
                    StockDetailData(
                        stockCode = stockCode,
                        stockName = stockName,
                        amountHistory = amountHistory,
                        weightHistory = weightHistory,
                        containingEtfs = containingEtfs
                    )
                )
            } catch (e: Exception) {
                _stockDetailState.value = StockDetailState.Error(
                    e.message ?: "데이터를 불러오는데 실패했습니다"
                )
            }
        }
    }

    /**
     * Handle ranking item click - show detail instead of navigating to Analysis.
     */
    fun onRankingItemDetailClick(item: EnhancedStockRanking) {
        showStockDetail(item.stockCode, item.stockName)
    }

    // ==================== Theme List Tab ====================

    /**
     * Load active ETFs for theme list.
     */
    fun loadActiveEtfs() {
        viewModelScope.launch {
            _themeListState.value = ThemeListState.Loading

            etfRepository.getActiveEtfSummaries().fold(
                onSuccess = { etfs ->
                    _themeListState.value = if (etfs.isEmpty()) {
                        ThemeListState.NoData
                    } else {
                        ThemeListState.Success(etfs)
                    }
                },
                onFailure = { error ->
                    _themeListState.value = if (error.message?.contains("데이터가 없습니다") == true ||
                        error.message?.contains("No data") == true) {
                        ThemeListState.NoData
                    } else {
                        ThemeListState.Error(error.message ?: "ETF 목록 로드 실패")
                    }
                }
            )
        }
    }

    /**
     * Update ETF search query for theme list.
     */
    fun updateEtfSearchQuery(query: String) {
        _etfSearchQuery.value = query
    }

    /**
     * Search ETFs by name for theme list.
     */
    fun searchEtfs(query: String) {
        viewModelScope.launch {
            _themeListState.value = ThemeListState.Loading

            val result = if (query.trim().isEmpty()) {
                etfRepository.getActiveEtfSummaries()
            } else {
                etfRepository.searchActiveEtfs(query.trim())
            }

            result.fold(
                onSuccess = { etfs ->
                    _themeListState.value = if (etfs.isEmpty()) {
                        ThemeListState.NoData
                    } else {
                        ThemeListState.Success(etfs)
                    }
                },
                onFailure = { error ->
                    _themeListState.value = ThemeListState.Error(error.message ?: "검색 실패")
                }
            )
        }
    }

    /**
     * Show ETF detail bottom sheet and load data.
     */
    fun showEtfDetail(etfCode: String, etfName: String) {
        _showEtfDetail.value = true
        loadEtfDetailData(etfCode)
    }

    /**
     * Hide ETF detail bottom sheet.
     */
    fun hideEtfDetail() {
        _showEtfDetail.value = false
    }

    private fun loadEtfDetailData(etfCode: String) {
        viewModelScope.launch {
            _etfDetailState.value = EtfDetailState.Loading

            etfRepository.getEtfDetail(etfCode).fold(
                onSuccess = { detail ->
                    _etfDetailState.value = EtfDetailState.Success(detail)
                },
                onFailure = { error ->
                    _etfDetailState.value = EtfDetailState.Error(error.message ?: "ETF 상세 정보 로드 실패")
                }
            )
        }
    }

    // ==================== Helper ====================

    private fun EtfCollectionHistoryEntity.toHistoryItem(): CollectionHistoryItem {
        val duration = completedAt?.let { (it - startedAt) / 1000 }
        return CollectionHistoryItem(
            id = id,
            date = collectedDate,
            etfCount = totalEtfs,
            constituentCount = totalConstituents,
            status = CollectionStatus.fromValue(status),
            errorMessage = errorMessage,
            startedAt = startedAt,
            completedAt = completedAt,
            durationSeconds = duration
        )
    }
}
