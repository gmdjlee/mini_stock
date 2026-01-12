# Stock Analyzer

키움증권 REST API를 활용한 주식 분석 Python 라이브러리.

## Features

- **종목 검색**: 이름/코드로 종목 검색
- **수급 분석**: 시가총액, 외국인/기관 순매수 분석
- **가격 데이터**: 일/주/월봉 OHLCV 데이터 조회
- **자동 인증**: OAuth 토큰 자동 발급 및 갱신

## Installation

```bash
# 개발 모드 설치
pip install -e ".[dev]"

# 또는 의존성만 설치
pip install -r requirements.txt
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
if result["ok"]:
    for stock in result["data"]:
        print(f"{stock['ticker']} - {stock['name']}")

# 종목 정보 조회
result = get_info(client, "005930")
if result["ok"]:
    print(f"Price: {result['data']['price']:,}")
    print(f"Market Cap: {result['data']['mcap']:,}")

# 수급 분석
result = analyze(client, "005930", days=30)
if result["ok"]:
    data = result["data"]
    print(f"Foreign 5d Net: {data['for_5d'][0]:,}")
    print(f"Institution 5d Net: {data['ins_5d'][0]:,}")

# 일봉 데이터
result = get_daily(client, "005930", days=10)
if result["ok"]:
    data = result["data"]
    for i, date in enumerate(data["dates"][:5]):
        print(f"{date}: Close={data['close'][i]:,}")
```

## API Reference

### Stock Search

```python
from stock_analyzer.stock.search import search, get_all, get_info, get_name

# 종목 검색 (이름 또는 코드)
search(client, "삼성")  # → {"ok": True, "data": [...]}

# 전체 종목 조회
get_all(client, market="0")  # 0:전체, 1:KOSPI, 2:KOSDAQ

# 종목 기본정보
get_info(client, "005930")  # → ticker, name, price, mcap, per, pbr

# 종목명 조회
get_name(client, "005930")  # → "삼성전자"
```

### Supply/Demand Analysis

```python
from stock_analyzer.stock.analysis import analyze, get_foreign_trend, get_institution_trend

# 수급 분석 (시계열)
analyze(client, "005930", days=180)
# → dates, mcap, for_5d, ins_5d

# 외국인 매매동향
get_foreign_trend(client, "005930")
# → net_buy, holding_qty, holding_ratio

# 기관 매매동향
get_institution_trend(client, "005930")
# → net_buy, finance, insurance, invest_trust
```

### OHLCV Data

```python
from stock_analyzer.stock.ohlcv import get_daily, get_weekly, get_monthly

# 일봉 데이터
get_daily(client, "005930", days=180)

# 주봉 데이터
get_weekly(client, "005930", weeks=52)

# 월봉 데이터
get_monthly(client, "005930", months=24)

# 날짜 범위 지정
get_daily(client, "005930", start_date="20250101", end_date="20250110")
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
| `TIMEOUT` | 요청 시간 초과 |

## Testing

```bash
# 단위 테스트 실행
python -m pytest tests/unit/ -v

# 커버리지 포함
python -m pytest tests/unit/ --cov=stock_analyzer

# 통합 테스트 (API 키 필요)
python -m pytest tests/integration/ -v

# E2E 테스트 (API 키 필요)
python -m pytest tests/e2e/ -v
```

## Project Structure

```
stock-analyzer/
├── pyproject.toml          # 프로젝트 설정
├── .env.example            # 환경변수 템플릿
├── README.md
│
├── src/stock_analyzer/
│   ├── __init__.py
│   ├── config.py           # 설정 관리
│   │
│   ├── core/               # 공통 유틸리티
│   │   ├── log.py          # 로깅
│   │   ├── http.py         # HTTP 클라이언트
│   │   ├── date.py         # 날짜 유틸
│   │   └── json_util.py    # JSON 헬퍼
│   │
│   ├── client/             # API 클라이언트
│   │   ├── auth.py         # OAuth 토큰 관리
│   │   └── kiwoom.py       # 키움 REST API
│   │
│   └── stock/              # 주식 데이터
│       ├── search.py       # 종목 검색
│       ├── analysis.py     # 수급 분석
│       └── ohlcv.py        # 가격 데이터
│
├── tests/
│   ├── conftest.py         # pytest 설정
│   ├── unit/               # 단위 테스트
│   ├── integration/        # 통합 테스트
│   └── e2e/                # E2E 테스트
│
└── scripts/
    └── run_analysis.py     # 샘플 스크립트
```

## Requirements

- Python >= 3.10
- pandas >= 2.0.0
- numpy >= 1.24.0
- requests >= 2.31.0
- python-dotenv >= 1.0.0

## License

MIT License

## Related

- [키움 REST API 가이드](https://openapi.kiwoom.com/guide/apiguide)
- [명세서](../docs/STOCK_APP_SPEC.md)
