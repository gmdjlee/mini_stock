package com.stockapp.feature.ranking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.theme.ThemeToggleButton
import com.stockapp.core.ui.theme.LocalExtendedColors
import com.stockapp.feature.ranking.domain.model.ExchangeType
import com.stockapp.feature.ranking.domain.model.ItemCount
import com.stockapp.feature.ranking.domain.model.MarketType
import com.stockapp.feature.ranking.domain.model.RankingItem
import com.stockapp.feature.ranking.domain.model.RankingResult
import com.stockapp.feature.ranking.domain.model.RankingType
import com.stockapp.feature.settings.domain.model.InvestmentMode
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingVm = hiltViewModel(),
    onStockClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val rankingType by viewModel.rankingType.collectAsState()
    val marketType by viewModel.marketType.collectAsState()
    val exchangeType by viewModel.exchangeType.collectAsState()
    val itemCount by viewModel.itemCount.collectAsState()
    val investmentMode by viewModel.investmentMode.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("순위정보") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                    ThemeToggleButton()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Ranking type selector
            RankingTypeSelector(
                selectedType = rankingType,
                onTypeSelected = viewModel::onRankingTypeChange
            )

            // Market type tabs (KOSPI / KOSDAQ)
            MarketTypeTabs(
                selectedMarket = marketType,
                onMarketSelected = viewModel::onMarketTypeChange
            )

            // Exchange type tabs (only in Production mode)
            if (investmentMode == InvestmentMode.PRODUCTION) {
                ExchangeTypeTabs(
                    selectedExchange = exchangeType,
                    availableExchanges = viewModel.getAvailableExchangeTypes(),
                    onExchangeSelected = viewModel::onExchangeTypeChange
                )
            }

            // Item count selector
            ItemCountSelector(
                selectedCount = itemCount,
                onCountSelected = viewModel::onItemCountChange
            )

            // Content
            when (val currentState = state) {
                is RankingState.Loading -> {
                    LoadingContent()
                }
                is RankingState.NoApiKey -> {
                    NoApiKeyContent()
                }
                is RankingState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        RankingTable(
                            result = currentState.result,
                            onItemClick = { item ->
                                viewModel.onStockClick(item)
                                onStockClick()
                            }
                        )
                    }
                }
                is RankingState.Error -> {
                    ErrorContent(
                        message = currentState.message,
                        onRetry = viewModel::refresh
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingTypeSelector(
    selectedType: RankingType,
    onTypeSelected: (RankingType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedType.displayName)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            RankingType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MarketTypeTabs(
    selectedMarket: MarketType,
    onMarketSelected: (MarketType) -> Unit
) {
    val markets = listOf(MarketType.KOSPI, MarketType.KOSDAQ)
    val selectedIndex = markets.indexOf(selectedMarket).coerceAtLeast(0)

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.fillMaxWidth()
    ) {
        markets.forEachIndexed { index, market ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onMarketSelected(market) },
                text = { Text(market.displayName) }
            )
        }
    }
}

@Composable
private fun ExchangeTypeTabs(
    selectedExchange: ExchangeType,
    availableExchanges: List<ExchangeType>,
    onExchangeSelected: (ExchangeType) -> Unit
) {
    val selectedIndex = availableExchanges.indexOf(selectedExchange).coerceAtLeast(0)

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        availableExchanges.forEachIndexed { index, exchange ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onExchangeSelected(exchange) },
                text = { Text(exchange.displayName) }
            )
        }
    }
}

@Composable
private fun ItemCountSelector(
    selectedCount: ItemCount,
    onCountSelected: (ItemCount) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ItemCount.entries.forEach { count ->
            FilterChip(
                selected = selectedCount == count,
                onClick = { onCountSelected(count) },
                label = { Text("${count.value}개") }
            )
        }
    }
}

