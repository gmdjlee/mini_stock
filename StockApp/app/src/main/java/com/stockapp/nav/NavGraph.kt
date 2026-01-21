package com.stockapp.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stockapp.feature.analysis.ui.AnalysisScreen
import com.stockapp.feature.indicator.ui.IndicatorScreen
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
                    // Use same navigation pattern as bottom nav to maintain consistent state
                    navController.navigate(Screen.Analysis.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
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

        // Settings screen
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
