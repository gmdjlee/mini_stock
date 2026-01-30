package com.stockapp.feature.settings.ui

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.backup.BackupMetadata
import com.stockapp.core.backup.BackupType
import com.stockapp.core.backup.RestoreMode
import com.stockapp.core.backup.ValidationResult
import com.stockapp.core.ui.theme.LocalExtendedColors
import java.util.Calendar

@Composable
fun DbBackupTab(
    viewModel: DbBackupVm = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // File pickers
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.createBackup(it) }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectRestoreFile(it, null) }
    }

    // Date pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        ShowDatePicker(
            onDateSelected = { viewModel.setStartDate(it) },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        ShowDatePicker(
            onDateSelected = { viewModel.setEndDate(it) },
            onDismiss = { showEndDatePicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info card
        InfoCard()

        // Backup section
        BackupSection(
            backupType = uiState.backupType,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            isCreating = uiState.isCreating,
            progress = uiState.backupProgress,
            message = uiState.backupMessage,
            result = uiState.lastBackupResult,
            onBackupTypeChange = viewModel::setBackupType,
            onStartDateClick = { showStartDatePicker = true },
            onEndDateClick = { showEndDatePicker = true },
            onClearStartDate = { viewModel.setStartDate(null) },
            onClearEndDate = { viewModel.setEndDate(null) },
            onCreateClick = {
                createFileLauncher.launch(viewModel.generateBackupFileName())
            },
            onResultDismiss = viewModel::clearBackupResult
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Restore section
        RestoreSection(
            selectedFileName = uiState.selectedFileName,
            validationResult = uiState.validationResult,
            metadata = uiState.backupMetadata,
            restoreMode = uiState.restoreMode,
            isRestoring = uiState.isRestoring,
            progress = uiState.restoreProgress,
            message = uiState.restoreMessage,
            result = uiState.lastRestoreResult,
            onSelectFileClick = { openFileLauncher.launch(arrayOf("application/json")) },
            onClearFile = viewModel::clearSelectedFile,
            onRestoreModeChange = viewModel::setRestoreMode,
            onRestoreClick = viewModel::showRestoreConfirmation,
            onResultDismiss = viewModel::clearRestoreResult,
            formatDate = viewModel::formatDate,
            formatEntityCounts = viewModel::formatEntityCounts
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Restore confirmation dialog
    if (uiState.showRestoreConfirmation) {
        RestoreConfirmationDialog(
            metadata = uiState.backupMetadata,
            restoreMode = uiState.restoreMode,
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::dismissRestoreConfirmation,
            formatDate = viewModel::formatDate
        )
    }
}

@Composable
private fun InfoCard() {
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
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "데이터베이스 백업 및 복원",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "수집된 데이터를 백업하고 다른 기기에서 복원할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BackupSection(
    backupType: BackupType,
    startDate: String?,
    endDate: String?,
    isCreating: Boolean,
    progress: Float,
    message: String,
    result: BackupResultState?,
    onBackupTypeChange: (BackupType) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onClearStartDate: () -> Unit,
    onClearEndDate: () -> Unit,
    onCreateClick: () -> Unit,
    onResultDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "백업",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Backup type selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "백업 유형",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = backupType == BackupType.FULL,
                        onClick = { onBackupTypeChange(BackupType.FULL) },
                        enabled = !isCreating
                    )
                    Text(
                        text = "전체 백업",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = backupType == BackupType.FILTERED,
                        onClick = { onBackupTypeChange(BackupType.FILTERED) },
                        enabled = !isCreating
                    )
                    Text(
                        text = "기간 지정",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Date range (only for FILTERED)
                AnimatedVisibility(visible = backupType == BackupType.FILTERED) {
                    Column(
                        modifier = Modifier.padding(start = 40.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateSelector(
                            label = "시작일",
                            date = startDate,
                            enabled = !isCreating,
                            onClick = onStartDateClick,
                            onClear = onClearStartDate
                        )
                        DateSelector(
                            label = "종료일",
                            date = endDate,
                            enabled = !isCreating,
                            onClick = onEndDateClick,
                            onClear = onClearEndDate
                        )
                    }
                }
            }
        }

        // Progress
        AnimatedVisibility(visible = isCreating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Result
        AnimatedVisibility(visible = result != null && !isCreating) {
            result?.let { res ->
                ResultCard(
                    isSuccess = res is BackupResultState.Success,
                    message = when (res) {
                        is BackupResultState.Success -> "백업이 완료되었습니다: ${res.fileName}"
                        is BackupResultState.Error -> res.message
                    },
                    onDismiss = onResultDismiss
                )
            }
        }

        // Create backup button
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating
        ) {
            Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isCreating) "백업 중..." else "백업 생성")
        }
    }
}

