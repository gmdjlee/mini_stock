# 순위정보(Ranking) 기능 이식 명세서 — UI 및 상태 관리

> **핵심 로직 명세**: `RANKING_FEATURE_SPEC.md` 참조
> **이 문서**: ViewModel 상태 관리, 화면 레이아웃, 포맷팅 규칙, 네비게이션

---

## 1. ViewModel 상태 관리 (RankingVm)

### 1.1 UI 상태 (sealed class)

```kotlin
sealed class RankingState {
    data object Loading : RankingState()
    data object NoApiKey : RankingState()
    data class Success(val result: RankingResult) : RankingState()
    data class Error(val message: String) : RankingState()
}
```

### 1.2 StateFlow 정의 (12개)

| StateFlow | 타입 | 초기값 | 설명 |
|-----------|------|--------|------|
| `state` | `RankingState` | `Loading` | 메인 UI 상태 |
| `rankingType` | `RankingType` | `DAILY_VOLUME_TOP` | 선택된 순위 유형 |
| `marketType` | `MarketType` | `KOSPI` | 시장 필터 |
| `exchangeType` | `ExchangeType` | `KRX_MOCK` | 거래소 필터 |
| `itemCount` | `ItemCount` | `TEN` | 표시 개수 |
| `investmentMode` | `InvestmentMode` | `MOCK` | 투자 모드 |
| `isRefreshing` | `Boolean` | `false` | Pull-to-Refresh 상태 |
| `orderBookDirection` | `OrderBookDirection` | `BUY` | ka10021 매수/매도 |
| `investorType` | `InvestorType` | `FOREIGN` | ka90009 투자자 유형 |
| `tradeDirection` | `TradeDirection` | `NET_BUY` | ka90009 매매 방향 |
| `valueType` | `ValueType` | `AMOUNT` | ka90009 금액/수량 |
| `excludeEtf` | `Boolean` | `false` | ETF 제외 토글 |

+ 내부 변수: `_fullResult: RankingResult?` (전체 결과 캐시, 로컬 필터링용)

### 1.3 초기화 흐름

```
init → checkApiKeyAndLoad()
  1. settingsProvider.getApiKeyConfig().first()
  2. config.isValid() == false → state = NoApiKey, return
  3. investmentMode = config.investmentMode
  4. exchangeType = MOCK → KRX_MOCK, PRODUCTION → KRX
  5. loadRanking()
```

### 1.4 필터 변경 시 동작

| 필터 변경 | API 재호출 | 로컬 필터링 | 메서드 |
|-----------|-----------|------------|--------|
| rankingType | O | - | `onRankingTypeChange()` |
| marketType | O | - | `onMarketTypeChange()` |
| exchangeType | O | - | `onExchangeTypeChange()` |
| orderBookDirection | O | - | `onOrderBookDirectionChange()` |
| investorType | O | - | `onInvestorTypeChange()` |
| tradeDirection | O | - | `onTradeDirectionChange()` |
| valueType | O | - | `onValueTypeChange()` |
| **itemCount** | - | **O** | `onItemCountChange()` |
| **excludeEtf** | - | **O** | `onExcludeEtfChange()` |

> **핵심 설계**: `loadRanking()`은 항상 `ItemCount.THIRTY`(최대 30개)를 요청한다. 결과를 `_fullResult`에 저장 후, `applyLocalFilters()`로 ETF 제외 + 개수 제한을 로컬 적용한다. 이렇게 하면 개수 변경이나 ETF 토글 시 API 재호출 없이 즉시 반응한다.

### 1.5 loadRanking() 흐름

```
1. state = Loading
2. getRankingUC(..., itemCount = THIRTY) 호출
3. 성공 시:
   - _fullResult = data
   - applyLocalFilters()
4. 실패 시:
   - _fullResult = null
   - NoApiKeyError → state = NoApiKey
   - NetworkError → state = Error(error.message)
   - AuthError → state = Error("인증 오류: ${error.message}")
   - ApiCallError → state = Error("API 오류: ${error.message}")
   - 기타 → state = Error(error.message ?: "알 수 없는 오류")
```

### 1.6 applyLocalFilters() 로직

