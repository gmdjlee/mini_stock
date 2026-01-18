# Stock Input Autocomplete Module - StockApp 개발 명세서

**Version**: 1.1 (StockApp Adapted)
**Created**: 2026-01-18
**Updated**: 2026-01-18
**Target Project**: StockApp (mini_stock)

---

## 1. 모듈 개요

### 1.1 목표
StockApp의 **자동완성 기능이 포함된 종목 입력 컴포넌트**를 독립적인 재사용 가능 모듈로 구현하여, SearchScreen뿐만 아니라 Analysis, Indicator 등 다른 화면에서도 쉽게 사용할 수 있도록 함.

### 1.2 현재 구현 현황

| 항목 | 현재 상태 |
|------|----------|
| 메인 화면 | `SearchScreen.kt` (전체 화면 방식) |
| 사용 화면 | 1개 (Search) |
| 검색 히스토리 | Room DB 저장 (SearchHistoryEntity) |
| 검색 소스 | PyClient → Python API (`stock_analyzer.stock.search`) |

### 1.3 핵심 기능

| # | 기능 | 설명 | 우선순위 |
|---|------|------|----------|
| 1 | 자동완성 검색 | 종목명/코드 입력 시 드롭다운 결과 표시 | P0 |
| 2 | 검색 히스토리 | 최근 검색 기록 저장 및 표시 | P0 |
| 3 | 디바운싱 | 입력 시 300ms 지연 후 검색 실행 | P0 |
| 4 | 선택 콜백 | 종목 선택 시 Stock 객체 반환 | P0 |
| 5 | 커스텀 스타일링 | Material 3 테마 호환 | P1 |
| 6 | 인라인 사용 | 다른 화면에 삽입 가능한 컴포넌트 | P1 |

### 1.4 모듈 설계 원칙

```
✓ Composable 단독 사용 가능 (ViewModel 의존성 최소화)
✓ 검색 로직 외부 주입 (콜백 기반) 또는 내장 State 사용
✓ 기존 SearchRepo/PyClient 재사용
✓ Material 3 테마 호환
✓ 기존 Stock, SearchHistoryEntity 모델 활용
✓ 최소 의존성 (Compose Material 3 + Room)
```

---

## 2. StockApp 아키텍처 분석

### 2.1 현재 검색 구조

```
feature/search/
├── ui/
│   ├── SearchScreen.kt      # 전체 화면 (Scaffold 기반)
│   └── SearchVm.kt          # ViewModel (debounce 300ms)
├── domain/
│   ├── model/Stock.kt       # Stock, Market enum
│   ├── repo/SearchRepo.kt   # 검색 Repository 인터페이스
│   └── usecase/
│       ├── SearchStockUC.kt
│       └── SaveHistoryUC.kt
├── data/
│   └── repo/SearchRepoImpl.kt  # PyClient 호출
└── di/
    └── SearchModule.kt
```

### 2.2 데이터 흐름

```
User Input
    ↓
SearchVm (debounce 300ms)
    ↓
SearchStockUC
    ↓
SearchRepoImpl
    ↓
PyClient.call("stock_analyzer.stock.search", "search", [query])
    ↓
Python API → 키움증권 REST API
    ↓
SearchResponse (JSON)
    ↓
List<Stock>
```

### 2.3 기존 데이터 모델

```kotlin
// domain/model/Stock.kt
data class Stock(
    val ticker: String,
    val name: String,
    val market: Market
)

enum class Market {
    KOSPI, KOSDAQ, OTHER
}

// core/db/entity/StockEntity.kt
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticker: String,
    val name: String,
    val searchedAt: Long = System.currentTimeMillis()
)
```

### 2.4 현재 UI 구조

```
SearchScreen (Scaffold)
├── TopAppBar ("종목 검색")
└── Column
    ├── SearchBar (OutlinedTextField)
    └── Content (state-based)
        ├── Idle → HistoryList
        ├── Loading → CircularProgressIndicator
        ├── Results → StockList
        └── Error → ErrorState
```

