package com.stockapp.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 base shapes for StockApp.
 * Based on DESIGN_SYSTEM_SPEC.md v1.0.0
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Extended shape system for StockApp beyond Material Design 3 defaults.
 */
@Immutable
data class ExtendedShapes(
    // Cards
    val card: Shape = RoundedCornerShape(32.dp),
    val cardLarge: Shape = card,
    val cardMedium: Shape = RoundedCornerShape(24.dp),
    val cardSmall: Shape = RoundedCornerShape(16.dp),
    val aiInsightsCard: Shape = card,

    // Buttons (pill-shaped)
    val button: Shape = RoundedCornerShape(100.dp),
    val buttonOutlined: Shape = button,
    val buttonLarge: Shape = button,

    // Dialogs & Sheets
    val dialog: Shape = RoundedCornerShape(28.dp),
    val bottomSheet: Shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),

    // Chips
    val chip: Shape = RoundedCornerShape(100.dp),
    val filterChip: Shape = chip,
    val statusChip: Shape = RoundedCornerShape(8.dp),

    // FAB
    val fab: Shape = RoundedCornerShape(16.dp),
    val fabExtended: Shape = RoundedCornerShape(100.dp),

    // Other
    val searchBar: Shape = RoundedCornerShape(100.dp),
    val badge: Shape = RoundedCornerShape(8.dp),
    val listItem: Shape = RoundedCornerShape(16.dp),
    val iconContainer: Shape = RoundedCornerShape(16.dp),
    val circle: Shape = CircleShape,

    // Inputs
    val textField: Shape = RoundedCornerShape(8.dp),
    val textFieldOutlined: Shape = textField
)

/**
 * CompositionLocal for accessing ExtendedShapes throughout the app.
 */
val LocalExtendedShapes = compositionLocalOf { ExtendedShapes() }
