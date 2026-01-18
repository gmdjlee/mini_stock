package com.stockapp.core.ui.component.chart

import android.graphics.Color
import android.graphics.DashPathEffect
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
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.stockapp.core.ui.theme.ChartDefaultBlack
import com.stockapp.core.ui.theme.ChartGreen
import com.stockapp.core.ui.theme.ChartGridDark
import com.stockapp.core.ui.theme.ChartGridLight
import com.stockapp.core.ui.theme.ChartOrange
import com.stockapp.core.ui.theme.ChartPurple
import com.stockapp.core.ui.theme.ChartRed
import com.stockapp.core.ui.theme.ElderBlue
import com.stockapp.core.ui.theme.ElderGreen
import com.stockapp.core.ui.theme.ElderRed
import com.stockapp.core.ui.theme.SignalBuyStrong
import com.stockapp.core.ui.theme.SignalBuyWeak
import com.stockapp.core.ui.theme.SignalSellStrong
import com.stockapp.core.ui.theme.SignalSellWeak
import com.github.mikephil.charting.charts.ScatterChart

/**
 * MacdChart - MACD chart with histogram and signal line
 * EtfMonitor style implementation using MPAndroidChart
 *
 * @param dates List of date strings
 * @param macdValues MACD line values
 * @param signalValues Signal line values
 * @param histogramValues Histogram values (MACD - Signal)
 */
@Composable
fun MacdChart(
    dates: List<String>,
    macdValues: List<Double>,
    signalValues: List<Double>,
    histogramValues: List<Double>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    val lineColor1 = ChartDefaultBlack.toArgb()  // MACD line
    val lineColor2 = ChartOrange.toArgb()        // Signal line (dashed)

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
                setDrawOrder(arrayOf(
                    CombinedChart.DrawOrder.BAR,
                    CombinedChart.DrawOrder.LINE
                ))

                // X Axis
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridColor = gridColor
                    textColor = textColor
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

                // Left Y Axis (MACD values)
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                }

                // Right Y Axis (disabled)
                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }

                // Marker
                marker = MacdMarkerView(ctx, dates, macdValues, signalValues)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()

            // Histogram bars
            val barEntries = histogramValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val barDataSet = BarDataSet(barEntries, "Histogram").apply {
                colors = histogramValues.map { value ->
                    if (value >= 0) ChartGreen.toArgb() else ChartRed.toArgb()
                }
                setDrawValues(false)
            }
            combinedData.setData(BarData(barDataSet).apply { barWidth = 0.7f })

            // MACD and Signal lines
            val macdEntries = macdValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val signalEntries = signalValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }

            val macdDataSet = LineDataSet(macdEntries, "MACD").apply {
                color = lineColor1
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }

            val signalDataSet = LineDataSet(signalEntries, "Signal").apply {
                color = lineColor2
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                enableDashedLine(10f, 5f, 0f)
            }

            combinedData.setData(LineData(macdDataSet, signalDataSet))

            chart.data = combinedData
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.MACD)
    )
}

/**
 * TrendSignalChart - Trend Signal chart with price, MA, and Fear/Greed
 * EtfMonitor style with dual Y-axis and signal markers
 *
 * @param dates List of date strings
 * @param priceValues Close price values
 * @param fearGreedValues Fear/Greed index values (-1 to 1.5)
 * @param buySignals List of indices with buy signals
 * @param sellSignals List of indices with sell signals
 */
