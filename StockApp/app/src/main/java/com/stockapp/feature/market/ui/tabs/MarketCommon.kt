package com.stockapp.feature.market.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stockapp.feature.market.domain.model.MarketDateRange

@Composable
fun DateRangeSelector(
    selectedRange: MarketDateRange,
    onRangeSelected: (MarketDateRange) -> Unit,
    allowedRanges: List<MarketDateRange> = MarketDateRange.DEFAULT_RANGES
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allowedRanges.forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) }
            )
        }
    }
}
