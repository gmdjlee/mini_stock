package com.stockapp.feature.market.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockapp.feature.market.domain.model.FearGreedSignal
import kotlin.math.cos
import kotlin.math.sin

private val EXTREME_FEAR_COLOR = Color(0xFF1565C0)
private val FEAR_COLOR = Color(0xFF2196F3)
private val NEUTRAL_COLOR = Color(0xFF9E9E9E)
private val GREED_COLOR = Color(0xFFFF5722)
private val EXTREME_GREED_COLOR = Color(0xFFD32F2F)

private val GAUGE_SEGMENTS = listOf(
    EXTREME_FEAR_COLOR to 36f,  // 0-20: 36 degrees
    FEAR_COLOR to 36f,          // 20-40
    NEUTRAL_COLOR to 36f,       // 40-60
    GREED_COLOR to 36f,         // 60-80
    EXTREME_GREED_COLOR to 36f  // 80-100
)

/**
 * Semi-circle gauge for Fear & Greed index.
 * Draws a 180-degree arc with 5 color segments and a needle indicator.
 */
@Composable
fun FearGreedGauge(
    score: Double,
    signal: FearGreedSignal,
    modifier: Modifier = Modifier
) {
    val scoreColor = fearGreedSignalColor(signal)
    val clampedScore = score.coerceIn(0.0, 100.0)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(width = 240.dp, height = 140.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.size(width = 240.dp, height = 140.dp)) {
                val strokeWidth = 24f
                val padding = strokeWidth / 2 + 8f
                val arcSize = Size(
                    width = size.width - padding * 2,
                    height = (size.width - padding * 2)
                )
                val topLeft = Offset(padding, size.height - arcSize.height / 2)

                // Draw background arc (light gray)
                drawArc(
                    color = Color(0xFFE8E8E8),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Draw colored segments
                var currentAngle = 180f
                for ((color, sweep) in GAUGE_SEGMENTS) {
                    drawArc(
                        color = color,
                        startAngle = currentAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    currentAngle += sweep
                }

                // Draw needle
                val needleAngle = 180.0 + (clampedScore / 100.0) * 180.0
                val needleRadians = Math.toRadians(needleAngle)
                val centerX = size.width / 2
                val centerY = size.height
                val needleLength = arcSize.width / 2 - strokeWidth - 4f

                val needleEndX = centerX + (needleLength * cos(needleRadians)).toFloat()
                val needleEndY = centerY + (needleLength * sin(needleRadians)).toFloat()

                // Needle line
                drawLine(
                    color = Color(0xFF333333),
                    start = Offset(centerX, centerY),
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // Needle pivot circle
                drawCircle(
                    color = Color(0xFF333333),
                    radius = 6f,
                    center = Offset(centerX, centerY)
                )
            }
        }

        // Score text
        Text(
            text = "%.0f".format(clampedScore),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = scoreColor
        )

        Text(
            text = signal.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = scoreColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Range labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "극도의 공포",
                style = MaterialTheme.typography.labelSmall,
                color = EXTREME_FEAR_COLOR
            )
            Text(
                "극도의 탐욕",
                style = MaterialTheme.typography.labelSmall,
                color = EXTREME_GREED_COLOR
            )
        }
    }
}

fun fearGreedSignalColor(signal: FearGreedSignal): Color = when (signal) {
    FearGreedSignal.EXTREME_GREED -> EXTREME_GREED_COLOR
    FearGreedSignal.GREED -> GREED_COLOR
    FearGreedSignal.NEUTRAL -> NEUTRAL_COLOR
    FearGreedSignal.FEAR -> FEAR_COLOR
    FearGreedSignal.EXTREME_FEAR -> EXTREME_FEAR_COLOR
}
