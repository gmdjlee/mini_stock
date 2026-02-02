# CLAUDE.md - Stock Analyzer Project

## Project Overview

키움증권 REST API를 활용한 주식 분석 도구. Python으로 데이터 수집/분석 로직을 검증한 후 Android 앱으로 통합.

## ⚠️ Development Scope

| Component | Status | Note |
|-----------|--------|------|
| **Python (stock-analyzer)** | 🔒 **FROZEN** | 개발 완료, 변경/개선 대상 아님 |
| **Android (StockApp)** | 🚀 **ACTIVE** | 현재 개발/개선 대상 |
| **Kotlin Native Migration** | 📋 **PLANNED** | Python → Kotlin 전환 예정 |

**중요**: Python 패키지는 참조용으로만 사용합니다. 향후 모든 개발, 개선, 버그 수정은 Android 앱(StockApp)에만 적용됩니다.

---

## 🔄 Kotlin Native Migration (계획됨)

### 개요

Python (Chaquopy) 기반 기능을 순수 Kotlin으로 전환하는 프로젝트입니다.

**상세 명세서**: `docs/KOTLIN_MIGRATION_SPEC.md`

### 전환 대상

| 기능 | 현재 (Python) | 전환 후 (Kotlin) | 상태 |
|------|---------------|------------------|------|
| 종목 검색 | `stock/search.py` | `NativeSearchRepo` | 📋 계획됨 |
| 수급 분석 | `stock/analysis.py` | `NativeAnalysisRepo` | 📋 계획됨 |
| OHLCV 조회 | `stock/ohlcv.py` | `OhlcvService` | 📋 계획됨 |
| Trend Signal | `indicator/trend.py` | `TrendCalculator` | 📋 계획됨 |
| Elder Impulse | `indicator/elder.py` | `ElderCalculator` | 📋 계획됨 |
| DeMark TD | `indicator/demark.py` | `DemarkCalculator` | 📋 계획됨 |
| **실시간 수급** (신규) | - | `RealtimeSupplyRepo` | 📋 계획됨 |

### 전환 이점

| 항목 | Python (현재) | Kotlin (전환 후) |
|------|---------------|------------------|
| APK 크기 | ~80-100MB | ~15-25MB |
| 앱 시작 속도 | Python 초기화 2-5초 | 즉시 |
| 코드베이스 | 2개 언어 | 1개 언어 |
| 디버깅 | 제한적 | 완전 지원 |

### 구현 일정 (예정)

| Phase | 기간 | 내용 |
|-------|------|------|
| Phase 1 | 2일 | 핵심 인프라 (MathUtil, FeatureFlags) |
| Phase 2 | 2일 | 종목 검색 전환 |
| Phase 3 | 4일 | OHLCV, 수급 분석 전환 |
| Phase 4 | 6일 | 기술 지표 전환 |
| Phase 5 | 3일 | 실시간 수급 기능 (신규) |
| Phase 6 | 3일 | 통합 테스트 |
| Phase 7 | 1일 | 문서화, 정리 |

**총 예상 기간**: 21일 (3주)

---

## Claude Code Agent 활용 지침

개발 작업 시 다음 Agent들을 **적극적으로 활용**하세요:

### 필수 활용 Agent

| Agent | 용도 | 활용 시점 |
|-------|------|----------|
| **Explore** | 코드베이스 탐색, 파일 검색 | 코드 구조 파악, 기능 위치 찾기 |
| **Plan** | 구현 계획 수립, 아키텍처 설계 | 새 기능 개발 전, 리팩토링 전 |
| **code-simplifier** | 코드 단순화, 정리 | 코드 작성 완료 후 |
| **verify-app** | 앱 실행 및 품질 검증 | 코드 변경 후 |

### Agent 활용 예시

```
# 코드베이스 탐색 시
Task(subagent_type="Explore", prompt="Find all files related to stock scheduling")

# 구현 계획 수립 시
Task(subagent_type="Plan", prompt="Plan implementation for new notification feature")

# 코드 작성 후 단순화
Task(subagent_type="code-simplifier", prompt="Simplify the recently added code")

# 앱 검증
Task(subagent_type="verify-app", prompt="Run the app and verify scheduling feature works")
```

### 개발 워크플로우

1. **탐색** (Explore): 관련 코드 위치 및 패턴 파악
2. **계획** (Plan): 구현 전략 수립 (복잡한 작업 시)
3. **구현**: 코드 작성
4. **단순화** (code-simplifier): 불필요한 복잡성 제거
5. **검증** (verify-app): 앱 실행하여 동작 확인

---

## Current Status

### Python 패키지 (stock-analyzer) 🔒 FROZEN

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 0 | ✅ Done | 프로젝트 설정, 키움 API 클라이언트 |
| Phase 1 | ✅ Done | 종목 검색, 수급 분석, OHLCV |
| Phase 2 | ✅ Done | 기술적 지표 (Trend, Elder, DeMark) |
| Phase 3 | ✅ Done | 차트 시각화 (Candle, Line, Bar) |
| Phase 4 | ✅ Done | 조건검색, 시장 지표 |
| Phase 5 | ✅ Done | 시가총액 & 수급 오실레이터 |

**테스트**: 168개 (모두 통과)
**코드**: ~6,200 lines (29 Python 파일)
**코드 품질**: 8.5/10 (리뷰 보고서: `docs/CODE_REVIEW_REPORT.md`)

> ⚠️ **이 프로젝트는 동결(frozen) 상태입니다.** Python 코드에 대한 수정, 개선, 기능 추가 요청은 무시하세요. Android 앱 개발을 위한 참조 및 Chaquopy 통합 용도로만 사용됩니다.

### Android 앱 (StockApp) 🚀 ACTIVE

| Phase | Status | Description |
|-------|--------|-------------|
| App Phase 0 | ✅ Done | Android 프로젝트 설정, Chaquopy 통합 |
| App Phase 1 | ✅ Done | 종목 검색, 수급 분석 화면 |
| App Phase 2 | ✅ Done | 기술적 지표 화면 (MPAndroidChart) |
| App Phase 3 | ⛔ Removed | ~~시장 지표, 조건검색 화면~~ (제거됨) |
| App Phase 4 | ✅ Done | **설정 화면 (API 키 관리, 투자 모드)** |
| App Phase 5 | ✅ Done | **자동 스케줄링 (WorkManager 기반)** |
| App Phase 6 | ✅ Done | **순위정보 (Ranking) - Kotlin REST API 직접 호출** |
| App Phase 7 | ✅ Done | **재무정보 (Financial) - KIS API 직접 호출** |
| App Phase 8 | ✅ Done | **ETF 분석 - ETF 포트폴리오 추적 및 분석** |

**코드**: 160 files, ~30,033 lines (Kotlin)
**코드 품질**: 7.5/10 (테스트 부재, 보안/스레드안전성 이슈 발견)
**사전 준비 문서**: `docs/ANDROID_PREPARATION.md`

> 🚀 **이 프로젝트가 현재 활성 개발 대상입니다.** 모든 기능 추가, 버그 수정, 개선 작업은 여기에 적용됩니다.

### 현재 앱 네비게이션 (Bottom Nav)

| 탭 | 화면 | 기능 |
|----|------|------|
| 🔍 Search | SearchScreen | 종목 검색, 검색 히스토리 |
| 📊 Analysis | AnalysisScreen | 수급 분석, 매매 신호 |
| 📈 Indicator | IndicatorScreen | 기술적 지표 (Trend, Elder, DeMark) |
| 🏦 Financial | FinancialScreen | 재무정보 (수익성, 안정성) |
| 🏆 Ranking | RankingScreen | 순위정보 (호가잔량, 거래량, 신용비율 등) |
| 📁 ETF | EtfScreen | ETF 포트폴리오 추적 및 분석 |
| ⚙️ Settings | SettingsScreen | API 키 설정, 스케줄링 설정, ETF 키워드, DB 백업/복원 |

## Quick Commands

```bash
# Android 앱 빌드 (주요 명령어)
cd StockApp
./gradlew build              # 전체 빌드
./gradlew assembleDebug      # Debug APK 빌드
./gradlew installDebug       # 디바이스에 설치
./gradlew test               # 단위 테스트
./gradlew lint               # Lint 검사
./gradlew ktlintCheck        # Kotlin 코드 스타일 검사
./gradlew ktlintFormat       # Kotlin 코드 포맷팅

# Release 빌드
./gradlew assembleRelease    # Release APK 빌드

# 의존성 확인
./gradlew dependencies       # 전체 의존성 트리
./gradlew app:dependencies   # 앱 모듈 의존성

# Python 테스트 (참조용 - 수정 불필요)
cd stock-analyzer
uv sync --all-extras
uv run pytest tests/unit/ -v
```

