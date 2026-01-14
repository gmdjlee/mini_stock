# 키움 REST API 빠른 참조표 (Quick Reference)

## 인증 (OAuth)

```python
# 토큰 발급
POST /oauth2/token
api-id: au10001
{
    "grant_type": "client_credentials",
    "appkey": "YOUR_APP_KEY",
    "secretkey": "YOUR_SECRET_KEY"
}

# 토큰 폐기
POST /oauth2/revoke
api-id: au10002
{
    "appkey": "YOUR_APP_KEY",
    "secretkey": "YOUR_SECRET_KEY",
    "token": "YOUR_TOKEN"
}
```

---

## 종목정보

```python
# 주식기본정보 (ka10001)
POST /api/dostk/stkinfo
{"stk_cd": "005930"}

# 거래원정보 (ka10002)
POST /api/dostk/stkinfo
{"stk_cd": "005930"}

# 체결정보 (ka10003)
POST /api/dostk/stkinfo
{"stk_cd": "005930"}
```

---

## 시세

```python
# 주식호가 (ka10004)
POST /api/dostk/mrkt
{"stk_cd": "005930"}

# 주식일주월시분 (ka10005)
POST /api/dostk/mrkt
{
    "stk_cd": "005930",
    "base_dt": "",
    "upd_stkpc_tp": "1"  # 수정주가 적용
}

# 주식시분 (ka10006)
POST /api/dostk/mrkt
{"stk_cd": "005930"}
```

---

## 차트

```python
# 틱차트 (ka10079)
POST /api/dostk/chart
{"stk_cd": "005930", "tick_rng": "1"}

# 분봉차트 (ka10080)
POST /api/dostk/chart
{"stk_cd": "005930", "tick_rng": "1"}  # 1, 3, 5, 10, 15, 30, 45, 60분

# 일봉차트 (ka10081)
POST /api/dostk/chart
{
    "stk_cd": "005930",
    "base_dt": "",
    "upd_stkpc_tp": "1"
}

# 주봉차트 (ka10082)
POST /api/dostk/chart
{"stk_cd": "005930", "base_dt": "", "upd_stkpc_tp": "1"}

# 월봉차트 (ka10083)
POST /api/dostk/chart
{"stk_cd": "005930", "base_dt": "", "upd_stkpc_tp": "1"}

# 년봉차트 (ka10094)
POST /api/dostk/chart
{"stk_cd": "005930", "base_dt": "", "upd_stkpc_tp": "1"}
```

---

## 순위정보

```python
# 거래량상위 (ka10030)
POST /api/dostk/ranking
{"mrkt_tp": "0"}  # 0:전체, 1:코스피, 2:코스닥

# 거래대금상위 (ka10032)
POST /api/dostk/ranking
{"mrkt_tp": "0"}

# 등락률상위 (ka10027)
POST /api/dostk/ranking
{"mrkt_tp": "0", "sort_tp": "1"}  # 1:상승, 2:하락

# 외인매매상위 (ka10034)
POST /api/dostk/ranking
{"mrkt_tp": "0", "period_tp": "1"}
```

---

## 계좌

```python
# 예수금상세현황 (kt00001)
POST /api/dostk/acnt
{}

# 계좌평가현황 (kt00004)
POST /api/dostk/acnt
{}

# 체결잔고 (kt00005)
POST /api/dostk/acnt
{}

# 주문체결현황 (kt00009)
POST /api/dostk/acnt
{}

# 일별잔고수익률 (ka01690)
POST /api/dostk/acnt
{"qry_dt": "20250101"}

# 미체결요청 (ka10075)
POST /api/dostk/acnt
{}

# 체결요청 (ka10076)
POST /api/dostk/acnt
{}
```

---

## 주문

```python
# 매수주문 (kt10000)
POST /api/dostk/order
{
    "stk_cd": "005930",
    "ord_qty": "10",
    "ord_uv": "70000",  # 시장가일 때 "0"
    "ord_tp": "00"      # 00:지정가, 03:시장가
}

# 매도주문 (kt10001)
POST /api/dostk/order
{
    "stk_cd": "005930",
    "ord_qty": "10",
    "ord_uv": "70000",
    "ord_tp": "00"
}

# 정정주문 (kt10002)
POST /api/dostk/order
{
    "org_ord_no": "12345",
    "stk_cd": "005930",
    "ord_qty": "5",
    "ord_uv": "71000"
}

# 취소주문 (kt10003)
POST /api/dostk/order
{
    "org_ord_no": "12345",
    "stk_cd": "005930",
    "ord_qty": "10"
}
```

---

## 업종

```python
# 업종현재가 (ka20001)
POST /api/dostk/sector
{"upjong_cd": "001"}

# 업종별주가 (ka20002)
POST /api/dostk/sector
{"upjong_cd": "001"}

# 전업종지수 (ka20003)
POST /api/dostk/sector
{"mrkt_tp": "0"}

# 업종일봉 (ka20006)
POST /api/dostk/sector
{"upjong_cd": "001"}
```

---

## ETF

