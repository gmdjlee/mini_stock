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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.stockapp.core.ui.component.chart.GrowthRateMarkerView
import com.stockapp.core.ui.component.chart.IncomeBarMarkerView
import com.stockapp.core.ui.component.chart.setupCommonChartProperties
import com.stockapp.feature.financial.domain.model.FinancialSummary

/**
 * Profitability tab content.
 * Shows:
 * - Bar chart: 매출액, 영업이익, 당기순이익 by 결산년월
 * - Line chart: 매출액/영업이익/순이익 증가율
 * - Line chart: 자기자본/총자산 증가율
 */
@Composable
fun ProfitabilityContent(
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
        SummaryCard(summary)

        // 1. Bar Chart: 매출액, 영업이익, 당기순이익
        if (summary.hasProfitabilityData) {
            ChartCard(
                title = "손익 추이",
                subtitle = "단위: 억원"
            ) {
                IncomeBarChart(
                    periods = summary.displayPeriods,
                    revenues = summary.revenues,
                    operatingProfits = summary.operatingProfits,
                    netIncomes = summary.netIncomes
                )
            }
        }

        // 2. Line Chart: 성장률 (매출액, 영업이익, 순이익)
        if (summary.hasGrowthData) {
            ChartCard(
                title = "성장률 추이",
                subtitle = "매출액, 영업이익, 순이익 증가율 (%)"
            ) {
                GrowthRateLineChart(
                    periods = summary.displayPeriods,
                    revenueGrowth = summary.revenueGrowthRates,
                    operatingProfitGrowth = summary.operatingProfitGrowthRates,
                    netIncomeGrowth = summary.netIncomeGrowthRates
                )
            }
        }

        // 3. Line Chart: 자산 성장률
        if (summary.hasAssetGrowthData) {
            ChartCard(
                title = "자산 성장률",
                subtitle = "자기자본, 총자산 증가율 (%)"
            ) {
                AssetGrowthLineChart(
                    periods = summary.displayPeriods,
                    equityGrowth = summary.equityGrowthRates,
                    totalAssetsGrowth = summary.totalAssetsGrowthRates
                )
            }
        }

        // Empty state - show when no meaningful data exists
        if (!summary.hasProfitabilityData && !summary.hasGrowthData && !summary.hasAssetGrowthData) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "수익성 데이터가 없습니다.\n재무 데이터를 가져올 수 없거나 해당 종목의 재무정보가 제공되지 않습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: FinancialSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "최근 실적 요약",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "매출액",
                    value = summary.latestRevenue?.let { "${formatNumber(it)}억" } ?: "-"
                )
                SummaryItem(
                    label = "영업이익",
                    value = summary.latestOperatingProfit?.let { "${formatNumber(it)}억" } ?: "-"
                )
                SummaryItem(
                    label = "당기순이익",
                    value = summary.latestNetIncome?.let { "${formatNumber(it)}억" } ?: "-"
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
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
    }
}

@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun IncomeBarChart(
    periods: List<String>,
    revenues: List<Long>,
    operatingProfits: List<Long>,
    netIncomes: List<Long>,
    modifier: Modifier = Modifier
) {
    val chartTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val chartGridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val revenueColor = Color(0xFF4CAF50).toArgb()  // Green
    val operatingProfitColor = Color(0xFF2196F3).toArgb()  // Blue
    val netIncomeColor = Color(0xFFFF9800).toArgb()  // Orange

    // Memoize chart data (P2 fix)
    val revenueEntries = remember(revenues) {
        revenues.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value.toFloat())
        }
    }
    val operatingProfitEntries = remember(operatingProfits) {
        operatingProfits.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value.toFloat())
        }
    }
    val netIncomeEntries = remember(netIncomes) {
        netIncomes.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value.toFloat())
        }
    }

    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.apply {
                    isEnabled = true
                    textColor = chartTextColor
                }
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = chartTextColor
                    valueFormatter = IndexAxisValueFormatter(periods)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = chartGridColor
                    textColor = chartTextColor
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false

                setFitBars(true)
                animateY(500)

                // Enable interactivity (zoom, drag, touch)
                setupCommonChartProperties()

                // Marker for touch labeling
                marker = IncomeBarMarkerView(
                    context,
                    periods,
                    revenues,
                    operatingProfits,
                    netIncomes
                )
            }
        },
        update = { chart ->
            val groupSpace = 0.08f
            val barSpace = 0.02f
            val barWidth = 0.28f

            val revenueDataSet = BarDataSet(revenueEntries, "매출액").apply {
                color = revenueColor
                setDrawValues(false)
            }
            val operatingProfitDataSet = BarDataSet(operatingProfitEntries, "영업이익").apply {
                color = operatingProfitColor
                setDrawValues(false)
            }
            val netIncomeDataSet = BarDataSet(netIncomeEntries, "당기순이익").apply {
                color = netIncomeColor
                setDrawValues(false)
            }

            val barData = BarData(revenueDataSet, operatingProfitDataSet, netIncomeDataSet).apply {
                this.barWidth = barWidth
            }

            chart.data = barData
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = periods.size.toFloat()

            if (periods.size > 1) {
                chart.groupBars(0f, groupSpace, barSpace)
            }
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(top = 8.dp)
    )
}

