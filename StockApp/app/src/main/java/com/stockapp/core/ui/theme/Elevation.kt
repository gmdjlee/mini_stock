package com.stockapp.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation system for StockApp with depth hierarchy for visual layering.
 */
@Immutable
data class Elevation(
    val level0: Dp = 0.dp,   // Flat surfaces
    val level1: Dp = 1.dp,   // Cards at rest, list items, chips
    val level2: Dp = 3.dp,   // Elevated cards, hover states
    val level3: Dp = 6.dp,   // Dialogs, pickers, modals
    val level4: Dp = 8.dp,   // Navigation drawers, bottom sheets
    val level5: Dp = 12.dp   // App bars
)

/**
 * CompositionLocal for accessing Elevation throughout the app.
 */
val LocalElevation = compositionLocalOf { Elevation() }
