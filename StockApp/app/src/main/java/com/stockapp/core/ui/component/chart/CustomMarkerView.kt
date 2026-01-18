package com.stockapp.core.ui.component.chart

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.stockapp.R

/**
 * CustomMarkerView - General purpose chart marker
 * Shows date and formatted value on touch
 */
@SuppressLint("ViewConstructor")
class CustomMarkerView(
    context: Context,
    private val dates: List<String>,
    private val formatter: (Float) -> String
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val date = if (index >= 0 && index < dates.size) dates[index] else ""
            val formattedValue = formatter(entry.y)
            tvContent?.text = "$date\n$formattedValue"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * MacdMarkerView - MACD chart marker
 * Shows date, MACD value, and Signal value
 */
@SuppressLint("ViewConstructor")
class MacdMarkerView(
    context: Context,
    private val dates: List<String>,
    private val macdValues: List<Double>,
    private val signalValues: List<Double>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val date = if (index >= 0 && index < dates.size) dates[index] else ""
            val macd = if (index >= 0 && index < macdValues.size) {
                String.format("%.4f", macdValues[index])
            } else "N/A"
            val signal = if (index >= 0 && index < signalValues.size) {
                String.format("%.4f", signalValues[index])
            } else "N/A"

            tvContent?.text = "$date\nMACD: $macd\nSignal: $signal"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * MarketCapMarkerView - Market cap and oscillator marker
 * Shows different format based on dataset (market cap vs oscillator)
 */
@SuppressLint("ViewConstructor")
class MarketCapMarkerView(
    context: Context,
    private val dates: List<String>,
    private val isOscillatorDataSet: (Int) -> Boolean
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val date = if (index >= 0 && index < dates.size) dates[index] else ""

            val formattedValue = if (isOscillatorDataSet(highlight?.dataSetIndex ?: 0)) {
                String.format("%.4f%%", entry.y)
            } else {
                formatMarketCapForChart(entry.y.toDouble())
            }

            tvContent?.text = "$date\n$formattedValue"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * TrendSignalMarkerView - Trend signal chart marker
 * Shows price and Fear/Greed value
 */
@SuppressLint("ViewConstructor")
class TrendSignalMarkerView(
    context: Context,
    private val dates: List<String>,
    private val priceValues: List<Double>,
    private val fearGreedValues: List<Double>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val date = if (index >= 0 && index < dates.size) dates[index] else ""

            val price = if (index >= 0 && index < priceValues.size) {
                String.format("%,.0f", priceValues[index])
            } else "N/A"

            val fearGreed = if (index >= 0 && index < fearGreedValues.size) {
                String.format("%.3f", fearGreedValues[index])
            } else "N/A"

            tvContent?.text = "$date\nPrice: $price\nF&G: $fearGreed"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * ElderImpulseMarkerView - Elder Impulse chart marker
 * Shows market cap and impulse state
 */
@SuppressLint("ViewConstructor")
class ElderImpulseMarkerView(
    context: Context,
    private val dates: List<String>,
    private val mcapValues: List<Double>,
    private val impulseStates: List<Int>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val date = if (index >= 0 && index < dates.size) dates[index] else ""

            val mcap = if (index >= 0 && index < mcapValues.size) {
                formatMarketCapForChart(mcapValues[index])
            } else "N/A"

            val impulse = if (index >= 0 && index < impulseStates.size) {
                when (impulseStates[index]) {
                    1 -> "Bullish"
                    -1 -> "Bearish"
                    else -> "Neutral"
                }
            } else "N/A"

            tvContent?.text = "$date\nMcap: $mcap\nImpulse: $impulse"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * DemarkTDMarkerView - DeMark TD Setup marker
 * Shows sell/buy setup counts
 */
@SuppressLint("ViewConstructor")
class DemarkTDMarkerView(
    context: Context,
    private val dates: List<String>,
    private val sellSetupValues: List<Int>,
    private val buySetupValues: List<Int>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val date = if (index >= 0 && index < dates.size) dates[index] else ""

            val sellSetup = if (index >= 0 && index < sellSetupValues.size) {
                sellSetupValues[index].toString()
            } else "0"

            val buySetup = if (index >= 0 && index < buySetupValues.size) {
                buySetupValues[index].toString()
            } else "0"

            tvContent?.text = "$date\nSell: $sellSetup\nBuy: $buySetup"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
