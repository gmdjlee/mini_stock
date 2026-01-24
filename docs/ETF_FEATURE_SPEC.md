# 액티브 ETF 기능 명세서 (StockApp 최적화 버전)

## 1. 개요

### 1.1 목적
본 문서는 StockApp Android 앱에 액티브 ETF 정보 조회 기능을 추가하기 위한 명세서입니다. 키움증권 REST API를 활용하여 ETF의 기본 정보, 시세, NAV 추적 데이터 등을 수집하고 관리합니다.

### 1.2 주요 기능
1. 전체 ETF 목록 조회 및 필터링 (키워드 기반 액티브 ETF 필터링)
2. 개별 ETF 상세 정보 조회
3. ETF 일별 NAV/추적오차 추이 조회
4. ETF 외국인/기관 수급 데이터 조회
5. Room DB 기반 캐싱

### 1.3 제약사항
- **Python 패키지 FROZEN**: Kotlin REST API 직접 호출 방식 사용 (Ranking 기능과 동일)
- **구성종목 API 미제공**: 키움 API에서 ETF 구성종목 정보를 제공하지 않음
  - 대안: 외인/기관 수급, NAV 괴리율, 추적오차 분석 제공

### 1.4 참조 문서
- 키움 API 문서: `docs/kiwoom_api_docs/detail/국내주식_ETF.md`
- Ranking 기능 구현 참조: `StockApp/app/src/main/java/com/stockapp/feature/ranking/`

---

## 2. 시스템 아키텍처

### 2.1 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────────┐
│                        StockApp ETF Module                          │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐   ┌──────────────┐   ┌─────────────────────────┐  │
│  │  Settings   │   │ Rate Limiter │   │    KiwoomApiClient      │  │
│  │   Repo      │   │   (500ms)    │   │  (Kotlin REST API)      │  │
│  └─────────────┘   └──────────────┘   └─────────────────────────┘  │
│         │                 │                      │                  │
│         └─────────────────┼──────────────────────┘                  │
│                           ▼                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    ETF Data Processor                        │   │
│  │  ┌────────────┐  ┌────────────┐  ┌─────────────────────┐    │   │
│  │  │  ETF List  │  │  Keyword   │  │  ETF Detail/Daily   │    │   │
│  │  │  (ka40004) │→ │  Filter    │→ │  Data (ka40002/3/8) │    │   │
│  │  └────────────┘  └────────────┘  └─────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                           │                                         │
│                           ▼                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    Room Database                             │   │
│  │      (EtfCacheEntity, EtfDetailCacheEntity, etc.)           │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 모듈 구성 (Clean Architecture)

```
feature/etf/
├── domain/
│   ├── model/
│   │   ├── EtfModels.kt          # 도메인 모델
│   │   └── EtfParams.kt          # API 파라미터
│   ├── repo/
│   │   └── EtfRepo.kt            # Repository 인터페이스
│   └── usecase/
│       ├── GetEtfListUC.kt       # ETF 목록 조회
│       └── GetEtfDetailUC.kt     # ETF 상세 조회
├── data/
│   ├── dto/
│   │   └── EtfDto.kt             # API 응답 DTO
│   └── repo/
│       ├── EtfRepoImpl.kt        # Repository 구현
│       └── EtfParseUtils.kt      # 파싱 유틸
├── ui/
│   ├── EtfScreen.kt              # 메인 화면
│   ├── EtfVm.kt                  # ViewModel
│   └── EtfDetailSheet.kt         # 상세 바텀시트
└── di/
    └── EtfModule.kt              # Hilt DI 모듈
```

---

## 3. 데이터 수집 사양

### 3.1 사용 API 목록

