# CLAUDE.md - Stock Analyzer Project

## Project Overview

키움증권 REST API를 활용한 주식 분석 도구. Python으로 데이터 수집/분석 로직을 검증한 후 Android 앱으로 통합 예정.

## Current Status

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 0 | ✅ Done | 프로젝트 설정, 키움 API 클라이언트 |
| Phase 1 | ✅ Done | 종목 검색, 수급 분석, OHLCV |
| Phase 2 | ✅ Done | 기술적 지표 (Trend, Elder, DeMark) |
| Phase 3 | ✅ Done | 차트 시각화 (Candle, Line, Bar) |
| Phase 4 | ⏳ Pending | 조건검색, 시장 지표 |

## Quick Commands

```bash
cd stock-analyzer

# uv 사용 (권장)
uv venv && source .venv/bin/activate
uv pip install -e ".[dev]"
uv run pytest tests/unit/ -v
uv run python scripts/run_analysis.py

# pip 사용
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
python -m pytest tests/unit/ -v
python scripts/run_analysis.py

# 전체 테스트 (API 키 필요)
uv run pytest tests/ -v
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
│   │   └── demark.py       # DeMark TD Setup
│   ├── chart/              # 차트 시각화
│   │   ├── candle.py       # 캔들스틱 차트
│   │   ├── line.py         # 라인 차트
│   │   └── bar.py          # 바 차트
│   ├── market/             # (Phase 4) 시장 지표
│   └── search/             # (Phase 4) 조건검색
├── tests/
│   ├── unit/               # 단위 테스트 (98개)
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

### 함수 호출 예시
```python
from stock_analyzer.client.kiwoom import KiwoomClient
from stock_analyzer.stock import search, analysis, ohlcv
from stock_analyzer.indicator import trend, elder, demark
from stock_analyzer.chart import candle, line, bar

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
```

## Kiwoom API Reference

| API ID | 기능 | 모듈 |
|--------|------|------|
| au10001 | 토큰 발급 | client/auth.py |
| ka10099 | 종목 리스트 | stock/search.py |
| ka10001 | 주식 기본정보 | stock/search.py |
| ka10008 | 외국인 매매동향 | stock/analysis.py |
| ka10059 | 투자자별 매매 | stock/analysis.py |
| ka10081 | 일봉 차트 | stock/ohlcv.py, indicator/* |
| ka10082 | 주봉 차트 | stock/ohlcv.py |
| ka10083 | 월봉 차트 | stock/ohlcv.py |

## Technical Indicators (Phase 2)

### Trend Signal (`indicator/trend.py`)
MA, CMF, Fear/Greed를 조합한 추세 신호
- `ma_signal`: MA 정배열/역배열 (1: 상승, 0: 중립, -1: 하락)
- `cmf`: Chaikin Money Flow (-1 ~ 1)
- `fear_greed`: 공포/탐욕 지수 (0-100)
- `trend`: 종합 추세 ("bullish", "neutral", "bearish")

### Elder Impulse (`indicator/elder.py`)
EMA13과 MACD 히스토그램을 이용한 캔들 색상 결정
- `color`: 캔들 색상 ("green", "red", "blue")
- `ema13`: 13일 지수이동평균
- `macd_hist`: MACD 히스토그램

### DeMark TD (`indicator/demark.py`)
DeMark TD Sequential의 Setup 부분
- `setup_count`: Setup 카운트 (0-9)
- `setup_type`: Setup 유형 ("buy", "sell", "none")
- `setup_complete`: 9 완성 여부
- `perfected`: Perfected Setup 여부

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
