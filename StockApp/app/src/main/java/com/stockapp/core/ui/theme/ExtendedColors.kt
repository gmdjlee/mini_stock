package com.stockapp.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended color system for StockApp.
 * Based on EtfMonitor_Rel design system.
 *
 * Provides semantic colors for:
 * - Status colors (Korean market: Red=Up, Blue=Down)
 * - Chart colors (Moss Green Nature theme)
 * - Semantic colors (Success, Warning, Info)
 *
 * Usage:
 * ```
 * MaterialTheme.extendedColors.statusUp
 * MaterialTheme.extendedColors.success
 * ```
 */
@Immutable
data class ExtendedColors(
    // Status Colors (Korean market convention: Red=Up, Blue=Down)
    /** Price increase / Bullish (Red in Korean market) */
    val statusUp: Color = Color(0xFFF44336),
    /** Price decrease / Bearish (Blue in Korean market) */
    val statusDown: Color = Color(0xFF2196F3),
    /** No change / Neutral */
    val statusNeutral: Color = Color(0xFF9E9E9E),

    // Chart Colors (EtfMonitor Moss Green Nature theme)
    /** Main chart line - Moss green */
    val chartPrimary: Color = Color(0xFF4C6C43),
    /** Secondary chart line - Teal */
    val chartSecondary: Color = Color(0xFF396663),
    /** Tertiary chart line - Olive */
    val chartTertiary: Color = Color(0xFF586249),
    /** Positive/Bullish indicator - Teal green */
    val chartGreen: Color = Color(0xFF2E7D5A),
    /** Negative/Bearish indicator - Error red */
    val chartRed: Color = Color(0xFFBA1A1A),
    /** Neutral indicator - Teal */
    val chartBlue: Color = Color(0xFF396663),

    // Semantic Colors
    /** Success state */
    val success: Color = Color(0xFF4CAF50),
    /** Success state container */
    val successContainer: Color = Color(0xFFD4EDDA),
    /** Content on success container */
    val onSuccessContainer: Color = Color(0xFF155724),
    /** Warning state */
    val warning: Color = Color(0xFFFF9800),
    /** Warning state container */
    val warningContainer: Color = Color(0xFFFFF3CD),
    /** Content on warning container */
    val onWarningContainer: Color = Color(0xFF856404),
    /** Info state */
    val info: Color = Color(0xFF2196F3),
    /** Info state container */
    val infoContainer: Color = Color(0xFFCCE5FF),
    /** Content on info container */
    val onInfoContainer: Color = Color(0xFF004085),

    // Signal Colors (Trading signals)
    /** Strong buy signal */
    val signalStrongBuy: Color = Color(0xFFF44336),
    /** Weak buy signal */
    val signalBuy: Color = Color(0xFFFF8A80),
    /** Strong sell signal */
    val signalStrongSell: Color = Color(0xFF2196F3),
    /** Weak sell signal */
    val signalSell: Color = Color(0xFF82B1FF),
    /** Neutral signal */
    val signalNeutral: Color = Color(0xFF9E9E9E),

    // Elder Impulse Colors
    /** Elder Impulse - Bullish (Green) */
    val elderGreen: Color = Color(0xFF4CAF50),
    /** Elder Impulse - Bearish (Red) */
    val elderRed: Color = Color(0xFFF44336),
    /** Elder Impulse - Neutral (Blue) */
    val elderBlue: Color = Color(0xFF2196F3),

    // Danger Colors (for production mode warnings)
    /** Danger/Critical state */
    val danger: Color = Color(0xFFC62828),
    /** Danger state container */
    val dangerContainer: Color = Color(0xFFFFF3E0),
    /** Content on danger container */
    val onDangerContainer: Color = Color(0xFFE65100),

    // Chart UI Colors (theme-aware)
    /** Chart text/label color */
    val chartOnSurface: Color = Color.Black,
    /** Chart grid color */
    val chartGrid: Color = Color(0xFFE1E4D5),

    // Content Colors
    /** Content on primary color backgrounds */
    val onPrimary: Color = Color.White,

    // Highlight Colors
    /** Active/highlight state (e.g., DeMark active setup) */
    val activeHighlight: Color = Color(0xFFFFEB3B)
)

/**
 * Dark theme extended colors.
 */
val DarkExtendedColors = ExtendedColors(
    // Status Colors
    statusUp = Color(0xFFFF6B6B),
    statusDown = Color(0xFF64B5F6),
    statusNeutral = Color(0xFFBDBDBD),

    // Chart Colors (darker variants)
    chartPrimary = Color(0xFF7A9A6E),
    chartSecondary = Color(0xFF5D9994),
    chartTertiary = Color(0xFF8A9472),
    chartGreen = Color(0xFF4CAF88),
    chartRed = Color(0xFFE57373),
    chartBlue = Color(0xFF5D9994),

    // Semantic Colors (darker variants)
    success = Color(0xFF66BB6A),
    successContainer = Color(0xFF1B5E20),
    onSuccessContainer = Color(0xFFA5D6A7),
    warning = Color(0xFFFFB74D),
    warningContainer = Color(0xFFE65100),
    onWarningContainer = Color(0xFFFFE0B2),
    info = Color(0xFF64B5F6),
    infoContainer = Color(0xFF0D47A1),
    onInfoContainer = Color(0xFFBBDEFB),

    // Signal Colors (brighter for dark theme)
    signalStrongBuy = Color(0xFFFF6B6B),
    signalBuy = Color(0xFFFF8A80),
    signalStrongSell = Color(0xFF64B5F6),
    signalSell = Color(0xFF90CAF9),
    signalNeutral = Color(0xFFBDBDBD),

    // Elder Impulse Colors (brighter for dark theme)
    elderGreen = Color(0xFF66BB6A),
    elderRed = Color(0xFFEF5350),
    elderBlue = Color(0xFF64B5F6),

    // Danger Colors (brighter for dark theme)
    danger = Color(0xFFEF5350),
    dangerContainer = Color(0xFF4A1C1C),
    onDangerContainer = Color(0xFFFFAB91),

    // Chart UI Colors (inverted for dark theme)
    chartOnSurface = Color.White,
    chartGrid = Color(0xFF353733),

    // Content Colors (same for dark theme)
    onPrimary = Color.White,

    // Highlight Colors (brighter for dark theme)
    activeHighlight = Color(0xFFFFEE58)
)

/**
 * Light theme extended colors.
 */
val LightExtendedColors = ExtendedColors()

/**
 * CompositionLocal for accessing ExtendedColors throughout the app.
 */
val LocalExtendedColors = compositionLocalOf { ExtendedColors() }
