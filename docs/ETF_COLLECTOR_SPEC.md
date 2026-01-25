# ETF Collector Android Feature Specification

**작성일:** 2026-01-25
**버전:** 1.1
**상태:** 🟢 구현 완료

---

## 1. 개요

### 1.1 목적

기존 etf-collector Python 프로젝트를 Android StockApp에 통합하여 ETF 구성종목 데이터를 수집, 저장, 분석하는 기능을 제공합니다.

### 1.2 주요 기능

| # | 기능 | 설명 |
|---|------|------|
| 1 | ETF 리스트 수집 | 전체 ETF 리스트 수집 (Kiwoom API 또는 사전정의 목록) |
| 2 | ETF 필터링 | 액티브 ETF 및 사용자 키워드 기반 필터링 |
| 3 | 구성종목 수집 | 필터링된 ETF의 구성종목 및 비중 데이터 수집 (KIS API) |
| 4 | 키워드 설정 | 설정 화면에서 포함/제외 키워드 관리 |
| 5 | ETF 통계 | 금액 순위, 신규 편입, 제외, 비중 변동 종목 분석 |
| 6 | 종목 차트 | 금액 추이, 비중 추이, 평균 비중 추이 시각화 |

### 1.3 기술 스택

| 기술 | 용도 |
|------|------|
| Chaquopy | Python etf-collector 통합 |
| Room | ETF/구성종목 데이터 저장 |
| Vico Charts | 추이 차트 시각화 |
| Hilt | 의존성 주입 |
| Jetpack Compose | UI |

---

## 2. 아키텍처

### 2.1 시스템 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    Android StockApp                          │
├─────────────────────────────────────────────────────────────┤
│  feature/etf/                                               │
│  ├── ui/EtfStatsScreen          # ETF 통계 메인 화면        │
│  ├── ui/EtfDetailScreen         # 종목 상세 차트            │
│  └── ui/EtfCollectorVm          # ViewModel                 │
├─────────────────────────────────────────────────────────────┤
│  feature/settings/                                          │
│  └── ui/EtfKeywordTab           # 키워드 설정 탭            │
├─────────────────────────────────────────────────────────────┤
│  core/db/                                                   │
│  ├── entity/EtfEntity           # ETF 정보                  │
│  ├── entity/EtfConstituentEntity # 구성종목 일별 데이터     │
│  ├── entity/EtfKeywordEntity    # 키워드 설정               │
│  └── dao/Etf*Dao                # DAO 인터페이스            │
├─────────────────────────────────────────────────────────────┤
│  core/py/PyClient                                           │
│  └── etf_collector 모듈 호출                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              etf-collector (Python via Chaquopy)            │
├─────────────────────────────────────────────────────────────┤
│  android_api.py                                             │
│  ├── get_etf_list()            # ETF 목록 조회              │
│  └── get_constituents()         # 구성종목 조회             │
├─────────────────────────────────────────────────────────────┤
│  API Integration                                            │
│  ├── Kiwoom API (ka40004)      # ETF 전체시세              │
│  └── KIS API (FHKST121600C0)   # ETF 구성종목               │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 데이터 흐름

```
1. 사용자가 ETF 수집 시작
   │
   ▼
2. EtfCollectorVm → PyClient → etf_collector.android_api
   │
   ├── get_etf_list() → ETF 목록 (Kiwoom 또는 사전정의)
   │
   ├── 키워드 필터링 (Room에서 키워드 조회)
   │   ├── 액티브 ETF 필터
   │   ├── 포함 키워드 필터
   │   └── 제외 키워드 필터
   │
   └── get_constituents() × N개 ETF → 구성종목 데이터
       │
       ▼
3. Room DB 저장
   ├── EtfEntity (ETF 정보)
   └── EtfConstituentEntity (일별 구성종목 스냅샷)
       │
       ▼
4. 통계 계산 (Room Query)
   ├── 금액 순위 집계
   ├── 신규 편입 종목 추출
   ├── 제외 종목 추출
   ├── 비중 증가 종목 추출
   └── 비중 감소 종목 추출
       │
       ▼
5. UI 표시
   ├── EtfStatsScreen (탭별 테이블)
   └── EtfDetailScreen (차트)
```

---

## 3. 데이터베이스 스키마

