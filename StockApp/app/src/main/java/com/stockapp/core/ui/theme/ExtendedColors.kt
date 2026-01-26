package com.stockapp.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended color system for StockApp.
 * Based on DESIGN_SYSTEM_SPEC.md v1.0.0 (Moss Green Nature theme)
 *
 * Provides semantic colors for:
 * - Financial status colors (Korean market: Red=Up, Blue=Down)
 * - Chart colors (Moss Green Nature theme)
 * - Semantic colors (Success, Warning, Info)
 * - Shimmer effect colors
 * - AI insights colors
 * - Interactive state colors
 *
 * Usage:
 * ```
 * MaterialTheme.extendedColors.statusUp
 * MaterialTheme.extendedColors.success
 * MaterialTheme.extendedColors.chartPrimary
 * ```
 */
@Immutable
data class ExtendedColors(
    // ==========================================================================
    // Status Colors (Korean market convention: Red=Up, Blue=Down)
    // ==========================================================================

    /** Price increase / Bullish (Red in Korean market) */
    val statusUp: Color = Color(0xFFF44336),
    /** Price decrease / Bearish (Blue in Korean market) */
    val statusDown: Color = Color(0xFF2196F3),
    /** No change / Neutral */
    val statusNeutral: Color = Color(0xFF9E9E9E),

    // ==========================================================================
    // Financial Status Colors (Design System Spec)
    // ==========================================================================

    /** New holdings - Moss green */
    val statusNew: Color = Color(0xFF4C6C43),
    /** Weight increased - Teal green */
    val statusIncrease: Color = Color(0xFF2E7D5A),
    /** Weight decreased - Error red */
    val statusDecrease: Color = Color(0xFFBA1A1A),
    /** Removed holdings - Grey */
    val statusRemoved: Color = Color(0xFF8F9285),
    /** Maintained holdings - Olive */
    val statusMaintain: Color = Color(0xFF586249),

    // ==========================================================================
    // Chart Colors (Moss Green Nature theme)
    // ==========================================================================

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
    /** Accent data - Purple */
    val chartPurple: Color = Color(0xFF8E7CC3),
    /** Warning/highlight - Orange */
    val chartOrange: Color = Color(0xFFE0A050),
    /** Special highlight - Light teal */
    val chartCyan: Color = Color(0xFFA0CFCF),
    /** Special data - Dusty pink */
    val chartPink: Color = Color(0xFFD4A5A5),

    // Chart UI Colors
    /** Chart text/label color */
    val chartOnSurface: Color = Color(0xFF1B1C18),
    /** Chart grid color */
    val chartGrid: Color = Color(0xFFE1E4D5),
    /** Chart card background */
    val chartCardBackground: Color = Color(0xFFFFFFFF),

    // ==========================================================================
    // Semantic Colors
    // ==========================================================================

    /** Success state */
    val success: Color = Color(0xFF2E7D5A),
    /** Success state container */
    val successContainer: Color = Color(0xFFC4EED0),
    /** Content on success container */
    val onSuccessContainer: Color = Color(0xFF002108),
    /** Warning state */
    val warning: Color = Color(0xFFE0A050),
    /** Warning state container */
    val warningContainer: Color = Color(0xFFFFDDB0),
    /** Content on warning container */
    val onWarningContainer: Color = Color(0xFF2B1700),
    /** Info state */
    val info: Color = Color(0xFF396663),
    /** Info state container */
    val infoContainer: Color = Color(0xFFBBEBEB),
    /** Content on info container */
    val onInfoContainer: Color = Color(0xFF002020),

    // ==========================================================================
    // Signal Colors (Trading signals - Korean market)
    // ==========================================================================

    /** Strong buy signal - Red */
    val signalStrongBuy: Color = Color(0xFFF44336),
    /** Weak buy signal - Light red */
    val signalBuy: Color = Color(0xFFFF8A80),
    /** Strong sell signal - Blue */
    val signalStrongSell: Color = Color(0xFF2196F3),
    /** Weak sell signal - Light blue */
    val signalSell: Color = Color(0xFF82B1FF),
    /** Neutral signal - Grey */
    val signalNeutral: Color = Color(0xFF9E9E9E),

    // ==========================================================================
    // Elder Impulse Colors
    // ==========================================================================

    /** Elder Impulse - Bullish (Green) */
    val elderGreen: Color = Color(0xFF4CAF50),
    /** Elder Impulse - Bearish (Red) */
    val elderRed: Color = Color(0xFFF44336),
    /** Elder Impulse - Neutral (Blue) */
    val elderBlue: Color = Color(0xFF2196F3),

    // ==========================================================================
    // Danger Colors (for production mode warnings)
    // ==========================================================================

    /** Danger/Critical state */
    val danger: Color = Color(0xFFC62828),
    /** Danger state container */
    val dangerContainer: Color = Color(0xFFFFF3E0),
    /** Content on danger container */
    val onDangerContainer: Color = Color(0xFFE65100),

    // ==========================================================================
    // Surface Elevation Colors
    // ==========================================================================

    /** Surface elevation level 1 */
    val surfaceElevation1: Color = Color(0xFFF8F6EE),
    /** Surface elevation level 2 */
    val surfaceElevation2: Color = Color(0xFFF2F0E8),
    /** Surface elevation level 3 */
    val surfaceElevation3: Color = Color(0xFFECEAD2),

    // ==========================================================================
    // Shimmer Effect Colors
    // ==========================================================================

    /** Shimmer base color */
    val shimmerColor: Color = Color(0xFFE1E4D5),
    /** Shimmer highlight color */
    val shimmerHighlight: Color = Color(0xFFFEFCF4),

    // ==========================================================================
    // AI Insights Colors
    // ==========================================================================

    /** AI insights card background */
    val aiInsightsBackground: Color = Color(0xFF2D4438),
    /** AI insights accent color */
    val aiInsightsAccent: Color = Color(0xFFCDEDA3),
    /** AI insights text color */
    val aiInsightsText: Color = Color(0xFFFFFFFF),
    /** AI insights subtext color (80% white) */
    val aiInsightsSubtext: Color = Color(0xCCFFFFFF),

    // ==========================================================================
    // Interactive State Colors
    // ==========================================================================

    /** Ripple effect color */
    val ripple: Color = Color(0x1F4C6C43),
    /** Hover state color */
    val hover: Color = Color(0x0F4C6C43),

    // ==========================================================================
    // Content Colors
    // ==========================================================================

    /** Content on primary color backgrounds */
    val onPrimary: Color = Color.White,

    // ==========================================================================
    // Highlight Colors
    // ==========================================================================

    /** Active/highlight state (e.g., DeMark active setup) */
    val activeHighlight: Color = Color(0xFFFFEB3B)
)

