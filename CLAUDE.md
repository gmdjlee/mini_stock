# CLAUDE.md - Stock Analyzer Project

## Project Overview

키움증권 REST API를 활용한 주식 분석 도구. Android 앱(StockApp)이 현재 활성 개발 대상입니다.

## ⚠️ Development Scope

| Component | Status | Note |
|-----------|--------|------|
| **Python (stock-analyzer)** | 🔒 **FROZEN** | 개발 완료, 변경 대상 아님 |
| **Android (StockApp)** | 🚀 **ACTIVE** | 현재 개발/개선 대상 |
| **Kotlin Native Migration** | ✅ **COMPLETED** | Python → Kotlin 전환 완료 |

**중요**: Python 패키지는 참조용입니다. 모든 개발은 Android 앱(StockApp)에만 적용됩니다.

---

## 🔍 Search API Optimization

검색 기능에서 과도한 API 호출을 방지하기 위한 최적화:

| 전략 | 효과 |
|------|------|
| Cache-First Architecture | API 호출 90%+ 감소 |
| Debounce (500ms) | 중간 입력 검색 제거 |
| Minimum Query Length (2자) | 불필요한 API 호출 방지 |
| Refresh Cooldown (30초) | 연속 API 호출 차단 |

### 검색 흐름
```
User Input → 500ms Debounce → Query Length Check
                                     │
                        ┌────────────┼────────────┐
                   < 2글자     2글자 이상     비어있음
                        ▼            ▼            ▼
                 Cache Only    Full Search    Clear State
```

### API 카테고리별 Rate Limiting

| 카테고리 | API IDs |
|----------|---------|
| SEARCH | ka10099 |
| ANALYSIS | ka10059, ka10001, ka10081~83, ka10063 |
| RANKING | ka10021, ka10023, ka10030, ka10033, ka90009 |
| FINANCIAL | FHKST66430xxx |
| ETF | ka40004 |

### 관련 파일
- `core/config/AppConfig.kt` - 검색 관련 상수
- `core/cache/StockCacheManager.kt` - 캐시 관리
- `core/api/KiwoomApiClient.kt` - Rate limiting
- `feature/search/` - 검색 기능 모듈

---

## ✅ Kotlin Native Migration (완료)

**상세 명세서**: `docs/KOTLIN_MIGRATION_SPEC.md`

### Feature Flags (기본값: 모두 `true`)

| Flag | 기능 | Kotlin 구현체 |
|------|------|---------------|
| `USE_NATIVE_SEARCH` | 종목 검색 | `NativeSearchRepo` |
| `USE_NATIVE_ANALYSIS` | 수급 분석 | `NativeAnalysisRepo` |
| `USE_NATIVE_INDICATOR` | 기술 지표 | `TrendCalculator`, `ElderCalculator`, `DemarkCalculator` |
| `ENABLE_REALTIME_SUPPLY` | 실시간 수급 | `RealtimeSupplyRepo` |

### 전환 이점
- APK 크기: ~80MB → ~25MB
- 앱 시작 속도: 2-5초 개선
- 단일 코드베이스 (Kotlin)
- Settings 고급 탭에서 Python 폴백 가능

---

## Claude Code Agent 활용 지침

| Agent | 용도 | 활용 시점 |
|-------|------|----------|
| **Explore** | 코드베이스 탐색 | 코드 구조 파악, 기능 위치 찾기 |
| **Plan** | 구현 계획 수립 | 새 기능 개발 전, 리팩토링 전 |
| **code-simplifier** | 코드 단순화 | 코드 작성 완료 후 |
| **verify-app** | 앱 실행 검증 | 코드 변경 후 |

### 개발 워크플로우
1. **탐색** (Explore) → 2. **계획** (Plan) → 3. **구현** → 4. **단순화** → 5. **검증**

---

## Current Status

