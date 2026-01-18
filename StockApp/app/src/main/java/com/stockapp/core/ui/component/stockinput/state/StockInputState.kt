package com.stockapp.core.ui.component.stockinput.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.stockapp.feature.search.domain.model.Stock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * StockInputField 상태 관리 클래스
 */
@Stable
class StockInputState(
    initialValue: String = "",
    private val debounceMs: Long = 300L,
    private val onSearch: suspend (String) -> List<Stock>,
    private val scope: CoroutineScope
) {
    private var _value by mutableStateOf(initialValue)
    val value: String get() = _value

    private var _suggestions by mutableStateOf<List<Stock>>(emptyList())
    val suggestions: List<Stock> get() = _suggestions

    private var _isLoading by mutableStateOf(false)
    val isLoading: Boolean get() = _isLoading

    private var _selectedStock by mutableStateOf<Stock?>(null)
    val selectedStock: Stock? get() = _selectedStock

    private var searchJob: Job? = null

    fun onValueChange(newValue: String) {
        _value = newValue
        _selectedStock = null

        if (newValue.isBlank()) {
            _suggestions = emptyList()
            _isLoading = false
            searchJob?.cancel()
            return
        }

        searchJob?.cancel()
        searchJob = scope.launch {
            delay(debounceMs)
            _isLoading = true
            try {
                _suggestions = onSearch(newValue)
            } catch (e: Exception) {
                _suggestions = emptyList()
            } finally {
                _isLoading = false
            }
        }
    }

    fun onSelect(stock: Stock) {
        _value = stock.name
        _selectedStock = stock
        _suggestions = emptyList()
        searchJob?.cancel()
    }

    fun clear() {
        searchJob?.cancel()
        _value = ""
        _suggestions = emptyList()
        _selectedStock = null
        _isLoading = false
    }
}

@Composable
fun rememberStockInputState(
    initialValue: String = "",
    debounceMs: Long = 300L,
    onSearch: suspend (String) -> List<Stock>
): StockInputState {
    val scope = rememberCoroutineScope()
    return remember {
        StockInputState(
            initialValue = initialValue,
            debounceMs = debounceMs,
            onSearch = onSearch,
            scope = scope
        )
    }
}
