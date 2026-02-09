# 데이터 수집 최적화 (SQL DB 기반)

## 개요

StockApp의 데이터 수집 로직을 정규화된 SQL DB 캐시 기반으로 전면 최적화했습니다.
기존에는 기능(Analysis, Indicator, Market)마다 동일 종목에 대해 독립적으로 API를 호출하여 세션당 3~5회 중복 호출이 발생했으나, 정규화된 원시 데이터 캐시 테이블을 도입하여 API 호출을 60~70% 감소시켰습니다.

### 변경 전 문제점

| 문제 | 영향 |
|------|------|
| OHLCV 데이터를 기능별 독립 호출 | 동일 종목 세션당 3~5회 중복 API 호출 |
| 기능 캐시가 계산 결과(JSON blob)만 저장 | 원시 데이터 공유 불가 |
| 종목리스트 전체 삭제 + 재삽입 동기화 | 동기화 중 데이터 유실 가능 |
| 시장 지표 매번 KRX 직접 호출 | Market 화면 진입 시 매번 대기 |
| DB 크기 관리 없음 | 캐시 무한 증가 |

### 변경 후 개선

| 항목 | 개선 효과 |
|------|----------|
| OHLCV API 호출 | 종목당 3~5회 → 0~1회 (캐시 유효 시 0회) |
| 투자자 매매 API 호출 | ~50% 감소 (Analysis + Market 공유) |
| 종목리스트 동기화 | 원자적 upsert (데이터 유실 없음) |
| Market 화면 로딩 | 4시간 캐시에서 즉시 로딩 |
| DB 크기 | 자동 정리 (OHLCV 365일, 투자자 매매 180일) |

---

## 아키텍처

### 데이터 흐름 (변경 후)

```
┌─ Analysis ─┐
│             │──→ OhlcvService ──→ [ohlcv_cache DB] ──→ KRX API
├─ Indicator ─┤       (공유)        (정규화 캐시)      (증분 페치)
│             │
├─ Market ───┤──→ InvestorTradingService ──→ [investor_trading_cache DB] ──→ KRX API
│             │           (공유)                   (정규화 캐시)
└─────────────┘

┌─ Market 화면 ──→ MarketRepoImpl ──→ [market_indicator_cache DB] ──→ KRX API
│                   (계산 결과 캐시)      (JSON blob 캐시, 4h TTL)
└─────────────────

┌─ Background Sync ──→ syncStockList() ──→ smartSync() (diff 기반)
│                  ──→ syncTopStocksAnalysis() (우선순위 기반)
│                  ──→ syncMarketData() (시장 지표 사전 수집)
│                  ──→ DbCleanupManager (자동 정리)
└─────────────────────
```

### 캐시 계층 구조

```
Layer 1: 정규화 원시 데이터 캐시 (NEW)
  ├── ohlcv_cache          ← OHLCV 일봉 (ticker + date PK)
  └── investor_trading_cache ← 투자자 매매 (ticker + date PK)

Layer 2: 기능별 계산 결과 캐시 (기존 유지)
  ├── analysis_cache       ← 수급 분석 결과 (JSON blob)
  ├── indicator_cache      ← 기술 지표 결과 (JSON blob)
  └── market_indicator_cache ← 시장 지표 결과 (JSON blob, 4h TTL)

Layer 3: 마스터 데이터
  └── stocks               ← 종목 정보 (smartSync로 갱신)
```

---

## Phase 1: OHLCV 정규화 캐시 테이블

### 새 테이블: `ohlcv_cache`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `ticker` | TEXT (PK) | 종목 코드 (예: "005930") |
| `date` | TEXT (PK) | 날짜 (yyyyMMdd) |
| `open` | INTEGER | 시가 |
| `high` | INTEGER | 고가 |
| `low` | INTEGER | 저가 |
| `close` | INTEGER | 종가 |
| `volume` | LONG | 거래량 |
| `cachedAt` | LONG | 캐시 시점 (ms) |