## File Locations

```
stock-analyzer/
├── src/stock_analyzer/
│   ├── config.py           # 설정 (API 키, 상수)
│   ├── core/               # 공통 유틸 (log, http, date, json)
│   ├── client/
│   │   ├── auth.py         # OAuth 토큰 관리
│   │   └── kiwoom.py       # 키움 REST API 클라이언트
│   ├── stock/
│   │   ├── search.py       # 종목 검색
│   │   ├── analysis.py     # 수급 분석
│   │   └── ohlcv.py        # 가격 데이터
│   ├── indicator/          # 기술적 지표
│   │   ├── trend.py        # Trend Signal (MA, CMF, Fear/Greed)
│   │   ├── elder.py        # Elder Impulse (EMA13, MACD)
│   │   ├── demark.py       # DeMark TD Setup
│   │   └── oscillator.py   # 수급 오실레이터 (Phase 5)
│   ├── chart/              # 차트 시각화
│   │   ├── candle.py       # 캔들스틱 차트
│   │   ├── line.py         # 라인 차트
│   │   ├── bar.py          # 바 차트
│   │   └── oscillator.py   # 오실레이터 차트 (Phase 5)
│   ├── market/             # 시장 지표
│   │   └── deposit.py      # 예탁금, 신용잔고
│   └── search/             # 조건검색
│       └── condition.py    # HTS 조건검색
├── tests/
│   ├── unit/               # 단위 테스트 (127개)
│   ├── integration/        # 통합 테스트
│   └── e2e/                # E2E 테스트
└── scripts/
    └── run_analysis.py     # 샘플 스크립트
```

## Common Patterns

### API 응답 규격
```python
# 성공
{"ok": True, "data": {...}}

# 에러
{"ok": False, "error": {"code": "ERROR_CODE", "msg": "메시지"}}
```

### 에러 코드
| Code | Description |
|------|-------------|
| `INVALID_ARG` | 잘못된 인자 |
| `TICKER_NOT_FOUND` | 종목 없음 |
| `NO_DATA` | 데이터 없음 |
| `API_ERROR` | 외부 API 오류 |
| `AUTH_ERROR` | 인증 실패 |
| `NETWORK_ERROR` | 네트워크 오류 |
| `CHART_ERROR` | 차트 생성 실패 |
| `CONDITION_NOT_FOUND` | 조건검색 없음 |
| `INSUFFICIENT_DATA` | 데이터 부족 (오실레이터 계산용) |

### 함수 호출 예시
```python
from stock_analyzer.client.kiwoom import KiwoomClient
from stock_analyzer.stock import search, analysis, ohlcv
from stock_analyzer.indicator import trend, elder, demark, oscillator
from stock_analyzer.chart import candle, line, bar
from stock_analyzer.chart import oscillator as osc_chart  # Phase 5
from stock_analyzer.market import deposit
from stock_analyzer.search import condition

# 클라이언트 생성
client = KiwoomClient(app_key, secret_key, base_url)

# 종목 검색
result = search.search(client, "삼성전자")

# 수급 분석
result = analysis.analyze(client, "005930", days=180)

# OHLCV 데이터
result = ohlcv.get_daily(client, "005930", days=30)

# 기술적 지표
result = trend.calc(client, "005930", days=180)   # Trend Signal
result = elder.calc(client, "005930", days=180)   # Elder Impulse
result = demark.calc(client, "005930", days=180)  # DeMark TD

# 차트 생성
result = candle.plot_from_ohlcv(ohlcv_data)       # 캔들스틱 차트
result = line.plot_trend(trend_data)              # 트렌드 시그널 차트
result = bar.plot_supply_demand(analysis_data)    # 수급 분석 차트

# 시장 지표 (Phase 4)
result = deposit.get_deposit(client, days=30)           # 예탁금 추이
result = deposit.get_credit(client, days=30)            # 신용잔고 추이
result = deposit.get_market_indicators(client, days=30) # 통합 시장 지표

# 조건검색 (Phase 4)
result = condition.get_list(client)                     # 조건검색 목록
result = condition.search(client, "000", "골든크로스")   # 조건검색 실행
result = condition.search_by_idx(client, "000")         # 인덱스로 조건검색

# 수급 오실레이터 (Phase 5 - Pending)
result = oscillator.calc(client, "005930", days=180)    # 오실레이터 계산
signal = oscillator.analyze_signal(result)              # 매매 신호 분석
result = osc_chart.plot(osc_data)                       # 오실레이터 차트
```

## Kiwoom API Reference

