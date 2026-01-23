# Stock Menu App - 개발 명세서

**Version**: 2.0  
**Created**: 2026-01-11  
**Data Source**: 키움증권 REST API  
**Based on**: EtfMonitor Stock Feature

---

## 1. 프로젝트 개요

### 1.1 목표

EtfMonitor의 **종목 메뉴** 기능을 독립적인 경량 앱으로 분리하여 개발.  
Python 단일 프로젝트에서 데이터 수집 로직을 먼저 검증한 후, Android 앱으로 통합.

### 1.2 데이터 소스: 키움 REST API

| 항목 | 내용 |
|------|------|
| API 개수 | 200개 이상 |
| 수급 데이터 | 외국인/기관 순매수, 투자자별 매매 |
| 조건검색 | HTS 조건검색 API 지원 |
| 프로그램매매 | 프로그램 순매수 상위 종목 |
| 모의투자 | 모의투자 서버 지원 |
| 실시간 시세 | WebSocket 지원 |

**선정 이유**: 키움 REST API는 조건검색, 프로그램매매 등 다양한 기능을 제공하며, 200개 이상의 API를 통해 확장성이 높음.

### 1.3 핵심 기능

| # | 기능 | 설명 | 키움 API | 우선순위 |
|---|------|------|----------|----------|
| 1 | 종목 검색 | 이름/코드로 종목 검색 | ka10099, ka10100 | P0 |
| 2 | 수급 분석 | 시가총액, 외국인/기관 순매수 | ka10001, ka10008, ka10059 | P0 |
| 3 | 기술적 지표 | Trend Signal, Elder Impulse, DeMark TD | ka10081~ka10083 | P1 |
| 4 | OHLCV 차트 | 일/주봉 캔들 차트 | ka10081, ka10082 | P1 |
| 5 | 시장 지표 | 예탁금, 신용잔고 추이 | kt00001, ka10013 | P2 |

### 1.4 개발 원칙

```
✓ 기능 하나씩 구현 → 테스트 → 검증 → 다음 기능
✓ Python 먼저 완성 → 앱에 통합
✓ 클린 아키텍처 (Domain/Data/Presentation)
✓ 간결한 네이밍 (명확하지만 최소 길이)
✓ 에러 추적 용이한 구조
✓ Claude Code 개발 환경 최적화
```

---

## 2. 키움 REST API 개요

### 2.1 API 기본 정보

| 항목 | 내용 |
|------|------|
| 운영 도메인 | `https://api.kiwoom.com` |
| 모의투자 도메인 | `https://mockapi.kiwoom.com` |
| 인증 방식 | OAuth 2.0 (Bearer Token) |
| 데이터 포맷 | JSON |
| Content-Type | `application/json;charset=UTF-8` |

### 2.2 API 신청 절차

1. 키움증권 계좌 개설 (위탁 종합, ISA, 연금저축 등)
2. HTS ID 연결
3. 홈페이지 로그인 → [트레이딩 채널] → [키움 REST API]
4. API 사용 등록 후 앱키(App Key) 및 시크릿키(Secret Key) 발급

> ⚠️ 모바일에서는 신청 불가, PC에서만 가능

### 2.3 인증 흐름

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Client    │      │ Kiwoom API  │      │   Server    │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │   POST /oauth2/token           │
       │   {appkey, secretkey}          │
       │ ─────────────────────────────► │
       │                                │
       │   {token, expires_dt}          │
       │ ◄───────────────────────────── │
       │                                │
       │   API Request                  │
       │   Header: authorization        │
       │ ─────────────────────────────► │
       │                                │
       │   Response                     │
       │ ◄───────────────────────────── │
```

### 2.4 토큰 발급 (au10001)

**Request**
```json
{
  "grant_type": "client_credentials",
  "appkey": "AxserEsdcredca.....",
  "secretkey": "SEefdcwcforehDre2fdvc...."
}
```

**Response**
```json
{
  "expires_dt": "20261107083713",
  "token_type": "bearer",
  "token": "WQJCwyqInphKnR3bSRtB9NE1lv...",
  "return_code": 0,
  "return_msg": "정상적으로 처리되었습니다"
}
```

---

## 3. 기능별 API 매핑

### 3.1 종목 검색 (Phase 1)

| 기능 | API ID | API 명 | URL |
|------|--------|--------|-----|
| 종목 리스트 | ka10099 | 종목정보 리스트 | /api/dostk/stkinfo |
| 종목 검색 | ka10100 | 종목정보 조회 | /api/dostk/stkinfo |
| 업종 코드 | ka10101 | 업종코드 리스트 | /api/dostk/stkinfo |

### 3.2 수급 분석 (Phase 1)

| 기능 | API ID | API 명 | URL |
|------|--------|--------|-----|
| 주식 기본정보 (시총) | ka10001 | 주식기본정보요청 | /api/dostk/stkinfo |
| 외국인 매매동향 | ka10008 | 주식외국인종목별매매동향 | /api/dostk/frgnistt |
| 기관 매매추이 | ka10045 | 종목별기관매매추이요청 | /api/dostk/stkinfo |
| 투자자별 매매 | ka10059 | 종목별투자자기관별요청 | /api/dostk/stkinfo |
| 투자자별 합계 | ka10061 | 종목별투자자기관별합계요청 | /api/dostk/stkinfo |

### 3.3 기술적 지표 (Phase 2)

| 기능 | API ID | API 명 | URL |
|------|--------|--------|-----|
| 일봉 차트 | ka10081 | 주식일봉차트조회요청 | /api/dostk/chart |
| 주봉 차트 | ka10082 | 주식주봉차트조회요청 | /api/dostk/chart |
| 월봉 차트 | ka10083 | 주식월봉차트조회요청 | /api/dostk/chart |
| 분봉 차트 | ka10080 | 주식분봉차트조회요청 | /api/dostk/chart |
| 틱 차트 | ka10079 | 주식틱차트조회요청 | /api/dostk/chart |

### 3.4 ETF 데이터 (Phase 2)

| 기능 | API ID | API 명 | URL |
|------|--------|--------|-----|
| ETF 전체시세 | ka40004 | ETF전체시세요청 | /api/dostk/etf |
| ETF 일별추이 | ka40003 | ETF일별추이요청 | /api/dostk/etf |
| ETF 종목정보 | ka40002 | ETF종목정보요청 | /api/dostk/etf |

### 3.5 추가 기능 (Phase 3+)

| 기능 | API ID | API 명 |
|------|--------|--------|
| 조건검색 목록 | ka10171 | 조건검색 목록조회 |
| 조건검색 실행 | ka10172 | 조건검색 요청 일반 |
| 프로그램 순매수 | ka90003 | 프로그램순매수상위50요청 |
| 대차거래 추이 | ka10068 | 대차거래추이요청 |
| 신용매매 동향 | ka10013 | 신용매매동향요청 |

---

## 4. 개발 Phase

### Phase 0: 프로젝트 설정 (Day 1)

```
[Python]                          [App]
├── 프로젝트 구조 생성             ├── Android 프로젝트 생성
├── 가상환경 설정                  ├── Gradle 설정 (Chaquopy)
├── 키움 API 클라이언트            ├── Hilt DI 설정
└── 테스트 프레임워크              └── Room DB 설정
```

### Phase 1: 종목 검색 + 수급 분석 (Core)

```
[Python] 
├── client/kiwoom.py         # 키움 API 클라이언트
├── client/auth.py           # OAuth 토큰 관리
├── stock/search.py          # 종목 검색
├── stock/analysis.py        # 수급 분석 (시총, 외인/기관)
└── stock/ohlcv.py           # 가격 데이터

[App]
├── domain/
│   ├── model/Stock.kt       # Stock, StockData
│   ├── repo/StockRepo.kt    # Repository interface
│   └── usecase/SearchStock.kt
├── data/
│   └── StockRepoImpl.kt
└── ui/
    ├── SearchScreen.kt
    └── AnalysisScreen.kt
