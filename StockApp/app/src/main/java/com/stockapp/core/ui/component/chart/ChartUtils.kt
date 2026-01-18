package com.stockapp.core.ui.component.chart

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.renderer.scatter.IShapeRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.stockapp.core.ui.theme.ChartCardBackgroundDark
import com.stockapp.core.ui.theme.ChartCardBackgroundLight

/**
 * ChartCard - EtfMonitor style chart container
 * Provides consistent styling with title, subtitle, and content area
 */
@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val chartCardBackground = if (isDark) ChartCardBackgroundDark else ChartCardBackgroundLight

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300)),
        colors = CardDefaults.cardColors(
            containerColor = chartCardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp,
            hoveredElevation = 5.dp
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/**
 * ChartLabelCalculator - Dynamic label count calculation based on data points
 * Ensures optimal readability across different data ranges
 */
object ChartLabelCalculator {
    fun calculateOptimalLabelCount(dataPoints: Int): Int {
        return when {
            dataPoints <= 7 -> dataPoints.coerceAtLeast(2)   // 1 week: daily
            dataPoints <= 14 -> 7                            // 2 weeks: every 2 days
            dataPoints <= 30 -> 10                           // 1 month: every 3 days
            dataPoints <= 90 -> 10                           // 3 months: every 9 days
            dataPoints <= 180 -> 8                           // 6 months: every 22 days
            dataPoints <= 365 -> 8                           // 1 year: every 45 days
            dataPoints <= 730 -> 10                          // 2 years: every 73 days
            else -> 12                                       // > 2 years
        }
    }
}

/**
 * DateFormatter - Format dates for chart display
 * Python reference style: YYYY-MM format for longer datasets
 */
object DateFormatter {
    fun formatForChartByDataCount(date: String, dataCount: Int): String {
        // date format: "YYYY-MM-DD" or "YYYYMMDD"
        return try {
            val normalizedDate = date.replace("-", "")
            val year = normalizedDate.substring(0, 4)
            val month = normalizedDate.substring(4, 6)
            val day = normalizedDate.substring(6, 8)

            when {
                dataCount <= 30 -> "$month/$day"           // Show month/day
                dataCount <= 90 -> "$month/$day"           // Show month/day for up to 3 months
                else -> "$year-$month"                     // Python style: YYYY-MM for longer periods
            }
        } catch (e: Exception) {
            date.takeLast(5)
        }
    }
}

/**
 * InvertedTriangleShapeRenderer - Custom shape renderer for sell signals
 * Draws an inverted triangle (point facing down)
 */
class InvertedTriangleShapeRenderer : IShapeRenderer {
    override fun renderShape(
        c: Canvas,
        dataSet: IScatterDataSet,
        viewPortHandler: ViewPortHandler,
        posX: Float,
        posY: Float,
        renderPaint: Paint
    ) {
        val shapeSize = dataSet.scatterShapeSize
        val halfSize = shapeSize / 2f

        val path = Path()
        // Inverted triangle: flat top, point at bottom
        path.moveTo(posX - halfSize, posY - halfSize)  // Top left
        path.lineTo(posX + halfSize, posY - halfSize)  // Top right
        path.lineTo(posX, posY + halfSize)              // Bottom center (point)
        path.close()

        renderPaint.style = Paint.Style.FILL
        c.drawPath(path, renderPaint)
    }
}

/**
 * TriangleShapeRenderer - Custom shape renderer for buy signals
 * Draws a normal triangle (point facing up)
 */
class TriangleShapeRenderer : IShapeRenderer {
    override fun renderShape(
        c: Canvas,
        dataSet: IScatterDataSet,
        viewPortHandler: ViewPortHandler,
        posX: Float,
        posY: Float,
        renderPaint: Paint
    ) {
        val shapeSize = dataSet.scatterShapeSize
        val halfSize = shapeSize / 2f

        val path = Path()
        // Normal triangle: point at top, flat bottom
        path.moveTo(posX, posY - halfSize)              // Top center (point)
        path.lineTo(posX - halfSize, posY + halfSize)  // Bottom left
        path.lineTo(posX + halfSize, posY + halfSize)  // Bottom right
        path.close()

        renderPaint.style = Paint.Style.FILL
        c.drawPath(path, renderPaint)
    }
}

/**
 * Chart height constants for consistency
 */
object ChartHeights {
    val MARKET_CAP_OSCILLATOR = 350.dp
    val MACD = 300.dp
    val TREND_SIGNAL = 350.dp
    val ELDER_IMPULSE = 300.dp
    val DEMARK_TD = 300.dp
    val DEFAULT = 300.dp
}

/**
 * Format market cap value for display
 * @param billions Value in billions (억)
 * @return Formatted string with appropriate unit (조/억)
 */
fun formatMarketCapForChart(billions: Double): String {
    return when {
        billions >= 10000 -> "${(billions / 10000).toInt()}조"
        billions >= 1000 -> String.format("%.1f조", billions / 10000f)
        else -> "${billions.toInt()}억"
    }
}

/**
 * Format percentage for chart markers
 */
fun formatPercentForChart(value: Double): String {
    return String.format("%.4f%%", value * 100)
}

/**
 * Get grid color based on theme
 */
@Composable
fun getChartGridColor(): Int {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        Color(0xFF353733).toArgb()
    } else {
        Color(0xFFE1E4D5).toArgb()
    }
}

/**
 * Get text color based on theme
 */
@Composable
fun getChartTextColor(): Int {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        Color.White.toArgb()
    } else {
        Color.Black.toArgb()
    }
}