인덱스: `ticker`, `cachedAt`

### 핵심 파일

| 파일 | 역할 |
|------|------|
| `core/db/entity/OhlcvCacheEntity.kt` | Room 엔티티 |
| `core/db/dao/OhlcvCacheDao.kt` | DAO (범위 조회, 증분 페치용 메서드) |
| `core/stock/data/OhlcvService.kt` | DB 캐시 우선 + 증분 페치 서비스 |

### OhlcvService 동작 방식

```
getOhlcv(ticker, days, period)
  │
  ├─ Weekly/Monthly → Daily 캐시에서 조회 → resampleToWeekly/Monthly()
  │
  └─ Daily → getOhlcvWithCache(ticker, days)
               │
               ├─ Step 1: DB 캐시 조회 (date 범위)
               │   └─ cachedCount >= expectedTradingDays(days × 0.65)?
               │       ├─ YES → 즉시 반환
               │       └─ NO → Step 2
               │
               ├─ Step 2: Mutex 락 (ticker별, 동시 요청 병합)
               │   └─ 재확인 후 여전히 부족하면 Step 3
               │
               ├─ Step 3: 증분 페치 범위 결정
               │   ├─ latestCachedDate 존재? → latestDate+1 ~ 오늘
               │   └─ 캐시 없음? → startDate ~ 오늘 (전체 페치)
               │
               ├─ Step 4: KRX API 호출 (페치 범위만)
               │   └─ 실패 시 Kiwoom API 폴백
               │
               ├─ Step 5: 새 데이터 DB 저장
               │
               └─ Step 6: DB에서 전체 범위 재조회 → 반환
```

### 캐시 충분성 판단

거래일 기준으로 캐시 충분성을 판단합니다 (주말/공휴일 제외):

```
expectedTradingDays = requestedDays × OHLCV_CACHE_SUFFICIENCY_RATIO(0.65)
cacheSufficient = cachedCount >= expectedTradingDays
```

예: 180일 요청 시 → 117개 이상 거래일 데이터가 있으면 캐시 충분

### 동시 요청 병합 (Mutex)

동일 종목에 대해 여러 기능이 동시에 OHLCV를 요청할 때 API를 한 번만 호출합니다:

```kotlin
// Per-ticker mutex prevents concurrent duplicate API calls
private val tickerMutexes = ConcurrentHashMap<String, Mutex>()

getMutex(ticker).withLock {
    // Re-check cache (another coroutine may have populated it)
    // Only one coroutine fetches from API; others wait and use cache
}
```

---

## Phase 2: 투자자 매매 정규화 캐시 테이블

### 새 테이블: `investor_trading_cache`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `ticker` | TEXT (PK) | 종목코드 또는 "MARKET_KOSPI" / "MARKET_KOSDAQ" / "MARKET_ALL" |
| `date` | TEXT (PK) | 날짜 (yyyyMMdd) |
| `foreignNet` | LONG | 외국인 순매수 (원) |
| `institutionNet` | LONG | 기관 순매수 (원) |
| `individualNet` | LONG | 개인 순매수 (원) |
| `totalTrading` | LONG | 총 거래대금 (원) |
| `cachedAt` | LONG | 캐시 시점 (ms) |

인덱스: `ticker`, `cachedAt`

### 핵심 파일

| 파일 | 역할 |
|------|------|
| `core/db/entity/InvestorTradingCacheEntity.kt` | Room 엔티티 |
| `core/db/dao/InvestorTradingCacheDao.kt` | DAO |
| `core/stock/data/InvestorTradingService.kt` | 공유 서비스 (DB 캐시 + 증분 페치) |

### InvestorTradingService 인터페이스

