package com.stockapp.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stockapp.feature.analysis.ui.AnalysisScreen
import com.stockapp.feature.condition.ui.ConditionScreen
import com.stockapp.feature.indicator.ui.IndicatorScreen
import com.stockapp.feature.market.ui.MarketScreen
import com.stockapp.feature.search.ui.SearchScreen
import com.stockapp.feature.settings.ui.SettingsScreen

/**
 * Main navigation graph.
 * All screens are now bottom navigation tabs with shared stock state.
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
        composable(Screen.Search.route) {
            SearchScreen(
                onStockClick = { ticker ->
                    // Navigate to Analysis tab after selecting stock
                    navController.navigate(Screen.Analysis.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Analysis screen - uses shared stock state
        composable(Screen.Analysis.route) {
            AnalysisScreen()
        }

        // Indicator screen - uses shared stock state
        composable(Screen.Indicator.route) {
            IndicatorScreen()
        }

        // Market screen - shows market indicators
        composable(Screen.Market.route) {
            MarketScreen()
        }

        // Condition screen - stock selection updates shared state
        composable(Screen.Condition.route) {
            ConditionScreen(
                onStockClick = { ticker ->
                    // Navigate to Analysis tab after selecting stock
                    navController.navigate(Screen.Analysis.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Settings screen
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