```

### Phase 2: 기술적 지표 (Technical)

```
[Python] 
├── indicator/trend.py       # Trend Signal (MA, CMF, Fear/Greed)
├── indicator/elder.py       # Elder Impulse (EMA13, MACD)
└── indicator/demark.py      # DeMark TD Setup

[App]
├── domain/model/Indicator.kt    # TrendSignal, Elder, DeMark
├── ui/IndicatorScreen.kt        # 지표 화면 (탭 구조)
└── ui/component/ChartCard.kt
```

### Phase 3: 차트 시각화 (Chart)

```
[App]
├── ui/chart/
│   ├── CandleChart.kt       # OHLCV 캔들
│   ├── LineChart.kt         # 라인 차트
│   └── BarChart.kt          # 바 차트
└── ui/component/DateRange.kt
```

### Phase 4: 시장 지표 + 조건검색 (Market)

```
[Python]
├── market/deposit.py        # 예탁금, 신용잔고
└── search/condition.py      # 조건검색

[App]
├── domain/model/Deposit.kt
├── ui/DepositScreen.kt
└── ui/ConditionScreen.kt
```

### Phase 5: 시가총액 & 수급 오실레이터 (Oscillator)

```
[Python]
├── indicator/oscillator.py  # 수급 오실레이터 계산
│   ├── calc()               # 오실레이터 계산 (Supply Ratio MACD)
│   ├── analyze_signal()     # 매매 신호 분석
│   └── get_signal_score()   # 신호 점수 (-100 ~ +100)
└── chart/oscillator.py      # 오실레이터 차트
    └── plot()               # 듀얼 축 차트 (시가총액 + 오실레이터)

[App]
├── domain/model/Oscillator.kt
├── ui/OscillatorScreen.kt
└── ui/component/OscillatorChart.kt
```

**핵심 계산:**
```python
# Supply Ratio = (외국인 순매수 + 기관 순매수) / 시가총액
supply_ratio = (foreign_5d + institution_5d) / market_cap

# MACD 스타일 오실레이터
ema12 = EMA(supply_ratio, 12)
ema26 = EMA(supply_ratio, 26)
macd = ema12 - ema26
signal = EMA(macd, 9)
oscillator = macd - signal  # Histogram
```

**신호 점수:**
| 항목 | 점수 범위 | 설명 |
|------|----------|------|
| 오실레이터 값 | ±40 | >0.5%: +40, >0.2%: +20, <-0.5%: -40 |
| MACD 크로스 | ±30 | 골든크로스: +30, 데드크로스: -30 |
| 히스토그램 추세 | ±30 | 상승 지속: +30, 하락 지속: -30 |

**매매 신호:**
| Score | Signal | 설명 |
|-------|--------|------|
| >= 60 | STRONG_BUY | 강력 매수 |
| >= 20 | BUY | 매수 |
| -20 ~ 20 | NEUTRAL | 중립 |
| <= -20 | SELL | 매도 |
| <= -60 | STRONG_SELL | 강력 매도 |

---

## 5. Python 프로젝트 구조

### 5.1 디렉토리 구조

```
stock-analyzer/
├── pyproject.toml           # 프로젝트 설정
├── README.md
├── .env.example             # 키움 API 키 템플릿
│
├── src/
│   └── stock_analyzer/
│       ├── __init__.py
│       ├── config.py        # 설정 (API 키, 상수)
│       │
│       ├── core/            # 공통 유틸
│       │   ├── __init__.py
│       │   ├── log.py       # 로거
│       │   ├── http.py      # HTTP 클라이언트
│       │   ├── date.py      # 날짜 유틸
│       │   └── json.py      # JSON 헬퍼
│       │
│       ├── client/          # 키움 API 클라이언트
│       │   ├── __init__.py
│       │   ├── kiwoom.py    # 키움 REST API 래퍼
│       │   └── auth.py      # OAuth 토큰 관리
│       │
│       ├── stock/           # 종목 데이터
│       │   ├── __init__.py
│       │   ├── search.py    # 검색
│       │   ├── analysis.py  # 수급 분석
│       │   └── ohlcv.py     # 가격 데이터
│       │
│       ├── indicator/       # 기술적 지표
│       │   ├── __init__.py
│       │   ├── trend.py     # Trend Signal
│       │   ├── elder.py     # Elder Impulse
│       │   ├── demark.py    # DeMark TD
│       │   └── oscillator.py # 수급 오실레이터 (Phase 5)
│       │
│       ├── chart/           # 차트 시각화
│       │   ├── __init__.py
│       │   ├── candle.py    # 캔들스틱 차트
│       │   ├── line.py      # 라인 차트
│       │   ├── bar.py       # 바 차트
│       │   └── oscillator.py # 오실레이터 차트 (Phase 5)
│       │
│       ├── market/          # 시장 지표
│       │   ├── __init__.py
│       │   └── deposit.py   # 예탁금
│       │
│       └── search/          # 조건검색
│           ├── __init__.py
│           └── condition.py # HTS 조건검색
│
├── tests/                   # 테스트
│   ├── conftest.py
│   ├── test_auth.py
│   ├── test_search.py
│   ├── test_analysis.py
│   └── test_indicator.py
│
└── scripts/                 # CLI/유틸 스크립트
    └── run_analysis.py
```

### 5.2 환경 변수 설정

```bash
# .env.example
KIWOOM_APP_KEY=your_app_key_here
KIWOOM_SECRET_KEY=your_secret_key_here
KIWOOM_BASE_URL=https://api.kiwoom.com      # 운영
# KIWOOM_BASE_URL=https://mockapi.kiwoom.com  # 모의투자
```

### 5.3 핵심 모듈 명세

#### 5.3.1 client/auth.py

```python
"""키움 OAuth 토큰 관리."""
from dataclasses import dataclass
from datetime import datetime
from typing import Optional
import requests

@dataclass
class TokenInfo:
    token: str
    expires_dt: datetime
    token_type: str = "bearer"

    @property
    def is_expired(self) -> bool:
        return datetime.now() >= self.expires_dt

    @property
    def bearer(self) -> str:
        return f"Bearer {self.token}"


class AuthClient:
    """OAuth 토큰 발급 및 관리."""

    def __init__(self, app_key: str, secret_key: str, base_url: str):
        self.app_key = app_key
        self.secret_key = secret_key
        self.base_url = base_url
        self._token: Optional[TokenInfo] = None

    def get_token(self, force_refresh: bool = False) -> TokenInfo:
        """토큰 반환 (필요시 자동 갱신)."""
        if force_refresh or self._token is None or self._token.is_expired:
            self._token = self._fetch_token()
        return self._token

    def _fetch_token(self) -> TokenInfo:
        """토큰 발급 (au10001)."""
        resp = requests.post(
            f"{self.base_url}/oauth2/token",
            headers={
                "api-id": "au10001",
                "Content-Type": "application/json;charset=UTF-8"
            },
            json={
                "grant_type": "client_credentials",
                "appkey": self.app_key,
                "secretkey": self.secret_key
            }
        )
        resp.raise_for_status()
        data = resp.json()

        if data.get("return_code") != 0:
            raise AuthError(data.get("return_msg", "토큰 발급 실패"))

        expires_dt = datetime.strptime(data["expires_dt"], "%Y%m%d%H%M%S")
        return TokenInfo(
            token=data["token"],
            expires_dt=expires_dt,
            token_type=data.get("token_type", "bearer")
        )


class AuthError(Exception):
    """인증 오류."""
    pass
```

#### 5.3.2 client/kiwoom.py

```python
"""키움 REST API 클라이언트."""
from dataclasses import dataclass
from typing import Any, Dict, List, Optional
import requests
from .auth import AuthClient, TokenInfo

@dataclass
class ApiResponse:
    ok: bool
    data: Optional[Any] = None
    error: Optional[Dict[str, str]] = None
    has_next: bool = False
    next_key: Optional[str] = None