### 3.1 Entity 정의

#### EtfEntity (ETF 기본 정보)

```kotlin
@Entity(tableName = "etfs")
data class EtfEntity(
    @PrimaryKey
    val etfCode: String,              // ETF 코드 (6자리)
    val etfName: String,              // ETF 명
    val etfType: String,              // "Active" / "Passive"
    val managementCompany: String,    // 운용사
    val trackingIndex: String,        // 추적 지수
    val assetClass: String,           // 자산 유형
    val totalAssets: Double,          // 총 자산 (억원)
    val isFiltered: Boolean,          // 필터링 대상 여부
    val updatedAt: Long               // 마지막 업데이트 시간
)
```

#### EtfConstituentEntity (구성종목 일별 스냅샷)

```kotlin
@Entity(
    tableName = "etf_constituents",
    primaryKeys = ["etfCode", "stockCode", "collectedDate"],
    indices = [
        Index("stockCode"),
        Index("collectedDate"),
        Index(value = ["etfCode", "collectedDate"])
    ]
)
data class EtfConstituentEntity(
    val etfCode: String,              // ETF 코드
    val etfName: String,              // ETF 명
    val stockCode: String,            // 종목 코드
    val stockName: String,            // 종목명
    val currentPrice: Int,            // 현재가
    val priceChange: Int,             // 전일대비
    val priceChangeSign: String,      // 부호 (1-5)
    val priceChangeRate: Double,      // 등락률
    val volume: Long,                 // 거래량
    val tradingValue: Long,           // 거래대금
    val marketCap: Long,              // 시가총액
    val weight: Double,               // 비중 (%)
    val evaluationAmount: Long,       // 평가금액
    val collectedDate: String,        // 수집일 (YYYY-MM-DD)
    val collectedAt: Long             // 수집 시간 (timestamp)
)
```

#### EtfKeywordEntity (필터 키워드)

```kotlin
@Entity(tableName = "etf_keywords")
data class EtfKeywordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,              // 키워드
    val filterType: String,           // "INCLUDE" / "EXCLUDE"
    val isEnabled: Boolean = true,    // 활성화 여부
    val createdAt: Long               // 생성 시간
)
```

#### EtfCollectionHistoryEntity (수집 히스토리)

```kotlin
@Entity(tableName = "etf_collection_history")
data class EtfCollectionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val collectedDate: String,        // 수집일
    val totalEtfs: Int,               // 수집 ETF 수
    val totalConstituents: Int,       // 수집 구성종목 수
    val status: String,               // SUCCESS / FAILED / PARTIAL
    val errorMessage: String?,        // 에러 메시지
    val startedAt: Long,              // 시작 시간
    val completedAt: Long?            // 완료 시간
)
```

### 3.2 DAO 인터페이스

#### EtfDao

```kotlin
@Dao
interface EtfDao {
    @Query("SELECT * FROM etfs WHERE isFiltered = 1 ORDER BY etfName")
    suspend fun getFilteredEtfs(): List<EtfEntity>

    @Query("SELECT * FROM etfs ORDER BY etfName")
    suspend fun getAllEtfs(): List<EtfEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(etfs: List<EtfEntity>)

    @Query("UPDATE etfs SET isFiltered = :isFiltered WHERE etfCode = :etfCode")
    suspend fun updateFilterStatus(etfCode: String, isFiltered: Boolean)
}
```

#### EtfConstituentDao