@Composable
private fun RestoreSection(
    selectedFileName: String?,
    validationResult: ValidationResult?,
    metadata: BackupMetadata?,
    restoreMode: RestoreMode,
    isRestoring: Boolean,
    progress: Float,
    message: String,
    result: RestoreResultState?,
    onSelectFileClick: () -> Unit,
    onClearFile: () -> Unit,
    onRestoreModeChange: (RestoreMode) -> Unit,
    onRestoreClick: () -> Unit,
    onResultDismiss: () -> Unit,
    formatDate: (Long) -> String,
    formatEntityCounts: (Map<String, Int>) -> String
) {
    val extendedColors = LocalExtendedColors.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "복원",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // File selection
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedFileName == null) {
                    OutlinedButton(
                        onClick = onSelectFileClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRestoring
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("백업 파일 선택")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedFileName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearFile,
                            enabled = !isRestoring
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "파일 제거"
                            )
                        }
                    }

                    // Validation result
                    when (validationResult) {
                        is ValidationResult.Valid -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            metadata?.let { meta ->
                                BackupMetadataCard(
                                    metadata = meta,
                                    formatDate = formatDate,
                                    formatEntityCounts = formatEntityCounts
                                )
                            }
                        }
                        is ValidationResult.Invalid -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = validationResult.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        null -> {}
                    }
                }
            }
        }

        // Restore mode selection
        AnimatedVisibility(visible = validationResult is ValidationResult.Valid) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "복원 방식",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = restoreMode == RestoreMode.MERGE,
                            onClick = { onRestoreModeChange(RestoreMode.MERGE) },
                            enabled = !isRestoring
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "병합")
                            Text(
                                text = "기존 데이터를 유지하고 백업 데이터 추가",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = restoreMode == RestoreMode.REPLACE,
                            onClick = { onRestoreModeChange(RestoreMode.REPLACE) },
                            enabled = !isRestoring
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "교체")
                            Text(
                                text = "기존 데이터 삭제 후 백업 데이터로 교체",
                                style = MaterialTheme.typography.bodySmall,
                                color = extendedColors.danger
                            )
                        }
                    }
                }
            }
        }

        // Progress
        AnimatedVisibility(visible = isRestoring) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Result
        AnimatedVisibility(visible = result != null && !isRestoring) {
            result?.let { res ->
                ResultCard(
                    isSuccess = res is RestoreResultState.Success,
                    message = when (res) {
                        is RestoreResultState.Success -> {
                            val counts = res.result.restoredCounts.values.sum()
                            "복원이 완료되었습니다: ${counts}개 항목"
                        }
                        is RestoreResultState.Error -> res.message
                    },
                    onDismiss = onResultDismiss
                )
            }
        }

        // Restore button
        Button(
            onClick = onRestoreClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRestoring && validationResult is ValidationResult.Valid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isRestoring) "복원 중..." else "복원 시작")
        }
    }
}

@Composable
private fun DateSelector(
    label: String,
    date: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = date ?: "선택하세요")
        }
        if (date != null) {
            IconButton(onClick = onClear, enabled = enabled) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "날짜 제거"
                )
            }
        }
    }
}

@Composable
private fun BackupMetadataCard(
    metadata: BackupMetadata,
    formatDate: (Long) -> String,
    formatEntityCounts: (Map<String, Int>) -> String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = LocalExtendedColors.current.successContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = LocalExtendedColors.current.onSuccessContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "유효한 백업 파일",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LocalExtendedColors.current.onSuccessContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "생성일: ${formatDate(metadata.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.onSuccessContainer
            )
            Text(
                text = "백업 유형: ${if (metadata.backupType == BackupType.FULL) "전체" else "기간 지정"}",
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.onSuccessContainer
            )
            if (metadata.backupType == BackupType.FILTERED) {
                Text(
                    text = "기간: ${metadata.filterStartDate ?: "?"} ~ ${metadata.filterEndDate ?: "?"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.onSuccessContainer
                )
            }
            Text(
                text = formatEntityCounts(metadata.entityCounts),
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.onSuccessContainer
            )
        }
    }
}

@Composable
private fun ResultCard(
    isSuccess: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) {
                extendedColors.successContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isSuccess) {
                    extendedColors.onSuccessContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSuccess) {
                    extendedColors.onSuccessContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "닫기",
                    tint = if (isSuccess) {
                        extendedColors.onSuccessContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    }
}

@Composable
private fun RestoreConfirmationDialog(
    metadata: BackupMetadata?,
    restoreMode: RestoreMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    formatDate: (Long) -> String
) {
    val extendedColors = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (restoreMode == RestoreMode.REPLACE) {
                    Icons.Default.Warning
                } else {
                    Icons.Default.Restore
                },
                contentDescription = null,
                tint = if (restoreMode == RestoreMode.REPLACE) {
                    extendedColors.danger
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        },
        title = {
            Text(
                text = if (restoreMode == RestoreMode.REPLACE) {
                    "데이터 교체 확인"
                } else {
                    "데이터 복원 확인"
                }
            )
        },
        text = {
            Column {
                if (restoreMode == RestoreMode.REPLACE) {
                    Text(
                        text = "기존 데이터가 모두 삭제됩니다. 이 작업은 되돌릴 수 없습니다.",
                        color = extendedColors.danger,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                metadata?.let {
                    Text("백업 생성일: ${formatDate(it.createdAt)}")
                    Text("백업 항목: ${it.entityCounts.values.sum()}개")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("복원을 진행하시겠습니까?")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (restoreMode == RestoreMode.REPLACE) {
                    ButtonDefaults.buttonColors(
                        containerColor = extendedColors.danger
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text("복원")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun ShowDatePicker(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    DatePickerDialog(
        context,
        { _, year, month, day ->
            val date = String.format("%04d-%02d-%02d", year, month + 1, day)
            onDateSelected(date)
            onDismiss()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        setOnCancelListener { onDismiss() }
    }.show()
}
