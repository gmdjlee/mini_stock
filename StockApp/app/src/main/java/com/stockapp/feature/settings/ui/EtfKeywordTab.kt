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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfKeyword
import com.stockapp.feature.etf.domain.model.FilterType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ETF Keyword Filter settings tab.
 * Provides UI for:
 * - Keyword filtering (include/exclude)
 * - Collection scheduling
 * - Collection status monitoring
 * - Data management (retention, cleanup)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtfKeywordTab(
    viewModel: EtfSettingsVm
) {
    val uiState by viewModel.uiState.collectAsState()
    val showTimePicker by viewModel.showTimePicker.collectAsState()
    val showAddKeywordDialog by viewModel.showAddKeywordDialog.collectAsState()
    val addKeywordType by viewModel.addKeywordType.collectAsState()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsState()

    val timePickerState = rememberTimePickerState(
        initialHour = uiState.collectionHour,
        initialMinute = uiState.collectionMinute,
        is24Hour = true
    )

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
                            text = "ETF 구성종목 수집 필터 및 스케줄링을 설정합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Collection status section
        item {
            CollectionStatusSection(
                uiState = uiState,
                onStartCollection = { viewModel.startManualCollection() }
            )
        }

        // Auto collection toggle
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "자동 수집",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (uiState.isAutoCollectionEnabled) "활성화됨" else "비활성화됨",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isAutoCollectionEnabled,
                        onCheckedChange = { viewModel.setAutoCollectionEnabled(it) }
                    )
                }
            }
        }

        // Collection time setting
        item {
            AnimatedVisibility(visible = uiState.isAutoCollectionEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.showTimePicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "수집 시간",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "매일 이 시간에 자동으로 수집합니다",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = uiState.collectionTimeDisplay,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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

        // Collection history section
        if (uiState.collectionHistory.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "수집 기록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(uiState.collectionHistory.take(5)) { history ->
                CollectionHistoryItem(history = history)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        EtfTimePickerDialog(
            state = timePickerState,
            onDismiss = { viewModel.hideTimePicker() },
            onConfirm = {
                viewModel.setCollectionTime(timePickerState.hour, timePickerState.minute)
            }
        )
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

@Composable
private fun CollectionStatusSection(
    uiState: EtfSettingsUiState,
    onStartCollection: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "수집 현황",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                when (uiState.collectionState) {
                    is CollectionUiState.Idle -> {
                        OutlinedButton(
                            onClick = onStartCollection
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("수동 수집")
                        }
                    }
                    is CollectionUiState.Collecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    else -> {
                        OutlinedButton(
                            onClick = onStartCollection
                        ) {
                            Text("다시 수집")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = uiState.collectionState) {
                is CollectionUiState.Idle -> {
                    if (uiState.lastCollectionTime != null) {
                        Text(
                            text = "마지막 수집: ${uiState.lastCollectionTimeDisplay}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "수집된 데이터가 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is CollectionUiState.Collecting -> {
                    Column {
                        Text(
                            text = "수집 중... (${state.current}/${state.total})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.total > 0) {
                            LinearProgressIndicator(
                                progress = { state.current.toFloat() / state.total },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                is CollectionUiState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = extendedColors.success,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "수집 완료: ${state.etfCount}개 ETF, ${state.constituentCount}개 종목",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extendedColors.success
                        )
                    }
                }
                is CollectionUiState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
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

            // Data retention slider
            Text(
                text = "데이터 보관 기간",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${dataRetentionDays}일 이상 지난 데이터는 자동으로 삭제됩니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = dataRetentionDays.toFloat(),
                onValueChange = { onRetentionDaysChange(it.toInt()) },
                valueRange = 7f..90f,
                steps = 11
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
                    text = "${dataRetentionDays}일",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "90일",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun CollectionHistoryItem(
    history: EtfCollectionHistoryUiItem
) {
    val extendedColors = LocalExtendedColors.current
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (history.status) {
                    CollectionStatus.SUCCESS -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = extendedColors.success,
                        modifier = Modifier.size(20.dp)
                    )
                    CollectionStatus.FAILED -> Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    CollectionStatus.PARTIAL -> Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    CollectionStatus.IN_PROGRESS -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = dateFormat.format(Date(history.startedAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = buildString {
                            append(history.collectionType)
                            if (history.status == CollectionStatus.SUCCESS) {
                                append(" | ${history.etfCount}개 ETF")
                                if (history.constituentCount > 0) {
                                    append(", ${history.constituentCount}종목")
                                }
                            } else if (history.status == CollectionStatus.FAILED) {
                                append(" | ${history.errorMessage ?: "실패"}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (history.durationDisplay != null) {
                Text(
                    text = history.durationDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtfTimePickerDialog(
    state: androidx.compose.material3.TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("수집 시간 설정") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("확인")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
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
 * Collection UI state for display.
 */
sealed class CollectionUiState {
    data object Idle : CollectionUiState()
    data class Collecting(val current: Int, val total: Int) : CollectionUiState()
    data class Success(val etfCount: Int, val constituentCount: Int) : CollectionUiState()
    data class Error(val message: String) : CollectionUiState()
}

/**
 * Collection history UI item.
 */
data class EtfCollectionHistoryUiItem(
    val id: Long,
    val collectionType: String,
    val status: CollectionStatus,
    val etfCount: Int,
    val constituentCount: Int,
    val errorMessage: String?,
    val startedAt: Long,
    val completedAt: Long?,
    val durationDisplay: String?
)

/**
 * ETF settings UI state.
 */
data class EtfSettingsUiState(
    val isAutoCollectionEnabled: Boolean = false,
    val collectionHour: Int = 6,
    val collectionMinute: Int = 0,
    val activeOnly: Boolean = false,
    val dataRetentionDays: Int = 30,
    val includeKeywords: List<EtfKeyword> = emptyList(),
    val excludeKeywords: List<EtfKeyword> = emptyList(),
    val filteredEtfCount: Int = 0,
    val totalEtfCount: Int = 0,
    val totalRecordCount: Int = 0,
    val dataDateRange: String? = null,
    val lastCollectionTime: Long? = null,
    val collectionState: CollectionUiState = CollectionUiState.Idle,
    val collectionHistory: List<EtfCollectionHistoryUiItem> = emptyList(),
    val error: String? = null
) {
    val collectionTimeDisplay: String
        get() = String.format("%02d:%02d", collectionHour, collectionMinute)

    val lastCollectionTimeDisplay: String
        get() {
            return lastCollectionTime?.let {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(it))
            } ?: ""
        }
}
