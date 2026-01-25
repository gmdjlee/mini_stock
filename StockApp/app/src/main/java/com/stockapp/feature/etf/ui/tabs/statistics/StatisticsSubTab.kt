package com.stockapp.feature.etf.ui.tabs.statistics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sub-tab definitions for ETF Statistics screen.
 * Following EtfTab pattern from EtfVm.kt
 */
enum class StatisticsSubTab(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    AMOUNT_RANKING(
        title = "금액순위",
        icon = Icons.Default.Leaderboard,
        description = "ETF 평가금액 순위"
    ),
    NEWLY_INCLUDED(
        title = "신규편입",
        icon = Icons.Default.Add,
        description = "새로 편입된 종목"
    ),
    REMOVED(
        title = "편출",
        icon = Icons.Default.Remove,
        description = "ETF에서 제외된 종목"
    ),
    WEIGHT_INCREASED(
        title = "비중증가",
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        description = "비중이 증가한 종목"
    ),
    WEIGHT_DECREASED(
        title = "비중감소",
        icon = Icons.AutoMirrored.Filled.TrendingDown,
        description = "비중이 감소한 종목"
    ),
    CASH_DEPOSIT(
        title = "예금현황",
        icon = Icons.Default.Savings,
        description = "ETF 현금/예금 보유현황"
    ),
    STOCK_ANALYSIS(
        title = "종목분석",
        icon = Icons.Default.Analytics,
        description = "개별 종목 ETF 편입 분석"
    )
}
