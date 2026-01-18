package com.stockapp.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations.
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
    data object Market : Screen("market", "시장 지표", Icons.AutoMirrored.Filled.TrendingUp)
    data object Condition : Screen("condition", "조건검색", Icons.Default.FilterList)
    data object Settings : Screen("settings", "설정", Icons.Default.Settings)

    // Detail screens
    data object StockDetail : Screen("stock/{ticker}", "종목 상세") {
        fun createRoute(ticker: String) = "stock/$ticker"
    }

    data object IndicatorDetail : Screen("indicator_detail/{ticker}", "지표 상세") {
        fun createRoute(ticker: String) = "indicator_detail/$ticker"
    }

    companion object {
        val bottomNavItems = listOf(Search, Analysis, Indicator, Market, Condition, Settings)
    }
}

/**
 * Navigation arguments.
 */
object NavArgs {
    const val TICKER = "ticker"
    const val TYPE = "type"
}
