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
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.stockapp.core.ui.theme.ChartDefaultBlack
import com.stockapp.core.ui.theme.ChartGreen
import com.stockapp.core.ui.theme.ChartGridDark
import com.stockapp.core.ui.theme.ChartGridLight
import com.stockapp.core.ui.theme.ChartRed
import com.stockapp.core.ui.theme.ChartTertiary
import com.stockapp.core.ui.theme.SignalBuyStrong
import com.stockapp.core.ui.theme.SignalSellStrong

/**
 * MarketCapOscillatorChart - Market Cap & Supply Oscillator Chart
 * EtfMonitor style dual-axis chart with market cap and oscillator lines
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
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setExtraBottomOffset(10f)
                setDrawOrder(arrayOf(CombinedChart.DrawOrder.LINE))

                // X Axis
                xAxis.apply {
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

                // Left Y Axis (Market Cap)
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatMarketCapForChart(value.toDouble())
                        }
                    }
                }

                // Right Y Axis (Oscillator percentage)
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = ChartTertiary.toArgb()
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

            // Market Cap line (left axis) - black cubic bezier
            val mcapEntries = mcapValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val mcapDataSet = LineDataSet(mcapEntries, "시가총액").apply {
                color = ChartDefaultBlack.toArgb()
                lineWidth = 2.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.LEFT
            }

            // Oscillator line (right axis) - tertiary color cubic bezier
            // Convert oscillator values to percentage for display
            val oscillatorEntries = oscillatorValues.mapIndexed { index, value ->
                Entry(index.toFloat(), (value * 100).toFloat())  // Convert to percentage
            }
            val oscillatorDataSet = LineDataSet(oscillatorEntries, "오실레이터").apply {
                color = ChartTertiary.toArgb()
                lineWidth = 2.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.RIGHT
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
 * SupplyDemandBarChart - Foreign/Institution Net Buying Bar Chart
 * EtfMonitor style grouped bar chart
 *
 * @param dates List of date strings
 * @param foreignValues Foreign net buying values (in 억)
 * @param institutionValues Institution net buying values (in 억)
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

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setExtraBottomOffset(10f)

                // X Axis
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
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

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return "${value.toInt()}억"
                        }
                    }
                }
                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }
            }
        },
        update = { chart ->
            // Group bars by creating entries at different x positions
            val foreignEntries = foreignValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val institutionEntries = institutionValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }

            val foreignDataSet = BarDataSet(foreignEntries, "외국인").apply {
                color = SignalBuyStrong.toArgb()  // Red for foreign (Korean convention)
                setDrawValues(false)
            }

            val institutionDataSet = BarDataSet(institutionEntries, "기관").apply {
                color = SignalSellStrong.toArgb()  // Blue for institution
                setDrawValues(false)
            }

            val barData = BarData(foreignDataSet, institutionDataSet).apply {
                barWidth = 0.35f
            }

            // Group bars
            val groupSpace = 0.1f
            val barSpace = 0.05f
            barData.groupBars(0f, groupSpace, barSpace)

            chart.data = CombinedData().apply {
                setData(barData)
            }

            // Adjust X axis to show grouped bars properly
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = foreignValues.size.toFloat()

            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.DEFAULT)
    )
}

/**
 * MarketDepositChart - Market Deposit/Credit Chart
 * For displaying customer deposit and credit loan trends
 *
 * @param dates List of date strings
 * @param depositValues Deposit values (in 원)
 * @param creditValues Credit loan values (in 원)
 */
@Composable
fun MarketDepositChart(
    dates: List<String>,
    depositValues: List<Double>,
    creditValues: List<Double>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setExtraBottomOffset(10f)

                // X Axis
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
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

                // Left Y Axis (Deposit)
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val trillion = value / 1_000_000_000_000
                            return String.format("%.1f조", trillion)
                        }
                    }
                }

                // Right Y Axis (Credit)
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = ChartRed.toArgb()
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val trillion = value / 1_000_000_000_000
                            return String.format("%.1f조", trillion)
                        }
                    }
                }

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }
            }
        },
        update = { chart ->
            val combinedData = CombinedData()

            // Deposit line (left axis)
            val depositEntries = depositValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val depositDataSet = LineDataSet(depositEntries, "고객예탁금").apply {
                color = ChartDefaultBlack.toArgb()
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.LEFT
            }

            // Credit line (right axis)
            val creditEntries = creditValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val creditDataSet = LineDataSet(creditEntries, "신용융자").apply {
                color = ChartRed.toArgb()
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.RIGHT
            }

            combinedData.setData(LineData(depositDataSet, creditDataSet))

            chart.data = combinedData
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.DEFAULT)
    )
}

/**
 * Simple Line Chart for single series
 * Reusable chart for CMF, Fear/Greed, etc.
 *
 * @param dates List of date strings
 * @param values Data values
 * @param lineColor Color for the line
 * @param label Label for the dataset
 */
@Composable
fun SimpleLineChart(
    dates: List<String>,
    values: List<Double>,
    lineColor: androidx.compose.ui.graphics.Color,
    label: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setExtraBottomOffset(10f)

                // X Axis
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
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

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                }
                axisRight.isEnabled = false

                legend.isEnabled = false

                // Marker
                marker = CustomMarkerView(ctx, dates) { value ->
                    String.format("%.4f", value)
                }
            }
        },
        update = { chart ->
            val entries = values.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val dataSet = LineDataSet(entries, label).apply {
                color = lineColor.toArgb()
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
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