---

## 3. 모듈 설계

### 3.1 모듈 구조

```
core/ui/component/stockinput/
├── StockInputField.kt              # 메인 Composable
├── StockInputHistoryDialog.kt      # 히스토리 다이얼로그
├── StockInputDefaults.kt           # 기본값 및 스타일
├── model/
│   └── StockInputModels.kt         # 데이터 모델
└── state/
    └── StockInputState.kt          # 상태 관리 (선택적)
```

### 3.2 핵심 API 설계

#### 3.2.1 기본 사용 (Stateless)

```kotlin
@Composable
fun StockInputField(
    // 필수
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<Stock>,
    onSelect: (Stock) -> Unit,

    // 선택
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    placeholder: String = "종목명 또는 코드 검색",

    // 히스토리 (선택)
    history: List<Stock> = emptyList(),
    onHistorySelect: ((Stock) -> Unit)? = null,
    onHistoryClick: (() -> Unit)? = null,  // 히스토리 버튼 클릭

    // 커스터마이징
    colors: StockInputColors = StockInputDefaults.colors(),
    shape: Shape = StockInputDefaults.shape
)
```

#### 3.2.2 상태 관리 사용 (Stateful)

```kotlin
@Composable
fun rememberStockInputState(
    initialValue: String = "",
    debounceMs: Long = 300L,
    onSearch: suspend (String) -> List<Stock>
): StockInputState

class StockInputState {
    val value: String
    val suggestions: List<Stock>
    val isLoading: Boolean
    val selectedStock: Stock?

    fun onValueChange(newValue: String)
    fun onSelect(stock: Stock)
    fun clear()
}

// State 기반 오버로드
@Composable
fun StockInputField(
    state: StockInputState,
    onSelect: (Stock) -> Unit,
    modifier: Modifier = Modifier,
    // ... 기타 파라미터
)
```

### 3.3 데이터 모델

기존 Stock 모델을 재사용하며, 필요시 확장:

```kotlin
// core/ui/component/stockinput/model/StockInputModels.kt

import com.stockapp.feature.search.domain.model.Stock
import com.stockapp.feature.search.domain.model.Market
import com.stockapp.core.db.entity.SearchHistoryEntity

/**
 * SearchHistoryEntity를 Stock으로 변환
 */
fun SearchHistoryEntity.toStock(): Stock = Stock(
    ticker = ticker,
    name = name,
    market = Market.OTHER  // 히스토리에는 market 정보 없음
)

/**
 * Stock을 SearchHistoryEntity로 변환
 */
fun Stock.toHistoryEntity(): SearchHistoryEntity = SearchHistoryEntity(
    ticker = ticker,
    name = name
)
```

### 3.4 스타일링 API

```kotlin
// core/ui/component/stockinput/StockInputDefaults.kt

object StockInputDefaults {

    val shape: Shape
        @Composable get() = MaterialTheme.shapes.medium

    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        focusedContainerColor: Color = MaterialTheme.colorScheme.surface,
        textColor: Color = MaterialTheme.colorScheme.onSurface,
        placeholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor: Color = MaterialTheme.colorScheme.outline,
        dropdownContainerColor: Color = MaterialTheme.colorScheme.surface,
        dropdownElevation: Dp = 8.dp
    ): StockInputColors = StockInputColors(
        containerColor = containerColor,
        focusedContainerColor = focusedContainerColor,
        textColor = textColor,
        placeholderColor = placeholderColor,
        iconColor = iconColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        dropdownContainerColor = dropdownContainerColor,
        dropdownElevation = dropdownElevation
    )
}

@Immutable
data class StockInputColors(
    val containerColor: Color,
    val focusedContainerColor: Color,
    val textColor: Color,
    val placeholderColor: Color,
    val iconColor: Color,
    val focusedBorderColor: Color,
    val unfocusedBorderColor: Color,
    val dropdownContainerColor: Color,
    val dropdownElevation: Dp
)
```

