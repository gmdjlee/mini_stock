package com.stockapp.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Deep link URI scheme for the app.
 */
object DeepLinkScheme {
    const val SCHEME = "stockapp"
    const val HOST = ""

    fun buildUri(path: String): String = "$SCHEME://$path"
}

/**
 * Navigation destinations with deep link support.
 *
 * Deep link URL scheme:
 * - stockapp://search -> StockAnalysis (tab=0)
 * - stockapp://stock/{ticker} -> StockAnalysis (tab=1, Analysis)
 * - stockapp://stock/{ticker}/indicator -> StockAnalysis (tab=2)
 * - stockapp://stock/{ticker}/financial -> StockAnalysis (tab=3)
 * - stockapp://ranking
 * - stockapp://etf
 * - stockapp://settings
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val deepLinkPattern: String? = null
) {
    data object StockAnalysis : Screen(
        route = "stock_analysis?${NavArgs.TICKER}={${NavArgs.TICKER}}&${NavArgs.TAB}={${NavArgs.TAB}}",
        title = "종목 분석",
        icon = Icons.Default.Analytics
    ) {
        const val baseRoute = "stock_analysis"

        fun createRoute(ticker: String? = null, tab: Int? = null): String {
            val params = mutableListOf<String>()
            ticker?.let { params.add("${NavArgs.TICKER}=$it") }
            tab?.let { params.add("${NavArgs.TAB}=$it") }
            return if (params.isEmpty()) baseRoute else "$baseRoute?${params.joinToString("&")}"
        }
    }

    data object Ranking : Screen(
        route = "ranking",
        title = "순위정보",
        icon = Icons.Default.Leaderboard,
        deepLinkPattern = DeepLinkScheme.buildUri("ranking")
    )

    data object Market : Screen(
        route = "market",
        title = "시장",
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        deepLinkPattern = DeepLinkScheme.buildUri("market")
    )

    data object Etf : Screen(
        route = "etf",
        title = "ETF",
        icon = Icons.Default.PieChart,
        deepLinkPattern = DeepLinkScheme.buildUri("etf")
    )

    data object Settings : Screen(
        route = "settings",
        title = "설정",
        icon = Icons.Default.Settings,
        deepLinkPattern = DeepLinkScheme.buildUri("settings")
    )

    companion object {
        val bottomNavItems = listOf(StockAnalysis, Ranking, Market, Etf, Settings)

        fun getBaseRoute(screen: Screen): String = when (screen) {
            StockAnalysis -> StockAnalysis.baseRoute
            else -> screen.route
        }
    }
}

/**
 * Navigation arguments.
 */
object NavArgs {
    const val TICKER = "ticker"
    const val TAB = "tab"
    const val TYPE = "type"
}
