package com.stockapp.feature.scheduling.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stockapp.feature.etf.domain.model.CollectionHistory
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.repo.EtfRepository
import com.stockapp.feature.etf.worker.EtfCollectionWorker
import com.stockapp.feature.scheduling.SchedulingManager
import com.stockapp.feature.scheduling.SyncWorkState
import com.stockapp.feature.scheduling.domain.model.SchedulingConfig
import com.stockapp.feature.scheduling.domain.model.SyncHistory
import com.stockapp.feature.scheduling.domain.model.SyncStatus
import com.stockapp.feature.scheduling.domain.repo.SchedulingRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * ETF collection state for UI display.
 */
sealed class EtfCollectionState {
    data object Idle : EtfCollectionState()
    data class Collecting(val current: Int, val total: Int) : EtfCollectionState()
    data class Success(val etfCount: Int, val constituentCount: Int) : EtfCollectionState()
    data class Error(val message: String) : EtfCollectionState()
}

/**
 * ETF collection status for display.
 */
data class EtfCollectionStatus(
    val lastCollectionTime: Long? = null,
    val lastEtfCount: Int = 0,
    val lastConstituentCount: Int = 0,
    val lastStatus: CollectionStatus = CollectionStatus.SUCCESS,
    val collectionState: EtfCollectionState = EtfCollectionState.Idle
) {
    val lastCollectionTimeDisplay: String
        get() = lastCollectionTime?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(it))
        } ?: "없음"

    val hasCollectedData: Boolean
        get() = lastCollectionTime != null && lastEtfCount > 0
}

/**
 * UI State for Scheduling Screen.
 */
data class SchedulingUiState(
    val config: SchedulingConfig = SchedulingConfig(),
    val syncHistory: List<SyncHistory> = emptyList(),
    val syncWorkState: SyncWorkState = SyncWorkState.IDLE,
    val etfCollectionStatus: EtfCollectionStatus = EtfCollectionStatus(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isSyncing: Boolean
        get() = syncWorkState == SyncWorkState.RUNNING || syncWorkState == SyncWorkState.ENQUEUED

    val isEtfCollecting: Boolean
        get() = etfCollectionStatus.collectionState is EtfCollectionState.Collecting

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
    @ApplicationContext private val context: Context,
    private val schedulingRepo: SchedulingRepo,
    private val schedulingManager: SchedulingManager,
    private val etfRepository: EtfRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _etfCollectionStatus = MutableStateFlow(EtfCollectionStatus())

    val uiState: StateFlow<SchedulingUiState> = combine(
        schedulingRepo.observeConfig(),
        schedulingRepo.observeSyncHistory(),
        schedulingManager.observeSyncState(),
        _etfCollectionStatus,
        _isLoading,
        _error
    ) { flows ->
        val config = flows[0] as SchedulingConfig
        @Suppress("UNCHECKED_CAST")
        val history = flows[1] as List<SyncHistory>
        val syncState = flows[2] as SyncWorkState
        val etfStatus = flows[3] as EtfCollectionStatus
        val loading = flows[4] as Boolean
        val error = flows[5] as String?

        SchedulingUiState(
            config = config,
            syncHistory = history,
            syncWorkState = syncState,
            etfCollectionStatus = etfStatus,
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

        // Load ETF collection status
        loadEtfCollectionStatus()

        // Observe ETF collection work progress
        observeEtfCollectionProgress()
    }

    private fun loadEtfCollectionStatus() {
        viewModelScope.launch {
            etfRepository.getRecentHistory(1).fold(
                onSuccess = { historyList ->
                    val latest = historyList.firstOrNull()
                    if (latest != null) {
                        _etfCollectionStatus.value = EtfCollectionStatus(
                            lastCollectionTime = latest.completedAt ?: latest.startedAt,
                            lastEtfCount = latest.totalEtfs,
                            lastConstituentCount = latest.totalConstituents,
                            lastStatus = latest.status,
                            collectionState = EtfCollectionState.Idle
                        )
                    }
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    private fun observeEtfCollectionProgress() {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(EtfCollectionWorker.WORK_NAME_ONCE)
                .collect { workInfos ->
                    val workInfo = workInfos.firstOrNull() ?: return@collect

                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val current = workInfo.progress.getInt(EtfCollectionWorker.KEY_PROGRESS_CURRENT, 0)
                            val total = workInfo.progress.getInt(EtfCollectionWorker.KEY_PROGRESS_TOTAL, 0)
                            _etfCollectionStatus.value = _etfCollectionStatus.value.copy(
                                collectionState = EtfCollectionState.Collecting(current, total)
                            )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val etfCount = workInfo.outputData.getInt(EtfCollectionWorker.KEY_RESULT_ETF_COUNT, 0)
                            val constituentCount = workInfo.outputData.getInt(EtfCollectionWorker.KEY_RESULT_CONSTITUENT_COUNT, 0)
                            _etfCollectionStatus.value = _etfCollectionStatus.value.copy(
                                lastCollectionTime = System.currentTimeMillis(),
                                lastEtfCount = etfCount,
                                lastConstituentCount = constituentCount,
                                lastStatus = CollectionStatus.SUCCESS,
                                collectionState = EtfCollectionState.Success(etfCount, constituentCount)
                            )
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(EtfCollectionWorker.KEY_RESULT_ERROR)
                                ?: "수집 실패"
                            _etfCollectionStatus.value = _etfCollectionStatus.value.copy(
                                collectionState = EtfCollectionState.Error(error)
                            )
                        }
                        WorkInfo.State.CANCELLED -> {
                            _etfCollectionStatus.value = _etfCollectionStatus.value.copy(
                                collectionState = EtfCollectionState.Idle
                            )
                        }
                        else -> {}
                    }
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

    /**
     * Trigger ETF data re-collection.
     */
    fun triggerEtfCollection() {
        viewModelScope.launch {
            _etfCollectionStatus.value = _etfCollectionStatus.value.copy(
                collectionState = EtfCollectionState.Collecting(0, 0)
            )

            // Build filter config from enabled keywords
            val keywords = etfRepository.getEnabledKeywords().getOrDefault(emptyList())
            val includeKeywords = keywords
                .filter { it.filterType.value == "INCLUDE" }
                .map { it.keyword }
            val excludeKeywords = keywords
                .filter { it.filterType.value == "EXCLUDE" }
                .map { it.keyword }

            val filterConfig = EtfFilterConfig(
                activeOnly = true,
                includeKeywords = includeKeywords.ifEmpty { EtfFilterConfig.DEFAULT_INCLUDE_KEYWORDS },
                excludeKeywords = excludeKeywords.ifEmpty { EtfFilterConfig.DEFAULT_EXCLUDE_KEYWORDS }
            )

            // Start background worker
            EtfCollectionWorker.collectNow(context, filterConfig)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