| API ID | 기능 | 모듈 | 응답 필드 |
|--------|------|------|----------|
| au10001 | 토큰 발급 | client/auth.py | `token`, `expires_dt` |
| ka10099 | 종목 리스트 | stock/search.py | `stk_list` |
| ka10001 | 주식 기본정보 | stock/search.py | `stk_nm`, `cur_prc`, `mac` |
| ka10008 | 외국인 매매동향 | stock/analysis.py | - |
| ka10059 | 투자자별 매매 | stock/analysis.py | `stk_invsr_orgn` |
| ka10081 | 일봉 차트 | stock/ohlcv.py, indicator/* | `stk_dt_pole_chart_qry` |
| ka10082 | 주봉 차트 | stock/ohlcv.py | `stk_stk_pole_chart_qry` |
| ka10083 | 월봉 차트 | stock/ohlcv.py | `stk_mth_pole_chart_qry` |
| ka10171 | 조건검색 목록 | search/condition.py | `cond_list` |
| ka10172 | 조건검색 실행 | search/condition.py | `stk_list` |
| kt00001 | 예탁금 추이 | market/deposit.py | `deposit_list` |
| ka10013 | 신용잔고 추이 | market/deposit.py | `credit_list` |
| ka10021 | 호가잔량급증 | ranking (Kotlin) | `stk_cd_list`, `stk_nm_list` 등 |
| ka10023 | 거래량급증 | ranking (Kotlin) | `stk_cd_list`, `stk_nm_list` 등 |
| ka10030 | 당일거래량상위 | ranking (Kotlin) | `stk_cd_list`, `stk_nm_list` 등 |
| ka10033 | 신용비율상위 | ranking (Kotlin) | `stk_cd_list`, `stk_nm_list` 등 |
| ka90009 | 외국인기관상위 | ranking (Kotlin) | `for_netprps_*`, `orgn_netprps_*` |

### API 응답 필드명 (실제 API 기준)

**차트 API (ka10081/82/83)**
```python
# 응답 구조
{
    "stk_dt_pole_chart_qry": [  # 일봉: stk_dt_pole_chart_qry
        {                       # 주봉: stk_stk_pole_chart_qry
            "dt": "20260114",   # 월봉: stk_mth_pole_chart_qry
            "open_pric": 137000,
            "high_pric": 140300,
            "low_pric": 136800,
            "cur_prc": 140300,   # 종가 (close)
            "trde_qty": 18444394 # 거래량 (volume)
        }
    ]
}
```

**주식 기본정보 API (ka10001)**
```python
# 응답 구조
{
    "stk_nm": "삼성전자",
    "cur_prc": 55000,
    "mac": 3800000  # 시가총액 (억원 단위) - 380조원 = 3,800,000억원
}
```

**투자자별 매매 API (ka10059)**
```python
# 응답 구조
{
    "stk_invsr_orgn": [
        {
            "dt": "20260114",
            "frgnr_invsr": 23987,       # 외국인 순매수
            "orgn": 264048,              # 기관 순매수
            "ind_invsr": -496193,        # 개인 순매수
            "mrkt_tot_amt": 380000000    # 시가총액 (백만원 단위) - 380조원 = 380,000,000백만원
        }
    ]
}
```

**주의: 시가총액 단위 차이**
- `mac` (ka10001): 억원 단위 (100,000,000원)
- `mrkt_tot_amt` (ka10059): 백만원 단위 (1,000,000원)

## Technical Indicators (Phase 2)

### Trend Signal (`indicator/trend.py`)
MA, CMF, Fear/Greed를 조합한 추세 신호
- `ma_signal`: MA 정배열/역배열 (1: 상승, 0: 중립, -1: 하락)
- `cmf`: Chaikin Money Flow (-1 ~ 1)
- `fear_greed`: 공포/탐욕 지수 (약 -1 ~ 1.5)
  - 구성요소: Momentum5(45%) + Pos52(45%) + VolSurge(5%) + VolSpike(5%)
  - 임계값: >0.5 탐욕(상승 과열), <-0.5 공포(하락 과열)
- `trend`: 종합 추세 ("bullish", "neutral", "bearish")

### Elder Impulse (`indicator/elder.py`)
EMA13과 MACD 히스토그램을 이용한 캔들 색상 결정
- `color`: 캔들 색상 ("green", "red", "blue")
- `ema13`: 13일 지수이동평균
- `macd_hist`: MACD 히스토그램

### DeMark TD (`indicator/demark.py`)
커스텀 TD Setup (레퍼런스 기반)
- `sell_setup`: Sell 카운트 (4일 전 비교, 상승 피로 측정, 무제한)
- `buy_setup`: Buy 카운트 (2일 전 비교, 하락 피로 측정, 무제한)
- Sell과 Buy는 독립적으로 계산 (동시에 값이 있을 수 있음)

## Chart Visualization (Phase 3)

### Candlestick Chart (`chart/candle.py`)
OHLCV 데이터로 캔들스틱 차트 생성
- `plot()`: 기본 캔들스틱 차트 (MA 오버레이, Elder 색상 지원)
- `plot_from_ohlcv()`: OHLCV 결과 딕셔너리로 차트 생성
- 옵션: 거래량 서브플롯, MA 라인 오버레이, Elder Impulse 색상

### Line Chart (`chart/line.py`)
라인 차트 및 지표 시각화
- `plot()`: 다중 시리즈 라인 차트
- `plot_trend()`: Trend Signal 멀티패널 차트 (MA, CMF, Fear/Greed)
- `plot_elder()`: Elder Impulse 차트 (EMA13, MACD Histogram)

### Bar Chart (`chart/bar.py`)
바 차트 및 수급 분석 시각화
- `plot()`: 단일 시리즈 바 차트 (색상별 부호 지원)
- `plot_multi()`: 그룹/스택 바 차트
- `plot_supply_demand()`: 수급 분석 차트 (시가총액, 외인/기관 순매수)
- `plot_demark()`: DeMark TD Setup 바 차트

### 차트 출력
```python
# 차트 결과
result = candle.plot_from_ohlcv(ohlcv_data, save_path="/tmp/chart.png")
if result["ok"]:
    image_bytes = result["data"]["image_bytes"]  # PNG 바이트
    saved_path = result["data"]["save_path"]     # 저장된 파일 경로
```

## Market Indicators (Phase 4)

### Deposit (`market/deposit.py`)
예탁금 및 신용잔고 추이 조회
- `get_deposit(client, days)`: 고객예탁금, 신용융자 추이
- `get_credit(client, days)`: 신용잔고, 신용비율 추이
- `get_market_indicators(client, days)`: 통합 시장 지표

```python
# 예탁금 데이터
result = deposit.get_deposit(client, days=30)
if result["ok"]:
    dates = result["data"]["dates"]         # ["2025-01-10", ...]
    deposits = result["data"]["deposit"]    # [50000000000000, ...]
    credit_loan = result["data"]["credit_loan"]  # [15000000000000, ...]
```

## Condition Search (Phase 4)

### Condition (`search/condition.py`)
HTS 조건검색 기능
- `get_list(client)`: 조건검색 목록 조회
- `search(client, cond_idx, cond_name)`: 조건검색 실행
- `search_by_idx(client, cond_idx)`: 인덱스로 조건검색 (자동으로 이름 조회)

```python
# 조건검색 목록
result = condition.get_list(client)
if result["ok"]:
    for cond in result["data"]:
        print(f"{cond['idx']}: {cond['name']}")

# 조건검색 실행
result = condition.search(client, "000", "골든크로스")
if result["ok"]:
    for stock in result["data"]["stocks"]:
        print(f"{stock['ticker']}: {stock['name']} ({stock['change']}%)")
```

## Market Cap Oscillator (Phase 5)

### 개요
시가총액과 외국인/기관 수급 데이터를 기반으로 MACD 스타일 오실레이터를 계산하여 매매 신호 생성

### Oscillator (`indicator/oscillator.py`)
수급 기반 오실레이터 계산
- `calc(client, ticker, days)`: 오실레이터 계산
- `analyze_signal(osc_result)`: 매매 신호 분석 (-100 ~ +100 점수)

### 핵심 계산
```python
# Supply Ratio = (외국인 + 기관 순매수) / 시가총액
supply_ratio = (foreign_5d + institution_5d) / market_cap

# MACD 스타일 오실레이터
ema12 = EMA(supply_ratio, 12)
ema26 = EMA(supply_ratio, 26)
macd = ema12 - ema26
signal = EMA(macd, 9)
oscillator = macd - signal  # Histogram
```

### 매매 신호 점수
| 항목 | 점수 | 설명 |
|------|------|------|
| 오실레이터 값 | ±40 | >0.5%: +40, >0.2%: +20 |
| MACD 크로스 | ±30 | 골든크로스: +30, 데드크로스: -30 |
| 히스토그램 추세 | ±30 | 상승 지속: +30, 하락 지속: -30 |

### 신호 유형
| Score | Signal | 설명 |
|-------|--------|------|
| >= 60 | STRONG_BUY | 강력 매수 |
| >= 20 | BUY | 매수 |
| -20 ~ 20 | NEUTRAL | 중립 |
| <= -20 | SELL | 매도 |
| <= -60 | STRONG_SELL | 강력 매도 |

### 사용 예시
```python
from stock_analyzer.indicator import oscillator

# 오실레이터 계산
result = oscillator.calc(client, "005930", days=180)
if result["ok"]:
    data = result["data"]
    print(f"시가총액: {data['market_cap'][-1]:.1f}조")
    print(f"오실레이터: {data['oscillator'][-1]:.6f}")

# 매매 신호 분석
signal = oscillator.analyze_signal(result)
if signal["ok"]:
    print(f"점수: {signal['data']['total_score']}")
    print(f"신호: {signal['data']['signal_type']}")
    print(f"설명: {signal['data']['description']}")
```

### 차트 (`chart/oscillator.py`)
- `plot(osc_data)`: 듀얼 축 차트 (시가총액 + 오실레이터)

## Environment Setup

```bash
# .env 파일 생성
cp stock-analyzer/.env.example stock-analyzer/.env

# API 키 설정
KIWOOM_APP_KEY=your_app_key
KIWOOM_SECRET_KEY=your_secret_key
KIWOOM_BASE_URL=https://api.kiwoom.com
```

## Development Notes

- Python 3.10+ 필요
- [uv](https://github.com/astral-sh/uv) 패키지 매니저 권장 (pip 대비 10-100배 빠름)
- 모든 함수는 `{"ok": bool, "data/error": ...}` 형식 반환
- 토큰은 자동 갱신됨 (AuthClient.get_token)
- 테스트는 mock 클라이언트 사용 (실제 API 호출 없음)

## Spec Document

상세 명세서: `docs/STOCK_APP_SPEC.md`

---

## Android 앱 개발 가이드

### 사전 준비 요약

**상세 문서**: `docs/ANDROID_PREPARATION.md`

#### 핵심 기술 스택
| 기술 | 용도 | 버전 |
|------|------|------|
| Kotlin | 앱 개발 언어 | 2.1.0+ |
| Jetpack Compose | UI 프레임워크 | BOM 2024.12 |
| Chaquopy | Python 통합 | 15.0.1+ |
| Hilt | 의존성 주입 | 2.54 |
| Room | 로컬 DB | 2.8.3 |
| Vico | 차트 라이브러리 | 2.0.0 |

#### Chaquopy 호환성

| 패키지 | 지원 | 앱에서 처리 |
|--------|------|-------------|
| `requests` | ✅ | Python |
| `python-dotenv` | ✅ | Python |
| `numpy` | ⚠️ | Python (wheel 필요) |
| `pandas` | ⚠️ | Python (wheel 필요) |
| `matplotlib` | ❌ | **Vico Charts로 대체** |
| `mplfinance` | ❌ | **Vico Charts로 대체** |

#### Python 모듈 → Android 매핑

```
Python (Android용)           Kotlin (Android)
├── client/kiwoom.py    →   PyClient 호출
├── stock/search.py     →   SearchScreen
├── stock/analysis.py   →   AnalysisScreen
├── stock/ohlcv.py      →   ChartScreen (Vico)
├── indicator/trend.py  →   IndicatorScreen
├── indicator/elder.py  →   IndicatorScreen
├── indicator/demark.py →   IndicatorScreen
├── market/deposit.py   →   MarketScreen
├── search/condition.py →   ConditionScreen
│
└── chart/*             ✗   Vico Charts로 대체
```

#### StockApp 파일 구조

```
StockApp/
├── app/src/main/java/com/stockapp/
│   ├── App.kt                      # Hilt Application
│   ├── MainActivity.kt             # Main Activity
│   ├── core/
│   │   ├── db/                     # Room Database (v9)
│   │   │   ├── AppDb.kt
│   │   │   ├── entity/*.kt         # 15개 Entity (Stock, Analysis, Search, Indicator, Scheduling, ETF 등)
│   │   │   └── dao/*.kt            # 12개 DAO
│   │   ├── py/                     # Python Bridge
│   │   │   ├── PyClient.kt
│   │   │   └── PyResponse.kt
│   │   ├── backup/                 # DB 백업/복원
│   │   │   ├── BackupModels.kt
│   │   │   ├── BackupSerializer.kt
│   │   │   ├── BackupManager.kt
│   │   │   └── BackupMigrator.kt
│   │   ├── cache/                  # 캐시 관리
│   │   │   └── StockCacheManager.kt
│   │   ├── state/                  # 공유 상태
│   │   │   └── SelectedStockManager.kt
│   │   ├── theme/                  # 테마 관리
│   │   │   ├── ThemeManager.kt
│   │   │   └── ThemeToggle.kt
│   │   ├── ui/                     # Common UI
│   │   │   ├── theme/              # Color, Type, Theme, Spacing
│   │   │   └── component/          # ErrorCard, LoadingIndicator, Charts
│   │   │       ├── chart/          # TechnicalCharts, ChartUtils
│   │   │       └── stockinput/     # StockInputField 컴포넌트
│   │   ├── api/                    # Kiwoom REST API (Kotlin 직접 호출)
│   │   │   ├── ApiModels.kt        # API 응답/에러 모델
│   │   │   ├── TokenManager.kt     # OAuth 토큰 관리
│   │   │   ├── KiwoomApiClient.kt  # Kiwoom REST API 클라이언트
│   │   │   └── KisApiClient.kt     # KIS REST API 클라이언트
│   │   ├── util/                   # 유틸리티
│   │   │   └── TradingDayUtil.kt   # 한국 주식시장 거래일 계산
│   │   └── di/                     # DI Modules
│   │       ├── AppModule.kt
│   │       ├── DbModule.kt
│   │       └── PyModule.kt
│   ├── feature/
│   │   ├── search/                 # 종목 검색 (Phase 1)
│   │   │   ├── domain/
│   │   │   ├── data/
│   │   │   ├── ui/SearchScreen.kt, SearchVm.kt
│   │   │   └── di/SearchModule.kt
│   │   ├── analysis/               # 수급 분석 (Phase 1)
│   │   │   ├── domain/
│   │   │   ├── data/
│   │   │   ├── ui/AnalysisScreen.kt, AnalysisVm.kt
│   │   │   └── di/AnalysisModule.kt
│   │   ├── indicator/              # 기술적 지표 (Phase 2)
│   │   │   ├── domain/
│   │   │   ├── data/
│   │   │   ├── ui/IndicatorScreen.kt, IndicatorVm.kt
│   │   │   └── di/IndicatorModule.kt
│   │   ├── settings/               # 설정 (Phase 4)
│   │   │   ├── domain/
│   │   │   │   ├── model/ApiKeyConfig.kt  # API 키, InvestmentMode
│   │   │   │   ├── repo/SettingsRepo.kt, BackupRepo.kt
│   │   │   │   └── usecase/*.kt           # CreateBackupUC, RestoreBackupUC, ValidateBackupUC
│   │   │   ├── data/
│   │   │   │   └── repo/SettingsRepoImpl.kt, BackupRepoImpl.kt
│   │   │   ├── ui/SettingsScreen.kt, SettingsVm.kt, DbBackupTab.kt, DbBackupVm.kt
│   │   │   └── di/SettingsModule.kt
│   │   ├── scheduling/             # 자동 스케줄링 (Phase 5)
│   │   │   ├── SchedulingManager.kt       # WorkManager 오케스트레이션
│   │   │   ├── SyncWorkState.kt           # 동기화 상태 enum
│   │   │   ├── domain/
│   │   │   │   ├── model/SchedulingModels.kt  # SchedulingConfig, SyncStatus
│   │   │   │   └── repo/SchedulingRepo.kt
│   │   │   ├── data/
│   │   │   │   └── repo/SchedulingRepoImpl.kt
│   │   │   ├── worker/
│   │   │   │   └── StockSyncWorker.kt     # WorkManager Worker
│   │   │   ├── ui/SchedulingTab.kt, SchedulingVm.kt
│   │   │   └── di/SchedulingModule.kt
│   │   ├── ranking/                # 순위정보 (Phase 6)
│   │       ├── domain/
│   │       │   ├── model/RankingModels.kt    # RankingType, RankingItem, RankingResult
│   │       │   ├── model/RankingParams.kt    # API 요청 파라미터
│   │       │   ├── repo/RankingRepo.kt
│   │       │   └── usecase/GetRankingUC.kt
│   │       ├── data/
│   │       │   ├── dto/RankingDto.kt         # API 응답 DTO
│   │       │   └── repo/RankingRepoImpl.kt
│   │       ├── ui/RankingScreen.kt, RankingVm.kt
│   │       └── di/RankingModule.kt
│   │   ├── financial/              # 재무정보 (Phase 7)
│   │   │   ├── domain/
│   │   │   │   ├── model/FinancialModels.kt  # FinancialData, FinancialSummary
│   │   │   │   ├── repo/FinancialRepo.kt
│   │   │   │   └── usecase/GetFinancialSummaryUC.kt
│   │   │   ├── data/
│   │   │   │   ├── dto/FinancialDto.kt       # KIS API 응답 DTO
│   │   │   │   └── repo/FinancialRepoImpl.kt
│   │   │   ├── ui/FinancialScreen.kt, FinancialVm.kt, ProfitabilityContent.kt, StabilityContent.kt
│   │   │   └── di/FinancialModule.kt
│   │   └── etf/                    # ETF 분석 (Phase 8) ⭐ NEW
│   │       ├── domain/
│   │       │   ├── model/EtfModels.kt        # EtfData, EtfConstituent
│   │       │   ├── repo/EtfRepo.kt, EtfCollectionRepo.kt
│   │       │   └── usecase/*.kt              # 7개 UseCase
│   │       ├── data/
│   │       │   ├── dto/EtfDto.kt             # API 응답 DTO
│   │       │   └── repo/EtfRepoImpl.kt, EtfCollectionRepoImpl.kt
│   │       ├── worker/EtfCollectionWorker.kt # 백그라운드 수집
│   │       ├── ui/                           # 17개 UI 컴포넌트
│   │       │   ├── EtfScreen.kt, EtfVm.kt
│   │       │   ├── tabs/                     # 5개 탭 컴포넌트
│   │       │   └── components/               # 상세 컴포넌트
│   │       └── di/EtfModule.kt
│   └── nav/
│       ├── Nav.kt                  # Screen 정의 (7개 탭: Search, Analysis, Indicator, Financial, Ranking, ETF, Settings)
│       └── NavGraph.kt             # Navigation
│
└── app/src/main/python/            # Python 패키지 (chart/ 제외)
    └── stock_analyzer/
```

#### 개발 순서

1. **Android Studio 프로젝트 생성** (Empty Compose Activity)
2. **Gradle 설정** (Chaquopy, Hilt, Room, Vico)
3. **Python 패키지 복사** (`chart/` 제외)
4. **PyClient 브릿지 구현**
5. **Feature별 화면 구현**

#### Quick Commands (Android)

```bash
# 프로젝트 생성 후
cd StockApp

# 빌드
./gradlew build

# 단위 테스트
./gradlew test

# 앱 설치 및 실행
./gradlew installDebug
```

### PyClient 사용 예시

```kotlin
// Python 함수 호출
val result = pyClient.call(
    module = "stock_analyzer.stock.search",
    func = "search",
    args = listOf(client, "삼성전자")
) { json ->
    json.decodeFromString<SearchResponse>(json)
}

when (result) {
    is Result.Success -> {
        // data 처리
    }
    is Result.Failure -> {
        // error 처리
    }
}
```

### App Phase 1: 종목 검색 + 수급 분석

#### SearchScreen
- 종목명 또는 코드로 검색
- 300ms debounce 적용
- 검색 히스토리 표시 (최대 50개)
- 검색 결과에서 종목 선택 시 수급 분석 화면으로 이동

#### AnalysisScreen
- 시가총액 (조원 단위)
- 외국인/기관 순매수 (억원 단위)
- 수급 비율 및 매매 신호
- Pull-to-refresh 지원
- 캐시 TTL: 24시간

#### 수급 신호 기준
| Signal | 조건 | 설명 |
|--------|------|------|
| STRONG_BUY | > 0.5% | 강력 매수 |
| BUY | > 0.2% | 매수 |
| NEUTRAL | -0.2% ~ 0.2% | 중립 |
| SELL | < -0.2% | 매도 |
| STRONG_SELL | < -0.5% | 강력 매도 |

#### Kotlin 코드 예시
```kotlin
// 수급 분석 호출
val result = pyClient.call(
    module = "stock_analyzer.stock.analysis",
    func = "analyze",
    args = listOf("005930", 180),
    timeoutMs = 60_000
) { json ->
    json.decodeFromString<AnalysisResponse>(json)
}

// StockData 모델
data class StockData(
    val ticker: String,
    val name: String,
    val dates: List<String>,
    val mcap: List<Long>,      // 시가총액
    val for5d: List<Long>,     // 외국인 순매수
    val ins5d: List<Long>      // 기관 순매수
)
```

### App Phase 2: 기술적 지표

#### IndicatorScreen (탭 구조)
- **Trend Signal**: MA 신호, CMF, Fear/Greed 지수
- **Elder Impulse**: 캔들 색상 (Green/Red/Blue), MACD Histogram
- **DeMark TD Setup**: Sell/Buy 카운트, 매매 신호

#### 네비게이션
- AnalysisScreen에서 "기술 지표 보기" 버튼 → IndicatorScreen
- 탭으로 3가지 지표 간 전환

#### 지표 모델
```kotlin
// Trend Signal
data class TrendSummary(
    val currentTrend: String,      // "bullish", "neutral", "bearish"
    val currentCmf: Double,        // -1 ~ 1
    val currentFearGreed: Double,  // -1 ~ 1.5
    val trendLabel: String,        // "상승 추세", "하락 추세", "중립"
    val cmfLabel: String,          // "자금 유입", "자금 유출", "중립"
    val fearGreedLabel: String     // "탐욕 (과열)", "공포 (침체)", "중립"
)

// Elder Impulse
data class ElderSummary(
    val currentColor: String,      // "green", "red", "blue"
    val colorLabel: String,        // "상승 (Green)", "하락 (Red)", "중립 (Blue)"
    val impulseSignal: String      // "매수 유리", "매도 유리", "관망"
)

// DeMark TD Setup
data class DemarkSummary(
    val currentSellSetup: Int,     // Sell 카운트
    val currentBuySetup: Int,      // Buy 카운트
    val sellSignal: String,        // "매도 신호 (카운트 X)" 또는 "없음"
    val buySignal: String          // "매수 신호 (카운트 X)" 또는 "없음"
)
```

#### Python 호출 예시
```kotlin
// Trend Signal 조회
val result = pyClient.call(
    module = "stock_analyzer.indicator.trend",
    func = "calc",
    args = listOf("005930", 180, "daily")
) { json -> json.decodeFromString<TrendResponse>(json) }

// Elder Impulse 조회
val result = pyClient.call(
    module = "stock_analyzer.indicator.elder",
    func = "calc",
    args = listOf("005930", 180, "daily")
) { json -> json.decodeFromString<ElderResponse>(json) }

// DeMark TD Setup 조회
val result = pyClient.call(
    module = "stock_analyzer.indicator.demark",
    func = "calc",
    args = listOf("005930", 180, "daily")
) { json -> json.decodeFromString<DemarkResponse>(json) }
```

#### Charts 사용 (MPAndroidChart)
- **LineChartContent**: CMF, Fear/Greed 추이
- **BarChartContent**: MACD Histogram
- **DemarkSetupChart**: Sell/Buy Setup 카운트 추이
- 모든 차트는 `AndroidView`로 래핑된 MPAndroidChart 사용

### App Phase 3: ~~시장 지표 + 조건검색~~ (제거됨)

> ⚠️ **이 기능은 제거되었습니다.** Market 및 Condition 기능은 앱에서 제외되었습니다.

---

### App Phase 4: 설정 화면 (Settings)

#### SettingsScreen (API 키 관리 + 투자 모드)

**탭 구조**:
- **API Key 탭**: 키움 API 키 설정
- **Scheduling 탭**: 자동 동기화 설정

#### API 키 설정 기능
- App Key, Secret Key 입력
- 투자 모드 선택: MOCK (모의투자) / PRODUCTION (실전투자)
- API 연결 테스트
- **보안**: EncryptedSharedPreferences (AES256 암호화)

#### 설정 모델
```kotlin
// 투자 모드
enum class InvestmentMode {
    MOCK,       // 모의투자 (mockapi.kiwoom.com)
    PRODUCTION  // 실전투자 (api.kiwoom.com)
}

// API 키 설정
data class ApiKeyConfig(
    val appKey: String,
    val secretKey: String,
    val investmentMode: InvestmentMode
)
```

#### 사용 예시
```kotlin
// 설정 저장
settingsRepo.saveApiKeyConfig(
    ApiKeyConfig(
        appKey = "your_app_key",
        secretKey = "your_secret_key",
        investmentMode = InvestmentMode.MOCK
    )
)

// 설정 조회
val config = settingsRepo.getApiKeyConfig()
```

---

### App Phase 5: 자동 스케줄링 (Scheduling)

#### SchedulingTab (자동 동기화 설정)

**기능**:
- 자동 동기화 활성화/비활성화
- 동기화 시간 설정 (기본: 01:00 AM)
- 수동 동기화 실행
- 동기화 히스토리 조회
- 마지막 동기화 상태 표시

**기술 스택**: Android WorkManager (백그라운드 작업)

#### 스케줄링 모델
```kotlin
// 동기화 상태
enum class SyncStatus {
    NEVER,       // 한 번도 실행 안 됨
    SUCCESS,     // 성공
    FAILED,      // 실패
    IN_PROGRESS  // 진행 중
}

// 동기화 유형
enum class SyncType {
    SCHEDULED,   // 예약된 동기화
    MANUAL       // 수동 동기화
}

// 스케줄링 설정
data class SchedulingConfig(
    val isEnabled: Boolean,      // 자동 동기화 활성화 여부
    val syncHour: Int,           // 동기화 시각 (시)
    val syncMinute: Int,         // 동기화 시각 (분)
    val lastSyncAt: Long?,       // 마지막 동기화 시각
    val lastSyncStatus: SyncStatus  // 마지막 동기화 상태
)

// 동기화 히스토리
data class SyncHistory(
    val id: Long,
    val syncType: SyncType,
    val startedAt: Long,
    val completedAt: Long?,
    val status: SyncStatus,
    val syncedStocksCount: Int,
    val errorMessage: String?
)
```

#### WorkManager Worker
```kotlin
// StockSyncWorker.kt
class StockSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. 등록된 종목 목록 조회
        // 2. 각 종목 데이터 동기화 (수급 분석, 지표)
        // 3. 결과 저장 및 히스토리 기록
        return Result.success()
    }
}
```

#### 스케줄링 관리
```kotlin
// 스케줄 등록
schedulingManager.scheduleDaily(hour = 1, minute = 0)

// 수동 동기화 실행
schedulingManager.syncNow()

// 스케줄 취소
schedulingManager.cancelSchedule()

// 동기화 상태 관찰
schedulingManager.syncState.collect { state ->
    when (state) {
        SyncWorkState.IDLE -> { /* 대기 중 */ }
        SyncWorkState.RUNNING -> { /* 실행 중 */ }
        SyncWorkState.SUCCEEDED -> { /* 성공 */ }
        SyncWorkState.FAILED -> { /* 실패 */ }
    }
}
```

---

### App Phase 6: 순위정보 (Ranking)

#### RankingScreen (순위정보 조회)

**기능**:
- 6가지 순위 유형: 호가잔량급증(매수/매도), 거래량급증, 당일거래량상위, 신용비율상위, 외국인기관상위
- 시장 필터: KOSPI (001), KOSDAQ (101), 전체 (000, ka90009 전용)
- 거래소 필터: KRX (실전), NXT (실전), KRX (모의)
- 표시 개수 선택: 5, 10, 20, 30개
- 종목 클릭 시 Analysis 화면으로 이동

**기술 스택**: OkHttp (Kotlin REST API 직접 호출)

> ⚠️ **중요**: Python 패키지가 FROZEN 상태이므로, Ranking 기능은 Kotlin에서 직접 Kiwoom REST API를 호출합니다.

#### 순위 유형
| 유형 | API ID | 설명 |
|------|--------|------|
| ORDER_BOOK_SURGE_BUY | ka10021 | 호가잔량급증 (매수) |
| ORDER_BOOK_SURGE_SELL | ka10021 | 호가잔량급증 (매도) |
| VOLUME_SURGE | ka10023 | 거래량급증 |
| DAILY_VOLUME_TOP | ka10030 | 당일거래량상위 |
| CREDIT_RATIO_TOP | ka10033 | 신용비율상위 |
| FOREIGN_INSTITUTION_TOP | ka90009 | 외국인기관상위 |

#### 거래소 필터 (투자 모드별)
| 투자 모드 | 거래소 옵션 | 코드 |
|-----------|-------------|------|
| MOCK (모의) | KRX만 | stex_tp: 3 |
| PRODUCTION (실전) | KRX, NXT | stex_tp: 1, 2 |

#### 외국인/기관상위 (ka90009) 전용 필터

| 필터 | 옵션 | 설명 |
|------|------|------|
| 투자자유형 | 외국인, 기관, 전체 | InvestorType enum |
| 매매방향 | 순매수, 순매도 | TradeDirection enum |
| 표시단위 | 금액, 수량 | ValueType enum (amt_qty_tp) |
| 시장 | KOSPI, KOSDAQ, 전체 | MarketType.ALL 지원 |

**ka90009 API 응답 구조**:
- 각 row에 4가지 데이터가 포함됨:
  - `for_netprps_*`: 외인 순매수 종목
  - `for_netslmt_*`: 외인 순매도 종목
  - `orgn_netprps_*`: 기관 순매수 종목
  - `orgn_netslmt_*`: 기관 순매도 종목

#### 핵심 모델
```kotlin
// 순위 유형
enum class RankingType(val displayName: String, val apiId: String) {
    ORDER_BOOK_SURGE_BUY("호가잔량급증(매수)", "ka10021"),
    ORDER_BOOK_SURGE_SELL("호가잔량급증(매도)", "ka10021"),
    VOLUME_SURGE("거래량급증", "ka10023"),
    DAILY_VOLUME_TOP("당일거래량상위", "ka10030"),
    CREDIT_RATIO_TOP("신용비율상위", "ka10033"),
    FOREIGN_INSTITUTION_TOP("외국인기관상위", "ka90009")
}

