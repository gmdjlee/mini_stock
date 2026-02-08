package com.stockapp.feature.market.ui.tabs

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.stockapp.core.ui.component.chart.BloodLineChart
import com.stockapp.feature.market.domain.model.BloodIndicatorHistory
import com.stockapp.feature.market.domain.model.BloodSignal
import com.stockapp.feature.market.domain.model.MarketDateRange
import com.stockapp.feature.market.ui.MarketVm

@Composable
fun BloodTab(viewModel: MarketVm) {
    val state by viewModel.bloodState.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DateRangeSelector(
            selectedRange = dateRange,
            onRangeSelected = { viewModel.setDateRange(it) },
            allowedRanges = MarketDateRange.entries.toList()
        )

        Spacer(modifier = Modifier.height(16.dp))

        val currentState = state
        when (currentState) {
            is MarketVm.BloodState.Idle -> {}

            is MarketVm.BloodState.Loading -> {
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
                            text = "Blood Indicator 분석 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is MarketVm.BloodState.Success -> {
                BloodContent(data = currentState.data)
            }

            is MarketVm.BloodState.Error -> {
                NoDataCard(message = currentState.message)
            }
        }
    }
}

@Composable
private fun NoDataCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Blood Indicator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BLOOD = US 3M T-Bill Yield / High Yield Spread",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "설정 메뉴에서 FRED API 키를 입력하고\n데이터를 수집해 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BloodContent(data: BloodIndicatorHistory) {
    if (data.dates.isEmpty()) {
        NoDataCard(message = "Blood Indicator 데이터가 없습니다.")
        return
    }

    val latestBlood = data.bloodValues.lastOrNull() ?: 0.0
    val latestSma = data.sma100Values.lastOrNull() ?: 0.0
    val latestSignal = data.signals.lastOrNull() ?: BloodSignal.NEUTRAL
    val latestDate = data.dates.lastOrNull() ?: ""

    // Enhanced signal card with icon
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Blood Indicator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Large BLOOD value
            Text(
                text = "%.4f".format(latestBlood),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Signal with icon
            val signalColor = bloodSignalColor(latestSignal)
            val signalIcon = when (latestSignal) {
                BloodSignal.RISK_ON -> Icons.AutoMirrored.Filled.TrendingUp
                BloodSignal.RISK_OFF -> Icons.AutoMirrored.Filled.TrendingDown
                BloodSignal.NEUTRAL -> Icons.AutoMirrored.Filled.TrendingFlat
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = signalIcon,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = latestSignal.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = signalColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SMA100: ${"%.4f".format(latestSma)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = latestSignal.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatBloodDate(latestDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Components card (when data is available)
    if (data.tBillYields.isNotEmpty() && data.hySpreadValues.isNotEmpty()) {
        val latestTBill = data.tBillYields.lastOrNull() ?: 0.0
        val latestHySpread = data.hySpreadValues.lastOrNull() ?: 0.0
        val smaComparison = if (latestBlood > latestSma) "상향 돌파" else "하향 돌파"
        val smaCompColor = if (latestBlood > latestSma) Color(0xFF388E3C) else Color(0xFFD32F2F)

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "구성 요소",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                ComponentRow("US 3M T-Bill", "${"%.2f".format(latestTBill)}%")
                ComponentRow("HY Spread", "${"%.2f".format(latestHySpread)}%")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ComponentRow("100주 SMA", "%.4f".format(latestSma), Color(0xFF1976D2))
                ComponentRow("SMA 대비", smaComparison, smaCompColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Chart: BLOOD + SMA100
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "추이 차트",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            BloodLineChart(
                dates = data.dates,
                bloodValues = data.bloodValues,
                sma100Values = data.sma100Values
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Formula explanation
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "산출 방법",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "BLOOD = US 3-Month T-Bill Yield / High Yield Spread\n\n" +
                    "- Risk On: BLOOD > SMA100 × 1.1\n" +
                    "- Risk Off: BLOOD < SMA100 × 0.9\n" +
                    "- Neutral: 그 외 구간\n\n" +
                    "T-Bill: Yahoo Finance (^IRX)\n" +
                    "HY Spread: FRED (BAMLH0A0HYM2)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComponentRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun bloodSignalColor(signal: BloodSignal): Color = when (signal) {
    BloodSignal.RISK_ON -> Color(0xFF388E3C)
    BloodSignal.NEUTRAL -> Color(0xFF9E9E9E)
    BloodSignal.RISK_OFF -> Color(0xFFD32F2F)
}

private fun formatBloodDate(yyyymmdd: String): String {
    if (yyyymmdd.length != 8) return yyyymmdd
    return "${yyyymmdd.substring(0, 4)}/${yyyymmdd.substring(4, 6)}/${yyyymmdd.substring(6, 8)}"
}
