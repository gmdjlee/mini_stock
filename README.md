# Mini Stock

키움증권 REST API 기반 주식 분석 애플리케이션

## Overview

EtfMonitor의 종목 메뉴 기능을 독립적인 경량 앱으로 분리하여 개발하는 프로젝트입니다.
Python으로 데이터 수집 로직을 먼저 검증한 후, Android 앱으로 통합합니다.

## Project Structure

```
mini_stock/
├── docs/
│   └── STOCK_APP_SPEC.md   # 개발 명세서
├── stock-analyzer/          # Python 라이브러리
│   ├── src/stock_analyzer/
│   ├── tests/
│   └── README.md
├── CLAUDE.md               # Claude Code 가이드
└── README.md
```

## Development Phases

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 0 | 프로젝트 설정, API 클라이언트 | ✅ Complete |
| Phase 1 | 종목 검색, 수급 분석, OHLCV | ✅ Complete |
| Phase 2 | 기술적 지표 (Trend, Elder, DeMark) | ⏳ Pending |
| Phase 3 | 차트 시각화 | ⏳ Pending |
| Phase 4 | 조건검색, 시장 지표 | ⏳ Pending |

## Features

### Implemented (Phase 0-1)
- ✅ 키움 REST API OAuth 인증
- ✅ 종목 검색 (이름/코드)
- ✅ 주식 기본정보 조회
- ✅ 외국인/기관 수급 분석
- ✅ 일/주/월봉 OHLCV 데이터

### Planned (Phase 2+)
- ⏳ Trend Signal (MA, CMF, Fear/Greed)
- ⏳ Elder Impulse (EMA13, MACD)
- ⏳ DeMark TD Setup
- ⏳ 캔들/라인 차트
- ⏳ HTS 조건검색
- ⏳ 예탁금, 신용잔고 추이

## Quick Start

### Using uv (Recommended)

```bash
# 1. uv 설치
curl -LsSf https://astral.sh/uv/install.sh | sh

# 2. 의존성 설치 및 테스트 실행
cd stock-analyzer
uv venv && source .venv/bin/activate
uv pip install -e ".[dev]"

# 3. 환경변수 설정
cp .env.example .env
# .env 파일에 키움 API 키 입력

# 4. 테스트 실행
uv run pytest tests/unit/ -v

# 5. 샘플 스크립트 실행
uv run python scripts/run_analysis.py
```

### Using pip

```bash
# 1. Python 라이브러리 설치
cd stock-analyzer
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"

# 2. 환경변수 설정
cp .env.example .env
# .env 파일에 키움 API 키 입력

# 3. 테스트 실행
python -m pytest tests/unit/ -v

# 4. 샘플 스크립트 실행
python scripts/run_analysis.py
```

## Documentation

- **명세서**: [docs/STOCK_APP_SPEC.md](docs/STOCK_APP_SPEC.md)
- **Claude 가이드**: [CLAUDE.md](CLAUDE.md)
- **Python 라이브러리**: [stock-analyzer/README.md](stock-analyzer/README.md)

## Tech Stack

### Python (stock-analyzer)
- Python 3.10+
- [uv](https://github.com/astral-sh/uv) - 빠른 패키지 매니저
- pandas, numpy
- requests
- pytest

### App (planned)
- Kotlin
- Jetpack Compose
- Hilt DI
- Room DB
- Chaquopy (Python Bridge)

## Kiwoom REST API

| 기능 | API ID | Description |
|------|--------|-------------|
| 인증 | au10001 | 접근토큰 발급 |
| 종목 | ka10099, ka10001 | 종목 리스트, 기본정보 |
| 수급 | ka10008, ka10059 | 외국인/기관 매매동향 |
| 차트 | ka10081~83 | 일/주/월봉 데이터 |
| 조건 | ka10171, ka10172 | 조건검색 |

## License

MIT License
