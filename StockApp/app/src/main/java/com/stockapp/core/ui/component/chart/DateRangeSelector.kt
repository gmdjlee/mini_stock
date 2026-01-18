package com.stockapp.core.ui.component.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.theme.ChartPrimary

/**
 * DateRangeOption - Available date range selections
 * Days are in business days (trading days)
 */
enum class DateRangeOption(val label: String, val days: Int) {
    WEEK("1주", 7),
    MONTH("1개월", 30),
    THREE_MONTHS("3개월", 90),
    SIX_MONTHS("6개월", 180),
    YEAR("1년", 365),
    THREE_YEARS("3년", 1095),
    FIVE_YEARS("5년", 1825),
    SEVEN_YEARS("7년", 2555),
    ALL("전체", -1);

    companion object {
        /**
         * Get DateRangeOption from days value
         */
        fun fromDays(days: Int): DateRangeOption {
            return entries.find { it.days == days } ?: YEAR
        }
    }
}

/**
 * DateRangeSelector - Horizontal scrollable date range selector
 * EtfMonitor style implementation
 */
@Composable
fun DateRangeSelector(
    selectedRange: DateRangeOption,
    onRangeSelected: (DateRangeOption) -> Unit,
    modifier: Modifier = Modifier,
    availableOptions: List<DateRangeOption> = listOf(
        DateRangeOption.MONTH,
        DateRangeOption.THREE_MONTHS,
        DateRangeOption.SIX_MONTHS,
        DateRangeOption.YEAR,
        DateRangeOption.THREE_YEARS
    )
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableOptions.forEach { option ->
            DateRangeChip(
                label = option.label,
                isSelected = option == selectedRange,
                onClick = { onRangeSelected(option) }
            )
        }
    }
}

/**
 * DateRangeChip - Individual selectable chip for date range
 */
@Composable
private fun DateRangeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) ChartPrimary else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) ChartPrimary else Color.Transparent
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

/**
 * Compact DateRangeSelector for limited space
 * Shows only essential options
 */
@Composable
fun CompactDateRangeSelector(
    selectedRange: DateRangeOption,
    onRangeSelected: (DateRangeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    DateRangeSelector(
        selectedRange = selectedRange,
        onRangeSelected = onRangeSelected,
        modifier = modifier,
        availableOptions = listOf(
            DateRangeOption.MONTH,
            DateRangeOption.THREE_MONTHS,
            DateRangeOption.SIX_MONTHS,
            DateRangeOption.YEAR
        )
    )
}

/**
 * Full DateRangeSelector with all options
 */
@Composable
fun FullDateRangeSelector(
    selectedRange: DateRangeOption,
    onRangeSelected: (DateRangeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    DateRangeSelector(
        selectedRange = selectedRange,
        onRangeSelected = onRangeSelected,
        modifier = modifier,
        availableOptions = DateRangeOption.entries
    )
}
