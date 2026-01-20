# CLAUDE.md - Stock Analyzer Project

## Project Overview

키움증권 REST API를 활용한 주식 분석 도구. Python으로 데이터 수집/분석 로직을 검증한 후 Android 앱으로 통합.

## ⚠️ Development Scope

| Component | Status | Note |
|-----------|--------|------|
| **Python (stock-analyzer)** | 🔒 **FROZEN** | 개발 완료, 변경/개선 대상 아님 |
| **Android (StockApp)** | 🚀 **ACTIVE** | 현재 개발/개선 대상 |

**중요**: Python 패키지는 참조용으로만 사용합니다. 향후 모든 개발, 개선, 버그 수정은 Android 앱(StockApp)에만 적용됩니다.

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
| App Phase 2 | ✅ Done | 기술적 지표 화면 (Vico Charts) |
| App Phase 3 | ✅ Done | 시장 지표, 조건검색 화면 |

**코드**: ~101 files, ~11,100 lines (Kotlin)
**코드 품질**: 7.4/10 (테스트 부재로 감점)
**사전 준비 문서**: `docs/ANDROID_PREPARATION.md`

> 🚀 **이 프로젝트가 현재 활성 개발 대상입니다.** 모든 기능 추가, 버그 수정, 개선 작업은 여기에 적용됩니다.

## Quick Commands

