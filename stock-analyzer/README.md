# Stock Analyzer

키움증권 REST API를 활용한 주식 분석 Python 라이브러리.

> ⚠️ **이 패키지는 FROZEN 상태입니다.** 참조용으로만 사용되며, 추가 개발은 StockApp Android 앱에서 진행됩니다.

## Features

### Phase 0-1: 기본 기능
- **종목 검색**: 이름/코드로 종목 검색
- **수급 분석**: 시가총액, 외국인/기관 순매수 분석
- **가격 데이터**: 일/주/월봉 OHLCV 데이터 조회
- **자동 인증**: OAuth 토큰 자동 발급 및 갱신

### Phase 2: 기술적 지표
- **Trend Signal**: MA 정배열, CMF, Fear/Greed 지수
- **Elder Impulse**: EMA13, MACD 히스토그램 기반 캔들 색상
- **DeMark TD Setup**: 매수/매도 Setup 카운트

### Phase 3: 차트 시각화
- **캔들스틱 차트**: OHLCV 차트 (Elder 색상 지원)
- **라인 차트**: 지표 시각화
- **바 차트**: 수급 분석 차트

### Phase 4: 시장 지표
- **예탁금 추이**: 고객예탁금, 신용융자
- **신용잔고**: 신용잔고, 신용비율

### Phase 5: 수급 오실레이터
- **MACD 스타일 오실레이터**: 시가총액 대비 수급 비율 기반
- **매매 신호 분석**: -100 ~ +100 점수

## Installation

### Using uv (Recommended)