// ka90009 전용 필터 enum
enum class InvestorType(val displayName: String) {
    FOREIGN("외국인"),
    INSTITUTION("기관"),
    ALL("전체")
}

enum class TradeDirection(val displayName: String) {
    NET_BUY("순매수"),
    NET_SELL("순매도")
}

enum class ValueType(val code: String, val displayName: String) {
    AMOUNT("1", "금액"),
    QUANTITY("2", "수량")
}

// 순위 아이템
data class RankingItem(
    val rank: Int,
    val ticker: String,
    val name: String,
    val currentPrice: Long,
    val priceChange: Long,
    val priceChangeSign: String,  // "+", "-", ""
    val changeRate: Double,
    val volume: Long? = null,
    val surgeQuantity: Long? = null,
    val surgeRate: Double? = null,
    val creditRatio: Double? = null,
    val foreignNetBuy: Long? = null,
    val institutionNetBuy: Long? = null,
    val foreignNetSell: Long? = null,
    val institutionNetSell: Long? = null,
    val totalBuyQuantity: Long? = null,
    val netValue: Long? = null  // 선택된 필터 기준 표시 값
)

// 순위 결과
data class RankingResult(
    val rankingType: RankingType,
    val marketType: MarketType,
    val exchangeType: ExchangeType,
    val items: List<RankingItem>,
    val fetchedAt: LocalDateTime,
    // ka90009 필터 컨텍스트
    val investorType: InvestorType? = null,
    val tradeDirection: TradeDirection? = null,
    val valueType: ValueType? = null
)
```

#### Kotlin REST API 클라이언트
```kotlin
// KiwoomApiClient.kt - Python 없이 직접 API 호출
@Singleton
class KiwoomApiClient @Inject constructor(
    private val tokenManager: TokenManager
) {
    suspend fun <T> call(
        apiId: String,
        url: String,
        body: Map<String, String>,
        appKey: String,
        secretKey: String,
        baseUrl: String,
        parser: (String) -> T
    ): Result<T>
}

