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
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.stockapp.core.ui.theme.AdditionalBuy
import com.stockapp.core.ui.theme.AdditionalSell
import com.stockapp.core.ui.theme.ChartDefaultBlack
import com.stockapp.core.ui.theme.ChartGreen
import com.stockapp.core.ui.theme.ChartGridDark
import com.stockapp.core.ui.theme.ChartGridLight
import com.stockapp.core.ui.theme.ChartOrange
import com.stockapp.core.ui.theme.ChartRed
import com.stockapp.core.ui.theme.DemarkBlue
import com.stockapp.core.ui.theme.DemarkRed
import com.stockapp.core.ui.theme.ElderGreen
import com.stockapp.core.ui.theme.ElderRed
import com.stockapp.core.ui.theme.FearGreedGreen
import com.stockapp.core.ui.theme.FearGreedRed
import com.stockapp.core.ui.theme.PrimaryBuy
import com.stockapp.core.ui.theme.PrimarySell
import com.stockapp.core.ui.theme.TabBlue
import com.stockapp.core.ui.theme.TabGray
import com.stockapp.core.ui.theme.TabOrange
import com.github.mikephil.charting.charts.ScatterChart

/**
 * MacdChart - MACD chart with histogram and signal line
 * Python reference style implementation using MPAndroidChart
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

    // Python style colors: MACD blue (#2196F3), Signal orange (#FF9800)
    val lineColor1 = com.stockapp.core.ui.theme.MacdBlue.toArgb()  // MACD line - blue
    val lineColor2 = com.stockapp.core.ui.theme.MacdSignalOrange.toArgb()  // Signal line (dashed) - orange

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
            // Histogram bars - Python style: teal (#26A69A) for positive, red (#EF5350) for negative
            val barEntries = histogramValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val barDataSet = BarDataSet(barEntries, "Histogram").apply {
                colors = histogramValues.map { value ->
                    if (value >= 0) {
                        com.stockapp.core.ui.theme.HistogramTeal.toArgb()  // Teal for positive
                    } else {
                        com.stockapp.core.ui.theme.HistogramRed.toArgb()   // Red for negative
                    }
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
 * Python reference style with dual Y-axis and signal markers
 * Style: {ticker}.KS Weekly Strategy + Fear & Greed
 *
 * @param dates List of date strings
 * @param priceValues Close price values
 * @param ma10Values MA10 values (optional, for overlay)
 * @param fearGreedValues Fear/Greed index values (-1 to 1.5)
 * @param primaryBuySignals List of indices with primary buy signals (bullish trend)
 * @param additionalBuySignals List of indices with additional buy signals
 * @param primarySellSignals List of indices with primary sell signals (bearish trend)
 * @param additionalSellSignals List of indices with additional sell signals
 */
