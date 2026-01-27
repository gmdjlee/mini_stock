package com.stockapp.feature.etf.ui.tabs.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.component.stockinput.StockInputField
import com.stockapp.feature.etf.domain.model.ContainingEtfInfo
import com.stockapp.feature.etf.domain.model.StockAnalysisResult
import com.stockapp.feature.etf.ui.detail.AmountTrendChart
import com.stockapp.feature.etf.ui.detail.WeightTrendChart
import com.stockapp.feature.search.domain.model.Stock

/**
 * Sealed class for stock analysis state
 */
sealed class StockAnalysisState {
    data object Initial : StockAnalysisState()
    data object Loading : StockAnalysisState()
    data class Success(val result: StockAnalysisResult) : StockAnalysisState()
    data class NotFound(val query: String) : StockAnalysisState()
    data class Error(val message: String) : StockAnalysisState()
}

@Composable
fun StockAnalysisTab(
    state: StockAnalysisState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNavigateToAnalysis: (stockCode: String, stockName: String) -> Unit,
    suggestions: List<Stock> = emptyList(),
    isLoading: Boolean = false,
    onStockSelect: (Stock) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            // FAB for navigating to Analysis screen - shown when analysis result available
            if (state is StockAnalysisState.Success) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onNavigateToAnalysis(
                            state.result.stockCode,
                            state.result.stockName
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null
                        )
                    },
                    text = { Text("수급분석") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Autocomplete search field
            StockInputField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                suggestions = suggestions,
                onSelect = onStockSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                isLoading = isLoading,
                placeholder = "종목코드 또는 종목명 검색"
            )

            // Content based on state
            when (state) {
                is StockAnalysisState.Initial -> InitialContent()
                is StockAnalysisState.Loading -> StatisticsLoadingContent()
                is StockAnalysisState.NotFound -> StatisticsEmptyContent(
                    title = "종목을 찾을 수 없습니다",
                    description = "'${state.query}' 에 해당하는 종목이 ETF에 편입되어 있지 않습니다.",
                    icon = Icons.Default.Search
                )
                is StockAnalysisState.Error -> StatisticsErrorContent(
                    message = state.message,
                    title = "분석 실패"
                )
                is StockAnalysisState.Success -> AnalysisResultContent(
                    result = state.result
                )
            }
        }
    }
}

@Composable
private fun InitialContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "종목을 검색하세요",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "해당 종목이 어떤 ETF에 편입되어 있는지\n분석 결과를 확인할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnalysisResultContent(result: StockAnalysisResult) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = result.stockName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = result.stockCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "총 편입금액",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatAmount(result.totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "편입 ETF 수",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${result.etfCount}개",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Amount trend chart
        item {
            if (result.amountHistory.isNotEmpty()) {
                Text(
                    text = "ETF 내 총 평가금액 추이",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                AmountTrendChart(
                    amountHistory = result.amountHistory,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Weight trend chart
        item {
            if (result.weightHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ETF 내 평균 비중 추이",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                WeightTrendChart(
                    weightHistory = result.weightHistory,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Containing ETFs list
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "편입 ETF 목록",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            ContainingEtfTableHeader()
            HorizontalDivider()
        }

        if (result.containingEtfs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "편입된 ETF가 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = result.containingEtfs.sortedByDescending { it.amount },
                key = { it.etfCode }
            ) { etf ->
                ContainingEtfRow(etf = etf)
                HorizontalDivider()
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
    }
}

@Composable
private fun ContainingEtfTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ETF명",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "비중",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "평가금액",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContainingEtfRow(etf: ContainingEtfInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = etf.etfName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = etf.etfCode,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = String.format("%.2f%%", etf.weight),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End
        )
        Text(
            text = formatAmount(etf.amount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End
        )
    }
}

