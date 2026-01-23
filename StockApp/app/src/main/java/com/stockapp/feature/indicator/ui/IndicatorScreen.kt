package com.stockapp.feature.indicator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.theme.ThemeToggleButton
import com.stockapp.feature.indicator.domain.model.IndicatorType

/**
 * Main Indicator Screen.
 * Displays technical indicators (Trend, Elder, DeMark) for the selected stock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorScreen(
    viewModel: IndicatorVm = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = state) {
                        is IndicatorState.Success -> "${s.stockName} 기술 지표"
                        else -> "기술 지표"
                    }
                    Text(title)
                },
                actions = {
                    if (state is IndicatorState.Success || state is IndicatorState.Error) {
                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !isRefreshing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "새로고침"
                            )
                        }
                    }
                    ThemeToggleButton()
                }
            )
        }
    ) { paddingValues ->
        when (val currentState = state) {
            is IndicatorState.NoStock -> {
                NoStockContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is IndicatorState.Loading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is IndicatorState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = IndicatorType.entries.indexOf(selectedTab),
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 16.dp
                    ) {
                        IndicatorType.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(tab.label) }
                            )
                        }
                    }

                    // Content
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        IndicatorContent(
                            state = currentState,
                            selectedTab = selectedTab,
                            selectedTimeframe = selectedTimeframe,
                            onTimeframeSelect = { viewModel.selectTimeframe(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            is IndicatorState.Error -> {
                ErrorContent(
                    message = currentState.msg,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun IndicatorContent(
    state: IndicatorState.Success,
    selectedTab: IndicatorType,
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeframe selector (Daily/Weekly/Monthly)
        TimeframeSelector(
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelect = onTimeframeSelect
        )

        when (selectedTab) {
            IndicatorType.TREND -> {
                state.trend?.let { TrendContent(it, selectedTimeframe) }
                    ?: DataNotLoaded()
            }
            IndicatorType.ELDER -> {
                state.elder?.let { ElderContent(it, selectedTimeframe) }
                    ?: DataNotLoaded()
            }
            IndicatorType.DEMARK -> {
                state.demark?.let { DemarkContent(it, selectedTimeframe) }
                    ?: DataNotLoaded()
            }
        }
    }
}
