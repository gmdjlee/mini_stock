package com.stockapp.feature.indicator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.component.chart.ChartCard
import com.stockapp.core.ui.component.chart.SimpleLineChart
import com.stockapp.core.ui.component.chart.TrendSignalChart
import com.stockapp.core.ui.theme.ChartPurple
import com.stockapp.feature.indicator.domain.model.TrendSummary

/**
 * Trend Signal Content Section.
 * Displays MA, CMF, and Fear/Greed indicators with buy/sell signals.
 */
@Composable
internal fun TrendContent(summary: TrendSummary, timeframe: Timeframe) {
    // Python returns data in reverse chronological order (newest first)
    // take(N) gets newest N days, reversed() converts to chronological order for chart display
    val displayCount = minOf(IndicatorChartConfig.CHART_MAX_DAYS, summary.dates.size)
    val dates = summary.dates.take(displayCount).reversed()
    val priceHistory = summary.priceHistory.take(displayCount).reversed()
    val fearGreedHistory = summary.fearGreedHistory.take(displayCount).reversed()
    val cmfHistory = summary.cmfHistory.take(displayCount).reversed()

    // MA10 for overlay line (using ma10History if available, otherwise ma20)
    val ma10History = summary.ma10History.take(displayCount)
        .mapNotNull { it?.toDouble() }
        .reversed()

    // MA20 for MA overlay line - use ma20 to avoid overlap with priceHistory (which uses ma10)
    val ma20History = summary.ma20History.take(displayCount)
        .mapNotNull { it?.toDouble() }
        .reversed()

    // Calculate signal indices for the displayed data range
    // Signals are calculated on reversed data (chronological order)
    val maSignalHistory = summary.maSignalHistory.take(displayCount).reversed()

    // Generate signal indices based on TREND_SIGNAL_CHART.md spec:
    // - Primary Buy: maSignal == 1 (3 conditions: High>Prev_High, Close>MA, CMF>0)
    // - Additional Buy: maSignal == 0 but CMF > 0 AND Close > MA (2 conditions met with MA)
    // - Primary Sell: maSignal == -1 (3 conditions: Low<Prev_Low, Close<MA, CMF<0)
    // - Additional Sell: maSignal == 0 but CMF < 0 AND Close < MA (2 conditions met with MA)
    val primaryBuySignals = mutableListOf<Int>()
    val additionalBuySignals = mutableListOf<Int>()
    val primarySellSignals = mutableListOf<Int>()
    val additionalSellSignals = mutableListOf<Int>()

    for (i in maSignalHistory.indices) {
        val signal = maSignalHistory[i]

        when (signal) {
            // Primary signals: all 3 conditions met (from Python ma_signal)
            1 -> primaryBuySignals.add(i)
            -1 -> primarySellSignals.add(i)
            // Additional signals: check CMF and Close vs MA conditions
            0 -> {
                // Need CMF and price data for additional signal calculation
                if (i < cmfHistory.size && i < priceHistory.size && i < ma20History.size) {
                    val cmf = cmfHistory[i]
                    val close = priceHistory[i]
                    val ma = ma20History.getOrNull(i)

                    if (ma != null) {
                        // Additional Buy: CMF > 0 AND Close > MA
                        if (cmf > 0 && close > ma) {
                            additionalBuySignals.add(i)
                        }
                        // Additional Sell: CMF < 0 AND Close < MA
                        else if (cmf < 0 && close < ma) {
                            additionalSellSignals.add(i)
                        }
                    }
                }
            }
        }
    }

    // Format latest date for subtitle
    val latestDate = dates.lastOrNull()?.let { date ->
        val normalized = date.replace("-", "")
        if (normalized.length >= 8) {
            "${normalized.substring(0, 4)}-${normalized.substring(4, 6)}-${normalized.substring(6, 8)}"
        } else date
    } ?: ""
    val timeframeLabel = when (timeframe) {
        Timeframe.DAILY -> "d"
        Timeframe.WEEKLY -> "w"
        Timeframe.MONTHLY -> "m"
    }

    // Title with current status
    Text(
        text = "추세 시그널 (MA/CMF/Fear&Greed)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    // Main Trend Signal Chart with all signals
    if (priceHistory.isNotEmpty() && fearGreedHistory.isNotEmpty()) {
        ChartCard(
            title = "추세 시그널 (${timeframe.label})",
            subtitle = "최신 데이터: $latestDate ($timeframeLabel)"
        ) {
            TrendSignalChart(
                dates = dates,
                priceValues = priceHistory,
                fearGreedValues = fearGreedHistory,
                ma10Values = ma20History,  // Use MA20 to avoid overlap with priceHistory (which uses MA10)
                primaryBuySignals = primaryBuySignals,
                additionalBuySignals = additionalBuySignals,
                primarySellSignals = primarySellSignals,
                additionalSellSignals = additionalSellSignals
            )
        }
    }

    // Metrics Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            title = "CMF",
            value = String.format("%.3f", summary.currentCmf),
            label = summary.cmfLabel,
            color = if (summary.currentCmf > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Fear/Greed",
            value = String.format("%.3f", summary.currentFearGreed),
            label = summary.fearGreedLabel,
            color = when {
                summary.currentFearGreed > 0.5 -> Color(0xFFF44336)
                summary.currentFearGreed < -0.5 -> Color(0xFF2196F3)
                else -> Color(0xFF9E9E9E)
            },
            modifier = Modifier.weight(1f)
        )
    }

    // CMF Chart (EtfMonitor style)
    if (cmfHistory.isNotEmpty()) {
        ChartCard(title = "CMF (Chaikin Money Flow)") {
            SimpleLineChart(
                dates = dates,
                values = cmfHistory,
                lineColor = Color(0xFF2196F3),
                label = "CMF"
            )
        }
    }

    // Fear/Greed Chart (EtfMonitor style)
    if (fearGreedHistory.isNotEmpty()) {
        ChartCard(title = "Fear/Greed Index") {
            SimpleLineChart(
                dates = dates,
                values = fearGreedHistory,
                lineColor = ChartPurple,
                label = "Fear/Greed"
            )
        }
    }
}
