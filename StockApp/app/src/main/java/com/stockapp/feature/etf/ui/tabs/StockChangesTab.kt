package com.stockapp.feature.etf.ui.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.theme.LocalExtendedColors
import com.stockapp.feature.etf.domain.model.StockChangeType
import com.stockapp.feature.etf.domain.usecase.EnhancedStockChange
import com.stockapp.feature.etf.domain.usecase.StockChangesResult
import com.stockapp.feature.etf.ui.ChangesState
import com.stockapp.feature.etf.ui.EtfVm
import java.text.NumberFormat
import java.util.Locale

/**
 * Filter type for changes tab.
 */
private enum class ChangeFilter(
    val title: String,
    val icon: ImageVector,
    val changeType: StockChangeType?
) {
    ALL("전체", Icons.Default.SwapVert, null),
    NEWLY_INCLUDED("신규편입", Icons.Default.Add, StockChangeType.NEWLY_INCLUDED),
    REMOVED("편출", Icons.Default.Remove, StockChangeType.REMOVED),
    WEIGHT_INCREASED("비중증가", Icons.Default.TrendingUp, StockChangeType.WEIGHT_INCREASED),
    WEIGHT_DECREASED("비중감소", Icons.Default.TrendingDown, StockChangeType.WEIGHT_DECREASED)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockChangesTab(
    viewModel: EtfVm,
    onStockClick: () -> Unit
) {
    val changesState by viewModel.changesState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedFilter by remember { mutableStateOf(ChangeFilter.ALL) }

    when (val state = changesState) {
        is ChangesState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is ChangesState.NoData -> {
            NoDataContent()
        }

        is ChangesState.Error -> {
            ErrorContent(message = state.message)
        }

        is ChangesState.Success -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshChanges() },
                modifier = Modifier.fillMaxSize()
            ) {
                ChangesContent(
                    result = state.result,
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    onItemClick = { item ->
                        viewModel.showStockDetail(item.stockCode, item.stockName)
                    },
                    onItemLongClick = { item ->
                        viewModel.onRankingItemClick(
                            com.stockapp.feature.etf.domain.usecase.EnhancedStockRanking(
                                rank = item.rank,
                                stockCode = item.stockCode,
                                stockName = item.stockName,
                                totalAmount = item.totalAmount,
                                etfCount = item.etfNames.size,
                                previousAmount = null,
                                amountChange = null,
                                isNew = false
                            )
                        )
                        onStockClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ChangesContent(
    result: StockChangesResult,
    selectedFilter: ChangeFilter,
    onFilterChange: (ChangeFilter) -> Unit,
    onItemClick: (EnhancedStockChange) -> Unit,
    onItemLongClick: (EnhancedStockChange) -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    // Get filtered items
    val filteredItems = when (selectedFilter) {
        ChangeFilter.ALL -> {
            buildList {
                addAll(result.newlyIncluded)
                addAll(result.removed)
                addAll(result.weightIncreased)
                addAll(result.weightDecreased)
            }.sortedByDescending { it.totalAmount }
        }
        ChangeFilter.NEWLY_INCLUDED -> result.newlyIncluded
        ChangeFilter.REMOVED -> result.removed
        ChangeFilter.WEIGHT_INCREASED -> result.weightIncreased
        ChangeFilter.WEIGHT_DECREASED -> result.weightDecreased
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ETF 변동종목",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "비교: ${result.previousDate} → ${result.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Summary badges
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryBadge(
                    label = "신규편입",
                    count = result.newlyIncluded.size,
                    color = extendedColors.success
                )
                SummaryBadge(
                    label = "편출",
                    count = result.removed.size,
                    color = MaterialTheme.colorScheme.error
                )
                SummaryBadge(
                    label = "비중증가",
                    count = result.weightIncreased.size,
                    color = extendedColors.danger
                )
                SummaryBadge(
                    label = "비중감소",
                    count = result.weightDecreased.size,
                    color = extendedColors.info
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChangeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(filter.title)
                            }
                        }
                    )
                }
            }
        }

        // Items
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "해당 조건의 변동종목이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredItems) { item ->
                ChangeItemCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryBadge(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChangeItemCard(
    item: EnhancedStockChange,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val extendedColors = LocalExtendedColors.current

    val (icon, iconColor, typeLabel) = when (item.changeType) {
        StockChangeType.NEWLY_INCLUDED -> Triple(
            Icons.Default.Add,
            extendedColors.success,
            "신규편입"
        )
        StockChangeType.REMOVED -> Triple(
            Icons.Default.Remove,
            MaterialTheme.colorScheme.error,
            "편출"
        )
        StockChangeType.WEIGHT_INCREASED -> Triple(
            Icons.Default.TrendingUp,
            extendedColors.danger,
            "비중증가"
        )
        StockChangeType.WEIGHT_DECREASED -> Triple(
            Icons.Default.TrendingDown,
            extendedColors.info,
            "비중감소"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Stock info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.stockName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = iconColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.stockCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.etfNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "관련 ETF: ${item.etfNames.take(3).joinToString(", ")}" +
                            if (item.etfNames.size > 3) " 외 ${item.etfNames.size - 3}개" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(item.totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.etfNames.size}개 ETF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                    text = "변동종목이 없습니다",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "최소 2일 이상의 수집 데이터가 필요합니다.",
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

private fun formatAmount(amount: Long): String {
    return when {
        amount >= 1_000_000_000_000 -> String.format("%.1f조", amount / 1_000_000_000_000.0)
        amount >= 100_000_000 -> String.format("%.0f억", amount / 100_000_000.0)
        amount >= 10_000 -> String.format("%.0f만", amount / 10_000.0)
        else -> NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
    }
}
