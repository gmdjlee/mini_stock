# ETF 통계 기능 명세서 (StockApp)

> **Version**: 1.0
> **Date**: 2026-01-25
> **Status**: 기존 ETF 기능 확장
> **Purpose**: ETF 구성종목 통계 분석 및 시각화 기능 확장

---

## 목차

1. [개요](#1-개요)
2. [현재 구현 현황](#2-현재-구현-현황)
3. [확장 기능 명세](#3-확장-기능-명세)
4. [UI 명세](#4-ui-명세)
5. [데이터 모델](#5-데이터-모델)
6. [구현 계획](#6-구현-계획)
7. [코드 패턴](#7-코드-패턴)

---

## 1. 개요

### 1.1 목적

StockApp의 기존 ETF 기능을 확장하여 다음 통계 분석 기능을 추가:
- ETF 보유 종목의 평가금액 순위
- 기간별 종목 변동 추적 (신규 편입, 편출, 비중 증가/감소)
- 예금(현금) 비중 추이 분석
- 개별 종목의 ETF 편입 현황 분석

### 1.2 핵심 기능

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| **보유량 순위** | 전체 ETF 합산 평가금액 기준 종목 순위 | High |
| **신규 편입** | 기간 내 새로 편입된 종목 목록 | High |
| **편출** | 기간 내 편출된 종목 목록 | High |
| **비중 증가** | 기간 내 비중이 증가한 종목 | Medium |
| **비중 감소** | 기간 내 비중이 감소한 종목 | Medium |
| **예금 추이** | ETF 현금 비중 추이 차트 | Medium |
| **종목 분석** | 특정 종목의 ETF 편입 현황 분석 | Low |

### 1.3 기술 스택 (기존 프로젝트 준수)

| 컴포넌트 | 기술 | 비고 |
|----------|------|------|
| UI | Jetpack Compose | Material Design 3 |
| 상태관리 | StateFlow | MVVM 패턴 |
| DI | Hilt | SingletonComponent |
| DB | Room | 기존 Entity 확장 |
| 차트 | Vico | 기존 차트 컴포넌트 활용 |

---

## 2. 현재 구현 현황

### 2.1 기존 ETF 모듈 구조

```
feature/etf/
├── domain/
│   ├── model/
│   │   └── EtfModels.kt              # ✅ 도메인 모델 정의됨
│   ├── repo/
│   │   ├── EtfRepository.kt          # ✅ 저장소 인터페이스
│   │   └── EtfCollectorRepo.kt       # ✅ 수집기 인터페이스
│   └── usecase/
│       ├── GetStockRankingUC.kt      # ✅ 순위 조회 UC
│       └── GetStockChangesUC.kt      # ✅ 변동 조회 UC
├── data/
│   ├── dto/
│   │   └── EtfApiDto.kt              # ✅ API DTO
│   └── repo/
│       ├── EtfRepositoryImpl.kt      # ✅ 저장소 구현
│       └── EtfCollectorRepoImpl.kt   # ✅ 수집기 구현
├── ui/
│   ├── EtfScreen.kt                  # ✅ 메인 화면
│   ├── EtfVm.kt                      # ✅ ViewModel
│   ├── detail/
│   │   ├── EtfCharts.kt              # ✅ 차트 컴포넌트
│   │   └── StockDetailBottomSheet.kt # ✅ 종목 상세 시트
│   └── tabs/
│       ├── CollectionStatusTab.kt    # ✅ 수집현황 탭
│       ├── StockRankingTab.kt        # ✅ 종목랭킹 탭
│       ├── StockChangesTab.kt        # ✅ 변동종목 탭
│       └── EtfSettingsTab.kt         # ✅ 설정 탭
├── worker/
│   └── EtfCollectionWorker.kt        # ✅ 백그라운드 수집
└── di/
    └── EtfModule.kt                  # ✅ DI 모듈
```

### 2.2 기존 탭 구조

```kotlin
enum class EtfTab(val title: String) {
    COLLECTION_STATUS("수집현황"),
    STOCK_RANKING("종목랭킹"),
    STOCK_CHANGES("변동종목"),
    SETTINGS("설정")
}
```

### 2.3 기존 데이터베이스 Entity

| Entity | 용도 | 상태 |
|--------|------|------|
| `EtfEntity` | ETF 기본정보 | ✅ 구현됨 |
| `EtfConstituentEntity` | 구성종목 스냅샷 | ✅ 구현됨 |
| `EtfKeywordEntity` | 필터 키워드 | ✅ 구현됨 |
| `EtfCollectionHistoryEntity` | 수집 히스토리 | ✅ 구현됨 |
| `DailyEtfStatisticsEntity` | 일별 통계 | ❌ 추가 필요 |

---

## 3. 확장 기능 명세

### 3.1 탭 구조 확장

기존 4개 탭에서 **7개 통계 서브탭**을 가진 **통계 허브** 형태로 확장:

```kotlin
// 메인 탭 (기존 유지)
enum class EtfTab(val title: String) {
    COLLECTION_STATUS("수집현황"),
    STATISTICS("통계"),           // 변경: 기존 STOCK_RANKING, STOCK_CHANGES 통합
    SETTINGS("설정")
}

// 통계 서브탭 (신규)
enum class StatisticsSubTab(val title: String) {
    AMOUNT_RANKING("보유량 순위"),
    NEWLY_INCLUDED("신규 편입"),
    REMOVED("편출"),
    WEIGHT_INCREASED("비중 증가"),
    WEIGHT_DECREASED("비중 감소"),
    CASH_DEPOSIT("예금 추이"),
    STOCK_ANALYSIS("종목 분석")
}
```

### 3.2 기간 선택 옵션

```kotlin
enum class DateRangeOption(val days: Int, val label: String) {
    DAY(1, "1일"),
    WEEK(7, "1주"),
    MONTH(30, "1개월"),
    THREE_MONTHS(90, "3개월"),
    SIX_MONTHS(180, "6개월"),
    YEAR(365, "1년"),
    ALL(-1, "전체")
}
```

### 3.3 종목 상태 분류

```kotlin
enum class HoldingStatus(val displayName: String) {
    NEW("신규"),
    INCREASE("증가"),
    DECREASE("감소"),
    REMOVED("편출"),
    MAINTAIN("유지")
}

// 상태 판정 로직
private const val WEIGHT_CHANGE_THRESHOLD = 0.01f  // 0.01% 기준

fun determineStatus(currentWeight: Float, previousWeight: Float): HoldingStatus {
    return when {
        previousWeight == 0f && currentWeight > 0f -> HoldingStatus.NEW
        currentWeight == 0f && previousWeight > 0f -> HoldingStatus.REMOVED
        currentWeight - previousWeight > WEIGHT_CHANGE_THRESHOLD -> HoldingStatus.INCREASE
        previousWeight - currentWeight > WEIGHT_CHANGE_THRESHOLD -> HoldingStatus.DECREASE
        else -> HoldingStatus.MAINTAIN
    }
}
```

### 3.4 현금/예금 판별

```kotlin
fun isCashDeposit(stockName: String): Boolean {
    val lowerName = stockName.lowercase()
    return lowerName.contains("원화예금") ||
           lowerName.contains("현금") ||
           lowerName.contains("cash") ||
           lowerName.contains("예금") ||
           lowerName.contains("krw")
}
```

---

## 4. UI 명세

### 4.1 화면 계층 구조

```
EtfScreen (메인 화면)
├── Tab 0: "수집현황" (CollectionStatusTab) - 기존 유지
│   ├── 수집 버튼
│   ├── 진행 상태
│   └── 수집 히스토리 목록
│
├── Tab 1: "통계" (StatisticsHubContent) - 신규
│   ├── ScrollableTabRow (7개 서브탭)
│   │   ├── Tab 0: "보유량 순위"
│   │   ├── Tab 1: "신규 편입"
│   │   ├── Tab 2: "편출"
│   │   ├── Tab 3: "비중 증가"
│   │   ├── Tab 4: "비중 감소"
│   │   ├── Tab 5: "예금 추이"
│   │   └── Tab 6: "종목 분석"
│   │
│   ├── DateRangeSelector (Tab 6 제외)
│   │   └── FilterChip Row [1일|1주|1개월|3개월|6개월|1년|전체]
│   │
│   ├── 기간 정보 표시
│   │   └── Text: "2026-01-20 ~ 2026-01-25"
│   │
│   └── 탭 콘텐츠
│       ├── AmountRankingContent
│       ├── StockChangeContent (x4)
│       ├── CashDepositContent
│       └── StockAnalysisContent
│
└── Tab 2: "설정" (EtfSettingsTab) - 기존 유지
    ├── 자동 수집 설정
    ├── 수집 시간 설정
    └── 필터 키워드 관리
```

### 4.2 DateRangeSelector 컴포넌트

```kotlin
@Composable
fun DateRangeSelector(
    selectedRange: DateRangeOption,
    onRangeSelected: (DateRangeOption) -> Unit,
    availableOptions: List<DateRangeOption> = DateRangeOption.entries,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(availableOptions) { option ->
            FilterChip(
                selected = option == selectedRange,
                onClick = { onRangeSelected(option) },
                label = { Text(option.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
```

### 4.3 AmountRankingContent

| 컬럼 | 설명 | 너비 |
|------|------|------|
| # | 순위 | 32.dp |
| 종목명 | 종목명 + NEW 뱃지 | weight(1f) |
| 합산금액 | 평가금액 합계 | 80.dp |
| ETF수 | 편입 ETF 개수 | 50.dp |
| 변동 | 전일 대비 변동 | 70.dp |

```kotlin
@Composable
private fun AmountRankingRow(
    item: EnhancedStockRanking,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위
        Text(
            text = item.rank.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        // 종목명 + NEW 뱃지
        Row(modifier = Modifier.weight(1f)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.stockName, fontWeight = FontWeight.Medium)
                    if (item.isNew) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = "신규",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    item.stockCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 합산금액
        Text(
            text = formatAmount(item.totalAmount),
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End
        )

        // ETF 수
        Text(
            text = "${item.etfCount}개",
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End
        )

        // 변동
        Text(
            text = formatAmountChange(item.amountChange),
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End,
            color = getChangeColor(item.amountChange)
        )
    }
}
```

### 4.4 StockChangeContent (신규/편출/증가/감소)

필터 칩으로 변동 유형 선택:

```kotlin
@Composable
private fun StockChangeContent(
    result: StockChangesResult,
    changeType: StockChangeType,
    onItemClick: (EnhancedStockChange) -> Unit,
    onItemLongClick: (EnhancedStockChange) -> Unit
) {
    val items = when (changeType) {
        StockChangeType.NEWLY_INCLUDED -> result.newlyIncluded
        StockChangeType.REMOVED -> result.removed
        StockChangeType.WEIGHT_INCREASED -> result.weightIncreased
        StockChangeType.WEIGHT_DECREASED -> result.weightDecreased
    }

    LazyColumn {
        // 헤더 카드
        item {
            SummaryCard(
                date = result.date,
                previousDate = result.previousDate,
                totalCount = items.size
            )
        }

        // 아이템 목록
        items(items) { item ->
            StockChangeItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}
```

### 4.5 CashDepositContent

예금 추이 차트와 ETF별 예금 비중 목록:

```kotlin
@Composable
private fun CashDepositContent(
    cashDepositData: List<CashDepositTrend>,
    etfCashDetails: List<EtfCashDetail>
) {
    LazyColumn {
        // 추이 차트
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("예금 비중 추이", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    CashDepositLineChart(data = cashDepositData)
                }
            }
        }

        // ETF별 예금 현황
        item {
            Text(
                "ETF별 예금 현황",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(etfCashDetails) { detail ->
            EtfCashDetailItem(detail = detail)
        }
    }
}
```

### 4.6 StockAnalysisContent

종목 검색 및 ETF 편입 현황 분석:

```kotlin
@Composable
private fun StockAnalysisContent(
    query: String,
    onQueryChange: (String) -> Unit,
    analysisResult: StockAnalysisResult?,
    onSearch: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    Column {
        // 검색 필드
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("종목명 또는 코드 검색") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, "지우기")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // 분석 결과
        analysisResult?.let { result ->
            LazyColumn {
                // 종목 정보 헤더
                item {
                    StockInfoHeader(
                        stockCode = result.stockCode,
                        stockName = result.stockName,
                        totalAmount = result.totalAmount,
                        etfCount = result.etfCount,
                        onNavigateToAnalysis = {
                            onNavigateToAnalysis(result.stockCode, result.stockName)
                        }
                    )
                }

                // 금액 추이 차트
                item {
                    AmountHistoryChart(data = result.amountHistory)
                }

                // 비중 추이 차트
                item {
                    WeightHistoryChart(data = result.weightHistory)
                }

                // 편입 ETF 목록
                item {
                    Text(
                        "편입 ETF 목록",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(result.containingEtfs) { etf ->
                    ContainingEtfItem(etf = etf)
                }
            }
        }
    }

    // FAB: 수급 분석으로 이동
    if (analysisResult != null) {
        ExtendedFloatingActionButton(
            onClick = {
                onNavigateToAnalysis(
                    analysisResult.stockCode,
                    analysisResult.stockName
                )
            },
            icon = { Icon(Icons.Default.ShowChart, null) },
            text = { Text("종목 분석") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
```

### 4.7 상태 색상 시스템

기존 프로젝트의 `ExtendedColors`를 활용:

```kotlin
// 상태별 색상 매핑
@Composable
fun getStatusColor(status: HoldingStatus): Color {
    val extendedColors = LocalExtendedColors.current
    return when (status) {
        HoldingStatus.NEW -> extendedColors.success          // Green
        HoldingStatus.INCREASE -> extendedColors.danger      // Red (상승)
        HoldingStatus.DECREASE -> extendedColors.info        // Blue (하락)
        HoldingStatus.REMOVED -> MaterialTheme.colorScheme.outline
        HoldingStatus.MAINTAIN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

// 금액 변동 색상
@Composable
fun getChangeColor(change: Long?): Color {
    val extendedColors = LocalExtendedColors.current
    return when {
        change == null -> MaterialTheme.colorScheme.onSurfaceVariant
        change > 0 -> extendedColors.danger    // 상승 = 빨강 (한국 주식)
        change < 0 -> extendedColors.info      // 하락 = 파랑
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
```

---

## 5. 데이터 모델

### 5.1 신규 Entity: DailyEtfStatisticsEntity

```kotlin
@Entity(
    tableName = "daily_etf_statistics",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyEtfStatisticsEntity(
    @PrimaryKey
    val date: String,                     // YYYY-MM-DD
    val newStockCount: Int,               // 신규 편입 종목 수
    val newStockAmount: Long,             // 신규 편입 총 금액
    val removedStockCount: Int,           // 편출 종목 수
    val removedStockAmount: Long,         // 편출 총 금액
    val increasedStockCount: Int,         // 비중 증가 종목 수
    val increasedStockAmount: Long,       // 비중 증가 총 금액
    val decreasedStockCount: Int,         // 비중 감소 종목 수
    val decreasedStockAmount: Long,       // 비중 감소 총 금액
    val cashDepositAmount: Long,          // 예금 총액
    val cashDepositChange: Long,          // 예금 변동액
    val cashDepositChangeRate: Double,    // 예금 변동률 (%)
    val totalEtfCount: Int,               // 총 ETF 수
    val totalHoldingAmount: Long,         // 총 보유 금액
    val calculatedAt: Long                // 계산 시점
)
```

### 5.2 신규 DAO: DailyEtfStatisticsDao

```kotlin
@Dao
interface DailyEtfStatisticsDao {
    @Query("SELECT * FROM daily_etf_statistics WHERE date = :date")
    suspend fun getByDate(date: String): DailyEtfStatisticsEntity?

    @Query("SELECT * FROM daily_etf_statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getInRange(startDate: String, endDate: String): List<DailyEtfStatisticsEntity>

    @Query("SELECT * FROM daily_etf_statistics ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DailyEtfStatisticsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statistics: DailyEtfStatisticsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statistics: List<DailyEtfStatisticsEntity>)

    @Query("DELETE FROM daily_etf_statistics WHERE date < :date")
    suspend fun deleteOlderThan(date: String)
}
```

### 5.3 신규 Domain Model

```kotlin
// 기간별 비교 결과
data class ComparisonResult(
    val currentDate: String,
    val previousDate: String,
    val items: List<HoldingWithComparison>,
    val summary: ComparisonSummary
)

data class HoldingWithComparison(
    val stockCode: String,
    val stockName: String,
    val currentWeight: Float,
    val previousWeight: Float,
    val currentAmount: Long,
    val previousAmount: Long,
    val status: HoldingStatus,
    val etfNames: List<String>
) {
    val weightChange: Float get() = currentWeight - previousWeight
    val amountChange: Long get() = currentAmount - previousAmount
}

data class ComparisonSummary(
    val newCount: Int,
    val removedCount: Int,
    val increasedCount: Int,
    val decreasedCount: Int,
    val maintainCount: Int
)

// 예금 추이 데이터
data class CashDepositTrend(
    val date: String,
    val totalAmount: Long,
    val changeAmount: Long,
    val changeRate: Double
)

// ETF별 예금 현황
data class EtfCashDetail(
    val etfCode: String,
    val etfName: String,
    val cashAmount: Long,
    val cashWeight: Double,
    val cashName: String
)

// 종목 분석 결과
data class StockAnalysisResult(
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfCount: Int,
    val amountHistory: List<AmountHistory>,
    val weightHistory: List<WeightHistory>,
    val containingEtfs: List<ContainingEtfInfo>
)

data class ContainingEtfInfo(
    val etfCode: String,
    val etfName: String,
    val weight: Double,
    val amount: Long,
    val collectedDate: String
)
```

### 5.4 UseCase 확장

```kotlin
// 기간별 비교 조회
class GetComparisonInRangeUC @Inject constructor(
    private val repository: EtfRepository
) {
    suspend operator fun invoke(
        dateRange: DateRangeOption
    ): Result<ComparisonResult>
}

// 예금 추이 조회
class GetCashDepositTrendUC @Inject constructor(
    private val repository: EtfRepository
) {
    suspend operator fun invoke(
        dateRange: DateRangeOption
    ): Result<List<CashDepositTrend>>
}

// 종목 분석
class GetStockAnalysisUC @Inject constructor(
    private val repository: EtfRepository
) {
    suspend operator fun invoke(
        stockCode: String
    ): Result<StockAnalysisResult>
}

// 일별 통계 계산 (수집 후 호출)
class CalculateDailyStatisticsUC @Inject constructor(
    private val repository: EtfRepository
) {
    suspend operator fun invoke(date: String): Result<Unit>
}
```

---

## 6. 구현 계획

### Phase 1: 데이터 레이어 확장 (1-2일)

| 단계 | 작업 | 상세 |
|------|------|------|
| 1.1 | Entity 추가 | `DailyEtfStatisticsEntity` 생성 |
| 1.2 | DAO 추가 | `DailyEtfStatisticsDao` 생성 |
| 1.3 | Database 마이그레이션 | Room 스키마 버전 업데이트 |
| 1.4 | Repository 확장 | 통계 관련 메서드 추가 |

### Phase 2: Domain 레이어 확장 (1일)

| 단계 | 작업 | 상세 |
|------|------|------|
| 2.1 | 도메인 모델 추가 | `ComparisonResult`, `CashDepositTrend` 등 |
| 2.2 | UseCase 추가 | 4개 UseCase 구현 |
| 2.3 | 통계 계산 로직 | 일별 통계 계산 함수 |

### Phase 3: UI 컴포넌트 (2-3일)

| 단계 | 작업 | 상세 |
|------|------|------|
| 3.1 | DateRangeSelector | 기간 선택 컴포넌트 |
| 3.2 | StatisticsHubContent | 7개 탭 컨테이너 |
| 3.3 | AmountRankingContent | 보유량 순위 화면 (기존 확장) |
| 3.4 | StockChangeContent | 변동 종목 화면 (기존 확장) |
| 3.5 | CashDepositContent | 예금 추이 화면 |
| 3.6 | StockAnalysisContent | 종목 분석 화면 |

### Phase 4: ViewModel 확장 (1일)

| 단계 | 작업 | 상세 |
|------|------|------|
| 4.1 | 상태 클래스 추가 | `StatisticsState`, `CashDepositState` 등 |
| 4.2 | ViewModel 메서드 | 기간 선택, 데이터 로드 |
| 4.3 | 이벤트 핸들링 | 종목 클릭, 네비게이션 |

### Phase 5: 통합 및 테스트 (1일)

| 단계 | 작업 | 상세 |
|------|------|------|
| 5.1 | EtfScreen 수정 | 탭 구조 변경 |
| 5.2 | 네비게이션 연동 | 수급 분석 화면 연결 |
| 5.3 | 테스트 | UI 테스트, 로직 테스트 |

---

## 7. 코드 패턴

### 7.1 ViewModel 상태 패턴 (기존 준수)

```kotlin
// 통계 상태
sealed class StatisticsState {
    data object Loading : StatisticsState()
    data object NoData : StatisticsState()
    data class Success(
        val selectedSubTab: StatisticsSubTab,
        val selectedDateRange: DateRangeOption,
        val currentDate: String,
        val previousDate: String,
        val rankingResult: StockRankingResult?,
        val changesResult: StockChangesResult?,
        val cashDepositTrend: List<CashDepositTrend>?,
        val stockAnalysisResult: StockAnalysisResult?
    ) : StatisticsState()
    data class Error(val message: String) : StatisticsState()
}
```

### 7.2 금액 포맷팅 유틸 (기존 패턴)

```kotlin
object AmountFormatter {
    fun format(amount: Long): String {
        return when {
            amount >= 1_000_000_000_000 -> String.format("%.1f조", amount / 1_000_000_000_000.0)
            amount >= 100_000_000 -> String.format("%.0f억", amount / 100_000_000.0)
            amount >= 10_000 -> String.format("%.0f만", amount / 10_000.0)
            else -> NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
        }
    }

    fun formatWithSign(amount: Long): String {
        val sign = if (amount > 0) "+" else ""
        return sign + format(kotlin.math.abs(amount))
    }
}
```

### 7.3 날짜 유틸

```kotlin
object DateFormatter {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun format(date: LocalDate): String = date.format(formatter)

    fun parse(dateStr: String): LocalDate = LocalDate.parse(dateStr, formatter)

    fun getDateRangeStart(option: DateRangeOption): String {
        if (option == DateRangeOption.ALL) return "1970-01-01"
        val cutoff = LocalDate.now().minusDays(option.days.toLong())
        return format(cutoff)
    }
}
```

### 7.4 Repository 확장 메서드

```kotlin
interface EtfRepository {
    // 기존 메서드...

    // 신규 메서드
    suspend fun getComparisonInRange(
        startDate: String,
        endDate: String
    ): Result<ComparisonResult>

    suspend fun getCashDepositTrend(
        startDate: String,
        endDate: String
    ): Result<List<CashDepositTrend>>

    suspend fun getStockAnalysis(stockCode: String): Result<StockAnalysisResult>

    suspend fun calculateDailyStatistics(date: String): Result<Unit>

    suspend fun getDailyStatistics(date: String): Result<DailyEtfStatisticsEntity?>

    suspend fun getDailyStatisticsInRange(
        startDate: String,
        endDate: String
    ): Result<List<DailyEtfStatisticsEntity>>
}
```

---

## 부록 A: 문자열 리소스 (한국어)

```xml
<!-- strings.xml 추가 -->
<resources>
    <!-- 통계 탭 -->
    <string name="etf_statistics_tab">통계</string>
    <string name="etf_tab_amount_ranking">보유량 순위</string>
    <string name="etf_tab_newly_included">신규 편입</string>
    <string name="etf_tab_removed">편출</string>
    <string name="etf_tab_weight_increased">비중 증가</string>
    <string name="etf_tab_weight_decreased">비중 감소</string>
    <string name="etf_tab_cash_deposit">예금 추이</string>
    <string name="etf_tab_stock_analysis">종목 분석</string>

    <!-- 기간 선택 -->
    <string name="date_range_day">1일</string>
    <string name="date_range_week">1주</string>
    <string name="date_range_month">1개월</string>
    <string name="date_range_three_months">3개월</string>
    <string name="date_range_six_months">6개월</string>
    <string name="date_range_year">1년</string>
    <string name="date_range_all">전체</string>

    <!-- 상태 -->
    <string name="status_new">신규</string>
    <string name="status_increase">증가</string>
    <string name="status_decrease">감소</string>
    <string name="status_removed">편출</string>
    <string name="status_maintain">유지</string>

    <!-- 분석 -->
    <string name="stock_search_hint">종목명 또는 코드 검색</string>
    <string name="navigate_to_analysis">종목 분석</string>
    <string name="etf_count_format">%d개 ETF</string>
    <string name="date_range_format">%s ~ %s</string>
</resources>
```

---

## 부록 B: 기존 코드 수정 사항

### B.1 EtfTab enum 수정

```kotlin
// 기존
enum class EtfTab(val title: String) {
    COLLECTION_STATUS("수집현황"),
    STOCK_RANKING("종목랭킹"),
    STOCK_CHANGES("변동종목"),
    SETTINGS("설정")
}

// 변경
enum class EtfTab(val title: String) {
    COLLECTION_STATUS("수집현황"),
    STATISTICS("통계"),    // STOCK_RANKING, STOCK_CHANGES 통합
    SETTINGS("설정")
}
```

### B.2 EtfVm 확장

```kotlin
@HiltViewModel
class EtfVm @Inject constructor(
    // 기존 의존성...
    private val getComparisonInRangeUC: GetComparisonInRangeUC,  // 추가
    private val getCashDepositTrendUC: GetCashDepositTrendUC,    // 추가
    private val getStockAnalysisUC: GetStockAnalysisUC           // 추가
) : ViewModel() {

    // 통계 서브탭
    private val _selectedSubTab = MutableStateFlow(StatisticsSubTab.AMOUNT_RANKING)
    val selectedSubTab: StateFlow<StatisticsSubTab> = _selectedSubTab.asStateFlow()

    // 기간 선택
    private val _selectedDateRange = MutableStateFlow(DateRangeOption.WEEK)
    val selectedDateRange: StateFlow<DateRangeOption> = _selectedDateRange.asStateFlow()

    // 예금 추이 상태
    private val _cashDepositState = MutableStateFlow<CashDepositState>(CashDepositState.Loading)
    val cashDepositState: StateFlow<CashDepositState> = _cashDepositState.asStateFlow()

    // 종목 분석 상태
    private val _stockAnalysisState = MutableStateFlow<StockAnalysisState>(StockAnalysisState.Idle)
    val stockAnalysisState: StateFlow<StockAnalysisState> = _stockAnalysisState.asStateFlow()

    // 검색 쿼리
    private val _analysisQuery = MutableStateFlow("")
    val analysisQuery: StateFlow<String> = _analysisQuery.asStateFlow()

    fun selectSubTab(tab: StatisticsSubTab) {
        _selectedSubTab.value = tab
        when (tab) {
            StatisticsSubTab.CASH_DEPOSIT -> loadCashDepositData()
            else -> { /* 기존 로직 */ }
        }
    }

    fun selectDateRange(range: DateRangeOption) {
        _selectedDateRange.value = range
        reloadCurrentTab()
    }

    private fun loadCashDepositData() {
        viewModelScope.launch {
            _cashDepositState.value = CashDepositState.Loading
            getCashDepositTrendUC(_selectedDateRange.value).fold(
                onSuccess = { data ->
                    _cashDepositState.value = CashDepositState.Success(data)
                },
                onFailure = { error ->
                    _cashDepositState.value = CashDepositState.Error(
                        error.message ?: "데이터 로드 실패"
                    )
                }
            )
        }
    }

    fun searchStock(query: String) {
        _analysisQuery.value = query
        if (query.length >= 2) {
            viewModelScope.launch {
                _stockAnalysisState.value = StockAnalysisState.Loading
                getStockAnalysisUC(query).fold(
                    onSuccess = { result ->
                        _stockAnalysisState.value = StockAnalysisState.Success(result)
                    },
                    onFailure = { error ->
                        _stockAnalysisState.value = StockAnalysisState.Error(
                            error.message ?: "분석 실패"
                        )
                    }
                )
            }
        } else {
            _stockAnalysisState.value = StockAnalysisState.Idle
        }
    }
}
```

### B.3 AppDatabase 스키마 업데이트

```kotlin
@Database(
    entities = [
        // 기존 Entity...
        DailyEtfStatisticsEntity::class  // 추가
    ],
    version = 2,  // 버전 증가
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    // 기존 DAO...
    abstract fun dailyEtfStatisticsDao(): DailyEtfStatisticsDao  // 추가
}
```

---

**문서 끝**

*이 명세서는 기존 StockApp ETF 기능을 확장하여 통계 분석 기능을 추가하는 가이드입니다. 기존 코드 패턴과 아키텍처를 준수하며, 데이터 수집 기능은 기존 구현을 그대로 활용합니다.*