| API ID | API 명 | 용도 | URL |
|--------|--------|------|-----|
| ka40004 | ETF전체시세요청 | 전체 ETF 목록 + 시세 | `/api/dostk/etf` |
| ka40002 | ETF종목정보요청 | 개별 ETF 상세 정보 | `/api/dostk/etf` |
| ka40003 | ETF일별추이요청 | NAV, 괴리율, 추적오차 추이 | `/api/dostk/etf` |
| ka40008 | ETF일자별체결요청 | 외인/기관 순매수 데이터 | `/api/dostk/etf` |

### 3.2 ETF 목록 조회 (ka40004)

#### 3.2.1 요청 파라미터

| 파라미터 | 필수 | 설명 | 예시 |
|---------|------|------|------|
| `txon_type` | Y | 과세유형 (0: 전체) | "0" |
| `navpre` | Y | NAV대비 (0: 전체, 1: 프리미엄, 2: 디스카운트) | "0" |
| `mngmcomp` | Y | 운용사 (0000: 전체) | "0000" |
| `txon_yn` | Y | 과세여부 (0: 전체) | "0" |
| `trace_idex` | Y | 추적지수 (0: 전체) | "0" |
| `stex_tp` | Y | 거래소구분 (1: KRX, 2: NXT, 3: KRX모의) | "1" |

#### 3.2.2 응답 데이터

```kotlin
data class EtfItem(
    val ticker: String,              // 종목코드 (6자리)
    val name: String,                // 종목명
    val classification: String,      // 종목분류
    val closePrice: Long,            // 종가
    val priceChange: Long,           // 전일대비
    val priceChangeSign: String,     // 대비기호 (+, -, "")
    val changeRate: Double,          // 대비율 (%)
    val volume: Long,                // 거래량
    val nav: Double,                 // NAV
    val trackingError: Double,       // 추적오차율 (%)
    val trackingIndexName: String,   // 추적지수명
    val trackingIndexCode: String,   // 추적지수코드
    val leverage: Double,            // 배수 (레버리지/인버스)
    val managementCompany: String    // 운용사
)
```

### 3.3 ETF 상세 정보 (ka40002)

#### 3.3.1 요청 파라미터

| 파라미터 | 필수 | 설명 | 예시 |
|---------|------|------|------|
| `stk_cd` | Y | 종목코드 | "069500" |

#### 3.3.2 응답 데이터

```kotlin
data class EtfDetail(
    val ticker: String,              // 종목코드
    val name: String,                // 종목명
    val trackingIndexName: String,   // ETF대상지수명
    val underlyingPrice: Double,     // 원주가격
    val taxTypeEtf: String,          // ETF과세유형
    val taxTypeEtn: String,          // ETN과세유형
    val listingDate: String,         // 상장일
    val expirationDate: String?,     // 만기일 (ETN)
    val totalAssets: Long,           // 순자산
    val sharesOutstanding: Long,     // 발행주식수
    val managementCompany: String    // 운용사
)
```

### 3.4 ETF 일별 추이 (ka40003)

#### 3.4.1 응답 데이터

```kotlin
data class EtfDailyData(
    val date: String,                // 체결일자 (YYYYMMDD)
    val closePrice: Long,            // 현재가
    val priceChange: Long,           // 전일대비
    val priceChangeSign: String,     // 대비기호
    val changeRate: Double,          // 대비율 (%)
    val volume: Long,                // 거래량
    val nav: Double,                 // NAV
    val tradingValue: Long,          // 누적거래대금
    val navIndexDisparity: Double,   // NAV/지수 괴리율 (%)
    val navEtfDisparity: Double,     // NAV/ETF 괴리율 (%)
    val trackingError: Double,       // 추적오차율 (%)
    val trackingPrice: Long,         // 추적현재가
    val trackingChange: Long         // 추적전일대비
)
```

### 3.5 ETF 일자별 체결 (ka40008) - 외인/기관 수급

#### 3.5.1 응답 데이터

