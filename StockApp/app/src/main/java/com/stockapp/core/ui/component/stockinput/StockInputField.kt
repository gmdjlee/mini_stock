package com.stockapp.core.ui.component.stockinput

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.component.stockinput.state.StockInputState
import com.stockapp.feature.search.domain.model.Market
import com.stockapp.feature.search.domain.model.Stock

/**
 * 자동완성 기능이 포함된 종목 입력 컴포넌트 (Stateless)
 */
@Composable
fun StockInputField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<Stock>,
    onSelect: (Stock) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    placeholder: String = "종목명 또는 코드 검색",
    history: List<Stock> = emptyList(),
    onHistorySelect: ((Stock) -> Unit)? = null,
    onHistoryClick: (() -> Unit)? = null,
    colors: StockInputColors = StockInputDefaults.colors(),
    shape: Shape = StockInputDefaults.shape
) {
    var showHistoryDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            // TextField
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = colors.placeholderColor
                    )
                },
                leadingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "검색",
                            tint = colors.iconColor
                        )
                    }
                },
                trailingIcon = {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "지우기",
                                tint = colors.iconColor
                            )
                        }
                    } else if (history.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (onHistoryClick != null) {
                                    onHistoryClick()
                                } else {
                                    showHistoryDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "검색 히스토리",
                                tint = colors.iconColor
                            )
                        }
                    }
                },
                singleLine = true,
                shape = shape
            )

            // Dropdown suggestions
            if (suggestions.isNotEmpty() && value.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suggestions_dropdown"),
                    elevation = CardDefaults.cardElevation(defaultElevation = colors.dropdownElevation),
                    colors = CardDefaults.cardColors(containerColor = colors.dropdownContainerColor)
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(suggestions, key = { it.ticker }) { stock ->
                            SuggestionItem(
                                stock = stock,
                                onClick = { onSelect(stock) }
                            )
                            if (stock != suggestions.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // History dialog
    if (showHistoryDialog) {
        StockInputHistoryDialog(
            history = history,
            onDismiss = { showHistoryDialog = false },
            onSelect = { stock ->
                showHistoryDialog = false
                if (onHistorySelect != null) {
                    onHistorySelect(stock)
                } else {
                    onSelect(stock)
                }
            }
        )
    }
}

/**
 * 자동완성 기능이 포함된 종목 입력 컴포넌트 (Stateful)
 */
@Composable
fun StockInputField(
    state: StockInputState,
    onSelect: (Stock) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "종목명 또는 코드 검색",
    history: List<Stock> = emptyList(),
    onHistorySelect: ((Stock) -> Unit)? = null,
    colors: StockInputColors = StockInputDefaults.colors(),
    shape: Shape = StockInputDefaults.shape
) {
    StockInputField(
        value = state.value,
        onValueChange = state::onValueChange,
        suggestions = state.suggestions,
        onSelect = { stock ->
            state.onSelect(stock)
            onSelect(stock)
        },
        modifier = modifier,
        enabled = enabled,
        isLoading = state.isLoading,
        placeholder = placeholder,
        history = history,
        onHistorySelect = onHistorySelect ?: { stock ->
            state.onSelect(stock)
            onSelect(stock)
        },
        colors = colors,
        shape = shape
    )
}

@Composable
private fun SuggestionItem(
    stock: Stock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stock.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stock.ticker,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        MarketBadge(market = stock.market)
    }
}

@Composable
private fun MarketBadge(market: Market) {
    val (text, color) = when (market) {
        Market.KOSPI -> "KOSPI" to MaterialTheme.colorScheme.primary
        Market.KOSDAQ -> "KOSDAQ" to MaterialTheme.colorScheme.secondary
        Market.OTHER -> "기타" to MaterialTheme.colorScheme.tertiary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
