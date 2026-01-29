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
import com.github.mikephil.charting.charts.ScatterChart
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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.stockapp.core.ui.theme.AdditionalBuy
import com.stockapp.core.ui.theme.AdditionalSell
import com.stockapp.core.ui.theme.ChartDefaultBlack
import com.stockapp.core.ui.theme.ChartGridDark
import com.stockapp.core.ui.theme.ChartGridLight
import com.stockapp.core.ui.theme.DemarkBlue
import com.stockapp.core.ui.theme.DemarkRed
import com.stockapp.core.ui.theme.ElderGreen
import com.stockapp.core.ui.theme.ElderRed
import com.stockapp.core.ui.theme.FearGreedGreen
import com.stockapp.core.ui.theme.FearGreedRed
import com.stockapp.core.ui.theme.HistogramRed
import com.stockapp.core.ui.theme.HistogramTeal
import com.stockapp.core.ui.theme.MacdBlue
import com.stockapp.core.ui.theme.MacdSignalOrange
import com.stockapp.core.ui.theme.PrimaryBuy
import com.stockapp.core.ui.theme.PrimarySell
import com.stockapp.core.ui.theme.TabBlue
import com.stockapp.core.ui.theme.TabGray
import com.stockapp.core.ui.theme.TabOrange
import com.stockapp.core.ui.theme.TrendSignalFearGreedColor
import com.stockapp.core.ui.theme.TrendSignalMaColor
import com.stockapp.core.ui.theme.TrendSignalPriceColor

/**
 * Technical indicator charts for IndicatorScreen.
 * Contains: MacdChart, TrendSignalChart, ElderImpulseChart, DemarkTDChart
 */

/**
 * Helper function to setup X-axis with date formatting.
 */