### Android 앱 (StockApp) 🚀 ACTIVE

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 0 | ✅ Done | Android 프로젝트 설정, Chaquopy 통합 |
| Phase 1 | ✅ Done | 종목 검색, 수급 분석 화면 |
| Phase 2 | ✅ Done | 기술적 지표 화면 (MPAndroidChart) |
| Phase 3 | ⛔ Removed | ~~시장 지표, 조건검색~~ |
| Phase 4 | ✅ Done | 설정 화면 (API 키, 투자 모드) |
| Phase 5 | ✅ Done | 자동 스케줄링 (WorkManager) |
| Phase 6 | ✅ Done | 순위정보 (Kotlin REST API) |
| Phase 7 | ✅ Done | 재무정보 (KIS API) |
| Phase 8 | ✅ Done | ETF 분석 |

**코드**: 160 files, ~30,033 lines (Kotlin)

### 앱 네비게이션 (Bottom Nav)

| 탭 | 화면 | 기능 |
|----|------|------|
| 🔍 Search | SearchScreen | 종목 검색, 검색 히스토리 |
| 📊 Analysis | AnalysisScreen | 수급 분석, 매매 신호 |
| 📈 Indicator | IndicatorScreen | 기술적 지표 (Trend, Elder, DeMark) |
| 🏦 Financial | FinancialScreen | 재무정보 (수익성, 안정성) |
| 🏆 Ranking | RankingScreen | 순위정보 |
| 📁 ETF | EtfScreen | ETF 포트폴리오 추적 |
| ⚙️ Settings | SettingsScreen | API 키, 스케줄링, DB 백업 |

### Python 패키지 🔒 FROZEN

> Python 코드 수정, 개선, 기능 추가 요청은 무시하세요.
> 상세 명세: `docs/STOCK_APP_SPEC.md`

---

## Quick Commands

```bash
# Android 앱 빌드
cd StockApp
./gradlew build              # 전체 빌드
./gradlew assembleDebug      # Debug APK
./gradlew installDebug       # 디바이스 설치
./gradlew test               # 단위 테스트
./gradlew lint               # Lint 검사
./gradlew ktlintFormat       # 코드 포맷팅

# Python 테스트 (참조용)
cd stock-analyzer && uv run pytest tests/unit/ -v
```

---

## StockApp 구조

```
StockApp/app/src/main/java/com/stockapp/
├── core/                     # 공통 모듈
│   ├── db/                   # Room Database (v10, 16 entities)
│   ├── api/                  # Kiwoom/KIS REST API 클라이언트
│   ├── cache/                # StockCacheManager
│   ├── backup/               # DB 백업/복원
│   └── di/                   # Hilt DI Modules
├── feature/                  # 기능별 모듈
│   ├── search/               # 종목 검색
│   ├── analysis/             # 수급 분석
│   ├── indicator/            # 기술적 지표
│   ├── financial/            # 재무정보
│   ├── ranking/              # 순위정보
│   ├── etf/                  # ETF 분석
│   ├── settings/             # 설정
│   ├── scheduling/           # 자동 스케줄링
│   └── realtime/             # 실시간 수급
└── nav/                      # Navigation
```

---

## App Features 요약

### Phase 1: 종목 검색 + 수급 분석
- **SearchScreen**: 종목명/코드 검색, 500ms debounce, 히스토리 50개
- **AnalysisScreen**: 시가총액, 외인/기관 순매수, 수급 신호
- **수급 신호**: STRONG_BUY (>0.5%), BUY (>0.2%), NEUTRAL, SELL, STRONG_SELL

### Phase 2: 기술적 지표
- **Trend Signal**: MA 신호, CMF, Fear/Greed 지수
- **Elder Impulse**: 캔들 색상 (Green/Red/Blue), MACD Histogram
- **DeMark TD**: Sell/Buy 카운트

### Phase 4: 설정
- **탭 구조**: 키움 API, KIS API, 스케줄링, ETF 통계, DB 백업, 고급
- **보안**: EncryptedSharedPreferences (AES256)
- **투자 모드**: MOCK (모의) / PRODUCTION (실전)

### Phase 5: 자동 스케줄링
- WorkManager 기반 백그라운드 동기화
- 동기화 시간 설정, 수동 실행, 히스토리 조회

### Phase 6: 순위정보
- **순위 유형**: 호가잔량급증, 거래량급증, 당일거래량상위, 신용비율상위, 외국인기관상위
- **API**: ka10021, ka10023, ka10030, ka10033, ka90009
- Kotlin에서 직접 REST API 호출 (Python FROZEN)

