package com.stockapp.feature.scheduling.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.feature.scheduling.SchedulingManager
import com.stockapp.feature.scheduling.SyncWorkState
import com.stockapp.feature.scheduling.domain.model.SchedulingConfig
import com.stockapp.feature.scheduling.domain.model.SyncHistory
import com.stockapp.feature.scheduling.domain.model.SyncStatus
import com.stockapp.feature.scheduling.domain.repo.SchedulingRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * UI State for Scheduling Screen.
 */
data class SchedulingUiState(
    val config: SchedulingConfig = SchedulingConfig(),
    val syncHistory: List<SyncHistory> = emptyList(),
    val syncWorkState: SyncWorkState = SyncWorkState.IDLE,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isSyncing: Boolean
        get() = syncWorkState == SyncWorkState.RUNNING || syncWorkState == SyncWorkState.ENQUEUED

    val lastSyncDisplay: String
        get() = if (config.lastSyncAt > 0) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
            dateFormat.format(Date(config.lastSyncAt))
        } else {
            "없음"
        }

    val lastSyncStatusDisplay: String
        get() = when (config.lastSyncStatus) {
            SyncStatus.NEVER -> "동기화 기록 없음"
            SyncStatus.SUCCESS -> "성공"
            SyncStatus.FAILED -> "실패: ${config.lastSyncMessage ?: "알 수 없는 오류"}"
            SyncStatus.IN_PROGRESS -> "진행 중..."
        }
}

@HiltViewModel
class SchedulingVm @Inject constructor(
    private val schedulingRepo: SchedulingRepo,
    private val schedulingManager: SchedulingManager
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<SchedulingUiState> = combine(
        schedulingRepo.observeConfig(),
        schedulingRepo.observeSyncHistory(),
        schedulingManager.observeSyncState(),
        _isLoading,
        _error
    ) { config, history, syncState, loading, error ->
        SchedulingUiState(
            config = config,
            syncHistory = history,
            syncWorkState = syncState,
            isLoading = loading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchedulingUiState()
    )

    private val _showTimePicker = MutableStateFlow(false)
    val showTimePicker: StateFlow<Boolean> = _showTimePicker.asStateFlow()

    init {
        // Initialize scheduling if enabled
        viewModelScope.launch {
            val config = schedulingRepo.getConfig()
            if (config.isEnabled) {
                schedulingManager.scheduleDailySync(config.syncHour, config.syncMinute)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                schedulingRepo.setEnabled(enabled)
                val config = schedulingRepo.getConfig()
                if (enabled) {
                    schedulingManager.scheduleDailySync(config.syncHour, config.syncMinute)
                } else {
                    schedulingManager.cancelDailySync()
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun setSyncTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                schedulingRepo.setSyncTime(hour, minute)
                val config = schedulingRepo.getConfig()
                if (config.isEnabled) {
                    schedulingManager.scheduleDailySync(hour, minute)
                }
                _showTimePicker.value = false
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun showTimePicker() {
        _showTimePicker.value = true
    }

    fun hideTimePicker() {
        _showTimePicker.value = false
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                schedulingManager.triggerImmediateSync()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