@Composable
fun TrendSignalChart(
    dates: List<String>,
    priceValues: List<Double>,
    fearGreedValues: List<Double>,
    buySignals: List<Int> = emptyList(),
    sellSignals: List<Int> = emptyList(),
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
                setDrawOrder(arrayOf(
                    CombinedChart.DrawOrder.LINE,
                    CombinedChart.DrawOrder.SCATTER
                ))

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

                // Left Y Axis (Price)
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                }

                // Right Y Axis (Fear/Greed: -1 to 1.5)
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = ChartPurple.toArgb()
                    axisMinimum = -1.5f
                    axisMaximum = 1.5f
                }

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }

                // Marker
                marker = TrendSignalMarkerView(ctx, dates, priceValues, fearGreedValues)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()

            // Price line (left axis)
            val priceEntries = priceValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val priceDataSet = LineDataSet(priceEntries, "Price").apply {
                color = ChartDefaultBlack.toArgb()
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.LEFT
                enableDashedLine(10f, 5f, 0f)
            }

            // Fear/Greed line (right axis)
            val fearGreedEntries = fearGreedValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val fearGreedDataSet = LineDataSet(fearGreedEntries, "Fear/Greed").apply {
                color = ChartPurple.toArgb()
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.RIGHT
            }

            combinedData.setData(LineData(priceDataSet, fearGreedDataSet))

            // Signal markers
            if (buySignals.isNotEmpty() || sellSignals.isNotEmpty()) {
                val scatterDataSets = mutableListOf<ScatterDataSet>()

                // Buy signals (red triangles - Korean convention)
                if (buySignals.isNotEmpty()) {
                    val buyEntries = buySignals.mapNotNull { index ->
                        if (index >= 0 && index < priceValues.size) {
                            Entry(index.toFloat(), priceValues[index].toFloat())
                        } else null
                    }
                    if (buyEntries.isNotEmpty()) {
                        val buyDataSet = ScatterDataSet(buyEntries, "Buy").apply {
                            color = SignalBuyStrong.toArgb()
                            setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
                            scatterShapeSize = 24f
                            setDrawValues(false)
                            axisDependency = YAxis.AxisDependency.LEFT
                        }
                        scatterDataSets.add(buyDataSet)
                    }
                }

                // Sell signals (blue inverted triangles)
                if (sellSignals.isNotEmpty()) {
                    val sellEntries = sellSignals.mapNotNull { index ->
                        if (index >= 0 && index < priceValues.size) {
                            Entry(index.toFloat(), priceValues[index].toFloat())
                        } else null
                    }
                    if (sellEntries.isNotEmpty()) {
                        val sellDataSet = ScatterDataSet(sellEntries, "Sell").apply {
                            color = SignalSellStrong.toArgb()
                            scatterShapeSize = 24f
                            setDrawValues(false)
                            axisDependency = YAxis.AxisDependency.LEFT
                            shapeRenderer = InvertedTriangleShapeRenderer()
                        }
                        scatterDataSets.add(sellDataSet)
                    }
                }

                if (scatterDataSets.isNotEmpty()) {
                    combinedData.setData(ScatterData(scatterDataSets))
                }
            }

            chart.data = combinedData
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.TREND_SIGNAL)
    )
}

/**
 * ElderImpulseChart - Elder Impulse System chart
 * Shows market cap with impulse color markers
 *
 * @param dates List of date strings
 * @param mcapValues Market cap values (in 억)
 * @param ema13Values EMA13 values
 * @param impulseStates Impulse states: 1=bullish, 0=neutral, -1=bearish
 */
