package com.stockapp.feature.indicator.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onBackClick: () -> Unit,
    viewModel: IndicatorVm = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

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
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
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
            )
        }
    ) { paddingValues ->
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
                when (val currentState = state) {
                    is IndicatorState.Loading -> {
                        LoadingContent()
                    }

                    is IndicatorState.Success -> {
                        IndicatorContent(
                            state = currentState,
                            selectedTab = selectedTab,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is IndicatorState.Error -> {
                        ErrorContent(
                            message = currentState.msg,
                            onRetry = { viewModel.retry() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorContent(
    state: IndicatorState.Success,
    selectedTab: IndicatorType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (selectedTab) {
            IndicatorType.TREND -> {
                state.trend?.let { TrendContent(it) }
                    ?: DataNotLoaded()
            }
            IndicatorType.ELDER -> {
                state.elder?.let { ElderContent(it) }
                    ?: DataNotLoaded()
            }
            IndicatorType.DEMARK -> {
                state.demark?.let { DemarkContent(it) }
                    ?: DataNotLoaded()
            }
        }
    }
}

// ========== Trend Signal Content ==========

@Composable
private fun TrendContent(summary: TrendSummary) {
    // Current Status Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getTrendColor(summary.currentTrend).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "현재 추세",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.trendLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = getTrendColor(summary.currentTrend)
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

    // CMF Chart
    ChartCard(title = "CMF (Chaikin Money Flow)") {
        LineChartContent(
            values = summary.cmfHistory.take(60).reversed(),
            color = Color(0xFF2196F3)
        )
    }

    // Fear/Greed Chart
    ChartCard(title = "Fear/Greed Index") {
        LineChartContent(
            values = summary.fearGreedHistory.take(60).reversed(),
            color = Color(0xFFFF9800)
        )
    }
}

// ========== Elder Impulse Content ==========

@Composable
private fun ElderContent(summary: ElderSummary) {
    // Current Status Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getElderColor(summary.currentColor).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "현재 Impulse",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.colorLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = getElderColor(summary.currentColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary.impulseSignal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // MACD Histogram
    MetricCard(
        title = "MACD Histogram",
        value = String.format("%.0f", summary.currentMacdHist),
        label = if (summary.currentMacdHist > 0) "상승 모멘텀" else "하락 모멘텀",
        color = if (summary.currentMacdHist > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
        modifier = Modifier.fillMaxWidth()
    )

    // Impulse Color Chart
    ChartCard(title = "Elder Impulse 색상 분포") {
        ElderColorChart(colors = summary.colorHistory.take(60).reversed())
    }

    // MACD Histogram Chart
    ChartCard(title = "MACD Histogram") {
        BarChartContent(
            values = summary.macdHistHistory.take(60).reversed()
        )
    }
}

// ========== DeMark TD Setup Content ==========

@Composable
private fun DemarkContent(summary: DemarkSummary) {
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
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        text = "${summary.currentBuySetup}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
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
            title = "Sell Signal",
            signal = summary.sellSignal,
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        SignalCard(
            title = "Buy Signal",
            signal = summary.buySignal,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
    }

    // Max Values
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            title = "최대 Sell",
            value = "${summary.maxSellSetup}",
            label = "기간 내 최대값",
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "최대 Buy",
            value = "${summary.maxBuySetup}",
            label = "기간 내 최대값",
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
    }

    // Setup Chart
    ChartCard(title = "TD Setup 추이") {
        DemarkSetupChart(
            sellSetup = summary.sellSetupHistory.take(60).reversed(),
            buySetup = summary.buySetupHistory.take(60).reversed()
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.height(200.dp)) {
                content()
            }
        }
    }
}

// ========== Chart Components ==========

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
private fun BarChartContent(values: List<Double>) {
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
private fun ElderColorChart(colors: List<String>) {
    if (colors.isEmpty()) {
        NoChartData()
        return
    }

    // Convert colors to numeric values for bar chart
    val values = colors.map { color ->
        when (color) {
            "green" -> 1.0
            "red" -> -1.0
            else -> 0.0
        }
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
private fun DemarkSetupChart(
    sellSetup: List<Int>,
    buySetup: List<Int>
) {
    if (sellSetup.isEmpty() || buySetup.isEmpty()) {
        NoChartData()
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(sellSetup, buySetup) {
        modelProducer.runTransaction {
            // Show sell setup as positive, buy setup as negative for contrast
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
                        color = Color(0xFFF44336),
                        thickness = 4.dp
                    ),
                    rememberLineComponent(
                        color = Color(0xFF2196F3),
                        thickness = 4.dp
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

private fun getTrendColor(trend: String): Color = when (trend) {
    "bullish" -> Color(0xFF4CAF50)
    "bearish" -> Color(0xFFF44336)
    else -> Color(0xFF9E9E9E)
}

private fun getElderColor(color: String): Color = when (color) {
    "green" -> Color(0xFF4CAF50)
    "red" -> Color(0xFFF44336)
    else -> Color(0xFF2196F3)
}
