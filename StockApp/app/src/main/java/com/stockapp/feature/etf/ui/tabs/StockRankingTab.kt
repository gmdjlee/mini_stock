package com.stockapp.feature.etf.ui.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockapp.core.ui.theme.LocalExtendedColors
import com.stockapp.feature.etf.domain.model.RankingSortColumn
import com.stockapp.feature.etf.domain.model.RankingSortState
import com.stockapp.feature.etf.domain.model.SortCriteria
import com.stockapp.feature.etf.domain.model.SortDirection
import com.stockapp.feature.etf.domain.usecase.EnhancedStockRanking
import com.stockapp.feature.etf.ui.EtfVm
import com.stockapp.feature.etf.ui.RankingState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockRankingTab(
    viewModel: EtfVm,
    onStockClick: () -> Unit
) {
    val rankingState by viewModel.rankingState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val sortState by viewModel.rankingSortState.collectAsState()

    when (val state = rankingState) {
        is RankingState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is RankingState.NoData -> {
            NoDataContent()
        }

        is RankingState.Error -> {
            ErrorContent(message = state.message)
        }

        is RankingState.Success -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshRanking() },
                modifier = Modifier.fillMaxSize()
            ) {
                RankingContent(
                    result = state.result,
                    sortState = sortState,
                    onSortColumnClick = { column ->
                        viewModel.onRankingSortColumnClick(column)
                    },
                    onSortColumnLongClick = { column ->
                        viewModel.onRankingSortColumnLongClick(column)
                    },
                    onResetSort = {
                        viewModel.resetRankingSort()
                    },
                    onItemClick = { item ->
                        viewModel.showStockDetail(item.stockCode, item.stockName)
                    },
                    onItemLongClick = { item ->
                        viewModel.onRankingItemClick(item)
                        onStockClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun RankingContent(
    result: com.stockapp.feature.etf.domain.usecase.StockRankingResult,
    sortState: RankingSortState,
    onSortColumnClick: (RankingSortColumn) -> Unit,
    onSortColumnLongClick: (RankingSortColumn) -> Unit,
    onResetSort: () -> Unit,
    onItemClick: (EnhancedStockRanking) -> Unit,
    onItemLongClick: (EnhancedStockRanking) -> Unit
) {
    // Apply sorting to rankings
    val sortedRankings = result.rankings.applySorting(sortState)

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
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ETF 평가금액 순위",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "기준일: ${result.date}" + (result.previousDate?.let { " (비교: $it)" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Table header
        item {
            RankingTableHeader(
                sortState = sortState,
                onSortColumnClick = onSortColumnClick,
                onSortColumnLongClick = onSortColumnLongClick,
                onResetSort = onResetSort
            )
            HorizontalDivider()
        }

        // Ranking items (with display rank)
        itemsIndexed(sortedRankings) { index, item ->
            RankingRow(
                displayRank = index + 1,
                item = item,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
            HorizontalDivider()
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RankingTableHeader(
    sortState: RankingSortState,
    onSortColumnClick: (RankingSortColumn) -> Unit,
    onSortColumnLongClick: (RankingSortColumn) -> Unit,
    onResetSort: () -> Unit
) {
    Column {
        // Sort info row (shows when multi-sorting)
        if (sortState.criteria.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "정렬: ${sortState.getDisplayDescription()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onResetSort,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "초기화",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Column headers row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank column (non-sortable)
            Text(
                text = "#",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Stock name column (non-sortable)
            Text(
                text = "종목명",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Total amount column (sortable)
            SortableColumnHeader(
                text = "합산금액",
                column = RankingSortColumn.TOTAL_AMOUNT,
                sortState = sortState,
                onClick = onSortColumnClick,
                onLongClick = onSortColumnLongClick,
                modifier = Modifier.width(96.dp)
            )

            // ETF count column (sortable)
            SortableColumnHeader(
                text = "ETF수",
                column = RankingSortColumn.ETF_COUNT,
                sortState = sortState,
                onClick = onSortColumnClick,
                onLongClick = onSortColumnLongClick,
                modifier = Modifier.width(96.dp)
            )

            // Amount change column (sortable)
            SortableColumnHeader(
                text = "변동",
                column = RankingSortColumn.AMOUNT_CHANGE,
                sortState = sortState,
                onClick = onSortColumnClick,
                onLongClick = onSortColumnLongClick,
                modifier = Modifier.width(96.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SortableColumnHeader(
    text: String,
    column: RankingSortColumn,
    sortState: RankingSortState,
    onClick: (RankingSortColumn) -> Unit,
    onLongClick: (RankingSortColumn) -> Unit,
    modifier: Modifier = Modifier
) {
    val priority = sortState.getPriority(column)
    val direction = sortState.getDirection(column)
    val isActive = priority != null

    val color = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .combinedClickable(
                onClick = { onClick(column) },
                onLongClick = { onLongClick(column) }
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority badge (1, 2, 3)
        if (priority != null) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = priority.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End,
            color = color,
            modifier = Modifier.weight(1f)
        )

        if (isActive && direction != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = if (direction == SortDirection.DESCENDING) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.Default.KeyboardArrowUp
                },
                contentDescription = if (direction == SortDirection.DESCENDING) {
                    "내림차순 정렬 (우선순위 $priority)"
                } else {
                    "오름차순 정렬 (우선순위 $priority)"
                },
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankingRow(
    displayRank: Int,
    item: EnhancedStockRanking,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val extendedColors = LocalExtendedColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank (use displayRank for sorted order)
        Text(
            text = displayRank.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        // Stock name with NEW badge
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.stockName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isNew) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = "신규",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = item.stockCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Total amount
        Text(
            text = formatAmount(item.totalAmount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(96.dp),
            textAlign = TextAlign.End
        )

        // ETF count
        Text(
            text = "${item.etfCount}개",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(96.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Amount change
        val changeText = item.amountChange?.let { formatAmountChange(it) } ?: "-"
        val changeColor = when {
            item.isNew -> MaterialTheme.colorScheme.primary
            item.amountChange == null -> MaterialTheme.colorScheme.onSurfaceVariant
            item.amountChange > 0 -> extendedColors.danger
            item.amountChange < 0 -> extendedColors.info
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = if (item.isNew) "NEW" else changeText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(96.dp),
            textAlign = TextAlign.End,
            color = changeColor,
            fontWeight = if (item.isNew) FontWeight.Bold else FontWeight.Normal
        )
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
                    text = "수집된 데이터가 없습니다",
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
    return when {
        kotlin.math.abs(change) >= 1_000_000_000_000 -> String.format("%s%.1f조", sign, change / 1_000_000_000_000.0)
        kotlin.math.abs(change) >= 100_000_000 -> String.format("%s%.0f억", sign, change / 100_000_000.0)
        kotlin.math.abs(change) >= 10_000 -> String.format("%s%.0f만", sign, change / 10_000.0)
        else -> sign + NumberFormat.getNumberInstance(Locale.KOREA).format(change)
    }
}

/**
 * Apply multi-column sorting to rankings list.
 * Criteria order determines priority (first = primary sort key).
 */
private fun List<EnhancedStockRanking>.applySorting(
    sortState: RankingSortState
): List<EnhancedStockRanking> {
    if (sortState.criteria.isEmpty()) return this

    // Build chained comparator from criteria list
    val comparator = sortState.criteria.fold<SortCriteria, Comparator<EnhancedStockRanking>?>(
        null
    ) { acc, criteria ->
        val columnComparator: Comparator<EnhancedStockRanking> = when (criteria.column) {
            RankingSortColumn.TOTAL_AMOUNT -> compareBy { it.totalAmount }
            RankingSortColumn.ETF_COUNT -> compareBy { it.etfCount }
            RankingSortColumn.AMOUNT_CHANGE -> compareBy { it.amountChange ?: Long.MIN_VALUE }
        }.let { base ->
            if (criteria.direction == SortDirection.DESCENDING) {
                base.reversed()
            } else {
                base
            }
        }

        acc?.then(columnComparator) ?: columnComparator
    }

    return comparator?.let { sortedWith(it) } ?: this
}
