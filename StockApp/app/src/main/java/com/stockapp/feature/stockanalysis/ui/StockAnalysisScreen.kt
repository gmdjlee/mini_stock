package com.stockapp.feature.stockanalysis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stockapp.feature.analysis.ui.AnalysisScreen
import com.stockapp.feature.financial.ui.FinancialScreen
import com.stockapp.feature.indicator.ui.IndicatorScreen
import com.stockapp.feature.search.ui.SearchScreen
import kotlinx.coroutines.launch

private enum class StockTab(val title: String) {
    SEARCH("검색"),
    ANALYSIS("수급 분석"),
    INDICATOR("기술 지표"),
    FINANCIAL("재무정보")
}

private val tabs = StockTab.entries.toList()

@Composable
fun StockAnalysisScreen(
    initialTab: Int = 0
) {
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, tabs.lastIndex),
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(tab.title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (tabs[page]) {
                StockTab.SEARCH -> SearchScreen(
                    onStockClick = { _ ->
                        // Stock already selected via SelectedStockManager in SearchVm.
                        // Switch to analysis tab to show the selected stock's data.
                        scope.launch { pagerState.animateScrollToPage(StockTab.ANALYSIS.ordinal) }
                    }
                )
                StockTab.ANALYSIS -> AnalysisScreen()
                StockTab.INDICATOR -> IndicatorScreen()
                StockTab.FINANCIAL -> FinancialScreen()
            }
        }
    }
}
