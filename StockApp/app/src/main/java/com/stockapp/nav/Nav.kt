package com.stockapp.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
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
 * Navigation destinations with deep link support (P3).
 *
 * Deep link URL scheme:
 * - stockapp://search
 * - stockapp://stock/{ticker} -> Analysis
 * - stockapp://stock/{ticker}/indicator
 * - stockapp://stock/{ticker}/financial
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
    // Bottom nav destinations with deep links
    data object Search : Screen(
        route = "search",
        title = "검색",
        icon = Icons.Default.Search,
        deepLinkPattern = DeepLinkScheme.buildUri("search")
    )

    data object Analysis : Screen(
        route = "analysis?${NavArgs.TICKER}={${NavArgs.TICKER}}",
        title = "수급 분석",
        icon = Icons.Default.Analytics,
        deepLinkPattern = DeepLinkScheme.buildUri("stock/{${NavArgs.TICKER}}")
    ) {
        // Route without argument for bottom nav
        const val baseRoute = "analysis"

        fun createRoute(ticker: String? = null): String =
            if (ticker != null) "analysis?${NavArgs.TICKER}=$ticker" else baseRoute
    }

    data object Indicator : Screen(
        route = "indicator?${NavArgs.TICKER}={${NavArgs.TICKER}}",
        title = "기술 지표",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        deepLinkPattern = DeepLinkScheme.buildUri("stock/{${NavArgs.TICKER}}/indicator")
    ) {
        const val baseRoute = "indicator"

        fun createRoute(ticker: String? = null): String =
            if (ticker != null) "indicator?${NavArgs.TICKER}=$ticker" else baseRoute
    }

    data object Financial : Screen(
        route = "financial?${NavArgs.TICKER}={${NavArgs.TICKER}}",
        title = "재무정보",
        icon = Icons.Default.AccountBalance,
        deepLinkPattern = DeepLinkScheme.buildUri("stock/{${NavArgs.TICKER}}/financial")
    ) {
        const val baseRoute = "financial"

        fun createRoute(ticker: String? = null): String =
            if (ticker != null) "financial?${NavArgs.TICKER}=$ticker" else baseRoute
    }

    data object Ranking : Screen(
        route = "ranking",
        title = "순위정보",
        icon = Icons.Default.Leaderboard,
        deepLinkPattern = DeepLinkScheme.buildUri("ranking")
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
        val bottomNavItems = listOf(Search, Analysis, Indicator, Financial, Ranking, Etf, Settings)

        // Base routes for bottom nav selection matching
        fun getBaseRoute(screen: Screen): String = when (screen) {
            Analysis -> Analysis.baseRoute
            Indicator -> Indicator.baseRoute
            Financial -> Financial.baseRoute
            else -> screen.route
        }
    }
}

/**
 * Navigation arguments.
 */
object NavArgs {
    const val TICKER = "ticker"
    const val TYPE = "type"
}
