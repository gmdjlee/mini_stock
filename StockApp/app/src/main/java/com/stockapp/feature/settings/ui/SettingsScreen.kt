package com.stockapp.feature.settings.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockapp.core.theme.ThemeToggleButton
import com.stockapp.feature.scheduling.ui.SchedulingTab
import com.stockapp.feature.settings.domain.model.InvestmentMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsVm = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                actions = { ThemeToggleButton() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = SettingsTab.entries.indexOf(selectedTab),
                edgePadding = 16.dp
            ) {
                SettingsTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.title) }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                SettingsTab.API_KEY -> ApiKeyTab(viewModel)
                SettingsTab.SCHEDULING -> SchedulingTab()
            }
        }
    }
}

@Composable
private fun ApiKeyTab(viewModel: SettingsVm) {
    val appKey by viewModel.appKey.collectAsState()
    val secretKey by viewModel.secretKey.collectAsState()
    val investmentMode by viewModel.investmentMode.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var showAppKey by remember { mutableStateOf(false) }
    var showSecretKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info card
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
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "키움증권 API Key 설정",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "API Key와 Secret Key를 입력하고 저장하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // App Key input
        OutlinedTextField(
            value = appKey,
            onValueChange = { viewModel.updateAppKey(it) },
            label = { Text("App Key") },
            placeholder = { Text("앱 키를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showAppKey) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showAppKey = !showAppKey }) {
                    Icon(
                        imageVector = if (showAppKey) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (showAppKey) "숨기기" else "보기"
                    )
                }
            },
            enabled = !isSaving
        )

        // Secret Key input
        OutlinedTextField(
            value = secretKey,
            onValueChange = { viewModel.updateSecretKey(it) },
            label = { Text("Secret Key") },
            placeholder = { Text("시크릿 키를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showSecretKey) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showSecretKey = !showSecretKey }) {
                    Icon(
                        imageVector = if (showSecretKey) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (showSecretKey) "숨기기" else "보기"
                    )
                }
            },
            enabled = !isSaving
        )

        // Investment mode selection
        InvestmentModeSelector(
            selectedMode = investmentMode,
            onModeSelected = { viewModel.updateInvestmentMode(it) },
            enabled = !isSaving
        )

        // Save & Test button
        Button(
            onClick = { viewModel.saveAndTest() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && appKey.isNotBlank() && secretKey.isNotBlank()
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isSaving) "저장 및 테스트 중..." else "저장 및 연결 테스트"
            )
        }

        // Test result
        AnimatedVisibility(visible = testResult != TestResult.Idle) {
            TestResultCard(result = testResult)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InvestmentModeSelector(
    selectedMode: InvestmentMode,
    onModeSelected: (InvestmentMode) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "투자 구분",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            InvestmentMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == mode,
                        onClick = { onModeSelected(mode) },
                        enabled = enabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = when (mode) {
                            InvestmentMode.MOCK -> Icons.Default.Science
                            InvestmentMode.PRODUCTION -> Icons.AutoMirrored.Filled.TrendingUp
                        },
                        contentDescription = null,
                        tint = if (mode == InvestmentMode.PRODUCTION) {
                            Color(0xFFC62828)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == InvestmentMode.PRODUCTION) {
                                Color(0xFFC62828)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Warning for production mode
            AnimatedVisibility(visible = selectedMode == InvestmentMode.PRODUCTION) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "실전투자 모드입니다. 실제 거래가 발생할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestResultCard(result: TestResult) {
    val (containerColor, contentColor, icon, title, description) = when (result) {
        is TestResult.Idle -> return
        is TestResult.Testing -> TestResultInfo(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = null,
            title = "연결 테스트 중...",
            description = "API 서버에 연결을 시도하고 있습니다."
        )
        is TestResult.Success -> TestResultInfo(
            containerColor = Color(0xFFE8F5E9),
            contentColor = Color(0xFF2E7D32),
            icon = Icons.Default.CheckCircle,
            title = "연결 성공",
            description = "API Key가 정상적으로 확인되었습니다."
        )
        is TestResult.Failure -> TestResultInfo(
            containerColor = Color(0xFFFFEBEE),
            contentColor = Color(0xFFC62828),
            icon = Icons.Default.Error,
            title = "연결 실패",
            description = result.message
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

private data class TestResultInfo(
    val containerColor: Color,
    val contentColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val title: String,
    val description: String
)