```
1. _fullResult가 null이면 → loadRanking() 호출 후 return
2. excludeEtf == true → items.filterNot { isEtfOrEtn(it.name) }
3. .take(itemCount.value)
4. state = Success(fullResult.copy(items = filteredItems))
```

### 1.7 ETF/ETN 판별 로직

종목명 기반 ETF/ETN 필터링. 한국 ETF는 운용사 브랜드명으로 시작하는 패턴:

```kotlin
private fun isEtfOrEtn(name: String): Boolean {
    val upperName = name.uppercase()
    return ETF_BRAND_PATTERNS.any { upperName.startsWith(it) } ||
        upperName.contains("ETF") ||
        upperName.contains("ETN")
}

// 20개 브랜드 패턴 목록 (순서 무관)
private val ETF_BRAND_PATTERNS = listOf(
    "KODEX",      // 삼성자산운용
    "TIGER",      // 미래에셋
    "ARIRANG",    // 한화자산운용
    "KINDEX",     // 한국투자신탁
    "KBSTAR",     // KB자산운용
    "HANARO",     // NH-Amundi 자산운용
    "ACE",        // 한국투자신탁
    "SOL",        // 신한자산운용
    "KOSEF",      // 삼성자산운용
    "TREX",       // 미래에셋
    "SMART",      // 교보AXA 자산운용
    "TIMEFOLIO",  // 타임폴리오 자산운용
    "RISE",       // KB자산운용
    "PLUS",       // 신한자산운용
    "FOCUS",      // DB자산운용
    "WOORI",      // 우리자산운용
    "BNK",        // BNK자산운용
    "파워",       // 한국 ETN 접두사
    "TRUE",       // 한국 ETF 접두사
    "QV",         // 한국 ETF 접두사
)
```

### 1.8 유형별 조건부 로직

```kotlin
// 가용 시장 유형 (ka90009만 "전체" 지원)
fun getAvailableMarketTypes(): List<MarketType> =
    if (rankingType == FOREIGN_INSTITUTION_TOP)
        listOf(KOSPI, KOSDAQ, ALL)
    else
        listOf(KOSPI, KOSDAQ)

// 가용 거래소 유형 (투자모드 기반)
fun getAvailableExchangeTypes(): List<ExchangeType> =
    when (investmentMode) {
        MOCK -> listOf(KRX_MOCK)
        PRODUCTION -> listOf(KRX, NXT)
    }

// 유형 판별
fun isOrderBookSurgeType(): Boolean = rankingType == ORDER_BOOK_SURGE
fun isForeignInstitutionType(): Boolean = rankingType == FOREIGN_INSTITUTION_TOP
```

### 1.9 종목 클릭 처리

```kotlin
fun onStockClick(item: RankingItem) {
    stockSelector.selectTicker(item.ticker, item.name)
}
// UI에서는 onStockClick 콜백으로 네비게이션 트리거
```

### 1.10 Pull-to-Refresh

```kotlin
fun refresh() {
    viewModelScope.launch {
        isRefreshing = true
        loadRanking()
        isRefreshing = false
    }
}
```

---

## 2. 화면 레이아웃

### 2.1 컴포저블 트리 구조

```
RankingScreen(viewModel, onStockClick)
├── Scaffold
│   ├── TopAppBar
│   │   ├── title: Text("순위정보")
│   │   └── actions:
│   │       ├── IconButton(Refresh) → viewModel.refresh()
│   │       └── ThemeToggleButton()  (선택적, 다크모드 토글)
│   └── Column(fillMaxSize, padding)
│       ├── RankingTypeSelector(selectedType, onTypeSelected)
│       │   └── OutlinedButton → DropdownMenu (5개 RankingType)
│       │
│       ├── MarketTypeTabs(selectedMarket, availableMarkets, onMarketSelected)
│       │   └── TabRow (동적: KOSPI/KOSDAQ 또는 KOSPI/KOSDAQ/전체)
│       │
│       ├── [PRODUCTION 모드에서만] ExchangeTypeTabs
│       │   └── TabRow (KRX/NXT, surfaceVariant 배경)
│       │
│       ├── FilterRow(...)  (가로 스크롤)
│       │   ├── ItemCount 칩 (5개/10개/20개/30개)
│       │   ├── ETF 제외 토글 (Text + Switch)
│       │   ├── [ka10021] OrderBookDirection 칩 (매수/매도)
│       │   └── [ka90009] InvestorType 칩 (외국인/기관/전체)
│       │              TradeDirection 칩 (순매수/순매도)
│       │              ValueType 칩 (금액/수량)
│       │
│       └── [상태별 콘텐츠]
│           ├── Loading → LoadingContent (CircularProgressIndicator, 중앙)
│           ├── NoApiKey → NoApiKeyContent (에러 카드)
│           ├── Success → PullToRefreshBox
│           │   └── RankingTable(result, onItemClick)
│           │       ├── RankingTableHeader(result)  (동적 헤더)
│           │       └── LazyColumn
│           │           └── RankingTableRow(item, result, onClick)
│           │               (항목 사이 HorizontalDivider)
│           │           └── [items 비어있을 때] "데이터가 없습니다"
│           └── Error → ErrorContent (에러 카드 + "다시 시도" 버튼)
```

