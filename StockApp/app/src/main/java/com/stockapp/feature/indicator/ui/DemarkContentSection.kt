package com.stockapp.feature.indicator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.component.chart.ChartCard
import com.stockapp.core.ui.component.chart.DemarkTDChart
import com.stockapp.feature.indicator.domain.model.DemarkSummary

/**
 * DeMark TD Setup Content Section.
 * Displays DeMark TD Setup with sell/buy counts and reversal signals.
 */
@Composable
internal fun DemarkContent(summary: DemarkSummary, timeframe: Timeframe) {
    // Python returns data in reverse chronological order (newest first)
    // take(N) gets newest N days, reversed() converts to chronological order for chart display
    val displayCount = minOf(IndicatorChartConfig.CHART_MAX_DAYS, summary.dates.size)
    val dates = summary.dates.take(displayCount).reversed()
    val sellSetupHistory = summary.sellSetupHistory.take(displayCount).reversed()
    val buySetupHistory = summary.buySetupHistory.take(displayCount).reversed()
    val mcapHistory = summary.mcapHistory.take(displayCount).reversed()

    // Title
    Text(
        text = "DeMark TD Setup (${timeframe.label})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    val currentState = when {
        summary.currentSellSetup >= 9 -> "매도 피로 (${summary.currentSellSetup}) - 하락 전환 가능"
        summary.currentBuySetup >= 9 -> "매수 피로 (${summary.currentBuySetup}) - 상승 전환 가능"
        summary.currentSellSetup > summary.currentBuySetup -> "상승 지속 (${summary.currentSellSetup})"
        summary.currentBuySetup > summary.currentSellSetup -> "하락 지속 (${summary.currentBuySetup})"
        else -> "중립"
    }

    // DeMark TD Setup Chart (EtfMonitor style with dual bars and optional market cap overlay)
    if (sellSetupHistory.isNotEmpty() && buySetupHistory.isNotEmpty()) {
        ChartCard(
            title = "DeMark TD Setup (${timeframe.label})",
            subtitle = "현재 상태: $currentState"
        ) {
            DemarkTDChart(
                dates = dates,
                sellSetupValues = sellSetupHistory,
                buySetupValues = buySetupHistory,
                mcapValues = mcapHistory
            )
        }
    }

    // Current Status Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.hasActiveSetup)
                Color(0xFFFFEB3B).copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TD Setup 상태",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sell Setup",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        text = "${summary.currentSellSetup}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Buy Setup",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "${summary.currentBuySetup}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }

    // Signals
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SignalCard(
            title = "매도피로",
            signal = summary.sellSignal,
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        SignalCard(
            title = "매수피로",
            signal = summary.buySignal,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }
}
