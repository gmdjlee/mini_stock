package com.stockapp.feature.settings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.core.backup.BackupConfig
import com.stockapp.core.backup.BackupMetadata
import com.stockapp.core.backup.BackupProgress
import com.stockapp.core.backup.BackupType
import com.stockapp.core.backup.RestoreMode
import com.stockapp.core.backup.RestoreProgress
import com.stockapp.core.backup.RestoreResult
import com.stockapp.core.backup.ValidationResult
import com.stockapp.feature.settings.domain.usecase.CreateBackupUC
import com.stockapp.feature.settings.domain.usecase.RestoreBackupUC
import com.stockapp.feature.settings.domain.usecase.ValidateBackupUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * UI State for DB Backup tab.
 */
data class DbBackupUiState(
    // Backup settings
    val backupType: BackupType = BackupType.FULL,
    val startDate: String? = null,
    val endDate: String? = null,

    // Backup progress
    val isCreating: Boolean = false,
    val backupProgress: Float = 0f,
    val backupMessage: String = "",

    // Restore settings
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val validationResult: ValidationResult? = null,
    val backupMetadata: BackupMetadata? = null,
    val restoreMode: RestoreMode = RestoreMode.MERGE,

    // Restore progress
    val isRestoring: Boolean = false,
    val restoreProgress: Float = 0f,
    val restoreMessage: String = "",

    // Dialogs
    val showRestoreConfirmation: Boolean = false,

    // Results
    val lastBackupResult: BackupResultState? = null,
    val lastRestoreResult: RestoreResultState? = null
)

sealed class BackupResultState {
    data class Success(val fileName: String) : BackupResultState()
    data class Error(val message: String) : BackupResultState()
}

sealed class RestoreResultState {
    data class Success(val result: RestoreResult) : RestoreResultState()
    data class Error(val message: String) : RestoreResultState()
}

