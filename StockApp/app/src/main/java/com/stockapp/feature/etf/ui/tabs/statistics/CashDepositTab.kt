package com.stockapp.feature.etf.ui.tabs.statistics

import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.stockapp.core.ui.theme.ChartGridDark
import com.stockapp.core.ui.theme.ChartGridLight
import com.stockapp.core.ui.theme.LocalExtendedColors
import com.stockapp.feature.etf.domain.model.CashDepositTrend
import com.stockapp.feature.etf.domain.model.EtfCashDetail
import com.stockapp.feature.etf.domain.usecase.CashDepositTrendResult
import java.text.NumberFormat
import java.util.Locale

// Chart colors
private val CashLineColor = androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green

/**
 * Sealed class for cash deposit state - following RankingState pattern (EtfVm.kt)
 */
sealed class CashDepositState {
    data object Loading : CashDepositState()
    data object NoData : CashDepositState()
    data class Success(val result: CashDepositTrendResult) : CashDepositState()
    data class Error(val message: String) : CashDepositState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashDepositTab(
    state: CashDepositState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is CashDepositState.Loading -> LoadingContent()
        is CashDepositState.NoData -> NoDataContent()
        is CashDepositState.Error -> ErrorContent(message = state.message)
        is CashDepositState.Success -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier.fillMaxSize()
            ) {
                CashDepositContent(
                    result = state.result
                )
            }
        }
    }
}

@Composable
private fun CashDepositContent(
    result: CashDepositTrendResult
) {
    val extendedColors = LocalExtendedColors.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Header card with summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ETF 현금/예금 보유현황",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "기간: ${result.startDate} ~ ${result.endDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Total amount
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "총 현금보유",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatAmount(result.latestTotalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Change amount
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "변동금액",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val changeColor = when {
                                result.latestChangeAmount > 0 -> extendedColors.danger // Red for increase (Korean)
                                result.latestChangeAmount < 0 -> extendedColors.info   // Blue for decrease (Korean)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                text = formatAmountChange(result.latestChangeAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = changeColor
                            )
                        }
                        // Change rate
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "변동률",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val rateColor = when {
                                result.latestChangeRate > 0 -> extendedColors.danger
                                result.latestChangeRate < 0 -> extendedColors.info
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                text = String.format("%+.2f%%", result.latestChangeRate),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = rateColor
                            )
                        }
                    }
                }
            }
        }

        // Line chart - Following EtfCharts.kt pattern
        item {
            if (result.trend.isNotEmpty()) {
                Text(
                    text = "현금 보유 추이",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                CashDepositTrendChart(trend = result.trend)
            }
        }

        // ETF cash details section header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ETF별 현금 보유내역",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            // Table header
            EtfCashTableHeader()
            HorizontalDivider()
        }

        // ETF cash details list
        if (result.etfCashDetails.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "현금/예금 보유 ETF가 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = result.etfCashDetails.sortedByDescending { it.cashAmount },
                key = { "${it.etfCode}_${it.cashName}" }
            ) { detail ->
                EtfCashDetailRow(detail = detail)
                HorizontalDivider()
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

/**
 * Cash deposit trend line chart - Following EtfCharts.kt AmountTrendChart pattern
 */
@Composable
private fun CashDepositTrendChart(
    trend: List<CashDepositTrend>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val gridColor = if (isDark) ChartGridDark.toArgb() else ChartGridLight.toArgb()
    val textColor = if (isDark) Color.WHITE else Color.BLACK

    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    granularity = 1f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                    this.textColor = textColor
                    enableGridDashedLine(10f, 10f, 0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatAmountShort(value.toLong())
                        }
                    }
                }

                axisRight.isEnabled = false
                legend.isEnabled = false
                setExtraOffsets(8f, 8f, 8f, 8f)
            }
        },
        update = { chart ->
            val entries = trend.mapIndexed { index, data ->
                Entry(index.toFloat(), data.totalAmount.toFloat())
            }

            val dataSet = LineDataSet(entries, "현금보유").apply {
                color = CashLineColor.toArgb()
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(CashLineColor.toArgb())
                setDrawCircleHole(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = CashLineColor.toArgb()
                fillAlpha = 30
            }

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < trend.size) {
                        formatDateShort(trend[index].date)
                    } else ""
                }
            }
            chart.xAxis.setLabelCount(minOf(5, trend.size), false)

            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
    )
}

@Composable
private fun EtfCashTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ETF명",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "현금명",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "금액",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "비중",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EtfCashDetailRow(detail: EtfCashDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.etfName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail.etfCode,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = detail.cashName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatAmount(detail.cashAmount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End
        )
        Text(
            text = String.format("%.2f%%", detail.cashWeight),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End
        )
    }
}

// Loading/NoData/Error components - Reuse from StockRankingTab.kt pattern
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NoDataContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "현금 보유 데이터가 없습니다",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "수집현황 탭에서 ETF 데이터를 수집해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "데이터 로드 실패",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// Utility functions - From EtfCharts.kt and StockRankingTab.kt
private fun formatAmount(amount: Long): String {
    return when {
        amount >= 1_000_000_000_000 -> String.format("%.1f조", amount / 1_000_000_000_000.0)
        amount >= 100_000_000 -> String.format("%.0f억", amount / 100_000_000.0)
        amount >= 10_000 -> String.format("%.0f만", amount / 10_000.0)
        else -> NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
    }
}

private fun formatAmountChange(change: Long): String {
    val sign = if (change > 0) "+" else ""
    return sign + formatAmount(kotlin.math.abs(change))
}

private fun formatAmountShort(amount: Long): String = formatAmount(amount)

private fun formatDateShort(date: String): String {
    return try {
        val parts = date.split("-")
        if (parts.size >= 3) "${parts[1]}/${parts[2]}" else date
    } catch (e: Exception) {
        date
    }
}