```kotlin
@Dao
interface EtfConstituentDao {
    // 특정 날짜의 구성종목 조회
    @Query("""
        SELECT * FROM etf_constituents
        WHERE collectedDate = :date
        ORDER BY evaluationAmount DESC
    """)
    suspend fun getByDate(date: String): List<EtfConstituentEntity>

    // 종목별 금액 합계 순위
    @Query("""
        SELECT stockCode, stockName,
               SUM(evaluationAmount) as totalAmount,
               COUNT(DISTINCT etfCode) as etfCount
        FROM etf_constituents
        WHERE collectedDate = :date
        GROUP BY stockCode
        ORDER BY totalAmount DESC
        LIMIT :limit
    """)
    suspend fun getStockRankingByAmount(date: String, limit: Int): List<StockAmountRanking>

    // 신규 편입 종목 (전일 대비)
    @Query("""
        SELECT t.stockCode, t.stockName,
               SUM(t.evaluationAmount) as totalAmount,
               GROUP_CONCAT(DISTINCT t.etfName) as etfNames
        FROM etf_constituents t
        WHERE t.collectedDate = :today
          AND NOT EXISTS (
              SELECT 1 FROM etf_constituents p
              WHERE p.stockCode = t.stockCode
                AND p.etfCode = t.etfCode
                AND p.collectedDate = :yesterday
          )
        GROUP BY t.stockCode
        ORDER BY totalAmount DESC
    """)
    suspend fun getNewlyIncludedStocks(today: String, yesterday: String): List<StockChangeInfo>

    // 제외 종목
    @Query("""
        SELECT y.stockCode, y.stockName,
               SUM(y.evaluationAmount) as totalAmount,
               GROUP_CONCAT(DISTINCT y.etfName) as etfNames
        FROM etf_constituents y
        WHERE y.collectedDate = :yesterday
          AND NOT EXISTS (
              SELECT 1 FROM etf_constituents t
              WHERE t.stockCode = y.stockCode
                AND t.etfCode = y.etfCode
                AND t.collectedDate = :today
          )
        GROUP BY y.stockCode
        ORDER BY totalAmount DESC
    """)
    suspend fun getRemovedStocks(today: String, yesterday: String): List<StockChangeInfo>

    // 비중 증가 종목
    @Query("""
        SELECT t.stockCode, t.stockName,
               SUM(t.evaluationAmount) as totalAmount,
               GROUP_CONCAT(DISTINCT t.etfName) as etfNames
        FROM etf_constituents t
        JOIN etf_constituents y ON t.stockCode = y.stockCode
                                AND t.etfCode = y.etfCode
                                AND y.collectedDate = :yesterday
        WHERE t.collectedDate = :today
          AND t.weight > y.weight + :threshold
        GROUP BY t.stockCode
        ORDER BY totalAmount DESC
    """)
    suspend fun getWeightIncreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double = 0.1
    ): List<StockChangeInfo>

    // 비중 감소 종목
    @Query("""
        SELECT t.stockCode, t.stockName,
               SUM(t.evaluationAmount) as totalAmount,
               GROUP_CONCAT(DISTINCT t.etfName) as etfNames
        FROM etf_constituents t
        JOIN etf_constituents y ON t.stockCode = y.stockCode
                                AND t.etfCode = y.etfCode
                                AND y.collectedDate = :yesterday
        WHERE t.collectedDate = :today
          AND t.weight < y.weight - :threshold
        GROUP BY t.stockCode
        ORDER BY totalAmount DESC
    """)
    suspend fun getWeightDecreasedStocks(
        today: String,
        yesterday: String,
        threshold: Double = 0.1
    ): List<StockChangeInfo>

    // 종목별 금액 추이 (차트용)
    @Query("""
        SELECT collectedDate, SUM(evaluationAmount) as totalAmount
        FROM etf_constituents
        WHERE stockCode = :stockCode
        GROUP BY collectedDate
        ORDER BY collectedDate
    """)
    suspend fun getStockAmountHistory(stockCode: String): List<DateAmount>

    // 종목별 평균 비중 추이 (차트용)
    @Query("""
        SELECT collectedDate, AVG(weight) as avgWeight
        FROM etf_constituents
        WHERE stockCode = :stockCode
        GROUP BY collectedDate
        ORDER BY collectedDate
    """)
    suspend fun getStockWeightHistory(stockCode: String): List<DateWeight>

    // 데이터 기간 조회
    @Query("""
        SELECT MIN(collectedDate) as startDate, MAX(collectedDate) as endDate
        FROM etf_constituents
    """)
    suspend fun getDataDateRange(): DateRange?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(constituents: List<EtfConstituentEntity>)

    // 오래된 데이터 삭제 (30일 이상)
    @Query("DELETE FROM etf_constituents WHERE collectedDate < :cutoffDate")
    suspend fun deleteOldData(cutoffDate: String)
}
```

#### EtfKeywordDao

