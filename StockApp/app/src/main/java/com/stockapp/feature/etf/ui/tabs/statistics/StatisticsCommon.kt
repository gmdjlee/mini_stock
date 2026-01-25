package com.stockapp.feature.etf.ui.tabs.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Shared utilities for ETF Statistics UI components.
 * Consolidates common formatting functions and composables.
 */

// ============== Formatting Functions ==============

/**
 * Format amount in Korean style (조, 억, 만 units).
 */
fun formatAmount(amount: Long): String = when {
    amount >= 1_000_000_000_000 -> String.format("%.1f조", amount / 1_000_000_000_000.0)
    amount >= 100_000_000 -> String.format("%.0f억", amount / 100_000_000.0)
    amount >= 10_000 -> String.format("%.0f만", amount / 10_000.0)
    else -> NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
}

/**
 * Format amount change with sign prefix.
 */
fun formatAmountChange(change: Long): String {
    val sign = if (change > 0) "+" else ""
    return when {
        abs(change) >= 1_000_000_000_000 -> String.format("%s%.1f조", sign, change / 1_000_000_000_000.0)
        abs(change) >= 100_000_000 -> String.format("%s%.0f억", sign, change / 100_000_000.0)
        abs(change) >= 10_000 -> String.format("%s%.0f만", sign, change / 10_000.0)
        else -> sign + NumberFormat.getNumberInstance(Locale.KOREA).format(change)
    }
}

/**
 * Format date to short form (MM/dd).
 */
fun formatDateShort(date: String): String = try {
    val parts = date.split("-")
    if (parts.size >= 3) "${parts[1]}/${parts[2]}" else date
} catch (e: Exception) {
    date
}

// ============== Common State Composables ==============

/**
 * Loading state with centered progress indicator.
 */
@Composable
fun StatisticsLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error state card with customizable title and message.
 */
@Composable
fun StatisticsErrorContent(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "데이터 로드 실패"
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Empty/No data state card with customizable icon, title, and description.
 */
@Composable
fun StatisticsEmptyContent(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Storage
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