private fun XAxis.setupDateFormattedAxis(dates: List<String>, gridColor: Int, textColor: Int) {
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
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    // All axis labels in black for dark theme support
    val textColor = Color.BLACK

    // Python style colors: MACD blue (#2196F3), Signal orange (#FF9800)
    val lineColor1 = MacdBlue.toArgb()  // MACD line - blue
    val lineColor2 = MacdSignalOrange.toArgb()  // Signal line (dashed) - orange

    // Memoize chart data (P2 fix)
    val barEntries = remember(histogramValues) {
        histogramValues.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value.toFloat())
        }
    }
    val histogramColors = remember(histogramValues) {
        createColoredHistogram(histogramValues)
    }
    val macdEntries = remember(macdValues) {
        macdValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }
    }
    val signalEntries = remember(signalValues) {
        signalValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }
    }

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()
                setDrawOrder(arrayOf(
                    CombinedChart.DrawOrder.BAR,
                    CombinedChart.DrawOrder.LINE
                ))

                // X Axis
                xAxis.setupDateFormattedAxis(dates, gridColor, textColor)

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

            // Histogram bars - Python style: teal (#26A69A) for positive, red (#EF5350) for negative
            val barDataSet = BarDataSet(barEntries, "Histogram").apply {
                colors = histogramColors
                setDrawValues(false)
            }
            combinedData.setData(BarData(barDataSet).apply { barWidth = 0.7f })

            // MACD and Signal lines
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
 * Python reference style with dual Y-axis (left: price, right: 탐욕/중립/공포 text labels)
 * Style: {ticker}.KS Weekly Strategy + Fear & Greed
 *
 * @param dates List of date strings
 * @param priceValues Close price values (종가)
 * @param ma10Values MA10 values (optional, for overlay)
 * @param fearGreedValues Fear/Greed index values (-1 to 1.5)
 * @param primaryBuySignals List of indices with primary buy signals (매수)
 * @param additionalBuySignals List of indices with additional buy signals (보조매수)
 * @param primarySellSignals List of indices with primary sell signals (매도)
 * @param additionalSellSignals List of indices with additional sell signals (보조매도)
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
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    // All labels in black as per requirement
    val textColor = Color.BLACK

    // Merge legacy signals with new ones
    val effectivePrimaryBuy = if (primaryBuySignals.isEmpty()) buySignals else primaryBuySignals
    val effectivePrimarySell = if (primarySellSignals.isEmpty()) sellSignals else primarySellSignals

    // Memoize chart data (P2 fix)
    val priceEntries = remember(priceValues) {
        priceValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }
    }
    val fearGreedEntries = remember(fearGreedValues) {
        fearGreedValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }
    }
    val ma10Entries = remember(ma10Values) {
        ma10Values.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }
    }
    val thresholdEntries = remember(dates) {
        Triple(
            dates.indices.map { Entry(it.toFloat(), 0.5f) },
            dates.indices.map { Entry(it.toFloat(), -0.5f) },
            dates.size
        )
    }

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()
                setExtraRightOffset(30f)  // Extra space for right axis labels
                setDrawOrder(arrayOf(
                    CombinedChart.DrawOrder.LINE,
                    CombinedChart.DrawOrder.SCATTER
                ))

                // X Axis - Python reference style with YYYY-MM format for long periods
                xAxis.setupDateFormattedAxis(dates, gridColor, textColor)

                // Left Y Axis (Price/Market Cap) - Black labels
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                }

                // Right Y Axis (Fear/Greed with text labels: 탐욕, 중립, 공포)
                // Per TREND_SIGNAL_CHART.md spec: range -1.2 ~ +1.2
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = textColor  // Black labels
                    axisMinimum = -1.2f
                    axisMaximum = 1.2f
                    setLabelCount(5, true)  // Show 5 labels at fixed positions
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return when {
                                value > 0.6f -> "탐욕"
                                value > 0.2f -> "+"
                                value >= -0.2f -> "중립"
                                value >= -0.6f -> "-"
                                else -> "공포"
                            }
                        }
                    }
                }

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor  // Black legend text
                }

                // Marker
                marker = TrendSignalMarkerView(ctx, dates, priceValues, fearGreedValues)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()
            val lineDataSets = mutableListOf<LineDataSet>()

            // 종가 (Close price) line (left axis)
            // Per TREND_SIGNAL_CHART.md spec: Cubic Bezier, 2.5px, MACD lineColor1 (blue)
            val priceDataSet = LineDataSet(priceEntries, "종가").apply {
                color = TrendSignalPriceColor.toArgb()  // MACD lineColor1 (#2196F3)
                lineWidth = 2.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.LEFT
            }
            lineDataSets.add(priceDataSet)

            // MA line (left axis) - Dashed
            // Per TREND_SIGNAL_CHART.md spec: Dashed, 2.0px, MACD lineColor2 (orange #FF9800)
            if (ma10Entries.isNotEmpty()) {
                val ma10DataSet = LineDataSet(ma10Entries, "MA").apply {
                    color = TrendSignalMaColor.toArgb()  // MACD lineColor2 (#FF9800)
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                    axisDependency = YAxis.AxisDependency.LEFT
                    enableDashedLine(10f, 5f, 0f)  // Dashed line: 10px line, 5px gap
                }
                lineDataSets.add(ma10DataSet)
            }

            // F&G (Fear/Greed) line (right axis) - Purple
            // Per TREND_SIGNAL_CHART.md spec: Cubic Bezier, 1.5px, Purple RGB(156, 39, 176)
            val fearGreedDataSet = LineDataSet(fearGreedEntries, "F&G").apply {
                color = TrendSignalFearGreedColor.toArgb()  // Purple #9C27B0
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.RIGHT
            }
            lineDataSets.add(fearGreedDataSet)

            // Threshold lines for Fear/Greed (±0.5) - Dashed reference lines
            // +0.5 threshold (Greed zone - red dashed)
            val threshold05DataSet = LineDataSet(thresholdEntries.first, "탐욕(0.5)").apply {
                color = FearGreedRed.toArgb()
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 10f, 0f)
                axisDependency = YAxis.AxisDependency.RIGHT
                isHighlightEnabled = false
            }
            lineDataSets.add(threshold05DataSet)

            // -0.5 threshold (Fear zone - green dashed)
            val thresholdNeg05DataSet = LineDataSet(thresholdEntries.second, "공포(-0.5)").apply {
                color = FearGreedGreen.toArgb()
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 10f, 0f)
                axisDependency = YAxis.AxisDependency.RIGHT
                isHighlightEnabled = false
            }
            lineDataSets.add(thresholdNeg05DataSet)

            combinedData.setData(LineData(lineDataSets as List<ILineDataSet>))

            // Signal markers on close price line - Python reference style
            val scatterDataSets = mutableListOf<ScatterDataSet>()

            // 보조매수 (Additional Buy) - Light red, smaller triangle
            if (additionalBuySignals.isNotEmpty()) {
                val entries = additionalBuySignals.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "보조매수").apply {
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

            // 매수 (Primary Buy) - Dark red, larger triangle
            if (effectivePrimaryBuy.isNotEmpty()) {
                val entries = effectivePrimaryBuy.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "매수").apply {
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

            // 보조매도 (Additional Sell) - Light blue, smaller inverted triangle
            if (additionalSellSignals.isNotEmpty()) {
                val entries = additionalSellSignals.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "보조매도").apply {
                        color = AdditionalSell.toArgb()
                        scatterShapeSize = 16f
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                        shapeRenderer = InvertedTriangleShapeRenderer()
                    }
                    scatterDataSets.add(dataSet)
                }
            }

            // 매도 (Primary Sell) - Dark blue, larger inverted triangle
            if (effectivePrimarySell.isNotEmpty()) {
                val entries = effectivePrimarySell.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "매도").apply {
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
    // All axis labels in black for dark theme support
    val textColor = Color.BLACK

    // Use priceValues or fall back to mcapValues for backward compatibility
    val effectivePriceValues = if (priceValues.isNotEmpty()) priceValues else mcapValues

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()
                setDrawOrder(arrayOf(
                    CombinedChart.DrawOrder.LINE,
                    CombinedChart.DrawOrder.SCATTER
                ))

                // X Axis - Python style
                xAxis.setupDateFormattedAxis(dates, gridColor, textColor)

                // Left Y Axis (Price) - black text for dark theme support
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

            combinedData.setData(LineData(lineDataSets as List<ILineDataSet>))

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
    // All axis labels in black for dark theme support
    val textColor = Color.BLACK

    // Use priceValues or fall back to mcapValues for backward compatibility
    val effectivePriceValues = if (priceValues.isNotEmpty()) priceValues else mcapValues

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()
                setDrawOrder(arrayOf(
                    CombinedChart.DrawOrder.LINE
                ))

                // X Axis - Python style with date range based formatting
                xAxis.setupDateFormattedAxis(dates, gridColor, textColor)

                // Left Y Axis (Close Price) - black text for dark theme support
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                }

                // Right Y Axis (TD Setup Count) - black text for dark theme support
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = textColor
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

            combinedData.setData(LineData(lineDataSets as List<ILineDataSet>))

            chart.data = combinedData
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.DEMARK_TD)
    )
}