```kotlin
@Dao
interface EtfKeywordDao {
    @Query("SELECT * FROM etf_keywords WHERE isEnabled = 1 ORDER BY filterType, keyword")
    suspend fun getEnabledKeywords(): List<EtfKeywordEntity>

    @Query("SELECT * FROM etf_keywords WHERE filterType = :type AND isEnabled = 1")
    suspend fun getKeywordsByType(type: String): List<EtfKeywordEntity>

    @Insert
    suspend fun insert(keyword: EtfKeywordEntity): Long

    @Delete
    suspend fun delete(keyword: EtfKeywordEntity)

    @Query("UPDATE etf_keywords SET isEnabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)
}
```

### 3.3 Query Result Models

```kotlin
data class StockAmountRanking(
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfCount: Int
)

data class StockChangeInfo(
    val stockCode: String,
    val stockName: String,
    val totalAmount: Long,
    val etfNames: String  // comma-separated
)

data class DateAmount(
    val collectedDate: String,
    val totalAmount: Long
)

data class DateWeight(
    val collectedDate: String,
    val avgWeight: Double
)

data class DateRange(
    val startDate: String,
    val endDate: String
)
```

---

## 4. UI 설계

### 4.1 네비게이션 구조

```
Bottom Navigation (6탭으로 확장)
├── 🔍 Search
├── 📊 Analysis
├── 📈 Indicator
├── 🏆 Ranking
├── 📋 ETF Stats  ← NEW
└── ⚙️ Settings
    ├── API Key Tab
    ├── Scheduling Tab
    └── ETF Keywords Tab  ← NEW
```

### 4.2 ETF 통계 화면 (EtfStatsScreen)

```
┌────────────────────────────────────────────┐
│  ETF 통계                                   │
│  데이터 기간: 2026-01-01 ~ 2026-01-25      │
├────────────────────────────────────────────┤
│ [금액순위] [신규편입] [제외] [비중↑] [비중↓] │  ← 탭
├────────────────────────────────────────────┤
│  ┌──────────────────────────────────────┐  │
│  │ 금액 순위 탭 (테이블)                  │  │
│  │                                       │  │
│  │ #  종목명      합산금액   ETF수   신규  │  │
│  │ 1  삼성전자    15.2조    35     +2    │  │
│  │ 2  SK하이닉스  8.5조     28     +1    │  │
│  │ 3  NAVER      4.2조     22     0     │  │
│  │ ...                                   │  │
│  │                                       │  │
│  │ [새로고침]           마지막: 10:30    │  │
│  └──────────────────────────────────────┘  │
│                                            │
│  ┌──────────────────────────────────────┐  │
│  │ 신규 편입 탭 (테이블)                  │  │
│  │                                       │  │
│  │ #  종목명      합산금액   편입 ETF     │  │
│  │ 1  에이비온    152억     KODEX AI     │  │
│  │ 2  아이씨케이  98억      TIGER 반도체  │  │
│  │ ...                                   │  │
│  └──────────────────────────────────────┘  │
└────────────────────────────────────────────┘
```

### 4.3 종목 상세 차트 (Dialog/BottomSheet)

```
┌────────────────────────────────────────────┐
│  삼성전자 (005930)                    [X]  │
├────────────────────────────────────────────┤
│  [금액 추이] [비중 추이] [평균비중 추이]    │
├────────────────────────────────────────────┤
│                                            │
│  금액 추이 차트 (Vico LineChart)           │
│  ┌──────────────────────────────────────┐  │
│  │         ╭──────╮                     │  │
│  │    ╭───╯      ╰──────╮              │  │
│  │ ───╯                  ╰────         │  │
│  │                                     │  │
│  │ Jan 01  Jan 08  Jan 15  Jan 22     │  │
│  └──────────────────────────────────────┘  │
│                                            │
│  포함 ETF 목록:                            │
│  • KODEX 200 (비중 5.2%)                   │
│  • TIGER 반도체 (비중 12.3%)               │
│  • ACE AI반도체 (비중 8.1%)                │
│                                            │
└────────────────────────────────────────────┘
```

### 4.4 ETF 키워드 설정 탭

