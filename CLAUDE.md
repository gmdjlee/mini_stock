# CLAUDE.md - Stock Analyzer Project

## Project Overview

KRX 직접 데이터 + 키움/KIS REST API를 활용한 주식 분석 도구. Android 앱(StockApp)이 현재 활성 개발 대상입니다.

## ⚠️ Development Scope

| Component | Status | Note |
|-----------|--------|------|
| **Python (stock-analyzer)** | 🔒 **FROZEN** | 개발 완료, 변경 대상 아님 |
| **Android (StockApp)** | 🚀 **ACTIVE** | 현재 개발/개선 대상 |
| **Kotlin Native Migration** | ✅ **COMPLETED** | Python → Kotlin 전환 완료 |
| **kotlin_krx Integration** | ✅ **COMPLETED** | KRX 직접 데이터 수집 통합 완료 |

**중요**: Python 패키지는 참조용입니다. 모든 개발은 Android 앱(StockApp)에만 적용됩니다.

---

## 🔗 kotlin_krx Integration (KRX Direct Data)

**소스**: `D:\android_2025\kotlin_krx` (Gradle 서브모듈 `:krxkt`)

pykrx Python 라이브러리의 Kotlin 구현체. 한국거래소(KRX)에서 직접 데이터를 수집하며 API 키 불필요.

### 데이터 소스 전략

| 데이터 유형 | Primary | Fallback | 근거 |
|-------------|---------|----------|------|
| **배치 데이터** (종목리스트, OHLCV, 투자자매매, ETF) | KRX (kotlin_krx) | Kiwoom/KIS API | API 키 불필요, 무료, 데이터 풍부 |
| **실시간 데이터** (실시간 수급, 순위정보) | Kiwoom API | - | KRX는 실시간 미지원 |
| **재무정보** | KIS API | - | KRX에서 미제공 |

### KRX-First 적용 범위

| 기능 | KRX 사용 | 파일 | 비고 |
|------|----------|------|------|
| 종목 검색 (getAll) | ✅ | `NativeSearchRepoImpl.kt` | KRX ticker list → Stock 매핑 |
| OHLCV (일봉) | ✅ | `OhlcvService.kt` | 모든 지표 기능에 공통 적용 |
| 투자자별 매매 | ✅ | `NativeAnalysisRepoImpl.kt` | KRX investor trading → InvestorTrend |
| ETF 목록 | ✅ | `EtfCollectorRepoImpl.kt` | KRX ETF ticker list |
| 순위정보 | ❌ | `RankingRepoImpl.kt` | Kiwoom 실시간 API 유지 |
| 재무정보 | ❌ | `FinancialRepoImpl.kt` | KIS API 유지 (KRX 미제공) |
| 실시간 수급 | ❌ | `NativeRealtimeSupplyRepoImpl.kt` | Kiwoom ka10063 유지 |

### Feature Flag

| Flag | 기본값 | 설명 |
|------|--------|------|
| `USE_KRX_DATA_SOURCE` | `true` | KRX-First 전략 활성화/비활성화 |

### 핵심 파일
- `core/krx/KrxDataSource.kt` - KRX API 래퍼 (Hilt Singleton)
- `core/krx/DataSourceStrategy.kt` - KRX_FIRST / BROKER_FIRST 전략 enum
- `core/config/FeatureFlags.kt` - `USE_KRX_DATA_SOURCE` 플래그

### 참고사항
- KRX 데이터 수집은 한국 네트워크/VPN 필요 (`data.krx.co.kr` 접속)
- KRX 실패 시 자동으로 Kiwoom/KIS API 폴백
- 모든 KRX 호출은 `Result<T>` 래핑으로 안전한 에러 핸들링

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