class KiwoomClient:
    """키움 REST API 래퍼."""

    def __init__(
        self,
        app_key: str,
        secret_key: str,
        base_url: str = "https://api.kiwoom.com"
    ):
        self.base_url = base_url
        self.auth = AuthClient(app_key, secret_key, base_url)

    def _call(
        self,
        api_id: str,
        url: str,
        body: Dict[str, Any],
        cont_yn: str = "",
        next_key: str = ""
    ) -> ApiResponse:
        """API 호출."""
        token = self.auth.get_token()

        headers = {
            "api-id": api_id,
            "authorization": token.bearer,
            "Content-Type": "application/json;charset=UTF-8"
        }
        if cont_yn:
            headers["cont-yn"] = cont_yn
        if next_key:
            headers["next-key"] = next_key

        try:
            resp = requests.post(
                f"{self.base_url}{url}",
                headers=headers,
                json=body,
                timeout=30
            )
            resp.raise_for_status()
            data = resp.json()

            # 응답 헤더에서 연속조회 정보 추출
            has_next = resp.headers.get("cont-yn", "N") == "Y"
            next_key = resp.headers.get("next-key", "")

            if data.get("return_code", 0) != 0:
                return ApiResponse(
                    ok=False,
                    error={
                        "code": str(data.get("return_code")),
                        "msg": data.get("return_msg", "Unknown error")
                    }
                )

            return ApiResponse(
                ok=True,
                data=data,
                has_next=has_next,
                next_key=next_key
            )

        except requests.RequestException as e:
            return ApiResponse(
                ok=False,
                error={"code": "NETWORK_ERROR", "msg": str(e)}
            )

    # ========== 종목 검색 ==========

    def get_stock_list(self, market: str = "0") -> ApiResponse:
        """
        종목 리스트 조회 (ka10099).

        Args:
            market: 시장구분 (0:전체, 1:KOSPI, 2:KOSDAQ)
        """
        return self._call("ka10099", "/api/dostk/stkinfo", {
            "mrkt_tp": market
        })

    def get_stock_info(self, ticker: str) -> ApiResponse:
        """
        종목 기본정보 조회 (ka10001).

        Returns: 종목명, 현재가, 시가총액, PER, PBR 등
        """
        return self._call("ka10001", "/api/dostk/stkinfo", {
            "stk_cd": ticker
        })

    # ========== 수급 분석 ==========

    def get_foreign_trend(self, ticker: str) -> ApiResponse:
        """
        외국인 종목별 매매동향 (ka10008).

        Returns: 외국인 순매수, 보유수량, 보유비율 등
        """
        return self._call("ka10008", "/api/dostk/frgnistt", {
            "stk_cd": ticker
        })

    def get_institution_trend(self, ticker: str) -> ApiResponse:
        """
        기관 매매추이 (ka10045).

        Returns: 기관 순매수, 기관별 상세 (금융투자, 보험, 투신 등)
        """
        return self._call("ka10045", "/api/dostk/stkinfo", {
            "stk_cd": ticker
        })

    def get_investor_trend(self, ticker: str, period: str = "1") -> ApiResponse:
        """
        종목별 투자자 기관별 요청 (ka10059).

        Args:
            ticker: 종목코드
            period: 기간 (1:일별, 2:주별, 3:월별)
        """
        return self._call("ka10059", "/api/dostk/stkinfo", {
            "stk_cd": ticker,
            "inq_cnd": period
        })

    # ========== 차트 데이터 ==========

    def get_daily_chart(
        self,
        ticker: str,
        start_date: str,
        end_date: str,
        adj_price: str = "1"
    ) -> ApiResponse:
        """
        일봉 차트 조회 (ka10081).

        Args:
            ticker: 종목코드
            start_date: 시작일 (YYYYMMDD)
            end_date: 종료일 (YYYYMMDD)
            adj_price: 수정주가 여부 (0:미적용, 1:적용)
        """
        return self._call("ka10081", "/api/dostk/chart", {
            "stk_cd": ticker,
            "strt_dt": start_date,
            "end_dt": end_date,
            "adj_prc_tp": adj_price
        })

    def get_weekly_chart(
        self,
        ticker: str,
        start_date: str,
        end_date: str,
        adj_price: str = "1"
    ) -> ApiResponse:
        """
        주봉 차트 조회 (ka10082).
        """
        return self._call("ka10082", "/api/dostk/chart", {
            "stk_cd": ticker,
            "strt_dt": start_date,
            "end_dt": end_date,
            "adj_prc_tp": adj_price
        })

    def get_monthly_chart(
        self,
        ticker: str,
        start_date: str,
        end_date: str,
        adj_price: str = "1"
    ) -> ApiResponse:
        """
        월봉 차트 조회 (ka10083).
        """
        return self._call("ka10083", "/api/dostk/chart", {
            "stk_cd": ticker,
            "strt_dt": start_date,
            "end_dt": end_date,
            "adj_prc_tp": adj_price
        })

    # ========== ETF ==========

    def get_etf_list(self) -> ApiResponse:
        """ETF 전체시세 (ka40004)."""
        return self._call("ka40004", "/api/dostk/etf", {})

    def get_etf_daily(self, ticker: str) -> ApiResponse:
        """ETF 일별추이 (ka40003)."""
        return self._call("ka40003", "/api/dostk/etf", {
            "stk_cd": ticker
        })

    # ========== 조건검색 ==========

    def get_condition_list(self) -> ApiResponse:
        """조건검색 목록 조회 (ka10171)."""
        return self._call("ka10171", "/api/dostk/cond", {})

    def search_condition(self, cond_idx: str, cond_name: str) -> ApiResponse:
        """
        조건검색 실행 (ka10172).

        Args:
            cond_idx: 조건검색 인덱스
            cond_name: 조건검색 명
        """
        return self._call("ka10172", "/api/dostk/cond", {
            "cond_idx": cond_idx,
            "cond_nm": cond_name
        })
```

#### 5.3.3 stock/search.py

```python
"""종목 검색 기능."""
from typing import List, Optional
from dataclasses import dataclass
from ..client.kiwoom import KiwoomClient

@dataclass
class StockInfo:
    ticker: str
    name: str
    market: str  # KOSPI/KOSDAQ

def search(client: KiwoomClient, query: str) -> dict:
    """
    종목 검색.

    Args:
        client: 키움 API 클라이언트
        query: 검색어 (이름 또는 코드)

    Returns:
        {
            "ok": True,
            "data": [
                {"ticker": "005930", "name": "삼성전자", "market": "KOSPI"},
                ...
            ]
        }
    """
    if not query or not query.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "검색어가 필요합니다"}
        }

    # 전체 종목 리스트 조회
    resp = client.get_stock_list()
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    # 검색어로 필터링
    query = query.strip().upper()
    results = []

    for item in resp.data.get("stk_list", []):
        ticker = item.get("stk_cd", "")
        name = item.get("stk_nm", "")

        if query in ticker or query in name.upper():
            results.append({
                "ticker": ticker,
                "name": name,
                "market": _get_market_name(item.get("mrkt_tp", ""))
            })

    return {"ok": True, "data": results[:50]}  # 최대 50개


def get_all(client: KiwoomClient, market: str = "0") -> dict:
    """
    전체 종목 리스트.

    Args:
        client: 키움 API 클라이언트
        market: 시장구분 (0:전체, 1:KOSPI, 2:KOSDAQ)
    """
    resp = client.get_stock_list(market)
    if not resp.ok:
        return {"ok": False, "error": resp.error}

    results = []
    for item in resp.data.get("stk_list", []):
        results.append({
            "ticker": item.get("stk_cd", ""),
            "name": item.get("stk_nm", ""),
            "market": _get_market_name(item.get("mrkt_tp", ""))
        })

    return {"ok": True, "data": results}


def get_name(client: KiwoomClient, ticker: str) -> Optional[str]:
    """종목명 조회."""
    resp = client.get_stock_info(ticker)
    if resp.ok:
        return resp.data.get("stk_nm")
    return None


def _get_market_name(market_tp: str) -> str:
    """시장구분 코드 → 명칭."""
    return {"1": "KOSPI", "2": "KOSDAQ"}.get(market_tp, "기타")
