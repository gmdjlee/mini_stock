# 키움 REST API 문서

Claude Code에서 활용하기 쉽게 정리된 키움증권 REST API 문서입니다.

## 📁 파일 구조

```
kiwoom_api_docs/
├── 00_API_INDEX.md          # API 전체 인덱스 (빠른 참조용)
├── 01_auth.md               # OAuth 인증 API
├── 02_domestic_stock.md     # 국내주식 전체 API
├── api_spec.json            # JSON 형태 API 스펙 (프로그래밍용)
├── error_codes.json         # 오류 코드 목록
├── kiwoom_api_client.py     # Python 클라이언트 클래스
├── detail/                  # 서브카테고리별 상세 문서
│   ├── 국내주식_계좌.md
│   ├── 국내주식_시세.md
│   ├── 국내주식_차트.md
│   ├── 국내주식_주문.md
│   ├── 국내주식_실시간시세.md
│   └── ...
└── README.md
```

## 🚀 빠른 시작

### 1. API 스펙 확인

```python
import json

# API 스펙 로드
with open('api_spec.json', 'r', encoding='utf-8') as f:
    spec = json.load(f)

# 특정 API 정보 확인
api_info = spec['apis']['ka10081']  # 주식일봉차트
print(api_info)
```

### 2. Python 클라이언트 사용

```python
from kiwoom_api_client import KiwoomAPI, MarketType, APIID

# 실전투자 API 클라이언트 생성
api = KiwoomAPI(
    app_key="YOUR_APP_KEY",
    secret_key="YOUR_SECRET_KEY"
)

# 모의투자 API 클라이언트 생성
mock_api = KiwoomAPI(
    app_key="YOUR_APP_KEY",
    secret_key="YOUR_SECRET_KEY",
    market_type=MarketType.MOCK
)

# 토큰 발급
api.get_token()

# 주식 호가 조회
result = api.get_stock_quote("005930")

# 일봉 차트 조회
chart = api.get_chart_daily("005930")

# 거래량 상위 종목
volume_rank = api.get_volume_rank()
```

## 📋 주요 API 카테고리

| 카테고리 | 설명 | 파일 |
|----------|------|------|
| OAuth 인증 | 토큰 발급/폐기 | `01_auth.md` |
| 종목정보 | 기본정보, 거래원, 체결정보 | `detail/국내주식_종목정보.md` |
| 시세 | 호가, OHLCV, 일별주가 | `detail/국내주식_시세.md` |
| 차트 | 틱/분/일/주/월/년봉 | `detail/국내주식_차트.md` |
| 순위정보 | 거래량, 등락률, 외국인매매 | `detail/국내주식_순위정보.md` |
| 계좌 | 잔고, 예수금, 체결내역 | `detail/국내주식_계좌.md` |
| 주문 | 매수/매도/정정/취소 | `detail/국내주식_주문.md` |
| 업종 | 업종지수, 업종별 시세 | `detail/국내주식_업종.md` |
| ETF | ETF 시세, 수익률 | `detail/국내주식_ETF.md` |
| ELW | ELW 시세, 민감도 | `detail/국내주식_ELW.md` |
| 조건검색 | 조건식 검색 | `detail/국내주식_조건검색.md` |
| 실시간시세 | WebSocket 시세 | `detail/국내주식_실시간시세.md` |

## 🔑 API 도메인

| 구분 | 도메인 |
|------|--------|
| 운영 (REST) | `https://api.kiwoom.com` |
| 모의투자 (REST) | `https://mockapi.kiwoom.com` |
| 운영 (WebSocket) | `wss://api.kiwoom.com:10000` |
| 모의투자 (WebSocket) | `wss://mockapi.kiwoom.com:10000` |

## 📝 공통 헤더

모든 API 요청에 필요한 공통 헤더:

```python
headers = {
    "Content-Type": "application/json;charset=UTF-8",
    "api-id": "ka10081",                    # API ID (필수)
    "authorization": "Bearer {token}",      # 접근토큰 (필수)
    "cont-yn": "N",                          # 연속조회여부 (선택)
    "next-key": ""                           # 연속조회키 (선택)
}
```

## 🔍 자주 사용하는 API

### 인증
| API ID | 이름 | 설명 |
|--------|------|------|
| au10001 | 접근토큰 발급 | 인증 토큰 발급 |
| au10002 | 접근토큰 폐기 | 인증 토큰 폐기 |

### 시세/차트
| API ID | 이름 | 설명 |
|--------|------|------|
| ka10004 | 주식호가요청 | 실시간 호가 |
| ka10081 | 주식일봉차트 | 일봉 데이터 |
| ka10080 | 주식분봉차트 | 분봉 데이터 |

### 계좌
| API ID | 이름 | 설명 |
|--------|------|------|
| kt00001 | 예수금상세현황 | 예수금 조회 |
| kt00004 | 계좌평가현황 | 보유잔고 조회 |
| kt00009 | 주문체결현황 | 체결내역 조회 |

### 주문
| API ID | 이름 | 설명 |
|--------|------|------|
| kt10000 | 매수주문 | 주식 매수 |
| kt10001 | 매도주문 | 주식 매도 |
| kt10002 | 정정주문 | 주문 정정 |
| kt10003 | 취소주문 | 주문 취소 |

### 실시간 (WebSocket)
| Type | 이름 | 설명 |
|------|------|------|
| 0B | 주식체결 | 실시간 체결 |
| 0D | 주식호가잔량 | 실시간 호가 |
| 00 | 주문체결 | 내 주문 체결 |
| 04 | 잔고 | 실시간 잔고 변동 |

## ⚠️ 오류 코드

주요 오류 코드:

| 코드 | 설명 |
|------|------|
| 1511 | 필수 입력값 누락 |
| 1700 | 요청 횟수 초과 |
| 1902 | 종목 정보 없음 |
| 8005 | 토큰 유효하지 않음 |
| 8010 | IP 불일치 |

전체 오류 코드는 `error_codes.json` 참조

## 📚 문서 활용 팁

### Claude Code에서 API 검색

```python
# api_spec.json을 사용한 API 검색
import json

with open('api_spec.json', 'r', encoding='utf-8') as f:
    spec = json.load(f)

# 키워드로 검색
keyword = "차트"
for api_id, info in spec['apis'].items():
    if keyword in info.get('name', '') or keyword in info.get('subcategory', ''):
        print(f"{api_id}: {info['name']}")
```

### 특정 카테고리 API 목록

```python
# 계좌 관련 API 조회
for api_id, info in spec['apis'].items():
    if info.get('subcategory') == '계좌':
        print(f"{api_id}: {info['name']}")
```

## 📌 주의사항

1. **모의투자 지원**: 일부 API는 모의투자를 지원하지 않습니다. 각 API 문서에서 `mock_supported` 확인
2. **요청 제한**: API 호출 횟수 제한이 있으므로 과도한 요청 주의
3. **토큰 만료**: 토큰 만료 시간 확인 및 갱신 필요
4. **WebSocket**: 실시간 시세는 WebSocket 연결 필요

## 🔗 참고 링크

- [키움증권 OpenAPI](https://www.kiwoom.com/)
- [API 공식 문서](https://api.kiwoom.com/)

---

*이 문서는 원본 REST_API_DOC.md를 Claude Code 활용에 최적화하여 재구성한 것입니다.*
