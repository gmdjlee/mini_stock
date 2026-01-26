package com.stockapp.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing system for StockApp.
 * Based on DESIGN_SYSTEM_SPEC.md v1.0.0 (4dp baseline grid)
 *
 * Usage:
 * ```
 * .padding(MaterialTheme.spacing.md)
 * .padding(MaterialTheme.spacing.medium)  // Alias
 * .height(MaterialTheme.spacing.lg)
 * ```
 */
@Immutable
data class Spacing(
    // ==========================================================================
    // Primary Spacing Values (shortened names)
    // ==========================================================================

    /** 0.dp - No spacing */
    val none: Dp = 0.dp,
    /** 4.dp - Extra small spacing (1x baseline, icons, badges, compact elements) */
    val xs: Dp = 4.dp,
    /** 8.dp - Small spacing (2x baseline, text gaps, inline spacing) */
    val sm: Dp = 8.dp,
    /** 16.dp - Medium spacing (4x baseline, standard padding, card content) */
    val md: Dp = 16.dp,
    /** 24.dp - Large spacing (6x baseline, section spacing, large gaps) */
    val lg: Dp = 24.dp,
    /** 32.dp - Extra large spacing (8x baseline, screen margins, major sections) */
    val xl: Dp = 32.dp,
    /** 48.dp - Extra extra large spacing (12x baseline, hero sections, major separators) */
    val xxl: Dp = 48.dp
) {
    // ==========================================================================
    // Alias Properties (for spec compatibility)
    // ==========================================================================

    /** 4.dp - Alias for xs */
    val extraSmall: Dp get() = xs
    /** 8.dp - Alias for sm */
    val small: Dp get() = sm
    /** 16.dp - Alias for md */
    val medium: Dp get() = md
    /** 24.dp - Alias for lg */
    val large: Dp get() = lg
    /** 32.dp - Alias for xl */
    val extraLarge: Dp get() = xl
    /** 48.dp - Alias for xxl */
    val extraExtraLarge: Dp get() = xxl
}

/**
 * CompositionLocal for accessing Spacing throughout the app.
 */
val LocalSpacing = compositionLocalOf { Spacing() }
