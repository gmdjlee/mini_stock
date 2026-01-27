package com.stockapp.feature.financial.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.theme.ThemeToggleButton
import com.stockapp.core.ui.component.ErrorCard
import com.stockapp.feature.financial.domain.model.FinancialTab

/**
 * Financial Info screen with profitability and stability tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    viewModel: FinancialVm = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = state) {
                        is FinancialState.Success -> "${s.summary.name} 재무정보"
                        else -> "재무정보"
                    }
                    Text(title)
                },
                actions = {
                    if (state is FinancialState.Success) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                        }
                    }
                    ThemeToggleButton()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (val currentState = state) {
            is FinancialState.NoStock -> {
                NoStockContent(modifier = Modifier.padding(paddingValues))
            }
            is FinancialState.Loading -> {
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
            is FinancialState.NoApiKey -> {
                NoApiKeyContent(modifier = Modifier.padding(paddingValues))
            }
            is FinancialState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FinancialTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(tab.label) }
                            )
                        }
                    }

                    // Tab Content with pull-to-refresh
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (selectedTab) {
                            FinancialTab.PROFITABILITY -> {
                                ProfitabilityContent(
                                    summary = currentState.summary,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            FinancialTab.STABILITY -> {
                                StabilityContent(
                                    summary = currentState.summary,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            is FinancialState.Error -> {
                ErrorContent(
                    message = currentState.message,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun NoStockContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "종목을 선택해주세요.\n검색 화면에서 종목을 검색하고 선택하세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = "재무정보를 불러오는 중...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun NoApiKeyContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "API 키가 설정되지 않았습니다.\n설정 화면에서 API 키를 입력해주세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ErrorCard(
            code = "ERROR",
            message = message,
            onRetry = onRetry
        )
    }
}