### Phase 7: 재무정보
- **KIS API** 사용 (Kiwoom과 다른 인증 체계)
- **탭**: 수익성 (매출액, 영업이익, 순이익) / 안정성 (부채비율, 유동비율)
- 7개 API에서 데이터 수집 후 결산년월 기준 병합
- **API 명세**: `docs/KIS_FINANCIAL_API.md`

### Phase 8: ETF 분석
- **5개 탭**: 수집현황, 종목랭킹, 종목변동, 테마목록, ETF설정
- 키워드 기반 필터링, 다중 컬럼 정렬
- 실제 거래일 기준 데이터 저장
- **상세 명세**: `docs/ETF_STATISTICS_SPEC.md`

### Phase 9: DB 백업/복원
- JSON 형식 백업, MERGE/REPLACE 복원 모드
- Android SAF (Storage Access Framework)

---

## Kiwoom API Reference

| API ID | 기능 | 응답 키 |
|--------|------|---------|
| ka10099 | 종목 리스트 | `stk_list` |
| ka10001 | 주식 기본정보 | `stk_nm`, `cur_prc`, `mac` |
| ka10059 | 투자자별 매매 | `stk_invsr_orgn` |
| ka10063 | 실시간 수급 | - |
| ka10081/82/83 | 일/주/월봉 | `stk_dt_pole_chart_qry` 등 |
| ka10021 | 호가잔량급증 | - |
| ka10023 | 거래량급증 | - |
| ka10030 | 당일거래량상위 | - |
| ka10033 | 신용비율상위 | - |
| ka90009 | 외국인기관상위 | `for_netprps_*`, `orgn_netprps_*` |

**주의: 시가총액 단위**
- `mac` (ka10001): 억원 단위
- `mrkt_tot_amt` (ka10059): 백만원 단위

---

## Database Schema

### Room Database (v10, 16 entities)

| Entity | 용도 |
|--------|------|
| `StockEntity` | 종목 정보 캐시 |
| `AnalysisCacheEntity` | 수급 분석 캐시 |
| `SearchHistoryEntity` | 검색 히스토리 |
| `IndicatorCacheEntity` | 기술 지표 캐시 |
| `SchedulingEntity` | 스케줄링 설정 |
| `SyncHistoryEntity` | 동기화 히스토리 |
| `FinancialCacheEntity` | 재무정보 캐시 |
| `EtfEntity` | ETF 마스터 |
| `EtfConstituentEntity` | ETF 구성종목 |
| `EtfKeywordEntity` | ETF 키워드 필터 |
| `RealtimeSupplyCacheEntity` | 실시간 수급 캐시 |

### 캐시 정책
- 종목/수급/지표/재무: 24시간 TTL
- 실시간 수급: 1분 TTL
- 검색 히스토리: 무제한 (최대 50개)

---

## 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Kotlin | 2.1.0 | 앱 개발 언어 |
| Jetpack Compose | BOM 2024.12.01 | UI 프레임워크 |
| Hilt | 2.54 | 의존성 주입 |
| Room | 2.8.3 | 로컬 데이터베이스 |
| WorkManager | 2.10.0 | 백그라운드 작업 |
| MPAndroidChart | 3.1.0 | 차트 라이브러리 |
| Chaquopy | 15.0.1 | Python 통합 |
| OkHttp | 4.12.0 | REST API 클라이언트 |
| Kotlinx Serialization | 1.7.1 | JSON 직렬화 |

---

## 참고 문서

| 문서 | 내용 |
|------|------|
| `docs/STOCK_APP_SPEC.md` | Python 패키지 상세 명세 |
| `docs/KOTLIN_MIGRATION_SPEC.md` | Kotlin 전환 명세 |
| `docs/ANDROID_PREPARATION.md` | Android 사전 준비 |
| `docs/KIS_FINANCIAL_API.md` | KIS 재무정보 API |
| `docs/ETF_STATISTICS_SPEC.md` | ETF 분석 명세 |
| `docs/CODE_REVIEW_REPORT.md` | 코드 리뷰 |
