# 국내주식 > ETF

이 문서는 ETF 관련 API 9개를 포함합니다.

## API 목록

| API ID | API 명 | URL | 모의투자 | WebSocket |
|--------|--------|-----|----------|-----------|
| ka40001 | ETF수익율요청 | /api/dostk/etf | ✓ |  |
| ka40002 | ETF종목정보요청 | /api/dostk/etf | ✓ |  |
| ka40003 | ETF일별추이요청 | /api/dostk/etf | ✓ |  |
| ka40004 | ETF전체시세요청 | /api/dostk/etf | ✓ |  |
| ka40006 | ETF시간대별추이요청 | /api/dostk/etf | ✓ |  |
| ka40007 | ETF시간대별체결요청 | /api/dostk/etf | ✓ |  |
| ka40008 | ETF일자별체결요청 | /api/dostk/etf | ✓ |  |
| ka40009 | ETF시간대별체결요청 | /api/dostk/etf | ✓ |  |
| ka40010 | ETF시간대별추이요청 | /api/dostk/etf | ✓ |  |

---

## ETF수익율요청

**API ID**: `ka40001`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "etfobjt_idex_cd": "",  # Required - ETF대상지수코드
    "dt": "",  # Required - 기간
    "etfprft_rt": "",  # Optional - ETF수익률
    "cntr_prft_rt": "",  # Optional - 체결수익률
    "for_netprps_qty": "",  # Optional - 외인순매수수량
    "orgn_netprps_qty": "",  # Optional - 기관순매수수량
}
```

### Request Example

```json
{"stk_cd": "069500","etfobjt_idex_cd": "207","dt": "3"}
```

---

## ETF종목정보요청

**API ID**: `ka40002`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "stk_nm": "",  # Optional - 종목명
    "etfobjt_idex_nm": "",  # Optional - ETF대상지수명
    "wonju_pric": "",  # Optional - 원주가격
    "etftxon_type": "",  # Optional - ETF과세유형
    "etntxon_type": "",  # Optional - ETN과세유형
}
```

### Request Example

```json
{"stk_cd": "069500"}
```

---

## ETF일별추이요청

**API ID**: `ka40003`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "cntr_dt": "",  # Optional - 체결일자
    "cur_prc": "",  # Optional - 현재가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "pre_rt": "",  # Optional - 대비율
    "trde_qty": "",  # Optional - 거래량
    "nav": "",  # Optional - NAV
    "acc_trde_prica": "",  # Optional - 누적거래대금
    "navidex_dispty_rt": "",  # Optional - NAV/지수괴리율
    "navetfdispty_rt": "",  # Optional - NAV/ETF괴리율
    "trace_eor_rt": "",  # Optional - 추적오차율
    "trace_cur_prc": "",  # Optional - 추적현재가
    "trace_pred_pre": "",  # Optional - 추적전일대비
    "trace_pre_sig": "",  # Optional - 추적대비기호
}
```

### Request Example

```json
{"stk_cd": "069500"}
```

---

## ETF전체시세요청

**API ID**: `ka40004`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "txon_type": "",  # Required - 과세유형
    "navpre": "",  # Required - NAV대비
    "mngmcomp": "",  # Required - 운용사
    "txon_yn": "",  # Required - 과세여부
    "trace_idex": "",  # Required - 추적지수
    "stex_tp": "",  # Required - 거래소구분
    "stk_cd": "",  # Optional - 종목코드
    "stk_cls": "",  # Optional - 종목분류
    "stk_nm": "",  # Optional - 종목명
    "close_pric": "",  # Optional - 종가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "pre_rt": "",  # Optional - 대비율
    "trde_qty": "",  # Optional - 거래량
    "nav": "",  # Optional - NAV
    "trace_eor_rt": "",  # Optional - 추적오차율
    "txbs": "",  # Optional - 과표기준
    "dvid_bf_base": "",  # Optional - 배당전기준
    "pred_dvida": "",  # Optional - 전일배당금
    "trace_idex_nm": "",  # Optional - 추적지수명
    "drng": "",  # Optional - 배수
    "trace_idex_cd": "",  # Optional - 추적지수코드
    "trace_idex": "",  # Optional - 추적지수
    "trace_flu_rt": "",  # Optional - 추적등락율
}
```