```kotlin
data class EtfDailyTrade(
    val date: String,                // 일자
    val closePrice: Long,            // 현재가
    val priceChange: Long,           // 전일대비
    val priceChangeSign: String,     // 대비기호
    val volume: Long,                // 거래량
    val accVolume: Long,             // 누적거래량
    val foreignNetBuy: Long,         // 외인 순매수 수량
    val institutionNetBuy: Long      // 기관 순매수 수량
)
```

---

## 4. 키워드 필터링

### 4.1 액티브 ETF 필터링

키움 API에서 액티브 ETF를 직접 구분하는 필드가 없으므로, 종목명 키워드 기반 필터링을 구현합니다.

#### 4.1.1 필터링 모델

```kotlin
data class EtfKeywordFilter(
    val includeKeywords: List<String> = listOf("액티브", "Active"),
    val excludeKeywords: List<String> = emptyList(),
    val matchMode: KeywordMatchMode = KeywordMatchMode.ANY_INCLUDE
)

enum class KeywordMatchMode {
    ANY_INCLUDE,   // 포함 키워드 중 하나라도 일치
    ALL_INCLUDE,   // 포함 키워드 모두 일치
    CUSTOM         // 사용자 정의
}
```

#### 4.1.2 필터링 로직

```kotlin
fun matchesKeywordFilter(name: String, filter: EtfKeywordFilter): Boolean {
    // 1. 제외 키워드 체크 (먼저)
    if (filter.excludeKeywords.any { name.contains(it, ignoreCase = true) }) {
        return false
    }

    // 2. 포함 키워드 체크
    return when (filter.matchMode) {
        KeywordMatchMode.ANY_INCLUDE ->
            filter.includeKeywords.any { name.contains(it, ignoreCase = true) }
        KeywordMatchMode.ALL_INCLUDE ->
            filter.includeKeywords.all { name.contains(it, ignoreCase = true) }
        KeywordMatchMode.CUSTOM -> true
    }
}
```

### 4.2 기본 제공 필터

| 필터명 | 포함 키워드 | 제외 키워드 |
|--------|------------|------------|
| 액티브 ETF | ["액티브", "Active"] | [] |
| 레버리지 제외 | [] | ["레버리지", "2X", "3X"] |
| 인버스 제외 | [] | ["인버스", "inverse"] |
| AI/반도체 | ["AI", "반도체", "테크"] | [] |

---

## 5. 추가 필터 옵션

### 5.1 추적지수 필터

```kotlin
enum class EtfTrackingIndex(val code: String, val displayName: String) {
    ALL("0", "전체"),
    KOSPI200("1", "KOSPI200"),
    KOSDAQ150("2", "KOSDAQ150"),
    SECTOR("3", "섹터"),
    THEME("4", "테마"),
    BOND("5", "채권"),
    COMMODITY("6", "원자재"),
    OVERSEAS("7", "해외"),
    ACTIVE("8", "액티브")
}
```

### 5.2 거래소 필터

```kotlin
enum class EtfExchangeType(val code: String, val displayName: String) {
    KRX("1", "KRX"),
    NXT("2", "NXT"),
    KRX_MOCK("3", "KRX (모의)")
}
```

**투자 모드별 사용 가능 거래소:**
- MOCK (모의투자): `KRX_MOCK` 만 사용 가능
- PRODUCTION (실전투자): `KRX`, `NXT` 사용 가능

### 5.3 NAV 대비 필터

```kotlin
enum class NavComparison(val code: String, val displayName: String) {
    ALL("0", "전체"),
    PREMIUM("1", "프리미엄"),   // NAV > 현재가
    DISCOUNT("2", "디스카운트")  // NAV < 현재가
}
```

---

## 6. API 요청 제한 관리

### 6.1 기존 Rate Limiter 활용

StockApp의 `KiwoomApiClient`에 이미 500ms Rate Limit가 구현되어 있습니다.