```

#### 5.3.4 stock/analysis.py

```python
"""수급 분석 기능."""
from dataclasses import dataclass
from typing import List, Dict, Any
from ..client.kiwoom import KiwoomClient

@dataclass
class StockData:
    ticker: str
    name: str
    dates: List[str]
    mcap: List[int]       # 시가총액
    for_5d: List[int]     # 외국인 5일 순매수
    ins_5d: List[int]     # 기관 5일 순매수


def analyze(client: KiwoomClient, ticker: str, days: int = 180) -> dict:
    """
    수급 분석.

    Args:
        client: 키움 API 클라이언트
        ticker: 종목 코드
        days: 조회 기간 (일)

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "name": "삼성전자",
                "dates": ["2025-01-02", ...],
                "mcap": [380000000000000, ...],
                "for_5d": [1500000000, ...],
                "ins_5d": [-500000000, ...]
            }
        }

    Errors:
        - INVALID_ARG: 잘못된 인자
        - TICKER_NOT_FOUND: 종목 없음
        - NO_DATA: 데이터 없음
        - API_ERROR: API 호출 오류
    """
    if not ticker or not ticker.strip():
        return {
            "ok": False,
            "error": {"code": "INVALID_ARG", "msg": "종목코드가 필요합니다"}
        }

    ticker = ticker.strip()

    # 1. 기본 정보 조회
    info_resp = client.get_stock_info(ticker)
    if not info_resp.ok:
        return {"ok": False, "error": info_resp.error}

    name = info_resp.data.get("stk_nm", ticker)
    mcap = info_resp.data.get("mrkt_tot_amt", 0)

    # 2. 투자자별 매매동향 조회
    trend_resp = client.get_investor_trend(ticker)
    if not trend_resp.ok:
        return {"ok": False, "error": trend_resp.error}

    # API 응답 필드명: stk_invsr_orgn (실제 API) 또는 list (대체)
    trend_data = trend_resp.data.get("stk_invsr_orgn", []) or trend_resp.data.get("list", [])
    if not trend_data:
        return {
            "ok": False,
            "error": {"code": "NO_DATA", "msg": "수급 데이터가 없습니다"}
        }

    # 3. 데이터 파싱
    dates = []
    mcaps = []
    for_5d = []
    ins_5d = []

    for item in trend_data[:days]:
        dates.append(item.get("dt", ""))
        mcaps.append(int(item.get("mrkt_tot_amt", 0)))
        # API 필드명: frgnr_invsr (외국인), orgn (기관)
        for_5d.append(int(item.get("frgnr_invsr", 0)))
        ins_5d.append(int(item.get("orgn", 0)))

    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "name": name,
            "dates": dates,
            "mcap": mcaps,
            "for_5d": for_5d,
            "ins_5d": ins_5d
        }
    }
```

#### 5.3.5 indicator/oscillator.py (Phase 5)

```python
"""시가총액 & 수급 오실레이터 (MACD 스타일)."""
from dataclasses import dataclass
from typing import List, Optional
from enum import Enum
from ..client.kiwoom import KiwoomClient
from ..stock import analysis


class SignalType(Enum):
    STRONG_BUY = "STRONG_BUY"
    BUY = "BUY"
    NEUTRAL = "NEUTRAL"
    SELL = "SELL"
    STRONG_SELL = "STRONG_SELL"


@dataclass
class OscillatorResult:
    ticker: str
    name: str
    dates: List[str]
    market_cap: List[float]      # 시가총액 (정규화)
    supply_ratio: List[float]    # 수급 비율
    ema12: List[float]           # Supply Ratio EMA12
    ema26: List[float]           # Supply Ratio EMA26
    macd: List[float]            # MACD (ema12 - ema26)
    signal: List[float]          # Signal Line (EMA9 of MACD)
    oscillator: List[float]      # Oscillator (MACD - Signal)


@dataclass
class SignalAnalysis:
    total_score: int             # -100 ~ +100
    signal_type: SignalType
    oscillator_score: int        # ±40
    cross_score: int             # ±30
    trend_score: int             # ±30
    description: str


def calc(client: KiwoomClient, ticker: str, days: int = 180) -> dict:
    """
    수급 오실레이터 계산.

    Args:
        client: 키움 API 클라이언트
        ticker: 종목코드
        days: 조회 기간 (일)

    Returns:
        {
            "ok": True,
            "data": {
                "ticker": "005930",
                "name": "삼성전자",
                "dates": ["2025-01-02", ...],
                "market_cap": [380.0, ...],          # 조 단위
                "supply_ratio": [0.0015, ...],       # 수급 비율
                "ema12": [0.0012, ...],
                "ema26": [0.0010, ...],
                "macd": [0.0002, ...],
                "signal": [0.00015, ...],
                "oscillator": [0.00005, ...]         # 히스토그램
            }
        }
    """
    # 1. 수급 데이터 조회
    analysis_result = analysis.analyze(client, ticker, days)
    if not analysis_result["ok"]:
        return analysis_result

    data = analysis_result["data"]
    n = len(data["dates"])

    if n < 26:
        return {
            "ok": False,
            "error": {"code": "INSUFFICIENT_DATA", "msg": "최소 26일 데이터 필요"}
        }

    # 2. Supply Ratio 계산
    supply_ratio = []
    for i in range(n):
        mcap = data["mcap"][i]
        if mcap == 0:
            supply_ratio.append(0.0)
        else:
            supply = data["for_5d"][i] + data["ins_5d"][i]
            supply_ratio.append(supply / mcap)

    # 3. EMA 계산
    ema12 = _calc_ema(supply_ratio, 12)
    ema26 = _calc_ema(supply_ratio, 26)

    # 4. MACD 계산
    macd = [ema12[i] - ema26[i] for i in range(n)]

    # 5. Signal Line 계산
    signal = _calc_ema(macd, 9)

    # 6. Oscillator (Histogram) 계산
    oscillator = [macd[i] - signal[i] for i in range(n)]

    # 7. 시가총액 정규화 (조 단위)
    market_cap_trillion = [m / 1_000_000_000_000 for m in data["mcap"]]

    return {
        "ok": True,
        "data": {
            "ticker": ticker,
            "name": data["name"],
            "dates": data["dates"],
            "market_cap": market_cap_trillion,
            "supply_ratio": supply_ratio,
            "ema12": ema12,
            "ema26": ema26,
            "macd": macd,
            "signal": signal,
            "oscillator": oscillator
        }
    }


def analyze_signal(osc_result: dict) -> dict:
    """
    오실레이터 결과로 매매 신호 분석.

    Returns:
        {
            "ok": True,
            "data": {
                "total_score": 67,
                "signal_type": "STRONG_BUY",
                "oscillator_score": 40,
                "cross_score": 15,
                "trend_score": 12,
                "description": "수급 강세, MACD 시그널 상향"
            }
        }
    """
    if not osc_result.get("ok"):
        return osc_result

    data = osc_result["data"]
    osc = data["oscillator"]
    macd = data["macd"]
    signal = data["signal"]

    n = len(osc)
    if n < 3:
        return {
            "ok": False,
            "error": {"code": "INSUFFICIENT_DATA", "msg": "최소 3일 데이터 필요"}
        }

    score = 0

    # 1. Oscillator Value (±40)
    latest_osc = osc[-1]
    if latest_osc > 0.005:
        osc_score = 40
    elif latest_osc > 0.002:
        osc_score = 20
    elif latest_osc < -0.005:
        osc_score = -40
    elif latest_osc < -0.002:
        osc_score = -20
    else:
        osc_score = 0
    score += osc_score

    # 2. MACD Cross (±30)
    if macd[-1] > signal[-1] and macd[-2] <= signal[-2]:
        cross_score = 30  # Golden Cross
    elif macd[-1] < signal[-1] and macd[-2] >= signal[-2]:
        cross_score = -30  # Dead Cross
    elif macd[-1] > signal[-1]:
        cross_score = 15  # Above Signal
    else:
        cross_score = -15  # Below Signal
    score += cross_score

    # 3. Histogram Trend (±30)
    recent_hist = osc[-3:]
    if all(h > 0 for h in recent_hist) and _is_increasing(recent_hist):
        trend_score = 30
    elif all(h < 0 for h in recent_hist) and _is_decreasing(recent_hist):
        trend_score = -30
    else:
        trend_score = 0
    score += trend_score

    # Signal Type 결정
    score = max(-100, min(100, score))
    if score >= 60:
        signal_type = "STRONG_BUY"
    elif score >= 20:
        signal_type = "BUY"
    elif score <= -60:
        signal_type = "STRONG_SELL"
    elif score <= -20:
        signal_type = "SELL"
    else:
        signal_type = "NEUTRAL"

    return {
        "ok": True,
        "data": {
            "total_score": score,
            "signal_type": signal_type,
            "oscillator_score": osc_score,
            "cross_score": cross_score,
            "trend_score": trend_score,
            "description": _generate_description(signal_type, osc_score, cross_score)
        }
    }


