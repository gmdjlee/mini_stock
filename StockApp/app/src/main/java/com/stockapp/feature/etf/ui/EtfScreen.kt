package com.stockapp.feature.etf.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.theme.ThemeToggleButton
import com.stockapp.feature.etf.ui.detail.EtfDetailBottomSheet
import com.stockapp.feature.etf.ui.detail.StockDetailBottomSheet
import com.stockapp.feature.etf.ui.tabs.ThemeListTab
import com.stockapp.feature.etf.ui.tabs.statistics.StatisticsHubContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtfScreen(
    onStockClick: () -> Unit = {},
    viewModel: EtfVm = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val showStockDetail by viewModel.showStockDetail.collectAsState()
    val stockDetailState by viewModel.stockDetailState.collectAsState()

    // Statistics tab states
    val selectedSubTab by viewModel.selectedSubTab.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val previousDate by viewModel.previousDate.collectAsState()
    val cashDepositState by viewModel.cashDepositState.collectAsState()
    val isCashDepositRefreshing by viewModel.isCashDepositRefreshing.collectAsState()
    val stockAnalysisState by viewModel.stockAnalysisState.collectAsState()
    val stockSearchQuery by viewModel.stockSearchQuery.collectAsState()
    val stockSuggestions by viewModel.stockSuggestions.collectAsState()
    val isSuggestionsLoading by viewModel.isSuggestionsLoading.collectAsState()

    // Theme list tab states
    val showEtfDetail by viewModel.showEtfDetail.collectAsState()
    val etfDetailState by viewModel.etfDetailState.collectAsState()

    // ETF detail bottom sheet
    if (showEtfDetail) {
        EtfDetailBottomSheet(
            state = etfDetailState,
            onDismiss = { viewModel.hideEtfDetail() },
            onStockClick = { stockCode, stockName ->
                viewModel.showStockDetail(stockCode, stockName)
            }
        )
    }

    // Stock detail bottom sheet
    if (showStockDetail) {
        StockDetailBottomSheet(
            state = stockDetailState,
            onDismiss = { viewModel.hideStockDetail() },
            onNavigateToAnalysis = { stockCode, stockName ->
                viewModel.onRankingItemClick(
                    com.stockapp.feature.etf.domain.usecase.EnhancedStockRanking(
                        rank = 0,
                        stockCode = stockCode,
                        stockName = stockName,
                        totalAmount = 0L,
                        etfCount = 0,
                        previousAmount = null,
                        amountChange = null,
                        isNew = false
                    )
                )
                onStockClick()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ETF 통계") },
                actions = { ThemeToggleButton() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = EtfTab.entries.indexOf(selectedTab),
                edgePadding = 16.dp
            ) {
                EtfTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.title) }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                EtfTab.STATISTICS -> StatisticsHubContent(
                    viewModel = viewModel,
                    selectedSubTab = selectedSubTab,
                    onSubTabSelected = { viewModel.selectSubTab(it) },
                    selectedDateRange = selectedDateRange,
                    onDateRangeSelected = { viewModel.selectDateRange(it) },
                    currentDate = currentDate,
                    previousDate = previousDate,
                    onStockClick = onStockClick,
                    onNavigateToAnalysis = { stockCode, stockName ->
                        viewModel.navigateToAnalysisFromStock(stockCode, stockName)
                        onStockClick()
                    },
                    cashDepositState = cashDepositState,
                    isCashDepositRefreshing = isCashDepositRefreshing,
                    onCashDepositRefresh = { viewModel.refreshCashDeposit() },
                    stockAnalysisState = stockAnalysisState,
                    stockSearchQuery = stockSearchQuery,
                    onStockSearchQueryChange = { viewModel.updateStockSearchQuery(it) },
                    onStockSearch = { viewModel.searchStock() },
                    stockSuggestions = stockSuggestions,
                    isSuggestionsLoading = isSuggestionsLoading,
                    onStockSuggestionSelect = { viewModel.onStockSuggestionSelected(it) }
                )
                EtfTab.THEME_LIST -> ThemeListTab(
                    viewModel = viewModel,
                    onEtfClick = { etfCode, etfName ->
                        viewModel.showEtfDetail(etfCode, etfName)
                    }
                )
            }
        }
    }
}