```kotlin
// KiwoomApiClient.kt (기존 코드)
private val rateLimitMutex = Mutex()
private var lastRequestTime: Long = 0
private val minRequestInterval = 500L  // 500ms = 초당 2건

suspend fun <T> call(...): Result<T> {
    rateLimitMutex.withLock {
        val elapsed = System.currentTimeMillis() - lastRequestTime
        if (elapsed < minRequestInterval) {
            delay(minRequestInterval - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }
    // API 호출...
}
```

### 6.2 추가 고려사항

| 환경 | 키움 API 제한 | 현재 설정 | 비고 |
|------|--------------|----------|------|
| 실전투자 | 초당 5건 | 초당 2건 | 안전 마진 |
| 모의투자 | 초당 5건 | 초당 2건 | 안전 마진 |

---

## 7. 데이터 캐싱

### 7.1 Room Entity 정의

```kotlin
/**
 * ETF 목록 캐시 엔티티
 */
@Entity(
    tableName = "etf_cache",
    indices = [
        Index(value = ["name"]),
        Index(value = ["trackingIndexCode"])
    ]
)
data class EtfCacheEntity(
    @PrimaryKey
    val ticker: String,
    val name: String,
    val classification: String,
    val closePrice: Long,
    val priceChange: Long,
    val priceChangeSign: String,
    val changeRate: Double,
    val volume: Long,
    val nav: Double,
    val trackingError: Double,
    val trackingIndexName: String,
    val trackingIndexCode: String,
    val leverage: Double,
    val managementCompany: String,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * ETF 상세 캐시 엔티티
 */
@Entity(tableName = "etf_detail_cache")
data class EtfDetailCacheEntity(
    @PrimaryKey
    val ticker: String,
    val data: String,  // JSON 직렬화된 EtfDetail
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * ETF 키워드 필터 설정 엔티티
 */
@Entity(tableName = "etf_keyword_filter")
data class EtfKeywordFilterEntity(
    @PrimaryKey
    val id: Int = 1,  // 싱글톤
    val includeKeywords: String,  // JSON 배열
    val excludeKeywords: String,  // JSON 배열
    val matchMode: String,
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 7.2 캐시 정책

| 데이터 타입 | TTL | 갱신 조건 |
|------------|-----|----------|
| ETF 목록 | 1시간 | 수동 새로고침 또는 앱 시작 |
| ETF 상세 | 24시간 | 상세 화면 진입 시 |
| 일별 데이터 | 24시간 | 요청 시 |
| 키워드 필터 | 영구 | 사용자 설정 변경 시 |

### 7.3 DAO 인터페이스

```kotlin
@Dao
interface EtfCacheDao {
    // ETF 목록
    @Query("SELECT * FROM etf_cache ORDER BY volume DESC")
    suspend fun getAllEtfs(): List<EtfCacheEntity>

    @Query("SELECT * FROM etf_cache WHERE name LIKE '%' || :keyword || '%'")
    suspend fun searchEtfsByName(keyword: String): List<EtfCacheEntity>

    @Query("SELECT * FROM etf_cache WHERE ticker = :ticker")
    suspend fun getEtfByTicker(ticker: String): EtfCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(etfs: List<EtfCacheEntity>)

    @Query("DELETE FROM etf_cache")
    suspend fun deleteAll()

    @Query("DELETE FROM etf_cache WHERE cachedAt < :threshold")
    suspend fun deleteExpired(threshold: Long)

