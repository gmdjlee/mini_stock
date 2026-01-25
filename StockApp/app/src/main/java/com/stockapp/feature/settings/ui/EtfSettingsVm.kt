package com.stockapp.feature.settings.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stockapp.core.db.entity.EtfCollectionHistoryEntity
import com.stockapp.feature.etf.domain.model.CollectionStatus
import com.stockapp.feature.etf.domain.model.EtfFilterConfig
import com.stockapp.feature.etf.domain.model.EtfKeyword
import com.stockapp.feature.etf.domain.model.FilterType
import com.stockapp.feature.etf.domain.repo.EtfCollectorRepo
import com.stockapp.feature.etf.domain.repo.EtfRepository
import com.stockapp.feature.etf.worker.EtfCollectionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EtfSettingsVm @Inject constructor(
    @ApplicationContext private val context: Context,
    private val etfRepository: EtfRepository,
    private val etfCollectorRepo: EtfCollectorRepo
) : ViewModel() {

    // Main UI state
    private val _uiState = MutableStateFlow(EtfSettingsUiState())
    val uiState: StateFlow<EtfSettingsUiState> = _uiState.asStateFlow()

    // Dialog states
    private val _showTimePicker = MutableStateFlow(false)
    val showTimePicker: StateFlow<Boolean> = _showTimePicker.asStateFlow()

    private val _showAddKeywordDialog = MutableStateFlow(false)
    val showAddKeywordDialog: StateFlow<Boolean> = _showAddKeywordDialog.asStateFlow()

    private val _addKeywordType = MutableStateFlow(FilterType.INCLUDE)
    val addKeywordType: StateFlow<FilterType> = _addKeywordType.asStateFlow()

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()

    init {
        loadInitialData()
        observeKeywords()
        observeCollectionHistory()
        observeWorkProgress()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load data statistics
            loadDataStatistics()

            // Load filter preview
            loadFilterPreview()

            // Get last collection time from history
            etfRepository.getRecentHistory(1).fold(
                onSuccess = { history ->
                    val latest = history.firstOrNull()
                    _uiState.update { it.copy(lastCollectionTime = latest?.completedAt) }
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    private fun observeKeywords() {
        viewModelScope.launch {
            etfRepository.observeEnabledKeywords().collect { keywords ->
                val includeKeywords = keywords.filter { it.filterType == FilterType.INCLUDE }
                val excludeKeywords = keywords.filter { it.filterType == FilterType.EXCLUDE }
                _uiState.update {
                    it.copy(
                        includeKeywords = includeKeywords,
                        excludeKeywords = excludeKeywords
                    )
                }
            }
        }
    }

    private fun observeCollectionHistory() {
        viewModelScope.launch {
            etfCollectorRepo.observeCollectionHistory(10).collect { historyList ->
                _uiState.update {
                    it.copy(collectionHistory = historyList.map { entity -> entity.toUiItem() })
                }
            }
        }
    }

    private fun loadDataStatistics() {
        viewModelScope.launch {
            // Get data date range from repository
            etfRepository.getDataDateRange().fold(
                onSuccess = { range ->
                    if (range.startDate != null && range.endDate != null) {
                        _uiState.update { it.copy(dataDateRange = "${range.startDate} ~ ${range.endDate}") }
                    }
                },
                onFailure = { /* ignore */ }
            )

            // Get constituent count (estimate based on dates)
            etfRepository.getCollectionDates().fold(
                onSuccess = { dates ->
                    _uiState.update { it.copy(totalRecordCount = dates.size * 100) } // Rough estimate
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun loadFilterPreview() {
        viewModelScope.launch {
            // Get all ETFs
            etfRepository.getAllEtfs().fold(
                onSuccess = { allEtfs ->
                    val currentState = _uiState.value
                    val includeKeywords = currentState.includeKeywords.map { it.keyword }
                    val excludeKeywords = currentState.excludeKeywords.map { it.keyword }

                    val filtered = allEtfs.filter { etf ->
                        // Active only filter
                        if (currentState.activeOnly && etf.etfType.value != "Active") {
                            return@filter false
                        }

                        // Include keywords filter (if any include keywords are set, ETF must match at least one)
                        if (includeKeywords.isNotEmpty()) {
                            val matchesInclude = includeKeywords.any { keyword ->
                                etf.etfName.contains(keyword, ignoreCase = true)
                            }
                            if (!matchesInclude) return@filter false
                        }

                        // Exclude keywords filter
                        val matchesExclude = excludeKeywords.any { keyword ->
                            etf.etfName.contains(keyword, ignoreCase = true)
                        }
                        if (matchesExclude) return@filter false

                        true
                    }

                    _uiState.update {
                        it.copy(
                            totalEtfCount = allEtfs.size,
                            filteredEtfCount = filtered.size
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            filteredEtfCount = 0,
                            totalEtfCount = 0
                        )
                    }
                }
            )
        }
    }

    // ==================== Collection ====================

    fun startManualCollection() {
        viewModelScope.launch {
            _uiState.update { it.copy(collectionState = CollectionUiState.Collecting(0, 0)) }

            val currentState = _uiState.value
            val filterConfig = EtfFilterConfig(
                activeOnly = currentState.activeOnly,
                includeKeywords = currentState.includeKeywords.map { it.keyword },
                excludeKeywords = currentState.excludeKeywords.map { it.keyword }
            )

            // Start background worker
            EtfCollectionWorker.collectNow(context, filterConfig)
        }
    }

    private fun observeWorkProgress() {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(EtfCollectionWorker.WORK_NAME_ONCE)
                .collect { workInfos ->
                    val workInfo = workInfos.firstOrNull() ?: return@collect

                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val current = workInfo.progress.getInt(EtfCollectionWorker.KEY_PROGRESS_CURRENT, 0)
                            val total = workInfo.progress.getInt(EtfCollectionWorker.KEY_PROGRESS_TOTAL, 0)
                            _uiState.update { it.copy(collectionState = CollectionUiState.Collecting(current, total)) }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val etfCount = workInfo.outputData.getInt(EtfCollectionWorker.KEY_RESULT_ETF_COUNT, 0)
                            val constituentCount = workInfo.outputData.getInt(EtfCollectionWorker.KEY_RESULT_CONSTITUENT_COUNT, 0)
                            _uiState.update {
                                it.copy(
                                    collectionState = CollectionUiState.Success(etfCount, constituentCount),
                                    lastCollectionTime = System.currentTimeMillis()
                                )
                            }
                            // Reload statistics
                            loadDataStatistics()
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(EtfCollectionWorker.KEY_RESULT_ERROR)
                                ?: "수집 실패"
                            _uiState.update { it.copy(collectionState = CollectionUiState.Error(error)) }
                        }
                        WorkInfo.State.CANCELLED -> {
                            _uiState.update { it.copy(collectionState = CollectionUiState.Idle) }
                        }
                        else -> {}
                    }
                }
        }
    }

    // ==================== Settings ====================

    fun setAutoCollectionEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAutoCollectionEnabled = enabled) }

        if (enabled) {
            val currentState = _uiState.value
            EtfCollectionWorker.scheduleDailyCollection(
                context = context,
                hour = currentState.collectionHour,
                minute = currentState.collectionMinute,
                filterConfig = EtfFilterConfig(
                    activeOnly = currentState.activeOnly,
                    includeKeywords = currentState.includeKeywords.map { it.keyword },
                    excludeKeywords = currentState.excludeKeywords.map { it.keyword }
                )
            )
        } else {
            EtfCollectionWorker.cancelScheduledCollection(context)
        }
    }

    fun setCollectionTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(collectionHour = hour, collectionMinute = minute) }
        hideTimePicker()

        // Reschedule if enabled
        if (_uiState.value.isAutoCollectionEnabled) {
            setAutoCollectionEnabled(true)
        }
    }

    fun setActiveOnly(activeOnly: Boolean) {
        _uiState.update { it.copy(activeOnly = activeOnly) }
        loadFilterPreview()
    }

    fun setDataRetentionDays(days: Int) {
        _uiState.update { it.copy(dataRetentionDays = days) }
    }

    // ==================== Keywords ====================

    fun addKeyword(keyword: String) {
        viewModelScope.launch {
            val type = _addKeywordType.value
            etfRepository.addKeyword(keyword.trim(), type).fold(
                onSuccess = {
                    hideAddKeywordDialog()
                    loadFilterPreview()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message ?: "키워드 추가 실패") }
                }
            )
        }
    }

    fun deleteKeyword(id: Long) {
        viewModelScope.launch {
            etfRepository.deleteKeyword(id).fold(
                onSuccess = { loadFilterPreview() },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message ?: "키워드 삭제 실패") }
                }
            )
        }
    }

    // ==================== Data Management ====================

    fun cleanupOldData() {
        viewModelScope.launch {
            val cutoffDate = LocalDate.now().minusDays(_uiState.value.dataRetentionDays.toLong())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            etfRepository.deleteOldConstituents(cutoffDate).fold(
                onSuccess = {
                    loadDataStatistics()
                    _uiState.update { it.copy(error = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message ?: "데이터 정리 실패") }
                }
            )
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            // Delete all constituents
            etfRepository.deleteAllConstituents().fold(
                onSuccess = {
                    // Also delete all keywords
                    etfRepository.deleteAllKeywords()
                    // Reload statistics
                    loadDataStatistics()
                    loadFilterPreview()
                    _uiState.update { it.copy(error = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message ?: "데이터 초기화 실패") }
                }
            )
        }
    }

    // ==================== Dialog Management ====================

    fun showTimePicker() {
        _showTimePicker.value = true
    }

    fun hideTimePicker() {
        _showTimePicker.value = false
    }

    fun showAddKeywordDialog(type: FilterType) {
        _addKeywordType.value = type
        _showAddKeywordDialog.value = true
    }

    fun hideAddKeywordDialog() {
        _showAddKeywordDialog.value = false
    }

    fun showDeleteConfirmDialog() {
        _showDeleteConfirmDialog.value = true
    }

    fun hideDeleteConfirmDialog() {
        _showDeleteConfirmDialog.value = false
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ==================== Helpers ====================

    private fun EtfCollectionHistoryEntity.toUiItem(): EtfCollectionHistoryUiItem {
        val duration = completedAt?.let { (it - startedAt) / 1000 }
        return EtfCollectionHistoryUiItem(
            id = id,
            collectionType = if (status == CollectionStatus.IN_PROGRESS.value) "진행 중" else "수동",
            status = CollectionStatus.fromValue(status),
            etfCount = totalEtfs,
            constituentCount = totalConstituents,
            errorMessage = errorMessage,
            startedAt = startedAt,
            completedAt = completedAt,
            durationDisplay = duration?.let {
                if (it >= 60) "${it / 60}분 ${it % 60}초" else "${it}초"
            }
        )
    }
}