```bash
# Android 앱 빌드 (주요 명령어)
cd StockApp
./gradlew build
./gradlew installDebug
./gradlew test

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
│   │   ├── db/                     # Room Database
│   │   │   ├── AppDb.kt
│   │   │   ├── entity/StockEntity.kt
│   │   │   └── dao/*.kt
│   │   ├── py/                     # Python Bridge
│   │   │   ├── PyClient.kt
│   │   │   └── PyResponse.kt
│   │   ├── ui/                     # Common UI
│   │   │   ├── theme/
│   │   │   └── component/
│   │   └── di/                     # DI Modules
│   │       ├── AppModule.kt
│   │       ├── DbModule.kt
│   │       └── PyModule.kt
│   ├── feature/
│   │   ├── search/                 # 종목 검색 (Phase 1)
│   │   │   ├── domain/model/Stock.kt
│   │   │   ├── domain/repo/SearchRepo.kt
│   │   │   ├── domain/usecase/SearchStockUC.kt
│   │   │   ├── data/repo/SearchRepoImpl.kt
│   │   │   ├── ui/SearchScreen.kt
│   │   │   ├── ui/SearchVm.kt
│   │   │   └── di/SearchModule.kt
│   │   ├── analysis/               # 수급 분석 (Phase 1)
│   │   │   ├── domain/model/StockData.kt
│   │   │   ├── domain/repo/AnalysisRepo.kt
│   │   │   ├── domain/usecase/GetAnalysisUC.kt
│   │   │   ├── data/repo/AnalysisRepoImpl.kt
│   │   │   ├── ui/AnalysisScreen.kt
│   │   │   ├── ui/AnalysisVm.kt
│   │   │   └── di/AnalysisModule.kt
│   │   ├── indicator/              # 기술적 지표 (Phase 2)
│   │   │   ├── domain/model/IndicatorModels.kt
│   │   │   ├── domain/repo/IndicatorRepo.kt
│   │   │   ├── domain/usecase/GetTrendUC.kt
│   │   │   ├── domain/usecase/GetElderUC.kt
│   │   │   ├── domain/usecase/GetDemarkUC.kt
│   │   │   ├── data/repo/IndicatorRepoImpl.kt
│   │   │   ├── ui/IndicatorScreen.kt
│   │   │   ├── ui/IndicatorVm.kt
│   │   │   └── di/IndicatorModule.kt
│   │   ├── market/                 # 시장 지표 (Phase 3)
│   │   │   ├── domain/model/MarketModels.kt
│   │   │   ├── domain/repo/MarketRepo.kt
│   │   │   ├── domain/usecase/GetMarketIndicatorsUC.kt
│   │   │   ├── data/repo/MarketRepoImpl.kt
│   │   │   ├── ui/MarketScreen.kt
│   │   │   ├── ui/MarketVm.kt
│   │   │   └── di/MarketModule.kt
│   │   └── condition/              # 조건검색 (Phase 3)
│   │       ├── domain/model/ConditionModels.kt
│   │       ├── domain/repo/ConditionRepo.kt
│   │       ├── domain/usecase/GetConditionListUC.kt
│   │       ├── domain/usecase/SearchConditionUC.kt
│   │       ├── data/repo/ConditionRepoImpl.kt
│   │       ├── ui/ConditionScreen.kt
│   │       ├── ui/ConditionVm.kt
│   │       └── di/ConditionModule.kt
│   └── nav/
│       ├── Nav.kt                  # Screen 정의
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

#### Vico Charts 사용
- **LineChartContent**: CMF, Fear/Greed 추이
- **BarChartContent**: MACD Histogram
- **DemarkSetupChart**: Sell/Buy Setup 카운트 추이

### App Phase 3: 시장 지표 + 조건검색

#### MarketScreen (시장 지표)
- 고객예탁금, 신용융자, 신용잔고, 신용비율 표시
- 기간 선택 (7일, 14일, 30일, 60일, 90일)
- 추이 차트 (Vico LineChart)
- Pull-to-refresh 지원
- 캐시 TTL: 24시간

#### 시장 지표 모델
```kotlin
data class MarketSummary(
    val dates: List<String>,
    val currentDeposit: Long,        // 고객예탁금 (원)
    val currentCreditLoan: Long,     // 신용융자 (원)
    val currentCreditBalance: Long,  // 신용잔고 (원)
    val currentCreditRatio: Double,  // 신용비율 (%)
    // 전일 대비 변화
    val depositChange: Long,
    val creditLoanChange: Long,
    // 차트 데이터
    val depositHistory: List<Long>,
    val creditRatioHistory: List<Double>
)
```

#### Python 호출 예시 (Market)
```kotlin
// 시장 지표 조회
val result = pyClient.call(
    module = "stock_analyzer.market.deposit",
    func = "get_market_indicators",
    args = listOf(30)  // days
) { json -> json.decodeFromString<MarketIndicatorsResponse>(json) }
```

#### ConditionScreen (조건검색)
- 조건검색 목록 표시
- 조건 선택 시 검색 실행
- 검색 결과에서 종목 선택 시 수급 분석 화면으로 이동
- Pull-to-refresh 지원
- 캐시 TTL: 24시간

#### 조건검색 모델
```kotlin
data class Condition(
    val idx: String,    // 조건검색 인덱스
    val name: String    // 조건검색 이름
)

data class ConditionResult(
    val condition: Condition,
    val stocks: List<ConditionStock>
)

data class ConditionStock(
    val ticker: String,
    val name: String,
    val price: Int,
    val change: Double  // 등락률 (%)
)
```

#### Python 호출 예시 (Condition)
```kotlin
// 조건검색 목록 조회
val result = pyClient.call(
    module = "stock_analyzer.search.condition",
    func = "get_list",
    args = emptyList()
) { json -> json.decodeFromString<ConditionListResponse>(json) }

// 조건검색 실행
val result = pyClient.call(
    module = "stock_analyzer.search.condition",
    func = "search",
    args = listOf("000", "골든크로스")
) { json -> json.decodeFromString<ConditionSearchResponse>(json) }
```

### 참고 문서

- Android 사전 준비: `docs/ANDROID_PREPARATION.md`
- 상세 명세서: `docs/STOCK_APP_SPEC.md`
- Chaquopy: https://chaquo.com/chaquopy/
- Vico Charts: https://github.com/patrykandpatrick/vico