### 2.2 RankingTypeSelector

```
┌─────────────────────────┐
│   당일거래량상위    ▼    │  ← OutlinedButton (fillMaxWidth)
└─────────────────────────┘
  padding: horizontal=16.dp, vertical=8.dp

DropdownMenu (fillMaxWidth(0.9f)):
  ├── 호가잔량급증
  ├── 거래량급증
  ├── 당일거래량상위
  ├── 신용비율상위
  └── 외국인/기관상위
```

### 2.3 MarketTypeTabs

```
┌────────┬────────┬────────┐
│ KOSPI  │ KOSDAQ │ [전체] │  ← TabRow (전체는 ka90009만)
└────────┴────────┴────────┘
```

### 2.4 ExchangeTypeTabs (PRODUCTION 모드 전용)

```
┌────────┬────────┐
│  KRX   │  NXT   │  ← TabRow (surfaceVariant 배경)
└────────┴────────┘
```

### 2.5 FilterRow (가로 스크롤)

```
┌──────────────────────────────────────────────────────────┐
│ [5개] [10개] [20개] [30개]  ETF제외 [=]  [매수] [매도]  │  ← ka10021 시
│ [5개] [10개] [20개] [30개]  ETF제외 [=]                  │  ← ka10023/30/33 시
│ [5개] [10개] [20개] [30개]  ETF제외 [=]  [외국인] [기관] │  ← ka90009 시
│   [전체]  [순매수] [순매도]  [금액] [수량]               │
└──────────────────────────────────────────────────────────┘
  padding: horizontal=16.dp, vertical=8.dp
  칩 간격: 8.dp
  사용 컴포넌트: FilterChip (선택 상태 반영)
  ETF 제외: Text("ETF 제외") + Switch
```

### 2.6 RankingTable

#### 테이블 헤더 (RankingTableHeader)

```
┌──────┬───────────────┬──────────┬──────────┐
│ 순위  │     종목       │  현재가   │ [동적]   │
└──────┴───────────────┴──────────┴──────────┘
  배경: surfaceVariant
  padding: horizontal=16.dp, vertical=12.dp

동적 헤더 값 (getTypeSpecificHeader):
  ORDER_BOOK_SURGE       → "급증률"
  VOLUME_SURGE           → "급증률"
  DAILY_VOLUME_TOP       → "거래량"
  CREDIT_RATIO_TOP       → "신용비율"
  FOREIGN_INSTITUTION_TOP → "{투자자}{방향}" (예: "외인순매수", "기관순매도", "합계순매수")
    투자자: FOREIGN→"외인", INSTITUTION→"기관", ALL→"합계"
    방향: NET_BUY→"순매수", NET_SELL→"순매도"
```

#### 테이블 행 (RankingTableRow)

