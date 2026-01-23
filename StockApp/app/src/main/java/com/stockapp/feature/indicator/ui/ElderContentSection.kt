package com.stockapp.feature.indicator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.component.chart.ChartCard
import com.stockapp.core.ui.component.chart.ElderImpulseChart
import com.stockapp.core.ui.component.chart.MacdChart
import com.stockapp.core.ui.component.chart.MacdHistogramChart
import com.stockapp.feature.indicator.domain.model.ElderSummary

/**
 * Elder Impulse Content Section.
 * Displays Elder Impulse System with EMA13, MACD, and impulse colors.
 */
@Composable
internal fun ElderContent(summary: ElderSummary, timeframe: Timeframe) {
    // Prepare data for chart display (newest N days in chronological order)
    val dates = summary.dates.prepareForChart()
    val mcapHistory = summary.mcapHistory.prepareForChart()
    val ema13History = summary.ema13History.prepareForChart()
    val impulseStates = summary.impulseStates.prepareForChart()
    val macdLineHistory = summary.macdLineHistory.prepareForChart()
    val signalLineHistory = summary.signalLineHistory.prepareForChart()
    val macdHistHistory = summary.macdHistHistory.prepareForChart()

    // Title with current status
    Text(
        text = "Elder Impulse System (${timeframe.label})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    // Elder Impulse Chart (EtfMonitor style with market cap and impulse markers)
    if (mcapHistory.isNotEmpty() && ema13History.isNotEmpty() && impulseStates.isNotEmpty()) {
        ChartCard(
            title = "Elder Impulse (${timeframe.label})",
            subtitle = "현재 상태: ${summary.colorLabel}"
        ) {
            ElderImpulseChart(
                dates = dates,
                priceValues = mcapHistory,
                ema13Values = ema13History,
                impulseStates = impulseStates
            )
        }
    }

    // MACD Chart (EtfMonitor style with MACD line, Signal line, and Histogram)
    if (macdLineHistory.isNotEmpty() && signalLineHistory.isNotEmpty() && macdHistHistory.isNotEmpty()) {
        ChartCard(title = "MACD") {
            MacdChart(
                dates = dates,
                macdValues = macdLineHistory,
                signalValues = signalLineHistory,
                histogramValues = macdHistHistory
            )
        }
    } else if (macdHistHistory.isNotEmpty()) {
        // Fallback to histogram only if full MACD data not available
        ChartCard(title = "MACD Histogram") {
            MacdHistogramChart(
                dates = dates,
                histogramValues = macdHistHistory
            )
        }
    }

    // Impulse Signal Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getElderColor(summary.currentColor).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Impulse 신호",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.impulseSignal,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = getElderColor(summary.currentColor)
            )
        }
    }
}
