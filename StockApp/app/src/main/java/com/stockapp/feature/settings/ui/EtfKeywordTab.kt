package com.stockapp.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stockapp.core.ui.theme.LocalExtendedColors
import com.stockapp.feature.etf.domain.model.EtfKeyword
import com.stockapp.feature.etf.domain.model.FilterType

/**
 * ETF Keyword Filter settings tab.
 * Provides UI for:
 * - Keyword filtering (include/exclude)
 * - Active ETF only filter toggle
 * - Filter preview
 * - Data management (retention, cleanup)
 *
 * Note: Auto-collection settings have been moved to the Scheduling tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtfKeywordTab(
    viewModel: EtfSettingsVm
) {
    val uiState by viewModel.uiState.collectAsState()
    val showAddKeywordDialog by viewModel.showAddKeywordDialog.collectAsState()
    val addKeywordType by viewModel.addKeywordType.collectAsState()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header info card
        item {
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
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ETF 수집 설정",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ETF 구성종목 수집 필터를 설정합니다. 자동 수집은 스케줄링 탭에서 설정합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active only toggle
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "액티브 ETF만 수집",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "패시브 ETF를 제외하고 액티브 ETF만 수집합니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.activeOnly,
                        onCheckedChange = { viewModel.setActiveOnly(it) }
                    )
                }
            }
        }

        // Include keywords section
        item {
            KeywordSection(
                title = "포함 키워드",
                description = "이 키워드가 포함된 ETF만 수집합니다",
                keywords = uiState.includeKeywords,
                filterType = FilterType.INCLUDE,
                onAddClick = { viewModel.showAddKeywordDialog(FilterType.INCLUDE) },
                onDeleteClick = { viewModel.deleteKeyword(it) }
            )
        }

        // Exclude keywords section
        item {
            KeywordSection(
                title = "제외 키워드",
                description = "이 키워드가 포함된 ETF는 제외합니다",
                keywords = uiState.excludeKeywords,
                filterType = FilterType.EXCLUDE,
                onAddClick = { viewModel.showAddKeywordDialog(FilterType.EXCLUDE) },
                onDeleteClick = { viewModel.deleteKeyword(it) }
            )
        }

        // Filter preview
        item {
            FilterPreviewSection(
                filteredEtfCount = uiState.filteredEtfCount,
                totalEtfCount = uiState.totalEtfCount,
                onPreviewClick = { viewModel.loadFilterPreview() }
            )
        }

        // Data management section
        item {
            DataManagementSection(
                dataRetentionDays = uiState.dataRetentionDays,
                totalRecordCount = uiState.totalRecordCount,
                dataDateRange = uiState.dataDateRange,
                onRetentionDaysChange = { viewModel.setDataRetentionDays(it) },
                onCleanupClick = { viewModel.cleanupOldData() },
                onResetClick = { viewModel.showDeleteConfirmDialog() }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Add keyword dialog
    if (showAddKeywordDialog) {
        AddKeywordDialog(
            filterType = addKeywordType,
            onDismiss = { viewModel.hideAddKeywordDialog() },
            onConfirm = { keyword -> viewModel.addKeyword(keyword) }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("데이터 초기화") },
            text = {
                Text("모든 ETF 데이터를 삭제합니다.\n이 작업은 되돌릴 수 없습니다.\n\n계속하시겠습니까?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        viewModel.hideDeleteConfirmDialog()
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.hideDeleteConfirmDialog() }) {
                    Text("취소")
                }
            }
        )
    }

    // Error dialog
    uiState.error?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("오류") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("확인")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordSection(
    title: String,
    description: String,
    keywords: List<EtfKeyword>,
    filterType: FilterType,
    onAddClick: () -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val chipColor = if (filterType == FilterType.INCLUDE) {
        extendedColors.success
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = chipColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "키워드 추가",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (keywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keywords.forEach { keyword ->
                        AssistChip(
                            onClick = {},
                            label = { Text(keyword.keyword) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onDeleteClick(keyword.id) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "삭제",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = chipColor.copy(alpha = 0.1f),
                                labelColor = chipColor
                            )
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "설정된 키워드가 없습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilterPreviewSection(
    filteredEtfCount: Int,
    totalEtfCount: Int,
    onPreviewClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "필터 적용 결과",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(onClick = onPreviewClick) {
                    Text("새로고침")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (totalEtfCount > 0) {
                    "전체 $totalEtfCount 개 ETF 중 $filteredEtfCount 개가 수집 대상입니다"
                } else {
                    "ETF 목록을 불러오는 중..."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DataManagementSection(
    dataRetentionDays: Int,
    totalRecordCount: Int,
    dataDateRange: String?,
    onRetentionDaysChange: (Int) -> Unit,
    onCleanupClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "데이터 관리",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data retention setting
            Text(
                text = "데이터 보관 기간",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            // Unlimited toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (dataRetentionDays == -1) "무제한 (데이터 자동 삭제 안함)" else "${dataRetentionDays}일 이상 지난 데이터는 자동으로 삭제됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "무제한",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (dataRetentionDays == -1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = dataRetentionDays == -1,
                        onCheckedChange = { unlimited ->
                            if (unlimited) {
                                onRetentionDaysChange(-1)
                            } else {
                                onRetentionDaysChange(30) // default
                            }
                        }
                    )
                }
            }

            // Slider (only shown when not unlimited)
            AnimatedVisibility(visible = dataRetentionDays != -1) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = if (dataRetentionDays > 0) dataRetentionDays.toFloat() else 30f,
                        onValueChange = { onRetentionDaysChange(it.toInt()) },
                        valueRange = 7f..365f,
                        steps = 0
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "7일",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (dataRetentionDays > 0) dataRetentionDays else 30}일",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "365일",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Data statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "저장된 레코드",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%,d 건".format(totalRecordCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (dataDateRange != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "데이터 기간",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dataDateRange,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCleanupClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("정리")
                }
                OutlinedButton(
                    onClick = onResetClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "초기화",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddKeywordDialog(
    filterType: FilterType,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var keyword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (filterType == FilterType.INCLUDE) "포함 키워드 추가" else "제외 키워드 추가"
            )
        },
        text = {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("키워드") },
                placeholder = { Text("예: 반도체, AI") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (keyword.isNotBlank()) {
                            onConfirm(keyword)
                        }
                    }
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(keyword) },
                enabled = keyword.isNotBlank()
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * ETF settings UI state.
 */
data class EtfSettingsUiState(
    val activeOnly: Boolean = true,
    val dataRetentionDays: Int = -1,
    val includeKeywords: List<EtfKeyword> = emptyList(),
    val excludeKeywords: List<EtfKeyword> = emptyList(),
    val filteredEtfCount: Int = 0,
    val totalEtfCount: Int = 0,
    val totalRecordCount: Int = 0,
    val dataDateRange: String? = null,
    val error: String? = null
)