```
┌────────────────────────────────────────────┐
│  ETF 필터 키워드 설정                       │
├────────────────────────────────────────────┤
│                                            │
│  ☑ 액티브 ETF만 수집                       │
│                                            │
│  ── 포함 키워드 ──────────────────────────  │
│  [ 키워드 입력...              ] [추가]    │
│                                            │
│  ┌─────────────┐ ┌─────────────┐          │
│  │ 반도체    ✕ │ │ AI        ✕ │          │
│  └─────────────┘ └─────────────┘          │
│  ┌─────────────┐                          │
│  │ 2차전지   ✕ │                          │
│  └─────────────┘                          │
│                                            │
│  ── 제외 키워드 ──────────────────────────  │
│  [ 키워드 입력...              ] [추가]    │
│                                            │
│  ┌─────────────┐ ┌─────────────┐          │
│  │ 레버리지  ✕ │ │ 인버스    ✕ │          │
│  └─────────────┘ └─────────────┘          │
│  ┌─────────────┐ ┌─────────────┐          │
│  │ 2X        ✕ │ │ 3X        ✕ │          │
│  └─────────────┘ └─────────────┘          │
│                                            │
│  [수집 시작]                마지막: 10:30  │
└────────────────────────────────────────────┘
```

---

## 5. 구현 계획

### 5.1 Phase 구성

| Phase | 기능 | 예상 파일 수 | 우선순위 |
|-------|------|------------|---------|
| Phase 1 | DB 스키마 및 DAO | 8 files | 필수 |
| Phase 2 | 키워드 설정 UI | 5 files | 필수 |
| Phase 3 | ETF 수집 로직 | 6 files | 필수 |
| Phase 4 | ETF 통계 화면 | 8 files | 필수 |
| Phase 5 | 종목 상세 차트 | 4 files | 필수 |
| Phase 6 | 네비게이션 통합 | 2 files | 필수 |

### 5.2 상세 구현 계획

#### Phase 1: 데이터베이스 스키마 (DB Layer)

**목표:** ETF 및 구성종목 데이터 저장을 위한 Room 스키마 구현

**파일 목록:**
```
core/db/entity/
├── EtfEntity.kt                 # ETF 정보
├── EtfConstituentEntity.kt      # 구성종목 일별 스냅샷
├── EtfKeywordEntity.kt          # 필터 키워드
└── EtfCollectionHistoryEntity.kt # 수집 히스토리

core/db/dao/
├── EtfDao.kt                    # ETF DAO
├── EtfConstituentDao.kt         # 구성종목 DAO
├── EtfKeywordDao.kt             # 키워드 DAO
└── EtfCollectionHistoryDao.kt   # 히스토리 DAO

core/db/
└── AppDb.kt                     # 버전 업그레이드 (v5 → v6)
```

**마이그레이션:**
- Version 5 → 6: 4개 테이블 추가

---

#### Phase 2: 키워드 설정 UI (Settings Tab)

**목표:** 설정 화면에 ETF 키워드 관리 탭 추가

**파일 목록:**
```
feature/settings/
├── domain/
│   ├── model/EtfKeywordConfig.kt    # 키워드 모델
│   └── usecase/
│       ├── GetEtfKeywordsUC.kt
│       ├── AddEtfKeywordUC.kt
│       └── DeleteEtfKeywordUC.kt
├── ui/
│   ├── EtfKeywordTab.kt             # 키워드 설정 탭 Composable
│   └── SettingsScreen.kt            # 탭 추가 (기존 파일 수정)
└── SettingsVm.kt                    # 키워드 상태 추가 (기존 파일 수정)
```

---

#### Phase 3: ETF 수집 로직 (Data Layer)

**목표:** Python etf-collector를 통한 데이터 수집 및 저장

**파일 목록:**
```
feature/etf/
├── domain/
│   ├── model/
│   │   ├── EtfModels.kt             # EtfInfo, ConstituentStock
│   │   └── CollectionResult.kt      # 수집 결과
│   ├── repo/EtfCollectorRepo.kt     # Repository 인터페이스
│   └── usecase/
│       ├── CollectEtfDataUC.kt      # 수집 UseCase
│       └── GetFilteredEtfsUC.kt     # 필터링 UseCase
└── data/
    └── repo/EtfCollectorRepoImpl.kt # Repository 구현 (PyClient 사용)

core/py/
└── PyClient.kt                      # etf_collector 모듈 호출 추가

app/src/main/python/
└── etf_collector/                   # Python 패키지 복사
```