---

## 4. 컴포넌트 상세 명세

### 4.1 StockInputField

#### 구조

```
Box (fillMaxWidth)
├── OutlinedTextField
│   ├── leadingIcon
│   │   ├── CircularProgressIndicator (isLoading = true)
│   │   └── Icon(Search) (isLoading = false)
│   └── trailingIcon
│       ├── IconButton(History) (value.isEmpty && history.isNotEmpty)
│       └── IconButton(Clear) (value.isNotEmpty)
│
└── Card (드롭다운, suggestions.isNotEmpty && value.isNotBlank)
    └── LazyColumn (heightIn(max = 300.dp))
        └── items(suggestions)
            └── SuggestionItem
                ├── Column
                │   ├── Text(name)
                │   └── Text("ticker • market")
                └── MarketBadge
```

#### 동작 흐름

```
1. 사용자 입력
   └─→ onValueChange(newValue) 호출
       └─→ 외부에서 검색 실행 (debounce 적용)
           └─→ suggestions 업데이트
               └─→ 드롭다운 표시

2. 제안 항목 클릭
   └─→ value = suggestion.name 설정
   └─→ onSelect(suggestion) 호출
   └─→ suggestions 클리어 (드롭다운 닫힘)

3. 히스토리 버튼 클릭
   └─→ onHistoryClick() 호출 또는 히스토리 다이얼로그 표시

4. 히스토리 항목 선택
   └─→ value = history.name 설정
   └─→ onHistorySelect(history) 또는 onSelect(history) 호출
   └─→ 다이얼로그 닫기
```

### 4.2 StockInputHistoryDialog

```kotlin
@Composable
fun StockInputHistoryDialog(
    history: List<Stock>,
    onDismiss: () -> Unit,
    onSelect: (Stock) -> Unit,

    // 커스터마이징
    title: String = "최근 검색",
    emptyMessage: String = "검색 기록이 없습니다",
    confirmButtonText: String = "닫기"
)
```

### 4.3 SuggestionItem

```kotlin
@Composable
private fun SuggestionItem(
    stock: Stock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stock.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stock.ticker,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MarketBadge(market = stock.market)
    }
}
```

---

## 5. 상태 관리

### 5.1 StockInputState 클래스

```kotlin
// core/ui/component/stockinput/state/StockInputState.kt

@Stable
class StockInputState(
    initialValue: String = "",
    private val debounceMs: Long = 300L,
    private val onSearch: suspend (String) -> List<Stock>,
    private val scope: CoroutineScope
) {
    private val _value = mutableStateOf(initialValue)
    val value: String by _value

    private val _suggestions = mutableStateOf<List<Stock>>(emptyList())
    val suggestions: List<Stock> by _suggestions

    private val _isLoading = mutableStateOf(false)
    val isLoading: Boolean by _isLoading

    private val _selectedStock = mutableStateOf<Stock?>(null)
    val selectedStock: Stock? by _selectedStock

    private var searchJob: Job? = null

    fun onValueChange(newValue: String) {
        _value.value = newValue
        _selectedStock.value = null

        if (newValue.isBlank()) {
            _suggestions.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = scope.launch {
            delay(debounceMs)
            _isLoading.value = true
            try {
                _suggestions.value = onSearch(newValue)
            } catch (e: Exception) {
                _suggestions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSelect(stock: Stock) {
        _value.value = stock.name
        _selectedStock.value = stock
        _suggestions.value = emptyList()
    }

    fun clear() {
        searchJob?.cancel()
        _value.value = ""
        _suggestions.value = emptyList()
        _selectedStock.value = null
    }
}

@Composable
fun rememberStockInputState(
    initialValue: String = "",
    debounceMs: Long = 300L,
    onSearch: suspend (String) -> List<Stock>
): StockInputState {
    val scope = rememberCoroutineScope()
    return remember {
        StockInputState(
            initialValue = initialValue,
            debounceMs = debounceMs,
            onSearch = onSearch,
            scope = scope
        )
    }
}
```

