# CLAUDE.md - Stock Analyzer Project

## Project Overview

키움증권 REST API를 활용한 주식 분석 도구. Python으로 데이터 수집/분석 로직을 검증한 후 Android 앱으로 통합 예정.

## Current Status

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 0 | ✅ Done | 프로젝트 설정, 키움 API 클라이언트 |
| Phase 1 | ✅ Done | 종목 검색, 수급 분석, OHLCV |
| Phase 2 | ⏳ Pending | 기술적 지표 (Trend, Elder, DeMark) |
| Phase 3 | ⏳ Pending | 차트 시각화 |
| Phase 4 | ⏳ Pending | 조건검색, 시장 지표 |

## Quick Commands

```bash
# 테스트 실행
cd stock-analyzer
python -m pytest tests/unit/ -v

# 전체 테스트 (API 키 필요)
python -m pytest tests/ -v

# 패키지 설치 (개발 모드)
pip install -e ".[dev]"

# 샘플 스크립트 실행 (.env 필요)
python scripts/run_analysis.py
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
│   ├── indicator/          # (Phase 2) 기술적 지표
│   ├── market/             # (Phase 4) 시장 지표
│   └── search/             # (Phase 4) 조건검색
├── tests/
│   ├── unit/               # 단위 테스트 (38개)
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

### 함수 호출 예시
```python
from stock_analyzer.client.kiwoom import KiwoomClient
from stock_analyzer.stock import search, analysis, ohlcv

# 클라이언트 생성
client = KiwoomClient(app_key, secret_key, base_url)

# 종목 검색
result = search.search(client, "삼성전자")

# 수급 분석
result = analysis.analyze(client, "005930", days=180)

# OHLCV 데이터
result = ohlcv.get_daily(client, "005930", days=30)
```

## Kiwoom API Reference

| API ID | 기능 | 모듈 |
|--------|------|------|
| au10001 | 토큰 발급 | client/auth.py |
| ka10099 | 종목 리스트 | stock/search.py |
| ka10001 | 주식 기본정보 | stock/search.py |
| ka10008 | 외국인 매매동향 | stock/analysis.py |
| ka10059 | 투자자별 매매 | stock/analysis.py |
| ka10081 | 일봉 차트 | stock/ohlcv.py |
| ka10082 | 주봉 차트 | stock/ohlcv.py |
| ka10083 | 월봉 차트 | stock/ohlcv.py |

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
- 모든 함수는 `{"ok": bool, "data/error": ...}` 형식 반환
- 토큰은 자동 갱신됨 (AuthClient.get_token)
- 테스트는 mock 클라이언트 사용 (실제 API 호출 없음)

## Spec Document

상세 명세서: `docs/STOCK_APP_SPEC.md`