```kotlin
// 개별 종목용
suspend fun getInvestorTrading(ticker: String, days: Int): Result<List<InvestorTradingData>>

// 시장 전체용 (ticker = "MARKET_ALL" / "MARKET_KOSPI" / "MARKET_KOSDAQ")
suspend fun getMarketInvestorTrading(days: Int, market: String): Result<List<InvestorTradingData>>
```

동작 방식은 OhlcvService와 동일한 패턴:
- DB 캐시 우선 조회 → 충분하면 즉시 반환
- 부족하면 Mutex 락 → 증분 페치 → DB 저장 → 전체 범위 재조회

### 소비자 연동

| 소비자 | 변경 전 | 변경 후 |
|--------|---------|---------|
| `NativeAnalysisRepoImpl` | `krxDataSource.getTradingByInvestor()` 직접 호출 | `investorTradingService.getInvestorTrading()` |
| `MarketRepoImpl` (Fear/Greed) | `krxDataSource.getMarketTradingByInvestor()` 직접 호출 | `investorTradingService.getMarketInvestorTrading()` |
| `MarketRepoImpl` (Fund Flow) | `krxDataSource.getMarketTradingByInvestor()` 직접 호출 | `investorTradingService.getMarketInvestorTrading()` |

---

## Phase 3: 스마트 종목리스트 동기화

### 변경 전

```kotlin
// 매번 10K 종목 전체 삭제 + 재삽입 (동기화 중 데이터 유실 가능)
stockDao.deleteAll()
stockDao.insertAll(stocks)
```

### 변경 후

```kotlin
// Diff 기반 원자적 동기화
@Transaction
suspend fun smartSync(stocks: List<StockEntity>) {
    insertAll(stocks)  // OnConflictStrategy.REPLACE = upsert
    val activeTickers = stocks.map { it.ticker }.toSet()
    val allTickers = getAllTickers()
    val inactive = allTickers.filter { it !in activeTickers }
    inactive.chunked(500).forEach { batch -> deleteByTickers(batch) }
}
```

### 적용 위치

| 파일 | 변경 내용 |
|------|----------|
| `core/db/dao/StockDao.kt` | `smartSync()`, `getAllTickers()`, `deleteByTickers()` 추가 |
| `core/cache/StockCacheManager.kt` | `replaceAll()` → `smartSync()` |
| `feature/scheduling/data/repo/SchedulingRepoImpl.kt` | `deleteAll()+insertAll()` → `smartSync()` |

### 우선순위 기반 백그라운드 분석

변경 전: 알파벳 순 첫 100개 종목 분석
변경 후: 사용자가 실제 관심 있는 종목 우선 분석

```
Priority 1: 최근 검색 종목 (SearchHistoryDao, 최대 30개)
Priority 2: 기존 분석 데이터 보유 종목 (최신 50개)
→ 합산 중복제거 후 최대 100개
→ 둘 다 없으면 기존 방식 폴백 (첫 100개)
```

---

## Phase 4: 시장 지표 캐시 + 사전 수집

### MarketRepoImpl 캐시 연동

`MarketIndicatorCacheDao`를 활용하여 계산된 시장 지표 결과를 캐시합니다 (4시간 TTL).

| 메서드 | 캐시 키 | 설명 |
|--------|---------|------|
| `getFearGreedIndex()` | `fear_greed_latest` | 최신 공포/탐욕 지수 |
| `getFearGreedHistory()` | `fear_greed_history_{days}d` | 공포/탐욕 이력 |
| `getOscillatorHistory()` | `oscillator_{days}d` | 과매수/과매도 (가장 비용 높음) |
| `getFundFlowHistory()` | `fund_flow_{days}d` | 자금 동향 |

캐시 동작:
```
요청 → MarketIndicatorCacheDao.getIfFresh(key, now - 4h)
  ├─ 캐시 유효 → JSON 역직렬화 → 즉시 반환
  └─ 캐시 없음/만료 → KRX 수집 → 계산 → JSON 직렬화 → 캐시 저장 → 반환
```