### 5.2 오버로드 함수 (State 기반)

```kotlin
@Composable
fun StockInputField(
    state: StockInputState,
    onSelect: (Stock) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "종목명 또는 코드 검색",
    history: List<Stock> = emptyList(),
    onHistorySelect: ((Stock) -> Unit)? = null,
    colors: StockInputColors = StockInputDefaults.colors(),
    shape: Shape = StockInputDefaults.shape
) {
    StockInputField(
        value = state.value,
        onValueChange = state::onValueChange,
        suggestions = state.suggestions,
        onSelect = { stock ->
            state.onSelect(stock)
            onSelect(stock)
        },
        isLoading = state.isLoading,
        modifier = modifier,
        placeholder = placeholder,
        history = history,
        onHistorySelect = onHistorySelect ?: { stock ->
            state.onSelect(stock)
            onSelect(stock)
        },
        colors = colors,
        shape = shape
    )
}
```

---

## 6. SearchRepo 통합

### 6.1 검색 함수 추가

기존 SearchRepo에 suspend 함수 추가:

```kotlin
// feature/search/domain/repo/SearchRepo.kt

interface SearchRepo {
    suspend fun search(query: String): Result<List<Stock>>
    fun getAll(): Flow<List<Stock>>
    fun getHistory(): Flow<List<Stock>>
    suspend fun saveHistory(stock: Stock)
    suspend fun clearHistory()

    // 추가: State용 검색 함수
    suspend fun searchForSuggestions(query: String): List<Stock>
}

// feature/search/data/repo/SearchRepoImpl.kt

class SearchRepoImpl @Inject constructor(
    private val pyClient: PyClient,
    private val stockDao: StockDao,
    private val historyDao: SearchHistoryDao,
    private val json: Json
) : SearchRepo {

    // ... 기존 구현 ...

    override suspend fun searchForSuggestions(query: String): List<Stock> {
        return search(query).getOrElse { emptyList() }
    }
}
```

---

## 7. 사용 예시

### 7.1 기본 사용 (Stateless - ViewModel 연동)

```kotlin
// 기존 SearchScreen 리팩토링

@Composable
fun SearchScreen(
    onStockClick: (String) -> Unit,
    viewModel: SearchVm = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val state by viewModel.state.collectAsState()
    val history by viewModel.history.collectAsState()

    val suggestions = when (val s = state) {
        is SearchState.Results -> s.stocks
        else -> emptyList()
    }

    val isLoading = state is SearchState.Loading

    Scaffold(
        topBar = { TopAppBar(title = { Text("종목 검색") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            StockInputField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                suggestions = suggestions,
                onSelect = { stock ->
                    viewModel.onStockSelected(stock)
                    onStockClick(stock.ticker)
                },
                isLoading = isLoading,
                history = history,
                onHistorySelect = { stock ->
                    viewModel.onStockSelected(stock)
                    onStockClick(stock.ticker)
                }
            )

            // 에러 상태 표시
            if (state is SearchState.Error) {
                ErrorCard(
                    code = (state as SearchState.Error).code,
                    message = (state as SearchState.Error).msg,
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
```

### 7.2 상태 관리 사용 (Stateful - 다른 화면에서 사용)

```kotlin
// Analysis 화면에서 종목 선택

@Composable
fun AnalysisScreen(
    ticker: String? = null,
    viewModel: AnalysisVm = hiltViewModel()
) {
    val searchRepo = // Hilt로 주입 또는 ViewModel에서 제공

    val stockInputState = rememberStockInputState(
        debounceMs = 300L,
        onSearch = { query -> searchRepo.searchForSuggestions(query) }
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 종목이 선택되지 않은 경우 입력 필드 표시
        if (ticker == null && stockInputState.selectedStock == null) {
            Text(
                text = "분석할 종목을 선택하세요",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            StockInputField(
                state = stockInputState,
                onSelect = { stock ->
                    viewModel.loadAnalysis(stock.ticker)
                }
            )
        } else {
            // 선택된 종목의 분석 데이터 표시
            val selectedTicker = ticker ?: stockInputState.selectedStock?.ticker
            // ... 분석 UI
        }
    }
}
```

