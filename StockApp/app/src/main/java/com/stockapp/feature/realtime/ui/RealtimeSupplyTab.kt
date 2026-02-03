package com.stockapp.feature.realtime.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.feature.realtime.domain.model.RealtimeSupplySignal
import com.stockapp.feature.realtime.domain.model.RealtimeSupplySummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Realtime supply tab content.
 * Shows current trading session supply/demand data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeSupplyTab(
    viewModel: RealtimeSupplyVm = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val autoRefreshSettings by viewModel.autoRefreshSettings.collectAsState()
    val isTradingHours by viewModel.isTradingHours.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        when (val currentState = state) {
            is RealtimeSupplyState.NoStock -> {
                NoStockContent()
            }
            is RealtimeSupplyState.Loading -> {
                LoadingContent()
            }
            is RealtimeSupplyState.FeatureDisabled -> {
                FeatureDisabledContent()
            }
            is RealtimeSupplyState.Success -> {
                RealtimeSupplyContent(
                    summary = currentState.summary,
                    isTradingHours = isTradingHours,
                    tradingHoursString = viewModel.getTradingHoursString(),
                    autoRefreshEnabled = autoRefreshSettings.enabled,
                    onAutoRefreshChange = { viewModel.setAutoRefreshEnabled(it) },
                    onRefresh = { viewModel.refresh() }
                )
            }
            is RealtimeSupplyState.Error -> {
                ErrorContent(
                    code = currentState.code,
                    message = currentState.msg,
                    onRetry = { viewModel.retry() }
                )
            }
        }
    }
}

@Composable
private fun NoStockContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "종목을 선택해주세요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "검색 화면에서 종목을 검색하고 선택하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "실시간 수급 데이터 조회 중...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureDisabledContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "실시간 수급 기능이 비활성화되어 있습니다",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "설정 > Native 기능에서 '실시간 수급' 기능을 활성화하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorContent(
    code: String,
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "오류 발생",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "[$code] $message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("다시 시도")
            }
        }
    }
}

@Composable
private fun RealtimeSupplyContent(
    summary: RealtimeSupplySummary,
    isTradingHours: Boolean,
    tradingHoursString: String,
    autoRefreshEnabled: Boolean,
    onAutoRefreshChange: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Trading hours indicator
        TradingHoursIndicator(
            isTradingHours = isTradingHours,
            tradingHoursString = tradingHoursString
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stock info card
        StockInfoCard(summary = summary)

        Spacer(modifier = Modifier.height(16.dp))

        // Supply/Demand metrics card
        SupplyDemandCard(summary = summary)

        Spacer(modifier = Modifier.height(16.dp))

        // Signal card
        SignalCard(signal = summary.signal)

        Spacer(modifier = Modifier.height(16.dp))

        // Auto-refresh settings
        AutoRefreshCard(
            enabled = autoRefreshEnabled,
            onEnabledChange = onAutoRefreshChange,
            isTradingHours = isTradingHours
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Last updated info
        LastUpdatedInfo(
            fetchedAt = summary.fetchedAt,
            onRefresh = onRefresh
        )
    }
}

@Composable
private fun TradingHoursIndicator(
    isTradingHours: Boolean,
    tradingHoursString: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isTradingHours)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (isTradingHours) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isTradingHours) "장중" else "장외",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isTradingHours)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "(거래시간: $tradingHoursString)",
            style = MaterialTheme.typography.bodySmall,
            color = if (isTradingHours)
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StockInfoCard(summary: RealtimeSupplySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = summary.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary.ticker,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "현재가",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%,d원".format(summary.currentPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "누적 거래량",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%,d".format(summary.accumulatedVolume),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplyDemandCard(summary: RealtimeSupplySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "장중 수급 현황",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Net buy amount
            MetricRow(
                label = "순매수 금액",
                value = "%.1f억".format(summary.netBuyAmountBillion),
                valueColor = if (summary.netBuyAmountBillion >= 0)
                    Color(0xFFD32F2F) // Red for buy
                else
                    Color(0xFF1976D2) // Blue for sell
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Buy amount
            MetricRow(
                label = "매수 금액",
                value = "%.1f억".format(summary.buyAmountBillion),
                valueColor = Color(0xFFD32F2F)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Sell amount
            MetricRow(
                label = "매도 금액",
                value = "%.1f억".format(summary.sellAmountBillion),
                valueColor = Color(0xFF1976D2)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Net buy quantity
            MetricRow(
                label = "순매수 수량",
                value = "%,d".format(summary.netBuyQuantity),
                valueColor = if (summary.netBuyQuantity >= 0)
                    Color(0xFFD32F2F)
                else
                    Color(0xFF1976D2)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Net buy ratio
            val ratioPercent = summary.netBuyRatio * 100
            MetricRow(
                label = "순매수 비율",
                value = "%+.1f%%".format(ratioPercent),
                valueColor = if (ratioPercent >= 0)
                    Color(0xFFD32F2F)
                else
                    Color(0xFF1976D2)
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun SignalCard(signal: RealtimeSupplySignal) {
    val (backgroundColor, textColor) = remember(signal) {
        when (signal) {
            RealtimeSupplySignal.STRONG_BUY -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
            RealtimeSupplySignal.BUY -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
            RealtimeSupplySignal.NEUTRAL -> Color(0xFFF5F5F5) to Color(0xFF616161)
            RealtimeSupplySignal.SELL -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
            RealtimeSupplySignal.STRONG_SELL -> Color(0xFFBBDEFB) to Color(0xFF0D47A1)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "장중 수급 신호",
                style = MaterialTheme.typography.labelMedium,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = signal.label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = signal.description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AutoRefreshCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    isTradingHours: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "자동 새로고침",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isTradingHours) "1분마다 자동 갱신" else "장중에만 동작",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                enabled = isTradingHours
            )
        }
    }
}

@Composable
private fun LastUpdatedInfo(
    fetchedAt: Long,
    onRefresh: () -> Unit
) {
    val timeString = remember(fetchedAt) {
        val format = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        format.format(Date(fetchedAt))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "마지막 조회: $timeString",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "새로고침",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