[uv](https://github.com/astral-sh/uv)는 Rust로 작성된 빠른 Python 패키지 매니저입니다.

```bash
# uv 설치 (macOS/Linux)
curl -LsSf https://astral.sh/uv/install.sh | sh

# 가상환경 생성 및 의존성 설치
uv venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
uv pip install -e ".[dev]"

# 또는 한 번에 실행
uv run pytest tests/unit/ -v
```

### Using pip

```bash
# 가상환경 생성
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# 개발 모드 설치
pip install -e ".[dev]"
```

## Configuration

1. `.env` 파일 생성:
```bash
cp .env.example .env
```

2. API 키 설정:
```env
KIWOOM_APP_KEY=your_app_key_here
KIWOOM_SECRET_KEY=your_secret_key_here
KIWOOM_BASE_URL=https://api.kiwoom.com
```

> 키움증권 REST API 키는 [키움 OpenAPI](https://openapi.kiwoom.com) 에서 발급받을 수 있습니다.

## Quick Start

```python
from stock_analyzer.client.kiwoom import KiwoomClient
from stock_analyzer.config import Config
from stock_analyzer.stock.search import search, get_info
from stock_analyzer.stock.analysis import analyze
from stock_analyzer.stock.ohlcv import get_daily
from stock_analyzer.indicator import trend, elder, demark, oscillator

# 설정 로드
config = Config.from_env()

# 클라이언트 생성
client = KiwoomClient(
    app_key=config.app_key,
    secret_key=config.secret_key,
    base_url=config.base_url,
)

# 종목 검색
result = search(client, "삼성전자")

# 수급 분석
result = analyze(client, "005930", days=180)

# 기술적 지표
result = trend.calc(client, "005930", days=180)
result = elder.calc(client, "005930", days=180)
result = demark.calc(client, "005930", days=180)

# 수급 오실레이터
result = oscillator.calc(client, "005930", days=180)
signal = oscillator.analyze_signal(result)
```

## API Reference

### Stock Search (`stock/search.py`)

```python
from stock_analyzer.stock.search import search, get_all, get_info, get_name

search(client, "삼성")          # 종목 검색
get_all(client, market="0")     # 전체 종목 (0:전체, 1:KOSPI, 2:KOSDAQ)
get_info(client, "005930")      # 종목 기본정보
get_name(client, "005930")      # 종목명
```

### Supply/Demand Analysis (`stock/analysis.py`)

```python
from stock_analyzer.stock.analysis import analyze, get_foreign_trend, get_institution_trend

analyze(client, "005930", days=180)       # 수급 분석 시계열
get_foreign_trend(client, "005930")       # 외국인 매매동향
get_institution_trend(client, "005930")   # 기관 매매동향
```

### OHLCV Data (`stock/ohlcv.py`)

```python
from stock_analyzer.stock.ohlcv import get_daily, get_weekly, get_monthly

get_daily(client, "005930", days=180)     # 일봉
get_weekly(client, "005930", weeks=52)    # 주봉
get_monthly(client, "005930", months=24)  # 월봉
```

### Technical Indicators (`indicator/`)

```python
from stock_analyzer.indicator import trend, elder, demark, oscillator

# Trend Signal (MA, CMF, Fear/Greed)
trend.calc(client, "005930", days=180)

# Elder Impulse (EMA13, MACD Histogram)
elder.calc(client, "005930", days=180)

# DeMark TD Setup
demark.calc(client, "005930", days=180)

# 수급 오실레이터 (MACD 스타일)
oscillator.calc(client, "005930", days=180)
oscillator.analyze_signal(result)  # 매매 신호 분석
```

### Market Indicators (`market/deposit.py`)

```python
from stock_analyzer.market import deposit

deposit.get_deposit(client, days=30)           # 예탁금 추이
deposit.get_credit(client, days=30)            # 신용잔고 추이
deposit.get_market_indicators(client, days=30) # 통합 시장 지표
```

### Chart Visualization (`chart/`)

```python
from stock_analyzer.chart import candle, line, bar

candle.plot_from_ohlcv(ohlcv_data)             # 캔들스틱 차트
line.plot_trend(trend_data)                    # 트렌드 차트
bar.plot_supply_demand(analysis_data)          # 수급 차트
```

## Response Format

### Success
```json
{
  "ok": true,
  "data": {
    "ticker": "005930",
    "name": "삼성전자",
    ...
  }
}
```

### Error
```json
{
  "ok": false,
  "error": {
    "code": "INVALID_ARG",
    "msg": "종목코드가 필요합니다"
  }
}
```

### Error Codes

| Code | Description |
|------|-------------|
| `INVALID_ARG` | 잘못된 인자 |
| `TICKER_NOT_FOUND` | 종목 없음 |
| `NO_DATA` | 데이터 없음 |
| `API_ERROR` | API 오류 |
| `AUTH_ERROR` | 인증 실패 |
| `NETWORK_ERROR` | 네트워크 오류 |
| `CHART_ERROR` | 차트 생성 실패 |
| `INSUFFICIENT_DATA` | 데이터 부족 |

## Testing

```bash
# 단위 테스트 (168개)
uv run pytest tests/unit/ -v

# 커버리지
uv run pytest tests/unit/ --cov=stock_analyzer

# 통합 테스트 (API 키 필요)
uv run pytest tests/integration/ -v

# E2E 테스트 (API 키 필요)
uv run pytest tests/e2e/ -v
```

## Project Structure

```
stock-analyzer/
├── pyproject.toml
├── .env.example
├── README.md
│
├── src/stock_analyzer/
│   ├── __init__.py
│   ├── config.py
│   │
│   ├── core/               # 공통 유틸리티
│   │   ├── log.py
│   │   ├── http.py
│   │   ├── date.py
│   │   └── json_util.py
│   │
│   ├── client/             # API 클라이언트
│   │   ├── auth.py
│   │   └── kiwoom.py
│   │
│   ├── stock/              # 주식 데이터
│   │   ├── search.py
│   │   ├── analysis.py
│   │   └── ohlcv.py
│   │
│   ├── indicator/          # 기술적 지표
│   │   ├── trend.py
│   │   ├── elder.py
│   │   ├── demark.py
│   │   └── oscillator.py
│   │
│   ├── market/             # 시장 지표
│   │   └── deposit.py
│   │
│   ├── search/             # 조건검색
│   │   └── condition.py
│   │
│   └── chart/              # 차트 시각화
│       ├── candle.py
│       ├── line.py
│       ├── bar.py
│       └── oscillator.py
│
├── tests/
│   ├── conftest.py
│   ├── unit/               # 단위 테스트 (168개)
│   ├── integration/
│   └── e2e/
│
└── scripts/
    └── run_analysis.py
```

## Requirements

- Python >= 3.10
- pandas >= 2.0.0
- numpy >= 1.24.0
- requests >= 2.31.0
- python-dotenv >= 1.0.0
- matplotlib >= 3.7.0
- mplfinance >= 0.12.10

## License

MIT License

## Related

- [키움 REST API 가이드](https://openapi.kiwoom.com/guide/apiguide)
- [명세서](../docs/STOCK_APP_SPEC.md)
- [StockApp Android](../StockApp/README.md)