// 사용 예시
val result = apiClient.call(
    apiId = "ka10021",
    url = "/api/dostk/rkinfo",
    body = params.toRequestBody(),
    appKey = config.appKey,
    secretKey = config.secretKey,
    baseUrl = config.baseUrl
) { json ->
    json.decodeFromString<OrderBookSurgeResponse>(json)
}
```

#### ViewModel 상태
```kotlin
sealed class RankingState {
    data object Loading : RankingState()
    data object NoApiKey : RankingState()
    data class Success(val result: RankingResult) : RankingState()
    data class Error(val message: String) : RankingState()
}
```

---

### App Phase 7: 재무정보 (Financial)

#### FinancialScreen (재무정보 조회)

**기능**:
- 검색 화면에서 선택한 종목의 재무정보 표시
- 두 개의 탭: 수익성 (Profitability), 안정성 (Stability)
- 7개 KIS API에서 데이터 수집 후 결산년월 기준 병합
- 24시간 캐싱 (Room Database)

**기술 스택**: OkHttp (KIS REST API 직접 호출)

> ⚠️ **중요**: 재무정보는 KIS (한국투자증권) API를 사용합니다. Kiwoom API와 다른 인증 체계입니다.

#### KIS 재무정보 API

| API | tr_id | 설명 |
|-----|-------|------|
| 대차대조표 | FHKST66430100 | 총자산, 유동자산, 부채총계 |
| 손익계산서 | FHKST66430200 | 매출액, 영업이익, 당기순이익 |
| 재무비율 | FHKST66430300 | ROE, ROA, 부채비율 |
| 수익성비율 | FHKST66430400 | 매출총이익률, 영업이익률 |
| 기타주요비율 | FHKST66430500 | EPS, BPS, PER |
| 안정성비율 | FHKST66430600 | 유동비율, 당좌비율 |
| 성장성비율 | FHKST66430800 | 매출액증가율, 순이익증가율 |

**API 명세서**: `docs/KIS_FINANCIAL_API.md`

#### 탭 구조

**수익성 (Profitability) 탭**:
- 요약 카드: 최근 매출액, 영업이익, 당기순이익
- 그룹 바 차트: 결산년월별 손익 추이 (`hasProfitabilityData` 조건)
- 라인 차트: 매출액/영업이익/순이익 증가율 추이 (`hasGrowthData` 조건)
- 라인 차트: 자기자본/총자산 증가율 추이 (`hasAssetGrowthData` 조건)
- 차트는 0이 아닌 유의미한 데이터가 있을 때만 표시됨

**안정성 (Stability) 탭**:
- 요약 카드: 부채비율, 유동비율, 차입금 의존도 (평가 포함)
- 복합 라인 차트: 안정성 지표 추이 (`hasStabilityData` 조건)
- 개별 차트: 각 지표별 상세 추이
- 차트는 0이 아닌 유의미한 데이터가 있을 때만 표시됨

#### 차트 상호작용 기능

모든 재무정보 차트는 다음 상호작용 기능을 지원합니다:

| 기능 | 설명 | 사용법 |
|------|------|--------|
| **터치 마커** | 데이터 포인트 터치 시 상세 값 표시 | 차트의 데이터 포인트 탭 |
| **핀치 줌** | X/Y 축 확대/축소 | 두 손가락으로 핀치 |
| **드래그 팬** | 줌 후 차트 이동 | 한 손가락으로 드래그 |
| **더블 탭** | 원래 뷰로 리셋 | 차트 더블 탭 |

**마커 표시 내용**:

| 차트 | 마커 클래스 | 표시 내용 |
|------|------------|----------|
| 손익 바 차트 | `IncomeBarMarkerView` | 기간, 매출액, 영업이익, 순이익 |
| 성장률 차트 | `GrowthRateMarkerView` | 기간, 각 증가율 (%) |
| 자산 성장률 차트 | `GrowthRateMarkerView` | 기간, 자기자본/총자산 증가율 (%) |
| 안정성 복합 차트 | `StabilityRatioMarkerView` | 기간, 부채비율, 유동비율, 차입금 의존도 |
| 개별 비율 차트 | `SingleRatioMarkerView` | 기간, 해당 비율 값 (%) |

#### 안정성 지표 평가 기준

| 지표 | 양호 | 보통 | 주의 |
|------|------|------|------|
| 부채비율 | < 100% | 100-200% | > 200% |
| 유동비율 | > 200% | 100-200% | < 100% |
| 차입금 의존도 | < 30% | 30-50% | > 50% |

#### 핵심 모델
```kotlin
// 탭
enum class FinancialTab(val label: String) {
    PROFITABILITY("수익성"),
    STABILITY("안정성")
}

