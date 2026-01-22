package com.stockapp.feature.indicator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.theme.ThemeToggleButton
import com.stockapp.core.ui.component.chart.ChartCard
import com.stockapp.core.ui.component.chart.DemarkTDChart
import com.stockapp.core.ui.component.chart.ElderImpulseChart
import com.stockapp.core.ui.component.chart.MacdChart
import com.stockapp.core.ui.component.chart.MacdHistogramChart
import com.stockapp.core.ui.component.chart.SimpleLineChart
import com.stockapp.core.ui.component.chart.TrendSignalChart
import com.stockapp.core.ui.theme.ChartPrimary
import com.stockapp.core.ui.theme.ChartPurple
import com.stockapp.core.ui.theme.ElderBlue
import com.stockapp.core.ui.theme.ElderGreen
import com.stockapp.core.ui.theme.ElderRed
import com.stockapp.feature.indicator.domain.model.DemarkSummary
import com.stockapp.feature.indicator.domain.model.ElderSummary
import com.stockapp.feature.indicator.domain.model.IndicatorType
import com.stockapp.feature.indicator.domain.model.TrendSummary

/**
 * Chart display constants for trend signal visualization.
 */
private object ChartConfig {
    /** Maximum days to display in charts (6 months of weekly data) */
    const val CHART_MAX_DAYS = 180
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorScreen(
    viewModel: IndicatorVm = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = state) {
                        is IndicatorState.Success -> "${s.stockName} 기술 지표"
                        else -> "기술 지표"
                    }
                    Text(title)
                },
                actions = {
                    if (state is IndicatorState.Success || state is IndicatorState.Error) {
                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !isRefreshing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "새로고침"
                            )
                        }
                    }
                    ThemeToggleButton()
                }
            )
        }
    ) { paddingValues ->
        when (val currentState = state) {
            is IndicatorState.NoStock -> {
                NoStockContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is IndicatorState.Loading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is IndicatorState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = IndicatorType.entries.indexOf(selectedTab),
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 16.dp
                    ) {
                        IndicatorType.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(tab.label) }
                            )
                        }
                    }

                    // Content
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        IndicatorContent(
                            state = currentState,
                            selectedTab = selectedTab,
                            selectedTimeframe = selectedTimeframe,
                            onTimeframeSelect = { viewModel.selectTimeframe(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            is IndicatorState.Error -> {
                ErrorContent(
                    message = currentState.msg,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun NoStockContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "기술 지표",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "검색에서 종목을 선택하세요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun IndicatorContent(
    state: IndicatorState.Success,
    selectedTab: IndicatorType,
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeframe selector (일봉/주봉/월봉)
        TimeframeSelector(
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelect = onTimeframeSelect
        )

        when (selectedTab) {
            IndicatorType.TREND -> {
                state.trend?.let { TrendContent(it, selectedTimeframe) }
                    ?: DataNotLoaded()
            }
            IndicatorType.ELDER -> {
                state.elder?.let { ElderContent(it, selectedTimeframe) }
                    ?: DataNotLoaded()
            }
            IndicatorType.DEMARK -> {
                state.demark?.let { DemarkContent(it, selectedTimeframe) }
                    ?: DataNotLoaded()
            }
        }
    }
}

// ========== Timeframe Selector ==========

@Composable
private fun TimeframeSelector(
    selectedTimeframe: Timeframe,
    onTimeframeSelect: (Timeframe) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Timeframe.entries.forEach { timeframe ->
            val isSelected = timeframe == selectedTimeframe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isSelected) ChartPrimary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) ChartPrimary else Color.Transparent,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onTimeframeSelect(timeframe) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeframe.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ========== Trend Signal Content ==========

@Composable
private fun TrendContent(summary: TrendSummary, timeframe: Timeframe) {
    // Python returns data in reverse chronological order (newest first)
    // take(N) gets newest N days, reversed() converts to chronological order for chart display
    val displayCount = minOf(ChartConfig.CHART_MAX_DAYS, summary.dates.size)
    val dates = summary.dates.take(displayCount).reversed()
    val priceHistory = summary.priceHistory.take(displayCount).reversed()
    val fearGreedHistory = summary.fearGreedHistory.take(displayCount).reversed()
    val cmfHistory = summary.cmfHistory.take(displayCount).reversed()

    // MA10 for overlay line (using ma10History if available, otherwise ma20)
    val ma10History = summary.ma10History.take(displayCount)
        .mapNotNull { it?.toDouble() }
        .reversed()

    // MA20 for MA overlay line - use ma20 to avoid overlap with priceHistory (which uses ma10)
    val ma20History = summary.ma20History.take(displayCount)
        .mapNotNull { it?.toDouble() }
        .reversed()

    // Calculate signal indices for the displayed data range
    // Signals are calculated on reversed data (chronological order)
    val maSignalHistory = summary.maSignalHistory.take(displayCount).reversed()
    val trendHistory = summary.trendHistory.take(displayCount).reversed()

    // Generate signal indices based on trend and maSignal
    val primaryBuySignals = mutableListOf<Int>()
    val additionalBuySignals = mutableListOf<Int>()
    val primarySellSignals = mutableListOf<Int>()
    val additionalSellSignals = mutableListOf<Int>()

    for (i in maSignalHistory.indices) {
        if (i < trendHistory.size) {
            val signal = maSignalHistory[i]
            val trend = trendHistory[i]

            when {
                // Primary Buy: bullish trend AND positive signal
                signal == 1 && trend == "bullish" -> primaryBuySignals.add(i)
                // Additional Buy: positive signal but not bullish trend
                signal == 1 && trend != "bullish" -> additionalBuySignals.add(i)
                // Primary Sell: bearish trend AND negative signal
                signal == -1 && trend == "bearish" -> primarySellSignals.add(i)
                // Additional Sell: negative signal but not bearish trend
                signal == -1 && trend != "bearish" -> additionalSellSignals.add(i)
            }
        }
    }

    // Format latest date for subtitle
    val latestDate = dates.lastOrNull()?.let { date ->
        val normalized = date.replace("-", "")
        if (normalized.length >= 8) {
            "${normalized.substring(0, 4)}-${normalized.substring(4, 6)}-${normalized.substring(6, 8)}"
        } else date
    } ?: ""
    val timeframeLabel = when (timeframe) {
        Timeframe.DAILY -> "d"
        Timeframe.WEEKLY -> "w"
        Timeframe.MONTHLY -> "m"
    }

    // Title with current status
    Text(
        text = "추세 시그널 (MA/CMF/Fear&Greed)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    // Main Trend Signal Chart with all signals
    if (priceHistory.isNotEmpty() && fearGreedHistory.isNotEmpty()) {
        ChartCard(
            title = "추세 시그널 (${timeframe.label})",
            subtitle = "최신 데이터: $latestDate ($timeframeLabel)"
        ) {
            TrendSignalChart(
                dates = dates,
                priceValues = priceHistory,
                fearGreedValues = fearGreedHistory,
                ma10Values = ma20History,  // Use MA20 to avoid overlap with priceHistory (which uses MA10)
                primaryBuySignals = primaryBuySignals,
                additionalBuySignals = additionalBuySignals,
                primarySellSignals = primarySellSignals,
                additionalSellSignals = additionalSellSignals
            )
        }
    }

    // Metrics Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            title = "CMF",
            value = String.format("%.3f", summary.currentCmf),
            label = summary.cmfLabel,
            color = if (summary.currentCmf > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Fear/Greed",
            value = String.format("%.3f", summary.currentFearGreed),
            label = summary.fearGreedLabel,
            color = when {
                summary.currentFearGreed > 0.5 -> Color(0xFFF44336)
                summary.currentFearGreed < -0.5 -> Color(0xFF2196F3)
                else -> Color(0xFF9E9E9E)
            },
            modifier = Modifier.weight(1f)
        )
    }

    // CMF Chart (EtfMonitor style)
    if (cmfHistory.isNotEmpty()) {
        ChartCard(title = "CMF (Chaikin Money Flow)") {
            SimpleLineChart(
                dates = dates,
                values = cmfHistory,
                lineColor = Color(0xFF2196F3),
                label = "CMF"
            )
        }
    }

    // Fear/Greed Chart (EtfMonitor style)
    if (fearGreedHistory.isNotEmpty()) {
        ChartCard(title = "Fear/Greed Index") {
            SimpleLineChart(
                dates = dates,
                values = fearGreedHistory,
                lineColor = ChartPurple,
                label = "Fear/Greed"
            )
        }
    }
}

// ========== Elder Impulse Content ==========

@Composable
private fun ElderContent(summary: ElderSummary, timeframe: Timeframe) {
    // Python returns data in reverse chronological order (newest first)
    // take(N) gets newest N days, reversed() converts to chronological order for chart display
    val displayCount = minOf(ChartConfig.CHART_MAX_DAYS, summary.dates.size)
    val dates = summary.dates.take(displayCount).reversed()
    val mcapHistory = summary.mcapHistory.take(displayCount).reversed()
    val ema13History = summary.ema13History.take(displayCount).reversed()
    val impulseStates = summary.impulseStates.take(displayCount).reversed()
    val macdLineHistory = summary.macdLineHistory.take(displayCount).reversed()
    val signalLineHistory = summary.signalLineHistory.take(displayCount).reversed()
    val macdHistHistory = summary.macdHistHistory.take(displayCount).reversed()

    // Title with current status
    Text(
        text = "Elder Impulse System (${timeframe.label})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    // Elder Impulse Chart (EtfMonitor style with market cap and impulse markers)
    if (mcapHistory.isNotEmpty() && ema13History.isNotEmpty() && impulseStates.isNotEmpty()) {
        ChartCard(
            title = "Elder Impulse (${timeframe.label})",
            subtitle = "현재 상태: ${summary.colorLabel}"
        ) {
            ElderImpulseChart(
                dates = dates,
                priceValues = mcapHistory,
                ema13Values = ema13History,
                impulseStates = impulseStates
            )
        }
    }

    // MACD Chart (EtfMonitor style with MACD line, Signal line, and Histogram)
    if (macdLineHistory.isNotEmpty() && signalLineHistory.isNotEmpty() && macdHistHistory.isNotEmpty()) {
        ChartCard(title = "MACD") {
            MacdChart(
                dates = dates,
                macdValues = macdLineHistory,
                signalValues = signalLineHistory,
                histogramValues = macdHistHistory
            )
        }
    } else if (macdHistHistory.isNotEmpty()) {
        // Fallback to histogram only if full MACD data not available
        ChartCard(title = "MACD Histogram") {
            MacdHistogramChart(
                dates = dates,
                histogramValues = macdHistHistory
            )
        }
    }

    // Impulse Signal Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getElderColor(summary.currentColor).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Impulse 신호",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.impulseSignal,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = getElderColor(summary.currentColor)
            )
        }
    }
}

// ========== DeMark TD Setup Content ==========

@Composable
private fun DemarkContent(summary: DemarkSummary, timeframe: Timeframe) {
    // Python returns data in reverse chronological order (newest first)
    // take(N) gets newest N days, reversed() converts to chronological order for chart display
    val displayCount = minOf(ChartConfig.CHART_MAX_DAYS, summary.dates.size)
    val dates = summary.dates.take(displayCount).reversed()
    val sellSetupHistory = summary.sellSetupHistory.take(displayCount).reversed()
    val buySetupHistory = summary.buySetupHistory.take(displayCount).reversed()
    val mcapHistory = summary.mcapHistory.take(displayCount).reversed()

    // Title
    Text(
        text = "DeMark TD Setup (${timeframe.label})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    val currentState = when {
        summary.currentSellSetup >= 9 -> "매도 피로 (${summary.currentSellSetup}) - 하락 전환 가능"
        summary.currentBuySetup >= 9 -> "매수 피로 (${summary.currentBuySetup}) - 상승 전환 가능"
        summary.currentSellSetup > summary.currentBuySetup -> "상승 지속 (${summary.currentSellSetup})"
        summary.currentBuySetup > summary.currentSellSetup -> "하락 지속 (${summary.currentBuySetup})"
        else -> "중립"
    }

    // DeMark TD Setup Chart (EtfMonitor style with dual bars and optional market cap overlay)
    if (sellSetupHistory.isNotEmpty() && buySetupHistory.isNotEmpty()) {
        ChartCard(
            title = "DeMark TD Setup (${timeframe.label})",
            subtitle = "현재 상태: $currentState"
        ) {
            DemarkTDChart(
                dates = dates,
                sellSetupValues = sellSetupHistory,
                buySetupValues = buySetupHistory,
                mcapValues = mcapHistory
            )
        }
    }

    // Current Status Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.hasActiveSetup)
                Color(0xFFFFEB3B).copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TD Setup 상태",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sell Setup",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        text = "${summary.currentSellSetup}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Buy Setup",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "${summary.currentBuySetup}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }

    // Signals
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SignalCard(
            title = "매도피로",
            signal = summary.sellSignal,
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        SignalCard(
            title = "매수피로",
            signal = summary.buySignal,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }
}

// ========== Common Components ==========

@Composable
private fun MetricCard(
    title: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SignalCard(
    title: String,
    signal: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = signal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DataNotLoaded() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text("다시 시도")
            }
        }
    }
}

// ========== Helper Functions ==========

private fun getElderColor(color: String): Color = when (color) {
    "green" -> ElderGreen
    "red" -> ElderRed
    else -> ElderBlue
}
