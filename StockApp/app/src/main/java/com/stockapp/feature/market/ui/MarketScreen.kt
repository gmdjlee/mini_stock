package com.stockapp.feature.market.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.stockapp.feature.market.domain.model.ChangeDirection
import com.stockapp.feature.market.domain.model.MarketSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    viewModel: MarketVm = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedDays by viewModel.selectedDays.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시장 지표") },
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
            when (val currentState = state) {
                is MarketState.Loading -> {
                    LoadingContent()
                }

                is MarketState.Success -> {
                    MarketContent(
                        summary = currentState.summary,
                        selectedDays = selectedDays,
                        onDaysSelected = { viewModel.selectDays(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is MarketState.Error -> {
                    ErrorContent(
                        message = currentState.msg,
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketContent(
    summary: MarketSummary,
    selectedDays: Int,
    onDaysSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period selector
        PeriodSelector(
            selectedDays = selectedDays,
            onDaysSelected = onDaysSelected
        )

        // Summary cards
        Text(
            text = "현재 시장 상태",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "고객예탁금",
                value = summary.depositFormatted,
                changeDirection = summary.depositChangeDirection,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "신용융자",
                value = summary.creditLoanFormatted,
                changeDirection = summary.creditLoanChangeDirection,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "신용잔고",
                value = summary.creditBalanceFormatted,
                changeDirection = summary.creditBalanceChangeDirection,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "신용비율",
                value = summary.creditRatioFormatted,
                changeDirection = summary.creditRatioChangeDirection,
                modifier = Modifier.weight(1f)
            )
        }

        // Charts
        Text(
            text = "추이 차트",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Deposit chart
        ChartCard(title = "고객예탁금 추이") {
            LineChartContent(
                values = summary.depositHistory.reversed().map { it.toDouble() / 1_000_000_000_000 },
                label = "조원"
            )
        }

        // Credit loan chart
        ChartCard(title = "신용융자 추이") {
            LineChartContent(
                values = summary.creditLoanHistory.reversed().map { it.toDouble() / 1_000_000_000_000 },
                label = "조원"
            )
        }

        // Credit ratio chart
        ChartCard(title = "신용비율 추이") {
            LineChartContent(
                values = summary.creditRatioHistory.reversed(),
                label = "%"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PeriodSelector(
    selectedDays: Int,
    onDaysSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MarketVm.DAYS_OPTIONS.forEach { days ->
            FilterChip(
                selected = selectedDays == days,
                onClick = { onDaysSelected(days) },
                label = { Text("${days}일") }
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    changeDirection: ChangeDirection,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = when (changeDirection) {
                        ChangeDirection.UP -> Icons.Default.ArrowUpward
                        ChangeDirection.DOWN -> Icons.Default.ArrowDownward
                        ChangeDirection.NEUTRAL -> Icons.Default.Remove
                    },
                    contentDescription = null,
                    tint = when (changeDirection) {
                        ChangeDirection.UP -> Color(0xFFF44336)
                        ChangeDirection.DOWN -> Color(0xFF2196F3)
                        ChangeDirection.NEUTRAL -> Color(0xFF9E9E9E)
                    },
                    modifier = Modifier.height(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.height(180.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun LineChartContent(
    values: List<Double>,
    label: String
) {
    if (values.isEmpty()) {
        NoChartData()
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries { series(values) }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside
            ),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun NoChartData() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "차트 데이터 없음",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
