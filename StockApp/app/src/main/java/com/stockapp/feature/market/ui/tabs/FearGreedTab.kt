package com.stockapp.feature.market.ui.tabs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.component.chart.FearGreedLineChart
import com.stockapp.feature.market.domain.model.FearGreedHistory
import com.stockapp.feature.market.domain.model.FearGreedSignal
import com.stockapp.feature.market.domain.model.IndicatorComponent
import com.stockapp.feature.market.domain.model.MarketFearGreed
import com.stockapp.feature.market.ui.MarketVm
import com.stockapp.feature.market.ui.component.FearGreedGauge
import com.stockapp.feature.market.ui.component.StatItem
import com.stockapp.feature.market.ui.component.StatsRow
import com.stockapp.feature.market.ui.component.fearGreedSignalColor

@Composable
fun FearGreedTab(viewModel: MarketVm) {
    val fearGreedState by viewModel.fearGreedState.collectAsState()
    val historyState by viewModel.fearGreedHistoryState.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val currentFearGreed = fearGreedState
        when (currentFearGreed) {
            is MarketVm.FearGreedState.Idle -> {}

            is MarketVm.FearGreedState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "공포/탐욕 지수 계산 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is MarketVm.FearGreedState.Success -> {
                FearGreedGaugeCard(data = currentFearGreed.data)
                Spacer(modifier = Modifier.height(12.dp))

                // Stats row from history (yesterday, 1 week, 1 month)
                val currentHistory = historyState
                if (currentHistory is MarketVm.FearGreedHistoryState.Success) {
                    StatsRowFromHistory(currentHistory.data)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                IndicatorBreakdown(data = currentFearGreed.data)
            }

            is MarketVm.FearGreedState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = currentFearGreed.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History chart section
        DateRangeSelector(
            selectedRange = dateRange,
            onRangeSelected = { viewModel.setDateRange(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        val currentHistory = historyState
        when (currentHistory) {
            is MarketVm.FearGreedHistoryState.Idle -> {}
            is MarketVm.FearGreedHistoryState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is MarketVm.FearGreedHistoryState.Success -> {
                FearGreedChartCard(history = currentHistory.data)
            }

            is MarketVm.FearGreedHistoryState.Error -> {
                Text(
                    text = currentHistory.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FearGreedGaugeCard(data: MarketFearGreed) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "공포 & 탐욕 지수",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Semi-circle gauge
            FearGreedGauge(
                score = data.overallScore,
                signal = data.signal
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = data.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRowFromHistory(history: FearGreedHistory) {
    if (history.dates.isEmpty()) return

    val size = history.scores.size
    val yesterdayScore = if (size > 1) history.scores[size - 2] else null
    val weekAgoScore = if (size > 5) history.scores[size - 6] else null
    val monthAgoScore = if (size > 20) history.scores[size - 21] else null

    val items = listOf(
        StatItem(
            label = "어제",
            value = yesterdayScore?.let { "%.0f".format(it) } ?: "—",
            color = yesterdayScore?.let {
                fearGreedSignalColor(FearGreedSignal.fromScore(it))
            } ?: Color.Gray
        ),
        StatItem(
            label = "1주 전",
            value = weekAgoScore?.let { "%.0f".format(it) } ?: "—",
            color = weekAgoScore?.let {
                fearGreedSignalColor(FearGreedSignal.fromScore(it))
            } ?: Color.Gray
        ),
        StatItem(
            label = "1달 전",
            value = monthAgoScore?.let { "%.0f".format(it) } ?: "—",
            color = monthAgoScore?.let {
                fearGreedSignalColor(FearGreedSignal.fromScore(it))
            } ?: Color.Gray
        )
    )

    StatsRow(items = items)
}

@Composable
private fun FearGreedChartCard(history: FearGreedHistory) {
    if (history.dates.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "변동 추이",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            FearGreedLineChart(
                dates = history.dates,
                scores = history.scores,
                indexValues = history.indexValues
            )
        }
    }
}

@Composable
private fun IndicatorBreakdown(data: MarketFearGreed) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "구성 지표",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            IndicatorRow(data.momentum)
            IndicatorRow(data.rsi)
            IndicatorRow(data.volatility)
            IndicatorRow(data.investorFlow)
            IndicatorRow(data.shortSelling)
        }
    }
}

@Composable
private fun IndicatorRow(component: IndicatorComponent) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = component.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "%.0f".format(component.normalizedScore),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = indicatorScoreColor(component.normalizedScore)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { (component.normalizedScore / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = indicatorScoreColor(component.normalizedScore),
            trackColor = Color(0xFFE0E0E0)
        )

        Text(
            text = component.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun indicatorScoreColor(score: Double): Color = when {
    score >= 80 -> Color(0xFFD32F2F)
    score >= 60 -> Color(0xFFFF5722)
    score >= 40 -> Color(0xFF9E9E9E)
    score >= 20 -> Color(0xFF2196F3)
    else -> Color(0xFF1565C0)
}