    // ETF 상세
    @Query("SELECT * FROM etf_detail_cache WHERE ticker = :ticker")
    suspend fun getEtfDetail(ticker: String): EtfDetailCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: EtfDetailCacheEntity)

    // 키워드 필터
    @Query("SELECT * FROM etf_keyword_filter WHERE id = 1")
    fun getKeywordFilter(): Flow<EtfKeywordFilterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveKeywordFilter(filter: EtfKeywordFilterEntity)
}
```

---

## 8. UI 명세

### 8.1 화면 구성

#### 8.1.1 ETF 목록 화면 (EtfScreen)

```
┌────────────────────────────────────────┐
│  ETF                        🔄 🌙     │  ← TopAppBar
├────────────────────────────────────────┤
│  ☑ 액티브 ETF만 보기                   │  ← 액티브 필터 토글
├────────────────────────────────────────┤
│  전체 │ KOSPI200 │ 섹터 │ 테마 │ 해외  │  ← 추적지수 탭
├────────────────────────────────────────┤
│  검색결과: 25개 (전체 450개)           │  ← 결과 헤더
├────────────────────────────────────────┤
│  ┌──────────────────────────────────┐ │
│  │ KODEX 200 액티브            ▲    │ │  ← ETF 아이템
│  │ 069500 | KOSPI200                │ │
│  │ 35,250원     +1.25%      NAV     │ │
│  │                         35,300   │ │
│  └──────────────────────────────────┘ │
│  ───────────────────────────────────  │
│  ┌──────────────────────────────────┐ │
│  │ TIGER AI 반도체 액티브      ▲    │ │
│  │ 123456 | 섹터                    │ │
│  │ ...                              │ │
│  └──────────────────────────────────┘ │
└────────────────────────────────────────┘
```

#### 8.1.2 ETF 상세 바텀시트

```
┌────────────────────────────────────────┐
│  KODEX 200 액티브                      │
│  069500                                │
├────────────────────────────────────────┤
│  기본 정보                             │
│  ┌────────────────────────────────┐   │
│  │ 추적지수: KOSPI200             │   │
│  │ 운용사: 삼성자산운용           │   │
│  │ 과세유형: 비과세               │   │
│  └────────────────────────────────┘   │
├────────────────────────────────────────┤
│  NAV 분석                              │
│  ┌────────────────────────────────┐   │
│  │ 현재 NAV: 35,300               │   │
│  │ 괴리율: -0.14%                 │   │
│  │ 추적오차율: 0.02%              │   │
│  └────────────────────────────────┘   │
├────────────────────────────────────────┤
│  외인/기관 수급 (최근 5일)             │
│  ┌────────────────────────────────┐   │
│  │ 외인: +12,500 주               │   │
│  │ 기관: +8,300 주                │   │
│  └────────────────────────────────┘   │
├────────────────────────────────────────┤
│  [ 수급 분석 화면으로 이동 ]           │
└────────────────────────────────────────┘
```

### 8.2 상태 관리

```kotlin
sealed class EtfState {
    data object Loading : EtfState()
    data object NoApiKey : EtfState()
    data class Success(val result: EtfListResult) : EtfState()
    data class Error(val message: String) : EtfState()
}

sealed class EtfDetailState {
    data object Idle : EtfDetailState()
    data object Loading : EtfDetailState()
    data class Success(val result: EtfDetailResult) : EtfDetailState()
    data class Error(val message: String) : EtfDetailState()
}
```

### 8.3 사용자 인터랙션

| 액션 | 동작 |
|------|------|
| Pull-to-Refresh | ETF 목록 새로고침 |
| 액티브 토글 ON | 액티브 키워드 필터 적용 |
| 추적지수 탭 클릭 | 해당 추적지수로 필터링 |
| ETF 아이템 클릭 | 상세 바텀시트 표시 |
| "수급 분석으로 이동" | Analysis 화면으로 이동 |

---

## 9. 네비게이션 통합

### 9.1 방안 검토

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A | ETF 탭 추가 (6탭) | 독립적 접근 | 탭 과다 |
| B | Ranking 탭 대체 | 기존 구조 유지 | Ranking 제거 |
| C | Ranking 내 서브탭 | Ranking 확장 | 복잡도 증가 |
| D | Settings에서 접근 | 기존 구조 유지 | 접근성 저하 |

**권장: 방안 C** - Ranking 화면에 ETF 탭 추가

### 9.2 수정된 네비게이션

```kotlin
// 기존 5탭 유지
val bottomNavItems = listOf(Search, Analysis, Indicator, Ranking, Settings)