```
┌──────┬───────────────┬──────────┬──────────┐
│  1   │ 삼성전자       │  72,500  │  32.5억  │
│      │ 005930        │ +500     │          │
│      │               │ (0.69%)  │          │
└──────┴───────────────┴──────────┴──────────┘
  width: 40dp  | weight(1f)  | 80dp      | 80dp

열 구성:
  1. 순위 (rank) - width=40.dp, titleMedium, Bold
  2. 종목정보 - weight=1f
     - 종목명 (bodyMedium, Medium, maxLines=1, ellipsis)
     - 종목코드 (bodySmall, onSurfaceVariant)
  3. 가격 정보 - width=80.dp, End 정렬
     - 현재가 (bodyMedium, Medium)
     - 변동 (bodySmall, 색상: priceChangeSign 기준)
  4. 유형별 값 - width=80.dp, End 정렬, bodyMedium

클릭: 전체 행 clickable → onItemClick(item)
항목 간 구분: HorizontalDivider (padding horizontal=16.dp, outlineVariant)
빈 목록: "데이터가 없습니다" (bodyLarge, onSurfaceVariant, 중앙, padding=32.dp)
```

### 2.7 상태 화면

#### LoadingContent
- `CircularProgressIndicator` (화면 중앙)

#### NoApiKeyContent
```
┌──────────────────────────────────┐
│          ⚙️ (48dp)              │  ← Settings 아이콘
│                                  │
│  API 키가 설정되지 않았습니다     │  ← titleMedium
│                                  │
│  설정 화면에서 API 키를          │  ← bodyMedium (alpha 0.8)
│  입력해주세요                    │
└──────────────────────────────────┘
  Card: errorContainer 배경, padding=24.dp
  외부: fillMaxSize, 중앙 정렬, padding=16.dp
```

#### ErrorContent
```
┌──────────────────────────────────┐
│         오류 발생                │  ← titleMedium
│                                  │
│  {에러 메시지}                   │  ← bodyMedium (alpha 0.8, 중앙)
│                                  │
│       [다시 시도]                │  ← OutlinedButton
└──────────────────────────────────┘
  Card: errorContainer 배경, padding=24.dp
```

---

## 3. 포맷팅 규칙

### 3.1 가격 포맷 (formatPrice)

```
입력: Long (현재가)
규칙:
  - 0 → "-"
  - 그 외 → 한국 로케일 NumberFormat 적용 (천 단위 콤마)

예시:
  72500 → "72,500"
  0 → "-"
```

### 3.2 변동 포맷 (formatChange)

```
입력: change (Long), rate (Double), sign (String)
규칙:
  - change == 0 && rate == 0.0 → "-"
  - sign == "+" → "+{변동} ({비율}%)"
  - sign == "-" → "{변동} ({비율}%)"  (음수이므로 "-" 자동)
  - 기타 → "{변동} ({비율}%)"

예시:
  (500, 0.69, "+")  → "+500 (0.69%)"
  (-300, -1.23, "-") → "-300 (-1.23%)"
  (0, 0.0, "")      → "-"
```

### 3.3 거래량 포맷 (formatVolume)

```
입력: Long
규칙:
  - >= 100,000,000 (1억) → "{value/1억}억"  (소수점 1자리)
  - >= 10,000 (1만) → "{value/1만}만"  (소수점 1자리)
  - 그 외 → 한국 로케일 NumberFormat

예시:
  350000000 → "3.5억"
  1250000   → "125.0만"
  9500      → "9,500"
```

### 3.4 금액 포맷 (formatAmount)

```
입력: Long (부호 있음)
규칙:
  - >= 100,000,000 → "+{value/1억}억"  (양수, 소수점 0자리)
  - >= 10,000 → "+{value/1만}만"  (양수, 소수점 0자리)
  - <= -100,000,000 → "{value/1억}억"  (음수, 소수점 0자리, 자동 "-" 포함)
  - <= -10,000 → "{value/1만}만"  (음수, 소수점 0자리)
  - 그 외 → 한국 로케일 NumberFormat

예시:
  3250000000 → "+33억"
  15000000   → "+1500만"
  -500000000 → "-5억"
  -250000    → "-25만"
```

### 3.5 유형별 값 포맷 (formatTypeSpecificValue)

| 순위 유형 | 표시 필드 | 포맷 |
|-----------|----------|------|
| ORDER_BOOK_SURGE | `surgeRate` | `"%.1f%%"` (예: "32.5%") |
| VOLUME_SURGE | `surgeRate` | `"%.1f%%"` (예: "128.3%") |
| DAILY_VOLUME_TOP | `volume` | `formatVolume()` (예: "3.5억") |
| CREDIT_RATIO_TOP | `creditRatio` | `"%.2f%%"` (예: "15.23%") |
| FOREIGN_INSTITUTION_TOP | `netValue` (fallback: `foreignNetBuy`) | valueType==QUANTITY → `formatVolume()`, 아니면 → `formatAmount()` |

