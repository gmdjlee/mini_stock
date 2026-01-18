package com.stockapp.core.ui.component.stockinput

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * StockInputField 스타일 색상 클래스
 */
@Immutable
data class StockInputColors(
    val containerColor: Color,
    val focusedContainerColor: Color,
    val textColor: Color,
    val placeholderColor: Color,
    val iconColor: Color,
    val focusedBorderColor: Color,
    val unfocusedBorderColor: Color,
    val dropdownContainerColor: Color,
    val dropdownElevation: Dp
)

/**
 * StockInputField 기본값
 */
object StockInputDefaults {

    val shape: Shape
        @Composable get() = MaterialTheme.shapes.medium

    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        focusedContainerColor: Color = MaterialTheme.colorScheme.surface,
        textColor: Color = MaterialTheme.colorScheme.onSurface,
        placeholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor: Color = MaterialTheme.colorScheme.outline,
        dropdownContainerColor: Color = MaterialTheme.colorScheme.surface,
        dropdownElevation: Dp = 8.dp
    ): StockInputColors = StockInputColors(
        containerColor = containerColor,
        focusedContainerColor = focusedContainerColor,
        textColor = textColor,
        placeholderColor = placeholderColor,
        iconColor = iconColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        dropdownContainerColor = dropdownContainerColor,
        dropdownElevation = dropdownElevation
    )
}