// 재무정보 요약 (UI용)
data class FinancialSummary(
    val ticker: String,
    val name: String,
    val periods: List<FinancialPeriod>,
    // 수익성 데이터
    val revenues: List<Long>,
    val operatingProfits: List<Long>,
    val netIncomes: List<Long>,
    val revenueGrowthRates: List<Double>,
    val operatingProfitGrowthRates: List<Double>,
    val netIncomeGrowthRates: List<Double>,
    val equityGrowthRates: List<Double>,
    val totalAssetsGrowthRates: List<Double>,
    // 안정성 데이터
    val debtRatios: List<Double>,
    val currentRatios: List<Double>,
    val borrowingDependencies: List<Double>
) {
    // 데이터 유효성 검사 (0이 아닌 값이 있는지 확인)
    val hasProfitabilityData: Boolean  // 손익 데이터 존재 여부
    val hasGrowthData: Boolean         // 성장률 데이터 존재 여부
    val hasAssetGrowthData: Boolean    // 자산 성장률 데이터 존재 여부
    val hasStabilityData: Boolean      // 안정성 데이터 존재 여부
}

// 재무기간 (결산년월)
data class FinancialPeriod(
    val yearMonth: String,  // "202312"
    val displayLabel: String  // "2023.12"
)
```

#### ViewModel 상태
```kotlin
sealed class FinancialState {
    data object NoStock : FinancialState()
    data object Loading : FinancialState()
    data object NoApiKey : FinancialState()
    data class Success(val summary: FinancialSummary) : FinancialState()
    data class Error(val message: String) : FinancialState()
}
```

---

### App Phase 8: ETF 분석 (ETF)

#### EtfScreen (ETF 포트폴리오 추적)

**기능**:
- ETF 데이터 수집 및 분석
- 5개 탭 구조: 수집현황, 종목랭킹, 종목변동, 테마목록, ETF설정
- 키워드 기반 ETF 필터링
- 일별 ETF 통계 (현금예탁금, 신규종목)
- 종목 상세 분석 (BottomSheet)
- **실제 거래일 기준 데이터 저장** (API의 `stck_bsop_date` 사용)
- **미수집일 분석 및 표시** (수집 현황 탭)

**기술 스택**: WorkManager (백그라운드 수집), Room DB (15 entities)

#### ETF 데이터 날짜 처리

ETF 데이터는 **실제 거래일 기준**으로 저장됩니다:
- API 응답의 `stck_bsop_date` 필드에서 기준일자 추출
- `TradingDayUtil`을 통한 거래일 판단 (주말/공휴일 제외)
- 미수집일 분석 및 수집률 표시

```kotlin
// 거래일 유틸리티 (core/util/TradingDayUtil.kt)
object TradingDayUtil {
    fun apiToDbFormat(apiDate: String?): String?  // YYYYMMDD → YYYY-MM-DD
    fun isTradingDay(date: LocalDate): Boolean    // 거래일 여부 확인
    fun findMissingTradingDays(...)               // 미수집 거래일 탐지
}

