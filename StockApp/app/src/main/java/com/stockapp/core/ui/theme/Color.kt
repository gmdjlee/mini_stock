package com.stockapp.core.ui.theme

import androidx.compose.ui.graphics.Color

// Primary colors - Blue theme for finance app
val Blue10 = Color(0xFF001F3F)
val Blue20 = Color(0xFF003366)
val Blue30 = Color(0xFF004D99)
val Blue40 = Color(0xFF0066CC)
val Blue80 = Color(0xFF99CCFF)
val Blue90 = Color(0xFFCCE5FF)

// Secondary colors - Teal
val Teal10 = Color(0xFF002626)
val Teal20 = Color(0xFF004D4D)
val Teal30 = Color(0xFF007373)
val Teal40 = Color(0xFF009999)
val Teal80 = Color(0xFF99E5E5)
val Teal90 = Color(0xFFCCF2F2)

// Tertiary colors - Orange for accents
val Orange10 = Color(0xFF331A00)
val Orange20 = Color(0xFF663300)
val Orange30 = Color(0xFF994D00)
val Orange40 = Color(0xFFCC6600)
val Orange80 = Color(0xFFFFCC99)
val Orange90 = Color(0xFFFFE5CC)

// Error colors
val Red10 = Color(0xFF410E0B)
val Red20 = Color(0xFF601410)
val Red30 = Color(0xFF8C1D18)
val Red40 = Color(0xFFB3261E)
val Red80 = Color(0xFFF2B8B5)
val Red90 = Color(0xFFF9DEDC)

// Neutral colors
val Grey10 = Color(0xFF1A1C1E)
val Grey20 = Color(0xFF2F3133)
val Grey30 = Color(0xFF46484A)
val Grey40 = Color(0xFF5E6062)
val Grey80 = Color(0xFFC6C7C9)
val Grey90 = Color(0xFFE2E3E5)
val Grey95 = Color(0xFFF1F1F2)
val Grey99 = Color(0xFFFCFCFD)

// Stock-specific colors
val StockUp = Color(0xFFD32F2F)       // Red for price increase (Korean market convention)
val StockDown = Color(0xFF1976D2)      // Blue for price decrease
val StockNeutral = Color(0xFF757575)   // Grey for no change

// Elder Impulse colors
val ElderGreen = Color(0xFF4CAF50)     // Bullish
val ElderRed = Color(0xFFF44336)       // Bearish
val ElderBlue = Color(0xFF2196F3)      // Neutral

// Chart colors - EtfMonitor style (Moss Green Nature theme)
val ChartPrimary = Color(0xFF4C6C43)      // Moss green (main line)
val ChartSecondary = Color(0xFF396663)    // Teal (secondary line)
val ChartTertiary = Color(0xFF586249)     // Olive (tertiary line)
val ChartGreen = Color(0xFF2E7D5A)        // Teal green (bullish/positive)
val ChartRed = Color(0xFFBA1A1A)          // Error red (bearish/negative)
val ChartBlue = Color(0xFF396663)         // Teal (neutral)
val ChartPurple = Color(0xFF8E7CC3)       // Purple (accent - Fear/Greed)
val ChartOrange = Color(0xFFE0A050)       // Orange (warning/signal)
val ChartCyan = Color(0xFFA0CFCF)         // Light teal (highlight)
val ChartPink = Color(0xFFD4A5A5)         // Dusty pink (special)
val ChartDefaultBlack = Color(0xFF1C1C1C) // Default black

// Chart grid/background colors
val ChartGridLight = Color(0xFFE1E4D5)    // Light grid
val ChartGridDark = Color(0xFF353733)     // Dark grid
val ChartCardBackgroundLight = Color(0xFFFFFFFF)  // Light chart card background
val ChartCardBackgroundDark = Color(0xFFF5F7F5)   // Dark chart card background

// Legacy chart colors (for backward compatibility)
val ChartLine1 = Color(0xFF1976D2)
val ChartLine2 = Color(0xFF388E3C)
val ChartLine3 = Color(0xFFF57C00)
val ChartLine4 = Color(0xFF7B1FA2)
val ChartFill = Color(0x331976D2)

// Signal colors for Korean market (Red=Up, Blue=Down)
val SignalBuyStrong = Color(0xFFF44336)   // Strong buy - red
val SignalBuyWeak = Color(0xFFFF8A80)     // Weak buy - light red
val SignalSellStrong = Color(0xFF2196F3)  // Strong sell - blue
val SignalSellWeak = Color(0xFF82B1FF)    // Weak sell - light blue

// Matplotlib tab colors for Python chart compatibility
val TabBlue = Color(0xFF1F77B4)           // matplotlib tab:blue - Close price line
val TabOrange = Color(0xFFFF7F0E)         // matplotlib tab:orange - MA, Signal, EMA13
val TabGreen = Color(0xFF2CA02C)          // matplotlib tab:green
val TabRed = Color(0xFFD62728)            // matplotlib tab:red
val TabGray = Color(0xFF7F7F7F)           // matplotlib gray - Neutral (Elder)

// Python chart reference colors
val PrimaryBuy = Color(0xFF8B0000)        // darkred - Primary Buy signal
val AdditionalBuy = Color(0xFFFF0000)     // red with alpha - Additional Buy
val PrimarySell = Color(0xFF00008B)       // darkblue - Primary Sell signal
val AdditionalSell = Color(0xFF0000FF)    // blue with alpha - Additional Sell
val DemarkRed = Color(0xFFFF0000)         // red - TD Sell Setup line
val DemarkBlue = Color(0xFF0000FF)        // blue - TD Buy Setup line
val FearGreedGreen = Color(0xFF008000)    // green - Fear threshold line
val FearGreedRed = Color(0xFFFF0000)      // red - Greed threshold line

// Oscillator chart colors (Python style)
val OscillatorBlue = Color(0xFF1976D2)    // Market cap fill/line
val OscillatorOrange = Color(0xFFFF5722)  // Oscillator line
val HistogramTeal = Color(0xFF26A69A)     // Positive histogram
val HistogramRed = Color(0xFFEF5350)      // Negative histogram
val MacdBlue = Color(0xFF2196F3)          // MACD line
val MacdSignalOrange = Color(0xFFFF9800)  // Signal line
