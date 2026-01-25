package com.stockapp.feature.etf.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.theme.ThemeToggleButton
import com.stockapp.feature.etf.ui.tabs.CollectionStatusTab
import com.stockapp.feature.etf.ui.tabs.EtfSettingsTab
import com.stockapp.feature.etf.ui.tabs.StockChangesTab
import com.stockapp.feature.etf.ui.tabs.StockRankingTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtfScreen(
    onStockClick: () -> Unit = {},
    viewModel: EtfVm = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ETF 통계") },
                actions = { ThemeToggleButton() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = EtfTab.entries.indexOf(selectedTab),
                edgePadding = 16.dp
            ) {
                EtfTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.title) }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                EtfTab.COLLECTION_STATUS -> CollectionStatusTab(
                    viewModel = viewModel
                )
                EtfTab.STOCK_RANKING -> StockRankingTab(
                    viewModel = viewModel,
                    onStockClick = onStockClick
                )
                EtfTab.STOCK_CHANGES -> StockChangesTab(
                    viewModel = viewModel,
                    onStockClick = onStockClick
                )
                EtfTab.SETTINGS -> EtfSettingsTab(
                    viewModel = viewModel
                )
            }
        }
    }
}
