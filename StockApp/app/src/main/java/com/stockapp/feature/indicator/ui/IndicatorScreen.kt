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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.stockapp.feature.indicator.domain.model.DemarkSummary
import com.stockapp.feature.indicator.domain.model.ElderSummary
import com.stockapp.feature.indicator.domain.model.IndicatorType
import com.stockapp.feature.indicator.domain.model.TrendSummary

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
                        if (isSelected) Color(0xFF3D5A3D)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF3D5A3D) else Color.Transparent,
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
    // Title with timeframe
    Text(
        text = "추세 시그널 (MA/CMF/Fear&Greed)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "현재 상태: ${summary.trendLabel}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Main Trend Chart with price and indicators
    ChartCard(title = "추세 시그널 (${timeframe.label})") {
        TrendSignalChart(summary = summary)
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

    // CMF Chart
    ChartCard(title = "CMF (Chaikin Money Flow)") {
        LineChartContent(
            values = summary.cmfHistory.takeLast(60),
            color = Color(0xFF2196F3)
        )
    }

    // Fear/Greed Chart
    ChartCard(title = "Fear/Greed Index") {
        LineChartContent(
            values = summary.fearGreedHistory.takeLast(60),
            color = Color(0xFFFF9800)
        )
    }
}

// ========== Elder Impulse Content ==========

@Composable
private fun ElderContent(summary: ElderSummary, timeframe: Timeframe) {
    // Title with timeframe
    Text(
        text = "Elder Impulse System (${timeframe.label})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "현재 상태: ${summary.colorLabel}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Elder Impulse Chart with colored dots
    ChartCard(title = "Elder Impulse (${timeframe.label})") {
        ElderImpulseChart(summary = summary)
    }

    // MACD Chart
    ChartCard(title = "MACD") {
        MACDBarChart(values = summary.macdHistHistory.takeLast(60))
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
    // Title with timeframe
    Text(
        text = "DeMark TD Setup (${timeframe.label})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    val currentState = when {
        summary.currentSellSetup > summary.currentBuySetup -> "상승 지속 (${summary.currentSellSetup})"
        summary.currentBuySetup > summary.currentSellSetup -> "하락 지속 (${summary.currentBuySetup})"
        else -> "중립"
    }
    Text(
        text = "현재 상태: $currentState",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // DeMark Setup Chart (area chart style like screenshot)
    ChartCard(title = "DeMark TD Setup (${timeframe.label})") {
        DemarkAreaChart(
            sellSetup = summary.sellSetupHistory.takeLast(60),
            buySetup = summary.buySetupHistory.takeLast(60),
            mcapHistory = summary.mcapHistory.takeLast(60)
        )
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
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.height(240.dp)) {
                content()
            }
        }
    }
}

// ========== Chart Components ==========

@Composable
private fun TrendSignalChart(summary: TrendSummary) {
    val priceHistory = summary.priceHistory.takeLast(60)
    val fearGreedHistory = summary.fearGreedHistory.takeLast(60)

    if (priceHistory.isEmpty()) {
        NoChartData()
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(priceHistory, fearGreedHistory) {
        modelProducer.runTransaction {
            lineSeries {
                series(priceHistory)  // Price (dashed black)
                if (fearGreedHistory.isNotEmpty()) {
                    series(fearGreedHistory.map { it * 50000 + 100000 })  // Fear/Greed scaled (purple)
                }
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside
            ),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ElderImpulseChart(summary: ElderSummary) {
    val ema13History = summary.ema13History.takeLast(60)
    val macdHistHistory = summary.macdHistHistory.takeLast(60)

    if (ema13History.isEmpty()) {
        NoChartData()
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(ema13History, macdHistHistory) {
        modelProducer.runTransaction {
            lineSeries {
                series(ema13History)  // EMA13 line
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside
            ),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MACDBarChart(values: List<Double>) {
    if (values.isEmpty()) {
        NoChartData()
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(values) {
        modelProducer.runTransaction {
            columnSeries { series(values) }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = rememberStartAxis(
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside
            ),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun DemarkAreaChart(
    sellSetup: List<Int>,
    buySetup: List<Int>,
    mcapHistory: List<Double>
) {
    if (sellSetup.isEmpty() || buySetup.isEmpty()) {
        NoChartData()
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(sellSetup, buySetup, mcapHistory) {
        modelProducer.runTransaction {
            // Show sell setup as positive (red area), buy setup as negative (green area)
            columnSeries {
                series(sellSetup.map { it.toDouble() })
                series(buySetup.map { -it.toDouble() })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        color = Color(0xFFF44336).copy(alpha = 0.5f),
                        thickness = 8.dp
                    ),
                    rememberLineComponent(
                        color = Color(0xFF4CAF50).copy(alpha = 0.5f),
                        thickness = 8.dp
                    )
                )
            ),
            startAxis = rememberStartAxis(
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside
            ),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun LineChartContent(
    values: List<Double>,
    color: Color
) {
    if (values.isEmpty()) {
        NoChartData()
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries { series(values) }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside
            ),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun NoChartData() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "차트 데이터 없음",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    "green" -> Color(0xFF4CAF50)
    "red" -> Color(0xFFF44336)
    else -> Color(0xFF2196F3)
}
