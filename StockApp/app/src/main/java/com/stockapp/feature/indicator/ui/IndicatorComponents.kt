package com.stockapp.feature.indicator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.theme.ChartPrimary
import com.stockapp.core.ui.theme.ElderBlue
import com.stockapp.core.ui.theme.ElderGreen
import com.stockapp.core.ui.theme.ElderRed

/**
 * Chart display constants for indicator visualization.
 */
internal object IndicatorChartConfig {
    /** Maximum days to display in charts (6 months of weekly data) */
    const val CHART_MAX_DAYS = 180
}

// ========== Timeframe Selector ==========

@Composable
internal fun TimeframeSelector(
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Timeframe.entries.forEach { timeframe ->
            val isSelected = timeframe == selectedTimeframe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isSelected) ChartPrimary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) ChartPrimary else Color.Transparent,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onTimeframeSelect(timeframe) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeframe.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ========== Common Components ==========

@Composable
internal fun MetricCard(
    title: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SignalCard(
    title: String,
    signal: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = signal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun DataNotLoaded() {
    LoadingContent(modifier = Modifier.fillMaxWidth().height(200.dp))
}

@Composable
internal fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun ErrorContent(
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

@Composable
internal fun NoStockContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "기술 지표",
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

// ========== Helper Functions ==========

internal fun getElderColor(color: String): Color = when (color) {
    "green" -> ElderGreen
    "red" -> ElderRed
    else -> ElderBlue
}

/**
 * Prepare data for chart display by taking most recent N items and reversing to chronological order.
 * Python returns data in reverse chronological order (newest first).
 * take(N) gets newest N days, reversed() converts to chronological order for chart display.
 */
internal fun <T> List<T>.prepareForChart(maxDays: Int = IndicatorChartConfig.CHART_MAX_DAYS): List<T> =
    take(minOf(maxDays, size)).reversed()

/**
 * Prepare nullable Double list for chart display with null filtering.
 */
@JvmName("prepareNullableDoubleForChart")
internal fun List<Double?>.prepareNullableForChart(maxDays: Int = IndicatorChartConfig.CHART_MAX_DAYS): List<Double> =
    take(minOf(maxDays, size)).mapNotNull { it }.reversed()

/**
 * Prepare nullable Int list for chart display with null filtering and conversion to Double.
 */
@JvmName("prepareNullableIntForChart")
internal fun List<Int?>.prepareNullableForChart(maxDays: Int = IndicatorChartConfig.CHART_MAX_DAYS): List<Double> =
    take(minOf(maxDays, size)).mapNotNull { it?.toDouble() }.reversed()