@Composable
private fun RankingTable(
    result: RankingResult,
    onItemClick: (RankingItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        item {
            RankingTableHeader(result.rankingType)
        }

        // Items
        itemsIndexed(result.items) { index, item ->
            RankingTableRow(
                item = item,
                rankingType = result.rankingType,
                onClick = { onItemClick(item) }
            )
            if (index < result.items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // Empty state
        if (result.items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "데이터가 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingTableHeader(rankingType: RankingType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "순위",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = "종목",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "현재가",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = getTypeSpecificHeader(rankingType),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
    }
}

private fun getTypeSpecificHeader(rankingType: RankingType): String {
    return when (rankingType) {
        RankingType.ORDER_BOOK_SURGE_BUY,
        RankingType.ORDER_BOOK_SURGE_SELL -> "급증률"
        RankingType.VOLUME_SURGE -> "급증률"
        RankingType.DAILY_VOLUME_TOP -> "거래량"
        RankingType.CREDIT_RATIO_TOP -> "신용비율"
        RankingType.FOREIGN_INSTITUTION_TOP -> "외인순매수"
    }
}

@Composable
private fun RankingTableRow(
    item: RankingItem,
    rankingType: RankingType,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val priceColor = when (item.priceChangeSign) {
        "+" -> extendedColors.statusUp
        "-" -> extendedColors.statusDown
        else -> extendedColors.statusNeutral
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Text(
            text = "${item.rank}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )

        // Stock info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.ticker,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Price & change
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(80.dp)
        ) {
            Text(
                text = formatPrice(item.currentPrice),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatChange(item.priceChange, item.changeRate, item.priceChangeSign),
                style = MaterialTheme.typography.bodySmall,
                color = priceColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Type-specific column
        Text(
            text = formatTypeSpecificValue(item, rankingType),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
    }
}

private fun formatTypeSpecificValue(item: RankingItem, rankingType: RankingType): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    return when (rankingType) {
        RankingType.ORDER_BOOK_SURGE_BUY,
        RankingType.ORDER_BOOK_SURGE_SELL -> {
            item.surgeRate?.let { "%.1f%%".format(it) } ?: "-"
        }
        RankingType.VOLUME_SURGE -> {
            item.surgeRate?.let { "%.1f%%".format(it) } ?: "-"
        }
        RankingType.DAILY_VOLUME_TOP -> {
            item.volume?.let { formatVolume(it) } ?: "-"
        }
        RankingType.CREDIT_RATIO_TOP -> {
            item.creditRatio?.let { "%.2f%%".format(it) } ?: "-"
        }
        RankingType.FOREIGN_INSTITUTION_TOP -> {
            item.foreignNetBuy?.let { formatAmount(it) } ?: "-"
        }
    }
}

private fun formatPrice(price: Long): String {
    if (price == 0L) return "-"
    return NumberFormat.getNumberInstance(Locale.KOREA).format(price)
}

private fun formatChange(change: Long, rate: Double, sign: String): String {
    if (change == 0L && rate == 0.0) return "-"
    val signStr = if (sign == "+") "+" else if (sign == "-") "" else ""
    return "$signStr${NumberFormat.getNumberInstance(Locale.KOREA).format(change)} (${String.format("%.2f", rate)}%)"
}

private fun formatVolume(volume: Long): String {
    return when {
        volume >= 100_000_000 -> String.format("%.1f억", volume / 100_000_000.0)
        volume >= 10_000 -> String.format("%.1f만", volume / 10_000.0)
        else -> NumberFormat.getNumberInstance(Locale.KOREA).format(volume)
    }
}

private fun formatAmount(amount: Long): String {
    return when {
        amount >= 100_000_000 -> String.format("%+.0f억", amount / 100_000_000.0)
        amount >= 10_000 -> String.format("%+.0f만", amount / 10_000.0)
        amount <= -100_000_000 -> String.format("%.0f억", amount / 100_000_000.0)
        amount <= -10_000 -> String.format("%.0f만", amount / 10_000.0)
        else -> NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
    }
}

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
private fun NoApiKeyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "API 키가 설정되지 않았습니다",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "설정 화면에서 API 키를 입력해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "오류 발생",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onRetry) {
                    Text("다시 시도")
                }
            }
        }
    }
}