### 7.3 Indicator 화면에서 사용

```kotlin
@Composable
fun IndicatorScreen(
    ticker: String? = null,
    viewModel: IndicatorVm = hiltViewModel()
) {
    val searchRepo by viewModel.searchRepo  // ViewModel에서 제공

    val stockInputState = rememberStockInputState(
        onSearch = { query -> searchRepo.searchForSuggestions(query) }
    )

    var selectedTicker by remember { mutableStateOf(ticker) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 종목 선택 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedTicker != null) {
                Text(
                    text = "선택: ${stockInputState.selectedStock?.name ?: selectedTicker}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    selectedTicker = null
                    stockInputState.clear()
                }) {
                    Text("변경")
                }
            }
        }

        // 종목 선택 UI
        if (selectedTicker == null) {
            StockInputField(
                state = stockInputState,
                onSelect = { stock ->
                    selectedTicker = stock.ticker
                    viewModel.loadIndicator(stock.ticker)
                },
                placeholder = "지표를 볼 종목 검색"
            )
        }

        // 지표 탭 UI
        if (selectedTicker != null) {
            // ... Trend, Elder, Demark 탭
        }
    }
}
```

### 7.4 커스텀 스타일링

```kotlin
@Composable
fun ThemedStockInput() {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Stock>>(emptyList()) }

    StockInputField(
        value = query,
        onValueChange = { query = it },
        suggestions = suggestions,
        onSelect = { /* ... */ },

        // 커스텀 스타일
        colors = StockInputDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            dropdownElevation = 12.dp
        ),
        shape = RoundedCornerShape(12.dp),
        placeholder = "종목 검색..."
    )
}
```

---

## 8. SearchScreen 마이그레이션 가이드

### 8.1 현재 → 새 컴포넌트

#### Before (현재 SearchScreen)

```kotlin
@Composable
fun SearchScreen(onStockClick: (String) -> Unit, viewModel: SearchVm) {
    // ... Scaffold, TopAppBar ...

    Column {
        // 기존 SearchBar
        SearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearSearch
        )

        // State 기반 콘텐츠
        when (state) {
            is SearchState.Idle -> HistoryList(...)
            is SearchState.Loading -> LoadingState()
            is SearchState.Results -> StockList(...)
            is SearchState.Error -> ErrorState(...)
        }
    }
}
```

#### After (새 컴포넌트 사용)

```kotlin
@Composable
fun SearchScreen(onStockClick: (String) -> Unit, viewModel: SearchVm) {
    // ... Scaffold, TopAppBar ...

    Column {
        // 새 StockInputField (드롭다운 자동완성)
        StockInputField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            suggestions = (state as? SearchState.Results)?.stocks ?: emptyList(),
            onSelect = { stock ->
                viewModel.onStockSelected(stock)
                onStockClick(stock.ticker)
            },
            isLoading = state is SearchState.Loading,
            history = history,
            onHistorySelect = { stock ->
                viewModel.onStockSelected(stock)
                onStockClick(stock.ticker)
            }
        )

        // 에러만 별도 표시
        if (state is SearchState.Error) {
            ErrorCard(...)
        }
    }
}
```

### 8.2 변경점 요약

| 항목 | 이전 | 이후 |
|------|------|------|
| 검색 UI | 전체 화면 결과 리스트 | 드롭다운 자동완성 |
| 히스토리 | 별도 섹션 표시 | 버튼 + 다이얼로그 |
| 로딩 | 전체 화면 로딩 | 입력 필드 내 로딩 아이콘 |
| 에러 | 전체 화면 에러 | 별도 ErrorCard |
| 재사용성 | SearchScreen 전용 | 모든 화면에서 사용 가능 |