@Composable
fun ElderImpulseChart(
    dates: List<String>,
    mcapValues: List<Double>,
    ema13Values: List<Double>,
    impulseStates: List<Int>,
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
                setDrawOrder(arrayOf(
                    CombinedChart.DrawOrder.LINE,
                    CombinedChart.DrawOrder.SCATTER
                ))

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

                // Right Y Axis (EMA13)
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = Color.GRAY
                }

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }

                // Marker
                marker = ElderImpulseMarkerView(ctx, dates, mcapValues, impulseStates)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()

            // Market Cap line (left axis)
            val mcapEntries = mcapValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val mcapDataSet = LineDataSet(mcapEntries, "시가총액").apply {
                color = ChartDefaultBlack.toArgb()
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.LEFT
            }

            // EMA13 line (right axis)
            val ema13Entries = ema13Values.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val ema13DataSet = LineDataSet(ema13Entries, "EMA13").apply {
                color = Color.GRAY
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.RIGHT
                enableDashedLine(10f, 5f, 0f)
            }

            combinedData.setData(LineData(mcapDataSet, ema13DataSet))

            // Impulse markers
            val impulseEntries = impulseStates.mapIndexedNotNull { index, state ->
                if (index < mcapValues.size) {
                    Entry(index.toFloat(), mcapValues[index].toFloat())
                } else null
            }
            val impulseDataSet = ScatterDataSet(impulseEntries, "Impulse").apply {
                colors = impulseStates.map { state ->
                    when (state) {
                        1 -> ElderGreen.toArgb()
                        -1 -> ElderRed.toArgb()
                        else -> ElderBlue.toArgb()
                    }
                }
                setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                scatterShapeSize = 12f
                setDrawValues(false)
                axisDependency = YAxis.AxisDependency.LEFT
            }
            combinedData.setData(ScatterData(impulseDataSet))

            chart.data = combinedData
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.ELDER_IMPULSE)
    )
}

/**
 * DemarkTDChart - DeMark TD Setup chart
 * Shows sell/buy setup counts as filled area chart
 *
 * @param dates List of date strings
 * @param sellSetupValues Sell setup counts (positive)
 * @param buySetupValues Buy setup counts (shown as negative for visual separation)
 * @param mcapValues Optional market cap values for overlay
 */
@Composable
fun DemarkTDChart(
    dates: List<String>,
    sellSetupValues: List<Int>,
    buySetupValues: List<Int>,
    mcapValues: List<Double> = emptyList(),
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
                setDrawOrder(arrayOf(
                    CombinedChart.DrawOrder.BAR,
                    CombinedChart.DrawOrder.LINE
                ))

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

                // Left Y Axis (TD Setup counts)
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    axisMinimum = -15f
                    axisMaximum = 15f
                }

                // Right Y Axis (Market Cap - optional)
                axisRight.apply {
                    isEnabled = mcapValues.isNotEmpty()
                    setDrawGridLines(false)
                    this.textColor = Color.GRAY
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatMarketCapForChart(value.toDouble())
                        }
                    }
                }

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }

                // Marker
                marker = DemarkTDMarkerView(ctx, dates, sellSetupValues, buySetupValues)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()

            // Sell Setup bars (positive - red with 50% alpha fill)
            val sellEntries = sellSetupValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val sellDataSet = BarDataSet(sellEntries, "Sell Setup").apply {
                color = ChartRed.copy(alpha = 0.5f).toArgb()
                setDrawValues(false)
            }

            // Buy Setup bars (negative - green with 50% alpha fill)
            val buyEntries = buySetupValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), -value.toFloat())  // Negative for visual
            }
            val buyDataSet = BarDataSet(buyEntries, "Buy Setup").apply {
                color = ChartGreen.copy(alpha = 0.5f).toArgb()
                setDrawValues(false)
            }

            combinedData.setData(BarData(sellDataSet, buyDataSet).apply {
                barWidth = 0.4f
            })

            // Market Cap line (optional overlay)
            if (mcapValues.isNotEmpty()) {
                val mcapEntries = mcapValues.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                val mcapDataSet = LineDataSet(mcapEntries, "시가총액").apply {
                    color = ChartDefaultBlack.toArgb()
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    axisDependency = YAxis.AxisDependency.RIGHT
                }
                combinedData.setData(LineData(mcapDataSet))
            }

            chart.data = combinedData
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.DEMARK_TD)
    )
}

/**
 * Simple MACD Bar Chart (histogram only)
 * For use when only histogram values are needed
 */
@Composable
fun MacdHistogramChart(
    dates: List<String>,
    histogramValues: List<Double>,
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
            }
        },
        update = { chart ->
            val barEntries = histogramValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val barDataSet = BarDataSet(barEntries, "MACD Histogram").apply {
                colors = histogramValues.map { value ->
                    if (value >= 0) ChartGreen.toArgb() else ChartRed.toArgb()
                }
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