### 도메인 모델 직렬화

캐시 저장/복원을 위해 다음 모델에 `@Serializable` 추가:

- `MarketFearGreed`, `IndicatorComponent`, `FearGreedSignal`
- `FearGreedHistory`
- `OscillatorHistory`, `OscillatorSignal`
- `FundFlowHistory`

파일: `feature/market/domain/model/MarketModels.kt`

### 백그라운드 사전 수집

`SchedulingRepoImpl.syncAllData()`에 시장 데이터 사전 수집 단계 추가:

```
syncAllData()
  1. syncStockList()          ← 종목리스트 (smartSync)
  2. syncTopStocksAnalysis()  ← 수급 분석 (우선순위 기반)
  3. syncEtfData()            ← ETF 데이터
  4. syncMarketData()         ← 시장 지표 사전 수집 (NEW)
  5. dbCleanupManager.runCleanup() ← DB 정리
```

`syncMarketData()` 수집 항목:
- Fear & Greed 최신값
- Fear & Greed 이력 (90일)
- Fund Flow 이력 (90일)
- Oscillator는 비용이 높아 생략 (20일 × 300ms/일 = 6초+)

---

## Phase 5: DB 크기 자동 관리

### DbCleanupManager

```
core/db/cleanup/DbCleanupManager.kt

runCleanup()
  ├── cleanupOhlcvCache()
  │   └── ohlcv_cache에서 365일 초과 데이터 삭제 (종목별)
  │
  ├── cleanupInvestorTradingCache()
  │   └── investor_trading_cache에서 180일 초과 데이터 삭제
  │
  └── cleanupFeatureCaches()
      ├── analysis_cache에서 TTL(24h) 초과 삭제
      └── indicator_cache에서 TTL(24h) 초과 삭제
```

### 호출 시점

백그라운드 동기화(`syncAllData()`) 마지막 단계에서 자동 실행.

### DB 크기 예측

| 데이터 | 예상 크기/년 | 보존 기간 |
|--------|------------|----------|
| OHLCV (50종목 × 250거래일 × 40B) | ~500KB | 365일 |
| 투자자 매매 (50종목 × 250일 × 48B) | ~600KB | 180일 |
| 합계 | ~1.1MB/년 | - |

---

## DB Migration

### v12 → v13 (`MIGRATION_12_13`)

```sql
-- OHLCV 정규화 캐시 테이블
CREATE TABLE IF NOT EXISTS ohlcv_cache (
    ticker TEXT NOT NULL,
    date TEXT NOT NULL,
    open INTEGER NOT NULL,
    high INTEGER NOT NULL,
    low INTEGER NOT NULL,
    close INTEGER NOT NULL,
    volume INTEGER NOT NULL,
    cachedAt INTEGER NOT NULL,
    PRIMARY KEY (ticker, date)
);
CREATE INDEX IF NOT EXISTS index_ohlcv_cache_ticker ON ohlcv_cache (ticker);
CREATE INDEX IF NOT EXISTS index_ohlcv_cache_cachedAt ON ohlcv_cache (cachedAt);

-- 투자자 매매 정규화 캐시 테이블
CREATE TABLE IF NOT EXISTS investor_trading_cache (
    ticker TEXT NOT NULL,
    date TEXT NOT NULL,
    foreignNet INTEGER NOT NULL,
    institutionNet INTEGER NOT NULL,
    individualNet INTEGER NOT NULL,
    totalTrading INTEGER NOT NULL,
    cachedAt INTEGER NOT NULL,
    PRIMARY KEY (ticker, date)
);
CREATE INDEX IF NOT EXISTS index_investor_trading_cache_ticker ON investor_trading_cache (ticker);
CREATE INDEX IF NOT EXISTS index_investor_trading_cache_cachedAt ON investor_trading_cache (cachedAt);
```

---

## 캐시 TTL 설정

