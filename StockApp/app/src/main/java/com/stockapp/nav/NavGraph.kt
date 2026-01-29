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
import com.stockapp.core.state.SelectedStockManager
import com.stockapp.feature.analysis.ui.AnalysisScreen
import com.stockapp.feature.analysis.ui.AnalysisVm
import com.stockapp.feature.etf.ui.EtfScreen
import com.stockapp.feature.financial.ui.FinancialScreen
import com.stockapp.feature.indicator.ui.IndicatorScreen
import com.stockapp.feature.ranking.ui.RankingScreen
import com.stockapp.feature.search.ui.SearchScreen
import com.stockapp.feature.settings.ui.SettingsScreen

/**
 * Main navigation graph with deep link support (P3).
 *
 * Deep link URL scheme: stockapp://
 * - stockapp://search
 * - stockapp://stock/{ticker} -> Analysis with ticker
 * - stockapp://stock/{ticker}/indicator
 * - stockapp://stock/{ticker}/financial
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
        startDestination = Screen.Search.route,
        modifier = modifier
    ) {
        // Search screen - stock selection updates shared state
        composable(
            route = Screen.Search.route,
            deepLinks = Screen.Search.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) {
            SearchScreen(
                onStockClick = { ticker ->
                    // Navigate to Analysis tab after selecting stock
                    navController.navigate(Screen.Analysis.createRoute(ticker)) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Analysis screen - uses shared stock state, supports deep link with ticker
        composable(
            route = Screen.Analysis.route,
            arguments = listOf(
                navArgument(NavArgs.TICKER) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = Screen.Analysis.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) { backStackEntry ->
            val ticker = backStackEntry.arguments?.getString(NavArgs.TICKER)
            val viewModel: AnalysisVm = hiltViewModel()

            // Handle deep link ticker - select stock if provided
            LaunchedEffect(ticker) {
                ticker?.let { viewModel.selectTickerFromDeepLink(it) }
            }

            AnalysisScreen()
        }

        // Indicator screen - uses shared stock state, supports deep link with ticker
        composable(
            route = Screen.Indicator.route,
            arguments = listOf(
                navArgument(NavArgs.TICKER) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = Screen.Indicator.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) { backStackEntry ->
            val ticker = backStackEntry.arguments?.getString(NavArgs.TICKER)
            val analysisVm: AnalysisVm = hiltViewModel()

            LaunchedEffect(ticker) {
                ticker?.let { analysisVm.selectTickerFromDeepLink(it) }
            }

            IndicatorScreen()
        }

        // Financial screen - uses shared stock state, supports deep link with ticker
        composable(
            route = Screen.Financial.route,
            arguments = listOf(
                navArgument(NavArgs.TICKER) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = Screen.Financial.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) { backStackEntry ->
            val ticker = backStackEntry.arguments?.getString(NavArgs.TICKER)
            val analysisVm: AnalysisVm = hiltViewModel()

            LaunchedEffect(ticker) {
                ticker?.let { analysisVm.selectTickerFromDeepLink(it) }
            }

            FinancialScreen()
        }

        // Ranking screen - stock selection navigates to Analysis
        composable(
            route = Screen.Ranking.route,
            deepLinks = Screen.Ranking.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) {
            RankingScreen(
                onStockClick = {
                    navController.navigate(Screen.Analysis.createRoute()) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // ETF screen - stock selection navigates to Analysis
        composable(
            route = Screen.Etf.route,
            deepLinks = Screen.Etf.deepLinkPattern?.let {
                listOf(navDeepLink { uriPattern = it })
            } ?: emptyList()
        ) {
            EtfScreen(
                onStockClick = {
                    navController.navigate(Screen.Analysis.createRoute()) {
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
