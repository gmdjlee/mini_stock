package com.stockapp.feature.analysis.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.ui.component.chart.ChartCard
import com.stockapp.core.ui.component.chart.MarketCapOscillatorChart
import com.stockapp.core.ui.component.chart.SupplyDemandBarChart
import com.stockapp.feature.analysis.domain.model.AnalysisSummary
import com.stockapp.feature.analysis.domain.model.SupplySignal
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisVm = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = state) {
                        is AnalysisState.Success -> "${s.summary.name} 수급 분석"
                        else -> "수급 분석"
                    }
                    Text(title)
                },
                actions = {
                    if (state is AnalysisState.Success || state is AnalysisState.Error) {
                        IconButton(
                            onClick = viewModel::refresh,
                            enabled = !isRefreshing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "새로고침"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val currentState = state) {
            is AnalysisState.NoStock -> {
                NoStockContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is AnalysisState.Loading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is AnalysisState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AnalysisContent(
                        summary = currentState.summary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            is AnalysisState.Error -> {
                ErrorContent(
                    message = currentState.msg,
                    onRetry = viewModel::retry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun NoStockContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "수급 분석",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "검색에서 종목을 선택하세요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnalysisContent(
    summary: AnalysisSummary,
    modifier: Modifier = Modifier
) {
    // Python returns data in reverse chronological order (newest first)
    // take(N) gets newest N days, reversed() converts to chronological order for chart display
    val displayCount = minOf(120, summary.dates.size)
    val dates = summary.dates.take(displayCount).reversed()
    val mcapHistory = summary.mcapHistory.take(displayCount).reversed()
    val for5dHistory = summary.for5dHistory.take(displayCount).reversed()
    val ins5dHistory = summary.ins5dHistory.take(displayCount).reversed()

    // Calculate oscillator values (data is now in chronological order)
    val oscillatorValues = mcapHistory.mapIndexed { index, mcap ->
        if (mcap > 0 && index < for5dHistory.size && index < ins5dHistory.size) {
            (for5dHistory[index] + ins5dHistory[index]) / (mcap * 10000)  // Scaled
        } else {
            0.0
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stock header
        StockHeader(summary = summary)

        // Supply signal card
        SupplySignalCard(summary = summary)

        // Market cap card
        MetricCard(
            title = "시가총액",
            value = formatTrillion(summary.mcapTrillion),
            unit = "조원"
        )

        // Foreign/Institution cards in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "외국인 순매수",
                value = formatBillion(summary.for5dBillion),
                unit = "억원",
                valueColor = getValueColor(summary.for5dBillion),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "기관 순매수",
                value = formatBillion(summary.ins5dBillion),
                unit = "억원",
                valueColor = getValueColor(summary.ins5dBillion),
                modifier = Modifier.weight(1f)
            )
        }

        // Supply ratio
        MetricCard(
            title = "수급 비율",
            value = formatPercent(summary.supplyRatio * 100),
            unit = "%",
            valueColor = getValueColor(summary.supplyRatio)
        )

        // Market Cap & Oscillator Chart (EtfMonitor style)
        if (mcapHistory.isNotEmpty()) {
            ChartCard(
                title = "시가총액 & 수급 오실레이터",
                subtitle = "시가총액(좌축), 오실레이터(우축)"
            ) {
                MarketCapOscillatorChart(
                    dates = dates,
                    mcapValues = mcapHistory.map { it * 10000 },  // Convert to 억
                    oscillatorValues = oscillatorValues
                )
            }
        }

        // Foreign/Institution Net Buying Chart (EtfMonitor style)
        if (for5dHistory.isNotEmpty()) {
            ChartCard(
                title = "외국인/기관 순매수 추이",
                subtitle = "외국인(빨강), 기관(파랑)"
            ) {
                SupplyDemandBarChart(
                    dates = dates.takeLast(60),
                    foreignValues = for5dHistory.takeLast(60).map { it * 10 },  // Convert to 억
                    institutionValues = ins5dHistory.takeLast(60).map { it * 10 }  // Convert to 억
                )
            }
        }

        // Data info (dates is in chronological order: oldest first, newest last)
        if (dates.isNotEmpty()) {
            Text(
                text = "기간: ${dates.first()} ~ ${dates.last()} (${dates.size}일)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun StockHeader(
    summary: AnalysisSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = summary.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary.ticker,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SupplySignalCard(
    summary: AnalysisSummary,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = getSignalDisplay(summary.supplySignal)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "수급 신호",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
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

// Helper functions

private fun getSignalDisplay(signal: SupplySignal): Triple<ImageVector, Color, String> {
    return when (signal) {
        SupplySignal.STRONG_BUY -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            Color(0xFF4CAF50),
            "강력 매수"
        )
        SupplySignal.BUY -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            Color(0xFF8BC34A),
            "매수"
        )
        SupplySignal.NEUTRAL -> Triple(
            Icons.AutoMirrored.Filled.TrendingFlat,
            Color(0xFF9E9E9E),
            "중립"
        )
        SupplySignal.SELL -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            Color(0xFFFF9800),
            "매도"
        )
        SupplySignal.STRONG_SELL -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            Color(0xFFF44336),
            "강력 매도"
        )
    }
}

@Composable
private fun getValueColor(value: Double): Color {
    return when {
        value > 0 -> Color(0xFFF44336) // Red for positive (Korean market convention)
        value < 0 -> Color(0xFF2196F3) // Blue for negative
        else -> MaterialTheme.colorScheme.onSurface
    }
}

private val trillionFormat = DecimalFormat("#,##0.0")
private val billionFormat = DecimalFormat("#,##0")
private val percentFormat = DecimalFormat("#,##0.000")

private fun formatTrillion(value: Double): String = trillionFormat.format(value)
private fun formatBillion(value: Double): String {
    val inBillion = value * 10 // Convert from billion to 억
    return if (inBillion >= 0) "+${billionFormat.format(inBillion)}"
    else billionFormat.format(inBillion)
}
private fun formatPercent(value: Double): String {
    return if (value >= 0) "+${percentFormat.format(value)}"
    else percentFormat.format(value)
}