@Composable
private fun GrowthRateLineChart(
    periods: List<String>,
    revenueGrowth: List<Double>,
    operatingProfitGrowth: List<Double>,
    netIncomeGrowth: List<Double>,
    modifier: Modifier = Modifier
) {
    val chartTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val chartGridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val revenueColor = Color(0xFF4CAF50).toArgb()  // Green
    val operatingProfitColor = Color(0xFF2196F3).toArgb()  // Blue
    val netIncomeColor = Color(0xFFFF9800).toArgb()  // Orange

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.apply {
                    isEnabled = true
                    textColor = chartTextColor
                }
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = chartTextColor
                    valueFormatter = IndexAxisValueFormatter(periods)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = chartGridColor
                    textColor = chartTextColor
                }
                axisRight.isEnabled = false

                animateX(500)

                // Enable interactivity (zoom, drag, touch)
                setupCommonChartProperties()

                // Marker for touch labeling
                marker = GrowthRateMarkerView(
                    context,
                    periods,
                    listOf("매출액 증가율", "영업이익 증가율", "순이익 증가율"),
                    listOf(revenueGrowth, operatingProfitGrowth, netIncomeGrowth)
                )
            }
        },
        update = { chart ->
            val revenueEntries = revenueGrowth.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val operatingProfitEntries = operatingProfitGrowth.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val netIncomeEntries = netIncomeGrowth.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }

            val dataSets = listOf(
                LineDataSet(revenueEntries, "매출액 증가율").apply {
                    color = revenueColor
                    setCircleColor(revenueColor)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                },
                LineDataSet(operatingProfitEntries, "영업이익 증가율").apply {
                    color = operatingProfitColor
                    setCircleColor(operatingProfitColor)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                },
                LineDataSet(netIncomeEntries, "순이익 증가율").apply {
                    color = netIncomeColor
                    setCircleColor(netIncomeColor)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                }
            )

            chart.data = LineData(dataSets)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(top = 8.dp)
    )
}

@Composable
private fun AssetGrowthLineChart(
    periods: List<String>,
    equityGrowth: List<Double>,
    totalAssetsGrowth: List<Double>,
    modifier: Modifier = Modifier
) {
    val chartTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val chartGridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val equityColor = Color(0xFF9C27B0).toArgb()  // Purple
    val totalAssetsColor = Color(0xFF00BCD4).toArgb()  // Cyan

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.apply {
                    isEnabled = true
                    textColor = chartTextColor
                }
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = chartTextColor
                    valueFormatter = IndexAxisValueFormatter(periods)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = chartGridColor
                    textColor = chartTextColor
                }
                axisRight.isEnabled = false

                animateX(500)

                // Enable interactivity (zoom, drag, touch)
                setupCommonChartProperties()

                // Marker for touch labeling
                marker = GrowthRateMarkerView(
                    context,
                    periods,
                    listOf("자기자본 증가율", "총자산 증가율"),
                    listOf(equityGrowth, totalAssetsGrowth)
                )
            }
        },
        update = { chart ->
            val equityEntries = equityGrowth.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val totalAssetsEntries = totalAssetsGrowth.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }

            val dataSets = listOf(
                LineDataSet(equityEntries, "자기자본 증가율").apply {
                    color = equityColor
                    setCircleColor(equityColor)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                },
                LineDataSet(totalAssetsEntries, "총자산 증가율").apply {
                    color = totalAssetsColor
                    setCircleColor(totalAssetsColor)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                }
            )

            chart.data = LineData(dataSets)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(top = 8.dp)
    )
}

private fun formatNumber(value: Long): String {
    return when {
        value >= 10000 -> String.format("%.1f만", value / 10000.0)
        value >= 1000 -> String.format("%.1f천", value / 1000.0)
        else -> value.toString()
    }
}
