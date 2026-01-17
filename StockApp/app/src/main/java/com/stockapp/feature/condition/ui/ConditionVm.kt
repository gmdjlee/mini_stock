package com.stockapp.feature.condition.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.feature.condition.domain.model.Condition
import com.stockapp.feature.condition.domain.model.ConditionResult
import com.stockapp.feature.condition.domain.usecase.GetConditionListUC
import com.stockapp.feature.condition.domain.usecase.SearchConditionUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sealed class for condition list UI state.
 */
sealed class ConditionListState {
    data object Loading : ConditionListState()
    data class Success(val conditions: List<Condition>) : ConditionListState()
    data class Error(val code: String, val msg: String) : ConditionListState()
}

/**
 * Sealed class for condition search result UI state.
 */
sealed class ConditionSearchState {
    data object Idle : ConditionSearchState()
    data object Loading : ConditionSearchState()
    data class Success(val result: ConditionResult) : ConditionSearchState()
    data class Error(val code: String, val msg: String) : ConditionSearchState()
}

@HiltViewModel
class ConditionVm @Inject constructor(
    private val getConditionListUC: GetConditionListUC,
    private val searchConditionUC: SearchConditionUC
) : ViewModel() {

    private val _listState = MutableStateFlow<ConditionListState>(ConditionListState.Loading)
    val listState: StateFlow<ConditionListState> = _listState.asStateFlow()

    private val _searchState = MutableStateFlow<ConditionSearchState>(ConditionSearchState.Idle)
    val searchState: StateFlow<ConditionSearchState> = _searchState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedCondition = MutableStateFlow<Condition?>(null)
    val selectedCondition: StateFlow<Condition?> = _selectedCondition.asStateFlow()

    init {
        loadConditionList()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadConditionList(useCache = false)
    }

    fun retryList() {
        _listState.value = ConditionListState.Loading
        loadConditionList(useCache = false)
    }

    fun selectCondition(condition: Condition) {
        _selectedCondition.value = condition
        executeSearch(condition)
    }

    fun retrySearch() {
        _selectedCondition.value?.let { condition ->
            executeSearch(condition)
        }
    }

    fun clearSearchResult() {
        _searchState.value = ConditionSearchState.Idle
        _selectedCondition.value = null
    }

    private fun loadConditionList(useCache: Boolean = true) {
        viewModelScope.launch {
            try {
                getConditionListUC(useCache).fold(
                    onSuccess = { conditions ->
                        _listState.value = ConditionListState.Success(conditions)
                    },
                    onFailure = { e ->
                        _listState.value = ConditionListState.Error(
                            code = "ERROR",
                            msg = e.message ?: "조건검색 목록을 가져오는데 실패했습니다"
                        )
                    }
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun executeSearch(condition: Condition) {
        _searchState.value = ConditionSearchState.Loading

        viewModelScope.launch {
            searchConditionUC(condition.idx, condition.name).fold(
                onSuccess = { result ->
                    _searchState.value = ConditionSearchState.Success(result)
                },
                onFailure = { e ->
                    _searchState.value = ConditionSearchState.Error(
                        code = "ERROR",
                        msg = e.message ?: "조건검색에 실패했습니다"
                    )
                }
            )
        }
    }
}
