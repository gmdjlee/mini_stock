package com.stockapp.core.ui.component.chart

import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
import com.stockapp.core.ui.theme.ChartRed
import com.stockapp.core.ui.theme.OscillatorBlue
import com.stockapp.core.ui.theme.OscillatorOrange
import com.stockapp.core.ui.theme.TabBlue

/**
 * Analysis charts for AnalysisScreen.
 * Contains: MarketCapOscillatorChart, SupplyDemandBarChart
 */

/**
 * Helper function to configure common chart properties.
 */
private fun CombinedChart.setupCommonChartProperties() {
    description.isEnabled = false
    setTouchEnabled(true)
    isDragEnabled = true
    setScaleEnabled(true)
    setPinchZoom(true)
    setDrawGridBackground(false)
    setExtraBottomOffset(10f)
}

/**
 * Helper function to setup X-axis with data count based formatting.
 */
private fun XAxis.setupDataCountAxis(dates: List<String>, gridColor: Int, textColor: Int) {
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
                DateFormatter.formatForChartByDataCount(dates[index], dates.size)
            } else ""
        }
    }
}

/**
 * MarketCapOscillatorChart - Market Cap & Supply Oscillator Chart
 * Python reference style dual-axis chart with market cap and oscillator lines
 *
 * @param dates List of date strings
 * @param mcapValues Market cap values (in 억)
 * @param oscillatorValues Oscillator values (as percentage, e.g., 0.001 = 0.1%)
 */
@Composable
fun MarketCapOscillatorChart(
    dates: List<String>,
    mcapValues: List<Double>,
    oscillatorValues: List<Double>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()
                setDrawOrder(arrayOf(CombinedChart.DrawOrder.LINE))

                // X Axis - Python style
                xAxis.setupDataCountAxis(dates, gridColor, textColor)

                // Left Y Axis (Market Cap) - Python style: blue text
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = OscillatorBlue.toArgb()  // Blue like Python
                    enableGridDashedLine(10f, 10f, 0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatMarketCapForChart(value.toDouble())
                        }
                    }
                }

                // Right Y Axis (Oscillator percentage) - Python style: orange text
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = OscillatorOrange.toArgb()  // Orange like Python
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format("%.2f%%", value)
                        }
                    }
                }

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }

                // Marker
                marker = MarketCapMarkerView(ctx, dates) { dataSetIndex ->
                    dataSetIndex == 1  // Second dataset is oscillator
                }
            }
        },
        update = { chart ->
            val combinedData = CombinedData()

            // Market Cap line (left axis) - Python style: blue (#1976D2)
            val mcapEntries = mcapValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val mcapDataSet = LineDataSet(mcapEntries, "Market Cap").apply {
                color = OscillatorBlue.toArgb()  // Blue like Python
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
                setDrawFilled(true)
                fillColor = OscillatorBlue.toArgb()
                fillAlpha = 50  // 20% alpha like Python
            }

            // Oscillator line (right axis) - Python style: orange (#FF5722)
            // Convert oscillator values to percentage for display
            val oscillatorEntries = oscillatorValues.mapIndexed { index, value ->
                Entry(index.toFloat(), (value * 100).toFloat())  // Convert to percentage
            }
            val oscillatorDataSet = LineDataSet(oscillatorEntries, "Oscillator (%)").apply {
                color = OscillatorOrange.toArgb()  // Orange like Python
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.RIGHT
            }

            combinedData.setData(LineData(mcapDataSet, oscillatorDataSet))

            chart.data = combinedData
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.MARKET_CAP_OSCILLATOR)
    )
}

/**
 * SupplyDemandBarChart - Grouped bar chart for foreign/institution net buying
 * Python reference style with foreign (red) and institution (blue) bars
 *
 * @param dates List of date strings
 * @param foreignValues Foreign net buying values (in 억원)
 * @param institutionValues Institution net buying values (in 억원)
 */
@Composable
fun SupplyDemandBarChart(
    dates: List<String>,
    foreignValues: List<Double>,
    institutionValues: List<Double>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    // Python style colors
    val foreignColor = ChartRed.toArgb()   // Red for foreign
    val institutionColor = TabBlue.toArgb() // Blue for institution

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()

                // X Axis
                xAxis.apply {
                    setupDataCountAxis(dates, gridColor, textColor)
                    axisMinimum = -0.5f
                    axisMaximum = dates.size.toFloat() - 0.5f
                }

                // Left Y Axis (Net buying in 억원)
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return when {
                                kotlin.math.abs(value) >= 10000 -> String.format("%.0f만", value / 10000)
                                kotlin.math.abs(value) >= 1000 -> String.format("%.1f천", value / 1000)
                                else -> String.format("%.0f억", value)
                            }
                        }
                    }
                }

                // Right Y Axis (disabled)
                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }

                // Marker
                marker = SupplyDemandMarkerView(ctx, dates, foreignValues, institutionValues)
            }
        },
        update = { chart ->
            val groupSpace = 0.1f
            val barSpace = 0.05f
            val barWidth = 0.4f

            // Foreign bars
            val foreignEntries = foreignValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val foreignDataSet = BarDataSet(foreignEntries, "외국인").apply {
                color = foreignColor
                setDrawValues(false)
            }

            // Institution bars
            val institutionEntries = institutionValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val institutionDataSet = BarDataSet(institutionEntries, "기관").apply {
                color = institutionColor
                setDrawValues(false)
            }

            val barData = BarData(foreignDataSet, institutionDataSet).apply {
                this.barWidth = barWidth
            }

            chart.data = CombinedData().apply {
                setData(barData)
            }
            chart.barData.groupBars(-0.5f, groupSpace, barSpace)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.DEFAULT)
    )
}
