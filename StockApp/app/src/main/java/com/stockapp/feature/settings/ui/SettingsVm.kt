package com.stockapp.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockapp.feature.settings.domain.model.ApiKeyConfig
import com.stockapp.feature.settings.domain.model.InvestmentMode
import com.stockapp.feature.settings.domain.model.KisApiKeyConfig
import com.stockapp.feature.settings.domain.repo.SettingsRepo
import com.stockapp.feature.settings.domain.usecase.GetApiKeyConfigUC
import com.stockapp.feature.settings.domain.usecase.SaveApiKeyConfigUC
import com.stockapp.feature.settings.domain.usecase.TestApiKeyUC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings tab types.
 */
enum class SettingsTab(val title: String) {
    API_KEY("키움 API"),
    KIS_API("KIS API"),
    SCHEDULING("스케줄링"),
    ETF_STATISTICS("ETF 통계")
}

/**
 * API Key test result.
 */
sealed class TestResult {
    data object Idle : TestResult()
    data object Testing : TestResult()
    data object Success : TestResult()
    data class Failure(val message: String) : TestResult()
}

@HiltViewModel
class SettingsVm @Inject constructor(
    private val getApiKeyConfigUC: GetApiKeyConfigUC,
    private val saveApiKeyConfigUC: SaveApiKeyConfigUC,
    private val testApiKeyUC: TestApiKeyUC,
    private val settingsRepo: SettingsRepo
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(SettingsTab.API_KEY)
    val selectedTab: StateFlow<SettingsTab> = _selectedTab.asStateFlow()

    // Kiwoom API states
    private val _appKey = MutableStateFlow("")
    val appKey: StateFlow<String> = _appKey.asStateFlow()

    private val _secretKey = MutableStateFlow("")
    val secretKey: StateFlow<String> = _secretKey.asStateFlow()

    private val _investmentMode = MutableStateFlow(InvestmentMode.MOCK)
    val investmentMode: StateFlow<InvestmentMode> = _investmentMode.asStateFlow()

    private val _testResult = MutableStateFlow<TestResult>(TestResult.Idle)
    val testResult: StateFlow<TestResult> = _testResult.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // KIS API states
    private val _kisAppKey = MutableStateFlow("")
    val kisAppKey: StateFlow<String> = _kisAppKey.asStateFlow()

    private val _kisAppSecret = MutableStateFlow("")
    val kisAppSecret: StateFlow<String> = _kisAppSecret.asStateFlow()

    private val _kisTestResult = MutableStateFlow<TestResult>(TestResult.Idle)
    val kisTestResult: StateFlow<TestResult> = _kisTestResult.asStateFlow()

    private val _isKisSaving = MutableStateFlow(false)
    val isKisSaving: StateFlow<Boolean> = _isKisSaving.asStateFlow()

    init {
        loadConfig()
        loadKisConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            getApiKeyConfigUC().collect { config ->
                _appKey.value = config.appKey
                _secretKey.value = config.secretKey
                _investmentMode.value = config.investmentMode
            }
        }
    }

    private fun loadKisConfig() {
        viewModelScope.launch {
            settingsRepo.getKisApiKeyConfig().collect { config ->
                _kisAppKey.value = config.appKey
                _kisAppSecret.value = config.appSecret
            }
        }
    }

    fun selectTab(tab: SettingsTab) {
        _selectedTab.value = tab
    }

    fun updateAppKey(value: String) {
        _appKey.value = value
        resetStates()
    }

    fun updateSecretKey(value: String) {
        _secretKey.value = value
        resetStates()
    }

    fun updateInvestmentMode(mode: InvestmentMode) {
        _investmentMode.value = mode
        resetStates()
    }

    private fun resetStates() {
        _testResult.value = TestResult.Idle
        _saveSuccess.value = false
    }

    fun saveAndTest() {
        val config = ApiKeyConfig(
            appKey = _appKey.value.trim(),
            secretKey = _secretKey.value.trim(),
            investmentMode = _investmentMode.value
        )

        if (!config.isValid()) {
            _testResult.value = TestResult.Failure("API Key와 Secret Key를 입력해주세요")
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _testResult.value = TestResult.Testing

            try {
                // Save first
                saveApiKeyConfigUC(config)

                // Then test
                testApiKeyUC(config).fold(
                    onSuccess = {
                        _testResult.value = TestResult.Success
                        _saveSuccess.value = true
                    },
                    onFailure = { e ->
                        _testResult.value = TestResult.Failure(
                            e.message ?: "API 연결 테스트에 실패했습니다"
                        )
                    }
                )
            } catch (e: Exception) {
                _testResult.value = TestResult.Failure(
                    e.message ?: "저장 중 오류가 발생했습니다"
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun dismissSaveSuccess() {
        _saveSuccess.value = false
    }

    // ============================================================
    // KIS API Methods
    // ============================================================

    fun updateKisAppKey(value: String) {
        _kisAppKey.value = value
        resetKisStates()
    }

    fun updateKisAppSecret(value: String) {
        _kisAppSecret.value = value
        resetKisStates()
    }

    private fun resetKisStates() {
        _kisTestResult.value = TestResult.Idle
    }

    fun saveAndTestKis() {
        val config = KisApiKeyConfig(
            appKey = _kisAppKey.value.trim(),
            appSecret = _kisAppSecret.value.trim()
        )

        if (!config.isValid()) {
            _kisTestResult.value = TestResult.Failure("KIS App Key와 App Secret을 입력해주세요")
            return
        }

        viewModelScope.launch {
            _isKisSaving.value = true
            _kisTestResult.value = TestResult.Testing

            try {
                // Save first
                settingsRepo.saveKisApiKeyConfig(config)

                // Then test
                settingsRepo.testKisApiKey(config).fold(
                    onSuccess = {
                        _kisTestResult.value = TestResult.Success
                    },
                    onFailure = { e ->
                        _kisTestResult.value = TestResult.Failure(
                            e.message ?: "KIS API 연결 테스트에 실패했습니다"
                        )
                    }
                )
            } catch (e: Exception) {
                _kisTestResult.value = TestResult.Failure(
                    e.message ?: "저장 중 오류가 발생했습니다"
                )
            } finally {
                _isKisSaving.value = false
            }
        }
    }
}
