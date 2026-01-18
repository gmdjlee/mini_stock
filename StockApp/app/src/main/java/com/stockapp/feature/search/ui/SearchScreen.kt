package com.stockapp.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.cache.CacheState
import com.stockapp.core.ui.component.ErrorCard
import com.stockapp.core.ui.component.stockinput.StockInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onStockClick: (String) -> Unit,
    viewModel: SearchVm = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    val history by viewModel.history.collectAsState()
    val cacheCount by viewModel.cacheCount.collectAsState()
    val cacheState by viewModel.cacheState.collectAsState()

    val suggestions = when (val s = state) {
        is SearchState.Results -> s.stocks
        else -> emptyList()
    }

    val isLoading = state is SearchState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("종목 검색") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Cache status indicator
            CacheStatusBar(
                cacheState = cacheState,
                cacheCount = cacheCount,
                onRefresh = viewModel::refreshCache
            )

            Spacer(modifier = Modifier.height(8.dp))

            StockInputField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                suggestions = suggestions,
                onSelect = { stock ->
                    viewModel.onStockSelected(stock)
                    onStockClick(stock.ticker)
                },
                isLoading = isLoading,
                history = history,
                onHistorySelect = { stock ->
                    viewModel.onStockSelected(stock)
                    onStockClick(stock.ticker)
                }
            )

            // Error state
            if (state is SearchState.Error) {
                val errorState = state as SearchState.Error
                Spacer(modifier = Modifier.height(16.dp))
                ErrorCard(
                    code = errorState.code,
                    message = errorState.msg,
                    onRetry = viewModel::retry
                )
            }

            // Empty state for no results
            if (state is SearchState.Results && suggestions.isEmpty() && query.isNotBlank()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "검색 결과가 없습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Idle state hint
            if (state is SearchState.Idle && history.isEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "종목명 또는 코드를 검색하세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun CacheStatusBar(
    cacheState: CacheState,
    cacheCount: Int,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (cacheState) {
                is CacheState.Idle -> {
                    if (cacheCount > 0) {
                        Text(
                            text = "종목 $cacheCount 개 로드됨",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "종목 데이터 없음",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is CacheState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "종목 데이터 로딩 중...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is CacheState.Ready -> {
                    Text(
                        text = "종목 ${cacheState.count} 개 로드됨",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is CacheState.Error -> {
                    Text(
                        text = "로딩 실패: ${cacheState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (cacheState !is CacheState.Loading) {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "새로고침",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
