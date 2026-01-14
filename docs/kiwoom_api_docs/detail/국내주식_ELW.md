# 국내주식 > ELW

이 문서는 ELW 관련 API 11개를 포함합니다.

## API 목록

| API ID | API 명 | URL | 모의투자 | WebSocket |
|--------|--------|-----|----------|-----------|
| ka10048 | ELW일별민감도지표요청 | /api/dostk/elw | ✓ |  |
| ka10050 | ELW민감도지표요청 | /api/dostk/elw | ✓ |  |
| ka30001 | ELW가격급등락요청 | /api/dostk/elw | ✓ |  |
| ka30002 | 거래원별ELW순매매상위요청 | /api/dostk/elw | ✓ |  |
| ka30003 | ELWLP보유일별추이요청 | /api/dostk/elw | ✓ |  |
| ka30004 | ELW괴리율요청 | /api/dostk/elw | ✓ |  |
| ka30005 | ELW조건검색요청 | /api/dostk/elw | ✓ |  |
| ka30009 | ELW등락율순위요청 | /api/dostk/elw | ✓ |  |
| ka30010 | ELW잔량순위요청 | /api/dostk/elw | ✓ |  |
| ka30011 | ELW근접율요청 | /api/dostk/elw | ✓ |  |
| ka30012 | ELW종목상세정보요청 | /api/dostk/elw | ✓ |  |

---

## ELW일별민감도지표요청

**API ID**: `ka10048`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "dt": "",  # Optional - 일자
    "iv": "",  # Optional - IV
    "delta": "",  # Optional - 델타
    "gam": "",  # Optional - 감마
    "theta": "",  # Optional - 쎄타
    "vega": "",  # Optional - 베가
    "law": "",  # Optional - 로
    "lp": "",  # Optional - LP
}
```

### Request Example

```json
{"stk_cd": "57JBHH"```Request Example```### }
```

---

## ELW민감도지표요청

**API ID**: `ka10050`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "cntr_tm": "",  # Optional - 체결시간
    "cur_prc": "",  # Optional - 현재가
    "elwtheory_pric": "",  # Optional - ELW이론가
    "iv": "",  # Optional - IV
    "delta": "",  # Optional - 델타
    "gam": "",  # Optional - 감마
    "theta": "",  # Optional - 쎄타
    "vega": "",  # Optional - 베가
    "law": "",  # Optional - 로
    "lp": "",  # Optional - LP
}
```

### Request Example

```json
{"stk_cd": "57JBHH"}
```

---

## ELW가격급등락요청

**API ID**: `ka30001`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "flu_tp": "",  # Required - 등락구분
    "tm_tp": "",  # Required - 시간구분
    "tm": "",  # Required - 시간
    "trde_qty_tp": "",  # Required - 거래량구분
    "isscomp_cd": "",  # Required - 발행사코드
    "bsis_aset_cd": "",  # Required - 기초자산코드
    "rght_tp": "",  # Required - 권리구분
    "lpcd": "",  # Required - LP코드
    "trde_end_elwskip": "",  # Required - 거래종료ELW제외
    "base_pric_tm": "",  # Optional - 기준가시간
    "stk_cd": "",  # Optional - 종목코드
    "rank": "",  # Optional - 순위
    "stk_nm": "",  # Optional - 종목명
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "cur_prc": "",  # Optional - 현재가
    "base_pre": "",  # Optional - 기준대비
    "trde_qty": "",  # Optional - 거래량
    "jmp_rt": "",  # Optional - 급등율
}
```

### Request Example

```json
{"flu_tp": "1","tm_tp": "2","tm": "1","trde_qty_tp": "0","isscomp_cd": "000000000000","bsis_aset_cd": "000000000000","rght_tp": "000","lpcd": "000000000000","trde_end_elwskip": "0"}
```

---

## 거래원별ELW순매매상위요청

**API ID**: `ka30002`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "isscomp_cd": "",  # Required - 발행사코드
    "trde_qty_tp": "",  # Required - 거래량구분
    "trde_tp": "",  # Required - 매매구분
    "dt": "",  # Required - 기간
    "trde_end_elwskip": "",  # Required - 거래종료ELW제외
    "stk_cd": "",  # Optional - 종목코드
    "stk_nm": "",  # Optional - 종목명
    "stkpc_flu": "",  # Optional - 주가등락
    "flu_rt": "",  # Optional - 등락율
    "trde_qty": "",  # Optional - 거래량
    "netprps": "",  # Optional - 순매수
    "buy_trde_qty": "",  # Optional - 매수거래량
    "sel_trde_qty": "",  # Optional - 매도거래량
}
```

