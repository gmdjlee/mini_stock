package com.stockapp.feature.etf.ui.tabs.statistics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stockapp.feature.etf.domain.model.DateRangeOption
import com.stockapp.feature.etf.ui.EtfVm
import com.stockapp.feature.etf.ui.tabs.StockRankingTab
import com.stockapp.feature.search.domain.model.Stock

/**
 * Container component with ScrollableTabRow for 7 statistics sub-tabs.
 * Following EtfScreen.kt pattern.
 */
@Composable
fun StatisticsHubContent(
    viewModel: EtfVm,
    selectedSubTab: StatisticsSubTab,
    onSubTabSelected: (StatisticsSubTab) -> Unit,
    selectedDateRange: DateRangeOption,
    onDateRangeSelected: (DateRangeOption) -> Unit,
    currentDate: String?,
    previousDate: String?,
    onStockClick: () -> Unit,
    onNavigateToAnalysis: (stockCode: String, stockName: String) -> Unit,
    modifier: Modifier = Modifier,
    // Cash deposit state
    cashDepositState: CashDepositState = CashDepositState.Loading,
    isCashDepositRefreshing: Boolean = false,
    onCashDepositRefresh: () -> Unit = {},
    // Stock analysis state
    stockAnalysisState: StockAnalysisState = StockAnalysisState.Initial,
    stockSearchQuery: String = "",
    onStockSearchQueryChange: (String) -> Unit = {},
    onStockSearch: () -> Unit = {},
    // Stock analysis autocomplete
    stockSuggestions: List<Stock> = emptyList(),
    isSuggestionsLoading: Boolean = false,
    onStockSuggestionSelect: (Stock) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Sub-tab row (ScrollableTabRow)
        ScrollableTabRow(
            selectedTabIndex = StatisticsSubTab.entries.indexOf(selectedSubTab),
            edgePadding = 16.dp
        ) {
            StatisticsSubTab.entries.forEach { tab ->
                Tab(
                    selected = selectedSubTab == tab,
                    onClick = { onSubTabSelected(tab) },
                    text = { Text(tab.title) },
                    icon = { Icon(tab.icon, contentDescription = tab.title) }
                )
            }
        }

        // Date range selector (shown for applicable tabs - not for STOCK_ANALYSIS)
        if (selectedSubTab != StatisticsSubTab.STOCK_ANALYSIS) {
            DateRangeSelectorBar(
                selectedRange = selectedDateRange,
                onRangeSelected = onDateRangeSelected,
                showDateInfo = currentDate != null,
                currentDate = currentDate,
                previousDate = previousDate
            )
        }

        // Sub-tab content
        when (selectedSubTab) {
            StatisticsSubTab.AMOUNT_RANKING -> {
                // Reuse existing StockRankingTab
                StockRankingTab(
                    viewModel = viewModel,
                    onStockClick = onStockClick
                )
            }

            StatisticsSubTab.NEWLY_INCLUDED -> {
                // Filter to show only newly included from StockChangesTab
                FilteredStockChangesContent(
                    viewModel = viewModel,
                    filterType = StatisticsSubTab.NEWLY_INCLUDED,
                    onStockClick = onStockClick
                )
            }

            StatisticsSubTab.REMOVED -> {
                // Filter to show only removed from StockChangesTab
                FilteredStockChangesContent(
                    viewModel = viewModel,
                    filterType = StatisticsSubTab.REMOVED,
                    onStockClick = onStockClick
                )
            }

            StatisticsSubTab.WEIGHT_INCREASED -> {
                // Filter to show only weight increased
                FilteredStockChangesContent(
                    viewModel = viewModel,
                    filterType = StatisticsSubTab.WEIGHT_INCREASED,
                    onStockClick = onStockClick
                )
            }

            StatisticsSubTab.WEIGHT_DECREASED -> {
                // Filter to show only weight decreased
                FilteredStockChangesContent(
                    viewModel = viewModel,
                    filterType = StatisticsSubTab.WEIGHT_DECREASED,
                    onStockClick = onStockClick
                )
            }

            StatisticsSubTab.CASH_DEPOSIT -> {
                CashDepositTab(
                    state = cashDepositState,
                    isRefreshing = isCashDepositRefreshing,
                    onRefresh = onCashDepositRefresh
                )
            }

            StatisticsSubTab.STOCK_ANALYSIS -> {
                StockAnalysisTab(
                    state = stockAnalysisState,
                    searchQuery = stockSearchQuery,
                    onSearchQueryChange = onStockSearchQueryChange,
                    onSearch = onStockSearch,
                    onNavigateToAnalysis = onNavigateToAnalysis,
                    suggestions = stockSuggestions,
                    isLoading = isSuggestionsLoading,
                    onStockSelect = onStockSuggestionSelect
                )
            }
        }
    }
}

/**
 * Filtered content from StockChangesTab based on the sub-tab type.
 * This internally uses the existing ViewModel state but filters the display.
 */
@Composable
private fun FilteredStockChangesContent(
    viewModel: EtfVm,
    filterType: StatisticsSubTab,
    onStockClick: () -> Unit
) {
    val changesState by viewModel.changesState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    FilteredChangesTab(
        changesState = changesState,
        filterType = filterType,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshChanges() },
        onItemClick = { item ->
            viewModel.showStockDetail(item.stockCode, item.stockName)
        },
        onItemLongClick = { item ->
            viewModel.onRankingItemClick(
                com.stockapp.feature.etf.domain.usecase.EnhancedStockRanking(
                    rank = item.rank,
                    stockCode = item.stockCode,
                    stockName = item.stockName,
                    totalAmount = item.totalAmount,
                    etfCount = item.etfNames.size,
                    previousAmount = null,
                    amountChange = null,
                    isNew = false
                )
            )
            onStockClick()
        }
    )
}