def _calc_ema(values: List[float], period: int) -> List[float]:
    """EMA 계산."""
    alpha = 2 / (period + 1)
    ema = [values[0]]
    for i in range(1, len(values)):
        ema.append(alpha * values[i] + (1 - alpha) * ema[i - 1])
    return ema


def _is_increasing(values: List[float]) -> bool:
    return all(values[i] > values[i - 1] for i in range(1, len(values)))


def _is_decreasing(values: List[float]) -> bool:
    return all(values[i] < values[i - 1] for i in range(1, len(values)))


def _generate_description(signal_type: str, osc_score: int, cross_score: int) -> str:
    parts = []
    if osc_score > 0:
        parts.append("수급 강세")
    elif osc_score < 0:
        parts.append("수급 약세")

    if cross_score == 30:
        parts.append("골든크로스 발생")
    elif cross_score == -30:
        parts.append("데드크로스 발생")
    elif cross_score > 0:
        parts.append("MACD 시그널 상향")
    else:
        parts.append("MACD 시그널 하향")

    return ", ".join(parts) if parts else "중립"
```

### 5.4 JSON 응답 규격

#### 성공 응답
```json
{
  "ok": true,
  "data": { ... }
}
```

#### 에러 응답
```json
{
  "ok": false,
  "error": {
    "code": "TICKER_NOT_FOUND",
    "msg": "종목을 찾을 수 없습니다",
    "ctx": {"ticker": "999999"}
  }
}
```

#### 에러 코드

| Code | Description |
|------|-------------|
| `INVALID_ARG` | 잘못된 인자 |
| `TICKER_NOT_FOUND` | 종목 없음 |
| `NO_DATA` | 데이터 없음 |
| `API_ERROR` | 외부 API 오류 |
| `AUTH_ERROR` | 인증 실패 |
| `TOKEN_EXPIRED` | 토큰 만료 |
| `NETWORK_ERROR` | 네트워크 오류 |
| `RATE_LIMIT` | 요청 제한 초과 |
| `TIMEOUT` | 타임아웃 |

---

## 6. App 프로젝트 구조

### 6.1 디렉토리 구조

```
StockApp/
├── app/
│   ├── build.gradle.kts
│   │
│   └── src/main/
│       ├── java/com/stockapp/
│       │   ├── App.kt               # Hilt Application
│       │   ├── MainActivity.kt
│       │   │
│       │   ├── core/                # 공통 인프라
│       │   │   ├── db/              # Room DB
│       │   │   │   ├── AppDb.kt
│       │   │   │   ├── entity/
│       │   │   │   └── dao/
│       │   │   ├── py/              # Python Bridge
│       │   │   │   └── PyClient.kt
│       │   │   ├── ui/              # 공통 UI
│       │   │   │   ├── theme/
│       │   │   │   └── component/
│       │   │   └── di/              # DI Modules
│       │   │       ├── DbModule.kt
│       │   │       └── PyModule.kt
│       │   │
│       │   ├── feature/             # 기능별 모듈
│       │   │   ├── search/          # 종목 검색
│       │   │   │   ├── domain/
│       │   │   │   │   ├── model/
│       │   │   │   │   ├── repo/
│       │   │   │   │   └── usecase/
│       │   │   │   ├── data/
│       │   │   │   │   └── repo/
│       │   │   │   ├── ui/
│       │   │   │   │   ├── SearchScreen.kt
│       │   │   │   │   └── SearchVm.kt
│       │   │   │   └── di/
│       │   │   │       └── SearchModule.kt
│       │   │   │
│       │   │   ├── analysis/        # 수급 분석
│       │   │   ├── indicator/       # 기술적 지표
│       │   │   ├── condition/       # 조건검색
│       │   │   └── market/          # 시장 지표
│       │   │
│       │   └── nav/                 # 네비게이션
│       │       └── Nav.kt
│       │
│       ├── python/                  # Python 스크립트
│       │   ├── stock_analyzer/      # Python 패키지 복사
│       │   └── __init__.py
│       │
│       └── res/
│
├── gradle/
│   └── libs.versions.toml
│
└── build.gradle.kts
```

### 6.2 클린 아키텍처 계층

```
┌─────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                 │
│  Screen ← ViewModel (StateFlow)             │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Domain Layer                               │
│  UseCase ← Repository (interface)           │
│  Model (data class)                         │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Data Layer                                 │
│  RepositoryImpl → LocalDataSource (Room)    │
│                 → RemoteDataSource (Python) │
└─────────────────────────────────────────────┘
```

### 6.3 핵심 컴포넌트 명세

#### 6.3.1 PyClient (Python Bridge)

```kotlin
/**
 * Python 호출 클라이언트.
 * 모든 Python 호출은 이 클래스를 통해 수행.
 */
@Singleton
class PyClient @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val py = Python.getInstance()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun <T> call(
        module: String,
        func: String,
        args: List<Any> = emptyList(),
        timeoutMs: Long = 30_000,
        parser: (String) -> T
    ): Result<T>
}
```

#### 6.3.2 Stock Domain Model

```kotlin
// domain/model/Stock.kt
data class Stock(
    val ticker: String,
    val name: String,
    val market: Market  // KOSPI, KOSDAQ
)

enum class Market { KOSPI, KOSDAQ, OTHER }

data class StockData(
    val ticker: String,
    val name: String,
    val dates: List<String>,
    val mcap: List<Long>,
    val for5d: List<Long>,
    val ins5d: List<Long>
)
```

#### 6.3.3 Repository Interface

```kotlin
// domain/repo/StockRepo.kt
interface StockRepo {
    suspend fun search(query: String): Result<List<Stock>>
    suspend fun getAnalysis(ticker: String, days: Int = 180): Result<StockData>
    fun getHistory(): Flow<List<String>>  // 검색 히스토리
    suspend fun saveHistory(ticker: String)
}
```

#### 6.3.4 ViewModel State

```kotlin
// ui/SearchVm.kt
sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Results(val stocks: List<Stock>) : SearchState()
    data class Error(val code: String, val msg: String) : SearchState()
}

@HiltViewModel
class SearchVm @Inject constructor(
    private val searchUC: SearchStockUC
) : ViewModel() {
    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun search(q: String) { ... }
}
```

---

## 7. 데이터 모델

### 7.1 Database Entities

```kotlin
// Stock 캐시 (자동완성용)
@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val ticker: String,
    val name: String,
    val market: String,
    val updatedAt: Long
)

// 수급 분석 캐시
@Entity(tableName = "analysis_cache")
data class AnalysisCacheEntity(
    @PrimaryKey val ticker: String,
    val data: String,      // JSON serialized
    val startDate: String,
    val endDate: String,
    val cachedAt: Long
)

