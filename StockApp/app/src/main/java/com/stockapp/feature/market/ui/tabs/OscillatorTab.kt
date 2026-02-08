package com.stockapp.feature.market.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockapp.feature.market.domain.model.OscillatorHistory
import com.stockapp.feature.market.domain.model.OscillatorSignal
import com.stockapp.feature.market.ui.MarketVm

@Composable
fun OscillatorTab(viewModel: MarketVm) {
    val state by viewModel.oscillatorState.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DateRangeSelector(
            selectedRange = dateRange,
            onRangeSelected = { viewModel.setDateRange(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val currentState = state
        when (currentState) {
            is MarketVm.OscillatorState.Idle -> {}

            is MarketVm.OscillatorState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "시장 데이터 분석 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is MarketVm.OscillatorState.Success -> {
                OscillatorContent(data = currentState.data)
            }

            is MarketVm.OscillatorState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = currentState.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun OscillatorContent(data: OscillatorHistory) {
    if (data.dates.isEmpty()) {
        Text("데이터가 없습니다.")
        return
    }

    val latestValue = data.values.lastOrNull() ?: 0.5
    val latestSignal = data.signals.lastOrNull() ?: OscillatorSignal.NEUTRAL
    val latestDate = data.dates.lastOrNull() ?: ""

    // Enhanced signal summary card
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "과매수/과매도",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = latestDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Large oscillator value
            Text(
                text = "${"%.1f".format(latestValue * 100)}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = signalColor(latestSignal)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = signalColor(latestSignal),
                            shape = CircleShape
                        )
                )
                Text(
                    text = latestSignal.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = signalColor(latestSignal)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = latestSignal.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Data table
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "데이터 테이블",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "날짜",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Oscillator",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "상승",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFFD32F2F)
                )
                Text(
                    "하락",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1976D2)
                )
                Text(
                    "상태",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider()

            // Table rows (show up to 20, latest first)
            val rowCount = minOf(data.dates.size, 20)
            val startIdx = data.dates.size - rowCount

            for (i in data.dates.size - 1 downTo startIdx) {
                val signal = data.signals[i]
                val rowBgColor = when (signal) {
                    OscillatorSignal.EXTREME_GREED -> Color(0x0CD32F2F)
                    OscillatorSignal.GREED -> Color(0x06FF5722)
                    OscillatorSignal.EXTREME_FEAR -> Color(0x0C1565C0)
                    OscillatorSignal.FEAR -> Color(0x062196F3)
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBgColor)
                        .padding(vertical = 5.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(data.dates[i]),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${"%.1f".format(data.values[i] * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${"%.0f".format(data.advanceRatios[i] * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${"%.0f".format(data.declineRatios[i] * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = signal.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = signalColor(signal),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }

                if (i > startIdx) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private fun signalColor(signal: OscillatorSignal): Color = when (signal) {
    OscillatorSignal.EXTREME_GREED -> Color(0xFFD32F2F)
    OscillatorSignal.GREED -> Color(0xFFFF5722)
    OscillatorSignal.NEUTRAL -> Color(0xFF9E9E9E)
    OscillatorSignal.FEAR -> Color(0xFF2196F3)
    OscillatorSignal.EXTREME_FEAR -> Color(0xFF1565C0)
}

private fun formatDate(yyyymmdd: String): String {
    if (yyyymmdd.length != 8) return yyyymmdd
    return "${yyyymmdd.substring(4, 6)}/${yyyymmdd.substring(6, 8)}"
}
