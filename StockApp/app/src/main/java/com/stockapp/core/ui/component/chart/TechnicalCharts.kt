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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
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
import com.stockapp.core.ui.theme.OscillatorBlue
import com.stockapp.core.ui.theme.OscillatorOrange
import com.stockapp.core.ui.theme.TabBlue
import com.stockapp.core.ui.theme.TabGray
import com.stockapp.core.ui.theme.TabOrange
import com.github.mikephil.charting.charts.ScatterChart

/**
 * MacdChart - MACD chart with histogram and signal line
 * EtfMonitor reference style implementation using MPAndroidChart
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

    // EtfMonitor style colors
    val macdColor = Color.BLACK  // MACD line - black (default from colorSettings)
    val signalColor = com.stockapp.core.ui.theme.MacdSignalOrange.toArgb()  // Signal line - orange
    val positiveColor = ChartGreen.toArgb()  // Histogram positive - green
    val negativeColor = ChartRed.toArgb()    // Histogram negative - red

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

                // X Axis - EtfMonitor style
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridLineWidth = 1f
                    setGridColor(gridColor)
                    this.textColor = textColor
                    enableGridDashedLine(10f, 5f, 0f)
                    granularity = 1f
                    labelRotationAngle = -45f
                    setLabelCount(ChartLabelCalculator.calculateOptimalLabelCount(dates.size), false)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index >= 0 && index < dates.size) {
                                dates[index]
                            } else ""
                        }
                    }
                }

                // Left Y Axis (MACD values)
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridLineWidth = 1f
                    setGridColor(gridColor)
                    this.textColor = textColor
                    enableGridDashedLine(10f, 5f, 0f)
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                }

                // Right Y Axis (disabled)
                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = true
                    textSize = 12f
                    this.textColor = textColor
                }

                // Marker
                marker = MacdMarkerView(ctx, dates, macdValues, signalValues)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()

            // Histogram bars - EtfMonitor style: green for positive, red for negative
            val barEntries = histogramValues.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value.toFloat())
            }
            val barDataSet = BarDataSet(barEntries, "").apply {
                colors = histogramValues.map { value ->
                    if (value >= 0) positiveColor else negativeColor
                }
                setDrawValues(false)
                isHighlightEnabled = false
            }
            combinedData.setData(BarData(barDataSet).apply { barWidth = 0.8f })

            // MACD line - EtfMonitor style with circles
            val macdEntries = macdValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val macdDataSet = LineDataSet(macdEntries, "MACD").apply {
                color = macdColor
                lineWidth = 2f
                setCircleColor(macdColor)
                circleRadius = 2f
                setDrawCircleHole(false)
                setDrawValues(false)
                highLightColor = macdColor
            }

            // Signal line - EtfMonitor style with circles and dashed
            val signalEntries = signalValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val signalDataSet = LineDataSet(signalEntries, "Signal").apply {
                color = signalColor
                lineWidth = 2f
                setCircleColor(signalColor)
                circleRadius = 2f
                setDrawCircleHole(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
                highLightColor = signalColor
            }

            combinedData.setData(LineData(macdDataSet, signalDataSet))

            chart.data = combinedData
            chart.legend.isEnabled = true
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.MACD)
    )
}

/**
 * TrendSignalChart - Trend Signal chart with price, MA, and Fear/Greed
 * EtfMonitor reference style with dual Y-axis and signal markers
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

    // EtfMonitor style colors - Korean market convention (Red=Buy, Blue=Sell)
    val priceColor = Color.BLACK  // 종가 - black
    val maColor = Color.GRAY  // MA - gray dashed
    val buyColor = Color.rgb(244, 67, 54)        // 매수 (빨간색)
    val auxBuyColor = Color.rgb(255, 138, 128)   // 보조매수 (연한 빨간색)
    val sellColor = Color.rgb(33, 150, 243)      // 매도 (파란색)
    val auxSellColor = Color.rgb(130, 177, 255)  // 보조매도 (연한 파란색)
    val fearGreedColor = Color.rgb(156, 39, 176) // Fear & Greed 보라색

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

                // X Axis - EtfMonitor style
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridLineWidth = 1f
                    setGridColor(gridColor)
                    this.textColor = textColor
                    enableGridDashedLine(10f, 5f, 0f)
                    granularity = 1f
                    labelRotationAngle = -45f
                    setLabelCount(ChartLabelCalculator.calculateOptimalLabelCount(dates.size), false)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index >= 0 && index < dates.size) {
                                dates[index]
                            } else ""
                        }
                    }
                }

                // Left Y Axis (Price) - EtfMonitor style with formatting
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridLineWidth = 1f
                    setGridColor(gridColor)
                    this.textColor = textColor
                    enableGridDashedLine(10f, 5f, 0f)
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return when {
                                value >= 1_000_000 -> String.format("%.1f만", value / 10_000f)
                                value >= 10_000 -> String.format("%.0f", value)
                                else -> String.format("%.0f", value)
                            }
                        }
                    }
                }

                // Right Y Axis (Fear/Greed: -1.2 to 1.2) - EtfMonitor style with Korean labels
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = textColor
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                    axisMinimum = -1.2f
                    axisMaximum = 1.2f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return when {
                                value > 0.6f -> "탐욕"
                                value > 0.2f -> "+"
                                value > -0.2f -> "중립"
                                value > -0.6f -> "-"
                                else -> "공포"
                            }
                        }
                    }
                }

                legend.apply {
                    isEnabled = true
                    textSize = 10f
                    this.textColor = textColor
                }

                // Marker
                marker = TrendSignalMarkerView(ctx, dates, priceValues, fearGreedValues)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()
            val lineDataSets = mutableListOf<LineDataSet>()

            // 1. Close price line (left axis) - EtfMonitor style: black, CUBIC_BEZIER
            val priceEntries = priceValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val priceDataSet = LineDataSet(priceEntries, "종가").apply {
                color = priceColor
                lineWidth = 2.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.LEFT
                highLightColor = priceColor
            }
            lineDataSets.add(priceDataSet)

            // 2. MA line (left axis) - EtfMonitor style: gray, dashed
            if (ma10Values.isNotEmpty()) {
                val ma10Entries = ma10Values.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                val ma10DataSet = LineDataSet(ma10Entries, "MA").apply {
                    color = maColor
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                    axisDependency = YAxis.AxisDependency.LEFT
                    enableDashedLine(10f, 5f, 0f)
                    highLightColor = maColor
                }
                lineDataSets.add(ma10DataSet)
            }

            // 3. Fear/Greed line (right axis) - EtfMonitor style: purple, CUBIC_BEZIER
            val fearGreedEntries = fearGreedValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val fearGreedDataSet = LineDataSet(fearGreedEntries, "F&G").apply {
                color = fearGreedColor
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = YAxis.AxisDependency.RIGHT
                highLightColor = fearGreedColor
            }
            lineDataSets.add(fearGreedDataSet)

            combinedData.setData(LineData(lineDataSets as List<ILineDataSet>))

            // 4. Signal markers - EtfMonitor style (Korean convention)
            val scatterDataSets = mutableListOf<ScatterDataSet>()

            // Primary Buy (빨간색, 큰 삼각형) - Korean market convention
            if (effectivePrimaryBuy.isNotEmpty()) {
                val entries = effectivePrimaryBuy.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "매수").apply {
                        color = buyColor
                        setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
                        scatterShapeSize = 24f
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                    }
                    scatterDataSets.add(dataSet)
                }
            }

            // Additional Buy (연한 빨간색, 작은 삼각형)
            if (additionalBuySignals.isNotEmpty()) {
                val entries = additionalBuySignals.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "보조매수").apply {
                        color = auxBuyColor
                        setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
                        scatterShapeSize = 18f
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                    }
                    scatterDataSets.add(dataSet)
                }
            }

            // Primary Sell (파란색, 큰 역삼각형)
            if (effectivePrimarySell.isNotEmpty()) {
                val entries = effectivePrimarySell.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "매도").apply {
                        color = sellColor
                        scatterShapeSize = 24f
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                        shapeRenderer = InvertedTriangleShapeRenderer()
                    }
                    scatterDataSets.add(dataSet)
                }
            }

            // Additional Sell (연한 파란색, 작은 역삼각형)
            if (additionalSellSignals.isNotEmpty()) {
                val entries = additionalSellSignals.mapNotNull { index ->
                    if (index >= 0 && index < priceValues.size) {
                        Entry(index.toFloat(), priceValues[index].toFloat())
                    } else null
                }
                if (entries.isNotEmpty()) {
                    val dataSet = ScatterDataSet(entries, "보조매도").apply {
                        color = auxSellColor
                        scatterShapeSize = 18f
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
 * EtfMonitor reference style: Market Cap (left) with Close/EMA13 (right), impulse circles on close
 * Style: {ticker}.KS Elder Impulse System (Weekly, last 1 year)
 *
 * @param dates List of date strings
 * @param priceValues Close price values
 * @param ema13Values EMA13 values
 * @param impulseStates Impulse states: 1=bullish(green), 0=neutral(gray), -1=bearish(red)
 * @param mcapValues Market cap values for left Y-axis
 */