// 검색 히스토리
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticker: String,
    val name: String,
    val searchedAt: Long
)
```

### 7.2 캐시 정책

| Entity | TTL | 무효화 조건 |
|--------|-----|------------|
| stocks | 24h | 앱 시작 시 체크 |
| analysis_cache | 24h | 요청 일자가 캐시 범위 밖 |
| search_history | - | 최대 50개 유지 |

---

## 8. Python-App 인터페이스

### 8.1 호출 규격

```kotlin
// Python 호출 예시
val result = pyClient.call(
    module = "stock_analyzer.stock.search",
    func = "search",
    args = listOf(client, "삼성전자"),
    timeoutMs = 30_000
) { json ->
    json.decodeFromString<SearchResponse>(json)
}
```

### 8.2 모듈별 함수 매핑

| Python Module | Function | App UseCase | 키움 API |
|---------------|----------|-------------|----------|
| `client.kiwoom` | `KiwoomClient()` | 초기화 | - |
| `stock.search` | `search(client, query)` | SearchStockUC | ka10099 |
| `stock.search` | `get_all(client)` | GetAllStocksUC | ka10099 |
| `stock.analysis` | `analyze(client, ticker, days)` | GetAnalysisUC | ka10001, ka10059 |
| `stock.ohlcv` | `get_daily(client, ticker, ...)` | GetOhlcvUC | ka10081 |
| `indicator.trend` | `calc(client, ticker, ...)` | GetTrendSignalUC | ka10081 |
| `indicator.elder` | `calc(client, ticker, ...)` | GetElderImpulseUC | ka10082 |
| `search.condition` | `get_list(client)` | GetConditionListUC | ka10171 |
| `search.condition` | `search(client, ...)` | SearchConditionUC | ka10172 |

### 8.3 타임아웃 설정

| Function | Timeout | 비고 |
|----------|---------|------|
| search | 30s | 종목 리스트 조회 |
| analyze | 60s | 다중 API 호출 |
| ohlcv | 30s | 차트 데이터 |
| indicator.* | 30s | 지표 계산 |
| condition.search | 30s | 조건검색 |

---

## 9. 네이밍 규칙

### 9.1 Python

| Type | Convention | Example |
|------|------------|---------|
| Module | snake_case | `kiwoom.py` |
| Function | snake_case, 동사 | `search()`, `analyze()` |
| Class | PascalCase | `KiwoomClient`, `StockData` |
| Variable | snake_case, 약어 허용 | `mcap`, `for_5d` |
| Constant | UPPER_SNAKE | `MAX_RETRIES` |

### 9.2 Kotlin/App

| Type | Convention | Example |
|------|------------|---------|
| Package | lowercase | `com.stockapp.feature.search` |
| Class | PascalCase | `SearchVm`, `StockRepo` |
| Function | camelCase, 동사 | `search()`, `getAnalysis()` |
| Variable | camelCase | `stockList`, `for5d` |
| Constant | UPPER_SNAKE | `CACHE_TTL_MS` |

### 9.3 약어 사전

| Full | Abbrev | Usage |
|------|--------|-------|
| ViewModel | Vm | `SearchVm` |
| Repository | Repo | `StockRepo` |
| UseCase | UC | `SearchStockUC` |
| Implementation | Impl | `StockRepoImpl` |
| DataSource | DS | `LocalDS`, `RemoteDS` |
| market capitalization | mcap | `mcap` |
| foreign | for | `for5d` |
| institution | ins | `ins5d` |
| condition | cond | `condList` |

---

## 10. 에러 처리

### 10.1 Python 에러 처리

```python
# 표준 에러 반환
def search(client: KiwoomClient, query: str) -> dict:
    if not query:
        return {"ok": False, "error": {
            "code": "INVALID_ARG",
            "msg": "검색어가 필요합니다"
        }}

    try:
        # ... logic
        return {"ok": True, "data": results}
    except AuthError as e:
        log_err("search", e, {"query": query})
        return {"ok": False, "error": {
            "code": "AUTH_ERROR",
            "msg": str(e)
        }}
    except Exception as e:
        log_err("search", e, {"query": query})
        return {"ok": False, "error": {
            "code": "API_ERROR",
            "msg": str(e)
        }}
```

### 10.2 App 에러 처리

```kotlin
sealed class AppError(val code: String, val msg: String) {
    class InvalidArg(msg: String) : AppError("INVALID_ARG", msg)
    class NotFound(msg: String) : AppError("NOT_FOUND", msg)
    class Network(msg: String) : AppError("NETWORK", msg)
    class Auth(msg: String) : AppError("AUTH_ERROR", msg)
    class Python(code: String, msg: String) : AppError(code, msg)
    class Unknown(e: Throwable) : AppError("UNKNOWN", e.message ?: "알 수 없는 오류")
}

// ViewModel에서 에러 표시
when (val state = _state.value) {
    is SearchState.Error -> {
        // code로 에러 유형 구분, msg로 사용자 표시
        ErrorCard(code = state.code, msg = state.msg)
    }
}
```

### 10.3 로깅 구조

```
[Module] LEVEL: Message {context}

예시:
[client.kiwoom] INFO: API call {"api_id": "ka10099"}
[stock.search] INFO: search complete {"count": 15}
[stock.analysis] ERROR: API failed {"ticker": "005930", "error": "timeout"}
```

---

## 11. 테스트 계획

### 11.1 Python 테스트

```
tests/
├── unit/
│   ├── test_auth.py       # 토큰 관리
│   ├── test_search.py     # 검색 로직
│   └── test_indicator.py  # 지표 계산
│
├── integration/
│   └── test_kiwoom.py     # 키움 API (mock/live)
│
└── e2e/
    └── test_full_flow.py  # 전체 흐름
```

### 11.2 App 테스트

```
app/src/test/           # Unit Tests
├── PyClientTest.kt     # Python 호출
├── RepoTest.kt         # Repository
└── VmTest.kt           # ViewModel

app/src/androidTest/    # Instrumented Tests
├── DbTest.kt           # Room DB
└── ScreenTest.kt       # Compose UI
```

### 11.3 Phase별 테스트 체크리스트

#### Phase 1 체크리스트

- [ ] Python: 토큰 발급 성공
- [ ] Python: 토큰 자동 갱신
- [ ] Python: `search("")` → 에러 반환
- [ ] Python: `search("삼성")` → 결과 반환
- [ ] Python: `analyze("005930", 180)` → 데이터 반환
- [ ] Python: `analyze("999999", 180)` → 에러 반환
- [ ] App: 검색 → 결과 표시
- [ ] App: 검색 → 히스토리 저장
- [ ] App: 오프라인 → 캐시 사용
- [ ] App: 에러 → 에러 화면 표시

---

## 12. 개발 환경

### 12.1 Python 환경

```toml
# pyproject.toml
[project]
name = "stock-analyzer"
version = "0.1.0"
requires-python = ">=3.10"

dependencies = [
    "pandas>=2.0.0",
    "numpy>=1.24.0",
    "requests>=2.31.0",
    "python-dotenv>=1.0.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=7.4.0",
    "pytest-cov>=4.1.0",
    "pytest-asyncio>=0.21.0",
    "ruff>=0.1.0",
    "httpx>=0.25.0",  # 테스트용 mock
]
```

### 12.2 App 환경

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.1.0"
compose-bom = "2024.12.01"
hilt = "2.54"
room = "2.8.3"
chaquopy = "15.0.1"

[libraries]
# ... 생략
```

### 12.3 Claude Code 최적화

```markdown
# CLAUDE.md (신규 프로젝트용)

## Quick Commands
- `python -m pytest tests/` - Python 테스트
- `./gradlew test` - App 단위 테스트
- `./gradlew connectedAndroidTest` - App 통합 테스트

## File Locations
- Python: `stock-analyzer/src/stock_analyzer/`
- App: `StockApp/app/src/main/java/com/stockapp/`
- Tests: `tests/`, `app/src/test/`

## Common Patterns
- 모든 Python 함수는 `{"ok": bool, "data/error": ...}` 반환
- ViewModel은 sealed class로 상태 관리
- Repository는 Result<T> 반환

## 키움 API 참고
- API 문서: https://openapi.kiwoom.com/guide/apiguide
- 운영: https://api.kiwoom.com
- 모의투자: https://mockapi.kiwoom.com
```

