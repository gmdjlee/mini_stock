package com.stockapp.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation system for StockApp.
 * Based on DESIGN_SYSTEM_SPEC.md v1.0.0
 *
 * Provides a depth hierarchy for clear visual layering.
 *
 * Usage:
 * ```
 * ElevatedCard(
 *     elevation = CardDefaults.elevatedCardElevation(
 *         defaultElevation = MaterialTheme.elevation.level2
 *     )
 * )
 * ```
 */
@Immutable
data class Elevation(
    /** 0dp - Flat surfaces, text, basic content */
    val level0: Dp = 0.dp,
    /** 1dp - Cards at rest, list items, chips */
    val level1: Dp = 1.dp,
    /** 3dp - Elevated cards, hover states (most common) */
    val level2: Dp = 3.dp,
    /** 6dp - Dialogs, pickers, modal content */
    val level3: Dp = 6.dp,
    /** 8dp - Navigation drawers, bottom sheets */
    val level4: Dp = 8.dp,
    /** 12dp - App bars (top/bottom) */
    val level5: Dp = 12.dp
)

/**
 * CompositionLocal for accessing Elevation throughout the app.
 */
val LocalElevation = compositionLocalOf { Elevation() }
