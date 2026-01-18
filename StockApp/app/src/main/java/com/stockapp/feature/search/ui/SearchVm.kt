package com.stockapp.feature.search.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repo: SearchRepo
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _history = MutableStateFlow<List<Stock>>(emptyList())
    val history: StateFlow<List<Stock>> = _history.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Load search history
        viewModelScope.launch {
            repo.getHistory()
                .catch { /* ignore errors */ }
                .collect { _history.value = it }
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
            delay(300) // 300ms debounce
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
