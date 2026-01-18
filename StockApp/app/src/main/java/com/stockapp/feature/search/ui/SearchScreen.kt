package com.stockapp.feature.search.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
