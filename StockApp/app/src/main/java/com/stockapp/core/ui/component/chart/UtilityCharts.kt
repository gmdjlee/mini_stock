package com.stockapp.core.ui.component.chart

import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.stockapp.core.ui.theme.ChartGridDark
import com.stockapp.core.ui.theme.ChartGridLight
import com.stockapp.core.ui.theme.HistogramRed
import com.stockapp.core.ui.theme.HistogramTeal

/**
 * Utility charts for general use.
 * Contains: MacdHistogramChart, SimpleLineChart
 */

/**
 * Helper function to setup X-axis with date range based formatting.
 */
private fun XAxis.setupDateRangeAxis(dates: List<String>, gridColor: Int, textColor: Int) {
    position = XAxis.XAxisPosition.BOTTOM
    setDrawGridLines(true)
    this.gridColor = gridColor
    this.textColor = textColor
    enableGridDashedLine(10f, 10f, 0f)
    setLabelCount(ChartLabelCalculator.calculateOptimalLabelCount(dates.size), false)
    valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return if (index >= 0 && index < dates.size) {
                DateFormatter.formatForChartByDateRange(dates[index], dates)
            } else ""
        }
    }
}

/**
 * Helper function to create colored histogram entries.
 */
private fun createColoredHistogram(histogramValues: List<Double>): List<Int> =
    histogramValues.map { value ->
        if (value >= 0) HistogramTeal.toArgb() else HistogramRed.toArgb()
    }

/**
 * Simple MACD Bar Chart (histogram only)
 * Python reference style - for use when only histogram values are needed
 */
@Composable
fun MacdHistogramChart(
    dates: List<String>,
    histogramValues: List<Double>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    // All axis labels in black for dark theme support
    val textColor = Color.BLACK

    // Memoize chart data to avoid recreating on every recomposition (P2 fix)
    val barEntries = remember(histogramValues) {
        histogramValues.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value.toFloat())
        }
    }
    val histogramColors = remember(histogramValues) {
        createColoredHistogram(histogramValues)
    }

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()

                // X Axis - Python style with date range based formatting
                xAxis.setupDateRangeAxis(dates, gridColor, textColor)

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                }
                axisRight.isEnabled = false

                legend.isEnabled = false
            }
        },
        update = { chart ->
            // Python style: teal (#26A69A) for positive, red (#EF5350) for negative
            val barDataSet = BarDataSet(barEntries, "MACD Histogram").apply {
                colors = histogramColors
                setDrawValues(false)
            }

            chart.data = CombinedData().apply {
                setData(BarData(barDataSet).apply { barWidth = 0.7f })
            }
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.MACD)
    )
}

/**
 * SimpleLineChart - A simple single-series line chart
 * For displaying CMF, Fear/Greed, and other simple metrics
 *
 * @param dates List of date strings
 * @param values Data values to plot
 * @param lineColor Color for the line
 * @param label Legend label for the series
 */
@Composable
fun SimpleLineChart(
    dates: List<String>,
    values: List<Double>,
    lineColor: androidx.compose.ui.graphics.Color,
    label: String = "",
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    // All axis labels in black for dark theme support
    val textColor = Color.BLACK

    // Memoize chart data (P2 fix)
    val entries = remember(values) {
        values.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }
    }
    val lineColorArgb = remember(lineColor) { lineColor.toArgb() }

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()

                // X Axis - date range based formatting for weekly/monthly charts
                xAxis.setupDateRangeAxis(dates, gridColor, textColor)

                // Left Y Axis - black text for dark theme support
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                }

                // Right Y Axis (disabled)
                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = label.isNotEmpty()
                    this.textColor = textColor
                }
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, label).apply {
                color = lineColorArgb
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }

            chart.data = CombinedData().apply {
                setData(LineData(dataSet))
            }
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.DEFAULT)
    )
}