| 상수 | 값 | 용도 |
|------|-----|------|
| `OHLCV_CACHE_TTL_MS` | 24시간 | OHLCV 원시 데이터 유효 기간 |
| `OHLCV_MAX_RETENTION_DAYS` | 365일 | OHLCV 최대 보존 기간 |
| `OHLCV_CACHE_SUFFICIENCY_RATIO` | 0.65 | 캐시 충분성 비율 (주말/공휴일 고려) |
| `INVESTOR_TRADING_MAX_RETENTION_DAYS` | 180일 | 투자자 매매 최대 보존 기간 |
| `MARKET_CACHE_TTL_MS` | 4시간 | 시장 지표 계산 결과 유효 기간 |
| `ANALYSIS_CACHE_TTL_MS` | 24시간 | 수급 분석 결과 유효 기간 |
| `INDICATOR_CACHE_TTL_MS` | 24시간 | 기술 지표 결과 유효 기간 |

파일: `core/config/AppConfig.kt`

---

## 수정 파일 전체 목록

### 새 파일 (6개)

| 파일 | 용도 |
|------|------|
| `core/db/entity/OhlcvCacheEntity.kt` | OHLCV 정규화 캐시 엔티티 |
| `core/db/dao/OhlcvCacheDao.kt` | OHLCV 캐시 DAO |
| `core/db/entity/InvestorTradingCacheEntity.kt` | 투자자 매매 캐시 엔티티 |
| `core/db/dao/InvestorTradingCacheDao.kt` | 투자자 매매 캐시 DAO |
| `core/stock/data/InvestorTradingService.kt` | 공유 투자자 매매 서비스 |
| `core/db/cleanup/DbCleanupManager.kt` | DB 크기 자동 관리 |

### 수정 파일 (9개)

| 파일 | 변경 내용 |
|------|----------|
| `core/db/AppDb.kt` | 엔티티 추가, v13, `MIGRATION_12_13` |
| `core/di/DbModule.kt` | 새 DAO provider, 마이그레이션 등록 |
| `core/config/AppConfig.kt` | OHLCV/Investor TTL 상수 추가 |
| `core/stock/data/OhlcvService.kt` | DB 캐시 우선 + 증분 페치 + Mutex |
| `core/db/dao/StockDao.kt` | `smartSync()` 메서드 추가 |
| `core/cache/StockCacheManager.kt` | `replaceAll()` → `smartSync()` |
| `feature/scheduling/data/repo/SchedulingRepoImpl.kt` | 스마트 동기화, 우선순위 분석, 시장 사전 수집, DB 정리 |
| `feature/analysis/data/repo/NativeAnalysisRepoImpl.kt` | `InvestorTradingService` 연동 |
| `feature/market/data/repo/MarketRepoImpl.kt` | `InvestorTradingService` + 캐시 DAO 연동 |
| `feature/market/domain/model/MarketModels.kt` | `@Serializable` 추가 (캐시 직렬화용) |

---

## 검증 방법

1. **OHLCV 캐시**: 같은 종목으로 Analysis → Indicator(Trend) → Indicator(Elder) 진입. Logcat에서 `OhlcvService` 태그 확인 - 첫 번째만 "full fetch", 나머지는 "cache hit"
2. **투자자 매매 공유**: Analysis 진입 후 Market Fund Flow 진입. `InvestorTradingService` 태그에서 "Cache hit" 확인
3. **스마트 동기화**: 백그라운드 동기화 전후 종목 수 변화 없음 확인
4. **시장 캐시**: Market 화면 진입 → 나갔다 재진입 시 즉시 로딩 (4시간 이내)
5. **사전 수집**: 앱 시작 → 동기화 완료 → Market 화면 첫 진입 시 Logcat에서 "cache hit" 확인
6. **DB 정리**: `DbCleanupManager` 태그에서 cleanup 완료 로그 확인
7. **빌드**: `./gradlew assembleDebug` 성공