---

## 9. 테스트

### 9.1 단위 테스트

```kotlin
class StockInputStateTest {

    @Test
    fun `debounce prevents rapid search calls`() = runTest {
        var searchCount = 0
        val state = StockInputState(
            debounceMs = 100L,
            onSearch = { searchCount++; emptyList() },
            scope = this
        )

        state.onValueChange("삼")
        state.onValueChange("삼성")
        state.onValueChange("삼성전")

        advanceTimeBy(50)
        assertEquals(0, searchCount)

        advanceTimeBy(100)
        assertEquals(1, searchCount)
    }

    @Test
    fun `onSelect updates value and clears suggestions`() = runTest {
        val state = StockInputState(
            onSearch = { listOf(Stock("005930", "삼성전자", Market.KOSPI)) },
            scope = this
        )

        val stock = Stock("005930", "삼성전자", Market.KOSPI)
        state.onSelect(stock)

        assertEquals("삼성전자", state.value)
        assertEquals("005930", state.selectedStock?.ticker)
        assertTrue(state.suggestions.isEmpty())
    }

    @Test
    fun `clear resets all state`() = runTest {
        val state = StockInputState(
            onSearch = { emptyList() },
            scope = this
        )

        state.onValueChange("삼성")
        state.onSelect(Stock("005930", "삼성전자", Market.KOSPI))
        state.clear()

        assertEquals("", state.value)
        assertNull(state.selectedStock)
        assertTrue(state.suggestions.isEmpty())
    }
}
```

### 9.2 UI 테스트

```kotlin
class StockInputFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows suggestions when input has text`() {
        val suggestions = listOf(
            Stock("005930", "삼성전자", Market.KOSPI),
            Stock("000660", "SK하이닉스", Market.KOSPI)
        )

        composeTestRule.setContent {
            StockInputField(
                value = "삼성",
                onValueChange = {},
                suggestions = suggestions,
                onSelect = {}
            )
        }

