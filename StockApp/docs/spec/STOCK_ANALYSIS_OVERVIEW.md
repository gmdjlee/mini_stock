# Stock Analysis Feature - Porting Specification Overview

**Document ID**: STOCK_ANALYSIS_OVERVIEW
**Version**: 1.0
**Date**: 2026-02-23
**Status**: Final - Developer Handoff
**Audience**: Developers with no access to the original project

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Architecture](#2-architecture)
3. [Shared Infrastructure (Core Modules)](#3-shared-infrastructure-core-modules)
   - 3.1 [SelectedStockManager](#31-selectedstockmanager)
   - 3.2 [AppConfig Constants](#32-appconfig-constants)
   - 3.3 [FeatureFlags](#33-featureflags)
   - 3.4 [Cache Strategy](#34-cache-strategy)
   - 3.5 [TradingDayUtil](#35-tradingdayutil)
   - 3.6 [Data Source Strategy](#36-data-source-strategy)
   - 3.7 [API Clients](#37-api-clients)
   - 3.8 [Database Entities](#38-database-entities)
   - 3.9 [Dependency Injection Structure](#39-dependency-injection-structure)
4. [Container Screen: StockAnalysisScreen](#4-container-screen-stockanalysisscreen)
5. [Document Map](#5-document-map)
6. [Technology Stack Requirements](#6-technology-stack-requirements)
7. [File Manifest](#7-file-manifest)

---

## 1. Feature Overview

### 1.1 Summary

Stock Analysis is a **4-tab container screen** that acts as the primary workspace for stock investigation. The four inner tabs share a single selected stock, enabling a unified workflow: a user finds a stock in the Search tab and the remaining three analysis tabs automatically display data for that stock.

### 1.2 Tab Structure

| Tab Index | Tab Key | Korean Label | Content |
|-----------|---------|--------------|---------|
| 0 | `SEARCH` | 검색 | Stock search with autocomplete and history |
| 1 | `ANALYSIS` | 수급 분석 | Supply/demand analysis with investor trend charts |
| 2 | `INDICATOR` | 기술 지표 | Technical indicators (Trend, Elder Impulse, DeMark TD) |
| 3 | `FINANCIAL` | 재무정보 | Financial data (Profitability, Stability) via KIS API |

### 1.3 Cross-Tab Navigation Flow

When the user taps a stock in the Search tab:

1. `SearchVm` calls `SelectedStockManager.select(stock)` to publish the selection globally.
2. `SearchScreen` invokes the `onStockClick` callback received from `StockAnalysisScreen`.
3. The callback calls `pagerState.animateScrollToPage(StockTab.ANALYSIS.ordinal)` to navigate to tab index 1.
4. `AnalysisScreen`, `IndicatorScreen`, and `FinancialScreen` each hold a reference to `SelectedStockManager` via their respective ViewModels, and collect `selectedTicker` as a `StateFlow` to auto-load data whenever the selection changes.

```
User taps stock in SearchScreen
        |
        v
SearchVm.onStockSelected(stock)
  --> SelectedStockManager.select(stock)   [broadcasts ticker globally]
  --> onStockClick(stock) callback fires
        |
        v
StockAnalysisScreen receives callback
  --> pagerState.animateScrollToPage(index = 1)  [ANALYSIS tab]
        |
        v
AnalysisVm / IndicatorVm / FinancialVm
  --> collect selectedTicker StateFlow
  --> load data for new ticker
```

---

## 2. Architecture

### 2.1 Layered Architecture Diagram

```mermaid
graph TD
    subgraph UI["UI Layer (Compose + ViewModel)"]
        SA[StockAnalysisScreen<br/>HorizontalPager container]
        SS[SearchScreen + SearchVm]
        AS[AnalysisScreen + AnalysisVm]
        IS[IndicatorScreen + IndicatorVm]
        FS[FinancialScreen + FinancialVm]
    end

    subgraph Domain["Domain Layer"]
        SSM[SelectedStockManager<br/>@Singleton]
        UCSearch[SearchStockUC<br/>SaveHistoryUC]
        UCAnalysis[GetAnalysisUC]
        UCIndicator[GetTrendUC<br/>GetElderUC<br/>GetDemarkUC]
        UCFinancial[GetFinancialSummaryUC]
        RepoI[SearchRepo interface<br/>AnalysisRepo interface<br/>IndicatorRepo interface<br/>FinancialRepo interface]
    end

    subgraph Data["Data Layer"]
        Impl[NativeSearchRepoImpl<br/>NativeAnalysisRepoImpl<br/>NativeIndicatorRepoImpl<br/>FinancialRepoImpl]
        KiClient[KiwoomApiClient<br/>@Singleton]
        KisClient[KisApiClient<br/>@Singleton]
        KrxDS[KrxDataSource<br/>@Singleton]
        DB[(Room DB<br/>AppDb v14)]
        Cache[StockCacheManager<br/>@Singleton]
    end

    SA --> SS & AS & IS & FS
    SS --> UCSearch
    AS --> UCAnalysis
    IS --> UCIndicator
    FS --> UCFinancial
    SS & AS & IS & FS --> SSM
    UCSearch --> RepoI
    UCAnalysis --> RepoI
    UCIndicator --> RepoI
    UCFinancial --> RepoI
    RepoI --> Impl
    Impl --> KiClient & KisClient & KrxDS & DB
    Cache --> DB
```

### 2.2 Clean Architecture Principles Applied

| Layer | Dependency Rule | Allowed Imports |
|-------|----------------|-----------------|
| UI | depends on Domain | ViewModel, UseCase, Domain Models |
| Domain | depends on nothing below | Pure Kotlin only, no Android/Room/OkHttp |
| Data | implements Domain interfaces | Room, OkHttp, KRX library, kotlinx-serialization |

### 2.3 State Management Pattern

Every feature ViewModel follows this pattern:

```kotlin
// 1. UI state is a sealed class
sealed class FeatureState {
    data object Idle : FeatureState()
    data object Loading : FeatureState()
    data class Success(val data: DomainModel) : FeatureState()
    data class Error(val code: String, val msg: String) : FeatureState()
}

// 2. ViewModel exposes StateFlow
@HiltViewModel
class FeatureVm @Inject constructor(
    private val useCase: FeatureUseCase,
    private val selectedStockManager: SelectedStockManager
) : ViewModel() {

    private val _state = MutableStateFlow<FeatureState>(FeatureState.Idle)
    val state: StateFlow<FeatureState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            selectedStockManager.selectedTicker.collect { ticker ->
                if (ticker != null) loadData(ticker)
                else _state.value = FeatureState.Idle
            }
        }
    }
}
```

---

## 3. Shared Infrastructure (Core Modules)

All four sub-features depend on these core modules. They must be implemented first.

### 3.1 SelectedStockManager

**Package**: `com.stockapp.core.state`
**File**: `core/state/SelectedStockManager.kt`
**Scope**: `@Singleton` (Hilt)

The single source of truth for which stock is currently selected. Shared across all screens via Hilt injection.

#### 3.1.1 Class Definition

```kotlin
@Singleton
class SelectedStockManager @Inject constructor() {

    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    private val _selectedTicker = MutableStateFlow<String?>(null)
    val selectedTicker: StateFlow<String?> = _selectedTicker.asStateFlow()

    fun select(stock: Stock) {
        _selectedStock.value = stock
        _selectedTicker.value = stock.ticker
    }

    fun selectTicker(ticker: String, name: String? = null) {
        _selectedTicker.value = ticker
        if (name != null) {
            _selectedStock.value = Stock(
                ticker = ticker,
                name = name,
                market = Market.OTHER
            )
        }
    }

    fun clear() {
        _selectedStock.value = null
        _selectedTicker.value = null
    }

    fun hasSelection(): Boolean = _selectedTicker.value != null
}
```

#### 3.1.2 Function Contracts

| Function | Signature | Input | Output | Side Effect |
|----------|-----------|-------|--------|-------------|
| `select` | `fun select(stock: Stock): Unit` | Full `Stock` domain object | None | Updates both `_selectedStock` and `_selectedTicker` atomically |
| `selectTicker` | `fun selectTicker(ticker: String, name: String? = null): Unit` | Ticker string; optional name | None | Updates `_selectedTicker` always; updates `_selectedStock` only if `name != null`, with `Market.OTHER` |
| `clear` | `fun clear(): Unit` | None | None | Sets both flows to `null`; downstream collectors see `Idle` state |
| `hasSelection` | `fun hasSelection(): Boolean` | None | `Boolean` | No side effect; reads `_selectedTicker.value` synchronously |

#### 3.1.3 Edge Cases

- `selectTicker` with `name = null` updates `selectedTicker` but leaves `selectedStock` at its previous value. Do not rely on `selectedStock` being consistent unless `select()` was used.
- `clear()` sets both flows to `null`. Any ViewModel collecting `selectedTicker` must transition to `Idle` state on receiving `null`.
- The singleton is process-scoped. Navigation back to another bottom tab does not auto-clear the selection.

#### 3.1.4 Sample Data

```kotlin
// After: selectedStockManager.select(Stock("005930", "삼성전자", Market.KOSPI))
selectedStock.value == Stock(ticker = "005930", name = "삼성전자", market = Market.KOSPI)
selectedTicker.value == "005930"

// After: selectedStockManager.selectTicker("000660")
selectedTicker.value == "000660"
selectedStock.value == null   // if no prior select() call

// After: selectedStockManager.clear()
selectedStock.value == null
selectedTicker.value == null
```

---

### 3.2 AppConfig Constants

**Package**: `com.stockapp.core.config`
**File**: `core/config/AppConfig.kt`

Centralized `object` holding all numeric constants. Never inline magic numbers in feature code; always reference `AppConfig`.

```kotlin
object AppConfig {

    // Cache sizing
    const val MAX_STOCK_CACHE_SIZE    = 10_000        // Max stocks to retain in DB
    const val MAX_HISTORY_COUNT       = 50            // Max search history entries

    // API timeouts (ms)
    const val DEFAULT_TIMEOUT_MS      = 30_000L
    const val ANALYSIS_TIMEOUT_MS     = 60_000L
    const val STOCK_LIST_TIMEOUT_MS   = 120_000L
    const val SYNC_TIMEOUT_MS         = 120_000L

    // UI behavior (ms)
    const val SEARCH_DEBOUNCE_MS      = 500L          // Debounce delay for search input
    const val RETRY_DELAY_MS          = 500L

    // Search thresholds
    const val MIN_SEARCH_QUERY_LENGTH = 2             // Minimum chars to trigger full search
    const val MAX_SEARCH_RESULTS      = 50

    // Cache TTL (ms) — all 24h except MARKET_CACHE
    const val STOCK_CACHE_TTL_MS      = 86_400_000L   // 24h
    const val ANALYSIS_CACHE_TTL_MS   = 86_400_000L   // 24h
    const val INDICATOR_CACHE_TTL_MS  = 86_400_000L   // 24h
    const val FINANCIAL_CACHE_TTL_MS  = 86_400_000L   // 24h
    const val MARKET_CACHE_TTL_MS     = 14_400_000L   // 4h (market data refreshes more often)
    const val OHLCV_CACHE_TTL_MS      = 86_400_000L   // 24h

    // OHLCV data retention
    const val OHLCV_MAX_RETENTION_DAYS            = 365   // Days before auto-cleanup
    const val INVESTOR_TRADING_MAX_RETENTION_DAYS = 180
    const val OHLCV_CACHE_SUFFICIENCY_RATIO       = 0.65  // 65% of trading days must be present

    // API rate limiting
    const val API_RATE_LIMIT_MS       = 500L          // Min interval between calls per category

    // Indicator defaults
    const val DEFAULT_INDICATOR_DAYS  = 180
    const val CHART_MAX_DISPLAY_DAYS  = 180

    // DeMark TD thresholds
    const val DEMARK_ACTIVE_THRESHOLD = 5             // Count >= 5: show warning
    const val DEMARK_STRONG_THRESHOLD = 9             // Count >= 9: reversal likely

    // Fear/Greed thresholds
    const val FEAR_GREED_GREED_THRESHOLD = 0.5
    const val FEAR_GREED_FEAR_THRESHOLD  = -0.5
}
```

---

### 3.3 FeatureFlags

**Package**: `com.stockapp.core.config`
**File**: `core/config/FeatureFlags.kt`

Runtime feature toggles stored in Jetpack DataStore (`feature_flags` preferences file).

#### 3.3.1 Flag Keys and Defaults

```kotlin
object FeatureFlags {
    const val ENABLE_REALTIME_SUPPLY = "enable_realtime_supply"
    const val USE_KRX_DATA_SOURCE    = "use_krx_data_source"

    val DEFAULTS = mapOf(
        ENABLE_REALTIME_SUPPLY to true,
        USE_KRX_DATA_SOURCE    to true
    )
}
```

| Flag Key | Default | Effect when `true` |
|----------|---------|-------------------|
| `enable_realtime_supply` | `true` | Fetches intraday supply data from Kiwoom `ka10063` during trading hours and merges with historical data |
| `use_krx_data_source` | `true` | Activates KRX-First strategy: batch data (OHLCV, stock list, investor trading) comes from KRX with Kiwoom/KIS as fallback |

#### 3.3.2 FeatureFlagRepo Interface

```kotlin
interface FeatureFlagRepo {
    suspend fun isEnabled(flag: String): Boolean
    suspend fun setEnabled(flag: String, enabled: Boolean)
    fun observeFlag(flag: String): Flow<Boolean>
    suspend fun getAllFlags(): Map<String, Boolean>
    suspend fun resetToDefaults()
}
```

**Implementation**: `FeatureFlagRepoImpl` uses `DataStore<Preferences>` with `booleanPreferencesKey`. Missing keys fall back to `FeatureFlags.DEFAULTS`.

#### 3.3.3 Usage Pattern in Repository

```kotlin
// In NativeAnalysisRepoImpl
val useKrx = featureFlagRepo.isEnabled(FeatureFlags.USE_KRX_DATA_SOURCE)
val data = if (useKrx) {
    krxDataSource.getOhlcvByTicker(startDate, endDate, ticker)
        .getOrElse { apiClient.fetchOhlcv(ticker, startDate, endDate) }
} else {
    apiClient.fetchOhlcv(ticker, startDate, endDate)
}
```

---

### 3.4 Cache Strategy

#### 3.4.1 StockCacheManager

**Package**: `com.stockapp.core.cache`
**File**: `core/cache/StockCacheManager.kt`
**Scope**: `@Singleton`

Manages the lifecycle of the stock list cache (the `stocks` table). This is the prerequisite for search autocomplete.

```kotlin
sealed class CacheState {
    data object Idle : CacheState()
    data object Loading : CacheState()
    data class Ready(val count: Int) : CacheState()    // Cache valid, count stocks loaded
    data class Stale(val count: Int) : CacheState()    // Cache expired but usable
    data class Error(val message: String) : CacheState()
}

data class CacheStats(
    val count: Int,
    val lastUpdatedMs: Long,
    val isExpired: Boolean
)
```

#### 3.4.2 StockCacheManager Function Contracts

| Function | Signature | Business Logic |
|----------|-----------|----------------|
| `initializeLazy` | `suspend fun initializeLazy(): Result<CacheStats>` | If any stocks exist in DB, return immediately with `Ready` or `Stale` state without calling any API. Only calls API if DB is completely empty. Use this at app startup. |
| `initializeIfNeeded` | `suspend fun initializeIfNeeded(): Result<Int>` | Checks TTL. If cache age exceeds `STOCK_CACHE_TTL_MS`, blocks and refreshes. Use only when fresh data is strictly required. |
| `refreshCache` | `suspend fun refreshCache(bypassCooldown: Boolean = false): Result<Int>` | Enforces 30-second cooldown between external calls unless `bypassCooldown = true`. On success, calls `stockDao.smartSync(stocks)`. Returns count. |
| `isCacheAvailable` | `suspend fun isCacheAvailable(): Boolean` | Returns `true` if `stockDao.count() > 0`. Does not check TTL. |
| `isRefreshAvailable` | `fun isRefreshAvailable(): Boolean` | Returns `true` if 30s has elapsed since last refresh attempt. Non-suspending. |
| `getRemainingCooldownSec` | `fun getRemainingCooldownSec(): Int` | Returns seconds until next refresh is allowed. Returns 0 if available. |

**Cooldown constant**: `REFRESH_COOLDOWN_MS = 30_000L` (defined locally in the file, not in `AppConfig`).

**Size limit**: If the API returns more than `AppConfig.MAX_STOCK_CACHE_SIZE` (10,000) stocks, they are truncated after sorting by market priority (KOSPI first, then KOSDAQ, then OTHER) and then alphabetically by name.

#### 3.4.3 Cache TTL Summary

| Data Type | TTL | DB Table / Entity |
|-----------|-----|-------------------|
| Stock list | 24h | `stocks` via `AppDb.STOCK_CACHE_TTL` |
| Supply/Demand analysis | 24h | `analysis_cache` |
| Technical indicators | 24h | `indicator_cache` |
| Financial data | 24h | `financial_cache` |
| Market indicators | 4h | `market_indicator_cache` |
| OHLCV (raw bars) | 24h | `ohlcv_cache` |
| Investor trading (raw) | 24h | `investor_trading_cache` |
| Realtime supply | 1 min | `realtime_supply_cache` |

---

### 3.5 TradingDayUtil

**Package**: `com.stockapp.core.util`
**File**: `core/util/TradingDayUtil.kt`
**Type**: `object` (no instantiation)

Utility for Korean stock market calendar calculations. Used by `AnalysisRepo` and `AnalysisVm` to determine whether to fetch intraday data.

#### 3.5.1 Trading Hours Definition

```
Trading Hours: Monday - Friday, 09:00 ~ 15:30 KST
Excluded: Korean national holidays (recurring + lunar) and special closures
```

**Note**: `TradingDayUtil` does not contain a `isTradingHours()` function. Trading-hours detection (checking the current clock time against 09:00-15:30 window) is performed in `AnalysisVm` or `NativeAnalysisRepoImpl` using `LocalTime.now()` in conjunction with `TradingDayUtil.isTradingDay()` for the date check.

#### 3.5.2 Key Functions

| Function | Signature | Input | Output | Description |
|----------|-----------|-------|--------|-------------|
| `isTradingDay` | `fun isTradingDay(date: LocalDate): Boolean` | Any `LocalDate` | `Boolean` | Returns `false` for weekends, recurring Korean holidays, year-specific lunar holidays (2025, 2026 defined), and special closures |
| `getMostRecentTradingDay` | `fun getMostRecentTradingDay(date: LocalDate = LocalDate.now()): LocalDate` | Reference date (defaults to today) | Most recent trading `LocalDate` | Walks backward up to 30 days. Safe to call on weekends/holidays. |
| `getPreviousTradingDay` | `fun getPreviousTradingDay(date: LocalDate): LocalDate` | Any `LocalDate` | Previous trading day (never the input date) | Starts from `date.minusDays(1)` and walks backward. |
| `getTradingDaysBetween` | `fun getTradingDaysBetween(start: LocalDate, end: LocalDate): List<LocalDate>` | Inclusive date range | List of trading `LocalDate` in chronological order | Returns empty list if `start.isAfter(end)` |
| `findMissingTradingDays` | `fun findMissingTradingDays(collectedDates: Set<String>, start: LocalDate, end: LocalDate): List<String>` | Set of `yyyy-MM-dd` strings already in cache; date range | List of `yyyy-MM-dd` missing trading days | Used by OHLCV cache gap detection |
| `parseApiDate` | `fun parseApiDate(apiDate: String?): LocalDate?` | `yyyyMMdd` string (API format) | `LocalDate?` or `null` on failure | Validates length == 8 before parsing |
| `apiToDbFormat` | `fun apiToDbFormat(apiDate: String?): String?` | `yyyyMMdd` API string | `yyyy-MM-dd` DB string or `null` | Combines `parseApiDate` + `toDbFormat` |

#### 3.5.3 Holiday Data (Built-in)

| Holiday Set | Coverage |
|-------------|----------|
| `RECURRING_HOLIDAYS` | New Year (01-01), 3.1 Movement (03-01), Children's Day (05-05), Memorial Day (06-06), Liberation Day (08-15), Foundation Day (10-03), Hangul Day (10-09), Christmas (12-25), Year-end close (12-31) |
| `LUNAR_HOLIDAYS_2025` | Seollal: 01-28~30; Buddha's Birthday: 05-05; Chuseok: 10-05~07 |
| `LUNAR_HOLIDAYS_2026` | Seollal: 02-16~18; Buddha's Birthday: 05-24; Chuseok: 09-24~26 |
| `SPECIAL_CLOSURES` | `2025-06-04` (local election), `2026-03-09` (presidential election) |

**Important**: Lunar holidays must be updated manually each year by extending the `when` block inside `isTradingDay`.

---

### 3.6 Data Source Strategy

**Package**: `com.stockapp.core.krx`
**File**: `core/krx/DataSourceStrategy.kt`

```kotlin
enum class DataSourceStrategy {
    KRX_FIRST,     // Primary: KRX direct API; Fallback: Kiwoom/KIS
    BROKER_FIRST   // Primary: Kiwoom/KIS; no KRX fallback
}
```

| Strategy | Used For | Rationale |
|----------|----------|-----------|
| `KRX_FIRST` | Stock list (getAll), OHLCV, investor trading, market cap, ETF list | KRX is free, no API key required, richer data |
| `BROKER_FIRST` | Realtime supply, ranking, financial data | KRX does not support realtime or financial statements |

The active strategy is controlled by `FeatureFlags.USE_KRX_DATA_SOURCE`. When `true`, repositories use `KRX_FIRST` for applicable data types. When `false`, they default to `BROKER_FIRST` (direct Kiwoom/KIS calls only).

---

### 3.7 API Clients

#### 3.7.1 KiwoomApiClient

**Package**: `com.stockapp.core.api`
**File**: `core/api/KiwoomApiClient.kt`
**Scope**: `@Singleton`

```kotlin
@Singleton
class KiwoomApiClient @Inject constructor(
    private val tokenManager: TokenManager,
    private val httpClient: OkHttpClient,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
)
```

**Rate Limiting**: Category-based, independent 500ms minimum interval per category. Categories do not block each other.

```kotlin
enum class ApiCategory {
    SEARCH,      // ka10099
    ANALYSIS,    // ka10059, ka10001, ka10081, ka10082, ka10083
    RANKING,     // ka10021, ka10023, ka10030, ka10033, ka90009
    FINANCIAL,   // FHKST66430xxx series
    ETF,         // ka40004
    OTHER
}
```

**Authentication**: Bearer token via `TokenManager`. On 401/403 response, automatically attempts token refresh once before returning failure.

**Pagination**: Supported via `nextKey` parameter in request body. Callers iterate until `nextKey` is `null` or `""`.

**Base URL**: `https://api.kiwoom.com` (prod) / configured per environment.

#### 3.7.2 KisApiClient

**Package**: `com.stockapp.core.api`
**File**: `core/api/KisApiClient.kt`
**Scope**: `@Singleton`

```kotlin
data class KisApiConfig(
    val appKey: String,
    val appSecret: String,
    val baseUrl: String = "https://openapi.koreainvestment.com:9443"
)

data class KisTokenInfo(
    val token: String,
    val expiresAt: LocalDateTime,
    val tokenType: String = "Bearer"
) {
    val bearer: String get() = "$tokenType $token"
    fun isExpired(): Boolean = LocalDateTime.now() >= expiresAt.minusMinutes(5)
}
```

**Authentication**: OAuth2. Token is fetched on first use and cached in memory. Refresh is triggered when `isExpired()` returns `true` (5 minutes before actual expiry).

**Rate Limiting**: Global 500ms interval (single mutex, not category-based).

**Used exclusively by**: `FinancialRepoImpl` for KIS financial API calls (`FHKST66430xxx` API IDs).

#### 3.7.3 KrxDataSource

**Package**: `com.stockapp.core.krx`
**File**: `core/krx/KrxDataSource.kt`
**Scope**: `@Singleton`

Wraps the `kotlin_krx` (`:krxkt`) Gradle submodule. All calls are wrapped in `safeCall` which converts `KrxError` to `Result.failure`.

```kotlin
@Singleton
class KrxDataSource @Inject constructor(
    okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    val stock: KrxStock
    val etf: KrxEtf
    val index: KrxIndex
}
```

**Key Methods Used by Stock Analysis**:

| Method | Signature | Returns | Used By |
|--------|-----------|---------|---------|
| `getTickerList` | `suspend fun getTickerList(date: String, market: Market): Result<List<TickerInfo>>` | All ticker codes and names for a market on a given date | `NativeSearchRepoImpl.getAll()` |
| `getOhlcvByTicker` | `suspend fun getOhlcvByTicker(start: String, end: String, ticker: String): Result<List<StockOhlcvHistory>>` | Daily OHLCV bars | `OhlcvService`, `NativeIndicatorRepoImpl` |
| `getTradingByInvestor` | `suspend fun getTradingByInvestor(start: String, end: String, ticker: String, ...): Result<List<InvestorTrading>>` | Per-investor net buying | `NativeAnalysisRepoImpl` |

**Date format**: `yyyyMMdd` strings (e.g., `"20250101"`).

**Network requirement**: Korean network or VPN to reach `data.krx.co.kr`. Calls will fail with `KrxError` on restricted networks; the `KRX_FIRST` strategy then falls back to Kiwoom/KIS.

---

### 3.8 Database Entities

**Database class**: `AppDb` (Room, version 14, `stock_app.db`)
**Package**: `com.stockapp.core.db`

#### 3.8.1 Entities Used by Stock Analysis Feature

```kotlin
// Table: stocks
@Entity(tableName = "stocks",
    indices = [Index("name"), Index("market")])
data class StockEntity(
    @PrimaryKey val ticker: String,      // 6-digit KRX ticker, e.g. "005930"
    val name: String,                    // Korean stock name, e.g. "삼성전자"
    val market: String,                  // "KOSPI" | "KOSDAQ" | "OTHER"
    val updatedAt: Long                  // epoch ms
)

// Table: search_history
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticker: String,
    val name: String,
    val searchedAt: Long                 // epoch ms
)

// Table: analysis_cache
@Entity(tableName = "analysis_cache")
data class AnalysisCacheEntity(
    @PrimaryKey val ticker: String,
    val data: String,                    // JSON of StockData domain model
    val startDate: String,              // yyyy-MM-dd
    val endDate: String,                // yyyy-MM-dd
    val cachedAt: Long                  // epoch ms
)

// Table: indicator_cache
@Entity(tableName = "indicator_cache")
data class IndicatorCacheEntity(
    @PrimaryKey val key: String,         // format: "ticker:type:days:timeframe"
                                         // e.g. "005930:trend:180:daily"
    val ticker: String,
    val type: String,                    // "trend" | "elder" | "demark"
    val data: String,                    // JSON of indicator data
    val cachedAt: Long
)

// Table: financial_cache
@Entity(tableName = "financial_cache")
data class FinancialCacheEntity(
    @PrimaryKey val ticker: String,
    val name: String,
    val data: String,                    // JSON of FinancialDataCache
    val cachedAt: Long
)

// Table: ohlcv_cache  (composite PK: ticker + date)
@Entity(tableName = "ohlcv_cache",
    primaryKeys = ["ticker", "date"],
    indices = [Index("ticker"), Index("cachedAt")])
data class OhlcvCacheEntity(
    val ticker: String,
    val date: String,                    // yyyyMMdd format (NOT yyyy-MM-dd)
    val open: Int,
    val high: Int,
    val low: Int,
    val close: Int,
    val volume: Long,
    val cachedAt: Long
)

// Table: investor_trading_cache  (composite PK: ticker + date)
// For market-wide data: ticker = "MARKET_KOSPI" or "MARKET_KOSDAQ"
@Entity(tableName = "investor_trading_cache",
    primaryKeys = ["ticker", "date"],
    indices = [Index("ticker"), Index("cachedAt")])
data class InvestorTradingCacheEntity(
    val ticker: String,
    val date: String,                    // yyyyMMdd format
    val foreignNet: Long,               // Foreign net buying (백만원)
    val institutionNet: Long,           // Institutional net buying (백만원)
    val individualNet: Long,            // Individual net buying (백만원)
    val totalTrading: Long,             // Total trading value (백만원)
    val cachedAt: Long
)
```

#### 3.8.2 Sample Data

```kotlin
// StockEntity example
StockEntity(
    ticker = "005930",
    name = "삼성전자",
    market = "KOSPI",
    updatedAt = 1735689600000L          // 2025-01-01 00:00:00 UTC
)

// OhlcvCacheEntity example
OhlcvCacheEntity(
    ticker = "005930",
    date = "20250120",
    open = 56000,
    high = 57200,
    low = 55800,
    close = 56900,
    volume = 12_450_000L,
    cachedAt = 1737331200000L
)

// InvestorTradingCacheEntity example
InvestorTradingCacheEntity(
    ticker = "005930",
    date = "20250120",
    foreignNet = 45_820L,              // +458.2억원 (백만원 단위)
    institutionNet = -12_300L,         // -123억원
    individualNet = -33_520L,          // -335.2억원
    totalTrading = 712_500L,
    cachedAt = 1737331200000L
)

// Market-wide investor trading
InvestorTradingCacheEntity(
    ticker = "MARKET_KOSPI",
    date = "20250120",
    foreignNet = 285_000L,
    institutionNet = -120_000L,
    individualNet = -165_000L,
    totalTrading = 8_450_000L,
    cachedAt = 1737331200000L
)
```

#### 3.8.3 Critical Unit Notes

| Field | Unit | Conversion for Display |
|-------|------|----------------------|
| `mcap` in `StockData` | 원 (KRW) | Divide by `1_000_000_000_000` for 조원 |
| `for5d`, `ins5d` in `StockData` | 백만원 (million KRW) | Divide by `100` for 억원 |
| `foreignNet`, `institutionNet` in `InvestorTradingCacheEntity` | 백만원 | Divide by `100` for 억원 |
| `revenue`, `operatingProfit` in `IncomeStatement` | 억원 | Already display-ready |

---

### 3.9 Dependency Injection Structure

All modules use `@Module @InstallIn(SingletonComponent::class)`. Repository bindings use `abstract fun` with `@Binds @Singleton`.

#### 3.9.1 Core DI Module (`AppModule`)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient { /* 30s timeouts, cert pinning in release */ }

    @Provides @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton
    fun provideSelectedStockManager(): SelectedStockManager = SelectedStockManager()
}
```

#### 3.9.2 Feature DI Modules Pattern

Each feature module follows the same binding pattern:

```kotlin
// SearchModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {
    @Binds @Singleton
    abstract fun bindSearchRepo(impl: NativeSearchRepoImpl): SearchRepo
}

// AnalysisModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisModule {
    @Binds @Singleton
    abstract fun bindAnalysisRepo(impl: NativeAnalysisRepoImpl): AnalysisRepo
}

// IndicatorModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class IndicatorModule {
    @Binds @Singleton
    abstract fun bindIndicatorRepo(impl: NativeIndicatorRepoImpl): IndicatorRepo
}

// FinancialModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class FinancialModule {
    @Binds @Singleton
    abstract fun bindFinancialRepo(impl: FinancialRepoImpl): FinancialRepo
}
```

#### 3.9.3 Dispatcher Qualifiers

```kotlin
@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher      // Dispatchers.IO — all network and DB calls

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher // Dispatchers.Default — CPU-intensive calculations

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher    // Dispatchers.Main — UI updates (rarely needed with Compose)
```

---

## 4. Container Screen: StockAnalysisScreen

**File**: `feature/stockanalysis/ui/StockAnalysisScreen.kt`
**Package**: `com.stockapp.feature.stockanalysis.ui`

This screen is a pure Compose function with no ViewModel. All state management is delegated to the inner screens' ViewModels.

### 4.1 Full Implementation

```kotlin
private enum class StockTab(val title: String) {
    SEARCH("검색"),
    ANALYSIS("수급 분석"),
    INDICATOR("기술 지표"),
    FINANCIAL("재무정보")
}

private val tabs = StockTab.entries.toList()

@Composable
fun StockAnalysisScreen(initialTab: Int = 0) {
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, tabs.lastIndex),
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(tab.title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1          // Keep 1 page pre-loaded on each side
        ) { page ->
            when (tabs[page]) {
                StockTab.SEARCH    -> SearchScreen(onStockClick = { _ ->
                    scope.launch { pagerState.animateScrollToPage(StockTab.ANALYSIS.ordinal) }
                })
                StockTab.ANALYSIS  -> AnalysisScreen()
                StockTab.INDICATOR -> IndicatorScreen()
                StockTab.FINANCIAL -> FinancialScreen()
            }
        }
    }
}
```

### 4.2 Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `initialTab` | `Int` | `0` | Tab index to open on first composition. Clamped to valid range `[0, 3]` via `coerceIn`. |

### 4.3 Key Behavior Notes

- `beyondViewportPageCount = 1` causes Compose to compose (but not necessarily measure) the adjacent pages. This means `AnalysisVm`, `IndicatorVm`, and `FinancialVm` initialize when the user is on the Search tab, beginning to collect `selectedTicker` immediately.
- The `onStockClick` lambda receives the clicked `Stock` but ignores it (`_ ->`), because stock selection has already been committed to `SelectedStockManager` by `SearchVm` before the callback fires.
- Tab navigation uses `animateScrollToPage` (animated scroll), not `scrollToPage` (instant jump). This provides visual continuity.
- `ScrollableTabRow` with `edgePadding = 8.dp` allows the tab row to scroll horizontally if tabs overflow the screen width.

---

## 5. Document Map

This overview document is the entry point for a suite of four feature-specific specifications. Each child document is self-contained and covers one inner tab in full detail.

| Document | Feature Tab | Primary API | Data Source |
|----------|-------------|-------------|-------------|
| `STOCK_ANALYSIS_OVERVIEW.md` | Container (this file) | — | — |
| `STOCK_ANALYSIS_SEARCH.md` | 검색 (Search) | ka10099 (Kiwoom) / KRX `getTickerList` | KRX_FIRST |
| `STOCK_ANALYSIS_SUPPLY_DEMAND.md` | 수급 분석 (Analysis) | ka10059, ka10063 (Kiwoom) / KRX `getTradingByInvestor` | KRX_FIRST |
| `STOCK_ANALYSIS_INDICATORS.md` | 기술 지표 (Indicator) | ka10081/82/83 (Kiwoom) / KRX `getOhlcvByTicker` | KRX_FIRST |
| `STOCK_ANALYSIS_FINANCIAL.md` | 재무정보 (Financial) | FHKST66430xxx (KIS API) | BROKER_FIRST |

### 5.1 Cross-Reference Index

| Concept | Defined In | Referenced In |
|---------|-----------|---------------|
| `SelectedStockManager` | Section 3.1 | All 4 child specs |
| `CacheState` / `StockCacheManager` | Section 3.4 | Search spec |
| `TradingDayUtil` | Section 3.5 | Supply/demand spec |
| `DataSourceStrategy` | Section 3.6 | Search, Supply/demand, Indicator specs |
| `KrxDataSource` methods | Section 3.7.3 | Search, Supply/demand, Indicator specs |
| `AppConfig` constants | Section 3.2 | All 4 child specs |
| `FeatureFlags` | Section 3.3 | Search, Supply/demand, Indicator specs |
| DB entities (ohlcv, investor_trading) | Section 3.8 | Supply/demand, Indicator specs |
| `FinancialData` / `FinancialSummary` | Financial spec | Financial spec only |

---

## 6. Technology Stack Requirements

All versions are minimum requirements. The target platform is **Android API 26+** (Android 8.0+).

| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| Kotlin | 2.1.0+ | Language | Coroutines, serialization plugins required |
| Jetpack Compose BOM | 2024.12.01+ | UI framework | Material3 components only (no Material2) |
| Compose Foundation Pager | included in BOM | HorizontalPager, PagerState | |
| Hilt | 2.54+ | Dependency injection | KSP annotation processor |
| Room | 2.8.3+ | Local database | KSP annotation processor |
| WorkManager | 2.10.0+ | Background sync | Not required for Stock Analysis feature itself |
| OkHttp | 4.12.0+ | HTTP client | Used by KiwoomApiClient and KisApiClient |
| kotlinx-serialization | 1.7.1+ | JSON parsing | `@Serializable` on all DTO classes |
| MPAndroidChart | 3.1.0 | Charts | AndroidView wrapper required in Compose |
| kotlin_krx (:krxkt) | 1.0.0-SNAPSHOT | KRX direct data | Local Gradle submodule; Korean network required |
| Jetpack DataStore | 1.1.x+ | Feature flags persistence | Preferences DataStore variant |
| EncryptedSharedPreferences | security-crypto 1.1.x | API key storage | AES256 encryption |

### 6.1 Kotlin Compiler Plugins Required

```kotlin
// build.gradle.kts (app)
plugins {
    alias(libs.plugins.kotlin.serialization)  // kotlinx-serialization
    alias(libs.plugins.hilt)                  // Hilt code generation
    alias(libs.plugins.ksp)                   // KSP (Room + Hilt)
}
```

### 6.2 Android Manifest Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## 7. File Manifest

Complete list of all files required to implement the Stock Analysis feature group. Files are listed by layer.

### 7.1 Core Infrastructure Files (Implement First)

These files are shared prerequisites. Any of the four feature tabs will fail without them.

```
app/src/main/java/com/stockapp/
├── core/
│   ├── config/
│   │   ├── AppConfig.kt                        -- All numeric constants
│   │   ├── FeatureFlags.kt                     -- Feature flag keys, defaults, FeatureFlagRepo
│   │   └── ConfigModule.kt                     -- DI binding for FeatureFlagRepo
│   ├── state/
│   │   └── SelectedStockManager.kt             -- Cross-screen stock selection singleton
│   ├── cache/
│   │   └── StockCacheManager.kt                -- Stock list cache lifecycle manager
│   ├── krx/
│   │   ├── DataSourceStrategy.kt               -- KRX_FIRST / BROKER_FIRST enum
│   │   └── KrxDataSource.kt                    -- kotlin_krx library wrapper (@Singleton)
│   ├── api/
│   │   ├── KiwoomApiClient.kt                  -- Kiwoom REST client with category rate limiting
│   │   ├── KisApiClient.kt                     -- KIS REST client with OAuth2
│   │   ├── TokenManager.kt                     -- Kiwoom token cache
│   │   ├── ApiModels.kt                        -- Shared API request/response DTOs
│   │   └── token/
│   │       ├── TokenProvider.kt                -- Token provider interface
│   │       └── BaseTokenManager.kt             -- Shared token refresh logic
│   ├── db/
│   │   ├── AppDb.kt                            -- Room database definition (v14)
│   │   ├── entity/
│   │   │   ├── StockEntity.kt                  -- stocks table
│   │   │   ├── SearchHistoryEntity.kt          -- search_history table (in StockEntity.kt)
│   │   │   ├── AnalysisCacheEntity.kt          -- analysis_cache table (in StockEntity.kt)
│   │   │   ├── IndicatorCacheEntity.kt         -- indicator_cache table (in StockEntity.kt)
│   │   │   ├── FinancialCacheEntity.kt         -- financial_cache table (in StockEntity.kt)
│   │   │   ├── OhlcvCacheEntity.kt             -- ohlcv_cache table (composite PK)
│   │   │   ├── InvestorTradingCacheEntity.kt   -- investor_trading_cache table (composite PK)
│   │   │   ├── RealtimeSupplyCacheEntity.kt    -- realtime_supply_cache table
│   │   │   └── MarketIndicatorCacheEntity.kt   -- market_indicator_cache table
│   │   └── dao/
│   │       ├── StockDao.kt                     -- stocks CRUD + smartSync
│   │       ├── SearchHistoryDao.kt             -- search_history CRUD
│   │       ├── AnalysisCacheDao.kt             -- analysis_cache CRUD
│   │       ├── IndicatorCacheDao.kt            -- indicator_cache CRUD
│   │       ├── FinancialCacheDao.kt            -- financial_cache CRUD
│   │       ├── OhlcvCacheDao.kt                -- ohlcv_cache queries
│   │       ├── InvestorTradingCacheDao.kt      -- investor_trading_cache queries
│   │       └── RealtimeSupplyCacheDao.kt       -- realtime_supply_cache CRUD
│   ├── di/
│   │   ├── AppModule.kt                        -- OkHttpClient, Json, dispatchers
│   │   └── DbModule.kt                         -- Room AppDb + all DAOs
│   ├── util/
│   │   └── TradingDayUtil.kt                   -- Korean market calendar
│   ├── network/
│   │   ├── CertificatePinningConfig.kt         -- Release cert pinning
│   │   └── CertificateHashExtractor.kt         -- Debug: logs SPKI hashes
│   ├── stock/
│   │   ├── api/
│   │   │   └── StockApiModels.kt               -- Kiwoom ka10099 response models
│   │   ├── calc/
│   │   │   ├── MathUtil.kt                     -- EMA, SMA helper functions
│   │   │   ├── OhlcvResampler.kt               -- Daily → weekly/monthly resampling
│   │   │   ├── TrendCalculator.kt              -- MA, CMF, Fear/Greed calculation
│   │   │   ├── ElderCalculator.kt              -- EMA13, MACD, Elder Impulse color
│   │   │   └── DemarkCalculator.kt             -- TD Buy/Sell Setup counting
│   │   └── data/
│   │       ├── OhlcvService.kt                 -- Shared OHLCV fetch + cache service
│   │       └── InvestorTradingService.kt        -- Shared investor trading fetch + cache
│   ├── error/
│   │   ├── AppError.kt                         -- Sealed error hierarchy
│   │   ├── ErrorCompat.kt                      -- Result<T> extension helpers
│   │   └── ErrorStringProvider.kt              -- Error code → Korean message mapping
│   └── ui/
│       ├── theme/
│       │   ├── Theme.kt
│       │   ├── Color.kt
│       │   ├── ExtendedColors.kt
│       │   ├── Type.kt
│       │   ├── Shape.kt
│       │   ├── Spacing.kt
│       │   ├── Elevation.kt
│       │   └── Motion.kt
│       └── component/
│           ├── ErrorCard.kt                    -- Reusable error state card
│           ├── LoadingIndicator.kt             -- Reusable loading spinner
│           ├── StateCards.kt                   -- Empty/No-selection state cards
│           ├── chart/
│           │   ├── ChartUtils.kt               -- MPAndroidChart helpers
│           │   ├── AnalysisCharts.kt           -- Supply/demand charts
│           │   ├── IndicatorCharts.kt          -- Technical indicator charts
│           │   ├── MarketCharts.kt             -- Market indicator charts
│           │   ├── UtilityCharts.kt            -- Shared chart utilities
│           │   ├── DateRangeSelector.kt        -- Date range picker composable
│           │   └── CustomMarkerView.kt         -- MPAndroidChart tooltip
│           └── stockinput/
│               ├── StockInputField.kt          -- Reusable stock search field
│               ├── StockInputDefaults.kt       -- Default styling constants
│               ├── StockInputHistoryDialog.kt  -- History popup
│               ├── model/
│               │   └── StockInputModels.kt     -- Input field state models
│               └── state/
│                   └── StockInputState.kt      -- Input field state holder
```

### 7.2 Container Screen Files

```
app/src/main/java/com/stockapp/
└── feature/
    └── stockanalysis/
        └── ui/
            └── StockAnalysisScreen.kt          -- HorizontalPager 4-tab container
```

### 7.3 Search Feature Files

```
app/src/main/java/com/stockapp/
└── feature/
    └── search/
        ├── domain/
        │   ├── model/
        │   │   └── Stock.kt                    -- Stock, Market enum, StockDto, SearchResponse
        │   ├── repo/
        │   │   └── SearchRepo.kt               -- Search repository interface
        │   └── usecase/
        │       └── SearchStockUC.kt            -- SearchStockUC, SaveHistoryUC
        ├── data/
        │   └── repo/
        │       └── NativeSearchRepoImpl.kt     -- KRX_FIRST + Kiwoom fallback implementation
        ├── ui/
        │   ├── SearchScreen.kt                 -- Search UI composable
        │   └── SearchVm.kt                     -- SearchState, SearchVm
        └── di/
            └── SearchModule.kt                 -- Binds NativeSearchRepoImpl -> SearchRepo
```

### 7.4 Supply/Demand Analysis Feature Files

```
app/src/main/java/com/stockapp/
└── feature/
    └── analysis/
        ├── domain/
        │   ├── model/
        │   │   └── StockData.kt                -- StockData, AnalysisSummary, SupplySignal,
        │   │                                      AnalysisResponse, toSummary()
        │   ├── repo/
        │   │   └── AnalysisRepo.kt             -- getAnalysis, getAnalysisWithIntraday,
        │   │                                      getCachedAnalysis, clearCache
        │   └── usecase/
        │       └── GetAnalysisUC.kt            -- Orchestrates normal vs intraday fetch
        ├── data/
        │   └── repo/
        │       ├── NativeAnalysisRepoImpl.kt   -- KRX_FIRST + Kiwoom ka10059 + intraday merge
        │       ├── CachedStockData.kt          -- Cache read/write helper
        │       └── IntradayDataMerger.kt       -- Merges ka10063 intraday into historical
        ├── ui/
        │   ├── AnalysisScreen.kt               -- Supply/demand UI composable
        │   └── AnalysisVm.kt                   -- AnalysisUiState, AnalysisVm
        └── di/
            └── AnalysisModule.kt               -- Binds NativeAnalysisRepoImpl -> AnalysisRepo
```

### 7.5 Technical Indicators Feature Files

```
app/src/main/java/com/stockapp/
└── feature/
    └── indicator/
        ├── domain/
        │   ├── model/
        │   │   └── IndicatorModels.kt          -- TrendSignal, ElderImpulse, DemarkSetup,
        │   │                                      *Summary types, toSummary() extensions,
        │   │                                      IndicatorType enum
        │   ├── repo/
        │   │   └── IndicatorRepo.kt            -- getTrend, getElder, getDemark, clearCache
        │   └── usecase/
        │       ├── GetTrendUC.kt
        │       ├── GetElderUC.kt
        │       └── GetDemarkUC.kt
        ├── data/
        │   └── repo/
        │       └── NativeIndicatorRepoImpl.kt  -- OhlcvService + calculator integration
        ├── ui/
        │   ├── IndicatorScreen.kt              -- Tabbed indicator container
        │   ├── IndicatorVm.kt                  -- IndicatorUiState, IndicatorVm
        │   ├── TrendContentSection.kt          -- Trend Signal tab content
        │   ├── ElderContentSection.kt          -- Elder Impulse tab content
        │   ├── DemarkContentSection.kt         -- DeMark TD tab content
        │   └── IndicatorComponents.kt          -- Shared indicator UI components
        └── di/
            └── IndicatorModule.kt              -- Binds NativeIndicatorRepoImpl -> IndicatorRepo
```

### 7.6 Financial Information Feature Files

```
app/src/main/java/com/stockapp/
└── feature/
    └── financial/
        ├── domain/
        │   ├── model/
        │   │   └── FinancialModels.kt          -- FinancialData, FinancialSummary, BalanceSheet,
        │   │                                      IncomeStatement, ProfitabilityRatios,
        │   │                                      StabilityRatios, GrowthRatios,
        │   │                                      FinancialDataCache (*Cache types),
        │   │                                      toSummary(), toCache(), toData()
        │   ├── repo/
        │   │   └── FinancialRepo.kt            -- getFinancialData, refreshFinancialData,
        │   │                                      clearCache, clearExpiredCache
        │   └── usecase/
        │       └── GetFinancialSummaryUC.kt    -- Fetches + converts to FinancialSummary
        ├── data/
        │   ├── dto/
        │   │   └── FinancialDto.kt             -- KIS API response DTOs for all 7 endpoints
        │   └── repo/
        │       └── FinancialRepoImpl.kt        -- KIS API calls + cache merge by stac_yymm
        ├── ui/
        │   ├── FinancialScreen.kt              -- Financial info composable with 2 tabs
        │   ├── FinancialVm.kt                  -- FinancialUiState, FinancialVm
        │   ├── ProfitabilityContent.kt         -- 수익성 tab: revenue/profit bar + growth line
        │   └── StabilityContent.kt             -- 안정성 tab: debt ratio / current ratio
        └── di/
            └── FinancialModule.kt              -- Binds FinancialRepoImpl -> FinancialRepo
```

### 7.7 Implementation Order

Implement files in this order to minimize broken-dependency states during development:

```
Phase 1 — Core infrastructure (no feature dependencies):
  AppConfig.kt, FeatureFlags.kt, AppError.kt, ErrorCompat.kt
  StockEntity.kt (all entities in one file), OhlcvCacheEntity.kt,
  InvestorTradingCacheEntity.kt, all DAO files, AppDb.kt, DbModule.kt
  AppModule.kt (OkHttpClient, Json, dispatchers)
  TradingDayUtil.kt
  DataSourceStrategy.kt, KrxDataSource.kt
  TokenManager.kt, KiwoomApiClient.kt, KisApiClient.kt
  MathUtil.kt, OhlcvResampler.kt, TrendCalculator.kt,
  ElderCalculator.kt, DemarkCalculator.kt
  OhlcvService.kt, InvestorTradingService.kt
  SelectedStockManager.kt, StockCacheManager.kt
  ConfigModule.kt (FeatureFlagRepo binding)

Phase 2 — Search feature (required by cache initialization):
  Stock.kt, SearchRepo.kt, SearchStockUC.kt
  NativeSearchRepoImpl.kt, SearchModule.kt
  SearchVm.kt, SearchScreen.kt (can use stub UI)

Phase 3 — Container screen + Supply/Demand:
  StockAnalysisScreen.kt
  StockData.kt, AnalysisRepo.kt, GetAnalysisUC.kt
  CachedStockData.kt, IntradayDataMerger.kt, NativeAnalysisRepoImpl.kt
  AnalysisModule.kt, AnalysisVm.kt, AnalysisScreen.kt

Phase 4 — Technical indicators:
  IndicatorModels.kt, IndicatorRepo.kt, GetTrendUC.kt, GetElderUC.kt,
  GetDemarkUC.kt, NativeIndicatorRepoImpl.kt, IndicatorModule.kt
  IndicatorVm.kt, IndicatorScreen.kt + content sections

Phase 5 — Financial data:
  FinancialModels.kt, FinancialRepo.kt, GetFinancialSummaryUC.kt
  FinancialDto.kt, FinancialRepoImpl.kt, FinancialModule.kt
  FinancialVm.kt, FinancialScreen.kt + content sections

Phase 6 — UI polish:
  All chart composables, StockInputField, ErrorCard, StateCards, theme files
```

---

*End of STOCK_ANALYSIS_OVERVIEW.md*

*Next: See `STOCK_ANALYSIS_SEARCH.md` for detailed Search tab specification.*