**Python 호출:**
```kotlin
// PyClient를 통한 etf_collector 호출
val result = pyClient.call(
    module = "etf_collector.android_api",
    func = "get_etf_list",
    args = listOf(configJson, true)  // use_predefined=true
) { json -> json.decodeFromString<EtfListResponse>(json) }

val constituents = pyClient.call(
    module = "etf_collector.android_api",
    func = "get_constituents",
    args = listOf(configJson, etfCode)
) { json -> json.decodeFromString<ConstituentResponse>(json) }
```

---

#### Phase 4: ETF 통계 화면 (Feature Module)

**목표:** ETF 통계 메인 화면 및 5개 탭 구현

**파일 목록:**
```
feature/etf/
├── domain/
│   ├── model/
│   │   ├── EtfStatsModels.kt        # 통계 모델
│   │   └── StockRankingItem.kt      # 순위 아이템
│   ├── repo/EtfStatsRepo.kt         # 통계 Repository 인터페이스
│   └── usecase/
│       ├── GetAmountRankingUC.kt
│       ├── GetNewlyIncludedUC.kt
│       ├── GetRemovedStocksUC.kt
│       ├── GetWeightIncreasedUC.kt
│       └── GetWeightDecreasedUC.kt
├── data/
│   └── repo/EtfStatsRepoImpl.kt     # Room 쿼리 활용
├── ui/
│   ├── EtfStatsScreen.kt            # 메인 화면
│   ├── EtfStatsVm.kt                # ViewModel
│   ├── tabs/
│   │   ├── AmountRankingTab.kt      # 금액 순위 탭
│   │   ├── NewlyIncludedTab.kt      # 신규 편입 탭
│   │   ├── RemovedStocksTab.kt      # 제외 종목 탭
│   │   ├── WeightIncreasedTab.kt    # 비중 증가 탭
│   │   └── WeightDecreasedTab.kt    # 비중 감소 탭
│   └── components/
│       └── StockRankingTable.kt     # 공통 테이블 컴포넌트
└── di/
    └── EtfModule.kt                 # Hilt 모듈
```

---

#### Phase 5: 종목 상세 차트 (Detail View)

**목표:** 종목 선택 시 금액/비중 추이 차트 표시

**파일 목록:**
```
feature/etf/
└── ui/
    ├── detail/
    │   ├── StockDetailDialog.kt     # 상세 Dialog
    │   ├── StockDetailVm.kt         # 차트 데이터 ViewModel
    │   └── charts/
    │       ├── AmountTrendChart.kt  # 금액 추이 차트
    │       ├── WeightTrendChart.kt  # 비중 추이 차트
    │       └── AvgWeightChart.kt    # 평균 비중 추이 차트
    └── EtfStatsVm.kt                # 상세 화면 연동 (수정)
```

**Vico Charts 사용:**
```kotlin
// 금액 추이 라인 차트
CartesianChartHost(
    chart = rememberCartesianChart(
        rememberLineCartesianLayer()
    ),
    modelProducer = modelProducer
)
```

---

#### Phase 6: 네비게이션 통합

**목표:** Bottom Navigation에 ETF Stats 탭 추가

**파일 목록:**
```
nav/
├── Nav.kt          # Screen.EtfStats 추가
└── NavGraph.kt     # EtfStatsScreen 라우트 추가
```

---

## 6. API 연동

### 6.1 KIS API 설정 (Settings에 추가)

ETF 구성종목 수집을 위해 KIS API 키 설정이 필요합니다.

```kotlin
// SettingsScreen에 KIS API 탭 또는 필드 추가
data class KisApiConfig(
    val appKey: String,
    val appSecret: String,
    val environment: String  // "real" | "virtual"
)
```

### 6.2 Python 모듈 구조

```
app/src/main/python/
└── etf_collector/
    ├── __init__.py
    ├── android_api.py         # Android 진입점
    ├── config.py
    ├── auth/
    │   ├── kis_auth.py
    │   └── kiwoom_auth.py
    ├── collector/
    │   ├── constituent.py     # 구성종목 수집
    │   ├── etf_list.py        # 사전정의 목록
    │   └── kiwoom_etf_list.py # Kiwoom API
    ├── filter/
    │   └── keyword.py         # 키워드 필터
    ├── limiter/
    │   └── rate_limiter.py
    ├── data/
    │   └── active_etf_codes.py
    └── utils/
        ├── helpers.py
        ├── validators.py
        └── logger.py
```