// RankingScreen 내부 탭 구조
enum class RankingTab {
    RANKING,  // 기존 순위정보
    ETF       // 새 ETF 기능
}
```

### 9.3 대안: 독립 ETF 탭

```kotlin
// Nav.kt에 추가
sealed class Screen {
    // 기존...
    data object Etf : Screen("etf", "ETF", Icons.Default.TrendingUp)
}

// 6탭 구성 (권장하지 않음)
val bottomNavItems = listOf(Search, Analysis, Indicator, Etf, Ranking, Settings)
```

---

## 10. 구성종목 대안 기능

키움 API에서 ETF 구성종목 정보를 제공하지 않으므로, 대안 기능을 제공합니다.

### 10.1 제공 기능

| 기능 | 설명 | API |
|------|------|-----|
| 외인/기관 수급 | 누가 사고 파는지 | ka40008 |
| NAV 괴리율 분석 | 프리미엄/디스카운트 | ka40003 |
| 추적오차 분석 | ETF 품질 평가 | ka40003 |
| ETF 간 비교 | 동일 지수 추적 ETF 비교 | ka40004 |

### 10.2 향후 확장 가능성

| 방안 | 설명 | 난이도 |
|------|------|--------|
| 웹 스크래핑 | 운용사 웹사이트에서 수집 | 높음 |
| 외부 API | KRX, 증권사 API 연동 | 중간 |
| KIS API 추가 | 한국투자증권 API 병행 | 높음 |

---

## 11. 구현 순서

### Phase 1: Domain Layer (1일)
- [ ] `EtfModels.kt` - 도메인 모델 정의
- [ ] `EtfParams.kt` - API 파라미터 정의
- [ ] `EtfRepo.kt` - Repository 인터페이스
- [ ] `GetEtfListUC.kt`, `GetEtfDetailUC.kt` - Use Cases

### Phase 2: Data Layer (1-2일)
- [ ] `EtfDto.kt` - API 응답 DTO
- [ ] `EtfParseUtils.kt` - 파싱 유틸
- [ ] `EtfRepoImpl.kt` - Repository 구현

### Phase 3: Database (1일)
- [ ] `EtfCacheEntity.kt` - Room Entity
- [ ] `EtfCacheDao.kt` - DAO
- [ ] `AppDb.kt` 업데이트 (버전 증가)

### Phase 4: DI & UI (2일)
- [ ] `EtfModule.kt` - Hilt DI
- [ ] `EtfVm.kt` - ViewModel
- [ ] `EtfScreen.kt` - 메인 화면
- [ ] `EtfDetailSheet.kt` - 상세 바텀시트

### Phase 5: Navigation & Testing (1일)
- [ ] `Nav.kt`, `NavGraph.kt` 업데이트
- [ ] API 테스트 (모의/실전)
- [ ] 키워드 필터 테스트
- [ ] UI 폴리싱

**총 예상: 5-6일**

---

## 12. 체크리스트

### 구현 체크리스트
- [ ] Domain 모델 정의
- [ ] Repository 인터페이스 정의
- [ ] Use Case 구현
- [ ] DTO 정의
- [ ] Repository 구현 (API 호출)
- [ ] Room Entity 정의
- [ ] DAO 정의
- [ ] AppDb 버전 업데이트
- [ ] Hilt Module 정의
- [ ] ViewModel 구현
- [ ] Screen 구현
- [ ] Navigation 통합

### 테스트 체크리스트
- [ ] API 호출 테스트 (ka40004, ka40002, ka40003, ka40008)
- [ ] 키워드 필터링 테스트
- [ ] 캐싱 동작 테스트
- [ ] 투자 모드별 거래소 필터 테스트
- [ ] 에러 처리 테스트
- [ ] UI 상태 전환 테스트

---

## 13. 참조 코드

### 13.1 Ranking 기능 참조 경로

```
StockApp/app/src/main/java/com/stockapp/feature/ranking/
├── domain/
│   ├── model/RankingModels.kt     ← 모델 패턴 참조
│   ├── repo/RankingRepo.kt        ← 인터페이스 패턴 참조
│   └── usecase/GetRankingUC.kt    ← UseCase 패턴 참조
├── data/
│   ├── dto/RankingDto.kt          ← DTO 패턴 참조
│   └── repo/RankingRepoImpl.kt    ← API 호출 패턴 참조
├── ui/
│   ├── RankingVm.kt               ← ViewModel 패턴 참조
│   └── RankingScreen.kt           ← Screen 패턴 참조
└── di/
    └── RankingModule.kt           ← DI 패턴 참조