### Request Example

```json
{"isscomp_cd": "003","trde_qty_tp": "0","trde_tp": "2","dt": "60","trde_end_elwskip": "0"}
```

---

## ELWLP보유일별추이요청

**API ID**: `ka30003`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "bsis_aset_cd": "",  # Required - 기초자산코드
    "base_dt": "",  # Required - 기준일자
    "dt": "",  # Optional - 일자
    "cur_prc": "",  # Optional - 현재가
    "pre_tp": "",  # Optional - 대비구분
    "pred_pre": "",  # Optional - 전일대비
    "flu_rt": "",  # Optional - 등락율
    "trde_qty": "",  # Optional - 거래량
    "trde_prica": "",  # Optional - 거래대금
    "chg_qty": "",  # Optional - 변동수량
    "lprmnd_qty": "",  # Optional - LP보유수량
    "wght": "",  # Optional - 비중
}
```

### Request Example

```json
{"bsis_aset_cd": "57KJ99","base_dt": "20241122"}
```

---

## ELW괴리율요청

**API ID**: `ka30004`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "isscomp_cd": "",  # Required - 발행사코드
    "bsis_aset_cd": "",  # Required - 기초자산코드
    "rght_tp": "",  # Required - 권리구분
    "lpcd": "",  # Required - LP코드
    "trde_end_elwskip": "",  # Required - 거래종료ELW제외
    "stk_cd": "",  # Optional - 종목코드
    "isscomp_nm": "",  # Optional - 발행사명
    "sqnc": "",  # Optional - 회차
    "base_aset_nm": "",  # Optional - 기초자산명
    "rght_tp": "",  # Optional - 권리구분
    "dispty_rt": "",  # Optional - 괴리율
    "basis": "",  # Optional - 베이시스
    "srvive_dys": "",  # Optional - 잔존일수
    "theory_pric": "",  # Optional - 이론가
    "cur_prc": "",  # Optional - 현재가
    "pre_tp": "",  # Optional - 대비구분
    "pred_pre": "",  # Optional - 전일대비
    "flu_rt": "",  # Optional - 등락율
    "trde_qty": "",  # Optional - 거래량
    "stk_nm": "",  # Optional - 종목명
}
```

### Request Example

```json
{"isscomp_cd": "000000000000","bsis_aset_cd": "000000000000","rght_tp": "000","lpcd": "000000000000","trde_end_elwskip": "0"}
```

---

## ELW조건검색요청