@Composable
fun TrendSignalChart(
    dates: List<String>,
    priceValues: List<Double>,
    fearGreedValues: List<Double>,
    ma10Values: List<Double> = emptyList(),
    primaryBuySignals: List<Int> = emptyList(),
    additionalBuySignals: List<Int> = emptyList(),
    primarySellSignals: List<Int> = emptyList(),
    additionalSellSignals: List<Int> = emptyList(),
    // Legacy parameters for backward compatibility
    buySignals: List<Int> = emptyList(),
    sellSignals: List<Int> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    // Merge legacy signals with new ones
    val effectivePrimaryBuy = if (primaryBuySignals.isEmpty()) buySignals else primaryBuySignals
    val effectivePrimarySell = if (primarySellSignals.isEmpty()) sellSignals else primarySellSignals

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

                // X Axis - Python style
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

                // Right Y Axis (Fear/Greed: -1.5 to 1.5) - Python style
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = TabOrange.toArgb()  // Orange like Python
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
            val lineDataSets = mutableListOf<LineDataSet>()

            // Close price line (left axis) - Python style: tab:blue, solid
            val priceEntries = priceValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val priceDataSet = LineDataSet(priceEntries, "Close").apply {
                color = TabBlue.toArgb()  // tab:blue
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                axisDependency = YAxis.AxisDependency.LEFT
            }
            lineDataSets.add(priceDataSet)

            // MA10 line (left axis) - Python style: tab:orange, dashed
            if (ma10Values.isNotEmpty()) {
                val ma10Entries = ma10Values.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                val ma10DataSet = LineDataSet(ma10Entries, "MA10").apply {
                    color = TabOrange.toArgb()  // tab:orange
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                    axisDependency = YAxis.AxisDependency.LEFT
                    enableDashedLine(10f, 5f, 0f)  // Dashed line
                }
                lineDataSets.add(ma10DataSet)
            }

            // Fear/Greed line (right axis) - Python style: orange
            val fearGreedEntries = fearGreedValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val fearGreedDataSet = LineDataSet(fearGreedEntries, "FG Index").apply {
                color = TabOrange.toArgb()  // Orange like Python
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                axisDependency = YAxis.AxisDependency.RIGHT
            }
            lineDataSets.add(fearGreedDataSet)

            // Threshold lines for Fear/Greed (±0.5) - Python style
            // +0.5 threshold (Greed - red dashed)
            val threshold05Entries = dates.indices.map { Entry(it.toFloat(), 0.5f) }
            val threshold05DataSet = LineDataSet(threshold05Entries, "").apply {
                color = FearGreedRed.toArgb()
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 10f, 0f)
                axisDependency = YAxis.AxisDependency.RIGHT
                isHighlightEnabled = false
            }
            lineDataSets.add(threshold05DataSet)

            // -0.5 threshold (Fear - green dashed)
            val thresholdNeg05Entries = dates.indices.map { Entry(it.toFloat(), -0.5f) }
            val thresholdNeg05DataSet = LineDataSet(thresholdNeg05Entries, "").apply {
                color = FearGreedGreen.toArgb()
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 10f, 0f)
                axisDependency = YAxis.AxisDependency.RIGHT
                isHighlightEnabled = false
            }
            lineDataSets.add(thresholdNeg05DataSet)

            combinedData.setData(LineData(lineDataSets))

            // Signal markers - Python style
            val scatterDataSets = mutableListOf<ScatterDataSet>()

            // Additional Buy (light red, smaller) - Python style
            if (additionalBuySignals.isNotEmpty()) {
                val entries = additionalBuySignals.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "Add. Buy").apply {
                        color = AdditionalBuy.toArgb()
                        setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
                        scatterShapeSize = 16f
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                        shapeRenderer = TriangleShapeRenderer()
                    }
                    scatterDataSets.add(dataSet)
                }
            }

            // Primary Buy (dark red, larger) - Python style: darkred
            if (effectivePrimaryBuy.isNotEmpty()) {
                val entries = effectivePrimaryBuy.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "Primary Buy").apply {
                        color = PrimaryBuy.toArgb()  // darkred
                        setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
                        scatterShapeSize = 24f
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                        shapeRenderer = TriangleShapeRenderer()
                    }
                    scatterDataSets.add(dataSet)
                }
            }

            // Additional Sell (light blue, smaller) - Python style
            if (additionalSellSignals.isNotEmpty()) {
                val entries = additionalSellSignals.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "Add. Sell").apply {
                        color = AdditionalSell.toArgb()
                        scatterShapeSize = 16f
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                        shapeRenderer = InvertedTriangleShapeRenderer()
                    }
                    scatterDataSets.add(dataSet)
                }
            }

            // Primary Sell (dark blue, larger) - Python style: darkblue
            if (effectivePrimarySell.isNotEmpty()) {
                val entries = effectivePrimarySell.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "Primary Sell").apply {
                        color = PrimarySell.toArgb()  // darkblue
                        scatterShapeSize = 24f
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                        shapeRenderer = InvertedTriangleShapeRenderer()
                    }
                    scatterDataSets.add(dataSet)
                }
            }

            if (scatterDataSets.isNotEmpty()) {
                combinedData.setData(ScatterData(scatterDataSets as List<IScatterDataSet>))
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
 * Python reference style: Close line with EMA13, color dots on price
 * Style: {ticker}.KS Elder Impulse System (Weekly, last 1 year)
 *
 * @param dates List of date strings
 * @param priceValues Close price values
 * @param ema13Values EMA13 values
 * @param impulseStates Impulse states: 1=bullish(green), 0=neutral(gray), -1=bearish(red)
 */
@Composable
fun ElderImpulseChart(
    dates: List<String>,
    priceValues: List<Double>,
    ema13Values: List<Double>,
    impulseStates: List<Int>,
    // Legacy parameter for backward compatibility
    mcapValues: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    // Use priceValues or fall back to mcapValues for backward compatibility
    val effectivePriceValues = if (priceValues.isNotEmpty()) priceValues else mcapValues

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

                // X Axis - Python style
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

                // Right Y Axis (disabled - EMA13 shares same axis)
                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }

                // Marker
                marker = ElderImpulseMarkerView(ctx, dates, effectivePriceValues, impulseStates)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()
            val lineDataSets = mutableListOf<LineDataSet>()

            // Close price line (left axis) - Python style: tab:blue, solid
            val priceEntries = effectivePriceValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val priceDataSet = LineDataSet(priceEntries, "Close").apply {
                color = TabBlue.toArgb()  // tab:blue
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                axisDependency = YAxis.AxisDependency.LEFT
            }
            lineDataSets.add(priceDataSet)

            // EMA13 line (left axis) - Python style: tab:orange, dashed
            if (ema13Values.isNotEmpty()) {
                val ema13Entries = ema13Values.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                val ema13DataSet = LineDataSet(ema13Entries, "EMA13 (Weekly)").apply {
                    color = TabOrange.toArgb()  // tab:orange
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                    axisDependency = YAxis.AxisDependency.LEFT
                    enableDashedLine(10f, 5f, 0f)  // Dashed line
                }
                lineDataSets.add(ema13DataSet)
            }

            combinedData.setData(LineData(lineDataSets))

            // Impulse color markers ON price line - Python style
            // Neutral (gray) - draw first (smaller, behind)
            val neutralEntries = mutableListOf<Entry>()
            val bullishEntries = mutableListOf<Entry>()
            val bearishEntries = mutableListOf<Entry>()

            impulseStates.forEachIndexed { index, state ->
                if (index < effectivePriceValues.size) {
                    val entry = Entry(index.toFloat(), effectivePriceValues[index].toFloat())
                    when (state) {
                        1 -> bullishEntries.add(entry)
                        -1 -> bearishEntries.add(entry)
                        else -> neutralEntries.add(entry)
                    }
                }
            }

            val scatterDataSets = mutableListOf<ScatterDataSet>()

            // Neutral (gray) - Python style: gray, smaller
            if (neutralEntries.isNotEmpty()) {
                val neutralDataSet = ScatterDataSet(neutralEntries, "Neutral").apply {
                    color = TabGray.toArgb()  // gray
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 10f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                }
                scatterDataSets.add(neutralDataSet)
            }

            // Bullish (green) - Python style
            if (bullishEntries.isNotEmpty()) {
                val bullishDataSet = ScatterDataSet(bullishEntries, "Bullish Impulse").apply {
                    color = ElderGreen.toArgb()  // green
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 12f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                }
                scatterDataSets.add(bullishDataSet)
            }

            // Bearish (red) - Python style
            if (bearishEntries.isNotEmpty()) {
                val bearishDataSet = ScatterDataSet(bearishEntries, "Bearish Impulse").apply {
                    color = ElderRed.toArgb()  // red
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 12f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                }
                scatterDataSets.add(bearishDataSet)
            }

            if (scatterDataSets.isNotEmpty()) {
                combinedData.setData(ScatterData(scatterDataSets as List<IScatterDataSet>))
            }

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
 * Python reference style: Close line with TD Sell/Buy lines
 * Style: {ticker}.KS Daily/Weekly/Monthly DeMark TD Setup Counts (last 1 year)
 *
 * @param dates List of date strings
 * @param sellSetupValues Sell setup counts
 * @param buySetupValues Buy setup counts
 * @param priceValues Close price values (left axis)
 * @param chartType Chart type for title: "Daily", "Weekly", "Monthly"
 */
@Composable
fun DemarkTDChart(
    dates: List<String>,
    sellSetupValues: List<Int>,
    buySetupValues: List<Int>,
    priceValues: List<Double> = emptyList(),
    chartType: String = "Daily",
    // Legacy parameter for backward compatibility
    mcapValues: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    // Use priceValues or fall back to mcapValues for backward compatibility
    val effectivePriceValues = if (priceValues.isNotEmpty()) priceValues else mcapValues

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
                    CombinedChart.DrawOrder.LINE
                ))

                // X Axis - Python style
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

                // Left Y Axis (Close Price) - Python style
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                }

                // Right Y Axis (TD Setup Count) - Python style: gray
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = TabGray.toArgb()  // gray like Python
                    // Dynamic Y-axis range based on max TD count
                    val maxTD = maxOf(
                        sellSetupValues.maxOrNull() ?: 0,
                        buySetupValues.maxOrNull() ?: 0
                    )
                    axisMinimum = 0f
                    axisMaximum = (maxTD + 2).toFloat()
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
            val lineDataSets = mutableListOf<LineDataSet>()

            // Close price line (left axis) - Python style: black
            if (effectivePriceValues.isNotEmpty()) {
                val priceEntries = effectivePriceValues.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                val priceDataSet = LineDataSet(priceEntries, "Close").apply {
                    color = ChartDefaultBlack.toArgb()  // black
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                    axisDependency = YAxis.AxisDependency.LEFT
                }
                lineDataSets.add(priceDataSet)
            }

            // TD Sell Setup line (right axis) - Python style: red
            val sellEntries = sellSetupValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val sellDataSet = LineDataSet(sellEntries, "TD Sell Setup").apply {
                color = DemarkRed.toArgb()  // red
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                axisDependency = YAxis.AxisDependency.RIGHT
            }
            lineDataSets.add(sellDataSet)

            // TD Buy Setup line (right axis) - Python style: blue
            val buyEntries = buySetupValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val buyDataSet = LineDataSet(buyEntries, "TD Buy Setup").apply {
                color = DemarkBlue.toArgb()  // blue
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                axisDependency = YAxis.AxisDependency.RIGHT
            }
            lineDataSets.add(buyDataSet)

            combinedData.setData(LineData(lineDataSets))

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
 * Python reference style - for use when only histogram values are needed
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

                // X Axis - Python style
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
            // Python style: teal (#26A69A) for positive, red (#EF5350) for negative
            val barEntries = histogramValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val barDataSet = BarDataSet(barEntries, "MACD Histogram").apply {
                colors = histogramValues.map { value ->
                    if (value >= 0) {
                        com.stockapp.core.ui.theme.HistogramTeal.toArgb()
                    } else {
                        com.stockapp.core.ui.theme.HistogramRed.toArgb()
                    }
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