---

## 13. 마일스톤

### Python 패키지 (완료 - FROZEN)

| Phase | 목표 | 상태 |
|-------|------|------|
| P0 | 프로젝트 설정, 키움 API 클라이언트 | ✅ 완료 |
| P1 | 종목 검색 + 수급 분석 | ✅ 완료 |
| P2 | 기술적 지표 (Trend, Elder, DeMark) | ✅ 완료 |
| P3 | 차트 시각화 (Candle, Line, Bar) | ✅ 완료 |
| P4 | 조건검색 + 시장 지표 | ✅ 완료 |
| P5 | 수급 오실레이터 | ✅ 완료 |

> 🔒 **Python 패키지는 FROZEN 상태입니다.** 더 이상 수정하지 않습니다.

### Android 앱

| Phase | 목표 | 산출물 | 상태 |
|-------|------|--------|------|
| App P0 | Android 프로젝트 설정 | Chaquopy, Hilt, Room, Vico 설정 | ✅ 완료 |
| App P1 | 종목 검색 + 수급 분석 | SearchScreen, AnalysisScreen | ✅ 완료 |
| App P2 | 기술적 지표 + 차트 | IndicatorScreen, Vico Charts | ✅ 완료 |
| App P3 | ~~시장 지표 + 조건검색~~ | ~~MarketScreen, ConditionScreen~~ | ⛔ 제거됨 |
| App P4 | **설정 화면** | SettingsScreen (API 키, 투자 모드) | ✅ 완료 |
| App P5 | **자동 스케줄링** | SchedulingTab, WorkManager | ✅ 완료 |

**사전 준비 문서**: `docs/ANDROID_PREPARATION.md`

### 현재 앱 구조

**네비게이션 (Bottom Nav - 4탭)**:
1. 🔍 **Search** - 종목 검색, 검색 히스토리
2. 📊 **Analysis** - 수급 분석, 매매 신호
3. 📈 **Indicator** - 기술적 지표 (Trend, Elder, DeMark)
4. ⚙️ **Settings** - API 키 설정, 자동 스케줄링

---

## 14. 참고 자료

### 14.1 키움 REST API 공식

| 자료 | URL |
|------|-----|
| API 가이드 | https://openapi.kiwoom.com/guide/apiguide |
| 시작하기 | https://openapi.kiwoom.com/guide/start |
| 오류코드 | https://openapi.kiwoom.com/guide/errorcode |
| AI 코딩 어시스턴트 | https://openapi.kiwoom.com/assist |

### 14.2 주요 API 목록

| 분류 | API ID | API 명 |
|------|--------|--------|
| 인증 | au10001 | 접근토큰 발급 |
| 인증 | au10002 | 접근토큰 폐기 |
| 종목 | ka10099 | 종목정보 리스트 |
| 종목 | ka10100 | 종목정보 조회 |
| 종목 | ka10001 | 주식기본정보요청 |
| 수급 | ka10008 | 주식외국인종목별매매동향 |
| 수급 | ka10045 | 종목별기관매매추이요청 |
| 수급 | ka10059 | 종목별투자자기관별요청 |
| 차트 | ka10081 | 주식일봉차트조회요청 |
| 차트 | ka10082 | 주식주봉차트조회요청 |
| 차트 | ka10083 | 주식월봉차트조회요청 |
| ETF | ka40003 | ETF일별추이요청 |
| ETF | ka40004 | ETF전체시세요청 |
| 조건 | ka10171 | 조건검색 목록조회 |
| 조건 | ka10172 | 조건검색 요청 일반 |

### 14.3 현재 프로젝트 파일 (EtfMonitor)

| 카테고리 | 파일 | 참고용 |
|----------|------|--------|
| Python | `stocks.py` | 검색, 분석 로직 |
| Python | `trend_signal.py` | 기술적 지표 |
| Python | `core.py` | 공통 유틸 |
| App | `OscillatorPyClient.kt` | Python Bridge |
| App | `StockRepositoryImpl.kt` | Repository 패턴 |
| App | `OscillatorViewModel.kt` | 상태 관리 |
| App | `OscillatorScreen.kt` | UI 구성 |

### 14.4 외부 라이브러리

- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Vico Charts**: https://github.com/patrykandpatrick/vico
- **Chaquopy**: https://chaquo.com/chaquopy/

---

## 15. App Phase 4: 설정 화면 (Settings)

### 15.1 개요

API 키 관리 및 투자 모드 설정을 위한 화면. EncryptedSharedPreferences를 사용하여 민감한 정보를 안전하게 저장.

### 15.2 SettingsScreen 구조

```
SettingsScreen
├── TabRow
│   ├── API Key 탭
│   └── Scheduling 탭
├── API Key 탭 내용
│   ├── App Key 입력 필드
│   ├── Secret Key 입력 필드
│   ├── 투자 모드 선택 (MOCK / PRODUCTION)
│   ├── 연결 테스트 버튼
│   └── 저장 버튼
└── Scheduling 탭 → SchedulingTab (Phase 5)
```

### 15.3 도메인 모델

```kotlin
// 투자 모드
enum class InvestmentMode {
    MOCK,       // 모의투자 서버 (mockapi.kiwoom.com)
    PRODUCTION  // 실전투자 서버 (api.kiwoom.com)
}

// API 키 설정
data class ApiKeyConfig(
    val appKey: String,
    val secretKey: String,
    val investmentMode: InvestmentMode
)
```

### 15.4 Repository

```kotlin
interface SettingsRepo {
    suspend fun getApiKeyConfig(): ApiKeyConfig?
    suspend fun saveApiKeyConfig(config: ApiKeyConfig)
    suspend fun clearApiKeyConfig()
    suspend fun testApiConnection(): Result<Boolean>
}

// 구현: EncryptedSharedPreferences 사용
class SettingsRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepo {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "settings_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    // ...
}
```

### 15.5 Use Cases

```kotlin
// API 키 설정 조회
class GetApiKeyConfigUC @Inject constructor(
    private val repo: SettingsRepo
) {
    suspend operator fun invoke(): ApiKeyConfig?
}

// API 키 설정 저장
class SaveApiKeyConfigUC @Inject constructor(
    private val repo: SettingsRepo
) {
    suspend operator fun invoke(config: ApiKeyConfig)
}

// API 연결 테스트
class TestApiKeyUC @Inject constructor(
    private val repo: SettingsRepo
) {
    suspend operator fun invoke(): Result<Boolean>
}
```

---

## 16. App Phase 5: 자동 스케줄링 (Scheduling)

### 16.1 개요

WorkManager를 사용하여 매일 지정된 시간에 자동으로 주식 데이터를 동기화. 등록된 종목의 수급 분석 및 기술적 지표 데이터를 백그라운드에서 갱신.

### 16.2 SchedulingTab 구조

```
SchedulingTab
├── 자동 동기화 토글 스위치
├── 동기화 시간 설정 (TimePicker)
├── 마지막 동기화 정보
│   ├── 시간
│   └── 상태 (성공/실패)
├── 수동 동기화 버튼
└── 동기화 히스토리 목록
```

### 16.3 도메인 모델

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
    SCHEDULED,   // 예약된 자동 동기화
    MANUAL       // 사용자 수동 동기화
}