> null인 경우 모두 `"-"` 표시

---

## 4. 색상 체계

### 4.1 가격 변동 색상 (한국 주식 관례)

```kotlin
val priceColor = when (item.priceChangeSign) {
    "+" -> extendedColors.statusUp      // 빨강 (0xFFF44336) — 상승
    "-" -> extendedColors.statusDown    // 파랑 (0xFF2196F3) — 하락
    else -> extendedColors.statusNeutral // 회색 (0xFF9E9E9E) — 보합
}
```

> **주의**: 한국 주식 시장은 미국과 반대로 빨강=상승, 파랑=하락이 관례이다.

### 4.2 대상 프로젝트에 필요한 색상 정의

```kotlin
data class ExtendedColors(
    val statusUp: Color,       // 상승 색상 (권장: 0xFFF44336 빨강)
    val statusDown: Color,     // 하락 색상 (권장: 0xFF2196F3 파랑)
    val statusNeutral: Color   // 보합 색상 (권장: 0xFF9E9E9E 회색)
)

// CompositionLocal로 제공
val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors(...) }
```

### 4.3 NumberFormat 캐싱

```kotlin
// LazyColumn 행마다 새 인스턴스 생성 방지를 위해 파일 수준 캐싱
private val koreanNumberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
```

---

## 5. 네비게이션 통합

### 5.1 화면 등록

```
Route: "ranking"
Deep Link: "stockapp://ranking" (선택적)
Bottom Navigation:
  - 아이콘: Icons.Default.Leaderboard
  - 라벨: "순위정보"
  - 위치: 5개 탭 중 2번째 (인덱스 1)
```

### 5.2 종목 클릭 시 네비게이션

```
사용자가 순위 항목 클릭
  → viewModel.onStockClick(item)  // SelectedStockManager 업데이트
  → onStockClick() 콜백           // 네비게이션 트리거

네비게이션 동작:
  - 대상: 종목 분석 화면 (StockAnalysis, tab=1 수급분석)
  - popUpTo: navController.graph.findStartDestination().id, saveState=true
  - launchSingleTop: true
  - restoreState: true
```

### 5.3 RankingScreen 시그니처

```kotlin
@Composable
fun RankingScreen(
    viewModel: RankingVm = hiltViewModel(),
    onStockClick: () -> Unit = {}  // 네비게이션 콜백
)
```

---

## 6. 이식 시 주의사항

### 6.1 필수 구현 항목
1. **ApiClient**: rate limiting + 토큰 갱신 + 에러 핸들링
2. **SettingsProvider**: API 키 저장/조회 (EncryptedSharedPreferences 권장)
3. **StockSelector**: 앱 전역 종목 선택 상태 (Singleton StateFlow)
4. **ExtendedColors**: 한국 주식 색상 체계

### 6.2 선택적 구현 항목
- ThemeToggleButton (다크모드 토글) — 없으면 TopAppBar actions에서 제거
- Deep Link 지원 — 불필요하면 생략
- Pull-to-Refresh — Material3 `PullToRefreshBox` 사용

### 6.3 유연하게 변경 가능한 부분
- DI 프레임워크 (Hilt → Koin, Kodein 등)
- HTTP 클라이언트 (OkHttp → Ktor 등)
- JSON 라이브러리 (Kotlinx Serialization → Moshi, Gson 등 — DTO 어노테이션 변경 필요)
- 네비게이션 방식 (Compose Navigation → 다른 네비게이션 라이브러리)

### 6.4 변경하면 안 되는 부분
- API 엔드포인트 (`/api/dostk/rkinfo`)
- API 요청 바디 필드명 (`mrkt_tp`, `stex_tp`, `trde_tp` 등)
- API 응답 필드명 (DTO의 `@SerialName` 값)
- parseSign 로직 (1,2→"+", 4,5→"-")
- 종목코드 접미사 규칙 (`_AL`, `_KS`, `_KQ`)
- ka90009의 4-way 랭킹 구조
