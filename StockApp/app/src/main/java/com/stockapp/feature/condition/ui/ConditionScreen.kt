package com.stockapp.feature.condition.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.feature.condition.domain.model.Condition
import com.stockapp.feature.condition.domain.model.ConditionResult
import com.stockapp.feature.condition.domain.model.ConditionStock
import com.stockapp.feature.condition.domain.model.formattedChange
import com.stockapp.feature.condition.domain.model.formattedPrice
import com.stockapp.feature.condition.domain.model.isNegativeChange
import com.stockapp.feature.condition.domain.model.isPositiveChange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionScreen(
    onStockClick: (String) -> Unit = {},
    viewModel: ConditionVm = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedCondition by viewModel.selectedCondition.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedCondition != null) selectedCondition!!.name
                        else "조건검색"
                    )
                },
                navigationIcon = {
                    if (selectedCondition != null) {
                        IconButton(onClick = { viewModel.clearSearchResult() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기"
                            )
                        }
                    }
                },
                actions = {
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
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show search result if condition is selected
            if (selectedCondition != null) {
                when (val state = searchState) {
                    is ConditionSearchState.Idle -> {
                        // Should not happen when condition is selected
                    }
                    is ConditionSearchState.Loading -> {
                        LoadingContent()
                    }
                    is ConditionSearchState.Success -> {
                        SearchResultContent(
                            result = state.result,
                            onStockClick = onStockClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is ConditionSearchState.Error -> {
                        ErrorContent(
                            message = state.msg,
                            onRetry = { viewModel.retrySearch() }
                        )
                    }
                }
            } else {
                // Show condition list
                when (val state = listState) {
                    is ConditionListState.Loading -> {
                        LoadingContent()
                    }
                    is ConditionListState.Success -> {
                        ConditionListContent(
                            conditions = state.conditions,
                            onConditionClick = { viewModel.selectCondition(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is ConditionListState.Error -> {
                        ErrorContent(
                            message = state.msg,
                            onRetry = { viewModel.retryList() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionListContent(
    conditions: List<Condition>,
    onConditionClick: (Condition) -> Unit,
    modifier: Modifier = Modifier
) {
    if (conditions.isEmpty()) {
        EmptyContent(message = "등록된 조건검색이 없습니다")
        return
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "조건검색 목록 (${conditions.size}개)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(conditions, key = { it.idx }) { condition ->
            ConditionItem(
                condition = condition,
                onClick = { onConditionClick(condition) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConditionItem(
    condition: Condition,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = condition.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "조건 ${condition.idx}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "선택",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultContent(
    result: ConditionResult,
    onStockClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (result.stocks.isEmpty()) {
        EmptyContent(message = "검색 결과가 없습니다")
        return
    }

    LazyColumn(
        modifier = modifier
    ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "검색 결과",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${result.stocks.size}개 종목",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }

        items(result.stocks, key = { it.ticker }) { stock ->
            StockItem(
                stock = stock,
                onClick = { onStockClick(stock.ticker) }
            )
            HorizontalDivider()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StockItem(
    stock: ConditionStock,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = stock.name,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = stock.ticker,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stock.formattedPrice(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stock.formattedChange(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        stock.isPositiveChange() -> Color(0xFFF44336)
                        stock.isNegativeChange() -> Color(0xFF2196F3)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text("다시 시도")
            }
        }
    }
}

@Composable
private fun EmptyContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