// 미수집일 분석 결과
data class MissingDatesResult(
    val dataStartDate: String?,
    val dataEndDate: String?,
    val missingDates: List<String>,
    val totalTradingDays: Int,
    val collectedDays: Int,
    val coveragePercent: Double
)
```

#### 현금/예금 항목 감지 (Cash Detection)

ETF 구성종목 중 현금/예금 항목은 `CashItemUtil`에서 감지됩니다:

**감지 방식**:
1. **코드 기반**: 알려진 현금 종목코드와 일치하는지 확인
2. **이름 기반**: 종목명에 현금 관련 키워드가 포함되어 있는지 확인

**지원 종목코드** (`CASH_STOCK_CODES`):
| 코드 | 설명 |
|------|------|
| `010010` | KRW 현금/예금 (단축 코드) |
| `KRD010010001` | 원화현금/원화예금 (ISIN 형식) |

**지원 키워드** (`CASH_NAME_KEYWORDS`):
- 원화현금, 원화예금, 현금, cash, 예금, krw, 원화, 예치금, mmf, 머니마켓, money market

**로깅 기능**:
- 현금 항목 수집 시 자동으로 로그 기록
- Logcat 태그: `CashItemUtil`
- 기록 내용: ETF코드, 종목코드, 종목명, 평가금액, 매칭된 코드/키워드

```kotlin
// 로그 예시
// Cash detected: ETF=069500, code=KRD010010001, name=원화현금,
//   amount=123.45억, matchedByCode=true, matchedKeyword=null
```

**파일 위치**: `feature/etf/data/CashItemUtil.kt`

#### ETF 탭 구조

| 탭 | 컴포넌트 | 기능 |
|----|----------|------|
| 수집현황 | CollectionStatusTab | 수집 진행률, 통계 요약 |
| 종목랭킹 | StockRankingTab | 구성종목 비중 순위 (컬럼 정렬 지원) |
| 종목변동 | StockChangesTab | 편입/편출 종목 추적 |
| 테마목록 | ThemeListTab | 키워드 기반 테마 필터링 |
| ETF설정 | EtfSettingsTab | 키워드 관리, 수집 설정 |

#### 종목랭킹 테이블 정렬 (다중 컬럼 정렬)

종목랭킹 탭에서는 **다중 컬럼 정렬**을 지원합니다 (최대 3개 컬럼):

| 컬럼 | 정렬 대상 | 설명 |
|------|----------|------|
| 합산금액 | totalAmount | 기본 정렬 (내림차순) |
| ETF수 | etfCount | 포함된 ETF 개수 |
| 변동 | amountChange | 전일 대비 변동금액 |

**사용자 인터랙션**:

| 동작 | 결과 |
|------|------|
| 비활성 컬럼 클릭 | 정렬 목록 끝에 추가 (최대 3개), 내림차순 |
| 활성 컬럼 클릭 (내림차순) | 오름차순으로 전환 |
| 활성 컬럼 클릭 (오름차순) | **정렬 해제 (제거)** |
| 활성 컬럼 롱프레스 | 정렬 목록에서 제거 |
| "초기화" 버튼 | 기본값으로 리셋 (합산금액 내림차순) |
| 최대 3개 상태에서 새 컬럼 클릭 | 마지막 컬럼을 새 컬럼으로 교체 |

**클릭 사이클**: `OFF → 내림차순(↓) → 오름차순(↑) → OFF (해제)`

**시각적 피드백**:
- **우선순위 배지**: 원형 배지에 숫자 (1, 2, 3) 표시
- **활성 컬럼 (내림차순)**: 굵은 텍스트 + primary 색상
- **활성 컬럼 (오름차순)**: 굵은 텍스트 + tertiary 색상 (해제 예정 암시)
- **방향 화살표**: 컬럼 텍스트 옆에 ↑/↓ 표시
- **정렬 정보 바**: 다중 정렬 시 "정렬: 합산금액 > ETF수 > 변동" + 도움말 표시

```kotlin
// 정렬 모델 (EtfModels.kt)
enum class RankingSortColumn { TOTAL_AMOUNT, ETF_COUNT, AMOUNT_CHANGE }
enum class SortDirection { ASCENDING, DESCENDING }

// 개별 정렬 기준
data class SortCriteria(
    val column: RankingSortColumn,
    val direction: SortDirection = SortDirection.DESCENDING
)

