package com.stockapp.feature.market.ui.tabs

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockapp.core.ui.component.chart.FundFlowLineChart
import com.stockapp.feature.market.domain.model.FundFlowHistory
import com.stockapp.feature.market.ui.MarketVm

@Composable
fun FundFlowTab(viewModel: MarketVm) {
    val state by viewModel.fundFlowState.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DateRangeSelector(
            selectedRange = dateRange,
            onRangeSelected = { viewModel.setDateRange(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val currentState = state
        when (currentState) {
            is MarketVm.FundFlowState.Idle -> {}

            is MarketVm.FundFlowState.Loading -> {
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
                            text = "자금 동향 분석 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is MarketVm.FundFlowState.Success -> {
                FundFlowContent(data = currentState.data)
            }

            is MarketVm.FundFlowState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = currentState.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FundFlowContent(data: FundFlowHistory) {
    if (data.dates.isEmpty()) {
        Text("데이터가 없습니다.")
        return
    }

    // Enhanced summary card - latest data with larger fonts
    val latestForeign = data.foreignNetBuys.lastOrNull() ?: 0L
    val latestInstitution = data.institutionNetBuys.lastOrNull() ?: 0L
    val latestIndividual = data.individualNetBuys.lastOrNull() ?: 0L
    val latestDate = data.dates.lastOrNull() ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "투자자별 자금 동향",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatFlowDate(latestDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRowLarge(label = "외국인", value = latestForeign, labelColor = Color(0xFF1976D2))
            FlowRowLarge(label = "기관", value = latestInstitution, labelColor = Color(0xFF388E3C))
            FlowRowLarge(label = "개인", value = latestIndividual, labelColor = Color(0xFFD32F2F))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Cumulative trend card
    val cumForeign = data.foreignNetBuys.sum()
    val cumInstitution = data.institutionNetBuys.sum()
    val cumIndividual = data.individualNetBuys.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "누적 순매수 (기간 합계)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(label = "외국인", value = cumForeign)
            FlowRow(label = "기관", value = cumInstitution)
            FlowRow(label = "개인", value = cumIndividual)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Cumulative line chart
    val foreignCum = remember(data.foreignNetBuys) {
        data.foreignNetBuys.runningFold(0.0) { acc, v -> acc + v }.drop(1)
    }
    val instCum = remember(data.institutionNetBuys) {
        data.institutionNetBuys.runningFold(0.0) { acc, v -> acc + v }.drop(1)
    }
    val indivCum = remember(data.individualNetBuys) {
        data.individualNetBuys.runningFold(0.0) { acc, v -> acc + v }.drop(1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "누적 순매수 추이",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            FundFlowLineChart(
                dates = data.dates,
                foreignCumulative = foreignCum,
                institutionCumulative = instCum,
                individualCumulative = indivCum
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Daily history table
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "일별 추이",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("날짜", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("외국인", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Color(0xFF1976D2))
                Text("기관", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Color(0xFF388E3C))
                Text("개인", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Color(0xFFD32F2F))
            }

            HorizontalDivider()

            val recentCount = minOf(data.dates.size, 20)
            val startIdx = data.dates.size - recentCount

            for (i in data.dates.size - 1 downTo startIdx) {
                val rowBg = if ((data.dates.size - 1 - i) % 2 == 1) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatFlowDate(data.dates[i]),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    FlowValueText(
                        value = data.foreignNetBuys[i],
                        modifier = Modifier.weight(1f)
                    )
                    FlowValueText(
                        value = data.institutionNetBuys[i],
                        modifier = Modifier.weight(1f)
                    )
                    FlowValueText(
                        value = data.individualNetBuys[i],
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowRowLarge(label: String, value: Long, labelColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = labelColor
        )
        Text(
            text = formatAmount(value),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (value >= 0) Color(0xFFD32F2F) else Color(0xFF1976D2)
        )
    }
}

@Composable
private fun FlowRow(label: String, value: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = formatAmount(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (value >= 0) Color(0xFFD32F2F) else Color(0xFF1976D2)
        )
    }
}

@Composable
private fun FlowValueText(value: Long, modifier: Modifier = Modifier) {
    Text(
        text = formatCompactAmount(value),
        style = MaterialTheme.typography.bodySmall,
        color = if (value >= 0) Color(0xFFD32F2F) else Color(0xFF1976D2),
        modifier = modifier
    )
}

private fun formatAmount(value: Long): String {
    val absValue = kotlin.math.abs(value)
    val prefix = if (value >= 0) "+" else "-"
    return when {
        absValue >= 1_000_000_000_000 -> "$prefix${"%,.1f".format(absValue / 1_000_000_000_000.0)}조"
        absValue >= 100_000_000 -> "$prefix${"%,.0f".format(absValue / 100_000_000.0)}억"
        absValue >= 1_000_000 -> "$prefix${"%,.0f".format(absValue / 1_000_000.0)}백만"
        else -> "$prefix${"%,d".format(absValue)}"
    }
}

private fun formatCompactAmount(value: Long): String {
    val absValue = kotlin.math.abs(value)
    val prefix = if (value >= 0) "+" else "-"
    return when {
        absValue >= 100_000_000 -> "$prefix${"%,.0f".format(absValue / 100_000_000.0)}억"
        absValue >= 1_000_000 -> "$prefix${"%,.0f".format(absValue / 1_000_000.0)}백만"
        else -> "$prefix${"%,d".format(absValue)}"
    }
}

private fun formatFlowDate(yyyymmdd: String): String {
    if (yyyymmdd.length != 8) return yyyymmdd
    return "${yyyymmdd.substring(4, 6)}/${yyyymmdd.substring(6, 8)}"
}
