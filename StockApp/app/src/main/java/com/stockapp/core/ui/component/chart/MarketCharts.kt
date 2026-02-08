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
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.stockapp.core.ui.theme.ChartGridDark
import com.stockapp.core.ui.theme.ChartGridLight
import com.stockapp.core.ui.theme.OscillatorBlue
import com.stockapp.core.ui.theme.OscillatorOrange

/**
 * Market indicator charts.
 * Follows the same pattern as MarketCapOscillatorChart in AnalysisCharts.kt.
 */

// Color constants for market charts
private val FOREIGN_COLOR = android.graphics.Color.rgb(0x19, 0x76, 0xD2)   // Blue
private val INSTITUTION_COLOR = android.graphics.Color.rgb(0x38, 0x8E, 0x3C) // Green
private val INDIVIDUAL_COLOR = android.graphics.Color.rgb(0xD3, 0x2F, 0x2F)  // Red
private val BLOOD_COLOR = android.graphics.Color.rgb(0xD3, 0x2F, 0x2F)       // Red
private val SMA_COLOR = android.graphics.Color.rgb(0x19, 0x76, 0xD2)         // Blue

private fun XAxis.setupMarketAxis(dates: List<String>, gridColor: Int, textColor: Int) {
    position = XAxis.XAxisPosition.BOTTOM
    setDrawGridLines(true)
    this.gridColor = gridColor
    this.textColor = textColor
    enableGridDashedLine(10f, 10f, 0f)
    setLabelCount(ChartLabelCalculator.calculateOptimalLabelCount(dates.size), false)
    valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return if (index in dates.indices) {
                DateFormatter.formatForChartByDataCount(dates[index], dates.size)
            } else ""
        }
    }
}

/**
 * Dual-axis line chart for Fear & Greed history.
 * Left axis: Fear/Greed score (0-100), blue line with fill.
 * Right axis: KOSPI index value, orange line.
 */
@Composable
fun FearGreedLineChart(
    dates: List<String>,
    scores: List<Double>,
    indexValues: List<Double>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = Color.BLACK

    val scoreEntries = remember(scores) {
        scores.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
    }
    val indexEntries = remember(indexValues) {
        indexValues.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
    }
    val hasIndex = indexValues.isNotEmpty() && indexValues.size == scores.size

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()
                setDrawOrder(arrayOf(CombinedChart.DrawOrder.LINE))

                xAxis.setupMarketAxis(dates, gridColor, textColor)

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    axisMinimum = 0f
                    axisMaximum = 100f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) = "%.0f".format(value)
                    }
                }

                axisRight.apply {
                    isEnabled = hasIndex
                    setDrawGridLines(false)
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) = "%.0f".format(value)
                    }
                }

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                }
            }
        },
        update = { chart ->
            val scoreDataSet = LineDataSet(scoreEntries, "공포/탐욕").apply {
                color = OscillatorBlue.toArgb()
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                axisDependency = YAxis.AxisDependency.LEFT
                setDrawFilled(true)
                fillColor = OscillatorBlue.toArgb()
                fillAlpha = 40
            }

            val dataSets = mutableListOf<LineDataSet>(scoreDataSet)

            if (hasIndex) {
                val indexDataSet = LineDataSet(indexEntries, "KOSPI").apply {
                    color = OscillatorOrange.toArgb()
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                    axisDependency = YAxis.AxisDependency.RIGHT
                }
                dataSets.add(indexDataSet)
            }

            chart.data = CombinedData().apply {
                setData(LineData(dataSets.toList()))
            }
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.MARKET_INDICATOR)
    )
}

/**
 * Multi-line chart for fund flow (investor cumulative net buys).
 * 3 lines: Foreign (blue), Institution (green), Individual (red).
 */
@Composable
fun FundFlowLineChart(
    dates: List<String>,
    foreignCumulative: List<Double>,
    institutionCumulative: List<Double>,
    individualCumulative: List<Double>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = Color.BLACK

    val foreignEntries = remember(foreignCumulative) {
        foreignCumulative.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
    }
    val instEntries = remember(institutionCumulative) {
        institutionCumulative.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
    }
    val indivEntries = remember(individualCumulative) {
        individualCumulative.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
    }

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()
                setDrawOrder(arrayOf(CombinedChart.DrawOrder.LINE))

                xAxis.setupMarketAxis(dates, gridColor, textColor)

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val v = value.toDouble()
                            val abs = kotlin.math.abs(v)
                            val sign = if (v >= 0) "" else "-"
                            return when {
                                abs >= 1_000_000_000_000 -> "${sign}%.0f조".format(abs / 1_000_000_000_000)
                                abs >= 100_000_000 -> "${sign}%.0f억".format(abs / 100_000_000)
                                else -> "${sign}%.0f".format(v)
                            }
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
            val foreignDs = LineDataSet(foreignEntries, "외국인").apply {
                color = FOREIGN_COLOR
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }
            val instDs = LineDataSet(instEntries, "기관").apply {
                color = INSTITUTION_COLOR
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }
            val indivDs = LineDataSet(indivEntries, "개인").apply {
                color = INDIVIDUAL_COLOR
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }

            chart.data = CombinedData().apply {
                setData(LineData(foreignDs, instDs, indivDs))
            }
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.MARKET_INDICATOR)
    )
}

/**
 * Dual-line chart for Blood Indicator.
 * BLOOD: solid red line. SMA100: dashed blue line.
 * Both on left Y-axis with 4-decimal formatting.
 */
@Composable
fun BloodLineChart(
    dates: List<String>,
    bloodValues: List<Double>,
    sma100Values: List<Double>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = Color.BLACK

    val bloodEntries = remember(bloodValues) {
        bloodValues.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
    }
    val smaEntries = remember(sma100Values) {
        sma100Values.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
    }
    val hasSma = sma100Values.isNotEmpty() && sma100Values.size == bloodValues.size

    AndroidView(
        factory = { ctx ->
            CombinedChart(ctx).apply {
                setupCommonChartProperties()
                setDrawOrder(arrayOf(CombinedChart.DrawOrder.LINE))

                xAxis.setupMarketAxis(dates, gridColor, textColor)

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) = "%.4f".format(value)
                    }
                }

                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = true
                    this.textColor = textColor
                    setCustom(
                        buildList {
                            add(LegendEntry().apply {
                                label = "BLOOD"
                                formColor = BLOOD_COLOR
                                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                            })
                            if (hasSma) {
                                add(LegendEntry().apply {
                                    label = "SMA100"
                                    formColor = SMA_COLOR
                                    form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                                })
                            }
                        }
                    )
                }
            }
        },
        update = { chart ->
            val bloodDs = LineDataSet(bloodEntries, "BLOOD").apply {
                color = BLOOD_COLOR
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }

            val dataSets = mutableListOf<LineDataSet>(bloodDs)

            if (hasSma) {
                val smaDs = LineDataSet(smaEntries, "SMA100").apply {
                    color = SMA_COLOR
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                    enableDashedLine(10f, 5f, 0f)
                }
                dataSets.add(smaDs)
            }

            chart.data = CombinedData().apply {
                setData(LineData(dataSets.toList()))
            }
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeights.MARKET_INDICATOR)
    )
}