**API ID**: `ka30005`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "isscomp_cd": "",  # Required - 발행사코드
    "bsis_aset_cd": "",  # Required - 기초자산코드
    "rght_tp": "",  # Required - 권리구분
    "lpcd": "",  # Required - LP코드
    "sort_tp": "",  # Required - 정렬구분
    "stk_cd": "",  # Optional - 종목코드
    "isscomp_nm": "",  # Optional - 발행사명
    "sqnc": "",  # Optional - 회차
    "base_aset_nm": "",  # Optional - 기초자산명
    "rght_tp": "",  # Optional - 권리구분
    "expr_dt": "",  # Optional - 만기일
    "cur_prc": "",  # Optional - 현재가
    "pre_tp": "",  # Optional - 대비구분
    "pred_pre": "",  # Optional - 전일대비
    "flu_rt": "",  # Optional - 등락율
    "trde_qty": "",  # Optional - 거래량
    "trde_qty_pre": "",  # Optional - 거래량대비
    "trde_prica": "",  # Optional - 거래대금
    "pred_trde_qty": "",  # Optional - 전일거래량
    "sel_bid": "",  # Optional - 매도호가
    "buy_bid": "",  # Optional - 매수호가
    "prty": "",  # Optional - 패리티
    "gear_rt": "",  # Optional - 기어링비율
    "pl_qutr_rt": "",  # Optional - 손익분기율
    "cfp": "",  # Optional - 자본지지점
    "theory_pric": "",  # Optional - 이론가
    "innr_vltl": "",  # Optional - 내재변동성
    "delta": "",  # Optional - 델타
    "lvrg": "",  # Optional - 레버리지
    "exec_pric": "",  # Optional - 행사가격
    "cnvt_rt": "",  # Optional - 전환비율
    "lpposs_rt": "",  # Optional - LP보유비율
    "pl_qutr_pt": "",  # Optional - 손익분기점
    "fin_trde_dt": "",  # Optional - 최종거래일
    "flo_dt": "",  # Optional - 상장일
    "lpinitlast_suply_dt": "",  # Optional - LP초종공급일
    "stk_nm": "",  # Optional - 종목명
    "srvive_dys": "",  # Optional - 잔존일수
    "dispty_rt": "",  # Optional - 괴리율
    "lpmmcm_nm": "",  # Optional - LP회원사명
    "lpmmcm_nm_1": "",  # Optional - LP회원사명1
    "lpmmcm_nm_2": "",  # Optional - LP회원사명2
}
```

### Request Example

```json
{"isscomp_cd": "000000000017","bsis_aset_cd": "201","rght_tp": "1","lpcd": "000000000000","sort_tp": "0"}
```

---

## ELW등락율순위요청

**API ID**: `ka30009`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "sort_tp": "",  # Required - 정렬구분
    "rght_tp": "",  # Required - 권리구분
    "trde_end_skip": "",  # Required - 거래종료제외
    "rank": "",  # Optional - 순위
    "stk_cd": "",  # Optional - 종목코드
    "stk_nm": "",  # Optional - 종목명
    "cur_prc": "",  # Optional - 현재가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "flu_rt": "",  # Optional - 등락률
    "sel_req": "",  # Optional - 매도잔량
    "buy_req": "",  # Optional - 매수잔량
    "trde_qty": "",  # Optional - 거래량
    "trde_prica": "",  # Optional - 거래대금
}
```

### Request Example

```json
{"sort_tp": "1","rght_tp": "000","trde_end_skip": "0"}
```

---

## ELW잔량순위요청

**API ID**: `ka30010`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "sort_tp": "",  # Required - 정렬구분
    "rght_tp": "",  # Required - 권리구분
    "trde_end_skip": "",  # Required - 거래종료제외
    "stk_cd": "",  # Optional - 종목코드
    "rank": "",  # Optional - 순위
    "stk_nm": "",  # Optional - 종목명
    "cur_prc": "",  # Optional - 현재가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "flu_rt": "",  # Optional - 등락률
    "trde_qty": "",  # Optional - 거래량
    "sel_req": "",  # Optional - 매도잔량
    "buy_req": "",  # Optional - 매수잔량
    "netprps_req": "",  # Optional - 순매수잔량
    "trde_prica": "",  # Optional - 거래대금
}
```

### Request Example

```json
{"sort_tp": "1","rght_tp": "000","trde_end_skip": "0"}
```

---

## ELW근접율요청

**API ID**: `ka30011`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "stk_cd": "",  # Optional - 종목코드
    "stk_nm": "",  # Optional - 종목명
    "cur_prc": "",  # Optional - 현재가
    "pre_sig": "",  # Optional - 대비기호
    "pred_pre": "",  # Optional - 전일대비
    "flu_rt": "",  # Optional - 등락율
    "acc_trde_qty": "",  # Optional - 누적거래량
    "alacc_rt": "",  # Optional - 근접율
}
```

### Request Example

```json
{"stk_cd": "57JBHH"```Request Example```### }
```

---

## ELW종목상세정보요청

**API ID**: `ka30012`

### 기본 정보