// 다중 컬럼 정렬 상태 (최대 3개, 선택 순서가 우선순위)
data class RankingSortState(
    val criteria: List<SortCriteria> = listOf(
        SortCriteria(RankingSortColumn.TOTAL_AMOUNT, SortDirection.DESCENDING)
    )
) {
    companion object { const val MAX_SORT_COLUMNS = 3 }
    fun getPriority(column: RankingSortColumn): Int?  // 우선순위 (1, 2, 3)
    fun getDirection(column: RankingSortColumn): SortDirection?
    fun onColumnClick(column: RankingSortColumn): RankingSortState
    fun removeColumn(column: RankingSortColumn): RankingSortState
    fun reset(): RankingSortState
}
```

#### 핵심 모델
```kotlin
// ETF 데이터
data class EtfData(
    val etfCode: String,
    val etfName: String,
    val type: String,
    val totalAssets: Long,
    val constituents: List<EtfConstituent>
)

// ETF 구성종목
data class EtfConstituent(
    val stockCode: String,
    val stockName: String,
    val weight: Double,
    val evaluationAmount: Long
)

// ETF 키워드 필터
data class EtfKeyword(
    val keyword: String,
    val filterType: FilterType,  // INCLUDE, EXCLUDE
    val isEnabled: Boolean
)
```

#### 7개 UseCase
| UseCase | 기능 |
|---------|------|
| CollectAllEtfDataUC | 전체 ETF 데이터 수집 |
| CollectEtfDataUC | 개별 ETF 데이터 수집 |
| GetCashDepositTrendUC | 현금예탁금 추이 조회 |
| GetComparisonInRangeUC | 기간 비교 분석 |
| GetStockAnalysisUC | 종목 분석 조회 |
| GetStockChangesUC | 종목 변동 추적 |
| GetStockRankingUC | 종목 랭킹 조회 |

#### ViewModel 상태
```kotlin
sealed class EtfState {
    data object Loading : EtfState()
    data object NoApiKey : EtfState()
    data class Success(val data: EtfScreenData) : EtfState()
    data class Error(val message: String) : EtfState()
}
```

---

### App Phase 9: DB 백업/복원 (DB Backup)

#### DbBackupTab (데이터베이스 백업 및 복원)

**기능**:
- 전체 DB 백업 또는 날짜 범위 필터링 백업
- JSON 형식 백업 파일 생성
- 백업 파일 검증 및 복원
- MERGE (병합) 또는 REPLACE (교체) 복원 모드
- 백업 파일 버전 관리 및 마이그레이션

**기술 스택**: Kotlinx Serialization, Android SAF (Storage Access Framework)

#### 백업 유형
| 유형 | 설명 |
|------|------|
| FULL | 전체 데이터 백업 |
| FILTERED | 날짜 범위 필터링 백업 |

#### 복원 모드
| 모드 | 설명 |
|------|------|
| MERGE | 기존 데이터 유지, 새 데이터 추가/업데이트 |
| REPLACE | 기존 데이터 삭제 후 복원 |

#### 핵심 모델
```kotlin
// 백업 유형
enum class BackupType { FULL, FILTERED }

// 복원 모드
enum class RestoreMode { MERGE, REPLACE }

// 백업 메타데이터
data class BackupMetadata(
    val version: Int,
    val createdAt: Long,
    val appVersion: String,
    val backupType: BackupType,
    val startDate: String?,
    val endDate: String?,
    val entityCounts: Map<String, Int>
)

// 백업 파일 구조
data class BackupFile(
    val metadata: BackupMetadata,
    val tables: BackupTables
)
```

#### 호환성 처리
- `ignoreUnknownKeys = true`: 새 필드가 추가되어도 이전 버전에서 복원 가능
- nullable 테이블 목록: 테이블이 추가/제거되어도 호환성 유지
- 버전 마이그레이션: BackupMigrator를 통한 버전별 데이터 변환

#### 파일 구조
```
core/backup/
├── BackupModels.kt        # 백업 데이터 모델
├── BackupSerializer.kt    # JSON 직렬화/역직렬화
├── BackupManager.kt       # 백업/복원 핵심 로직
└── BackupMigrator.kt      # 버전 마이그레이션

feature/settings/
├── domain/
│   ├── repo/BackupRepo.kt
│   └── usecase/
│       ├── CreateBackupUC.kt
│       ├── RestoreBackupUC.kt
│       └── ValidateBackupUC.kt
├── data/repo/BackupRepoImpl.kt
└── ui/
    ├── DbBackupTab.kt
    └── DbBackupVm.kt
```

### 참고 문서

- Android 사전 준비: `docs/ANDROID_PREPARATION.md`
- 상세 명세서: `docs/STOCK_APP_SPEC.md`
- **Kotlin 전환 명세서**: `docs/KOTLIN_MIGRATION_SPEC.md`
- 코드 리뷰: `docs/CODE_REVIEW_REPORT.md`
- UI 디자인 리뷰: `docs/UI_DESIGN_REVIEW.md`
- 키움 API 문서: `docs/kiwoom_api_docs/`
- KIS 재무정보 API: `docs/KIS_FINANCIAL_API.md`

### 외부 라이브러리 문서

- Chaquopy: https://chaquo.com/chaquopy/
- Vico Charts: https://github.com/patrykandpatrick/vico
- Hilt (DI): https://dagger.dev/hilt/
- Room (DB): https://developer.android.com/training/data-storage/room
- WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started

---

## Database Schema

### Room Database (v9, 15 entities, 12 DAOs)

| Entity | 용도 | 주요 필드 |
|--------|------|----------|
| `StockEntity` | 종목 정보 캐시 | ticker, name, market, updatedAt |
| `AnalysisCacheEntity` | 수급 분석 캐시 | ticker, data (JSON), startDate, endDate, cachedAt |
| `SearchHistoryEntity` | 검색 히스토리 | id, ticker, name, searchedAt |
| `IndicatorCacheEntity` | 기술 지표 캐시 | key, ticker, type, data (JSON), cachedAt |
| `SchedulingEntity` | 스케줄링 설정 | id, isEnabled, syncHour, syncMinute, lastSyncAt, lastSyncStatus, isErrorStopped |
| `SyncHistoryEntity` | 동기화 히스토리 | id, syncType, startedAt, completedAt, status, syncedStocksCount |
| `StockAnalysisDataEntity` | 증분 분석 데이터 | ticker, date, data (JSON) |
| `IndicatorDataEntity` | 증분 지표 데이터 | ticker, date, indicatorType, data (JSON) |
| `FinancialCacheEntity` | 재무정보 캐시 | ticker, name, data (JSON), cachedAt |
| `EtfEntity` | ETF 마스터 데이터 | etfCode, etfName, type, totalAssets |
| `EtfConstituentEntity` | ETF 구성종목 | etfCode, stockCode, weight, evaluationAmount |
| `EtfKeywordEntity` | ETF 키워드 필터 | keyword, filterType, isEnabled |
| `EtfCollectionHistoryEntity` | ETF 수집 이력 | collectedDate, totalEtfs, status |
| `DailyEtfStatisticsEntity` | ETF 일별 통계 | date, newStockCount, cashDepositAmount |

### 캐시 정책

| 데이터 | TTL | 비고 |
|--------|-----|------|
| 종목 정보 | 24시간 | 앱 시작 시 체크 |
| 수급 분석 | 24시간 | 요청 시 갱신 |
| 기술 지표 | 24시간 | 요청 시 갱신 |
| 재무정보 | 24시간 | 요청 시 갱신 |
| 검색 히스토리 | 무제한 | 최대 50개 유지 |

---

## 기술 스택 요약

| 기술 | 버전 | 용도 |
|------|------|------|
| Kotlin | 2.1.0 | 앱 개발 언어 |
| Jetpack Compose | BOM 2024.12.01 | UI 프레임워크 |
| Hilt | 2.54 | 의존성 주입 |
| Room | 2.8.3 | 로컬 데이터베이스 (v9, 15 entities, 12 DAOs) |
| WorkManager | 2.10.0 | 백그라운드 작업 (스케줄링, ETF 수집) |
| MPAndroidChart | 3.1.0 | 차트 라이브러리 (모든 차트) |
| Vico | 2.0.0-alpha.28 | 차트 라이브러리 (미사용, 제거 권장) |
| Chaquopy | 15.0.1 | Python 통합 |
| DataStore | 1.1.1 | 설정 저장 |
| Security Crypto | 1.1.0-alpha06 | 암호화 저장소 (AES256) |
| OkHttp | 4.12.0 | Kotlin REST API 클라이언트 (순위정보, 재무정보) |
| Kotlinx Serialization | 1.7.1 | JSON 직렬화 |
