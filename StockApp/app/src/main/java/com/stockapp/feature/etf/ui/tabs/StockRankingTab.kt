package com.stockapp.feature.etf.ui.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.stockapp.core.ui.theme.LocalExtendedColors
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
    onItemClick: (EnhancedStockRanking) -> Unit,
    onItemLongClick: (EnhancedStockRanking) -> Unit
) {
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
            RankingTableHeader()
            HorizontalDivider()
        }

        // Ranking items
        items(result.rankings) { item ->
            RankingRow(
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
private fun RankingTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "종목명",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "합산금액",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "ETF수",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "변동",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankingRow(
    item: EnhancedStockRanking,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val extendedColors = LocalExtendedColors.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

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
        // Rank
        Text(
            text = item.rank.toString(),
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
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End
        )

        // ETF count
        Text(
            text = "${item.etfCount}개",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp),
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
            modifier = Modifier.width(70.dp),
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