- Method: `POST`
- URL: `/api/dostk/elw`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "stk_cd": "",  # Required - 종목코드
    "aset_cd": "",  # Optional - 자산코드
    "cur_prc": "",  # Optional - 현재가
    "pred_pre_sig": "",  # Optional - 전일대비기호
    "pred_pre": "",  # Optional - 전일대비
    "flu_rt": "",  # Optional - 등락율
    "lpmmcm_nm": "",  # Optional - LP회원사명
    "lpmmcm_nm_1": "",  # Optional - LP회원사명1
    "lpmmcm_nm_2": "",  # Optional - LP회원사명2
    "elwrght_cntn": "",  # Optional - ELW권리내용
    "elwexpr_evlt_pric": "",  # Optional - ELW만기평가가격
    "elwtheory_pric": "",  # Optional - ELW이론가
    "dispty_rt": "",  # Optional - 괴리율
    "elwinnr_vltl": "",  # Optional - ELW내재변동성
    "exp_rght_pric": "",  # Optional - 예상권리가
    "elwpl_qutr_rt": "",  # Optional - ELW손익분기율
    "elwexec_pric": "",  # Optional - ELW행사가
    "elwcnvt_rt": "",  # Optional - ELW전환비율
    "elwcmpn_rt": "",  # Optional - ELW보상율
    "elwpric_rising_part_rt": "",  # Optional - ELW가격상승참여율
    "elwrght_type": "",  # Optional - ELW권리유형
    "elwsrvive_dys": "",  # Optional - ELW잔존일수
    "stkcnt": "",  # Optional - 주식수
    "elwlpord_pos": "",  # Optional - ELWLP주문가능
    "lpposs_rt": "",  # Optional - LP보유비율
    "lprmnd_qty": "",  # Optional - LP보유수량
    "elwspread": "",  # Optional - ELW스프레드
    "elwprty": "",  # Optional - ELW패리티
    "elwgear": "",  # Optional - ELW기어링
    "elwflo_dt": "",  # Optional - ELW상장일
    "elwfin_trde_dt": "",  # Optional - ELW최종거래일
    "expr_dt": "",  # Optional - 만기일
    "exec_dt": "",  # Optional - 행사일
    "lpsuply_end_dt": "",  # Optional - LP공급종료일
    "elwpay_dt": "",  # Optional - ELW지급일
    "elwinvt_ix_comput": "",  # Optional - ELW투자지표산출
    "elwpay_agnt": "",  # Optional - ELW지급대리인
    "elwappr_way": "",  # Optional - ELW결재방법
    "elwrght_exec_way": "",  # Optional - ELW권리행사방식
    "elwpblicte_orgn": "",  # Optional - ELW발행기관
    "dcsn_pay_amt": "",  # Optional - 확정지급액
    "kobarr": "",  # Optional - KO베리어
    "iv": "",  # Optional - IV
    "clsprd_end_elwocr": "",  # Optional - 종기종료ELW발생
    "bsis_aset_1": "",  # Optional - 기초자산1
    "bsis_aset_comp_rt_1": "",  # Optional - 기초자산구성비율1
    "bsis_aset_2": "",  # Optional - 기초자산2
    "bsis_aset_comp_rt_2": "",  # Optional - 기초자산구성비율2
    "bsis_aset_3": "",  # Optional - 기초자산3
    "bsis_aset_comp_rt_3": "",  # Optional - 기초자산구성비율3
    "bsis_aset_4": "",  # Optional - 기초자산4
    "bsis_aset_comp_rt_4": "",  # Optional - 기초자산구성비율4
    "bsis_aset_5": "",  # Optional - 기초자산5
    "bsis_aset_comp_rt_5": "",  # Optional - 기초자산구성비율5
    "fr_dt": "",  # Optional - 평가시작일자
    "to_dt": "",  # Optional - 평가종료일자
    "fr_tm": "",  # Optional - 평가시작시간
    "evlt_end_tm": "",  # Optional - 평가종료시간
    "evlt_pric": "",  # Optional - 평가가격
    "evlt_fnsh_yn": "",  # Optional - 평가완료여부
    "all_hgst_pric": "",  # Optional - 전체최고가
    "all_lwst_pric": "",  # Optional - 전체최저가
    "imaf_hgst_pric": "",  # Optional - 직후최고가
    "imaf_lwst_pric": "",  # Optional - 직후최저가
}
```

### Request Example

```json
{"stk_cd": "57JBHH"}
```

---