---

## 7. 에러 처리

### 7.1 에러 코드

| 코드 | 설명 | 사용자 메시지 |
|------|------|--------------|
| `NO_API_KEY` | API 키 미설정 | "설정에서 API 키를 입력해주세요" |
| `AUTH_ERROR` | 인증 실패 | "API 인증에 실패했습니다" |
| `NETWORK_ERROR` | 네트워크 오류 | "네트워크 연결을 확인해주세요" |
| `API_ERROR` | API 응답 오류 | "데이터 조회에 실패했습니다" |
| `NO_DATA` | 데이터 없음 | "수집된 데이터가 없습니다" |
| `RATE_LIMIT` | 요청 제한 | "잠시 후 다시 시도해주세요" |

### 7.2 UI 상태

```kotlin
sealed class EtfStatsState {
    data object Loading : EtfStatsState()
    data object NoApiKey : EtfStatsState()
    data object NoData : EtfStatsState()
    data class Success(
        val dateRange: DateRange,
        val amountRanking: List<StockAmountRanking>,
        val newlyIncluded: List<StockChangeInfo>,
        val removed: List<StockChangeInfo>,
        val weightIncreased: List<StockChangeInfo>,
        val weightDecreased: List<StockChangeInfo>
    ) : EtfStatsState()
    data class Error(val message: String) : EtfStatsState()
}
```

---

## 8. 테스트 계획

### 8.1 단위 테스트

| 테스트 대상 | 테스트 케이스 |
|------------|-------------|
| EtfConstituentDao | 금액 순위 쿼리, 신규 편입 쿼리, 제외 쿼리 |
| EtfKeywordDao | 키워드 CRUD, 필터 타입별 조회 |
| KeywordFilter | 포함/제외 필터링 로직 |
| EtfStatsVm | 상태 전환, 데이터 로딩 |

### 8.2 통합 테스트

| 테스트 시나리오 |
|---------------|
| ETF 수집 → DB 저장 → 통계 조회 플로우 |
| 키워드 설정 → 필터링 → 수집 플로우 |
| 종목 선택 → 차트 데이터 로딩 플로우 |

---

## 9. 향후 확장

### 9.1 가능한 확장 기능

| 기능 | 설명 | 상태 |
|------|------|------|
| 자동 수집 | WorkManager를 통한 일일 자동 수집 | ✅ 구현됨 |
| 알림 | 신규 편입/제외 종목 알림 | 미구현 |
| 내보내기 | CSV/JSON 파일 내보내기 | 미구현 |
| 히스토리 | 수집 히스토리 조회 화면 | ✅ 구현됨 |

---

## 10. 체크리스트

### 구현 전 확인사항

- [ ] KIS API 키 발급 확인
- [ ] Kiwoom API 키 발급 확인 (선택)
- [ ] etf-collector Python 패키지 복사
- [ ] Chaquopy 호환성 확인

### 구현 완료 기준

- [x] Phase 1: DB 스키마 구현 및 마이그레이션
- [x] Phase 2: 키워드 설정 UI 동작
- [x] Phase 3: ETF 수집 정상 동작 (Kotlin REST API로 구현)
- [x] Phase 4: 통계 화면 4개 탭 표시 (UI 재설계: 변동종목 통합)
- [x] Phase 5: 종목 차트 표시 (BottomSheet)
- [x] Phase 6: 네비게이션 통합
- [ ] 빌드 성공 및 Lint 통과 (환경 제약으로 미확인)
- [ ] 기본 동작 테스트 완료

### 구현 노트

1. **Python/Chaquopy 대신 Kotlin REST API 사용**: Python 패키지 통합 대신 Kotlin에서 직접 Kiwoom/KIS API를 호출하는 방식으로 구현됨
2. **UI 재설계**: 명세서의 5개 탭 대신 4개 탭으로 통합
   - 수집현황 (추가됨)
   - 종목랭킹 (금액순위)
   - 변동종목 (신규편입/제외/비중↑/비중↓ 통합, 필터 칩으로 전환)
   - 설정 (탭 내 포함)
3. **종목 상세**: Dialog 대신 ModalBottomSheet 사용

---

**문서 작성자:** Claude
**마지막 수정:** 2026-01-25
