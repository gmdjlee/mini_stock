package com.stockapp.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stockapp.feature.search.ui.SearchScreen

/**
 * Main navigation graph.
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
        // Search screen
        composable(Screen.Search.route) {
            SearchScreen(
                onStockClick = { ticker ->
                    navController.navigate(Screen.StockDetail.createRoute(ticker))
                }
            )
        }

        // Analysis screen (placeholder)
        composable(Screen.Analysis.route) {
            PlaceholderScreen(title = "수급 분석")
        }

        // Indicator screen (placeholder)
        composable(Screen.Indicator.route) {
            PlaceholderScreen(title = "기술 지표")
        }

        // Market screen (placeholder)
        composable(Screen.Market.route) {
            PlaceholderScreen(title = "시장 지표")
        }

        // Condition screen (placeholder)
        composable(Screen.Condition.route) {
            PlaceholderScreen(title = "조건검색")
        }

        // Stock detail screen
        composable(
            route = Screen.StockDetail.route,
            arguments = listOf(
                navArgument(NavArgs.TICKER) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val ticker = backStackEntry.arguments?.getString(NavArgs.TICKER) ?: ""
            PlaceholderScreen(title = "종목 상세: $ticker")
        }

        // Indicator detail screen
        composable(
            route = Screen.IndicatorDetail.route,
            arguments = listOf(
                navArgument(NavArgs.TICKER) { type = NavType.StringType },
                navArgument(NavArgs.TYPE) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val ticker = backStackEntry.arguments?.getString(NavArgs.TICKER) ?: ""
            val type = backStackEntry.arguments?.getString(NavArgs.TYPE) ?: ""
            PlaceholderScreen(title = "지표 상세: $ticker - $type")
        }
    }
}

/**
 * Placeholder screen for unimplemented features.
 */
@Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "$title\n(구현 예정)",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private val Modifier = androidx.compose.ui.Modifier.fillMaxSize()

private fun androidx.compose.ui.Modifier.fillMaxSize() =
    this.then(androidx.compose.foundation.layout.fillMaxSize())
