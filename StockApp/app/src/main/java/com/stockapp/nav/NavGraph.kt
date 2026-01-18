package com.stockapp.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stockapp.feature.analysis.ui.AnalysisScreen
import com.stockapp.feature.condition.ui.ConditionScreen
import com.stockapp.feature.indicator.ui.IndicatorScreen
import com.stockapp.feature.market.ui.MarketScreen
import com.stockapp.feature.search.ui.SearchScreen
import com.stockapp.feature.settings.ui.SettingsScreen

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

        // Analysis screen (placeholder - no ticker context)
        composable(Screen.Analysis.route) {
            PlaceholderScreen(title = "수급 분석\n검색에서 종목을 선택하세요")
        }

        // Indicator screen (placeholder)
        composable(Screen.Indicator.route) {
            PlaceholderScreen(title = "기술 지표\n검색에서 종목을 선택하세요")
        }

        // Market screen - shows market indicators
        composable(Screen.Market.route) {
            MarketScreen()
        }

        // Condition screen - shows condition search
        composable(Screen.Condition.route) {
            ConditionScreen(
                onStockClick = { ticker ->
                    navController.navigate(Screen.StockDetail.createRoute(ticker))
                }
            )
        }

        // Settings screen
        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // Stock detail screen - shows Analysis
        composable(
            route = Screen.StockDetail.route,
            arguments = listOf(
                navArgument(NavArgs.TICKER) { type = NavType.StringType }
            )
        ) {
            AnalysisScreen(
                onBackClick = { navController.popBackStack() },
                onIndicatorClick = { ticker ->
                    navController.navigate(Screen.IndicatorDetail.createRoute(ticker))
                }
            )
        }

        // Indicator detail screen
        composable(
            route = Screen.IndicatorDetail.route,
            arguments = listOf(
                navArgument(NavArgs.TICKER) { type = NavType.StringType }
            )
        ) {
            IndicatorScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Placeholder screen for unimplemented features.
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
    }
}
