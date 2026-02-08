package com.stockapp.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Settings tab for market indicator data collection configuration.
 * Shows information about each indicator and data requirements.
 */
@Composable
fun MarketIndicatorSettingsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "시장 지표 데이터 안내",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "4개의 시장 지표 데이터 소스와 요구사항입니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Fear & Greed
        IndicatorInfoCard(
            title = "공포/탐욕 지수",
            description = "KOSPI 모멘텀, RSI, 변동성, 투자자 수급, 공매도 비율을 종합한 시장 심리 지표",
            dataSource = "KRX (자동 수집)",
            requiresApiKey = false
        )

        // Oscillator
        IndicatorInfoCard(
            title = "과매수/과매도",
            description = "전체 시장 종목의 상승/하락 비율을 5일 EMA로 평활화한 지표",
            dataSource = "KRX (자동 수집)",
            requiresApiKey = false
        )

        // Fund Flow
        IndicatorInfoCard(
            title = "자금 동향",
            description = "외국인, 기관, 개인 투자자별 순매수/순매도 추이",
            dataSource = "KRX (자동 수집)",
            requiresApiKey = false
        )

        // Blood Indicator
        IndicatorInfoCard(
            title = "Blood Indicator",
            description = "US 3-Month T-Bill Yield / High Yield Spread 비율.\n" +
                "시장 위험도를 측정하는 글로벌 지표입니다.\n\n" +
                "BLOOD = ^IRX (Yahoo Finance) / BAMLH0A0HYM2 (FRED)\n\n" +
                "이 지표를 사용하려면 FRED API 키가 필요합니다.\n" +
                "https://fred.stlouisfed.org/docs/api/api_key.html 에서\n" +
                "무료로 발급받을 수 있습니다.",
            dataSource = "Yahoo Finance + FRED API",
            requiresApiKey = true
        )

        // Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "공포/탐욕, 과매수/과매도, 자금 동향은 KRX 데이터를 실시간으로 " +
                        "수집하므로 별도 설정이 필요 없습니다. " +
                        "Blood Indicator는 추후 FRED API 키 입력 기능이 추가될 예정입니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun IndicatorInfoCard(
    title: String,
    description: String,
    dataSource: String,
    requiresApiKey: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (requiresApiKey) {
                    Text(
                        text = "API 키 필요",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "자동 수집",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "데이터 소스: $dataSource",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
