package com.stockapp.feature.market.ui

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
import com.stockapp.feature.market.domain.model.MarketTab
import com.stockapp.feature.market.ui.tabs.BloodTab
import com.stockapp.feature.market.ui.tabs.FearGreedTab
import com.stockapp.feature.market.ui.tabs.FundFlowTab
import com.stockapp.feature.market.ui.tabs.OscillatorTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    viewModel: MarketVm = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("시장 지표") })
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ScrollableTabRow(
                selectedTabIndex = MarketTab.entries.indexOf(selectedTab),
                edgePadding = 8.dp
            ) {
                MarketTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.title) }
                    )
                }
            }

            when (selectedTab) {
                MarketTab.FEAR_GREED -> FearGreedTab(viewModel = viewModel)
                MarketTab.OSCILLATOR -> OscillatorTab(viewModel = viewModel)
                MarketTab.FUND_FLOW -> FundFlowTab(viewModel = viewModel)
                MarketTab.BLOOD -> BloodTab(viewModel = viewModel)
            }
        }
    }
}
