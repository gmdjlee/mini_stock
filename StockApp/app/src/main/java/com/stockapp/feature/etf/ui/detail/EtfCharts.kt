package com.stockapp.feature.etf.ui.detail

import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.stockapp.core.ui.theme.ChartGridDark
import com.stockapp.core.ui.theme.ChartGridLight
import com.stockapp.feature.etf.domain.model.AmountHistory
import com.stockapp.feature.etf.domain.model.WeightHistory
import java.text.NumberFormat
import java.util.Locale

/**
 * ETF-specific chart components for stock detail visualization.
 */

private val AmountLineColor = androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
private val WeightLineColor = androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green

/**
 * Amount trend line chart showing total evaluation amount over time.
 */
@Composable
fun AmountTrendChart(
    amountHistory: List<AmountHistory>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = Color.BLACK

    if (amountHistory.isEmpty()) return

    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    granularity = 1f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatAmountShort(value.toLong())
                        }
                    }
                }

                axisRight.isEnabled = false
                legend.isEnabled = false
                setExtraOffsets(8f, 8f, 8f, 8f)
            }
        },
        update = { chart ->
            val entries = amountHistory.mapIndexed { index, data ->
                Entry(index.toFloat(), data.totalAmount.toFloat())
            }

            val dataSet = LineDataSet(entries, "평가금액").apply {
                color = AmountLineColor.toArgb()
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(AmountLineColor.toArgb())
                setDrawCircleHole(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = AmountLineColor.toArgb()
                fillAlpha = 30
            }

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < amountHistory.size) {
                        formatDateShort(amountHistory[index].date)
                    } else ""
                }
            }
            chart.xAxis.setLabelCount(minOf(5, amountHistory.size), false)

            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

/**
 * Weight trend line chart showing average weight percentage over time.
 */
@Composable
fun WeightTrendChart(
    weightHistory: List<WeightHistory>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = Color.BLACK

    if (weightHistory.isEmpty()) return

    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    granularity = 1f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format("%.2f%%", value)
                        }
                    }
                }

                axisRight.isEnabled = false
                legend.isEnabled = false
                setExtraOffsets(8f, 8f, 8f, 8f)
            }
        },
        update = { chart ->
            val entries = weightHistory.mapIndexed { index, data ->
                Entry(index.toFloat(), data.avgWeight.toFloat())
            }

            val dataSet = LineDataSet(entries, "평균비중").apply {
                color = WeightLineColor.toArgb()
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(WeightLineColor.toArgb())
                setDrawCircleHole(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = WeightLineColor.toArgb()
                fillAlpha = 30
            }

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < weightHistory.size) {
                        formatDateShort(weightHistory[index].date)
                    } else ""
                }
            }
            chart.xAxis.setLabelCount(minOf(5, weightHistory.size), false)

            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

/**
 * Format date string (YYYY-MM-DD) to short format (MM/DD).
 */
private fun formatDateShort(date: String): String {
    return try {
        val parts = date.split("-")
        if (parts.size >= 3) {
            "${parts[1]}/${parts[2]}"
        } else date
    } catch (e: Exception) {
        date
    }
}

/**
 * Format amount to short representation (억, 조).
 */
private fun formatAmountShort(amount: Long): String {
    return when {
        amount >= 1_000_000_000_000 -> String.format("%.1f조", amount / 1_000_000_000_000.0)
        amount >= 100_000_000 -> String.format("%.0f억", amount / 100_000_000.0)
        amount >= 10_000 -> String.format("%.0f만", amount / 10_000.0)
        else -> NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
    }
}
