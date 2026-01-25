package com.stockapp.feature.etf.ui.tabs.statistics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.theme.LocalExtendedColors
import com.stockapp.feature.etf.domain.model.StockChangeType
import com.stockapp.feature.etf.domain.usecase.EnhancedStockChange
import com.stockapp.feature.etf.ui.ChangesState

/**
 * Filtered changes tab - displays only a specific type of stock changes.
 * Based on StockChangesTab.kt but filtered by sub-tab type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredChangesTab(
    changesState: ChangesState,
    filterType: StatisticsSubTab,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onItemClick: (EnhancedStockChange) -> Unit,
    onItemLongClick: (EnhancedStockChange) -> Unit,
    modifier: Modifier = Modifier
) {
    when (changesState) {
        is ChangesState.Loading -> StatisticsLoadingContent()
        is ChangesState.NoData -> StatisticsEmptyContent(
            title = "데이터가 없습니다",
            description = "최소 2일 이상의 수집 데이터가 필요합니다."
        )
        is ChangesState.Error -> StatisticsErrorContent(message = changesState.message)
        is ChangesState.Success -> {
            val filteredItems = getFilteredItems(changesState.result, filterType)

            if (filteredItems.isEmpty()) {
                StatisticsEmptyContent(
                    title = "${filterType.title} 종목 없음",
                    description = "해당 기간에 ${filterType.description}이 없습니다.",
                    icon = filterType.icon
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = modifier.fillMaxSize()
                ) {
                    FilteredChangesContent(
                        items = filteredItems,
                        filterType = filterType,
                        currentDate = changesState.result.date,
                        previousDate = changesState.result.previousDate,
                        onItemClick = onItemClick,
                        onItemLongClick = onItemLongClick
                    )
                }
            }
        }
    }
}

private fun getFilteredItems(
    result: com.stockapp.feature.etf.domain.usecase.StockChangesResult,
    filterType: StatisticsSubTab
): List<EnhancedStockChange> {
    return when (filterType) {
        StatisticsSubTab.NEWLY_INCLUDED -> result.newlyIncluded
        StatisticsSubTab.REMOVED -> result.removed
        StatisticsSubTab.WEIGHT_INCREASED -> result.weightIncreased
        StatisticsSubTab.WEIGHT_DECREASED -> result.weightDecreased
        else -> emptyList()
    }
}

@Composable
private fun FilteredChangesContent(
    items: List<EnhancedStockChange>,
    filterType: StatisticsSubTab,
    currentDate: String,
    previousDate: String,
    onItemClick: (EnhancedStockChange) -> Unit,
    onItemLongClick: (EnhancedStockChange) -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val (icon, iconColor, titleText) = getFilterConfig(filterType, extendedColors)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Header card
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "비교: $previousDate → $currentDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "총 ${items.size}개 종목",
                            style = MaterialTheme.typography.bodySmall,
                            color = iconColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Items
        items(
            items = items,
            key = { it.stockCode }
        ) { item ->
            FilteredChangeItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun getFilterConfig(
    filterType: StatisticsSubTab,
    extendedColors: com.stockapp.core.ui.theme.ExtendedColors
): Triple<ImageVector, Color, String> {
    return when (filterType) {
        StatisticsSubTab.NEWLY_INCLUDED -> Triple(
            Icons.Default.Add,
            extendedColors.success,
            "신규 편입 종목"
        )
        StatisticsSubTab.REMOVED -> Triple(
            Icons.Default.Remove,
            MaterialTheme.colorScheme.error,
            "편출 종목"
        )
        StatisticsSubTab.WEIGHT_INCREASED -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            extendedColors.danger,
            "비중 증가 종목"
        )
        StatisticsSubTab.WEIGHT_DECREASED -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            extendedColors.info,
            "비중 감소 종목"
        )
        else -> Triple(
            Icons.Default.Add,
            MaterialTheme.colorScheme.primary,
            "종목"
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilteredChangeItemCard(
    item: EnhancedStockChange,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
            Icons.AutoMirrored.Filled.TrendingUp,
            extendedColors.danger,
            "비중증가"
        )
        StockChangeType.WEIGHT_DECREASED -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
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

