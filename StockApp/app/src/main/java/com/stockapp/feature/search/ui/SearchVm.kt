package com.stockapp.feature.search.ui

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
        _query.value = newQuery

        if (newQuery.isBlank()) {
            _state.value = SearchState.Idle
            return
        }

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            search(newQuery)
        }
    }

    /**
     * Execute search.
     */
    fun search(query: String = _query.value) {
        if (query.isBlank()) {
            _state.value = SearchState.Idle
            return
        }

        viewModelScope.launch {
            _state.value = SearchState.Loading

            searchUC(query)
                .onSuccess { stocks ->
                    _state.value = if (stocks.isEmpty()) {
                        SearchState.Results(emptyList())
                    } else {
                        SearchState.Results(stocks)
                    }
                }
                .onFailure { e ->
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
