package com.stockapp.feature.search.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.core.cache.CacheState
import com.stockapp.core.cache.StockCacheManager
import com.stockapp.core.config.AppConfig
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.repo.SearchRepo
import com.stockapp.feature.search.domain.usecase.SaveHistoryUC
import com.stockapp.feature.search.domain.usecase.SearchStockUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SearchVm"

/**
 * Search screen state.
 */
sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Results(val stocks: List<Stock>) : SearchState()
    data class Error(val code: String, val msg: String) : SearchState()
}

@HiltViewModel
class SearchVm @Inject constructor(
    private val searchUC: SearchStockUC,
    private val saveHistoryUC: SaveHistoryUC,
    private val repo: SearchRepo,
    private val cacheManager: StockCacheManager,
    private val selectedStockManager: SelectedStockManager
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _history = MutableStateFlow<List<Stock>>(emptyList())
    val history: StateFlow<List<Stock>> = _history.asStateFlow()

    private val _cacheCount = MutableStateFlow(0)
    val cacheCount: StateFlow<Int> = _cacheCount.asStateFlow()

    val cacheState: StateFlow<CacheState> = cacheManager.state

    private var searchJob: Job? = null

    init {
        // Load search history
        viewModelScope.launch {
            repo.getHistory()
                .catch { e ->
                    Log.e(TAG, "Failed to load search history", e)
                    // History loading failure is non-critical, continue with empty list
                }
                .collect { _history.value = it }
        }

        // Check cache status
        viewModelScope.launch {
            _cacheCount.value = repo.getCacheCount()
            Log.d(TAG, "init() cache count: ${_cacheCount.value}")
        }
    }

    /**
     * Refresh stock cache manually.
     */
    fun refreshCache() {
        viewModelScope.launch {
            Log.d(TAG, "refreshCache() started")
            cacheManager.refreshCache()
            _cacheCount.value = repo.getCacheCount()
            Log.d(TAG, "refreshCache() done, count: ${_cacheCount.value}")
        }
    }

    /**
     * Update query and trigger debounced search.
     */
    fun onQueryChange(newQuery: String) {
        Log.d(TAG, "onQueryChange() query: $newQuery")
        _query.value = newQuery

        if (newQuery.isBlank()) {
            Log.d(TAG, "onQueryChange() blank query, setting Idle")
            _state.value = SearchState.Idle
            return
        }

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Log.d(TAG, "onQueryChange() debounce delay started")
            delay(AppConfig.SEARCH_DEBOUNCE_MS)
            Log.d(TAG, "onQueryChange() debounce delay complete, triggering search")
            search(newQuery)
        }
    }

    /**
     * Execute search.
     */
    fun search(query: String = _query.value) {
        Log.d(TAG, "search() called with query: $query")

        if (query.isBlank()) {
            Log.d(TAG, "search() blank query, setting Idle")
            _state.value = SearchState.Idle
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "search() setting Loading state")
            _state.value = SearchState.Loading

            Log.d(TAG, "search() invoking searchUC")
            searchUC(query)
                .onSuccess { stocks ->
                    Log.d(TAG, "search() success: ${stocks.size} stocks found")
                    _state.value = if (stocks.isEmpty()) {
                        Log.d(TAG, "search() empty results, setting Results(emptyList)")
                        SearchState.Results(emptyList())
                    } else {
                        Log.d(TAG, "search() setting Results with ${stocks.size} stocks")
                        SearchState.Results(stocks)
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "search() failure: ${e.message}", e)
                    _state.value = SearchState.Error(
                        code = "SEARCH_ERROR",
                        msg = e.message ?: "검색 실패"
                    )
                }
        }
    }

    /**
     * Handle stock selection.
     */
    fun onStockSelected(stock: Stock) {
        // Update shared state for other screens
        selectedStockManager.select(stock)

        viewModelScope.launch {
            saveHistoryUC(stock)
        }
    }

    /**
     * Clear search.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _query.value = ""
        _state.value = SearchState.Idle
    }

    /**
     * Retry search after error.
     */
    fun retry() {
        search()
    }
}