// 스케줄링 설정
data class SchedulingConfig(
    val isEnabled: Boolean,          // 자동 동기화 활성화 여부
    val syncHour: Int,               // 동기화 시각 (시, 0-23)
    val syncMinute: Int,             // 동기화 시각 (분, 0-59)
    val lastSyncAt: Long?,           // 마지막 동기화 Unix timestamp
    val lastSyncStatus: SyncStatus   // 마지막 동기화 상태
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

// 동기화 결과
data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val failedCount: Int,
    val errorMessage: String?
)
```

### 16.4 WorkManager Worker

```kotlin
class StockSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject lateinit var analysisRepo: AnalysisRepo
    @Inject lateinit var indicatorRepo: IndicatorRepo
    @Inject lateinit var schedulingRepo: SchedulingRepo

    override suspend fun doWork(): Result {
        return try {
            // 1. 등록된 종목 목록 조회
            val stocks = schedulingRepo.getRegisteredStocks()

            // 2. 각 종목 데이터 동기화
            var syncedCount = 0
            for (stock in stocks) {
                // 수급 분석 데이터 갱신
                analysisRepo.refreshAnalysis(stock.ticker)
                // 기술적 지표 데이터 갱신
                indicatorRepo.refreshIndicators(stock.ticker)
                syncedCount++
            }

            // 3. 결과 저장
            schedulingRepo.recordSyncHistory(
                SyncHistory(
                    syncType = SyncType.SCHEDULED,
                    status = SyncStatus.SUCCESS,
                    syncedStocksCount = syncedCount
                )
            )

            Result.success()
        } catch (e: Exception) {
            schedulingRepo.recordSyncHistory(
                SyncHistory(
                    syncType = SyncType.SCHEDULED,
                    status = SyncStatus.FAILED,
                    errorMessage = e.message
                )
            )
            Result.retry()  // 최대 5회 재시도
        }
    }
}
```

### 16.5 SchedulingManager

```kotlin
class SchedulingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    private val _syncState = MutableStateFlow(SyncWorkState.IDLE)
    val syncState: StateFlow<SyncWorkState> = _syncState.asStateFlow()

    // 매일 자동 동기화 스케줄 등록
    fun scheduleDaily(hour: Int, minute: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<StockSyncWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(hour, minute), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "stock_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    // 수동 동기화 즉시 실행
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<StockSyncWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        workManager.enqueue(request)

        // 작업 상태 관찰
        workManager.getWorkInfoByIdLiveData(request.id)
            .observeForever { info ->
                _syncState.value = when (info?.state) {
                    WorkInfo.State.RUNNING -> SyncWorkState.RUNNING
                    WorkInfo.State.SUCCEEDED -> SyncWorkState.SUCCEEDED
                    WorkInfo.State.FAILED -> SyncWorkState.FAILED
                    else -> SyncWorkState.IDLE
                }
            }
    }

    // 스케줄 취소
    fun cancelSchedule() {
        workManager.cancelUniqueWork("stock_sync")
    }
}

// 동기화 작업 상태
enum class SyncWorkState {
    IDLE,       // 대기 중
    ENQUEUED,   // 큐에 등록됨
    RUNNING,    // 실행 중
    SUCCEEDED,  // 성공
    FAILED,     // 실패
    CANCELLED   // 취소됨
}
```

### 16.6 Database Entity

```kotlin
@Entity(tableName = "scheduling")
data class SchedulingEntity(
    @PrimaryKey val id: Int = 1,  // 싱글톤
    val isEnabled: Boolean,
    val syncHour: Int,
    val syncMinute: Int,
    val lastSyncAt: Long?,
    val lastSyncStatus: String  // SyncStatus.name
)

@Entity(tableName = "sync_history")
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncType: String,  // SyncType.name
    val startedAt: Long,
    val completedAt: Long?,
    val status: String,    // SyncStatus.name
    val syncedStocksCount: Int,
    val errorMessage: String?
)
```

### 16.7 DAO

```kotlin
@Dao
interface SchedulingDao {
    @Query("SELECT * FROM scheduling WHERE id = 1")
    fun getConfig(): Flow<SchedulingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: SchedulingEntity)

    @Query("SELECT * FROM sync_history ORDER BY startedAt DESC LIMIT 20")
    fun getHistory(): Flow<List<SyncHistoryEntity>>

    @Insert
    suspend fun insertHistory(history: SyncHistoryEntity)
}
```

---

## 부록 A: 키움 API 상세 명세

### A.1 접근토큰 발급 (au10001)

**Endpoint**: `POST /oauth2/token`

**Request**
```json
{
  "grant_type": "client_credentials",
  "appkey": "앱키",
  "secretkey": "시크릿키"
}
```

**Response**
```json
{
  "expires_dt": "20261107083713",
  "token_type": "bearer",
  "token": "WQJCwyqInphKnR3bSRtB9NE1lv...",
  "return_code": 0,
  "return_msg": "정상적으로 처리되었습니다"
}
```

### A.2 주식기본정보요청 (ka10001)

**Endpoint**: `POST /api/dostk/stkinfo`

**Headers**
```
api-id: ka10001
authorization: Bearer {token}
Content-Type: application/json;charset=UTF-8
```

**Request**
```json
{
  "stk_cd": "005930"
}
```

**Response**
```json
{
  "stk_cd": "005930",
  "stk_nm": "삼성전자",
  "cur_prc": 55000,
  "mrkt_tot_amt": 328000000000000,
  "per": 8.5,
  "pbr": 1.2,
  "return_code": 0,
  "return_msg": "정상적으로 처리되었습니다"
}
```

### A.3 주식일봉차트조회요청 (ka10081)

**Endpoint**: `POST /api/dostk/chart`

**Headers**
```
api-id: ka10081
authorization: Bearer {token}
```

**Request**
```json
{
  "stk_cd": "005930",
  "strt_dt": "20250101",
  "end_dt": "20251231",
  "adj_prc_tp": "1"
}
```

**Response** (실제 API 응답 기준)
```json
{
  "stk_cd": "005930",
  "stk_dt_pole_chart_qry": [
    {
      "dt": "20250102",
      "open_pric": 54000,
      "high_pric": 55500,
      "low_pric": 53800,
      "cur_prc": 55000,
      "trde_qty": 15000000
    }
  ],
  "return_code": 0,
  "return_msg": "정상적으로 처리되었습니다"
}
```

> **Note**: 주봉(ka10082)은 `stk_stk_pole_chart_qry`, 월봉(ka10083)은 `stk_mth_pole_chart_qry` 필드 사용

### A.4 종목별투자자기관별요청 (ka10059)

**Endpoint**: `POST /api/dostk/stkinfo`

**Headers**
```
api-id: ka10059
authorization: Bearer {token}
```

**Request** (실제 API 파라미터 기준)
```json
{
  "dt": "20260115",
  "stk_cd": "005930",
  "amt_qty_tp": "1",
  "trde_tp": "0",
  "unit_tp": "1000"
}
```

**Response** (실제 API 응답 기준)
```json
{
  "stk_invsr_orgn": [
    {
      "dt": "20260114",
      "cur_prc": "+140300",
      "frgnr_invsr": 23987,
      "orgn": 264048,
      "ind_invsr": -496193,
      "fnnc_invt": 244662,
      "insrnc": -15212,
      "invtrt": 152,
      "penfnd_etc": 12531,
      "natn": 0,
      "etc_corp": 209417
    }
  ],
  "return_code": 0,
  "return_msg": "정상적으로 처리되었습니다"
}
```

> **Note**: `frgnr_invsr`=외국인, `orgn`=기관계, `ind_invsr`=개인

---

## 부록 B: App UseCase 상세

### B.1 SearchStockUC

```kotlin
class SearchStockUC @Inject constructor(
    private val repo: StockRepo
) {
    suspend operator fun invoke(query: String): Result<List<Stock>> {
        if (query.isBlank()) {
            return Result.failure(AppError.InvalidArg("검색어를 입력하세요"))
        }
        return repo.search(query)
    }
}
```

### B.2 GetAnalysisUC

```kotlin
class GetAnalysisUC @Inject constructor(
    private val repo: StockRepo
) {
    suspend operator fun invoke(
        ticker: String,
        days: Int = 180,
        useCache: Boolean = true
    ): Result<StockData> {
        // 캐시 체크
        if (useCache) {
            val cached = repo.getCachedAnalysis(ticker, days)
            if (cached != null) return Result.success(cached)
        }
        return repo.getAnalysis(ticker, days)
    }
}
```

### B.3 GetConditionListUC

```kotlin
class GetConditionListUC @Inject constructor(
    private val repo: ConditionRepo
) {
    suspend operator fun invoke(): Result<List<Condition>> {
        return repo.getConditionList()
    }
}
```

---

**End of Specification**