| Flag | 기능 | 구현체 |
|------|------|--------|
| `USE_NATIVE_SEARCH` | 종목 검색 | `NativeSearchRepo` |
| `USE_NATIVE_ANALYSIS` | 수급 분석 | `NativeAnalysisRepo` |
| `USE_NATIVE_INDICATOR` | 기술 지표 | `TrendCalculator`, `ElderCalculator`, `DemarkCalculator` |
| `ENABLE_REALTIME_SUPPLY` | 실시간 수급 | `RealtimeSupplyRepo` |
| `USE_KRX_DATA_SOURCE` | KRX 직접 데이터 | `KrxDataSource` (배치 데이터 Primary) |

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
| Phase 3 | ✅ Done | 시장 지표 (공포/탐욕, 과매수/과매도, 자금 동향, Blood Indicator) |
| Phase 4 | ✅ Done | 설정 화면 (API 키, 투자 모드) |
| Phase 5 | ✅ Done | 자동 스케줄링 (WorkManager) |
| Phase 6 | ✅ Done | 순위정보 (Kotlin REST API) |
| Phase 7 | ✅ Done | 재무정보 (KIS API) |
| Phase 8 | ✅ Done | ETF 분석 |
| Phase 10 | ✅ Done | kotlin_krx 통합 (KRX-First 배치 데이터) |

**코드**: ~165 files, ~31,000 lines (Kotlin)

### 앱 네비게이션 (Bottom Nav 5탭)

| 탭 | 화면 | 기능 |
|----|------|------|
| 📊 종목 분석 | StockAnalysisScreen | 통합 화면 (내부 4탭: 검색, 수급 분석, 기술 지표, 재무정보) |
| 🏆 순위정보 | RankingScreen | 순위정보 |
| 📉 시장 | MarketScreen | 시장 지표 (공포/탐욕, 과매수/과매도, 자금 동향, Blood) |
| 📁 ETF | EtfScreen | ETF 포트폴리오 추적 |
| ⚙️ 설정 | SettingsScreen | API 키, 스케줄링, 시장 지표, DB 백업 |

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
│   ├── db/                   # Room Database (v12, 17 entities)
│   ├── api/                  # Kiwoom/KIS REST API 클라이언트
│   ├── krx/                  # KRX 직접 데이터 (kotlin_krx 래퍼)
│   ├── cache/                # StockCacheManager
│   ├── backup/               # DB 백업/복원
│   └── di/                   # Hilt DI Modules
├── feature/                  # 기능별 모듈
│   ├── stockanalysis/        # 종목 분석 (통합: 검색+수급분석+기술지표+재무정보)
│   ├── search/               # 종목 검색
│   ├── analysis/             # 수급 분석
│   ├── indicator/            # 기술적 지표
│   ├── market/               # 시장 지표 (공포/탐욕, 과매수/과매도, 자금 동향, Blood)
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

### Phase 3: 시장 지표
- **4개 탭**: 공포/탐욕, 과매수/과매도, 자금 동향, Blood Indicator
- **공포/탐욕**: KOSPI 모멘텀, RSI, 변동성, 투자자 수급, 공매도 비율 종합 (KRX 자동 수집)
- **과매수/과매도**: 전체 시장 종목 상승/하락 비율의 5일 EMA 평활화 (KRX 자동 수집)
- **자금 동향**: 외국인/기관/개인 투자자별 순매수/순매도 추이 (KRX 자동 수집)
- **Blood Indicator**: US 3-Month T-Bill Yield / High Yield Spread (Yahoo Finance + FRED API 키 필요)
- 데이터 미수집 시 설정 화면으로 유도하는 안내 카드 표시

### Phase 4: 설정
- **탭 구조**: 키움 API, KIS API, 스케줄링, ETF 통계, 시장 지표, DB 백업, 고급
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

### Room Database (v12, 17 entities)

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
| `MarketIndicatorCacheEntity` | 시장 지표 캐시 |

### 캐시 정책
- 종목/수급/지표/재무/시장지표: 24시간 TTL
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
| kotlin_krx | 1.0.0-SNAPSHOT | KRX 직접 데이터 수집 (배치 Primary) |

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