@HiltViewModel
class DbBackupVm @Inject constructor(
    private val createBackupUC: CreateBackupUC,
    private val restoreBackupUC: RestoreBackupUC,
    private val validateBackupUC: ValidateBackupUC
) : ViewModel() {

    private val _uiState = MutableStateFlow(DbBackupUiState())
    val uiState: StateFlow<DbBackupUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ============================================================
    // Backup Methods
    // ============================================================

    fun setBackupType(type: BackupType) {
        _uiState.update { it.copy(backupType = type) }
    }

    fun setStartDate(date: String?) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun setEndDate(date: String?) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun createBackup(outputUri: Uri) {
        val state = _uiState.value
        val config = BackupConfig(
            backupType = state.backupType,
            startDate = state.startDate,
            endDate = state.endDate
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreating = true,
                    backupProgress = 0f,
                    backupMessage = "백업 준비 중...",
                    lastBackupResult = null
                )
            }

            createBackupUC(config, outputUri) { progress ->
                when (progress) {
                    is BackupProgress.Creating -> {
                        _uiState.update {
                            it.copy(
                                backupProgress = progress.progress,
                                backupMessage = progress.message
                            )
                        }
                    }
                    is BackupProgress.Saving -> {
                        _uiState.update {
                            it.copy(
                                backupProgress = 0.95f,
                                backupMessage = "파일 저장 중..."
                            )
                        }
                    }
                    is BackupProgress.Complete -> {
                        _uiState.update {
                            it.copy(
                                backupProgress = 1f,
                                backupMessage = "완료"
                            )
                        }
                    }
                }
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            lastBackupResult = BackupResultState.Success(
                                getFileNameFromUri(outputUri)
                            )
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            lastBackupResult = BackupResultState.Error(
                                e.message ?: "백업 생성 실패"
                            )
                        )
                    }
                }
            )
        }
    }

    // ============================================================
    // Restore Methods
    // ============================================================

    fun selectRestoreFile(uri: Uri, fileName: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedFileUri = uri,
                    selectedFileName = fileName ?: getFileNameFromUri(uri),
                    validationResult = null,
                    backupMetadata = null,
                    lastRestoreResult = null
                )
            }

            // Validate the file
            validateBackupUC(uri).fold(
                onSuccess = { result ->
                    _uiState.update { state ->
                        state.copy(
                            validationResult = result,
                            backupMetadata = (result as? ValidationResult.Valid)?.metadata
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { state ->
                        state.copy(
                            validationResult = ValidationResult.Invalid(
                                e.message ?: "파일 검증 실패"
                            )
                        )
                    }
                }
            )
        }
    }

    fun setRestoreMode(mode: RestoreMode) {
        _uiState.update { it.copy(restoreMode = mode) }
    }

    fun showRestoreConfirmation() {
        _uiState.update { it.copy(showRestoreConfirmation = true) }
    }

    fun dismissRestoreConfirmation() {
        _uiState.update { it.copy(showRestoreConfirmation = false) }
    }

    fun confirmRestore() {
        val state = _uiState.value
        val uri = state.selectedFileUri ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showRestoreConfirmation = false,
                    isRestoring = true,
                    restoreProgress = 0f,
                    restoreMessage = "복원 준비 중...",
                    lastRestoreResult = null
                )
            }

            restoreBackupUC(uri, state.restoreMode) { progress ->
                when (progress) {
                    is RestoreProgress.Loading -> {
                        _uiState.update {
                            it.copy(
                                restoreProgress = 0.1f,
                                restoreMessage = "파일 로딩 중..."
                            )
                        }
                    }
                    is RestoreProgress.Validating -> {
                        _uiState.update {
                            it.copy(
                                restoreProgress = 0.15f,
                                restoreMessage = "파일 검증 중..."
                            )
                        }
                    }
                    is RestoreProgress.Migrating -> {
                        _uiState.update {
                            it.copy(
                                restoreProgress = 0.2f,
                                restoreMessage = "데이터 변환 중..."
                            )
                        }
                    }
                    is RestoreProgress.Restoring -> {
                        _uiState.update {
                            it.copy(
                                restoreProgress = 0.2f + (progress.progress * 0.75f),
                                restoreMessage = progress.message
                            )
                        }
                    }
                    is RestoreProgress.Complete -> {
                        _uiState.update {
                            it.copy(
                                restoreProgress = 1f,
                                restoreMessage = "완료"
                            )
                        }
                    }
                }
            }.fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            lastRestoreResult = if (result.success) {
                                RestoreResultState.Success(result)
                            } else {
                                RestoreResultState.Error(result.errorMessage ?: "복원 실패")
                            }
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            lastRestoreResult = RestoreResultState.Error(
                                e.message ?: "복원 실패"
                            )
                        )
                    }
                }
            )
        }
    }

    fun clearSelectedFile() {
        _uiState.update {
            it.copy(
                selectedFileUri = null,
                selectedFileName = null,
                validationResult = null,
                backupMetadata = null,
                lastRestoreResult = null
            )
        }
    }

    // ============================================================
    // Result Methods
    // ============================================================

    fun clearBackupResult() {
        _uiState.update { it.copy(lastBackupResult = null) }
    }

    fun clearRestoreResult() {
        _uiState.update { it.copy(lastRestoreResult = null) }
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private fun getFileNameFromUri(uri: Uri): String {
        return uri.lastPathSegment?.substringAfterLast('/') ?: "backup.json"
    }

    fun generateBackupFileName(): String {
        val timestamp = dateFormat.format(Date())
        val typeStr = when (_uiState.value.backupType) {
            BackupType.FULL -> "full"
            BackupType.FILTERED -> "filtered"
        }
        return "stockapp_${typeStr}_$timestamp.json"
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatEntityCounts(counts: Map<String, Int>): String {
        val totalCount = counts.values.sum()
        return "총 ${totalCount}개 항목"
    }
}