@Composable
fun ElderImpulseChart(
    dates: List<String>,
    priceValues: List<Double>,
    ema13Values: List<Double>,
    impulseStates: List<Int>,
    // Market cap for left Y-axis (EtfMonitor style)
    mcapValues: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    // EtfMonitor style colors
    val marketCapColor = Color.BLACK  // 시가총액 - black
    val emaColor = Color.GRAY  // EMA13 - gray dashed
    val bullColor = ChartGreen.toArgb()   // Bullish - green
    val bearColor = ChartRed.toArgb()     // Bearish - red
    val neutralColor = if (isDark) Color.LTGRAY else Color.DKGRAY  // Neutral - gray

    // Use mcapValues for left axis if available
    val hasMarketCap = mcapValues.isNotEmpty() && mcapValues.any { it > 0 }

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

                // X Axis - EtfMonitor style
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridLineWidth = 1f
                    setGridColor(gridColor)
                    this.textColor = textColor
                    granularity = 1f
                    labelRotationAngle = -45f
                    setLabelCount(ChartLabelCalculator.calculateOptimalLabelCount(dates.size), false)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val idx = value.toInt()
                            return if (idx in dates.indices) {
                                val date = dates[idx]
                                if (date.length >= 7) date.substring(5) else date
                            } else ""
                        }
                    }
                }

                // Left Y Axis - Market Cap (EtfMonitor style) with 조/억 formatting
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridLineWidth = 0.5f
                    setGridColor(gridColor)
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return if (value >= 1_000_000_000_000) {
                                String.format("%.1f조", value / 1_000_000_000_000)
                            } else {
                                String.format("%.0f억", value / 100_000_000)
                            }
                        }
                    }
                }

                // Right Y Axis - Close/EMA13 (EtfMonitor style)
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = textColor
                }

                legend.apply {
                    isEnabled = true
                    textSize = 10f
                    this.textColor = textColor
                }

                // Marker
                marker = ElderImpulseMarkerView(ctx, dates, priceValues, impulseStates)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()
            val lineDataSets = mutableListOf<LineDataSet>()

            // 1. Market Cap line (left axis) - EtfMonitor style: black
            if (hasMarketCap) {
                val mcapEntries = mcapValues.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                val mcapDataSet = LineDataSet(mcapEntries, "시가총액").apply {
                    color = marketCapColor
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                }
                lineDataSets.add(mcapDataSet)
            }

            // 2. EMA13 line (right axis) - EtfMonitor style: gray dashed
            if (ema13Values.isNotEmpty()) {
                val ema13Entries = ema13Values.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                val ema13DataSet = LineDataSet(ema13Entries, "EMA13").apply {
                    color = emaColor
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.RIGHT
                    enableDashedLine(10f, 5f, 0f)
                }
                lineDataSets.add(ema13DataSet)
            }

            combinedData.setData(LineData(lineDataSets as List<ILineDataSet>))

            // 3. Impulse state circles on Close price (right axis) - EtfMonitor style
            val neutralEntries = mutableListOf<Entry>()
            val bullishEntries = mutableListOf<Entry>()
            val bearishEntries = mutableListOf<Entry>()

            impulseStates.forEachIndexed { index, state ->
                if (index < priceValues.size) {
                    val entry = Entry(index.toFloat(), priceValues[index].toFloat())
                    when (state) {
                        1 -> bullishEntries.add(entry)
                        -1 -> bearishEntries.add(entry)
                        else -> neutralEntries.add(entry)
                    }
                }
            }

            val scatterDataSets = mutableListOf<ScatterDataSet>()

            // Bullish (green) - EtfMonitor style
            if (bullishEntries.isNotEmpty()) {
                val bullishDataSet = ScatterDataSet(bullishEntries, "Bullish").apply {
                    color = bullColor
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 12f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.RIGHT
                }
                scatterDataSets.add(bullishDataSet)
            }

            // Bearish (red) - EtfMonitor style
            if (bearishEntries.isNotEmpty()) {
                val bearishDataSet = ScatterDataSet(bearishEntries, "Bearish").apply {
                    color = bearColor
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 12f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.RIGHT
                }
                scatterDataSets.add(bearishDataSet)
            }

            // Neutral (gray) - EtfMonitor style
            if (neutralEntries.isNotEmpty()) {
                val neutralDataSet = ScatterDataSet(neutralEntries, "Neutral").apply {
                    color = neutralColor
                    setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                    scatterShapeSize = 12f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.RIGHT
                }
                scatterDataSets.add(neutralDataSet)
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
 * EtfMonitor reference style: Market Cap (left) with TD Setup counts (right), filled areas
 * Style: {ticker}.KS Daily/Weekly/Monthly DeMark TD Setup Counts (last 1 year)
 *
 * @param dates List of date strings
 * @param sellSetupValues Sell setup counts (매도 피로 - 상승 지속 카운트)
 * @param buySetupValues Buy setup counts (매수 피로 - 하락 지속 카운트)
 * @param mcapValues Market cap values for left Y-axis
 * @param chartType Chart type for title: "Daily", "Weekly", "Monthly"
 */
@Composable
fun DemarkTDChart(
    dates: List<String>,
    sellSetupValues: List<Int>,
    buySetupValues: List<Int>,
    // Market cap for left Y-axis (EtfMonitor style)
    mcapValues: List<Double> = emptyList(),
    priceValues: List<Double> = emptyList(),
    chartType: String = "Daily",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    // EtfMonitor style colors
    val marketCapColor = Color.BLACK  // 시가총액 - black
    val sellFatigueColor = ChartRed.toArgb()   // 매도피로 (상승 피로) - red
    val buyFatigueColor = ChartGreen.toArgb()  // 매수피로 (하락 피로) - green

    // Use mcapValues for left axis if available, fallback to priceValues
    val effectiveMcapValues = if (mcapValues.isNotEmpty() && mcapValues.any { it > 0 }) mcapValues else priceValues
    val hasMarketCap = effectiveMcapValues.isNotEmpty() && effectiveMcapValues.any { it > 0 }

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

                // X Axis - EtfMonitor style
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridLineWidth = 1f
                    setGridColor(gridColor)
                    this.textColor = textColor
                    granularity = 1f
                    labelRotationAngle = -45f
                    setLabelCount(ChartLabelCalculator.calculateOptimalLabelCount(dates.size), false)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val idx = value.toInt()
                            return if (idx in dates.indices) {
                                val date = dates[idx]
                                if (date.length >= 7) date.substring(5) else date
                            } else ""
                        }
                    }
                }

                // Left Y Axis - Market Cap (EtfMonitor style) with 조/억 formatting
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridLineWidth = 0.5f
                    setGridColor(gridColor)
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return if (value >= 1_000_000_000_000) {
                                String.format("%.1f조", value / 1_000_000_000_000)
                            } else {
                                String.format("%.0f억", value / 100_000_000)
                            }
                        }
                    }
                }

                // Right Y Axis - TD Setup Count (EtfMonitor style: -15 to +15)
                axisRight.apply {
                    isEnabled = true
                    setDrawGridLines(false)
                    this.textColor = textColor
                    axisMinimum = -15f
                    axisMaximum = 15f
                }

                legend.apply {
                    isEnabled = true
                    textSize = 10f
                    this.textColor = textColor
                }

                // Marker
                marker = DemarkTDMarkerView(ctx, dates, sellSetupValues, buySetupValues)
            }
        },
        update = { chart ->
            val combinedData = CombinedData()
            val lineDataSets = mutableListOf<LineDataSet>()

            // 1. Market Cap line (left axis) - EtfMonitor style: black
            if (hasMarketCap) {
                val mcapEntries = effectiveMcapValues.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }
                val mcapDataSet = LineDataSet(mcapEntries, "시가총액").apply {
                    color = marketCapColor
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                }
                lineDataSets.add(mcapDataSet)
            }

            // 2. TD Sell Setup line (right axis) - EtfMonitor style: red with filled area (positive values)
            val sellEntries = sellSetupValues.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val sellDataSet = LineDataSet(sellEntries, "매도피로").apply {
                color = sellFatigueColor
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                // EtfMonitor style: filled area
                setDrawFilled(true)
                fillColor = sellFatigueColor
                fillAlpha = 50
            }
            lineDataSets.add(sellDataSet)

            // 3. TD Buy Setup line (right axis) - EtfMonitor style: green with filled area (negative values)
            val buyEntries = buySetupValues.mapIndexed { index, value ->
                Entry(index.toFloat(), -value.toFloat())  // EtfMonitor: display as negative
            }
            val buyDataSet = LineDataSet(buyEntries, "매수피로").apply {
                color = buyFatigueColor
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                // EtfMonitor style: filled area
                setDrawFilled(true)
                fillColor = buyFatigueColor
                fillAlpha = 50
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

                // Left Y Axis
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
            val entries = values.mapIndexed { index, value ->
                Entry(index.toFloat(), value.toFloat())
            }
            val dataSet = LineDataSet(entries, label).apply {
                color = lineColor.toArgb()
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
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                setExtraBottomOffset(10f)
                setDrawOrder(arrayOf(CombinedChart.DrawOrder.LINE))

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
                axisDependency = YAxis.AxisDependency.LEFT
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
