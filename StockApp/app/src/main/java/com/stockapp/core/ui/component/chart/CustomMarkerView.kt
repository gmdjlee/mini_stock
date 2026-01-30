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

/**
 * OscillatorMarkerView - Market cap oscillator chart marker
 * Shows market cap and oscillator value
 */
@SuppressLint("ViewConstructor")
class OscillatorMarkerView(
    context: Context,
    private val dates: List<String>,
    private val mcapValues: List<Double>,
    private val oscillatorValues: List<Double>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val date = if (index >= 0 && index < dates.size) dates[index] else ""

            val mcap = if (index >= 0 && index < mcapValues.size) {
                formatMarketCapForChart(mcapValues[index])
            } else "N/A"

            val osc = if (index >= 0 && index < oscillatorValues.size) {
                String.format("%.4f%%", oscillatorValues[index] * 100)
            } else "N/A"

            tvContent?.text = "$date\n시가총액: $mcap\n오실레이터: $osc"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * SupplyDemandMarkerView - Supply demand bar chart marker
 * Shows foreign and institution net buying values
 */
@SuppressLint("ViewConstructor")
class SupplyDemandMarkerView(
    context: Context,
    private val dates: List<String>,
    private val foreignValues: List<Double>,
    private val institutionValues: List<Double>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val date = if (index >= 0 && index < dates.size) dates[index] else ""

            val foreign = if (index >= 0 && index < foreignValues.size) {
                String.format("%,.0f억", foreignValues[index])
            } else "N/A"

            val institution = if (index >= 0 && index < institutionValues.size) {
                String.format("%,.0f억", institutionValues[index])
            } else "N/A"

            tvContent?.text = "$date\n외국인: $foreign\n기관: $institution"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * IncomeBarMarkerView - Financial income bar chart marker
 * Shows period with revenue, operating profit, net income values
 */
@SuppressLint("ViewConstructor")
class IncomeBarMarkerView(
    context: Context,
    private val periods: List<String>,
    private val revenues: List<Long>,
    private val operatingProfits: List<Long>,
    private val netIncomes: List<Long>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val period = if (index >= 0 && index < periods.size) periods[index] else ""

            val revenue = if (index >= 0 && index < revenues.size) {
                formatFinancialValue(revenues[index])
            } else "N/A"
            val opProfit = if (index >= 0 && index < operatingProfits.size) {
                formatFinancialValue(operatingProfits[index])
            } else "N/A"
            val netIncome = if (index >= 0 && index < netIncomes.size) {
                formatFinancialValue(netIncomes[index])
            } else "N/A"

            tvContent?.text = "$period\n매출: $revenue\n영업이익: $opProfit\n순이익: $netIncome"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }

    private fun formatFinancialValue(value: Long): String {
        val absValue = kotlin.math.abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            absValue >= 10000 -> String.format("%s%.1f조", sign, absValue / 10000.0)
            absValue >= 1000 -> String.format("%s%.1f천억", sign, absValue / 1000.0)
            else -> "${sign}${absValue}억"
        }
    }
}

/**
 * GrowthRateMarkerView - Financial growth rate line chart marker
 * Shows period with multiple growth rate percentages
 */
@SuppressLint("ViewConstructor")
class GrowthRateMarkerView(
    context: Context,
    private val periods: List<String>,
    private val labels: List<String>,
    private val valuesList: List<List<Double>>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val period = if (index >= 0 && index < periods.size) periods[index] else ""

            val valuesText = labels.mapIndexed { seriesIndex, label ->
                val values = valuesList.getOrNull(seriesIndex) ?: emptyList()
                val value = if (index >= 0 && index < values.size) {
                    String.format("%.1f%%", values[index])
                } else "N/A"
                "$label: $value"
            }.joinToString("\n")

            tvContent?.text = "$period\n$valuesText"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * StabilityRatioMarkerView - Financial stability ratio marker
 * Shows period with debt ratio, current ratio, borrowing dependency
 */
@SuppressLint("ViewConstructor")
class StabilityRatioMarkerView(
    context: Context,
    private val periods: List<String>,
    private val debtRatios: List<Double>,
    private val currentRatios: List<Double>,
    private val borrowingDependencies: List<Double>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val period = if (index >= 0 && index < periods.size) periods[index] else ""

            val debt = if (index >= 0 && index < debtRatios.size) {
                String.format("%.1f%%", debtRatios[index])
            } else "N/A"
            val current = if (index >= 0 && index < currentRatios.size) {
                String.format("%.1f%%", currentRatios[index])
            } else "N/A"
            val borrowing = if (index >= 0 && index < borrowingDependencies.size) {
                String.format("%.1f%%", borrowingDependencies[index])
            } else "N/A"

            tvContent?.text = "$period\n부채비율: $debt\n유동비율: $current\n차입금의존도: $borrowing"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

/**
 * SingleRatioMarkerView - Single financial ratio marker
 * Shows period with a single ratio value and its label
 */
@SuppressLint("ViewConstructor")
class SingleRatioMarkerView(
    context: Context,
    private val periods: List<String>,
    private val values: List<Double>,
    private val ratioLabel: String
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            val period = if (index >= 0 && index < periods.size) periods[index] else ""

            val value = if (index >= 0 && index < values.size) {
                String.format("%.1f%%", values[index])
            } else "N/A"

            tvContent?.text = "$period\n$ratioLabel: $value"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