### Request Example

```json
{"txon_type": "0","navpre": "0","mngmcomp": "0000","txon_yn": "0","trace_idex": "0","stex_tp": "1"}
```

---

## ETF시간대별추이요청

**API ID**: `ka40006`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "stk_nm": "",  # Optional - 종목명
    "etfobjt_idex_nm": "",  # Optional - ETF대상지수명
    "wonju_pric": "",  # Optional - 원주가격
    "etftxon_type": "",  # Optional - ETF과세유형
    "etntxon_type": "",  # Optional - ETN과세유형
    "tm": "",  # Optional - 시간
    "close_pric": "",  # Optional - 종가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "flu_rt": "",  # Optional - 등락율
    "trde_qty": "",  # Optional - 거래량
    "nav": "",  # Optional - NAV
    "trde_prica": "",  # Optional - 거래대금
    "navidex": "",  # Optional - NAV지수
    "navetf": "",  # Optional - NAVETF
    "trace": "",  # Optional - 추적
    "trace_idex": "",  # Optional - 추적지수
}
```

### Request Example

```json
{"stk_cd": "069500"}
```

---

## ETF시간대별체결요청

**API ID**: `ka40007`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "stk_cls": "",  # Optional - 종목분류
    "stk_nm": "",  # Optional - 종목명
    "etfobjt_idex_nm": "",  # Optional - ETF대상지수명
    "etfobjt_idex_cd": "",  # Optional - ETF대상지수코드
    "objt_idex_pre_rt": "",  # Optional - 대상지수대비율
    "wonju_pric": "",  # Optional - 원주가격
    "cntr_tm": "",  # Optional - 체결시간
    "cur_prc": "",  # Optional - 현재가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "trde_qty": "",  # Optional - 거래량
    "stex_tp": "",  # Optional - 거래소구분
}
```

### Request Example

```json
{"stk_cd": "069500"}
```

---

## ETF일자별체결요청

**API ID**: `ka40008`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "cntr_tm": "",  # Optional - 체결시간
    "cur_prc": "",  # Optional - 현재가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "trde_qty": "",  # Optional - 거래량
    "dt": "",  # Optional - 일자
    "cur_prc_n": "",  # Optional - 현재가n
    "pre_sig_n": "",  # Optional - 대비기호n
    "pred_pre_n": "",  # Optional - 전일대비n
    "acc_trde_qty": "",  # Optional - 누적거래량
    "for_netprps_qty": "",  # Optional - 외인순매수수량
    "orgn_netprps_qty": "",  # Optional - 기관순매수수량
}
```

### Request Example

```json
{"stk_cd": "069500"}
```

---

## ETF시간대별체결요청

**API ID**: `ka40009`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "nav": "",  # Optional - NAV
    "navpred_pre": "",  # Optional - NAV전일대비
    "navflu_rt": "",  # Optional - NAV등락율
    "trace_eor_rt": "",  # Optional - 추적오차율
    "dispty_rt": "",  # Optional - 괴리율
    "stkcnt": "",  # Optional - 주식수
    "base_pric": "",  # Optional - 기준가
    "for_rmnd_qty": "",  # Optional - 외인보유수량
    "repl_pric": "",  # Optional - 대용가
    "conv_pric": "",  # Optional - 환산가격
    "drstk": "",  # Optional - DR/주
    "wonju_pric": "",  # Optional - 원주가격
}
```

### Request Example

```json
{"stk_cd": "069500"}
```

---

## ETF시간대별추이요청

**API ID**: `ka40010`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/etf`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "cur_prc": "",  # Optional - 현재가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "trde_qty": "",  # Optional - 거래량
    "for_netprps": "",  # Optional - 외인순매수
}
```

### Request Example

```json
{"stk_cd": "069500"}
```

---