/**
 * Dark theme extended colors.
 */
val DarkExtendedColors = ExtendedColors(
    // Status Colors (brighter for dark theme)
    statusUp = Color(0xFFFF6B6B),
    statusDown = Color(0xFF64B5F6),
    statusNeutral = Color(0xFFBDBDBD),

    // Financial Status Colors
    statusNew = Color(0xFF7A9A6E),
    statusIncrease = Color(0xFF4CAF88),
    statusDecrease = Color(0xFFE57373),
    statusRemoved = Color(0xFFBDBDBD),
    statusMaintain = Color(0xFF8A9472),

    // Chart Colors (brighter variants for dark theme)
    chartPrimary = Color(0xFF7A9A6E),
    chartSecondary = Color(0xFF5D9994),
    chartTertiary = Color(0xFF8A9472),
    chartGreen = Color(0xFF4CAF88),
    chartRed = Color(0xFFE57373),
    chartBlue = Color(0xFF5D9994),
    chartPurple = Color(0xFFB39DDB),
    chartOrange = Color(0xFFFFCC80),
    chartCyan = Color(0xFFB2DFDB),
    chartPink = Color(0xFFF8BBD9),

    // Chart UI Colors (inverted for dark theme)
    chartOnSurface = Color(0xFFE3E3DC),
    chartGrid = Color(0xFF353733),
    chartCardBackground = Color(0xFFF5F7F5),

    // Semantic Colors (brighter variants)
    success = Color(0xFF81C995),
    successContainer = Color(0xFF0F5223),
    onSuccessContainer = Color(0xFFC4EED0),
    warning = Color(0xFFFFCC80),
    warningContainer = Color(0xFF5C3D00),
    onWarningContainer = Color(0xFFFFDDB0),
    info = Color(0xFFA0CFCF),
    infoContainer = Color(0xFF1F4E4D),
    onInfoContainer = Color(0xFFBBEBEB),

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

    // Surface Elevation Colors (dark variants)
    surfaceElevation1 = Color(0xFF202120),
    surfaceElevation2 = Color(0xFF2A2C28),
    surfaceElevation3 = Color(0xFF353733),

    // Shimmer Effect Colors (dark variants)
    shimmerColor = Color(0xFF353733),
    shimmerHighlight = Color(0xFF44483D),

    // AI Insights Colors (same for both themes)
    aiInsightsBackground = Color(0xFF2D4438),
    aiInsightsAccent = Color(0xFFCDEDA3),
    aiInsightsText = Color(0xFFFFFFFF),
    aiInsightsSubtext = Color(0xCCFFFFFF),

    // Interactive State Colors (adjusted for dark theme)
    ripple = Color(0x29B1D18A),
    hover = Color(0x14B1D18A),

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