```

### 13.2 KiwoomApiClient 사용 예시

```kotlin
// EtfRepoImpl.kt
suspend fun getEtfList(params: EtfListParams): Result<EtfListResult> {
    val config = getApiConfig()

    return apiClient.call(
        apiId = "ka40004",
        url = "/api/dostk/etf",
        body = params.toRequestBody(),
        appKey = config.appKey,
        secretKey = config.secretKey,
        baseUrl = config.baseUrl
    ) { responseJson ->
        val items = findAndParseEtfItemsArray(responseJson)
        EtfListResult(items = items.map { it.toDomainModel() })
    }
}
```

---

## 부록 A: API 응답 예시

### A.1 ka40004 응답 (ETF 전체 시세)

```json
{
  "return_code": 0,
  "return_msg": "success",
  "etf_list": [
    {
      "stk_cd": "069500",
      "stk_nm": "KODEX 200",
      "stk_cls": "주식",
      "close_pric": "35250",
      "pre_sig": "2",
      "pred_pre": "500",
      "pre_rt": "1.44",
      "trde_qty": "5234567",
      "nav": "35300.50",
      "trace_eor_rt": "0.02",
      "trace_idex_nm": "KOSPI200",
      "trace_idex_cd": "001",
      "drng": "1.0"
    }
  ]
}
```

### A.2 ka40003 응답 (ETF 일별 추이)

```json
{
  "return_code": 0,
  "daily_data": [
    {
      "cntr_dt": "20260124",
      "cur_prc": "35250",
      "pre_sig": "2",
      "pred_pre": "500",
      "pre_rt": "1.44",
      "trde_qty": "5234567",
      "nav": "35300.50",
      "acc_trde_prica": "184532000000",
      "navidex_dispty_rt": "-0.14",
      "navetfdispty_rt": "-0.14",
      "trace_eor_rt": "0.02"
    }
  ]
}
```

### A.3 ka40008 응답 (외인/기관 수급)

```json
{
  "return_code": 0,
  "trade_data": [
    {
      "dt": "20260124",
      "cur_prc": "35250",
      "pre_sig": "2",
      "pred_pre": "500",
      "trde_qty": "1234567",
      "acc_trde_qty": "5234567",
      "for_netprps_qty": "12500",
      "orgn_netprps_qty": "8300"
    }
  ]
}
```

---

## 부록 B: 원본 명세서와의 차이점

| 항목 | 원본 명세서 | 최적화 버전 |
|------|------------|------------|
| API | KIS API | 키움증권 API |
| 구성종목 | 지원 (FHKST121600C0) | **미지원** (대안 기능 제공) |
| Rate Limit | 별도 구현 | 기존 KiwoomApiClient 활용 |
| 데이터 저장 | CSV/JSON/SQLite/PostgreSQL | Room DB (캐싱) |
| Python | 직접 사용 | **사용 안 함** (Kotlin 직접 호출) |
| 필터링 | Python dataclass | Kotlin enum/data class |
| UI | CLI | Android Jetpack Compose |

---

**문서 버전**: 1.0
**작성일**: 2026-01-24
**작성자**: Claude Code Agent
**기반 문서**: 액티브 ETF 정보 수집 프로그램 명세서 (KIS API 버전)
