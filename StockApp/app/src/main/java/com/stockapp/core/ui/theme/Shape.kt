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
 * Extended shape system for StockApp.
 * Provides specialized shapes beyond Material Design 3 defaults.
 *
 * Usage:
 * ```
 * Card(shape = MaterialTheme.extendedShapes.card)
 * Button(shape = MaterialTheme.extendedShapes.button)
 * ```
 */
@Immutable
data class ExtendedShapes(
    // Card Shapes
    /** Standard card corners - 32dp */
    val card: Shape = RoundedCornerShape(32.dp),
    /** Large cards - 32dp */
    val cardLarge: Shape = RoundedCornerShape(32.dp),
    /** Medium cards - 24dp */
    val cardMedium: Shape = RoundedCornerShape(24.dp),
    /** Small cards - 16dp */
    val cardSmall: Shape = RoundedCornerShape(16.dp),

    // Button Shapes
    /** Pill-shaped buttons - 100dp */
    val button: Shape = RoundedCornerShape(100.dp),
    /** Outlined buttons - 100dp */
    val buttonOutlined: Shape = RoundedCornerShape(100.dp),
    /** Large buttons - 100dp */
    val buttonLarge: Shape = RoundedCornerShape(100.dp),

    // Dialog & Sheet Shapes
    /** Dialog boxes - 28dp */
    val dialog: Shape = RoundedCornerShape(28.dp),
    /** Bottom sheet (top corners only) - 32dp */
    val bottomSheet: Shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),

    // Chip Shapes
    /** Pill-shaped chips - 100dp */
    val chip: Shape = RoundedCornerShape(100.dp),
    /** Filter chips - 100dp */
    val filterChip: Shape = RoundedCornerShape(100.dp),
    /** Status chips - 8dp */
    val statusChip: Shape = RoundedCornerShape(8.dp),

    // Specialized Shapes
    /** Floating action buttons - 16dp */
    val fab: Shape = RoundedCornerShape(16.dp),
    /** Extended FAB - 100dp */
    val fabExtended: Shape = RoundedCornerShape(100.dp),
    /** Search bars (pill) - 100dp */
    val searchBar: Shape = RoundedCornerShape(100.dp),
    /** Status badges - 8dp */
    val badge: Shape = RoundedCornerShape(8.dp),
    /** List item backgrounds - 16dp */
    val listItem: Shape = RoundedCornerShape(16.dp),
    /** AI insights card - 32dp */
    val aiInsightsCard: Shape = RoundedCornerShape(32.dp),
    /** Icon containers - 16dp */
    val iconContainer: Shape = RoundedCornerShape(16.dp),
    /** Circular elements */
    val circle: Shape = CircleShape,

    // Input Shapes
    /** Text fields - 8dp */
    val textField: Shape = RoundedCornerShape(8.dp),
    /** Text field outlined - 8dp */
    val textFieldOutlined: Shape = RoundedCornerShape(8.dp)
)

/**
 * CompositionLocal for accessing ExtendedShapes throughout the app.
 */
val LocalExtendedShapes = compositionLocalOf { ExtendedShapes() }
