package com.stockapp.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.feature.etf.domain.model.FilterType
import com.stockapp.feature.etf.domain.repo.EtfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for ETF settings tab.
 * Manages keyword filtering and data management.
 *
 * Note: Auto-collection scheduling has been moved to SchedulingVm.
 */
@HiltViewModel
class EtfSettingsVm @Inject constructor(
    private val etfRepository: EtfRepository
) : ViewModel() {

    // Main UI state
    private val _uiState = MutableStateFlow(EtfSettingsUiState())
    val uiState: StateFlow<EtfSettingsUiState> = _uiState.asStateFlow()

    // Dialog states
    private val _showAddKeywordDialog = MutableStateFlow(false)
    val showAddKeywordDialog: StateFlow<Boolean> = _showAddKeywordDialog.asStateFlow()

    private val _addKeywordType = MutableStateFlow(FilterType.INCLUDE)
    val addKeywordType: StateFlow<FilterType> = _addKeywordType.asStateFlow()

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()

    init {
        initializeKeywordsAndLoadData()
        observeKeywords()
    }

    private fun initializeKeywordsAndLoadData() {
        viewModelScope.launch {
            // Initialize default keywords if not exists
            etfRepository.initializeDefaultKeywords()

            // Load data statistics
            loadDataStatistics()

            // Load filter preview
            loadFilterPreview()
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

    // ==================== Settings ====================

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
            val retentionDays = _uiState.value.dataRetentionDays
            if (retentionDays == -1) {
                // Unlimited retention - no cleanup needed
                return@launch
            }

            val cutoffDate = LocalDate.now().minusDays(retentionDays.toLong())
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
}
