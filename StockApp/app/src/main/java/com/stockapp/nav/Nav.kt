package com.stockapp.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations.
 * All screens are now in bottom navigation - no detail screens.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    // Bottom nav destinations
    data object Search : Screen("search", "검색", Icons.Default.Search)
    data object Analysis : Screen("analysis", "수급 분석", Icons.Default.Analytics)
    data object Indicator : Screen("indicator", "기술 지표", Icons.AutoMirrored.Filled.ShowChart)
    data object Ranking : Screen("ranking", "순위정보", Icons.Default.Leaderboard)
    data object Etf : Screen("etf", "ETF", Icons.Default.PieChart)
    data object Settings : Screen("settings", "설정", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Search, Analysis, Indicator, Ranking, Etf, Settings)
    }
}

/**
 * Navigation arguments - kept for backward compatibility.
 */
object NavArgs {
    const val TICKER = "ticker"
    const val TYPE = "type"
}
