# PROGRESS.md — Financial Info Migration Spec
## Status: LOOP_COMPLETE
## Completed
- [x] S-001 Feature entry point identified
- [x] S-002 Presentation layer traced (FinancialScreen, FinancialVm, ProfitabilityContent, StabilityContent)
- [x] S-003 Domain layer traced (FinancialRepo, GetFinancialSummaryUC, FinancialModels)
- [x] S-004 Data layer traced (FinancialRepoImpl, FinancialDto, FinancialCacheDao, FinancialCacheEntity)
- [x] S-005 External dependencies mapped (8 libs with versions)
- [x] S-006 Shared dependencies mapped (15+ core files)
- [x] S-007 API contracts documented (5 KIS endpoints + OAuth2)
- [x] S-008 Data models documented (15+ models with all fields)
- [x] S-009 Calculation logic documented (YTD conversion, merging, evaluation, formatting, token mgmt)
- [x] S-010 UI logic documented (state machine, tabs, charts, error handling)
- [x] S-011 MIGRATION_SPEC.md assembled (1844 lines, all 12 sections, 2 mermaid diagrams)
- [x] S-012 FILE_MANIFEST.md created (27 files with layer, role, dependencies)
- [x] S-013 Cross-verified: Analyzer output matched spec against actual implementation
## Current
COMPLETE
## Feature Boundary
- Entry point: Bottom Nav "종목 분석" → StockAnalysisScreen → Tab 3 (FINANCIAL) → FinancialScreen
- Deep link: stockapp://stock/{ticker}/financial → NavGraph → StockAnalysisScreen(initialTab=3)
- Screens: FinancialScreen, ProfitabilityContent, StabilityContent
- ViewModels: FinancialVm (HiltViewModel)
- UseCases: GetFinancialSummaryUC
- Repositories: FinancialRepo (interface), FinancialRepoImpl (@Singleton)
- API endpoints: 5 KIS APIs (FHKST66430100/200/400/600/800) + OAuth2 tokenP
- DB tables: financial_cache (FinancialCacheEntity, PK=ticker, 24h TTL)
## Dependency Map
- External libs: Kotlin 2.1.0, Compose BOM 2024.12.01, Hilt 2.54, Room 2.8.3, OkHttp 4.12.0, MPAndroidChart v3.1.0, Kotlinx Serialization 1.7.1, Coroutines 1.10.2
- Shared modules: core/state/SelectedStockManager, core/db/AppDb, core/config/AppConfig, core/di/{AppModule,DbModule}, core/ui/component/{ErrorCard,chart/*}, core/theme/ThemeToggleButton, feature/settings/{SettingsRepo,KisApiKeyConfig}, feature/search/domain/model/Stock
## Business Logic Notes
1. YTD-to-Quarterly: convertYtdToQuarterly() converts KIS cumulative values to standalone quarterly (Q2=Q2YTD-Q1YTD)
2. Data Merging: 5 parallel API calls merged by stac_yymm (settlement period) using coroutineScope+async
3. Stability Evaluation: debtRatio (<100 양호, <200 보통, >=200 주의), currentRatio (>=200 양호, >=100 보통, <100 주의), borrowingDependency (<30 양호, <50 보통, >=50 주의)
4. OAuth2 Token: client_credentials grant, 23h cache with Mutex, baseUrl-aware invalidation
5. Cache: 24h TTL (FINANCIAL_CACHE_TTL_MS), JSON serialization via FinancialDataCache, Room storage
6. Partial failure tolerance: Individual API failure returns emptyList(), other data still displays
## Deliverables
- MIGRATION_SPEC.md: 1844 lines, 12 sections, 2 mermaid diagrams, 55 migration tasks
- FILE_MANIFEST.md: 27 files documented with layer, role, dependencies, dependency flow diagram

---
LOOP_COMPLETE
