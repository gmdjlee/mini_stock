package com.stockapp.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import android.util.Log
import com.stockapp.feature.analysis.ui.AnalysisVm
import com.stockapp.feature.etf.ui.EtfScreen
import com.stockapp.feature.market.ui.MarketScreen
import com.stockapp.feature.ranking.ui.RankingScreen
import com.stockapp.feature.settings.ui.SettingsScreen
import com.stockapp.feature.stockanalysis.ui.StockAnalysisScreen

/**
 * Main navigation graph with deep link support.
 *
 * Deep link URL scheme: stockapp://
 * - stockapp://search -> StockAnalysis (tab=0)
 * - stockapp://stock/{ticker} -> StockAnalysis (tab=1, Analysis)
 * - stockapp://stock/{ticker}/indicator -> StockAnalysis (tab=2)
 * - stockapp://stock/{ticker}/financial -> StockAnalysis (tab=3)
 * - stockapp://ranking
 * - stockapp://etf
 * - stockapp://settings
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.StockAnalysis.baseRoute,
        modifier = modifier
    ) {
        // Stock Analysis - integrated screen with 4 tabs (Search, Analysis, Indicator, Financial)
        composable(
            route = Screen.StockAnalysis.route,
            arguments = listOf(
                navArgument(NavArgs.TICKER) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(NavArgs.TAB) {
                    type = NavType.IntType
                    defaultValue = 0
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = DeepLinkScheme.buildUri("search") },
                navDeepLink { uriPattern = DeepLinkScheme.buildUri("stock/{${NavArgs.TICKER}}") },
                navDeepLink { uriPattern = DeepLinkScheme.buildUri("stock/{${NavArgs.TICKER}}/indicator") },
                navDeepLink { uriPattern = DeepLinkScheme.buildUri("stock/{${NavArgs.TICKER}}/financial") }
            )
        ) { backStackEntry ->
            val ticker = backStackEntry.arguments?.getString(NavArgs.TICKER)
            val tab = backStackEntry.arguments?.getInt(NavArgs.TAB) ?: 0
            val viewModel: AnalysisVm = hiltViewModel()

            // Handle deep link ticker
            LaunchedEffect(ticker) {
                ticker?.let {
                    if (AnalysisVm.isValidKoreanTicker(it)) {
                        viewModel.selectTickerFromDeepLink(it)
                    } else {
                        Log.w("NavGraph", "Invalid deep link ticker ignored: $it")
                    }
                }
            }

            // Determine initial tab from deep link URI
            val initialTab = when {
                ticker != null -> {
                    val uri = backStackEntry.destination.route ?: ""
                    when {
                        uri.contains("financial") || tab == 3 -> 3
                        uri.contains("indicator") || tab == 2 -> 2
                        else -> 1 // Default to analysis for stock deep links
                    }
                }
                else -> tab
            }

            StockAnalysisScreen(initialTab = initialTab)
        }

        // Ranking screen - stock selection navigates to StockAnalysis
        composable(
            route = Screen.Ranking.route,
            deepLinks = Screen.Ranking.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) {
            RankingScreen(
                onStockClick = {
                    navController.navigate(Screen.StockAnalysis.createRoute(tab = 1)) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Market screen
        composable(
            route = Screen.Market.route,
            deepLinks = Screen.Market.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) {
            MarketScreen()
        }

        // ETF screen - stock selection navigates to StockAnalysis
        composable(
            route = Screen.Etf.route,
            deepLinks = Screen.Etf.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) {
            EtfScreen(
                onStockClick = {
                    navController.navigate(Screen.StockAnalysis.createRoute(tab = 1)) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Settings screen
        composable(
            route = Screen.Settings.route,
            deepLinks = Screen.Settings.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) {
            SettingsScreen()
        }
    }
}
