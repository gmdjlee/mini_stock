package com.stockapp.core.state

import com.stockapp.feature.search.domain.model.Stock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for currently selected stock.
 * Shared across all screens for bottom navigation flow.
 */
@Singleton
class SelectedStockManager @Inject constructor() {

    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    private val _selectedTicker = MutableStateFlow<String?>(null)
    val selectedTicker: StateFlow<String?> = _selectedTicker.asStateFlow()

    /**
     * Select a stock.
     */
    fun select(stock: Stock) {
        _selectedStock.value = stock
        _selectedTicker.value = stock.ticker
    }

    /**
     * Select a stock by ticker only.
     */
    fun selectTicker(ticker: String, name: String? = null) {
        _selectedTicker.value = ticker
        if (name != null) {
            _selectedStock.value = Stock(
                ticker = ticker,
                name = name,
                market = com.stockapp.feature.search.domain.model.Market.OTHER
            )
        }
    }

    /**
     * Clear selection.
     */
    fun clear() {
        _selectedStock.value = null
        _selectedTicker.value = null
    }

    /**
     * Check if a stock is selected.
     */
    fun hasSelection(): Boolean = _selectedTicker.value != null
}