```python
# ETF수익율 (ka40001)
POST /api/dostk/etf
{"stk_cd": "069500"}

# ETF종목정보 (ka40002)
POST /api/dostk/etf
{"stk_cd": "069500"}

# ETF일별추이 (ka40003)
POST /api/dostk/etf
{"stk_cd": "069500"}

# ETF전체시세 (ka40004)
POST /api/dostk/etf
{}
```

---

## 조건검색

```python
# 조건검색 목록 (ka10171)
POST /api/dostk/cond
{}

# 조건검색 일반 (ka10172)
POST /api/dostk/cond
{
    "cond_idx": "000",
    "cond_nm": "내조건식"
}

# 조건검색 실시간 (ka10173)
POST /api/dostk/cond
{
    "cond_idx": "000",
    "cond_nm": "내조건식"
}
```

---

## 실시간시세 (WebSocket)

```python
# WebSocket 연결
wss://api.kiwoom.com:10000/api/dostk/websocket

# 실시간 등록 요청
{
    "trnm": "REG",        # REG:등록, REMOVE:해지
    "grp_no": "1",
    "refresh": "1",
    "data": [
        {
            "item": ["005930"],
            "type": ["0B"]    # 실시간 항목 코드
        }
    ]
}
```

### 실시간 항목 코드

| Type | 항목명 | 설명 |
|------|--------|------|
| 00 | 주문체결 | 내 주문 체결 통보 |
| 04 | 잔고 | 실시간 잔고 변동 |
| 0A | 주식기세 | 기세 정보 |
| 0B | 주식체결 | 실시간 체결가 |
| 0C | 주식우선호가 | 우선 호가 |
| 0D | 주식호가잔량 | 10단계 호가 |
| 0E | 주식시간외호가 | 시간외 호가 |
| 0F | 당일거래원 | 거래원 정보 |
| 0G | ETF NAV | ETF 추정 NAV |
| 0H | 예상체결 | 장전/장후 예상 |
| 0J | 업종지수 | 업종 지수 |
| 0U | 업종등락 | 업종 등락 |
| 0g | 종목정보 | 종목 기본정보 |
| 0m | ELW이론가 | ELW 이론가 |
| 0s | 장시작시간 | 장 시작/마감 |
| 0w | 프로그램매매 | 종목별 프로그램 |
| 1h | VI발동/해제 | VI 발동 정보 |

---

## 주요 파라미터 값

### 시장구분 (mrkt_tp)
- `0`: 전체
- `1`: 코스피
- `2`: 코스닥

### 주문유형 (ord_tp)
- `00`: 지정가
- `03`: 시장가
- `05`: 조건부지정가
- `06`: 최유리지정가
- `07`: 최우선지정가

### 기간구분
- `D`: 일
- `W`: 주
- `M`: 월
- `Y`: 년

### 수정주가 (upd_stkpc_tp)
- `0`: 미적용
- `1`: 적용

---

## Python 코드 템플릿

```python
import requests

class KiwoomAPI:
    def __init__(self, app_key, secret_key, is_mock=False):
        self.app_key = app_key
        self.secret_key = secret_key
        self.base_url = "https://mockapi.kiwoom.com" if is_mock else "https://api.kiwoom.com"
        self.token = None
    
    def get_token(self):
        """토큰 발급"""
        url = f"{self.base_url}/oauth2/token"
        headers = {
            "Content-Type": "application/json",
            "api-id": "au10001"
        }
        body = {
            "grant_type": "client_credentials",
            "appkey": self.app_key,
            "secretkey": self.secret_key
        }
        resp = requests.post(url, headers=headers, json=body)
        result = resp.json()
        self.token = result.get("token")
        return self.token
    
    def _request(self, api_id, url_path, body):
        """API 요청"""
        url = f"{self.base_url}{url_path}"
        headers = {
            "Content-Type": "application/json",
            "api-id": api_id,
            "authorization": f"Bearer {self.token}"
        }
        resp = requests.post(url, headers=headers, json=body)
        return resp.json()
    
    def get_stock_price(self, stock_code):
        """주식 현재가 조회"""
        return self._request("ka10004", "/api/dostk/mrkt", {"stk_cd": stock_code})
    
    def get_daily_chart(self, stock_code):
        """일봉 차트 조회"""
        return self._request("ka10081", "/api/dostk/chart", {
            "stk_cd": stock_code,
            "base_dt": "",
            "upd_stkpc_tp": "1"
        })

# 사용 예시
api = KiwoomAPI("YOUR_APP_KEY", "YOUR_SECRET_KEY")
api.get_token()
result = api.get_stock_price("005930")
print(result)
```

---

## 연속 조회 (Pagination)

```python
def get_all_data(api_id, body):
    """연속조회 처리"""
    all_data = []
    cont_yn = "N"
    next_key = ""
    
    while True:
        headers = {
            "api-id": api_id,
            "authorization": f"Bearer {token}",
            "cont-yn": cont_yn,
            "next-key": next_key
        }
        
        resp = requests.post(url, headers=headers, json=body)
        result = resp.json()
        all_data.extend(result.get("data", []))
        
        # 연속 조회 확인
        cont_yn = resp.headers.get("cont-yn", "N")
        next_key = resp.headers.get("next-key", "")
        
        if cont_yn != "Y":
            break
    
    return all_data
```
