package com.stockapp.feature.etf.ui.tabs.statistics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockapp.feature.etf.domain.model.DateRangeOption

/**
 * DateRangeSelectorBar using Material3 FilterChip.
 * Following the pattern from StockChangesTab.kt but using
 * DateRangeOption from EtfModels.kt
 */
@Composable
fun DateRangeSelectorBar(
    selectedRange: DateRangeOption,
    onRangeSelected: (DateRangeOption) -> Unit,
    modifier: Modifier = Modifier,
    availableOptions: List<DateRangeOption> = listOf(
        DateRangeOption.DAY,
        DateRangeOption.WEEK,
        DateRangeOption.MONTH,
        DateRangeOption.THREE_MONTHS,
        DateRangeOption.SIX_MONTHS
    ),
    showDateInfo: Boolean = false,
    currentDate: String? = null,
    previousDate: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableOptions.forEach { option ->
                FilterChip(
                    selected = selectedRange == option,
                    onClick = { onRangeSelected(option) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Optional date info display
        if (showDateInfo && currentDate != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (previousDate != null) {
                        "$previousDate ~ $currentDate"
                    } else {
                        "기준일: $currentDate"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Compact version without date info.
 */
@Composable
fun CompactDateRangeSelectorBar(
    selectedRange: DateRangeOption,
    onRangeSelected: (DateRangeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    DateRangeSelectorBar(
        selectedRange = selectedRange,
        onRangeSelected = onRangeSelected,
        modifier = modifier,
        availableOptions = listOf(
            DateRangeOption.WEEK,
            DateRangeOption.MONTH,
            DateRangeOption.THREE_MONTHS
        ),
        showDateInfo = false
    )
}

/**
 * Full version with all options and date info.
 */
@Composable
fun FullDateRangeSelectorBar(
    selectedRange: DateRangeOption,
    onRangeSelected: (DateRangeOption) -> Unit,
    currentDate: String?,
    previousDate: String?,
    modifier: Modifier = Modifier
) {
    DateRangeSelectorBar(
        selectedRange = selectedRange,
        onRangeSelected = onRangeSelected,
        modifier = modifier,
        availableOptions = DateRangeOption.entries,
        showDateInfo = true,
        currentDate = currentDate,
        previousDate = previousDate
    )
}
