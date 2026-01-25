# Mini Stock

키움증권 REST API 기반 주식 분석 애플리케이션

## Overview

EtfMonitor의 종목 메뉴 기능을 독립적인 경량 앱으로 분리하여 개발하는 프로젝트입니다.
Python으로 데이터 수집 로직을 먼저 검증한 후, Android 앱으로 통합합니다.

## Project Structure

```
mini_stock/
├── docs/                    # 문서
│   ├── STOCK_APP_SPEC.md    # 개발 명세서
│   ├── ANDROID_PREPARATION.md  # Android 개발 가이드
│   ├── CODE_REVIEW_REPORT.md   # 코드 리뷰 보고서
│   └── kiwoom_api_docs/     # 키움 API 레퍼런스
├── stock-analyzer/          # Python 라이브러리 (🔒 FROZEN)
│   ├── src/stock_analyzer/
│   ├── tests/
│   └── README.md
├── StockApp/                # Android 앱 (🚀 ACTIVE)
│   ├── app/
│   └── README.md
├── etf-collector/           # ETF 수집 도구
│   ├── src/etf_collector/
│   └── README.md
├── CLAUDE.md               # Claude Code 가이드
└── README.md
```

## Development Status

### Python Package (stock-analyzer) 🔒 FROZEN

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 0 | 프로젝트 설정, API 클라이언트 | ✅ Complete |
| Phase 1 | 종목 검색, 수급 분석, OHLCV | ✅ Complete |
| Phase 2 | 기술적 지표 (Trend, Elder, DeMark) | ✅ Complete |
| Phase 3 | 차트 시각화 (Candle, Line, Bar) | ✅ Complete |
| Phase 4 | 조건검색, 시장 지표 | ✅ Complete |
| Phase 5 | 수급 오실레이터 | ✅ Complete |

**테스트**: 168개 (모두 통과)
**코드**: ~6,200 lines (29 Python 파일)

> ⚠️ Python 패키지는 동결 상태입니다. Android 앱 참조용으로만 사용됩니다.

### Android App (StockApp) 🚀 ACTIVE

| Phase | Description | Status |
|-------|-------------|--------|
| App Phase 0 | 프로젝트 설정, Chaquopy 통합 | ✅ Complete |
| App Phase 1 | 종목 검색, 수급 분석 화면 | ✅ Complete |
| App Phase 2 | 기술적 지표 화면 (Vico Charts) | ✅ Complete |
| App Phase 3 | ~~시장 지표, 조건검색~~ | ⛔ Removed |
| App Phase 4 | 설정 화면 (API 키, 투자 모드) | ✅ Complete |
| App Phase 5 | 자동 스케줄링 (WorkManager) | ✅ Complete |
| App Phase 6 | 순위정보 (Kotlin REST API) | ✅ Complete |

**코드**: 91 files, ~13,697 lines (Kotlin)

> 🚀 Android 앱이 현재 활성 개발 대상입니다.

### ETF Collector ✅ COMPLETE

ETF 구성종목 수집 도구 (KIS API + Kiwoom API)
- 91개 테스트 통과
- Android/Chaquopy 통합 API 지원

## Features

### Python (stock-analyzer)
- ✅ 키움 REST API OAuth 인증
- ✅ 종목 검색 (이름/코드)
- ✅ 외국인/기관 수급 분석
- ✅ 일/주/월봉 OHLCV 데이터
- ✅ 기술적 지표 (Trend, Elder, DeMark)
- ✅ 수급 오실레이터 (MACD 스타일)
- ✅ 차트 시각화

### Android (StockApp)
- ✅ 종목 검색 (자동완성, 히스토리)
- ✅ 수급 분석 (시가총액, 외국인/기관)
- ✅ 기술적 지표 차트 (Vico)
- ✅ 순위정보 (호가잔량, 거래량, 신용비율 등)
- ✅ API 키 설정 (암호화 저장)
- ✅ 자동 스케줄링 (WorkManager)

## Quick Start

### Python (stock-analyzer)

```bash
cd stock-analyzer
uv sync --all-extras
cp .env.example .env
# .env 파일에 키움 API 키 입력
uv run pytest tests/unit/ -v
```

### Android (StockApp)

```bash
cd StockApp
./gradlew build
./gradlew installDebug
```

## Documentation

- **Claude 가이드**: [CLAUDE.md](CLAUDE.md)
- **명세서**: [docs/STOCK_APP_SPEC.md](docs/STOCK_APP_SPEC.md)
- **Android 가이드**: [docs/ANDROID_PREPARATION.md](docs/ANDROID_PREPARATION.md)
- **코드 리뷰**: [docs/CODE_REVIEW_REPORT.md](docs/CODE_REVIEW_REPORT.md)
- **Python 라이브러리**: [stock-analyzer/README.md](stock-analyzer/README.md)
- **Android 앱**: [StockApp/README.md](StockApp/README.md)
- **ETF Collector**: [etf-collector/README.md](etf-collector/README.md)

## Tech Stack

### Python (stock-analyzer)
- Python 3.10+
- [uv](https://github.com/astral-sh/uv) - 빠른 패키지 매니저
- pandas, numpy, requests
- matplotlib, mplfinance

### Android (StockApp)
- Kotlin 2.1.0
- Jetpack Compose (BOM 2024.12)
- Hilt DI (2.54)
- Room DB (2.8.3)
- Vico Charts (2.0.0)
- Chaquopy (15.0.1) - Python Bridge
- WorkManager - 백그라운드 스케줄링

## Kiwoom REST API

| 기능 | API ID | Description |
|------|--------|-------------|
| 인증 | au10001 | 접근토큰 발급 |
| 종목 | ka10099, ka10001 | 종목 리스트, 기본정보 |
| 수급 | ka10008, ka10059 | 외국인/기관 매매동향 |
| 차트 | ka10081~83 | 일/주/월봉 데이터 |
| 순위 | ka10021, ka10023, ka10030, ka10033, ka90009 | 호가잔량, 거래량, 신용비율, 외국인/기관 |

## License

MIT License
