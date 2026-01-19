package com.stockapp.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing system for consistent spacing throughout the app.
 * Based on EtfMonitor_Rel design system.
 *
 * Usage:
 * ```
 * .padding(MaterialTheme.spacing.md)
 * .height(MaterialTheme.spacing.lg)
 * ```
 */
data class Spacing(
    /** 0.dp - No spacing */
    val none: Dp = 0.dp,
    /** 4.dp - Extra small spacing (compact elements, tight gaps) */
    val xs: Dp = 4.dp,
    /** 8.dp - Small spacing (text line gaps, icon padding) */
    val sm: Dp = 8.dp,
    /** 16.dp - Medium spacing (standard padding, card content) */
    val md: Dp = 16.dp,
    /** 24.dp - Large spacing (section dividers, card gaps) */
    val lg: Dp = 24.dp,
    /** 32.dp - Extra large spacing (screen margins, major sections) */
    val xl: Dp = 32.dp,
    /** 48.dp - Extra extra large spacing (major dividers, hero sections) */
    val xxl: Dp = 48.dp
)

/**
 * CompositionLocal for accessing Spacing throughout the app.
 */
val LocalSpacing = compositionLocalOf { Spacing() }
