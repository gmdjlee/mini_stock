# FILE_MANIFEST.md - Financial Information Feature

## Purpose

This manifest documents every file that participates in the Financial Information feature of
the StockApp Android application. The feature presents KIS (Korea Investment and Securities)
API data in two tabs: Profitability (수익성) and Stability (안정성). Each row identifies the
file's architectural layer, its specific role within that layer, and the concrete classes,
interfaces, or resources it depends upon from elsewhere in the feature or the shared core.

Cross-reference numbering follows the order of the summary table. Use the `#` entry number
when tracing a dependency chain across layers.

---

## Layer Definitions

| Layer | Description |
|---|---|
| UI | Jetpack Compose screens, ViewModels, and composable sub-components |
| Domain | Interfaces, use cases, and all pure-Kotlin model classes |
| Data | Repository implementations, DTOs, Room DAOs, and cache entities |
| Core | Shared infrastructure: DI modules, constants, state holders, reusable UI utilities |
| Navigation | Compose Navigation graph and route definitions |
| Resource | Android XML layout and drawable files |

---

## Summary Table

| # | File Path | Layer | Role | Dependencies |
|---|---|---|---|---|
| 1 | `app/src/main/java/com/stockapp/feature/financial/ui/FinancialScreen.kt` | UI | Entry-point composable for the Financial screen. Renders a `Scaffold` with a `TopAppBar` (title, refresh `IconButton`, `ThemeToggleButton`), a `TabRow` for 수익성/안정성 selection, and `PullToRefreshBox`. Dispatches to `ProfitabilityContent` or `StabilityContent` based on `FinancialVm.selectedTab`. Handles all five `FinancialState` branches: `NoStock`, `Loading`, `NoApiKey`, `Success`, `Error`. | `FinancialVm` (#2), `FinancialTab` (#5), `ProfitabilityContent` (#3), `StabilityContent` (#4), `ErrorCard` (#17), `ThemeToggleButton` (#20) |
| 2 | `app/src/main/java/com/stockapp/feature/financial/ui/FinancialVm.kt` | UI | `@HiltViewModel`. Owns the `FinancialState` sealed class (`NoStock`, `Loading`, `NoApiKey`, `Success`, `Error`). Observes `SelectedStockManager.selectedStock` in `init` and calls `GetFinancialSummaryUC.invoke()` (cache-first) or `GetFinancialSummaryUC.refresh()` on demand. Exposes `state: StateFlow<FinancialState>`, `selectedTab: StateFlow<FinancialTab>`, and `isRefreshing: StateFlow<Boolean>`. Public functions: `selectTab()`, `refresh()`, `retry()`. | `SelectedStockManager` (#14), `GetFinancialSummaryUC` (#7), `FinancialSummary` (#5), `FinancialTab` (#5) |
| 3 | `app/src/main/java/com/stockapp/feature/financial/ui/ProfitabilityContent.kt` | UI | Composable for the 수익성 tab. Renders a `SummaryCard` with latest revenue / operating profit / net income values, a stacked grouped `BarChart` (revenue, operating profit, net income by settlement period, values in 억원), a `LineChart` for revenue/operating profit/net income growth rates, and a second `LineChart` for equity/total-assets growth rates. All charts are built with MPAndroidChart via `AndroidView`. | `FinancialSummary` (#5), `IncomeBarMarkerView` (#18), `GrowthRateMarkerView` (#18), `setupCommonChartProperties()` (#19), `setupMarkerOffsets()` (#19), MPAndroidChart (`BarChart`, `LineChart`, `BarData`, `LineData`) |
| 4 | `app/src/main/java/com/stockapp/feature/financial/ui/StabilityContent.kt` | UI | Composable for the 안정성 tab. Renders a `StabilitySummaryCard` with evaluation labels and latest ratio values, a combined `LineChart` overlaying all three ratios, and three individual `LineChart` composables for 부채비율, 유동비율, and 차입금 의존도. | `FinancialSummary` (#5), `StabilityRatioMarkerView` (#18), `SingleRatioMarkerView` (#18), `setupCommonChartProperties()` (#19), `setupMarkerOffsets()` (#19), MPAndroidChart (`LineChart`, `LineData`) |
| 5 | `app/src/main/java/com/stockapp/feature/financial/domain/model/FinancialModels.kt` | Domain | Single-file domain model library. Defines the `FinancialTab` enum (`PROFITABILITY`, `STABILITY`), `FinancialPeriod` (yearMonth YYYYMM, year, quarter, `toDisplayString()`), all raw data classes (`BalanceSheet`, `IncomeStatement`, `ProfitabilityRatios`, `StabilityRatios`, `GrowthRatios`, `FinancialRatios`, `OtherMajorRatios`), the merged `FinancialData`, and the UI-ready `FinancialSummary`. Contains cache-serializable counterparts (`FinancialDataCache`, `BalanceSheetCache`, etc.) annotated with `@Serializable`. Provides three extension functions: `convertYtdToQuarterly()` (private, converts KIS cumulative YTD to standalone quarterly values), `FinancialData.toSummary()`, `FinancialData.toCache()`, and `FinancialDataCache.toData()`. | `kotlinx.serialization` (`@Serializable`, `@SerialName`), `android.util.Log` (YTD warning logging) |
| 6 | `app/src/main/java/com/stockapp/feature/financial/domain/repo/FinancialRepo.kt` | Domain | Repository interface. Declares four suspend functions: `getFinancialData(ticker, name, useCache)` returns `Result<FinancialData>` (cache-first when `useCache=true`); `refreshFinancialData(ticker, name)` returns `Result<FinancialData>` (API-only); `clearCache(ticker)` removes one ticker's cache row; `clearExpiredCache()` prunes all rows older than `FINANCIAL_CACHE_TTL_MS`. | `FinancialData` (#5) |
| 7 | `app/src/main/java/com/stockapp/feature/financial/domain/usecase/GetFinancialSummaryUC.kt` | Domain | Use case with `@Inject constructor`. Exposes `suspend operator fun invoke(ticker, name, useCache)` and `suspend fun refresh(ticker, name)`. Both delegate to the corresponding `FinancialRepo` method, call `FinancialData.toSummary()`, and emit structured `Log.d` output in `DEBUG` builds only. Returns `Result<FinancialSummary>`. | `FinancialRepo` (#6), `FinancialData` (#5), `FinancialSummary` (#5), `toSummary()` (#5), `BuildConfig.DEBUG` |
| 8 | `app/src/main/java/com/stockapp/feature/financial/data/dto/FinancialDto.kt` | Data | DTO layer. Defines the generic `KisApiResponse<T>` wrapper handling both `output` and `output1` field names via `actualOutput`. Provides seven `@Serializable` DTO data classes: `BalanceSheetDto`, `IncomeStatementDto`, `ProfitabilityRatiosDto`, `StabilityRatiosDto`, `GrowthRatiosDto`, `FinancialRatiosDto`, `OtherMajorRatiosDto`. Each has a `toDomain()` function mapping to the corresponding domain model. Includes package-private `parseNumericLong()` which handles comma-separated, decimal, and whitespace-padded numeric strings returned by the KIS API. | `kotlinx.serialization` (`@Serializable`, `@SerialName`), all domain model classes from `FinancialModels.kt` (#5) |
| 9 | `app/src/main/java/com/stockapp/feature/financial/data/repo/FinancialRepoImpl.kt` | Data | `@Singleton` concrete repository. Implements `FinancialRepo`. OAuth2 token management: acquires a token from `POST /oauth2/tokenP`, caches it for 23 hours, invalidates on `baseUrl` change, and uses a `Mutex` to prevent TOCTOU races. Data fetch: fires 5 parallel `async` coroutines (balance sheet, income statement, profitability ratios, stability ratios, growth ratios) via `coroutineScope`. Generic `fetchFinancialData<D,T>()` eliminates per-endpoint boilerplate. `mergeFinancialData()` unions all `stac_yymm` period keys and builds `FinancialData`. Cache write uses `FinancialCacheDao.insert()` with JSON-serialized `FinancialDataCache`. Cache read checks `cachedAt` against `AppConfig.FINANCIAL_CACHE_TTL_MS`. Expired cache triggers automatic API fallback. KIS transaction IDs: `FHKST66430100`–`FHKST66430800`. | `FinancialCacheDao` (#11), `FinancialCacheEntity` (#12), `SettingsRepo` (#24), `Json` (from `AppModule` #16), `OkHttpClient` (#16), `@IoDispatcher` (#16), `AppConfig.FINANCIAL_CACHE_TTL_MS` (#15), all DTOs (#8), domain models (#5), `toCache()` / `toData()` (#5) |
| 10 | `app/src/main/java/com/stockapp/feature/financial/di/FinancialModule.kt` | DI | Hilt `@Module`, `@InstallIn(SingletonComponent::class)`. Contains one `@Binds @Singleton abstract fun bindFinancialRepo(impl: FinancialRepoImpl): FinancialRepo`. Registers `FinancialRepoImpl` as the singleton implementation of `FinancialRepo` across the entire application component. | `FinancialRepoImpl` (#9), `FinancialRepo` (#6) |
| 11 | `app/src/main/java/com/stockapp/core/db/dao/FinancialCacheDao.kt` | Data | Room `@Dao`. Provides: `get(ticker): FinancialCacheEntity?` (single ticker lookup), `getAllOnce(): List<FinancialCacheEntity>` (full table scan), `getInDateRange(startMs, endMs): List<FinancialCacheEntity>` (time-bounded query), `insert(cache)` (upsert with `REPLACE` conflict strategy), `delete(ticker)` (single-row delete), `deleteExpired(threshold)` (bulk prune by `cachedAt < threshold`), `deleteAll()` (full table clear). All functions are `suspend`. | `FinancialCacheEntity` (#12) |
| 12 | `app/src/main/java/com/stockapp/core/db/entity/StockEntity.kt` | Data | Multi-entity file. Contains `FinancialCacheEntity`: `@Entity(tableName = "financial_cache")`, `@PrimaryKey val ticker: String`, `val name: String`, `val data: String` (JSON-serialized `FinancialDataCache`), `val cachedAt: Long = System.currentTimeMillis()`. Also contains `StockEntity`, `AnalysisCacheEntity`, `SearchHistoryEntity`, and `IndicatorCacheEntity` (not used by this feature). | Room (`@Entity`, `@PrimaryKey`) |
| 13 | `app/src/main/java/com/stockapp/core/db/AppDb.kt` | Data | Room `@Database`, version 14, `exportSchema = false`. Lists all 20 entity classes in `entities = [...]`. Exposes `abstract fun financialCacheDao(): FinancialCacheDao`. Defines migration `MIGRATION_7_8` which creates the `financial_cache` table (columns: `ticker TEXT PK`, `name TEXT`, `data TEXT`, `cachedAt INTEGER`). References `AppConfig` for shared TTL constants. | Room (`@Database`, `RoomDatabase`), `FinancialCacheEntity` (#12), `FinancialCacheDao` (#11), `AppConfig` (#15), all other entity and DAO classes in the project |
| 14 | `app/src/main/java/com/stockapp/core/state/SelectedStockManager.kt` | Core | `@Singleton` state holder injected by Hilt. Holds `_selectedStock: MutableStateFlow<Stock?>` and `_selectedTicker: MutableStateFlow<String?>`. Public API: `select(stock: Stock)`, `selectTicker(ticker: String, name: String? = null)`, `clear()`, `hasSelection(): Boolean`. `FinancialVm` collects `selectedStock` to observe cross-tab stock changes without explicit navigation parameters. | `Stock` model from `feature/search/domain/model` |
| 15 | `app/src/main/java/com/stockapp/core/config/AppConfig.kt` | Core | Singleton `object` of compile-time constants. Financial-relevant constant: `FINANCIAL_CACHE_TTL_MS = 24 * 60 * 60 * 1000L` (86,400,000 ms = 24 hours). Also referenced by `AppDb` for shared cache TTL names. Other constants (search debounce, rate limiting, indicator days) are not directly consumed by the financial feature. | None (pure constants) |
| 16 | `app/src/main/java/com/stockapp/core/di/AppModule.kt` | DI | Hilt `@Module`, `@InstallIn(SingletonComponent::class)`. Provides three singletons consumed by `FinancialRepoImpl`: `Json` (`ignoreUnknownKeys=true`, `isLenient=true`, `coerceInputValues=true`, `encodeDefaults=true`), `OkHttpClient` (30-second timeouts, certificate pinning in release via `CertificatePinningConfig`, `HttpLoggingInterceptor` in debug), and `@IoDispatcher CoroutineDispatcher` (`Dispatchers.IO`). Also provides `SelectedStockManager` singleton. | Hilt, `OkHttpClient`, `kotlinx.serialization.json.Json`, `SelectedStockManager` (#14), `CertificatePinningConfig`, `CertificateHashExtractor` |
| 17 | `app/src/main/java/com/stockapp/core/ui/component/ErrorCard.kt` | UI | Reusable `@Composable`. Parameters: `code: String`, `message: String`, `onRetry: (() -> Unit)? = null`, `onDismiss: (() -> Unit)? = null`. Renders a Material3 `Card` with `errorContainer` background, a warning `Icon`, a code label, a message body, an optional dismiss `IconButton` (top-right), and an optional retry `TextButton`. Used by `FinancialScreen` to render the `FinancialState.Error` state. Also exports `ErrorMessage` (compact inline variant, not used by this feature). | Material3 (`Card`, `CardDefaults`, `MaterialTheme`), `extendedShapes`, `spacing` (theme extensions) |
| 18 | `app/src/main/java/com/stockapp/core/ui/component/chart/CustomMarkerView.kt` | UI | MPAndroidChart `MarkerView` subclasses. Base class `SmartMarkerView` overrides `draw()` to apply boundary-aware horizontal clamping (constant `EDGE_PADDING = 8f px`). Financial-specific subclasses: `IncomeBarMarkerView` (shows period + revenue + operating profit + net income, all formatted as 조/천억/억); `GrowthRateMarkerView` (shows period + N labeled growth rate percentages from a `List<List<Double>>`); `StabilityRatioMarkerView` (shows period + 부채비율 + 유동비율 + 차입금의존도); `SingleRatioMarkerView` (shows period + one labeled ratio percentage). All read `R.id.tvContent` from `R.layout.chart_marker_view` (#26). | MPAndroidChart (`MarkerView`, `Entry`, `Highlight`, `MPPointF`), `R.layout.chart_marker_view` (#26) |
| 19 | `app/src/main/java/com/stockapp/core/ui/component/chart/ChartUtils.kt` | UI | Shared chart utilities. Extension functions consumed by this feature: `LineChart.setupCommonChartProperties()` (disables description, enables touch/drag/scale/pinchZoom, removes grid background, adds 10f bottom offset); `BarChart.setupCommonChartProperties()` (same); `LineChart.setupMarkerOffsets()` (applies `MARKER_EXTRA_OFFSET = 60f` on left and right via `setExtraOffsets()`); `BarChart.setupMarkerOffsets()` (same). Also defines `ChartCard` composable (styled card container with title, optional subtitle, animated size), `formatMarketCapForChart()`, `ChartLabelCalculator`, `DateFormatter`, and renderer helpers (not directly used by the financial feature screens). | MPAndroidChart (`BarChart`, `LineChart`, `CombinedChart`), Material3, `LocalExtendedColors` |
| 20 | `app/src/main/java/com/stockapp/core/theme/ThemeToggle.kt` | UI | Provides `ThemeToggleButton()` composable and its backing `LocalThemeToggle` `CompositionLocal`. `ThemeToggleButton` renders a Material3 `IconButton` that toggles between `Icons.Default.DarkMode` and `Icons.Default.LightMode`. Placed in the `TopAppBar` actions of `FinancialScreen`. | Material3 (`Icon`, `IconButton`), `LocalThemeToggle` (composition local) |
| 21 | `app/src/main/java/com/stockapp/nav/Nav.kt` | Navigation | Defines `Screen` sealed class hierarchy and `NavArgs` constants. `Screen.StockAnalysis` provides the route template `"stock_analysis?ticker={ticker}&tab={tab}"`, `baseRoute`, and `createRoute(ticker, tab)`. Declares deep-link URI patterns used by `NavGraph`: `stockapp://stock/{ticker}/financial` targets tab index 3 (Financial). Lists `bottomNavItems` for the bottom navigation bar. | Compose Navigation (`ImageVector`), Material Icons |
| 22 | `app/src/main/java/com/stockapp/nav/NavGraph.kt` | Navigation | Top-level `@Composable NavHost`. Registers the `StockAnalysis` composable route with `navArgument` declarations for optional `ticker` (String, nullable) and `tab` (Int, default 0). Registers four deep-link `uriPattern` entries including `stockapp://stock/{ticker}/financial`. Resolves `initialTab = 3` when the URI contains "financial". Instantiates `AnalysisVm` via `hiltViewModel()` and validates tickers before calling `selectTickerFromDeepLink()`. Passes `initialTab` to `StockAnalysisScreen`. | `StockAnalysisScreen` (#23), `Screen.StockAnalysis` (#21), `NavArgs` (#21), Compose Navigation |
| 23 | `app/src/main/java/com/stockapp/feature/stockanalysis/ui/StockAnalysisScreen.kt` | UI | Parent container for the unified stock analysis experience. Uses `HorizontalPager` with `beyondViewportPageCount = 1` and a `ScrollableTabRow` for four tabs: `SEARCH` (검색), `ANALYSIS` (수급 분석), `INDICATOR` (기술 지표), `FINANCIAL` (재무정보). Instantiates `FinancialScreen()` at page index 3. Receives `initialTab: Int` from `NavGraph` to land on the Financial tab when opened via deep link. | `FinancialScreen` (#1), `SearchScreen`, `AnalysisScreen`, `IndicatorScreen`, Compose Foundation (`HorizontalPager`, `PagerState`) |
| 24 | `app/src/main/java/com/stockapp/feature/settings/domain/repo/SettingsRepo.kt` | Domain | Repository interface for persisted settings. Financial feature uses one method: `getKisApiKeyConfig(): Flow<KisApiKeyConfig>`. `FinancialRepoImpl` calls `.first()` on this flow at the start of each API fetch cycle to retrieve the current `appKey`, `appSecret`, and `investmentMode`. | `KisApiKeyConfig` (#25), `ApiKeyConfig` |
| 25 | `app/src/main/java/com/stockapp/feature/settings/domain/model/ApiKeyConfig.kt` | Domain | Defines `InvestmentMode` enum (`MOCK`, `PRODUCTION`) and `KisApiKeyConfig` data class: `appKey: String`, `appSecret: String`, `investmentMode: InvestmentMode`. `isValid()` returns true when both key fields are non-blank. `getBaseUrl()` returns `"https://openapivts.koreainvestment.com:29443"` for MOCK or `"https://openapi.koreainvestment.com:9443"` for PRODUCTION. The base URL determines the OAuth2 token endpoint and all subsequent KIS API calls, and also acts as the cache-invalidation key for the token cache in `FinancialRepoImpl`. | None (pure data model) |
| 26 | `app/src/main/res/layout/chart_marker_view.xml` | Resource | Android `LinearLayout` inflated by all `SmartMarkerView` subclasses (#18) via `R.layout.chart_marker_view`. Contains a single `TextView` with `id="@+id/tvContent"`, white text color, 12sp text size, center gravity, and 8dp padding. Background is `@drawable/chart_marker_background` (#27). | `@drawable/chart_marker_background` (#27) |
| 27 | `app/src/main/res/drawable/chart_marker_background.xml` | Resource | Shape drawable. Rectangle with `solid color="#CC333333"` (80% opaque dark grey), corner radius 4dp, and 4dp padding on all sides. Referenced by `chart_marker_view.xml` (#26) as the visual background of chart tooltip callouts. | None |

---

## Files by Layer

### UI Layer (9 files)

| # | File | Primary Consumers |
|---|---|---|
| 1 | `feature/financial/ui/FinancialScreen.kt` | `StockAnalysisScreen` (#23) via HorizontalPager |
| 2 | `feature/financial/ui/FinancialVm.kt` | `FinancialScreen` (#1) via `hiltViewModel()` |
| 3 | `feature/financial/ui/ProfitabilityContent.kt` | `FinancialScreen` (#1) |
| 4 | `feature/financial/ui/StabilityContent.kt` | `FinancialScreen` (#1) |
| 17 | `core/ui/component/ErrorCard.kt` | `FinancialScreen` (#1) |
| 18 | `core/ui/component/chart/CustomMarkerView.kt` | `ProfitabilityContent` (#3), `StabilityContent` (#4) |
| 19 | `core/ui/component/chart/ChartUtils.kt` | `ProfitabilityContent` (#3), `StabilityContent` (#4) |
| 20 | `core/theme/ThemeToggle.kt` | `FinancialScreen` (#1) |
| 23 | `feature/stockanalysis/ui/StockAnalysisScreen.kt` | `NavGraph` (#22) |

### Domain Layer (6 files)

| # | File | Primary Consumers |
|---|---|---|
| 5 | `feature/financial/domain/model/FinancialModels.kt` | `FinancialVm` (#2), `GetFinancialSummaryUC` (#7), `FinancialRepoImpl` (#9), `FinancialDto.kt` (#8) |
| 6 | `feature/financial/domain/repo/FinancialRepo.kt` | `GetFinancialSummaryUC` (#7), `FinancialModule` (#10) |
| 7 | `feature/financial/domain/usecase/GetFinancialSummaryUC.kt` | `FinancialVm` (#2) |
| 24 | `feature/settings/domain/repo/SettingsRepo.kt` | `FinancialRepoImpl` (#9) |
| 25 | `feature/settings/domain/model/ApiKeyConfig.kt` | `SettingsRepo` (#24), `FinancialRepoImpl` (#9) |

### Data Layer (6 files)

| # | File | Primary Consumers |
|---|---|---|
| 8 | `feature/financial/data/dto/FinancialDto.kt` | `FinancialRepoImpl` (#9) |
| 9 | `feature/financial/data/repo/FinancialRepoImpl.kt` | Hilt graph via `FinancialModule` (#10) |
| 11 | `core/db/dao/FinancialCacheDao.kt` | `FinancialRepoImpl` (#9) |
| 12 | `core/db/entity/StockEntity.kt` | `FinancialCacheDao` (#11), `AppDb` (#13) |
| 13 | `core/db/AppDb.kt` | Hilt `DbModule` (provides `FinancialCacheDao` instance) |

### DI / Core Layer (5 files)

| # | File | Primary Consumers |
|---|---|---|
| 10 | `feature/financial/di/FinancialModule.kt` | Hilt application graph |
| 14 | `core/state/SelectedStockManager.kt` | `FinancialVm` (#2), `AppModule` (#16) |
| 15 | `core/config/AppConfig.kt` | `FinancialRepoImpl` (#9), `AppDb` (#13) |
| 16 | `core/di/AppModule.kt` | `FinancialRepoImpl` (#9), `FinancialVm` indirectly via Hilt |

### Navigation Layer (3 files)

| # | File | Primary Consumers |
|---|---|---|
| 21 | `nav/Nav.kt` | `NavGraph` (#22), bottom navigation composable |
| 22 | `nav/NavGraph.kt` | `MainActivity` or application-level composable |
| 23 | `feature/stockanalysis/ui/StockAnalysisScreen.kt` | `NavGraph` (#22) |

### Resource Layer (2 files)

| # | File | Primary Consumers |
|---|---|---|
| 26 | `res/layout/chart_marker_view.xml` | All `SmartMarkerView` subclasses (#18) via `R.layout.chart_marker_view` |
| 27 | `res/drawable/chart_marker_background.xml` | `chart_marker_view.xml` (#26) via `@drawable/chart_marker_background` |

---

## Dependency Flow Diagram

```
NavGraph (#22)
  └─ StockAnalysisScreen (#23)
       └─ FinancialScreen (#1) ── hiltViewModel() ──► FinancialVm (#2)
            │                                              │
            │  FinancialState.Success                      ├── SelectedStockManager (#14)
            │                                              └── GetFinancialSummaryUC (#7)
            ├─ ProfitabilityContent (#3)                         │
            │    ├─ IncomeBarMarkerView (#18)               FinancialRepo (#6)
            │    ├─ GrowthRateMarkerView (#18)                   │
            │    └─ ChartUtils extensions (#19)             FinancialRepoImpl (#9)
            │                                                     │
            ├─ StabilityContent (#4)                        ├── FinancialCacheDao (#11)
            │    ├─ StabilityRatioMarkerView (#18)          │     └─ FinancialCacheEntity (#12)
            │    ├─ SingleRatioMarkerView (#18)             │           └─ AppDb (#13)
            │    └─ ChartUtils extensions (#19)             │
            │                                               ├── SettingsRepo (#24) ── KisApiKeyConfig (#25)
            ├─ ErrorCard (#17)                              ├── Json + OkHttpClient (#16)
            └─ ThemeToggleButton (#20)                      ├── IoDispatcher (#16)
                                                            ├── AppConfig.FINANCIAL_CACHE_TTL_MS (#15)
                                                            └── FinancialDto (#8)
                                                                  └── FinancialModels (#5)

chart_marker_view.xml (#26) ── @drawable ──► chart_marker_background.xml (#27)
SmartMarkerView subclasses (#18) ── R.layout ──► chart_marker_view.xml (#26)
```

---

## Notes for Implementors

1. **Token cache invalidation**: `FinancialRepoImpl` stores `tokenBaseUrl` alongside the cached token. Any change to `KisApiKeyConfig.investmentMode` (mock vs production) causes a full re-authentication on the next request, even if the previous token has not expired by time.

2. **YTD-to-quarterly conversion**: The KIS API returns cumulative Year-to-Date values for quarterly income statement data. `convertYtdToQuarterly()` in `FinancialModels.kt` derives standalone quarterly values. If a prior quarter is absent from the API response, the function falls back to the raw YTD value and emits a `Log.w`. This may cause the next quarter's derived value to be incorrect.

3. **`FinancialCacheEntity` location**: The `FinancialCacheEntity` class is defined inside `core/db/entity/StockEntity.kt` (file #12) alongside `StockEntity`, `AnalysisCacheEntity`, `SearchHistoryEntity`, and `IndicatorCacheEntity`. The filename does not reflect the multi-entity content.

4. **`ThemeToggleButton` package**: The composable resides in `core/theme/ThemeToggle.kt` but is imported by `FinancialScreen` from the package `com.stockapp.core.theme`. The source file is named `ThemeToggle.kt`, not `ThemeToggleButton.kt`.

5. **DB version**: `AppDb` is currently at version 14. The `financial_cache` table was introduced in `MIGRATION_7_8`. Any migration targeting a version below 8 must include that migration step.

6. **`AppConfig.FINANCIAL_CACHE_TTL_MS`**: Set to `86_400_000L` (24 hours). This constant is the single source of truth for cache expiry and is checked in both `FinancialRepoImpl.isCacheExpired()` and `FinancialRepoImpl.clearExpiredCache()`.

7. **Parallel API calls**: `FinancialRepoImpl.refreshFinancialData()` launches five `async` coroutines inside a `coroutineScope`. If any individual endpoint fails, that sub-result defaults to `emptyList()` via the `catch` in `fetchFinancialData()`. The feature therefore tolerates partial API failures gracefully: a missing endpoint simply produces zeros in the corresponding chart series.