        composeTestRule.onNodeWithText("삼성전자").assertIsDisplayed()
        composeTestRule.onNodeWithText("SK하이닉스").assertIsDisplayed()
    }

    @Test
    fun `shows history button when input is empty and history exists`() {
        composeTestRule.setContent {
            StockInputField(
                value = "",
                onValueChange = {},
                suggestions = emptyList(),
                onSelect = {},
                history = listOf(Stock("005930", "삼성전자", Market.KOSPI))
            )
        }

        composeTestRule.onNodeWithContentDescription("검색 히스토리").assertIsDisplayed()
    }

    @Test
    fun `shows clear button when input has text`() {
        composeTestRule.setContent {
            StockInputField(
                value = "삼성",
                onValueChange = {},
                suggestions = emptyList(),
                onSelect = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("지우기").assertIsDisplayed()
    }

    @Test
    fun `hides dropdown when suggestions are empty`() {
        composeTestRule.setContent {
            StockInputField(
                value = "xyz",
                onValueChange = {},
                suggestions = emptyList(),
                onSelect = {}
            )
        }

        composeTestRule.onNodeWithTag("suggestions_dropdown").assertDoesNotExist()
    }
}
```

---

## 10. 파일 구조 (최종)

```
StockApp/app/src/main/java/com/stockapp/
├── core/
│   ├── ui/
│   │   ├── component/
│   │   │   ├── stockinput/                    # 새 모듈
│   │   │   │   ├── StockInputField.kt         # 메인 Composable
│   │   │   │   ├── StockInputHistoryDialog.kt # 히스토리 다이얼로그
│   │   │   │   ├── StockInputDefaults.kt      # 기본값 및 스타일
│   │   │   │   ├── model/
│   │   │   │   │   └── StockInputModels.kt    # 변환 함수
│   │   │   │   └── state/
│   │   │   │       └── StockInputState.kt     # 상태 관리
│   │   │   ├── LoadingIndicator.kt            # (기존)
│   │   │   └── ErrorCard.kt                   # (기존)
│   │   └── theme/
│   ├── db/
│   │   └── entity/
│   │       └── StockEntity.kt                 # SearchHistoryEntity (기존)
│   └── py/
│       └── PyClient.kt                        # (기존)
│
└── feature/
    └── search/
        ├── domain/
        │   ├── model/Stock.kt                 # Stock, Market (기존)
        │   └── repo/SearchRepo.kt             # searchForSuggestions 추가
        ├── data/
        │   └── repo/SearchRepoImpl.kt         # 구현 추가
        └── ui/
            ├── SearchScreen.kt                # 리팩토링
            └── SearchVm.kt                    # (기존 유지)
```

---

## 11. 구현 체크리스트

### Phase 1: 핵심 컴포넌트 (P0)

- [ ] `StockInputModels.kt` - 변환 함수 정의
- [ ] `StockInputColors.kt` - 스타일 클래스 정의
- [ ] `StockInputDefaults.kt` - 기본값 객체 정의
- [ ] `StockInputField.kt` - 메인 Composable 구현
- [ ] `StockInputHistoryDialog.kt` - 히스토리 다이얼로그 구현
- [ ] 기본 단위 테스트 작성

### Phase 2: 상태 관리 (P1)

- [ ] `StockInputState.kt` - 상태 클래스 구현
- [ ] `rememberStockInputState` 함수 구현
- [ ] Stateful 오버로드 함수 구현
- [ ] 디바운싱 테스트 작성
- [ ] SearchRepo에 `searchForSuggestions` 추가

### Phase 3: 통합 (P1)

- [ ] SearchScreen 리팩토링
- [ ] AnalysisScreen 통합 (선택적)
- [ ] IndicatorScreen 통합 (선택적)
- [ ] UI 테스트 작성

### Phase 4: 최적화 (P2)

- [ ] 키보드 동작 최적화 (IME Action)
- [ ] 접근성 개선 (Content Description)
- [ ] 포커스 관리 개선
- [ ] KDoc 문서 작성

---

## 12. 의존성

### 12.1 기존 의존성 활용 (추가 불필요)

```toml
# gradle/libs.versions.toml (StockApp에 이미 포함)
[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version = "2024.12.01" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }
room-runtime = { module = "androidx.room:room-runtime", version = "2.8.3" }
room-ktx = { module = "androidx.room:room-ktx", version = "2.8.3" }
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version = "1.10.2" }
```

---

## 부록 A: StockApp 참조 파일

| 파일 | 경로 | 역할 |
|------|------|------|
| SearchScreen | `feature/search/ui/SearchScreen.kt` | 현재 검색 화면 |
| SearchVm | `feature/search/ui/SearchVm.kt` | 검색 ViewModel |
| Stock | `feature/search/domain/model/Stock.kt` | 도메인 모델 |
| SearchRepo | `feature/search/domain/repo/SearchRepo.kt` | Repository 인터페이스 |
| SearchRepoImpl | `feature/search/data/repo/SearchRepoImpl.kt` | PyClient 연동 |
| SearchHistoryEntity | `core/db/entity/StockEntity.kt` | 히스토리 Entity |
| SearchHistoryDao | `core/db/dao/SearchHistoryDao.kt` | 히스토리 DAO |
| PyClient | `core/py/PyClient.kt` | Python 호출 |

---

## 부록 B: Python API 참조

### stock_analyzer.stock.search

```python
def search(client: KiwoomClient, query: str) -> dict:
    """
    종목 검색

    Args:
        client: KiwoomClient 인스턴스
        query: 검색어 (종목명 또는 코드)

    Returns:
        {
            "ok": True,
            "data": [
                {"ticker": "005930", "name": "삼성전자", "market": "KOSPI"},
                {"ticker": "005935", "name": "삼성전자우", "market": "KOSPI"}
            ]
        }
    """
```

---

**End of Specification**
