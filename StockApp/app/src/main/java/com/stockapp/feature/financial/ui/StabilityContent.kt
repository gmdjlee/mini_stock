package com.stockapp.feature.financial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.stockapp.feature.financial.domain.model.FinancialSummary

/**
 * Stability tab content.
 * Shows charts for:
 * - 부채비율 (Debt Ratio)
 * - 유동비율 (Current Ratio)
 * - 차입금 의존도 (Borrowing Dependency)
 */
@Composable
fun StabilityContent(
    summary: FinancialSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards
        StabilitySummaryCard(summary)

        // Charts
        if (summary.hasStabilityData) {
            // Combined stability chart
            ChartCard(
                title = "안정성 지표 추이",
                subtitle = "부채비율, 유동비율, 차입금 의존도 (%)"
            ) {
                StabilityLineChart(
                    periods = summary.displayPeriods,
                    debtRatios = summary.debtRatios,
                    currentRatios = summary.currentRatios,
                    borrowingDependencies = summary.borrowingDependencies
                )
            }

            // Individual charts for detailed view
            if (summary.debtRatios.any { it != 0.0 }) {
                ChartCard(
                    title = "부채비율",
                    subtitle = "부채총계 / 자기자본 × 100 (%)"
                ) {
                    SingleRatioLineChart(
                        periods = summary.displayPeriods,
                        values = summary.debtRatios,
                        label = "부채비율",
                        color = Color(0xFFF44336)  // Red
                    )
                }
            }

            if (summary.currentRatios.any { it != 0.0 }) {
                ChartCard(
                    title = "유동비율",
                    subtitle = "유동자산 / 유동부채 × 100 (%)"
                ) {
                    SingleRatioLineChart(
                        periods = summary.displayPeriods,
                        values = summary.currentRatios,
                        label = "유동비율",
                        color = Color(0xFF4CAF50)  // Green
                    )
                }
            }

            if (summary.borrowingDependencies.any { it != 0.0 }) {
                ChartCard(
                    title = "차입금 의존도",
                    subtitle = "차입금 / 총자산 × 100 (%)"
                ) {
                    SingleRatioLineChart(
                        periods = summary.displayPeriods,
                        values = summary.borrowingDependencies,
                        label = "차입금 의존도",
                        color = Color(0xFFFF9800)  // Orange
                    )
                }
            }
        } else {
            Text(
                text = "안정성 데이터가 없습니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}

@Composable
private fun StabilitySummaryCard(summary: FinancialSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "최근 안정성 지표",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StabilitySummaryItem(
                    label = "부채비율",
                    value = summary.latestDebtRatio?.let { formatPercent(it) } ?: "-",
                    evaluation = evaluateDebtRatio(summary.latestDebtRatio)
                )
                StabilitySummaryItem(
                    label = "유동비율",
                    value = summary.latestCurrentRatio?.let { formatPercent(it) } ?: "-",
                    evaluation = evaluateCurrentRatio(summary.latestCurrentRatio)
                )
                StabilitySummaryItem(
                    label = "차입금 의존도",
                    value = summary.borrowingDependencies.lastOrNull()?.let { formatPercent(it) } ?: "-",
                    evaluation = evaluateBorrowingDependency(summary.borrowingDependencies.lastOrNull())
                )
            }
        }
    }
}

@Composable
private fun StabilitySummaryItem(
    label: String,
    value: String,
    evaluation: StabilityEvaluation
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = evaluation.label,
            style = MaterialTheme.typography.labelSmall,
            color = evaluation.color
        )
    }
}

@Composable
private fun StabilityLineChart(
    periods: List<String>,
    debtRatios: List<Double>,
    currentRatios: List<Double>,
    borrowingDependencies: List<Double>,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val debtColor = Color(0xFFF44336).toArgb()  // Red
    val currentColor = Color(0xFF4CAF50).toArgb()  // Green
    val borrowingColor = Color(0xFFFF9800).toArgb()  // Orange

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    this.textColor = textColor
                    valueFormatter = IndexAxisValueFormatter(periods)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = gridColor
                    this.textColor = textColor
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false

                animateX(500)
            }
        },
        update = { chart ->
            val dataSets = mutableListOf<LineDataSet>()

            if (debtRatios.any { it != 0.0 }) {
                val entries = debtRatios.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                dataSets.add(LineDataSet(entries, "부채비율").apply {
                    color = debtColor
                    setCircleColor(debtColor)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                })
            }

            if (currentRatios.any { it != 0.0 }) {
                val entries = currentRatios.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                dataSets.add(LineDataSet(entries, "유동비율").apply {
                    color = currentColor
                    setCircleColor(currentColor)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                })
            }

            if (borrowingDependencies.any { it != 0.0 }) {
                val entries = borrowingDependencies.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                dataSets.add(LineDataSet(entries, "차입금 의존도").apply {
                    color = borrowingColor
                    setCircleColor(borrowingColor)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                })
            }

            chart.data = if (dataSets.isNotEmpty()) LineData(dataSets) else null
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(top = 8.dp)
    )
}

@Composable
private fun SingleRatioLineChart(
    periods: List<String>,
    values: List<Double>,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val lineColor = color.toArgb()

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    this.textColor = textColor
                    valueFormatter = IndexAxisValueFormatter(periods)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = gridColor
                    this.textColor = textColor
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false

                animateX(500)
            }
        },
        update = { chart ->
            val entries = values.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }

            val dataSet = LineDataSet(entries, label).apply {
                this.color = lineColor
                setCircleColor(lineColor)
                lineWidth = 2.5f
                circleRadius = 4f
                setDrawFilled(true)
                fillColor = lineColor
                fillAlpha = 30
                setDrawValues(true)
                valueTextColor = textColor
                valueTextSize = 10f
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(top = 8.dp)
    )
}

private data class StabilityEvaluation(
    val label: String,
    val color: Color
)

private fun evaluateDebtRatio(value: Double?): StabilityEvaluation {
    return when {
        value == null -> StabilityEvaluation("-", Color.Gray)
        value < 100 -> StabilityEvaluation("양호", Color(0xFF4CAF50))
        value < 200 -> StabilityEvaluation("보통", Color(0xFFFF9800))
        else -> StabilityEvaluation("주의", Color(0xFFF44336))
    }
}

private fun evaluateCurrentRatio(value: Double?): StabilityEvaluation {
    return when {
        value == null -> StabilityEvaluation("-", Color.Gray)
        value >= 200 -> StabilityEvaluation("양호", Color(0xFF4CAF50))
        value >= 100 -> StabilityEvaluation("보통", Color(0xFFFF9800))
        else -> StabilityEvaluation("주의", Color(0xFFF44336))
    }
}

private fun evaluateBorrowingDependency(value: Double?): StabilityEvaluation {
    return when {
        value == null -> StabilityEvaluation("-", Color.Gray)
        value < 30 -> StabilityEvaluation("양호", Color(0xFF4CAF50))
        value < 50 -> StabilityEvaluation("보통", Color(0xFFFF9800))
        else -> StabilityEvaluation("주의", Color(0xFFF44336))
    }
}

private fun formatPercent(value: Double): String {
    return String.format("%.1f%%", value)
}
