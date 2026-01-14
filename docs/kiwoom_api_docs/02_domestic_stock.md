# 키움 REST API - 국내주식

## 개요
이 문서는 키움 REST API의 국내주식 관련 API를 정리한 것입니다.

## ELW

### ELW일별민감도지표요청 (ka10048)

#### 기본 정보

- **API ID**: `ka10048`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| dt | 일자 | N |
| iv | IV | N |
| delta | 델타 | N |
| gam | 감마 | N |
| theta | 쎄타 | N |
| vega | 베가 | N |
| law | 로 | N |
| lp | LP | N |

#### Request Example

```json
{"stk_cd": "57JBHH"```Request Example```### }
```

---

### ELW민감도지표요청 (ka10050)

#### 기본 정보

- **API ID**: `ka10050`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| cntr_tm | 체결시간 | N |
| cur_prc | 현재가 | N |
| elwtheory_pric | ELW이론가 | N |
| iv | IV | N |
| delta | 델타 | N |
| gam | 감마 | N |
| theta | 쎄타 | N |
| vega | 베가 | N |
| law | 로 | N |
| lp | LP | N |

#### Request Example

```json
{"stk_cd": "57JBHH"}
```

---

### ELW가격급등락요청 (ka30001)

#### 기본 정보

- **API ID**: `ka30001`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| flu_tp | 등락구분 | Y |
| tm_tp | 시간구분 | Y |
| tm | 시간 | Y |
| trde_qty_tp | 거래량구분 | Y |
| isscomp_cd | 발행사코드 | Y |
| bsis_aset_cd | 기초자산코드 | Y |
| rght_tp | 권리구분 | Y |
| lpcd | LP코드 | Y |
| trde_end_elwskip | 거래종료ELW제외 | Y |
| base_pric_tm | 기준가시간 | N |
| stk_cd | 종목코드 | N |
| rank | 순위 | N |
| stk_nm | 종목명 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| cur_prc | 현재가 | N |
| base_pre | 기준대비 | N |
| trde_qty | 거래량 | N |
| jmp_rt | 급등율 | N |

#### Request Example

```json
{"flu_tp": "1","tm_tp": "2","tm": "1","trde_qty_tp": "0","isscomp_cd": "000000000000","bsis_aset_cd": "000000000000","rght_tp": "000","lpcd": "000000000000","trde_end_elwskip": "0"}
```

---

### 거래원별ELW순매매상위요청 (ka30002)

#### 기본 정보

- **API ID**: `ka30002`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| isscomp_cd | 발행사코드 | Y |
| trde_qty_tp | 거래량구분 | Y |
| trde_tp | 매매구분 | Y |
| dt | 기간 | Y |
| trde_end_elwskip | 거래종료ELW제외 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| stkpc_flu | 주가등락 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| netprps | 순매수 | N |
| buy_trde_qty | 매수거래량 | N |
| sel_trde_qty | 매도거래량 | N |

#### Request Example

```json
{"isscomp_cd": "003","trde_qty_tp": "0","trde_tp": "2","dt": "60","trde_end_elwskip": "0"}
```

---

### ELWLP보유일별추이요청 (ka30003)

#### 기본 정보

- **API ID**: `ka30003`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| bsis_aset_cd | 기초자산코드 | Y |
| base_dt | 기준일자 | Y |
| dt | 일자 | N |
| cur_prc | 현재가 | N |
| pre_tp | 대비구분 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| chg_qty | 변동수량 | N |
| lprmnd_qty | LP보유수량 | N |
| wght | 비중 | N |

#### Request Example

```json
{"bsis_aset_cd": "57KJ99","base_dt": "20241122"}
```

---

### ELW괴리율요청 (ka30004)

#### 기본 정보

- **API ID**: `ka30004`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| isscomp_cd | 발행사코드 | Y |
| bsis_aset_cd | 기초자산코드 | Y |
| rght_tp | 권리구분 | Y |
| lpcd | LP코드 | Y |
| trde_end_elwskip | 거래종료ELW제외 | Y |
| stk_cd | 종목코드 | N |
| isscomp_nm | 발행사명 | N |
| sqnc | 회차 | N |
| base_aset_nm | 기초자산명 | N |
| rght_tp | 권리구분 | N |
| dispty_rt | 괴리율 | N |
| basis | 베이시스 | N |
| srvive_dys | 잔존일수 | N |
| theory_pric | 이론가 | N |
| cur_prc | 현재가 | N |
| pre_tp | 대비구분 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| stk_nm | 종목명 | N |

#### Request Example

```json
{"isscomp_cd": "000000000000","bsis_aset_cd": "000000000000","rght_tp": "000","lpcd": "000000000000","trde_end_elwskip": "0"}
```

---

### ELW조건검색요청 (ka30005)

#### 기본 정보

- **API ID**: `ka30005`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| isscomp_cd | 발행사코드 | Y |
| bsis_aset_cd | 기초자산코드 | Y |
| rght_tp | 권리구분 | Y |
| lpcd | LP코드 | Y |
| sort_tp | 정렬구분 | Y |
| stk_cd | 종목코드 | N |
| isscomp_nm | 발행사명 | N |
| sqnc | 회차 | N |
| base_aset_nm | 기초자산명 | N |
| rght_tp | 권리구분 | N |
| expr_dt | 만기일 | N |
| cur_prc | 현재가 | N |
| pre_tp | 대비구분 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| trde_qty_pre | 거래량대비 | N |
| trde_prica | 거래대금 | N |
| pred_trde_qty | 전일거래량 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| prty | 패리티 | N |
| gear_rt | 기어링비율 | N |
| pl_qutr_rt | 손익분기율 | N |
| cfp | 자본지지점 | N |
| theory_pric | 이론가 | N |
| innr_vltl | 내재변동성 | N |
| delta | 델타 | N |
| lvrg | 레버리지 | N |
| exec_pric | 행사가격 | N |
| cnvt_rt | 전환비율 | N |
| lpposs_rt | LP보유비율 | N |
| pl_qutr_pt | 손익분기점 | N |
| fin_trde_dt | 최종거래일 | N |
| flo_dt | 상장일 | N |
| lpinitlast_suply_dt | LP초종공급일 | N |
| stk_nm | 종목명 | N |
| srvive_dys | 잔존일수 | N |
| dispty_rt | 괴리율 | N |
| lpmmcm_nm | LP회원사명 | N |
| lpmmcm_nm_1 | LP회원사명1 | N |
| lpmmcm_nm_2 | LP회원사명2 | N |

#### Request Example

```json
{"isscomp_cd": "000000000017","bsis_aset_cd": "201","rght_tp": "1","lpcd": "000000000000","sort_tp": "0"}
```

---

### ELW등락율순위요청 (ka30009)

#### 기본 정보

- **API ID**: `ka30009`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| sort_tp | 정렬구분 | Y |
| rght_tp | 권리구분 | Y |
| trde_end_skip | 거래종료제외 | Y |
| rank | 순위 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| sel_req | 매도잔량 | N |
| buy_req | 매수잔량 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |

#### Request Example

```json
{"sort_tp": "1","rght_tp": "000","trde_end_skip": "0"}
```

---

### ELW잔량순위요청 (ka30010)

#### 기본 정보

- **API ID**: `ka30010`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| sort_tp | 정렬구분 | Y |
| rght_tp | 권리구분 | Y |
| trde_end_skip | 거래종료제외 | Y |
| stk_cd | 종목코드 | N |
| rank | 순위 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| sel_req | 매도잔량 | N |
| buy_req | 매수잔량 | N |
| netprps_req | 순매수잔량 | N |
| trde_prica | 거래대금 | N |

#### Request Example

```json
{"sort_tp": "1","rght_tp": "000","trde_end_skip": "0"}
```

---

### ELW근접율요청 (ka30011)

#### 기본 정보

- **API ID**: `ka30011`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| acc_trde_qty | 누적거래량 | N |
| alacc_rt | 근접율 | N |

#### Request Example

```json
{"stk_cd": "57JBHH"```Request Example```### }
```

---

### ELW종목상세정보요청 (ka30012)

#### 기본 정보

- **API ID**: `ka30012`
- **Method**: `POST`
- **URL**: `/api/dostk/elw`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| aset_cd | 자산코드 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| lpmmcm_nm | LP회원사명 | N |
| lpmmcm_nm_1 | LP회원사명1 | N |
| lpmmcm_nm_2 | LP회원사명2 | N |
| elwrght_cntn | ELW권리내용 | N |
| elwexpr_evlt_pric | ELW만기평가가격 | N |
| elwtheory_pric | ELW이론가 | N |
| dispty_rt | 괴리율 | N |
| elwinnr_vltl | ELW내재변동성 | N |
| exp_rght_pric | 예상권리가 | N |
| elwpl_qutr_rt | ELW손익분기율 | N |
| elwexec_pric | ELW행사가 | N |
| elwcnvt_rt | ELW전환비율 | N |
| elwcmpn_rt | ELW보상율 | N |
| elwpric_rising_part_rt | ELW가격상승참여율 | N |
| elwrght_type | ELW권리유형 | N |
| elwsrvive_dys | ELW잔존일수 | N |
| stkcnt | 주식수 | N |
| elwlpord_pos | ELWLP주문가능 | N |
| lpposs_rt | LP보유비율 | N |
| lprmnd_qty | LP보유수량 | N |
| elwspread | ELW스프레드 | N |
| elwprty | ELW패리티 | N |
| elwgear | ELW기어링 | N |
| elwflo_dt | ELW상장일 | N |
| elwfin_trde_dt | ELW최종거래일 | N |
| expr_dt | 만기일 | N |
| exec_dt | 행사일 | N |
| lpsuply_end_dt | LP공급종료일 | N |
| elwpay_dt | ELW지급일 | N |
| elwinvt_ix_comput | ELW투자지표산출 | N |
| elwpay_agnt | ELW지급대리인 | N |
| elwappr_way | ELW결재방법 | N |
| elwrght_exec_way | ELW권리행사방식 | N |
| elwpblicte_orgn | ELW발행기관 | N |
| dcsn_pay_amt | 확정지급액 | N |
| kobarr | KO베리어 | N |
| iv | IV | N |
| clsprd_end_elwocr | 종기종료ELW발생 | N |
| bsis_aset_1 | 기초자산1 | N |
| bsis_aset_comp_rt_1 | 기초자산구성비율1 | N |
| bsis_aset_2 | 기초자산2 | N |
| bsis_aset_comp_rt_2 | 기초자산구성비율2 | N |
| bsis_aset_3 | 기초자산3 | N |
| bsis_aset_comp_rt_3 | 기초자산구성비율3 | N |
| bsis_aset_4 | 기초자산4 | N |
| bsis_aset_comp_rt_4 | 기초자산구성비율4 | N |
| bsis_aset_5 | 기초자산5 | N |
| bsis_aset_comp_rt_5 | 기초자산구성비율5 | N |
| fr_dt | 평가시작일자 | N |
| to_dt | 평가종료일자 | N |
| fr_tm | 평가시작시간 | N |
| evlt_end_tm | 평가종료시간 | N |
| evlt_pric | 평가가격 | N |
| evlt_fnsh_yn | 평가완료여부 | N |
| all_hgst_pric | 전체최고가 | N |
| all_lwst_pric | 전체최저가 | N |
| imaf_hgst_pric | 직후최고가 | N |
| imaf_lwst_pric | 직후최저가 | N |

#### Request Example

```json
{"stk_cd": "57JBHH"}
```

---

## ETF

### ETF수익율요청 (ka40001)

#### 기본 정보

- **API ID**: `ka40001`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| etfobjt_idex_cd | ETF대상지수코드 | Y |
| dt | 기간 | Y |
| etfprft_rt | ETF수익률 | N |
| cntr_prft_rt | 체결수익률 | N |
| for_netprps_qty | 외인순매수수량 | N |
| orgn_netprps_qty | 기관순매수수량 | N |

#### Request Example

```json
{"stk_cd": "069500","etfobjt_idex_cd": "207","dt": "3"}
```

---

### ETF종목정보요청 (ka40002)

#### 기본 정보

- **API ID**: `ka40002`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| stk_nm | 종목명 | N |
| etfobjt_idex_nm | ETF대상지수명 | N |
| wonju_pric | 원주가격 | N |
| etftxon_type | ETF과세유형 | N |
| etntxon_type | ETN과세유형 | N |

#### Request Example

```json
{"stk_cd": "069500"}
```

---

### ETF일별추이요청 (ka40003)

#### 기본 정보

- **API ID**: `ka40003`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| cntr_dt | 체결일자 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| pre_rt | 대비율 | N |
| trde_qty | 거래량 | N |
| nav | NAV | N |
| acc_trde_prica | 누적거래대금 | N |
| navidex_dispty_rt | NAV/지수괴리율 | N |
| navetfdispty_rt | NAV/ETF괴리율 | N |
| trace_eor_rt | 추적오차율 | N |
| trace_cur_prc | 추적현재가 | N |
| trace_pred_pre | 추적전일대비 | N |
| trace_pre_sig | 추적대비기호 | N |

#### Request Example

```json
{"stk_cd": "069500"}
```

---

### ETF전체시세요청 (ka40004)

#### 기본 정보

- **API ID**: `ka40004`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| txon_type | 과세유형 | Y |
| navpre | NAV대비 | Y |
| mngmcomp | 운용사 | Y |
| txon_yn | 과세여부 | Y |
| trace_idex | 추적지수 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_cls | 종목분류 | N |
| stk_nm | 종목명 | N |
| close_pric | 종가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| pre_rt | 대비율 | N |
| trde_qty | 거래량 | N |
| nav | NAV | N |
| trace_eor_rt | 추적오차율 | N |
| txbs | 과표기준 | N |
| dvid_bf_base | 배당전기준 | N |
| pred_dvida | 전일배당금 | N |
| trace_idex_nm | 추적지수명 | N |
| drng | 배수 | N |
| trace_idex_cd | 추적지수코드 | N |
| trace_idex | 추적지수 | N |
| trace_flu_rt | 추적등락율 | N |

#### Request Example

```json
{"txon_type": "0","navpre": "0","mngmcomp": "0000","txon_yn": "0","trace_idex": "0","stex_tp": "1"}
```

---

### ETF시간대별추이요청 (ka40006)

#### 기본 정보

- **API ID**: `ka40006`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| stk_nm | 종목명 | N |
| etfobjt_idex_nm | ETF대상지수명 | N |
| wonju_pric | 원주가격 | N |
| etftxon_type | ETF과세유형 | N |
| etntxon_type | ETN과세유형 | N |
| tm | 시간 | N |
| close_pric | 종가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| nav | NAV | N |
| trde_prica | 거래대금 | N |
| navidex | NAV지수 | N |
| navetf | NAVETF | N |
| trace | 추적 | N |
| trace_idex | 추적지수 | N |

#### Request Example

```json
{"stk_cd": "069500"}
```

---

### ETF시간대별체결요청 (ka40007)

#### 기본 정보

- **API ID**: `ka40007`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| stk_cls | 종목분류 | N |
| stk_nm | 종목명 | N |
| etfobjt_idex_nm | ETF대상지수명 | N |
| etfobjt_idex_cd | ETF대상지수코드 | N |
| objt_idex_pre_rt | 대상지수대비율 | N |
| wonju_pric | 원주가격 | N |
| cntr_tm | 체결시간 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |
| stex_tp | 거래소구분 | N |

#### Request Example

```json
{"stk_cd": "069500"}
```

---

### ETF일자별체결요청 (ka40008)

#### 기본 정보

- **API ID**: `ka40008`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| cntr_tm | 체결시간 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |
| dt | 일자 | N |
| cur_prc_n | 현재가n | N |
| pre_sig_n | 대비기호n | N |
| pred_pre_n | 전일대비n | N |
| acc_trde_qty | 누적거래량 | N |
| for_netprps_qty | 외인순매수수량 | N |
| orgn_netprps_qty | 기관순매수수량 | N |

#### Request Example

```json
{"stk_cd": "069500"}
```

---

### ETF시간대별체결요청 (ka40009)

#### 기본 정보

- **API ID**: `ka40009`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| nav | NAV | N |
| navpred_pre | NAV전일대비 | N |
| navflu_rt | NAV등락율 | N |
| trace_eor_rt | 추적오차율 | N |
| dispty_rt | 괴리율 | N |
| stkcnt | 주식수 | N |
| base_pric | 기준가 | N |
| for_rmnd_qty | 외인보유수량 | N |
| repl_pric | 대용가 | N |
| conv_pric | 환산가격 | N |
| drstk | DR/주 | N |
| wonju_pric | 원주가격 | N |

#### Request Example

```json
{"stk_cd": "069500"}
```

---

### ETF시간대별추이요청 (ka40010)

#### 기본 정보

- **API ID**: `ka40010`
- **Method**: `POST`
- **URL**: `/api/dostk/etf`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |
| for_netprps | 외인순매수 | N |

#### Request Example

```json
{"stk_cd": "069500"}
```

---

## 계좌

### 일별잔고수익률 (ka01690)

#### 기본 정보

- **API ID**: `ka01690`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 아니오

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| qry_dt | 조회일자 | Y |
| dt | 일자 | N |
| tot_buy_amt | 총 매입가 | N |
| tot_evlt_amt | 총 평가금액 | N |
| tot_evltv_prft | 총 평가손익 | N |
| tot_prft_rt | 수익률 | N |
| dbst_bal | 예수금 | N |
| day_stk_asst | 추정자산 | N |
| buy_wght | 현금비중 | N |
| cur_prc | 현재가 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| rmnd_qty | 보유 수량 | N |
| buy_uv | 매입 단가 | N |
| buy_wght | 매수비중 | N |
| evltv_prft | 평가손익 | N |
| prft_rt | 수익률 | N |
| evlt_amt | 평가금액 | N |
| evlt_wght | 평가비중 | N |

#### Request Example

```json
{"qry_dt": "20250825"}
```

---

### 일자별종목별실현손익요청_일자 (ka10072)

#### 기본 정보

- **API ID**: `ka10072`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | N |
| strt_dt | 시작일자 | Y |
| stk_nm | 종목명 | N |
| cntr_qty | 체결량 | N |
| buy_uv | 매입단가 | N |
| cntr_pric | 체결가 | N |
| tdy_sel_pl | 당일매도손익 | N |
| pl_rt | 손익율 | N |
| stk_cd | 종목코드 | N |
| tdy_trde_cmsn | 당일매매수수료 | N |
| tdy_trde_tax | 당일매매세금 | N |
| wthd_alowa | 인출가능금액 | N |
| loan_dt | 대출일 | N |
| crd_tp | 신용구분 | N |
| stk_cd_1 | 종목코드1 | N |
| tdy_sel_pl_1 | 당일매도손익1 | N |

#### Request Example

```json
{"stk_cd": "005930","strt_dt": "20241128"}
```

---

### 일자별종목별실현손익요청_기간 (ka10073)

#### 기본 정보

- **API ID**: `ka10073`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | N |
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| dt | 일자 | N |
| tdy_htssel_cmsn | 당일hts매도수수료 | N |
| stk_nm | 종목명 | N |
| cntr_qty | 체결량 | N |
| buy_uv | 매입단가 | N |
| cntr_pric | 체결가 | N |
| tdy_sel_pl | 당일매도손익 | N |
| pl_rt | 손익율 | N |
| stk_cd | 종목코드 | N |
| tdy_trde_cmsn | 당일매매수수료 | N |
| tdy_trde_tax | 당일매매세금 | N |
| wthd_alowa | 인출가능금액 | N |
| loan_dt | 대출일 | N |
| crd_tp | 신용구분 | N |

#### Request Example

```json
{"stk_cd": "005930","strt_dt": "20241128","end_dt": "20241128"}
```

---

### 일자별실현손익요청 (ka10074)

#### 기본 정보

- **API ID**: `ka10074`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| tot_buy_amt | 총매수금액 | N |
| tot_sell_amt | 총매도금액 | N |
| rlzt_pl | 실현손익 | N |
| trde_cmsn | 매매수수료 | N |
| trde_tax | 매매세금 | N |
| dt | 일자 | N |
| buy_amt | 매수금액 | N |
| sell_amt | 매도금액 | N |
| tdy_sel_pl | 당일매도손익 | N |
| tdy_trde_cmsn | 당일매매수수료 | N |
| tdy_trde_tax | 당일매매세금 | N |

#### Request Example

```json
{"strt_dt": "20241128","end_dt": "20241128"}
```

---

### 미체결요청 (ka10075)

#### 기본 정보

- **API ID**: `ka10075`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| all_stk_tp | 전체종목구분 | Y |
| trde_tp | 매매구분 | Y |
| stk_cd | 종목코드 | N |
| stex_tp | 거래소구분 | Y |
| acnt_no | 계좌번호 | N |
| ord_no | 주문번호 | N |
| mang_empno | 관리사번 | N |
| stk_cd | 종목코드 | N |
| tsk_tp | 업무구분 | N |
| ord_stt | 주문상태 | N |
| stk_nm | 종목명 | N |
| ord_qty | 주문수량 | N |
| ord_pric | 주문가격 | N |
| oso_qty | 미체결수량 | N |
| cntr_tot_amt | 체결누계금액 | N |
| orig_ord_no | 원주문번호 | N |
| io_tp_nm | 주문구분 | N |
| trde_tp | 매매구분 | N |
| tm | 시간 | N |
| cntr_no | 체결번호 | N |
| cntr_pric | 체결가 | N |
| cntr_qty | 체결량 | N |
| cur_prc | 현재가 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| unit_cntr_pric | 단위체결가 | N |
| unit_cntr_qty | 단위체결량 | N |
| tdy_trde_cmsn | 당일매매수수료 | N |
| tdy_trde_tax | 당일매매세금 | N |
| ind_invsr | 개인투자자 | N |
| stex_tp | 거래소구분 | N |
| stex_tp_txt | 거래소구분텍스트 | N |
| sor_yn | SOR 여부값 | N |
| stop_pric | 스톱가 | N |

#### Request Example

```json
{"all_stk_tp": "1","trde_tp": "0","stk_cd": "005930","stex_tp": "0"}
```

---

### 체결요청 (ka10076)

#### 기본 정보

- **API ID**: `ka10076`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | N |
| qry_tp | 조회구분 | Y |
| sell_tp | 매도수구분 | Y |
| ord_no | 주문번호 | N |
| stex_tp | 거래소구분 | Y |
| ord_no | 주문번호 | N |
| stk_nm | 종목명 | N |
| io_tp_nm | 주문구분 | N |
| ord_pric | 주문가격 | N |
| ord_qty | 주문수량 | N |
| cntr_pric | 체결가 | N |
| cntr_qty | 체결량 | N |
| oso_qty | 미체결수량 | N |
| tdy_trde_cmsn | 당일매매수수료 | N |
| tdy_trde_tax | 당일매매세금 | N |
| ord_stt | 주문상태 | N |
| trde_tp | 매매구분 | N |
| orig_ord_no | 원주문번호 | N |
| ord_tm | 주문시간 | N |
| stk_cd | 종목코드 | N |
| stex_tp | 거래소구분 | N |
| stex_tp_txt | 거래소구분텍스트 | N |
| sor_yn | SOR 여부값 | N |
| stop_pric | 스톱가 | N |

#### Request Example

```json
{"stk_cd": "005930","qry_tp": "1","sell_tp": "0","ord_no": "","stex_tp": "0"}
```

---

### 당일실현손익상세요청 (ka10077)

#### 기본 정보

- **API ID**: `ka10077`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tdy_rlzt_pl | 당일실현손익 | N |
| stk_nm | 종목명 | N |
| cntr_qty | 체결량 | N |
| buy_uv | 매입단가 | N |
| cntr_pric | 체결가 | N |
| tdy_sel_pl | 당일매도손익 | N |
| pl_rt | 손익율 | N |
| tdy_trde_cmsn | 당일매매수수료 | N |
| tdy_trde_tax | 당일매매세금 | N |
| stk_cd | 종목코드 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 계좌수익률요청 (ka10085)

#### 기본 정보

- **API ID**: `ka10085`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stex_tp | 거래소구분 | Y |
| dt | 일자 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pur_pric | 매입가 | N |
| pur_amt | 매입금액 | N |
| rmnd_qty | 보유수량 | N |
| tdy_sel_pl | 당일매도손익 | N |
| tdy_trde_cmsn | 당일매매수수료 | N |
| tdy_trde_tax | 당일매매세금 | N |
| crd_tp | 신용구분 | N |
| loan_dt | 대출일 | N |
| setl_remn | 결제잔고 | N |
| clrn_alow_qty | 청산가능수량 | N |
| crd_amt | 신용금액 | N |
| crd_int | 신용이자 | N |
| expr_dt | 만기일 | N |

#### Request Example

```json
{"stex_tp": "0"}
```

---

### 미체결 분할주문 상세 (ka10088)

#### 기본 정보

- **API ID**: `ka10088`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| ord_no | 주문번호 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| ord_no | 주문번호 | N |
| ord_qty | 주문수량 | N |
| ord_pric | 주문가격 | N |
| osop_qty | 미체결수량 | N |
| io_tp_nm | 주문구분 | N |
| trde_tp | 매매구분 | N |
| sell_tp | 매도/수 구분 | N |
| cntr_qty | 체결량 | N |
| ord_stt | 주문상태 | N |
| cur_prc | 현재가 | N |
| stex_tp | 거래소구분 | N |
| stex_tp_txt | 거래소구분텍스트 | N |

#### Request Example

```json
{"ord_no": "8"}
```

---

### 당일매매일지요청 (ka10170)

#### 기본 정보

- **API ID**: `ka10170`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| base_dt | 기준일자 | N |
| ottks_tp | 단주구분 | Y |
| ch_crd_tp | 현금신용구분 | Y |
| tot_sell_amt | 총매도금액 | N |
| tot_buy_amt | 총매수금액 | N |
| tot_cmsn_tax | 총수수료_세금 | N |
| tot_exct_amt | 총정산금액 | N |
| tot_pl_amt | 총손익금액 | N |
| tot_prft_rt | 총수익률 | N |
| stk_nm | 종목명 | N |
| buy_avg_pric | 매수평균가 | N |
| buy_qty | 매수수량 | N |
| sel_avg_pric | 매도평균가 | N |
| sell_qty | 매도수량 | N |
| cmsn_alm_tax | 수수료_제세금 | N |
| pl_amt | 손익금액 | N |
| sell_amt | 매도금액 | N |
| buy_amt | 매수금액 | N |
| prft_rt | 수익률 | N |
| stk_cd | 종목코드 | N |

#### Request Example

```json
{"base_dt": "20241120","ottks_tp": "1","ch_crd_tp": "0"}
```

---

### 예수금상세현황요청 (kt00001)

#### 기본 정보

- **API ID**: `kt00001`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| qry_tp | 조회구분 | Y |
| entr | 예수금 | N |
| profa_ch | 주식증거금현금 | N |
| bncr_profa_ch | 수익증권증거금현금 | N |
| crd_grnta_ch | 신용보증금현금 | N |
| crd_grnt_ch | 신용담보금현금 | N |
| add_grnt_ch | 추가담보금현금 | N |
| etc_profa | 기타증거금 | N |
| uncl_stk_amt | 미수확보금 | N |
| shrts_prica | 공매도대금 | N |
| crd_set_grnta | 신용설정평가금 | N |
| chck_ina_amt | 수표입금액 | N |
| etc_chck_ina_amt | 기타수표입금액 | N |
| crd_grnt_ruse | 신용담보재사용 | N |
| knx_asset_evltv | 코넥스기본예탁금 | N |
| elwdpst_evlta | ELW예탁평가금 | N |
| lvlh_join_amt | 생계형가입금액 | N |
| lvlh_trns_alowa | 생계형입금가능금액 | N |
| remn_repl_evlta | 잔고대용평가금액 | N |
| profa_repl | 위탁증거금대용 | N |
| crd_grnta_repl | 신용보증금대용 | N |
| crd_grnt_repl | 신용담보금대용 | N |
| add_grnt_repl | 추가담보금대용 | N |
| rght_repl_amt | 권리대용금 | N |
| pymn_alow_amt | 출금가능금액 | N |
| ord_alow_amt | 주문가능금액 | N |
| ch_uncla | 현금미수금 | N |
| ch_uncla_dlfe | 현금미수연체료 | N |
| ch_uncla_tot | 현금미수금합계 | N |
| crd_int_npay | 신용이자미납 | N |
| int_npay_amt_dlfe | 신용이자미납연체료 | N |
| int_npay_amt_tot | 신용이자미납합계 | N |
| etc_loana | 기타대여금 | N |
| etc_loana_dlfe | 기타대여금연체료 | N |
| etc_loan_tot | 기타대여금합계 | N |
| nrpy_loan | 미상환융자금 | N |
| loan_sum | 융자금합계 | N |
| ls_sum | 대주금합계 | N |
| crd_grnt_rt | 신용담보비율 | N |
| mdstrm_usfe | 중도이용료 | N |
| min_ord_alow_yn | 최소주문가능금액 | N |
| loan_remn_evlt_amt | 대출총평가금액 | N |
| dpst_grntl_remn | 예탁담보대출잔고 | N |
| sell_grntl_remn | 매도담보대출잔고 | N |
| d1_entra | d+1추정예수금 | N |
| d1_slby_exct_amt | d+1매도매수정산금 | N |
| d1_buy_exct_amt | d+1매수정산금 | N |
| d1_out_rep_mor | d+1미수변제소요금 | N |
| d1_sel_exct_amt | d+1매도정산금 | N |
| d1_pymn_alow_amt | d+1출금가능금액 | N |
| d2_entra | d+2추정예수금 | N |
| d2_slby_exct_amt | d+2매도매수정산금 | N |
| d2_buy_exct_amt | d+2매수정산금 | N |
| d2_out_rep_mor | d+2미수변제소요금 | N |
| d2_sel_exct_amt | d+2매도정산금 | N |
| d2_pymn_alow_amt | d+2출금가능금액 | N |
| crnc_cd | 통화코드 | N |
| fx_entr | 외화예수금 | N |
| fc_krw_repl_evlta | 원화대용평가금 | N |
| fc_trst_profa | 해외주식증거금 | N |
| pymn_alow_amt | 출금가능금액 | N |
| fc_uncla | 외화미수(합계) | N |
| fc_ch_uncla | 외화현금미수금 | N |
| dly_amt | 연체료 | N |
| d1_fx_entr | d+1외화예수금 | N |
| d2_fx_entr | d+2외화예수금 | N |
| d3_fx_entr | d+3외화예수금 | N |
| d4_fx_entr | d+4외화예수금 | N |

#### Request Example

```json
{"qry_tp": "3"}
```

---

### 일별추정예탁자산현황요청 (kt00002)

#### 기본 정보

- **API ID**: `kt00002`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| start_dt | 시작조회기간 | Y |
| end_dt | 종료조회기간 | Y |
| dt | 일자 | N |
| entr | 예수금 | N |
| grnt_use_amt | 담보대출금 | N |
| crd_loan | 신용융자금 | N |
| ls_grnt | 대주담보금 | N |
| repl_amt | 대용금 | N |

#### Request Example

```json
{"start_dt": "20241111","end_dt": "20241125"}
```

---

### 추정자산조회요청 (kt00003)

#### 기본 정보

- **API ID**: `kt00003`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| qry_tp | 상장폐지조회구분 | Y |
| prsm_dpst_aset_amt | 추정예탁자산 | N |

#### Request Example

```json
{"qry_tp": "0"}
```

---

### 계좌평가현황요청 (kt00004)

#### 기본 정보

- **API ID**: `kt00004`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| qry_tp | 상장폐지조회구분 | Y |
| dmst_stex_tp | 국내거래소구분 | Y |
| acnt_nm | 계좌명 | N |
| brch_nm | 지점명 | N |
| entr | 예수금 | N |
| d2_entra | D+2추정예수금 | N |
| tot_est_amt | 유가잔고평가액 | N |
| aset_evlt_amt | 예탁자산평가액 | N |
| tot_pur_amt | 총매입금액 | N |
| prsm_dpst_aset_amt | 추정예탁자산 | N |
| tot_grnt_sella | 매도담보대출금 | N |
| tdy_lspft_amt | 당일투자원금 | N |
| invt_bsamt | 당월투자원금 | N |
| lspft_amt | 누적투자원금 | N |
| tdy_lspft | 당일투자손익 | N |
| lspft2 | 당월투자손익 | N |
| lspft | 누적투자손익 | N |
| tdy_lspft_rt | 당일손익율 | N |
| lspft_ratio | 당월손익율 | N |
| lspft_rt | 누적손익율 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| rmnd_qty | 보유수량 | N |
| avg_prc | 평균단가 | N |
| cur_prc | 현재가 | N |
| evlt_amt | 평가금액 | N |
| pl_amt | 손익금액 | N |
| pl_rt | 손익율 | N |
| loan_dt | 대출일 | N |
| pur_amt | 매입금액 | N |
| setl_remn | 결제잔고 | N |
| pred_buyq | 전일매수수량 | N |
| pred_sellq | 전일매도수량 | N |
| tdy_buyq | 금일매수수량 | N |
| tdy_sellq | 금일매도수량 | N |

#### Request Example

```json
{"qry_tp": "0","dmst_stex_tp": "KRX"}
```

---

### 체결잔고요청 (kt00005)

#### 기본 정보

- **API ID**: `kt00005`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| entr | 예수금 | N |
| entr_d1 | 예수금D+1 | N |
| entr_d2 | 예수금D+2 | N |
| pymn_alow_amt | 출금가능금액 | N |
| uncl_stk_amt | 미수확보금 | N |
| repl_amt | 대용금 | N |
| rght_repl_amt | 권리대용금 | N |
| ord_alowa | 주문가능현금 | N |
| ch_uncla | 현금미수금 | N |
| crd_int_npay_gold | 신용이자미납금 | N |
| etc_loana | 기타대여금 | N |
| nrpy_loan | 미상환융자금 | N |
| profa_ch | 증거금현금 | N |
| repl_profa | 증거금대용 | N |
| stk_buy_tot_amt | 주식매수총액 | N |
| evlt_amt_tot | 평가금액합계 | N |
| tot_pl_tot | 총손익합계 | N |
| tot_pl_rt | 총손익률 | N |
| tot_re_buy_alowa | 총재매수가능금액 | N |
| 20ord_alow_amt | 20%주문가능금액 | N |
| 30ord_alow_amt | 30%주문가능금액 | N |
| 40ord_alow_amt | 40%주문가능금액 | N |
| 50ord_alow_amt | 50%주문가능금액 | N |
| 60ord_alow_amt | 60%주문가능금액 | N |
| 100ord_alow_amt | 100%주문가능금액 | N |
| crd_loan_tot | 신용융자합계 | N |
| crd_loan_ls_tot | 신용융자대주합계 | N |
| crd_grnt_rt | 신용담보비율 | N |
| grnt_loan_amt | 매도담보대출금액 | N |
| crd_tp | 신용구분 | N |
| loan_dt | 대출일 | N |
| expr_dt | 만기일 | N |
| stk_cd | 종목번호 | N |
| stk_nm | 종목명 | N |
| setl_remn | 결제잔고 | N |
| cur_qty | 현재잔고 | N |
| cur_prc | 현재가 | N |
| buy_uv | 매입단가 | N |
| pur_amt | 매입금액 | N |
| evlt_amt | 평가금액 | N |
| evltv_prft | 평가손익 | N |
| pl_rt | 손익률 | N |

#### Request Example

```json
{"dmst_stex_tp": "KRX"}
```

---

### 계좌별주문체결내역상세요청 (kt00007)

#### 기본 정보

- **API ID**: `kt00007`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| ord_dt | 주문일자 | N |
| qry_tp | 조회구분 | Y |
| stk_bond_tp | 주식채권구분 | Y |
| sell_tp | 매도수구분 | Y |
| stk_cd | 종목코드 | N |
| fr_ord_no | 시작주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | Y |
| ord_no | 주문번호 | N |
| stk_cd | 종목번호 | N |
| trde_tp | 매매구분 | N |
| crd_tp | 신용구분 | N |
| ord_qty | 주문수량 | N |
| ord_uv | 주문단가 | N |
| cnfm_qty | 확인수량 | N |
| acpt_tp | 접수구분 | N |
| rsrv_tp | 반대여부 | N |
| ord_tm | 주문시간 | N |
| ori_ord | 원주문 | N |
| stk_nm | 종목명 | N |
| io_tp_nm | 주문구분 | N |
| loan_dt | 대출일 | N |
| cntr_qty | 체결수량 | N |
| cntr_uv | 체결단가 | N |
| ord_remnq | 주문잔량 | N |
| comm_ord_tp | 통신구분 | N |
| mdfy_cncl | 정정취소 | N |
| cnfm_tm | 확인시간 | N |
| dmst_stex_tp | 국내거래소구분 | N |
| cond_uv | 스톱가 | N |

#### Request Example

```json
{"ord_dt": "","qry_tp": "1","stk_bond_tp": "0","sell_tp": "0","stk_cd": "005930","fr_ord_no": "","dmst_stex_tp": "%"}
```

---

### 계좌별익일결제예정내역요청 (kt00008)

#### 기본 정보

- **API ID**: `kt00008`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dcd_seq | 시작결제번호 | N |
| trde_dt | 매매일자 | N |
| setl_dt | 결제일자 | N |
| sell_amt_sum | 매도정산합 | N |
| buy_amt_sum | 매수정산합 | N |
| seq | 일련번호 | N |
| stk_cd | 종목번호 | N |
| loan_dt | 대출일 | N |
| qty | 수량 | N |
| engg_amt | 약정금액 | N |
| cmsn | 수수료 | N |
| incm_tax | 소득세 | N |
| rstx | 농특세 | N |
| stk_nm | 종목명 | N |
| sell_tp | 매도수구분 | N |
| unp | 단가 | N |
| exct_amt | 정산금액 | N |
| trde_tax | 거래세 | N |
| resi_tax | 주민세 | N |
| crd_tp | 신용구분 | N |

#### Request Example

```json
{"strt_dcd_seq": ""}
```

---

### 계좌별주문체결현황요청 (kt00009)

#### 기본 정보

- **API ID**: `kt00009`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| ord_dt | 주문일자 | N |
| stk_bond_tp | 주식채권구분 | Y |
| mrkt_tp | 시장구분 | Y |
| sell_tp | 매도수구분 | Y |
| qry_tp | 조회구분 | Y |
| stk_cd | 종목코드 | N |
| fr_ord_no | 시작주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | Y |
| sell_grntl_engg_amt | 매도약정금액 | N |
| buy_engg_amt | 매수약정금액 | N |
| engg_amt | 약정금액 | N |
| stk_bond_tp | 주식채권구분 | N |
| ord_no | 주문번호 | N |
| stk_cd | 종목번호 | N |
| trde_tp | 매매구분 | N |
| io_tp_nm | 주문유형구분 | N |
| ord_qty | 주문수량 | N |
| ord_uv | 주문단가 | N |
| cnfm_qty | 확인수량 | N |
| rsrv_oppo | 예약/반대 | N |
| cntr_no | 체결번호 | N |
| acpt_tp | 접수구분 | N |
| orig_ord_no | 원주문번호 | N |
| stk_nm | 종목명 | N |
| setl_tp | 결제구분 | N |
| crd_deal_tp | 신용거래구분 | N |
| cntr_qty | 체결수량 | N |
| cntr_uv | 체결단가 | N |
| comm_ord_tp | 통신구분 | N |
| mdfy_cncl_tp | 정정/취소구분 | N |
| cntr_tm | 체결시간 | N |
| dmst_stex_tp | 국내거래소구분 | N |
| cond_uv | 스톱가 | N |

#### Request Example

```json
{"ord_dt": "","stk_bond_tp": "0","mrkt_tp": "0","sell_tp": "0","qry_tp": "0","stk_cd": "","fr_ord_no": "","dmst_stex_tp": "KRX"}
```

---

### 주문인출가능금액요청 (kt00010)

#### 기본 정보

- **API ID**: `kt00010`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| io_amt | 입출금액 | N |
| stk_cd | 종목번호 | Y |
| trde_tp | 매매구분 | Y |
| trde_qty | 매매수량 | N |
| uv | 매수가격 | Y |
| exp_buy_unp | 예상매수단가 | N |
| pred_reu_alowa | 전일재사용가능금액 | N |
| tdy_reu_alowa | 금일재사용가능금액 | N |
| entr | 예수금 | N |
| repl_amt | 대용금 | N |
| uncla | 미수금 | N |
| ord_pos_repl | 주문가능대용 | N |
| ord_alowa | 주문가능현금 | N |
| wthd_alowa | 인출가능금액 | N |
| nxdy_wthd_alowa | 익일인출가능금액 | N |
| pur_amt | 매입금액 | N |
| cmsn | 수수료 | N |
| pur_exct_amt | 매입정산금 | N |
| d2entra | D2추정예수금 | N |
| profa_rdex_aplc_tp | 증거금감면적용구분 | N |

#### Request Example

```json
{"io_amt": "","stk_cd": "005930","trde_tp": "2","trde_qty": "","uv": "267000","exp_buy_unp": ""}
```

---

### 증거금율별주문가능수량조회요청 (kt00011)

#### 기본 정보

- **API ID**: `kt00011`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목번호 | Y |
| uv | 매수가격 | N |
| stk_profa_rt | 종목증거금율 | N |
| profa_rt | 계좌증거금율 | N |
| aplc_rt | 적용증거금율 | N |
| entr | 예수금 | N |
| repl_amt | 대용금 | N |
| uncla | 미수금 | N |
| ord_pos_repl | 주문가능대용 | N |
| ord_alowa | 주문가능현금 | N |

#### Request Example

```json
{"stk_cd": "005930","uv": ""}
```

---

### 신용보증금율별주문가능수량조회요청 (kt00012)

#### 기본 정보

- **API ID**: `kt00012`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목번호 | Y |
| uv | 매수가격 | N |
| stk_assr_rt | 종목보증금율 | N |
| stk_assr_rt_nm | 종목보증금율명 | N |
| assr_40ord_alowq | 보증금40%주문가능 | N |
| entr | 예수금 | N |
| repl_amt | 대용금 | N |
| uncla | 미수금 | N |
| ord_pos_repl | 주문가능대용 | N |
| ord_alowa | 주문가능현금 | N |
| out_alowa | 미수가능금액 | N |
| out_pos_qty | 미수가능수량 | N |
| min_amt | 미수불가금액 | N |
| min_qty | 미수불가수량 | N |

#### Request Example

```json
{"stk_cd": "005930","uv": ""}
```

---

### 증거금세부내역조회요청 (kt00013)

#### 기본 정보

- **API ID**: `kt00013`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| tdy_reu_objt_amt | 금일재사용대상금액 | N |
| tdy_reu_use_amt | 금일재사용사용금액 | N |
| tdy_reu_alowa | 금일재사용가능금액 | N |
| tdy_reu_lmtt_amt | 금일재사용제한금액 | N |
| pred_reu_objt_amt | 전일재사용대상금액 | N |
| pred_reu_use_amt | 전일재사용사용금액 | N |
| pred_reu_alowa | 전일재사용가능금액 | N |
| pred_reu_lmtt_amt | 전일재사용제한금액 | N |
| ch_amt | 현금금액 | N |
| ch_profa | 현금증거금 | N |
| use_pos_ch | 사용가능현금 | N |
| ch_use_lmtt_amt | 현금사용제한금액 | N |
| use_pos_ch_fin | 사용가능현금최종 | N |
| repl_amt_amt | 대용금액 | N |
| repl_profa | 대용증거금 | N |
| use_pos_repl | 사용가능대용 | N |
| repl_use_lmtt_amt | 대용사용제한금액 | N |
| use_pos_repl_fin | 사용가능대용최종 | N |
| crd_grnta_ch | 신용보증금현금 | N |
| crd_grnta_repl | 신용보증금대용 | N |
| crd_grnt_ch | 신용담보금현금 | N |
| crd_grnt_repl | 신용담보금대용 | N |
| uncla | 미수금 | N |
| ls_grnt_reu_gold | 대주담보금재사용금 | N |
| 20ord_alow_amt | 20%주문가능금액 | N |
| 30ord_alow_amt | 30%주문가능금액 | N |
| 40ord_alow_amt | 40%주문가능금액 | N |
| 50ord_alow_amt | 50%주문가능금액 | N |
| 60ord_alow_amt | 60%주문가능금액 | N |
| 100ord_alow_amt | 100%주문가능금액 | N |
| evlt_repl_rt | 평가대용비율 | N |
| crd_repl_profa | 신용대용증거금 | N |
| ch_ord_repl_profa | 현금주문대용증거금 | N |
| crd_ord_repl_profa | 신용주문대용증거금 | N |
| crd_repl_conv_gold | 신용대용환산금 | N |
| ch_repl_lck_gold | 현금대용부족금 | N |
| crd_repl_lck_gold | 신용대용부족금 | N |
| ch_ord_alow_repla | 현금주문가능대용금 | N |
| crd_ord_alow_repla | 신용주문가능대용금 | N |
| d2vexct_entr | D2가정산예수금 | N |
| d2ch_ord_alow_amt | D2현금주문가능금액 | N |

#### Request Example

```json
{}```Response Example```### {"tdy_reu_objt_amt": "000000000000000","tdy_reu_use_amt": "000000000000000","tdy_reu_alowa": "000000000000000","tdy_reu_lmtt_amt": "000000000000000","tdy_reu_alowa_fin": "000000000000000","pred_reu_objt_amt": "000000000048141","pred_reu_use_amt": "000000000020947","pred_reu_alowa": "000000000027194","pred_reu_lmtt_amt": "000000000000000","pred_reu_alowa_fin": "000000000027194","ch_amt": "000000000017534","ch_profa": "000000000032193","use_pos_ch": "000000000085341","ch_use_lmtt_amt": "000000000000000","use_pos_ch_fin": "000000000085341","repl_amt_amt": "000000003915500","repl_profa": "000000000000000","use_pos_repl": "000000003915500","repl_use_lmtt_amt": "000000000000000","use_pos_repl_fin": "000000003915500","crd_grnta_ch": "000000000000000","crd_grnta_repl": "000000000000000","crd_grnt_ch": "000000000000000","crd_grnt_repl": "000000000000000","uncla": "000000000000","ls_grnt_reu_gold": "000000000000000","20ord_alow_amt": "000000000012550","30ord_alow_amt": "000000000012550","40ord_alow_amt": "000000000012550","50ord_alow_amt": "000000000012550","60ord_alow_amt": "000000000012550","100ord_alow_amt": "000000000012550","tdy_crd_rpya_loss_amt": "000000000000000","pred_crd_rpya_loss_amt": "000000000000000","tdy_ls_rpya_loss_repl_profa": "000000000000000","pred_ls_rpya_loss_repl_profa": "000000000000000","evlt_repl_amt_spg_use_skip": "000000006193400","evlt_repl_rt": "0.6322053","crd_repl_profa": "000000000000000","ch_ord_repl_profa": "000000000000000","crd_ord_repl_profa": "000000000000000","crd_repl_conv_gold": "000000000000000","repl_alowa": "000000003915500","repl_alowa_2": "000000003915500","ch_repl_lck_gold": "000000000000000","crd_repl_lck_gold": "000000000000000","ch_ord_alow_repla": "000000003915500","crd_ord_alow_repla": "000000006193400","d2vexct_entr": "000000000012550","d2ch_ord_alow_amt": "000000000012550","return_code": 0,"return_msg": "조회가 완료되었습니다."}
```

---

### 위탁종합거래내역요청 (kt00015)

#### 기본 정보

- **API ID**: `kt00015`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| tp | 구분 | Y |
| stk_cd | 종목코드 | N |
| crnc_cd | 통화코드 | N |
| gds_tp | 상품구분 | Y |
| frgn_stex_code | 해외거래소코드 | N |
| dmst_stex_tp | 국내거래소구분 | Y |
| trde_dt | 거래일자 | N |
| trde_no | 거래번호 | N |
| rmrk_nm | 적요명 | N |
| crd_deal_tp_nm | 신용거래구분명 | N |
| exct_amt | 정산금액 | N |
| loan_amt_rpya | 대출금상환 | N |
| fc_trde_amt | 거래금액(외) | N |
| fc_exct_amt | 정산금액(외) | N |
| entra_remn | 예수금잔고 | N |
| crnc_cd | 통화코드 | N |
| trde_ocr_tp | 거래종류구분 | N |
| trde_kind_nm | 거래종류명 | N |
| stk_nm | 종목명 | N |
| trde_amt | 거래금액 | N |
| trde_agri_tax | 거래및농특세 | N |
| rpy_diffa | 상환차금 | N |
| fc_trde_tax | 거래세(외) | N |
| dly_sum | 연체합 | N |
| fc_entra | 외화예수금잔고 | N |
| mdia_tp_nm | 매체구분명 | N |
| io_tp | 입출구분 | N |
| io_tp_nm | 입출구분명 | N |
| orig_deal_no | 원거래번호 | N |
| stk_cd | 종목코드 | N |
| trde_qty_jwa_cnt | 거래수량/좌수 | N |
| cmsn | 수수료 | N |
| int_ls_usfe | 이자/대주이용 | N |
| fc_cmsn | 수수료(외) | N |
| fc_dly_sum | 연체합(외) | N |
| vlbl_nowrm | 유가금잔 | N |
| proc_tm | 처리시간 | N |
| isin_cd | ISIN코드 | N |
| stex_cd | 거래소코드 | N |
| stex_nm | 거래소명 | N |
| trde_unit | 거래단가/환율 | N |
| incm_resi_tax | 소득/주민세 | N |
| loan_dt | 대출일 | N |
| uncl_ocr | 미수(원/주) | N |
| rpym_sum | 변제합 | N |
| cntr_dt | 체결일 | N |
| rcpy_no | 출납번호 | N |
| prcsr | 처리자 | N |
| proc_brch | 처리점 | N |
| trde_stle | 매매형태 | N |
| txon_base_pric | 과세기준가 | N |
| tax_sum_cmsn | 세금수수료합 | N |
| frgn_pay_txam | 외국납부세액(외) | N |
| fc_uncl_ocr | 미수(외) | N |
| rpym_sum_fr | 변제합(외) | N |
| rcpmnyer | 입금자 | N |
| trde_prtc_tp | 거래내역구분 | N |

#### Request Example

```json
{"strt_dt": "20241121","end_dt": "20241125","tp": "0","stk_cd": "","crnc_cd": "","gds_tp": "0","frgn_stex_code": "","dmst_stex_tp": "%"}
```

---

### 일별계좌수익률상세현황요청 (kt00016)

#### 기본 정보

- **API ID**: `kt00016`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| fr_dt | 평가시작일 | Y |
| to_dt | 평가종료일 | Y |
| mang_empno | 관리사원번호 | N |
| mngr_nm | 관리자명 | N |
| dept_nm | 관리자지점 | N |
| entr_fr | 예수금_초 | N |
| entr_to | 예수금_말 | N |
| scrt_evlt_amt_fr | 유가증권평가금액_초 | N |
| scrt_evlt_amt_to | 유가증권평가금액_말 | N |
| ls_grnt_fr | 대주담보금_초 | N |
| ls_grnt_to | 대주담보금_말 | N |
| crd_loan_fr | 신용융자금_초 | N |
| crd_loan_to | 신용융자금_말 | N |
| ch_uncla_fr | 현금미수금_초 | N |
| ch_uncla_to | 현금미수금_말 | N |
| krw_asgna_fr | 원화대용금_초 | N |
| krw_asgna_to | 원화대용금_말 | N |
| ls_evlta_fr | 대주평가금_초 | N |
| ls_evlta_to | 대주평가금_말 | N |
| rght_evlta_fr | 권리평가금_초 | N |
| rght_evlta_to | 권리평가금_말 | N |
| loan_amt_fr | 대출금_초 | N |
| loan_amt_to | 대출금_말 | N |
| etc_loana_fr | 기타대여금_초 | N |
| etc_loana_to | 기타대여금_말 | N |
| crd_int_npay_gold_fr | 신용이자미납금_초 | N |
| crd_int_npay_gold_to | 신용이자미납금_말 | N |
| crd_int_fr | 신용이자_초 | N |
| crd_int_to | 신용이자_말 | N |
| tot_amt_fr | 순자산액계_초 | N |
| tot_amt_to | 순자산액계_말 | N |
| invt_bsamt | 투자원금평잔 | N |
| evltv_prft | 평가손익 | N |
| prft_rt | 수익률 | N |
| tern_rt | 회전율 | N |
| termin_tot_trns | 기간내총입금 | N |
| termin_tot_pymn | 기간내총출금 | N |
| termin_tot_inq | 기간내총입고 | N |
| termin_tot_outq | 기간내총출고 | N |
| futr_repl_sella | 선물대용매도금액 | N |
| trst_repl_sella | 위탁대용매도금액 | N |

#### Request Example

```json
{"fr_dt": "20241111","to_dt": "20241125"}
```

---

### 계좌별당일현황요청 (kt00017)

#### 기본 정보

- **API ID**: `kt00017`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| d2_entra | D+2추정예수금 | N |
| crd_int_npay_gold | 신용이자미납금 | N |
| etc_loana | 기타대여금 | N |
| crd_loan_d2 | 신용융자금D+2 | N |
| crd_loan_evlta_d2 | 신용융자평가금D+2 | N |
| crd_ls_grnt_d2 | 신용대주담보금D+2 | N |
| crd_ls_evlta_d2 | 신용대주평가금D+2 | N |
| ina_amt | 입금금액 | N |
| outa | 출금금액 | N |
| inq_amt | 입고금액 | N |
| outq_amt | 출고금액 | N |
| sell_amt | 매도금액 | N |
| buy_amt | 매수금액 | N |
| cmsn | 수수료 | N |
| tax | 세금 | N |
| rp_evlt_amt | RP평가금액 | N |
| bd_evlt_amt | 채권평가금액 | N |
| elsevlt_amt | ELS평가금액 | N |
| crd_int_amt | 신용이자금액 | N |
| dvida_amt | 배당금액 | N |

#### Request Example

```json
{}```Response Example```### {"d2_entra": "000000012550","crd_int_npay_gold": "000000000000","etc_loana": "000000000000","gnrl_stk_evlt_amt_d2": "000005724100","dpst_grnt_use_amt_d2": "000000000000","crd_stk_evlt_amt_d2": "000000000000","crd_loan_d2": "000000000000","crd_loan_evlta_d2": "000000000000","crd_ls_grnt_d2": "000000000000","crd_ls_evlta_d2": "000000000000","ina_amt": "000000000000","outa": "000000000000","inq_amt": "000000000000","outq_amt": "000000000000","sell_amt": "000000000000","buy_amt": "000000000000","cmsn": "000000000000","tax": "000000000000","stk_pur_cptal_loan_amt": "000000000000","rp_evlt_amt": "000000000000","bd_evlt_amt": "000000000000","elsevlt_amt": "000000000000","crd_int_amt": "000000000000","sel_prica_grnt_loan_int_amt_amt": "000000000000","dvida_amt": "000000000000","return_code": 0,"return_msg": "조회가 완료되었습니다.."}
```

---

### 계좌평가잔고내역요청 (kt00018)

#### 기본 정보

- **API ID**: `kt00018`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| qry_tp | 조회구분 | Y |
| dmst_stex_tp | 국내거래소구분 | Y |
| tot_pur_amt | 총매입금액 | N |
| tot_evlt_amt | 총평가금액 | N |
| tot_evlt_pl | 총평가손익금액 | N |
| tot_prft_rt | 총수익률(%) | N |
| prsm_dpst_aset_amt | 추정예탁자산 | N |
| tot_loan_amt | 총대출금 | N |
| tot_crd_loan_amt | 총융자금액 | N |
| tot_crd_ls_amt | 총대주금액 | N |
| stk_cd | 종목번호 | N |
| stk_nm | 종목명 | N |
| evltv_prft | 평가손익 | N |
| prft_rt | 수익률(%) | N |
| pur_pric | 매입가 | N |
| pred_close_pric | 전일종가 | N |
| rmnd_qty | 보유수량 | N |
| trde_able_qty | 매매가능수량 | N |
| cur_prc | 현재가 | N |
| pred_buyq | 전일매수수량 | N |
| pred_sellq | 전일매도수량 | N |
| tdy_buyq | 금일매수수량 | N |
| tdy_sellq | 금일매도수량 | N |
| pur_amt | 매입금액 | N |
| pur_cmsn | 매입수수료 | N |
| evlt_amt | 평가금액 | N |
| sell_cmsn | 평가수수료 | N |
| tax | 세금 | N |
| sum_cmsn | 수수료합 | N |
| poss_rt | 보유비중(%) | N |
| crd_tp | 신용구분 | N |
| crd_tp_nm | 신용구분명 | N |
| crd_loan_dt | 대출일 | N |

#### Request Example

```json
{"qry_tp": "1","dmst_stex_tp": "KRX"}
```

---

### 금현물 잔고확인 (kt50020)

#### 기본 정보

- **API ID**: `kt50020`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| tot_entr | 예수금 | N |
| net_entr | 추정예수금 | N |
| tot_est_amt | 잔고평가액 | N |
| net_amt | 예탁자산평가액 | N |
| tot_book_amt2 | 총매입금액 | N |
| tot_dep_amt | 추정예탁자산 | N |
| paym_alowa | 출금가능금액 | N |
| pl_amt | 실현손익 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| real_qty | 보유수량 | N |
| avg_prc | 평균단가 | N |
| cur_prc | 현재가 | N |
| est_amt | 평가금액 | N |
| est_lspft | 손익금액 | N |
| est_ratio | 손익율 | N |
| cmsn | 수수료 | N |
| vlad_tax | 부가가치세 | N |
| book_amt2 | 매입금액 | N |
| pl_prch_prc | 손익분기매입가 | N |
| qty | 결제잔고 | N |
| buy_qty | 매수수량 | N |
| sell_qty | 매도수량 | N |
| able_qty | 가능수량 | N |

#### Request Example

```json
{}```Response Example```### {"tot_entr": "000098740486","net_entr": "000098740486","tot_est_amt": "000001207273","net_amt": "000099955866","tot_book_amt2": "000001254780","tot_dep_amt": "000099951884","paym_alowa": "000098740486","pl_amt": "000000000000","gold_acnt_evlt_prst": [{"stk_cd": "M04020000","stk_nm": "금 99.99_1Kg","real_qty": "000000000002","avg_prc": "000000152385","cur_prc": "000000151780","est_amt": "000000301569","est_lspft": "-00000003201","est_ratio": "-1.0503","cmsn": "000000001810","vlad_tax": "000000000181","book_amt2": "000000304770","pl_prch_prc": "153380.50","qty": "000000000002","buy_qty": "000000000000","sell_qty": "000000000000","able_qty": "000000000002"}
```

---

### 금현물 예수금 (kt50021)

#### 기본 정보

- **API ID**: `kt50021`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| entra | 예수금 | N |
| profa_ch | 증거금현금 | N |
| chck_ina_amt | 수표입금액 | N |
| etc_loan | 기타대여금 | N |
| etc_loan_dlfe | 기타대여금연체료 | N |
| etc_loan_tot | 기타대여금합계 | N |
| prsm_entra | 추정예수금 | N |
| buy_exct_amt | 매수정산금 | N |
| sell_exct_amt | 매도정산금 | N |
| sell_buy_exct_amt | 매도매수정산금 | N |
| dly_amt | 미수변제소요금 | N |
| pymn_alow_amt | 출금가능금액 | N |
| ord_alow_amt | 주문가능금액 | N |

#### Request Example

```json
{}```Response Example```### {"entra": "000000098740486","profa_ch": "000000000000000","chck_ina_amt": "000000000000000","etc_loan": "000000000000000","etc_loan_dlfe": "000000000000000","etc_loan_tot": "000000000000000","prsm_entra": "000000098740486","buy_exct_amt": "000000000000000","sell_exct_amt": "000000000000000","sell_buy_exct_amt": "000000000000000","dly_amt": "000000000000000","prsm_pymn_alow_amt": "000000098740486","pymn_alow_amt": "000000098740486","ord_alow_amt": "000000098740486","return_code": 0,"return_msg": "조회가 완료되었습니다."}
```

---

### 금현물 주문체결전체조회 (kt50030)

#### 기본 정보

- **API ID**: `kt50030`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| ord_dt | 주문일자 | Y |
| qry_tp | 조회구분 | N |
| mrkt_deal_tp | 시장구분 | Y |
| stk_bond_tp | 주식채권구분 | Y |
| slby_tp | 매도수구분 | Y |
| stk_cd | 종목코드 | N |
| fr_ord_no | 시작주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | N |
| stk_bond_tp | 주식채권구분 | N |
| ord_no | 주문번호 | N |
| stk_cd | 상품코드 | N |
| trde_tp | 매매구분 | N |
| io_tp_nm | 주문유형구분 | N |
| ord_qty | 주문수량 | N |
| ord_uv | 주문단가 | N |
| cnfm_qty | 확인수량 | N |
| data_send_end_tp | 접수구분 | N |
| mrkt_deal_tp | 시장구분 | N |
| rsrv_tp | 예약/반대여부 | N |
| orig_ord_no | 원주문번호 | N |
| stk_nm | 종목명 | N |
| dcd_tp_nm | 결제구분 | N |
| crd_deal_tp | 신용거래구분 | N |
| cntr_qty | 체결수량 | N |
| cntr_uv | 체결단가 | N |
| ord_remnq | 미체결수량 | N |
| comm_ord_tp | 통신구분 | N |
| mdfy_cncl_tp | 정정취소구분 | N |
| dmst_stex_tp | 국내거래소구분 | N |
| cond_uv | 스톱가 | N |

#### Request Example

```json
{"ord_dt": "20250821","qry_tp": "1","mrkt_deal_tp": "1","stk_bond_tp": "0","slby_tp": "0","stk_cd": "M04020000","fr_ord_no": "","dmst_stex_tp": "KRX"}
```

---

### 금현물 주문체결조회 (kt50031)

#### 기본 정보

- **API ID**: `kt50031`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| ord_dt | 주문일자 | N |
| qry_tp | 조회구분 | Y |
| stk_bond_tp | 주식채권구분 | Y |
| sell_tp | 매도수구분 | Y |
| stk_cd | 종목코드 | N |
| fr_ord_no | 시작주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | Y |
| ord_no | 주문번호 | N |
| stk_cd | 종목번호 | N |
| trde_tp | 매매구분 | N |
| crd_tp | 신용구분 | N |
| ord_qty | 주문수량 | N |
| ord_uv | 주문단가 | N |
| cnfm_qty | 확인수량 | N |
| acpt_tp | 접수구분 | N |
| rsrv_tp | 반대여부 | N |
| ord_tm | 주문시간 | N |
| ori_ord | 원주문 | N |
| stk_nm | 종목명 | N |
| io_tp_nm | 주문구분 | N |
| loan_dt | 대출일 | N |
| cntr_qty | 체결수량 | N |
| cntr_uv | 체결단가 | N |
| ord_remnq | 주문잔량 | N |
| comm_ord_tp | 통신구분 | N |
| mdfy_cncl | 정정취소 | N |
| cnfm_tm | 확인시간 | N |
| dmst_stex_tp | 국내거래소구분 | N |
| cond_uv | 스톱가 | N |

#### Request Example

```json
{"ord_dt": "20250821","qry_tp": "1","stk_bond_tp": "0","sell_tp": "0","stk_cd": "M04020000","fr_ord_no": "","dmst_stex_tp": "%"}
```

---

### 금현물 거래내역조회 (kt50032)

#### 기본 정보

- **API ID**: `kt50032`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | N |
| end_dt | 종료일자 | N |
| tp | 구분 | N |
| stk_cd | 종목코드 | N |
| acnt_print | 계좌번호 | N |
| deal_dt | 거래일자 | N |
| deal_no | 거래번호 | N |
| rmrk_nm | 적요명 | N |
| deal_qty | 거래수량 | N |
| gold_spot_vat | 금현물부가가치세 | N |
| exct_amt | 정산금액 | N |
| dly_sum | 연체합 | N |
| entra_remn | 예수금잔고 | N |
| mdia_nm | 메체구분명 | N |
| orig_deal_no | 원거래번호 | N |
| stk_nm | 종목명 | N |
| uv_exrt | 거래단가 | N |
| cmsn | 수수료 | N |
| uncl_ocr | 미수(원/g) | N |
| rpym_sum | 변제합 | N |
| spot_remn | 현물잔고 | N |
| proc_time | 처리시간 | N |
| rcpy_no | 출납번호 | N |
| stk_cd | 종목코드 | N |
| deal_amt | 거래금액 | N |
| tax_tot_amt | 소득/주민세 | N |
| cntr_dt | 체결일 | N |
| proc_brch_nm | 처리점 | N |
| prcsr | 처리자 | N |

#### Request Example

```json
{"strt_dt": "20250819","end_dt": "20250820","tp": "0","stk_cd": ""}
```

---

### 금현물 미체결조회 (kt50075)

#### 기본 정보

- **API ID**: `kt50075`
- **Method**: `POST`
- **URL**: `/api/dostk/acnt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| ord_dt | 주문일자 | Y |
| qry_tp | 조회구분 | N |
| mrkt_deal_tp | 시장구분 | Y |
| stk_bond_tp | 주식채권구분 | Y |
| sell_tp | 매도수구분 | Y |
| stk_cd | 종목코드 | N |
| fr_ord_no | 시작주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | N |
| stk_bond_tp | 주식채권구분 | N |
| ord_no | 주문번호 | N |
| stk_cd | 상품코드 | N |
| trde_tp | 매매구분 | N |
| io_tp_nm | 주문유형구분 | N |
| ord_qty | 주문수량 | N |
| ord_uv | 주문단가 | N |
| cnfm_qty | 확인수량 | N |
| data_send_end_tp | 접수구분 | N |
| mrkt_deal_tp | 시장구분 | N |
| rsrv_tp | 예약/반대여부 | N |
| orig_ord_no | 원주문번호 | N |
| stk_nm | 종목명 | N |
| dcd_tp_nm | 결제구분 | N |
| crd_deal_tp | 신용거래구분 | N |
| cntr_qty | 체결수량 | N |
| cntr_uv | 체결단가 | N |
| ord_remnq | 미체결수량 | N |
| comm_ord_tp | 통신구분 | N |
| mdfy_cncl_tp | 정정취소구분 | N |
| dmst_stex_tp | 국내거래소구분 | N |
| cond_uv | 스톱가 | N |

#### Request Example

```json
{"ord_dt": "20250821","qry_tp": "1","mrkt_deal_tp": "1","stk_bond_tp": "0","sell_tp": "0","stk_cd": "M04020000","fr_ord_no": "","dmst_stex_tp": "KRX"}
```

---

## 공매도

### 공매도추이요청 (ka10014)

#### 기본 정보

- **API ID**: `ka10014`
- **Method**: `POST`
- **URL**: `/api/dostk/shsa`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tm_tp | 시간구분 | N |
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| dt | 일자 | N |
| close_pric | 종가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| shrts_qty | 공매도량 | N |
| ovr_shrts_qty | 누적공매도량 | N |
| trde_wght | 매매비중 | N |
| shrts_trde_prica | 공매도거래대금 | N |
| shrts_avg_pric | 공매도평균가 | N |

#### Request Example

```json
{"stk_cd": "005930","tm_tp": "1","strt_dt": "20250501","end_dt": "20250519"}
```

---

## 기관/외국인

### 주식외국인종목별매매동향 (ka10008)

#### 기본 정보

- **API ID**: `ka10008`
- **Method**: `POST`
- **URL**: `/api/dostk/frgnistt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| dt | 일자 | N |
| close_pric | 종가 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |
| chg_qty | 변동수량 | N |
| poss_stkcnt | 보유주식수 | N |
| wght | 비중 | N |
| gain_pos_stkcnt | 취득가능주식수 | N |
| frgnr_limit | 외국인한도 | N |
| frgnr_limit_irds | 외국인한도증감 | N |
| limit_exh_rt | 한도소진률 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 주식기관요청 (ka10009)

#### 기본 정보

- **API ID**: `ka10009`
- **Method**: `POST`
- **URL**: `/api/dostk/frgnistt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| date | 날짜 | N |
| close_pric | 종가 | N |
| pre | 대비 | N |
| orgn_dt_acc | 기관기간누적 | N |
| orgn_daly_nettrde | 기관일별순매매 | N |
| frgnr_daly_nettrde | 외국인일별순매매 | N |
| frgnr_qota_rt | 외국인지분율 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 기관외국인연속매매현황요청 (ka10131)

#### 기본 정보

- **API ID**: `ka10131`
- **Method**: `POST`
- **URL**: `/api/dostk/frgnistt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dt | 기간 | Y |
| strt_dt | 시작일자 | N |
| end_dt | 종료일자 | N |
| mrkt_tp | 장구분 | Y |
| netslmt_tp | 순매도수구분 | Y |
| stk_inds_tp | 종목업종구분 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| stex_tp | 거래소구분 | Y |
| rank | 순위 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| prid_stkpc_flu_rt | 기간중주가등락률 | N |
| orgn_nettrde_amt | 기관순매매금액 | N |
| orgn_nettrde_qty | 기관순매매량 | N |
| frgnr_nettrde_qty | 외국인순매매량 | N |
| frgnr_nettrde_amt | 외국인순매매액 | N |
| nettrde_qty | 순매매량 | N |
| nettrde_amt | 순매매액 | N |

#### Request Example

```json
{"dt": "1","strt_dt": "","end_dt": "","mrkt_tp": "001","netslmt_tp": "2","stk_inds_tp": "0","amt_qty_tp": "0","stex_tp": "1"}
```

---

### 금현물투자자현황 (ka52301)

#### 기본 정보

- **API ID**: `ka52301`
- **Method**: `POST`
- **URL**: `/api/dostk/frgnistt`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| buy_amt_irds | 투자자별 매수 금액 | N |
| sell_uv | 투자자별 매도 단가 | N |
| buy_uv | 투자자별 매수 단가 | N |
| stk_nm | 투자자 구분명 | N |
| acc_netprps_amt | 누적 순매수 금액(억) | N |
| acc_netprps_qty | 누적 순매수 수량(천) | N |
| stk_cd | 투자자 코드 | N |

#### Request Example

```json
{}```Response Example```### {"inve_trad_stat": [{"all_dfrt_trst_sell_qty": "14","sell_qty_irds": "7","all_dfrt_trst_sell_amt": "22","sell_amt_irds": "11","all_dfrt_trst_buy_qty": "6","buy_qty_irds": "1","all_dfrt_trst_buy_amt": "9","buy_amt_irds": "1","all_dfrt_trst_netprps_qty": "-8","netprps_qty_irds": "-6","all_dfrt_trst_netprps_amt": "-12","netprps_amt_irds": "-10","sell_uv": "307","buy_uv": "311","stk_nm": "개인","acc_netprps_amt": "-12","acc_netprps_qty": "-8","stk_cd": "T94008"}
```

---

## 대차거래

### 대차거래추이요청 (ka10068)

#### 기본 정보

- **API ID**: `ka10068`
- **Method**: `POST`
- **URL**: `/api/dostk/slb`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | N |
| end_dt | 종료일자 | N |
| all_tp | 전체구분 | Y |
| dt | 일자 | N |
| dbrt_trde_cntrcnt | 대차거래체결주수 | N |
| dbrt_trde_rpy | 대차거래상환주수 | N |
| rmnd | 잔고주수 | N |
| dbrt_trde_irds | 대차거래증감 | N |
| remn_amt | 잔고금액 | N |

#### Request Example

```json
{"strt_dt": "20250401",```Request Example```"end_dt": "20250430","all_tp": "1"}
```

---

### 대차거래상위10종목요청 (ka10069)

#### 기본 정보

- **API ID**: `ka10069`
- **Method**: `POST`
- **URL**: `/api/dostk/slb`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | N |
| mrkt_tp | 시장구분 | Y |
| dbrt_trde_rpy_sum | 대차거래상환주수합 | N |
| rmnd_sum | 잔고주수합 | N |
| remn_amt_sum | 잔고금액합 | N |
| rmnd_rt | 잔고주수비율 | N |
| remn_amt_rt | 잔고금액비율 | N |
| stk_nm | 종목명 | N |
| stk_cd | 종목코드 | N |
| dbrt_trde_cntrcnt | 대차거래체결주수 | N |
| dbrt_trde_rpy | 대차거래상환주수 | N |
| rmnd | 잔고주수 | N |
| remn_amt | 잔고금액 | N |

#### Request Example

```json
{"strt_dt": "20241110","end_dt": "20241125","mrkt_tp": "001"}
```

---

### 대차거래내역요청 (ka90012)

#### 기본 정보

- **API ID**: `ka90012`
- **Method**: `POST`
- **URL**: `/api/dostk/slb`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dt | 일자 | Y |
| mrkt_tp | 시장구분 | Y |
| stk_nm | 종목명 | N |
| stk_cd | 종목코드 | N |
| dbrt_trde_cntrcnt | 대차거래체결주수 | N |
| dbrt_trde_rpy | 대차거래상환주수 | N |
| rmnd | 잔고주수 | N |
| remn_amt | 잔고금액 | N |

#### Request Example

```json
{"dt": "20241101","mrkt_tp": "101"}
```

---

### 대차거래추이요청(종목별) (종목별)

#### 기본 정보

- **API ID**: `종목별`
- **Method**: `POST`
- **URL**: `/api/dostk/slb`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | N |
| end_dt | 종료일자 | N |
| all_tp | 전체구분 | N |
| stk_cd | 종목코드 | Y |
| dt | 일자 | N |
| dbrt_trde_cntrcnt | 대차거래체결주수 | N |
| dbrt_trde_rpy | 대차거래상환주수 | N |
| dbrt_trde_irds | 대차거래증감 | N |
| rmnd | 잔고주수 | N |
| remn_amt | 잔고금액 | N |

#### Request Example

```json
{"strt_dt": "20250401","end_dt": "20250430","all_tp": "0","stk_cd": "005930"}
```

---

## 순위정보

### 호가잔량상위요청 (ka10020)

#### 기본 정보

- **API ID**: `ka10020`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| sort_tp | 정렬구분 | Y |
| trde_qty_tp | 거래량구분 | Y |
| stk_cnd | 종목조건 | Y |
| crd_cnd | 신용조건 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |
| tot_sel_req | 총매도잔량 | N |
| tot_buy_req | 총매수잔량 | N |
| netprps_req | 순매수잔량 | N |
| buy_rt | 매수비율 | N |

#### Request Example

```json
{"mrkt_tp": "001","sort_tp": "1","trde_qty_tp": "0000","stk_cnd": "0","crd_cnd": "0","stex_tp": "1"}
```

---

### 호가잔량급증요청 (ka10021)

#### 기본 정보

- **API ID**: `ka10021`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| trde_tp | 매매구분 | Y |
| sort_tp | 정렬구분 | Y |
| tm_tp | 시간구분 | Y |
| trde_qty_tp | 거래량구분 | Y |
| stk_cnd | 종목조건 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| int | 기준률 | N |
| now | 현재 | N |
| sdnin_qty | 급증수량 | N |
| sdnin_rt | 급증률 | N |
| tot_buy_qty | 총매수량 | N |

#### Request Example

```json
{"mrkt_tp": "001","trde_tp": "1","sort_tp": "1","tm_tp": "30","trde_qty_tp": "1","stk_cnd": "0","stex_tp": "3"}
```

---

### 잔량율급증요청 (ka10022)

#### 기본 정보

- **API ID**: `ka10022`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| rt_tp | 비율구분 | Y |
| tm_tp | 시간구분 | Y |
| trde_qty_tp | 거래량구분 | Y |
| stk_cnd | 종목조건 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| int | 기준률 | N |
| now_rt | 현재비율 | N |
| sdnin_rt | 급증률 | N |
| tot_sel_req | 총매도잔량 | N |
| tot_buy_req | 총매수잔량 | N |

#### Request Example

```json
{"mrkt_tp": "001","rt_tp": "1","tm_tp": "1","trde_qty_tp": "5","stk_cnd": "0","stex_tp": "3"}
```

---

### 거래량급증요청 (ka10023)

#### 기본 정보

- **API ID**: `ka10023`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| sort_tp | 정렬구분 | Y |
| tm_tp | 시간구분 | Y |
| trde_qty_tp | 거래량구분 | Y |
| tm | 시간 | N |
| stk_cnd | 종목조건 | Y |
| pric_tp | 가격구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| prev_trde_qty | 이전거래량 | N |
| now_trde_qty | 현재거래량 | N |
| sdnin_qty | 급증량 | N |
| sdnin_rt | 급증률 | N |

#### Request Example

```json
{"mrkt_tp": "000","sort_tp": "1","tm_tp": "2","trde_qty_tp": "5","tm": "","stk_cnd": "0","pric_tp": "0","stex_tp": "3"}
```

---

### 전일대비등락률상위요청 (ka10027)

#### 기본 정보

- **API ID**: `ka10027`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| sort_tp | 정렬구분 | Y |
| trde_qty_cnd | 거래량조건 | Y |
| stk_cnd | 종목조건 | Y |
| crd_cnd | 신용조건 | Y |
| updown_incls | 상하한포함 | Y |
| pric_cnd | 가격조건 | Y |
| trde_prica_cnd | 거래대금조건 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cls | 종목분류 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| sel_req | 매도잔량 | N |
| buy_req | 매수잔량 | N |
| now_trde_qty | 현재거래량 | N |
| cntr_str | 체결강도 | N |
| cnt | 횟수 | N |

#### Request Example

```json
{"mrkt_tp": "000","sort_tp": "1","trde_qty_cnd": "0000","stk_cnd": "0","crd_cnd": "0","updown_incls": "1","pric_cnd": "0","trde_prica_cnd": "0","stex_tp": "3"}
```

---

### 예상체결등락률상위요청 (ka10029)

#### 기본 정보

- **API ID**: `ka10029`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| sort_tp | 정렬구분 | Y |
| trde_qty_cnd | 거래량조건 | Y |
| stk_cnd | 종목조건 | Y |
| crd_cnd | 신용조건 | Y |
| pric_cnd | 가격조건 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| exp_cntr_pric | 예상체결가 | N |
| base_pric | 기준가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| exp_cntr_qty | 예상체결량 | N |
| sel_req | 매도잔량 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| buy_req | 매수잔량 | N |

#### Request Example

```json
{"mrkt_tp": "000","sort_tp": "1","trde_qty_cnd": "0","stk_cnd": "0","crd_cnd": "0","pric_cnd": "0","stex_tp": "3"}
```

---

### 당일거래량상위요청 (ka10030)

#### 기본 정보

- **API ID**: `ka10030`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| sort_tp | 정렬구분 | Y |
| mang_stk_incls | 관리종목포함 | Y |
| crd_tp | 신용구분 | Y |
| trde_qty_tp | 거래량구분 | Y |
| pric_tp | 가격구분 | Y |
| trde_prica_tp | 거래대금구분 | Y |
| mrkt_open_tp | 장운영구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| pred_rt | 전일비 | N |
| trde_tern_rt | 거래회전율 | N |
| trde_amt | 거래금액 | N |
| opmr_trde_qty | 장중거래량 | N |
| opmr_pred_rt | 장중전일비 | N |
| opmr_trde_rt | 장중거래회전율 | N |
| opmr_trde_amt | 장중거래금액 | N |
| af_mkrt_trde_qty | 장후거래량 | N |
| af_mkrt_pred_rt | 장후전일비 | N |
| af_mkrt_trde_rt | 장후거래회전율 | N |
| af_mkrt_trde_amt | 장후거래금액 | N |
| bf_mkrt_trde_qty | 장전거래량 | N |
| bf_mkrt_pred_rt | 장전전일비 | N |
| bf_mkrt_trde_rt | 장전거래회전율 | N |
| bf_mkrt_trde_amt | 장전거래금액 | N |

#### Request Example

```json
{"mrkt_tp": "000","sort_tp": "1","mang_stk_incls": "0","crd_tp": "0","trde_qty_tp": "0","pric_tp": "0","trde_prica_tp": "0","mrkt_open_tp": "0","stex_tp": "3"}
```

---

### 전일거래량상위요청 (ka10031)

#### 기본 정보

- **API ID**: `ka10031`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| qry_tp | 조회구분 | Y |
| rank_strt | 순위시작 | Y |
| rank_end | 순위끝 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |

#### Request Example

```json
{"mrkt_tp": "101","qry_tp": "1","rank_strt": "0","rank_end": "10","stex_tp": "3"}
```

---

### 거래대금상위요청 (ka10032)

#### 기본 정보

- **API ID**: `ka10032`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| mang_stk_incls | 관리종목포함 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| now_rank | 현재순위 | N |
| pred_rank | 전일순위 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| now_trde_qty | 현재거래량 | N |
| pred_trde_qty | 전일거래량 | N |
| trde_prica | 거래대금 | N |

#### Request Example

```json
{"mrkt_tp": "001","mang_stk_incls": "1","stex_tp": "3"}
```

---

### 신용비율상위요청 (ka10033)

#### 기본 정보

- **API ID**: `ka10033`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| trde_qty_tp | 거래량구분 | Y |
| stk_cnd | 종목조건 | Y |
| updown_incls | 상하한포함 | Y |
| crd_cnd | 신용조건 | Y |
| stex_tp | 거래소구분 | Y |
| stk_infr | 종목정보 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| crd_rt | 신용비율 | N |
| sel_req | 매도잔량 | N |
| buy_req | 매수잔량 | N |
| now_trde_qty | 현재거래량 | N |

#### Request Example

```json
{"mrkt_tp": "000","trde_qty_tp": "0","stk_cnd": "0","updown_incls": "1","crd_cnd": "0","stex_tp": "3"}
```

---

### 외인기간별매매상위요청 (ka10034)

#### 기본 정보

- **API ID**: `ka10034`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| trde_tp | 매매구분 | Y |
| dt | 기간 | Y |
| stex_tp | 거래소구분 | Y |
| rank | 순위 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| trde_qty | 거래량 | N |
| netprps_qty | 순매수량 | N |
| gain_pos_stkcnt | 취득가능주식수 | N |

#### Request Example

```json
{"mrkt_tp": "001","trde_tp": "2","dt": "0","stex_tp": "1"}
```

---

### 외인연속순매매상위요청 (ka10035)

#### 기본 정보

- **API ID**: `ka10035`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| trde_tp | 매매구분 | Y |
| base_dt_tp | 기준일구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| dm1 | D-1 | N |
| dm2 | D-2 | N |
| dm3 | D-3 | N |
| tot | 합계 | N |
| limit_exh_rt | 한도소진율 | N |
| pred_pre_1 | 전일대비1 | N |
| pred_pre_2 | 전일대비2 | N |
| pred_pre_3 | 전일대비3 | N |

#### Request Example

```json
{"mrkt_tp": "000","trde_tp": "2","base_dt_tp": "1","stex_tp": "1"}
```

---

### 외인한도소진율증가상위 (ka10036)

#### 기본 정보

- **API ID**: `ka10036`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| dt | 기간 | Y |
| stex_tp | 거래소구분 | Y |
| rank | 순위 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |
| poss_stkcnt | 보유주식수 | N |
| gain_pos_stkcnt | 취득가능주식수 | N |
| base_limit_exh_rt | 기준한도소진율 | N |
| limit_exh_rt | 한도소진율 | N |
| exh_rt_incrs | 소진율증가 | N |

#### Request Example

```json
{"mrkt_tp": "000","dt": "1","stex_tp": "1"}
```

---

### 외국계창구매매상위요청 (ka10037)

#### 기본 정보

- **API ID**: `ka10037`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| dt | 기간 | Y |
| trde_tp | 매매구분 | Y |
| sort_tp | 정렬구분 | Y |
| stex_tp | 거래소구분 | Y |
| rank | 순위 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| sel_trde_qty | 매도거래량 | N |
| buy_trde_qty | 매수거래량 | N |
| netprps_trde_qty | 순매수거래량 | N |
| netprps_prica | 순매수대금 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |

#### Request Example

```json
{"mrkt_tp": "000","dt": "0","trde_tp": "1","sort_tp": "2","stex_tp": "1"}
```

---

### 종목별증권사순위요청 (ka10038)

#### 기본 정보

- **API ID**: `ka10038`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| qry_tp | 조회구분 | Y |
| dt | 기간 | Y |
| rank_1 | 순위1 | N |
| rank_2 | 순위2 | N |
| rank_3 | 순위3 | N |
| prid_trde_qty | 기간중거래량 | N |
| rank | 순위 | N |
| mmcm_nm | 회원사명 | N |
| buy_qty | 매수수량 | N |
| sell_qty | 매도수량 | N |
| acc_netprps_qty | 누적순매수수량 | N |

#### Request Example

```json
{"stk_cd": "005930","strt_dt": "20241106","end_dt": "20241107","qry_tp": "2","dt": "1"}
```

---

### 증권사별매매상위요청 (ka10039)

#### 기본 정보

- **API ID**: `ka10039`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mmcm_cd | 회원사코드 | Y |
| trde_qty_tp | 거래량구분 | Y |
| trde_tp | 매매구분 | Y |
| dt | 기간 | Y |
| stex_tp | 거래소구분 | Y |
| rank | 순위 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| prid_stkpc_flu | 기간중주가등락 | N |
| flu_rt | 등락율 | N |
| prid_trde_qty | 기간중거래량 | N |
| netprps | 순매수 | N |
| buy_trde_qty | 매수거래량 | N |
| sel_trde_qty | 매도거래량 | N |
| netprps_amt | 순매수금액 | N |
| buy_amt | 매수금액 | N |
| sell_amt | 매도금액 | N |

#### Request Example

```json
{"mmcm_cd": "001","trde_qty_tp": "0","trde_tp": "1","dt": "1","stex_tp": "3"}
```

---

### 당일주요거래원요청 (ka10040)

#### 기본 정보

- **API ID**: `ka10040`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| sel_trde_ori_irds_1 | 매도거래원별증감1 | N |
| sel_trde_ori_qty_1 | 매도거래원수량1 | N |
| sel_trde_ori_1 | 매도거래원1 | N |
| sel_trde_ori_cd_1 | 매도거래원코드1 | N |
| buy_trde_ori_1 | 매수거래원1 | N |
| buy_trde_ori_cd_1 | 매수거래원코드1 | N |
| buy_trde_ori_qty_1 | 매수거래원수량1 | N |
| buy_trde_ori_irds_1 | 매수거래원별증감1 | N |
| sel_trde_ori_irds_2 | 매도거래원별증감2 | N |
| sel_trde_ori_qty_2 | 매도거래원수량2 | N |
| sel_trde_ori_2 | 매도거래원2 | N |
| sel_trde_ori_cd_2 | 매도거래원코드2 | N |
| buy_trde_ori_2 | 매수거래원2 | N |
| buy_trde_ori_cd_2 | 매수거래원코드2 | N |
| buy_trde_ori_qty_2 | 매수거래원수량2 | N |
| buy_trde_ori_irds_2 | 매수거래원별증감2 | N |
| sel_trde_ori_irds_3 | 매도거래원별증감3 | N |
| sel_trde_ori_qty_3 | 매도거래원수량3 | N |
| sel_trde_ori_3 | 매도거래원3 | N |
| sel_trde_ori_cd_3 | 매도거래원코드3 | N |
| buy_trde_ori_3 | 매수거래원3 | N |
| buy_trde_ori_cd_3 | 매수거래원코드3 | N |
| buy_trde_ori_qty_3 | 매수거래원수량3 | N |
| buy_trde_ori_irds_3 | 매수거래원별증감3 | N |
| sel_trde_ori_irds_4 | 매도거래원별증감4 | N |
| sel_trde_ori_qty_4 | 매도거래원수량4 | N |
| sel_trde_ori_4 | 매도거래원4 | N |
| sel_trde_ori_cd_4 | 매도거래원코드4 | N |
| buy_trde_ori_4 | 매수거래원4 | N |
| buy_trde_ori_cd_4 | 매수거래원코드4 | N |
| buy_trde_ori_qty_4 | 매수거래원수량4 | N |
| buy_trde_ori_irds_4 | 매수거래원별증감4 | N |
| sel_trde_ori_irds_5 | 매도거래원별증감5 | N |
| sel_trde_ori_qty_5 | 매도거래원수량5 | N |
| sel_trde_ori_5 | 매도거래원5 | N |
| sel_trde_ori_cd_5 | 매도거래원코드5 | N |
| buy_trde_ori_5 | 매수거래원5 | N |
| buy_trde_ori_cd_5 | 매수거래원코드5 | N |
| buy_trde_ori_qty_5 | 매수거래원수량5 | N |
| buy_trde_ori_irds_5 | 매수거래원별증감5 | N |
| frgn_sel_prsm_sum | 외국계매도추정합 | N |
| frgn_buy_prsm_sum | 외국계매수추정합 | N |
| sel_scesn_tm | 매도이탈시간 | N |
| sell_qty | 매도수량 | N |
| buy_scesn_tm | 매수이탈시간 | N |
| buy_qty | 매수수량 | N |
| qry_dt | 조회일자 | N |
| qry_tm | 조회시간 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 순매수거래원순위요청 (ka10042)

#### 기본 정보

- **API ID**: `ka10042`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| strt_dt | 시작일자 | N |
| end_dt | 종료일자 | N |
| qry_dt_tp | 조회기간구분 | Y |
| pot_tp | 시점구분 | Y |
| dt | 기간 | N |
| sort_base | 정렬기준 | Y |
| rank | 순위 | N |
| mmcm_cd | 회원사코드 | N |
| mmcm_nm | 회원사명 | N |

#### Request Example

```json
{"stk_cd": "005930","strt_dt": "20241031","end_dt": "20241107","qry_dt_tp": "0","pot_tp": "0","dt": "5","sort_base": "1"}
```

---

### 당일상위이탈원요청 (ka10053)

#### 기본 정보

- **API ID**: `ka10053`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| sel_scesn_tm | 매도이탈시간 | N |
| sell_qty | 매도수량 | N |
| buy_scesn_tm | 매수이탈시간 | N |
| buy_qty | 매수수량 | N |
| qry_dt | 조회일자 | N |
| qry_tm | 조회시간 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 동일순매매순위요청 (ka10062)

#### 기본 정보

- **API ID**: `ka10062`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | N |
| mrkt_tp | 시장구분 | Y |
| trde_tp | 매매구분 | Y |
| sort_cnd | 정렬조건 | Y |
| unit_tp | 단위구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| rank | 순위 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| acc_trde_qty | 누적거래량 | N |
| orgn_nettrde_qty | 기관순매매수량 | N |
| orgn_nettrde_amt | 기관순매매금액 | N |
| for_nettrde_qty | 외인순매매수량 | N |
| for_nettrde_amt | 외인순매매금액 | N |
| nettrde_qty | 순매매수량 | N |
| nettrde_amt | 순매매금액 | N |

#### Request Example

```json
{"strt_dt": "20241106","end_dt": "20241107","mrkt_tp": "000","trde_tp": "1","sort_cnd": "1","unit_tp": "1","stex_tp": "3"}
```

---

### 장중투자자별매매상위요청 (ka10065)

#### 기본 정보

- **API ID**: `ka10065`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trde_tp | 매매구분 | Y |
| mrkt_tp | 시장구분 | Y |
| orgn_tp | 기관구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| sel_qty | 매도량 | N |
| buy_qty | 매수량 | N |
| netslmt | 순매도 | N |

#### Request Example

```json
{```Request Example```"trde_tp": "1","mrkt_tp": "000","orgn_tp": "9000"}
```

---

### 시간외단일가등락율순위요청 (ka10098)

#### 기본 정보

- **API ID**: `ka10098`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| sort_base | 정렬기준 | Y |
| stk_cnd | 종목조건 | Y |
| trde_qty_cnd | 거래량조건 | Y |
| crd_cnd | 신용조건 | Y |
| trde_prica | 거래대금 | Y |
| rank | 순위 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| sel_tot_req | 매도총잔량 | N |
| buy_tot_req | 매수총잔량 | N |
| acc_trde_qty | 누적거래량 | N |
| acc_trde_prica | 누적거래대금 | N |
| tdy_close_pric | 당일종가 | N |

#### Request Example

```json
{"mrkt_tp": "000","sort_base": "5","stk_cnd": "0","trde_qty_cnd": "0","crd_cnd": "0","trde_prica": "0"}
```

---

### 외국인기관매매상위요청 (ka90009)

#### 기본 정보

- **API ID**: `ka90009`
- **Method**: `POST`
- **URL**: `/api/dostk/rkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| qry_dt_tp | 조회일자구분 | Y |
| date | 날짜 | N |
| stex_tp | 거래소구분 | Y |
| for_netslmt_stk_cd | 외인순매도종목코드 | N |
| for_netslmt_stk_nm | 외인순매도종목명 | N |
| for_netslmt_amt | 외인순매도금액 | N |
| for_netslmt_qty | 외인순매도수량 | N |
| for_netprps_stk_cd | 외인순매수종목코드 | N |
| for_netprps_stk_nm | 외인순매수종목명 | N |
| for_netprps_amt | 외인순매수금액 | N |
| for_netprps_qty | 외인순매수수량 | N |
| orgn_netslmt_amt | 기관순매도금액 | N |
| orgn_netslmt_qty | 기관순매도수량 | N |
| orgn_netprps_amt | 기관순매수금액 | N |
| orgn_netprps_qty | 기관순매수수량 | N |

#### Request Example

```json
{"mrkt_tp": "000","amt_qty_tp": "1","qry_dt_tp": "1","date": "20241101","stex_tp": "1"}
```

---

## 시세

### 주식호가요청 (ka10004)

#### 기본 정보

- **API ID**: `ka10004`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| bid_req_base_tm | 호가잔량기준시간 | N |
| sel_10th_pre_req_pre | 매도10차선잔량대비 | N |
| sel_10th_pre_req | 매도10차선잔량 | N |
| sel_10th_pre_bid | 매도10차선호가 | N |
| sel_9th_pre_req_pre | 매도9차선잔량대비 | N |
| sel_9th_pre_req | 매도9차선잔량 | N |
| sel_9th_pre_bid | 매도9차선호가 | N |
| sel_8th_pre_req_pre | 매도8차선잔량대비 | N |
| sel_8th_pre_req | 매도8차선잔량 | N |
| sel_8th_pre_bid | 매도8차선호가 | N |
| sel_7th_pre_req_pre | 매도7차선잔량대비 | N |
| sel_7th_pre_req | 매도7차선잔량 | N |
| sel_7th_pre_bid | 매도7차선호가 | N |
| sel_6th_pre_req_pre | 매도6차선잔량대비 | N |
| sel_6th_pre_req | 매도6차선잔량 | N |
| sel_6th_pre_bid | 매도6차선호가 | N |
| sel_5th_pre_req_pre | 매도5차선잔량대비 | N |
| sel_5th_pre_req | 매도5차선잔량 | N |
| sel_5th_pre_bid | 매도5차선호가 | N |
| sel_4th_pre_req_pre | 매도4차선잔량대비 | N |
| sel_4th_pre_req | 매도4차선잔량 | N |
| sel_4th_pre_bid | 매도4차선호가 | N |
| sel_3th_pre_req_pre | 매도3차선잔량대비 | N |
| sel_3th_pre_req | 매도3차선잔량 | N |
| sel_3th_pre_bid | 매도3차선호가 | N |
| sel_2th_pre_req_pre | 매도2차선잔량대비 | N |
| sel_2th_pre_req | 매도2차선잔량 | N |
| sel_2th_pre_bid | 매도2차선호가 | N |
| sel_1th_pre_req_pre | 매도1차선잔량대비 | N |
| sel_fpr_req | 매도최우선잔량 | N |
| sel_fpr_bid | 매도최우선호가 | N |
| buy_fpr_bid | 매수최우선호가 | N |
| buy_fpr_req | 매수최우선잔량 | N |
| buy_1th_pre_req_pre | 매수1차선잔량대비 | N |
| buy_2th_pre_bid | 매수2차선호가 | N |
| buy_2th_pre_req | 매수2차선잔량 | N |
| buy_2th_pre_req_pre | 매수2차선잔량대비 | N |
| buy_3th_pre_bid | 매수3차선호가 | N |
| buy_3th_pre_req | 매수3차선잔량 | N |
| buy_3th_pre_req_pre | 매수3차선잔량대비 | N |
| buy_4th_pre_bid | 매수4차선호가 | N |
| buy_4th_pre_req | 매수4차선잔량 | N |
| buy_4th_pre_req_pre | 매수4차선잔량대비 | N |
| buy_5th_pre_bid | 매수5차선호가 | N |
| buy_5th_pre_req | 매수5차선잔량 | N |
| buy_5th_pre_req_pre | 매수5차선잔량대비 | N |
| buy_6th_pre_bid | 매수6차선호가 | N |
| buy_6th_pre_req | 매수6차선잔량 | N |
| buy_6th_pre_req_pre | 매수6차선잔량대비 | N |
| buy_7th_pre_bid | 매수7차선호가 | N |
| buy_7th_pre_req | 매수7차선잔량 | N |
| buy_7th_pre_req_pre | 매수7차선잔량대비 | N |
| buy_8th_pre_bid | 매수8차선호가 | N |
| buy_8th_pre_req | 매수8차선잔량 | N |
| buy_8th_pre_req_pre | 매수8차선잔량대비 | N |
| buy_9th_pre_bid | 매수9차선호가 | N |
| buy_9th_pre_req | 매수9차선잔량 | N |
| buy_9th_pre_req_pre | 매수9차선잔량대비 | N |
| buy_10th_pre_bid | 매수10차선호가 | N |
| buy_10th_pre_req | 매수10차선잔량 | N |
| tot_sel_req_jub_pre | 총매도잔량직전대비 | N |
| tot_sel_req | 총매도잔량 | N |
| tot_buy_req | 총매수잔량 | N |
| tot_buy_req_jub_pre | 총매수잔량직전대비 | N |
| ovt_sel_req_pre | 시간외매도잔량대비 | N |
| ovt_sel_req | 시간외매도잔량 | N |
| ovt_buy_req | 시간외매수잔량 | N |
| ovt_buy_req_pre | 시간외매수잔량대비 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 주식일주월시분요청 (ka10005)

#### 기본 정보

- **API ID**: `ka10005`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| date | 날짜 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| close_pric | 종가 | N |
| pre | 대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| for_poss | 외인보유 | N |
| for_wght | 외인비중 | N |
| for_netprps | 외인순매수 | N |
| orgn_netprps | 기관순매수 | N |
| ind_netprps | 개인순매수 | N |
| frgn | 외국계 | N |
| crd_remn_rt | 신용잔고율 | N |
| prm | 프로그램 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 주식시분요청 (ka10006)

#### 기본 정보

- **API ID**: `ka10006`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| date | 날짜 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| close_pric | 종가 | N |
| pre | 대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| cntr_str | 체결강도 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 시세표성정보요청 (ka10007)

#### 기본 정보

- **API ID**: `ka10007`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| stk_nm | 종목명 | N |
| stk_cd | 종목코드 | N |
| date | 날짜 | N |
| tm | 시간 | N |
| pred_close_pric | 전일종가 | N |
| pred_trde_qty | 전일거래량 | N |
| upl_pric | 상한가 | N |
| lst_pric | 하한가 | N |
| pred_trde_prica | 전일거래대금 | N |
| flo_stkcnt | 상장주식수 | N |
| cur_prc | 현재가 | N |
| smbol | 부호 | N |
| flu_rt | 등락률 | N |
| pred_rt | 전일비 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| cntr_qty | 체결량 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| exp_cntr_pric | 예상체결가 | N |
| exp_cntr_qty | 예상체결량 | N |
| exp_sel_pri_bid | 예상매도우선호가 | N |
| exp_buy_pri_bid | 예상매수우선호가 | N |
| trde_strt_dt | 거래시작일 | N |
| exec_pric | 행사가격 | N |
| hgst_pric | 최고가 | N |
| lwst_pric | 최저가 | N |
| hgst_pric_dt | 최고가일 | N |
| lwst_pric_dt | 최저가일 | N |
| sel_1bid | 매도1호가 | N |
| sel_2bid | 매도2호가 | N |
| sel_3bid | 매도3호가 | N |
| sel_4bid | 매도4호가 | N |
| sel_5bid | 매도5호가 | N |
| sel_6bid | 매도6호가 | N |
| sel_7bid | 매도7호가 | N |
| sel_8bid | 매도8호가 | N |
| sel_9bid | 매도9호가 | N |
| sel_10bid | 매도10호가 | N |
| buy_1bid | 매수1호가 | N |
| buy_2bid | 매수2호가 | N |
| buy_3bid | 매수3호가 | N |
| buy_4bid | 매수4호가 | N |
| buy_5bid | 매수5호가 | N |
| buy_6bid | 매수6호가 | N |
| buy_7bid | 매수7호가 | N |
| buy_8bid | 매수8호가 | N |
| buy_9bid | 매수9호가 | N |
| buy_10bid | 매수10호가 | N |
| sel_1bid_req | 매도1호가잔량 | N |
| sel_2bid_req | 매도2호가잔량 | N |
| sel_3bid_req | 매도3호가잔량 | N |
| sel_4bid_req | 매도4호가잔량 | N |
| sel_5bid_req | 매도5호가잔량 | N |
| sel_6bid_req | 매도6호가잔량 | N |
| sel_7bid_req | 매도7호가잔량 | N |
| sel_8bid_req | 매도8호가잔량 | N |
| sel_9bid_req | 매도9호가잔량 | N |
| sel_10bid_req | 매도10호가잔량 | N |
| buy_1bid_req | 매수1호가잔량 | N |
| buy_2bid_req | 매수2호가잔량 | N |
| buy_3bid_req | 매수3호가잔량 | N |
| buy_4bid_req | 매수4호가잔량 | N |
| buy_5bid_req | 매수5호가잔량 | N |
| buy_6bid_req | 매수6호가잔량 | N |
| buy_7bid_req | 매수7호가잔량 | N |
| buy_8bid_req | 매수8호가잔량 | N |
| buy_9bid_req | 매수9호가잔량 | N |
| buy_10bid_req | 매수10호가잔량 | N |
| sel_1bid_jub_pre | 매도1호가직전대비 | N |
| sel_2bid_jub_pre | 매도2호가직전대비 | N |
| sel_3bid_jub_pre | 매도3호가직전대비 | N |
| sel_4bid_jub_pre | 매도4호가직전대비 | N |
| sel_5bid_jub_pre | 매도5호가직전대비 | N |
| sel_6bid_jub_pre | 매도6호가직전대비 | N |
| sel_7bid_jub_pre | 매도7호가직전대비 | N |
| sel_8bid_jub_pre | 매도8호가직전대비 | N |
| sel_9bid_jub_pre | 매도9호가직전대비 | N |
| sel_10bid_jub_pre | 매도10호가직전대비 | N |
| buy_1bid_jub_pre | 매수1호가직전대비 | N |
| buy_2bid_jub_pre | 매수2호가직전대비 | N |
| buy_3bid_jub_pre | 매수3호가직전대비 | N |
| buy_4bid_jub_pre | 매수4호가직전대비 | N |
| buy_5bid_jub_pre | 매수5호가직전대비 | N |
| buy_6bid_jub_pre | 매수6호가직전대비 | N |
| buy_7bid_jub_pre | 매수7호가직전대비 | N |
| buy_8bid_jub_pre | 매수8호가직전대비 | N |
| buy_9bid_jub_pre | 매수9호가직전대비 | N |
| buy_10bid_jub_pre | 매수10호가직전대비 | N |
| sel_1bid_cnt | 매도1호가건수 | N |
| sel_2bid_cnt | 매도2호가건수 | N |
| sel_3bid_cnt | 매도3호가건수 | N |
| sel_4bid_cnt | 매도4호가건수 | N |
| sel_5bid_cnt | 매도5호가건수 | N |
| buy_1bid_cnt | 매수1호가건수 | N |
| buy_2bid_cnt | 매수2호가건수 | N |
| buy_3bid_cnt | 매수3호가건수 | N |
| buy_4bid_cnt | 매수4호가건수 | N |
| buy_5bid_cnt | 매수5호가건수 | N |
| lpsel_1bid_req | LP매도1호가잔량 | N |
| lpsel_2bid_req | LP매도2호가잔량 | N |
| lpsel_3bid_req | LP매도3호가잔량 | N |
| lpsel_4bid_req | LP매도4호가잔량 | N |
| lpsel_5bid_req | LP매도5호가잔량 | N |
| lpsel_6bid_req | LP매도6호가잔량 | N |
| lpsel_7bid_req | LP매도7호가잔량 | N |
| lpsel_8bid_req | LP매도8호가잔량 | N |
| lpsel_9bid_req | LP매도9호가잔량 | N |
| lpsel_10bid_req | LP매도10호가잔량 | N |
| lpbuy_1bid_req | LP매수1호가잔량 | N |
| lpbuy_2bid_req | LP매수2호가잔량 | N |
| lpbuy_3bid_req | LP매수3호가잔량 | N |
| lpbuy_4bid_req | LP매수4호가잔량 | N |
| lpbuy_5bid_req | LP매수5호가잔량 | N |
| lpbuy_6bid_req | LP매수6호가잔량 | N |
| lpbuy_7bid_req | LP매수7호가잔량 | N |
| lpbuy_8bid_req | LP매수8호가잔량 | N |
| lpbuy_9bid_req | LP매수9호가잔량 | N |
| lpbuy_10bid_req | LP매수10호가잔량 | N |
| tot_buy_req | 총매수잔량 | N |
| tot_sel_req | 총매도잔량 | N |
| tot_buy_cnt | 총매수건수 | N |
| tot_sel_cnt | 총매도건수 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 신주인수권전체시세요청 (ka10011)

#### 기본 정보

- **API ID**: `ka10011`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| newstk_recvrht_tp | 신주인수권구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| fpr_sel_bid | 최우선매도호가 | N |
| fpr_buy_bid | 최우선매수호가 | N |
| acc_trde_qty | 누적거래량 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |

#### Request Example

```json
{"newstk_recvrht_tp": "00"}
```

---

### 일별기관매매종목요청 (ka10044)

#### 기본 정보

- **API ID**: `ka10044`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| trde_tp | 매매구분 | Y |
| mrkt_tp | 시장구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| netprps_qty | 순매수수량 | N |
| netprps_amt | 순매수금액 | N |

#### Request Example

```json
{"strt_dt": "20241106",```Request Example```"end_dt": "20241107","trde_tp": "1","mrkt_tp": "001","stex_tp": "3"}
```

---

### 종목별기관매매추이요청 (ka10045)

#### 기본 정보

- **API ID**: `ka10045`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| orgn_prsm_unp_tp | 기관추정단가구분 | Y |
| for_prsm_unp_tp | 외인추정단가구분 | Y |
| orgn_prsm_avg_pric | 기관추정평균가 | N |
| for_prsm_avg_pric | 외인추정평균가 | N |
| dt | 일자 | N |
| close_pric | 종가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| orgn_dt_acc | 기관기간누적 | N |
| for_dt_acc | 외인기간누적 | N |
| limit_exh_rt | 한도소진율 | N |

#### Request Example

```json
{"stk_cd": "005930","strt_dt": "20241007","end_dt": "20241107","orgn_prsm_unp_tp": "1","for_prsm_unp_tp": "1"}
```

---

### 체결강도추이시간별요청 (ka10046)

#### 기본 정보

- **API ID**: `ka10046`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| cntr_tm | 체결시간 | N |
| cur_prc | 현재가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비기호 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| acc_trde_prica | 누적거래대금 | N |
| acc_trde_qty | 누적거래량 | N |
| cntr_str | 체결강도 | N |
| cntr_str_5min | 체결강도5분 | N |
| cntr_str_20min | 체결강도20분 | N |
| cntr_str_60min | 체결강도60분 | N |
| stex_tp | 거래소구분 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 체결강도추이일별요청 (ka10047)

#### 기본 정보

- **API ID**: `ka10047`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| dt | 일자 | N |
| cur_prc | 현재가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비기호 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| acc_trde_prica | 누적거래대금 | N |
| acc_trde_qty | 누적거래량 | N |
| cntr_str | 체결강도 | N |
| cntr_str_5min | 체결강도5일 | N |
| cntr_str_20min | 체결강도20일 | N |
| cntr_str_60min | 체결강도60일 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 장중투자자별매매요청 (ka10063)

#### 기본 정보

- **API ID**: `ka10063`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| invsr | 투자자별 | Y |
| frgn_all | 외국계전체 | Y |
| smtm_netprps_tp | 동시순매수구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| acc_trde_qty | 누적거래량 | N |
| netprps_amt | 순매수금액 | N |
| prev_netprps_amt | 이전순매수금액 | N |
| buy_amt | 매수금액 | N |
| netprps_amt_irds | 순매수금액증감 | N |
| buy_amt_irds | 매수금액증감 | N |
| sell_amt | 매도금액 | N |
| sell_amt_irds | 매도금액증감 | N |
| netprps_qty | 순매수수량 | N |
| netprps_irds | 순매수증감 | N |
| buy_qty | 매수수량 | N |
| buy_qty_irds | 매수수량증감 | N |
| sell_qty | 매도수량 | N |
| sell_qty_irds | 매도수량증감 | N |

#### Request Example

```json
{"mrkt_tp": "000","amt_qty_tp": "1","invsr": "6","frgn_all": "0","smtm_netprps_tp": "0","stex_tp": "3"}
```

---

### 장마감후투자자별매매요청 (ka10066)

#### 기본 정보

- **API ID**: `ka10066`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| trde_tp | 매매구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| ind_invsr | 개인투자자 | N |
| frgnr_invsr | 외국인투자자 | N |
| orgn | 기관계 | N |
| fnnc_invt | 금융투자 | N |
| insrnc | 보험 | N |
| invtrt | 투신 | N |
| etc_fnnc | 기타금융 | N |
| bank | 은행 | N |
| penfnd_etc | 연기금등 | N |
| samo_fund | 사모펀드 | N |
| natn | 국가 | N |
| etc_corp | 기타법인 | N |

#### Request Example

```json
{"mrkt_tp": "000","amt_qty_tp": "1","trde_tp": "0","stex_tp": "3"}
```

---

### 증권사별종목매매동향요청 (ka10078)

#### 기본 정보

- **API ID**: `ka10078`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mmcm_cd | 회원사코드 | Y |
| stk_cd | 종목코드 | Y |
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| dt | 일자 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| acc_trde_qty | 누적거래량 | N |
| netprps_qty | 순매수수량 | N |
| buy_qty | 매수수량 | N |
| sell_qty | 매도수량 | N |

#### Request Example

```json
{"mmcm_cd": "001","stk_cd": "005930","strt_dt": "20241106","end_dt": "20241107"}
```

---

### 일별주가요청 (ka10086)

#### 기본 정보

- **API ID**: `ka10086`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| qry_dt | 조회일자 | Y |
| indc_tp | 표시구분 | Y |
| date | 날짜 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| close_pric | 종가 | N |
| pred_rt | 전일비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| amt_mn | 금액(백만) | N |
| crd_rt | 신용비 | N |
| ind | 개인 | N |
| orgn | 기관 | N |
| for_qty | 외인수량 | N |
| frgn | 외국계 | N |
| prm | 프로그램 | N |
| for_rt | 외인비 | N |
| for_poss | 외인보유 | N |
| for_wght | 외인비중 | N |
| for_netprps | 외인순매수 | N |
| orgn_netprps | 기관순매수 | N |
| ind_netprps | 개인순매수 | N |
| crd_remn_rt | 신용잔고율 | N |

#### Request Example

```json
{"stk_cd": "005930","qry_dt": "20241125","indc_tp": "0"}
```

---

### 시간외단일가요청 (ka10087)

#### 기본 정보

- **API ID**: `ka10087`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| bid_req_base_tm | 호가잔량기준시간 | N |
| sel_bid_tot_req | 매도호가총잔량 | N |
| buy_bid_tot_req | 매수호가총잔량 | N |
| ovt_sigpric_cur_prc | 시간외단일가_현재가 | N |
| ovt_sigpric_flu_rt | 시간외단일가_등락률 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 금현물체결추이 (ka50010)

#### 기본 정보

- **API ID**: `ka50010`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| cntr_pric | 체결가 | N |
| pred_pre | 전일 대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 누적 거래량 | N |
| acc_trde_prica | 누적 거래대금 | N |
| cntr_trde_qty | 거래량(체결량) | N |
| tm | 체결시간 | N |
| pre_sig | 전일대비기호 | N |
| pri_sel_bid_unit | 매도호가 | N |
| pri_buy_bid_unit | 매수호가 | N |
| cntr_str | 체결강도 | N |

#### Request Example

```json
{"stk_cd": "M04020000"}
```

---

### 금현물일별추이 (ka50012)

#### 기본 정보

- **API ID**: `ka50012`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| base_dt | 기준일자 | Y |
| cur_prc | 종가 | N |
| pred_pre | 전일 대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 누적 거래량 | N |
| acc_trde_prica | 누적 거래대금(백만) | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| dt | 일자 | N |
| pre_sig | 전일대비기호 | N |
| orgn_netprps | 기관 순매수 수량 | N |
| for_netprps | 외국인 순매수 수량 | N |
| ind_netprps | 순매매량(개인) | N |

#### Request Example

```json
{"stk_cd": "M04020000","base_dt": "20250820"}
```

---

### 금현물예상체결 (ka50087)

#### 기본 정보

- **API ID**: `ka50087`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| exp_cntr_pric | 예상 체결가 | N |
| exp_flu_rt | 예상 체결가 등락율 | N |
| exp_acc_trde_qty | 예상 체결 수량(누적) | N |
| exp_cntr_trde_qty | 예상 체결 수량 | N |
| exp_tm | 예상 체결 시간 | N |
| stex_tp | 거래소 구분 | N |

#### Request Example

```json
{"stk_cd": "M04020000"}
```

---

### 금현물 시세정보 (ka50100)

#### 기본 정보

- **API ID**: `ka50100`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| pred_rt | 전일비 | N |
| upl_pric | 상한가 | N |
| lst_pric | 하한가 | N |
| pred_close_pric | 전일종가 | N |

#### Request Example

```json
{"stk_cd": "M04020000"}
```

---

### 금현물 호가 (ka50101)

#### 기본 정보

- **API ID**: `ka50101`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tic_scope | 틱범위 | Y |
| cntr_pric | 체결가 | N |
| pred_pre | 전일 대비(원) | N |
| flu_rt | 등락율 | N |
| trde_qty | 누적 거래량 | N |
| acc_trde_prica | 누적 거래대금 | N |
| cntr_trde_qty | 거래량(체결량) | N |
| tm | 체결시간 | N |
| pre_sig | 전일대비기호 | N |
| pri_sel_bid_unit | 매도호가 | N |
| pri_buy_bid_unit | 매수호가 | N |
| cntr_str | 체결강도 | N |
| lpmmcm_nm_1 | K.O 접근도 | N |
| stex_tp | 거래소구분 | N |

#### Request Example

```json
{"stk_cd": "M04020000","tic_scope": "1"}
```

---

### 프로그램매매추이요청 시간대별 (ka90005)

#### 기본 정보

- **API ID**: `ka90005`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| date | 날짜 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| mrkt_tp | 시장구분 | Y |
| min_tic_tp | 분틱구분 | Y |
| stex_tp | 거래소구분 | Y |
| cntr_tm | 체결시간 | N |
| dfrt_trde_sel | 차익거래매도 | N |
| dfrt_trde_buy | 차익거래매수 | N |
| dfrt_trde_netprps | 차익거래순매수 | N |
| ndiffpro_trde_sel | 비차익거래매도 | N |
| ndiffpro_trde_buy | 비차익거래매수 | N |
| dfrt_trde_sell_qty | 차익거래매도수량 | N |
| dfrt_trde_buy_qty | 차익거래매수수량 | N |
| all_sel | 전체매도 | N |
| all_buy | 전체매수 | N |
| all_netprps | 전체순매수 | N |
| kospi200 | KOSPI200 | N |
| basis | BASIS | N |

#### Request Example

```json
{"date": "20241101","amt_qty_tp": "1","mrkt_tp": "P00101","min_tic_tp": "1","stex_tp": "1"}
```

---

### 프로그램매매차익잔고추이요청 (ka90006)

#### 기본 정보

- **API ID**: `ka90006`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| date | 날짜 | Y |
| stex_tp | 거래소구분 | Y |
| dt | 일자 | N |
| buy_dfrt_trde_qty | 매수차익거래수량 | N |
| buy_dfrt_trde_amt | 매수차익거래금액 | N |
| sel_dfrt_trde_qty | 매도차익거래수량 | N |
| sel_dfrt_trde_amt | 매도차익거래금액 | N |

#### Request Example

```json
{"date": "20241125","stex_tp": "1"}
```

---

### 프로그램매매누적추이요청 (ka90007)

#### 기본 정보

- **API ID**: `ka90007`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| date | 날짜 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| mrkt_tp | 시장구분 | Y |
| stex_tp | 거래소구분 | Y |
| dt | 일자 | N |
| kospi200 | KOSPI200 | N |
| basis | BASIS | N |
| dfrt_trde_tdy | 차익거래당일 | N |
| dfrt_trde_acc | 차익거래누적 | N |
| ndiffpro_trde_tdy | 비차익거래당일 | N |
| ndiffpro_trde_acc | 비차익거래누적 | N |
| all_tdy | 전체당일 | N |
| all_acc | 전체누적 | N |

#### Request Example

```json
{"date": "20240525","amt_qty_tp": "1","mrkt_tp": "0","stex_tp": "3"}
```

---

### 종목시간별프로그램매매추이요청 (ka90008)

#### 기본 정보

- **API ID**: `ka90008`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| amt_qty_tp | 금액수량구분 | Y |
| stk_cd | 종목코드 | Y |
| date | 날짜 | Y |
| tm | 시간 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| prm_sell_amt | 프로그램매도금액 | N |
| prm_buy_amt | 프로그램매수금액 | N |
| prm_netprps_amt | 프로그램순매수금액 | N |
| prm_sell_qty | 프로그램매도수량 | N |
| prm_buy_qty | 프로그램매수수량 | N |
| prm_netprps_qty | 프로그램순매수수량 | N |
| base_pric_tm | 기준가시간 | N |
| dbrt_trde_rpy_sum | 대차거래상환주수합 | N |
| remn_rcvord_sum | 잔고수주합 | N |
| stex_tp | 거래소구분 | N |

#### Request Example

```json
{"amt_qty_tp": "1","stk_cd": "005930","date": "20241125"}
```

---

### 프로그램매매추이요청 일자별 (ka90010)

#### 기본 정보

- **API ID**: `ka90010`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| date | 날짜 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| mrkt_tp | 시장구분 | Y |
| min_tic_tp | 분틱구분 | Y |
| stex_tp | 거래소구분 | Y |
| cntr_tm | 체결시간 | N |
| dfrt_trde_sel | 차익거래매도 | N |
| dfrt_trde_buy | 차익거래매수 | N |
| dfrt_trde_netprps | 차익거래순매수 | N |
| ndiffpro_trde_sel | 비차익거래매도 | N |
| ndiffpro_trde_buy | 비차익거래매수 | N |
| dfrt_trde_sell_qty | 차익거래매도수량 | N |
| dfrt_trde_buy_qty | 차익거래매수수량 | N |
| all_sel | 전체매도 | N |
| all_buy | 전체매수 | N |
| all_netprps | 전체순매수 | N |
| kospi200 | KOSPI200 | N |
| basis | BASIS | N |

#### Request Example

```json
{"date": "20241125","amt_qty_tp": "1","mrkt_tp": "P00101","min_tic_tp": "0","stex_tp": "1"}
```

---

### 종목일별프로그램매매추이요청 (ka90013)

#### 기본 정보

- **API ID**: `ka90013`
- **Method**: `POST`
- **URL**: `/api/dostk/mrkcond`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| amt_qty_tp | 금액수량구분 | N |
| stk_cd | 종목코드 | Y |
| date | 날짜 | N |
| dt | 일자 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| prm_sell_amt | 프로그램매도금액 | N |
| prm_buy_amt | 프로그램매수금액 | N |
| prm_netprps_amt | 프로그램순매수금액 | N |
| prm_sell_qty | 프로그램매도수량 | N |
| prm_buy_qty | 프로그램매수수량 | N |
| prm_netprps_qty | 프로그램순매수수량 | N |
| base_pric_tm | 기준가시간 | N |
| dbrt_trde_rpy_sum | 대차거래상환주수합 | N |
| remn_rcvord_sum | 잔고수주합 | N |
| stex_tp | 거래소구분 | N |

#### Request Example

```json
{"amt_qty_tp": "","stk_cd": "005930","date": ""}
```

---

## 신용주문

### 신용 매수주문 (kt10006)

#### 기본 정보

- **API ID**: `kt10006`
- **Method**: `POST`
- **URL**: `/api/dostk/crdordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| stk_cd | 종목코드 | Y |
| ord_qty | 주문수량 | Y |
| ord_uv | 주문단가 | N |
| trde_tp | 매매구분 | Y |
| cond_uv | 조건단가 | N |
| ord_no | 주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | N |

#### Request Example

```json
{```Request Example```"dmst_stex_tp": "KRX","stk_cd": "005930","ord_qty": "1","ord_uv": "2580","trde_tp": "0","cond_uv": ""}
```

---

### 신용 매도주문 (kt10007)

#### 기본 정보

- **API ID**: `kt10007`
- **Method**: `POST`
- **URL**: `/api/dostk/crdordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| stk_cd | 종목코드 | Y |
| ord_qty | 주문수량 | Y |
| ord_uv | 주문단가 | N |
| trde_tp | 매매구분 | Y |
| crd_deal_tp | 신용거래구분 | Y |
| crd_loan_dt | 대출일 | N |
| cond_uv | 조건단가 | N |
| ord_no | 주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | N |

#### Request Example

```json
{"dmst_stex_tp": "KRX","stk_cd": "005930","ord_qty": "3","ord_uv": "6450","trde_tp": "0","crd_deal_tp": "99","crd_loan_dt": "","cond_uv": ""}
```

---

### 신용 정정주문 (kt10008)

#### 기본 정보

- **API ID**: `kt10008`
- **Method**: `POST`
- **URL**: `/api/dostk/crdordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| orig_ord_no | 원주문번호 | Y |
| stk_cd | 종목코드 | Y |
| mdfy_qty | 정정수량 | Y |
| mdfy_uv | 정정단가 | Y |
| mdfy_cond_uv | 정정조건단가 | N |
| ord_no | 주문번호 | N |
| base_orig_ord_no | 모주문번호 | N |
| mdfy_qty | 정정수량 | N |
| dmst_stex_tp | 국내거래소구분 | N |

#### Request Example

```json
{"dmst_stex_tp": "KRX",```Request Example```"orig_ord_no": "0000455","stk_cd": "005930","mdfy_qty": "1","mdfy_uv": "2590","mdfy_cond_uv": ""}
```

---

### 신용 취소주문 (kt10009)

#### 기본 정보

- **API ID**: `kt10009`
- **Method**: `POST`
- **URL**: `/api/dostk/crdordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| orig_ord_no | 원주문번호 | Y |
| stk_cd | 종목코드 | Y |
| cncl_qty | 취소수량 | Y |
| ord_no | 주문번호 | N |
| base_orig_ord_no | 모주문번호 | N |
| cncl_qty | 취소수량 | N |

#### Request Example

```json
{"dmst_stex_tp": "KRX","orig_ord_no": "0001615","stk_cd": "005930","cncl_qty": "1"}
```

---

## 실시간시세

### 주문체결 (00)

#### 기본 정보

- **API ID**: `00`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": [""],"type": ["00"]}
```

---

### 잔고 (04)

#### 기본 정보

- **API ID**: `04`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1",```Request Example```"refresh": "1","data": [{"item": [""],"type": ["04"]}
```

---

### 주식기세 (0A)

#### 기본 정보

- **API ID**: `0A`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0A"]}
```

---

### 주식체결 (0B)

#### 기본 정보

- **API ID**: `0B`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0B"]}
```

---

### 주식우선호가 (0C)

#### 기본 정보

- **API ID**: `0C`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0C"]}
```

---

### 주식호가잔량 (0D)

#### 기본 정보

- **API ID**: `0D`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0D"]}
```

---

### 주식시간외호가 (0E)

#### 기본 정보

- **API ID**: `0E`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0E"]}
```

---

### 주식당일거래원 (0F)

#### 기본 정보

- **API ID**: `0F`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0F"]}
```

---

### ETF NAV (0G)

#### 기본 정보

- **API ID**: `0G`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["069500"],"type": ["0G"]}
```

---

### 주식예상체결 (0H)

#### 기본 정보

- **API ID**: `0H`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0H"]}
```

---

### 국제금환산가격 (0I)

#### 기본 정보

- **API ID**: `0I`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["MGD"],"type": ["0I"]}
```

---

### 업종지수 (0J)

#### 기본 정보

- **API ID**: `0J`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["001"],"type": ["0J"]}
```

---

### 업종등락 (0U)

#### 기본 정보

- **API ID**: `0U`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["001"],"type": ["0U"]}
```

---

### 주식종목정보 (0g)

#### 기본 정보

- **API ID**: `0g`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0g"]}
```

---

### ELW 이론가 (0m)

#### 기본 정보

- **API ID**: `0m`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["57JBHH"],"type": ["0m"]}
```

---

### 장시작시간 (0s)

#### 기본 정보

- **API ID**: `0s`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": [""],"type": ["0s"]}
```

---

### ELW 지표 (0u)

#### 기본 정보

- **API ID**: `0u`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["57JBHH"],"type": ["0u"]}
```

---

### 종목프로그램매매 (0w)

#### 기본 정보

- **API ID**: `0w`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": ["005930"],"type": ["0w"]}
```

---

### VI발동/해제 (1h)

#### 기본 정보

- **API ID**: `1h`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| grp_no | 그룹번호 | Y |
| refresh | 기존등록유지여부 | Y |
| item | 실시간 등록 요소 | N |
| type | 실시간 항목 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| type | 실시간항목 | N |
| name | 실시간 항목명 | N |
| item | 실시간 등록 요소 | N |

#### Request Example

```json
{"trnm": "REG","grp_no": "1","refresh": "1","data": [{"item": [""],"type": ["1h"]}
```

---

## 업종

### 업종프로그램요청 (ka10010)

#### 기본 정보

- **API ID**: `ka10010`
- **Method**: `POST`
- **URL**: `/api/dostk/sect`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| dfrt_trst_sell_qty | 차익위탁매도수량 | N |
| dfrt_trst_sell_amt | 차익위탁매도금액 | N |
| dfrt_trst_buy_qty | 차익위탁매수수량 | N |
| dfrt_trst_buy_amt | 차익위탁매수금액 | N |
| dfrt_trst_netprps_qty | 차익위탁순매수수량 | N |
| ndiffpro_trst_sell_qty | 비차익위탁매도수량 | N |
| ndiffpro_trst_buy_qty | 비차익위탁매수수량 | N |
| ndiffpro_trst_buy_am | 비차익위탁매수금액 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 업종별투자자순매수요청 (ka10051)

#### 기본 정보

- **API ID**: `ka10051`
- **Method**: `POST`
- **URL**: `/api/dostk/sect`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| base_dt | 기준일자 | N |
| stex_tp | 거래소구분 | Y |
| inds_cd | 업종코드 | N |
| inds_nm | 업종명 | N |
| cur_prc | 현재가 | N |
| pre_smbol | 대비부호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| sc_netprps | 증권순매수 | N |
| insrnc_netprps | 보험순매수 | N |
| invtrt_netprps | 투신순매수 | N |
| bank_netprps | 은행순매수 | N |
| jnsinkm_netprps | 종신금순매수 | N |
| endw_netprps | 기금순매수 | N |
| etc_corp_netprps | 기타법인순매수 | N |
| ind_netprps | 개인순매수 | N |
| frgnr_netprps | 외국인순매수 | N |
| natn_netprps | 국가순매수 | N |
| orgn_netprps | 기관계순매수 | N |

#### Request Example

```json
{"mrkt_tp": "0","amt_qty_tp": "0","base_dt": "20241107","stex_tp": "3"}
```

---

### 업종현재가요청 (ka20001)

#### 기본 정보

- **API ID**: `ka20001`
- **Method**: `POST`
- **URL**: `/api/dostk/sect`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| inds_cd | 업종코드 | Y |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| trde_frmatn_stk_num | 거래형성종목수 | N |
| trde_frmatn_rt | 거래형성비율 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| upl | 상한 | N |
| rising | 상승 | N |
| stdns | 보합 | N |
| fall | 하락 | N |
| lst | 하한 | N |
| 52wk_hgst_pric | 52주최고가 | N |
| 52wk_hgst_pric_dt | 52주최고가일 | N |
| 52wk_lwst_pric | 52주최저가 | N |
| 52wk_lwst_pric_dt | 52주최저가일 | N |
| tm_n | 시간n | N |
| cur_prc_n | 현재가n | N |
| pred_pre_sig_n | 전일대비기호n | N |
| pred_pre_n | 전일대비n | N |
| flu_rt_n | 등락률n | N |
| trde_qty_n | 거래량n | N |
| acc_trde_qty_n | 누적거래량n | N |

#### Request Example

```json
{"mrkt_tp": "0","inds_cd": "001"}
```

---

### 업종별주가요청 (ka20002)

#### 기본 정보

- **API ID**: `ka20002`
- **Method**: `POST`
- **URL**: `/api/dostk/sect`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| inds_cd | 업종코드 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| now_trde_qty | 현재거래량 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |

#### Request Example

```json
{"mrkt_tp": "0","inds_cd": "001","stex_tp": "1"}
```

---

### 전업종지수요청 (ka20003)

#### 기본 정보

- **API ID**: `ka20003`
- **Method**: `POST`
- **URL**: `/api/dostk/sect`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| inds_cd | 업종코드 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| wght | 비중 | N |
| trde_prica | 거래대금 | N |
| upl | 상한 | N |
| rising | 상승 | N |
| stdns | 보합 | N |
| fall | 하락 | N |
| lst | 하한 | N |
| flo_stk_num | 상장종목수 | N |

#### Request Example

```json
{"inds_cd": "001"}
```

---

### 업종현재가일별요청 (ka20009)

#### 기본 정보

- **API ID**: `ka20009`
- **Method**: `POST`
- **URL**: `/api/dostk/sect`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| inds_cd | 업종코드 | Y |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| trde_frmatn_stk_num | 거래형성종목수 | N |
| trde_frmatn_rt | 거래형성비율 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| upl | 상한 | N |
| rising | 상승 | N |
| stdns | 보합 | N |
| fall | 하락 | N |
| lst | 하한 | N |
| 52wk_hgst_pric | 52주최고가 | N |
| 52wk_hgst_pric_dt | 52주최고가일 | N |
| 52wk_lwst_pric | 52주최저가 | N |
| 52wk_lwst_pric_dt | 52주최저가일 | N |
| dt_n | 일자n | N |
| cur_prc_n | 현재가n | N |
| pred_pre_sig_n | 전일대비기호n | N |
| pred_pre_n | 전일대비n | N |
| flu_rt_n | 등락률n | N |
| acc_trde_qty_n | 누적거래량n | N |

#### Request Example

```json
{"mrkt_tp": "0","inds_cd": "001"}
```

---

## 조건검색

### 조건검색 목록조회 (ka10171)

#### 기본 정보

- **API ID**: `ka10171`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | TR명 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| seq | 조건검색식 일련번호 | N |
| name | 조건검색식 명 | N |

#### Request Example

```json
{"trnm": "CNSRLST"}
```

---

### 조건검색 요청 일반 (ka10172)

#### 기본 정보

- **API ID**: `ka10172`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| seq | 조건검색식 일련번호 | Y |
| search_type | 조회타입 | Y |
| stex_tp | 거래소구분 | Y |
| cont_yn | 연속조회여부 | N |
| next_key | 연속조회키 | N |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| seq | 조건검색식 일련번호 | N |
| cont_yn | 연속조회여부 | N |
| next_key | 연속조회키 | N |
| 9001 | 종목코드 | N |
| 302 | 종목명 | N |
| 10 | 현재가 | N |
| 25 | 전일대비기호 | N |
| 11 | 전일대비 | N |
| 12 | 등락율 | N |
| 13 | 누적거래량 | N |
| 16 | 시가 | N |
| 17 | 고가 | N |
| 18 | 저가 | N |

#### Request Example

```json
{"trnm": "CNSRREQ","seq": "4","search_type": "0","stex_tp": "K","cont_yn": "N","next_key": ""}
```

---

### 조건검색 요청 실시간 (ka10173)

#### 기본 정보

- **API ID**: `ka10173`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| seq | 조건검색식 일련번호 | Y |
| search_type | 조회타입 | Y |
| stex_tp | 거래소구분 | Y |
| return_code | 결과코드 | N |
| return_msg | 결과메시지 | N |
| trnm | 서비스명 | N |
| seq | 조건검색식 일련번호 | N |
| jmcode | 종목코드 | N |
| trnm | 서비스명 | Y |
| type | 실시간 항목 | Y |
| name | 실시간 항목명 | Y |

#### Request Example

```json
{"trnm": "CNSRREQ","seq": "4","search_type": "1","stex_tp": "K"}
```

---

### 조건검색 실시간 해제 (ka10174)

#### 기본 정보

- **API ID**: `ka10174`
- **Method**: `POST`
- **URL**: `/api/dostk/websocket`
- **모의투자 지원**: 예
- **WebSocket**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trnm | 서비스명 | Y |
| seq | 조건검색식 일련번호 | Y |
| return_code | 결과코드 | Y |
| return_msg | 결과메시지 | Y |
| trnm | 서비스명 | Y |
| seq | 조건검색식 일련번호 | Y |

#### Request Example

```json
{"trnm": "CNSRCLR","seq": "1"}
```

---

## 종목정보

### 실시간종목조회순위 (ka00198)

#### 기본 정보

- **API ID**: `ka00198`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| qry_tp | 구분 | Y |
| stk_nm | 종목명 | N |
| bigd_rank | 빅데이터 순위 | N |
| rank_chg | 순위 등락 | N |
| rank_chg_sign | 순위 등락 부호 | N |
| past_curr_prc | 과거 현재가 | N |
| base_comp_sign | 기준가 대비 부호 | N |
| base_comp_chgr | 기준가 대비 등락율 | N |
| prev_base_sign | 직전 기준 대비 부호 | N |
| dt | 일자 | N |
| tm | 시간 | N |
| stk_cd | 종목코드 | N |

#### Request Example

```json
{"qry_tp": "1"}
```

---

### 주식기본정보요청 (ka10001)

#### 기본 정보

- **API ID**: `ka10001`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| setl_mm | 결산월 | N |
| fav | 액면가 | N |
| cap | 자본금 | N |
| flo_stk | 상장주식 | N |
| crd_rt | 신용비율 | N |
| oyr_hgst | 연중최고 | N |
| oyr_lwst | 연중최저 | N |
| mac | 시가총액 | N |
| mac_wght | 시가총액비중 | N |
| for_exh_rt | 외인소진률 | N |
| repl_pric | 대용가 | N |
| per | PER | N |
| eps | EPS | N |
| roe | ROE | N |
| pbr | PBR | N |
| ev | EV | N |
| bps | BPS | N |
| sale_amt | 매출액 | N |
| bus_pro | 영업이익 | N |
| cup_nga | 당기순이익 | N |
| 250hgst | 250최고 | N |
| 250lwst | 250최저 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| upl_pric | 상한가 | N |
| lst_pric | 하한가 | N |
| base_pric | 기준가 | N |
| exp_cntr_pric | 예상체결가 | N |
| exp_cntr_qty | 예상체결수량 | N |
| 250hgst_pric_dt | 250최고가일 | N |
| 250hgst_pric_pre_rt | 250최고가대비율 | N |
| 250lwst_pric_dt | 250최저가일 | N |
| 250lwst_pric_pre_rt | 250최저가대비율 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| trde_pre | 거래대비 | N |
| fav_unit | 액면가단위 | N |
| dstr_stk | 유통주식 | N |
| dstr_rt | 유통비율 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 주식거래원요청 (ka10002)

#### 기본 정보

- **API ID**: `ka10002`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| flu_smbol | 등락부호 | N |
| base_pric | 기준가 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| sel_trde_ori_nm_1 | 매도거래원명1 | N |
| sel_trde_ori_1 | 매도거래원1 | N |
| sel_trde_qty_1 | 매도거래량1 | N |
| buy_trde_ori_nm_1 | 매수거래원명1 | N |
| buy_trde_ori_1 | 매수거래원1 | N |
| buy_trde_qty_1 | 매수거래량1 | N |
| sel_trde_ori_nm_2 | 매도거래원명2 | N |
| sel_trde_ori_2 | 매도거래원2 | N |
| sel_trde_qty_2 | 매도거래량2 | N |
| buy_trde_ori_nm_2 | 매수거래원명2 | N |
| buy_trde_ori_2 | 매수거래원2 | N |
| buy_trde_qty_2 | 매수거래량2 | N |
| sel_trde_ori_nm_3 | 매도거래원명3 | N |
| sel_trde_ori_3 | 매도거래원3 | N |
| sel_trde_qty_3 | 매도거래량3 | N |
| buy_trde_ori_nm_3 | 매수거래원명3 | N |
| buy_trde_ori_3 | 매수거래원3 | N |
| buy_trde_qty_3 | 매수거래량3 | N |
| sel_trde_ori_nm_4 | 매도거래원명4 | N |
| sel_trde_ori_4 | 매도거래원4 | N |
| sel_trde_qty_4 | 매도거래량4 | N |
| buy_trde_ori_nm_4 | 매수거래원명4 | N |
| buy_trde_ori_4 | 매수거래원4 | N |
| buy_trde_qty_4 | 매수거래량4 | N |
| sel_trde_ori_nm_5 | 매도거래원명5 | N |
| sel_trde_ori_5 | 매도거래원5 | N |
| sel_trde_qty_5 | 매도거래량5 | N |
| buy_trde_ori_nm_5 | 매수거래원명5 | N |
| buy_trde_ori_5 | 매수거래원5 | N |
| buy_trde_qty_5 | 매수거래량5 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 체결정보요청 (ka10003)

#### 기본 정보

- **API ID**: `ka10003`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tm | 시간 | N |
| cur_prc | 현재가 | N |
| pred_pre | 전일대비 | N |
| pre_rt | 대비율 | N |
| pri_sel_bid_unit | 우선매도호가단위 | N |
| pri_buy_bid_unit | 우선매수호가단위 | N |
| cntr_trde_qty | 체결거래량 | N |
| sign | sign | N |
| acc_trde_qty | 누적거래량 | N |
| acc_trde_prica | 누적거래대금 | N |
| cntr_str | 체결강도 | N |
| stex_tp | 거래소구분 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 신용매매동향요청 (ka10013)

#### 기본 정보

- **API ID**: `ka10013`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| dt | 일자 | Y |
| qry_tp | 조회구분 | Y |
| dt | 일자 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |
| new | 신규 | N |
| rpya | 상환 | N |
| remn | 잔고 | N |
| amt | 금액 | N |
| pre | 대비 | N |
| shr_rt | 공여율 | N |
| remn_rt | 잔고율 | N |

#### Request Example

```json
{"stk_cd": "005930","dt": "20241104","qry_tp": "1"}
```

---

### 일별거래상세요청 (ka10015)

#### 기본 정보

- **API ID**: `ka10015`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| strt_dt | 시작일자 | Y |
| dt | 일자 | N |
| close_pric | 종가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| bf_mkrt_trde_qty | 장전거래량 | N |
| bf_mkrt_trde_wght | 장전거래비중 | N |
| opmr_trde_qty | 장중거래량 | N |
| opmr_trde_wght | 장중거래비중 | N |
| af_mkrt_trde_qty | 장후거래량 | N |
| af_mkrt_trde_wght | 장후거래비중 | N |
| tot_3 | 합계3 | N |
| prid_trde_qty | 기간중거래량 | N |
| cntr_str | 체결강도 | N |
| for_poss | 외인보유 | N |
| for_wght | 외인비중 | N |
| for_netprps | 외인순매수 | N |
| orgn_netprps | 기관순매수 | N |
| ind_netprps | 개인순매수 | N |
| frgn | 외국계 | N |
| crd_remn_rt | 신용잔고율 | N |
| prm | 프로그램 | N |
| bf_mkrt_trde_prica | 장전거래대금 | N |
| opmr_trde_prica | 장중거래대금 | N |
| af_mkrt_trde_prica | 장후거래대금 | N |

#### Request Example

```json
{"stk_cd": "005930","strt_dt": "20241105"}
```

---

### 신고저가요청 (ka10016)

#### 기본 정보

- **API ID**: `ka10016`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| ntl_tp | 신고저구분 | Y |
| high_low_close_tp | 고저종구분 | Y |
| stk_cnd | 종목조건 | Y |
| trde_qty_tp | 거래량구분 | Y |
| crd_cnd | 신용조건 | Y |
| updown_incls | 상하한포함 | Y |
| dt | 기간 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |

#### Request Example

```json
{"mrkt_tp": "000","ntl_tp": "1","high_low_close_tp": "1","stk_cnd": "0","trde_qty_tp": "00000","crd_cnd": "0","updown_incls": "0","dt": "5","stex_tp": "1"}
```

---

### 상하한가요청 (ka10017)

#### 기본 정보

- **API ID**: `ka10017`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| updown_tp | 상하한구분 | Y |
| sort_tp | 정렬구분 | Y |
| stk_cnd | 종목조건 | Y |
| trde_qty_tp | 거래량구분 | Y |
| crd_cnd | 신용조건 | Y |
| trde_gold_tp | 매매금구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_infr | 종목정보 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| pred_trde_qty | 전일거래량 | N |
| sel_req | 매도잔량 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| buy_req | 매수잔량 | N |
| cnt | 횟수 | N |

#### Request Example

```json
{"mrkt_tp": "000","updown_tp": "1","sort_tp": "1","stk_cnd": "0","trde_qty_tp": "0000","crd_cnd": "0","trde_gold_tp": "0","stex_tp": "1"}
```

---

### 고저가근접요청 (ka10018)

#### 기본 정보

- **API ID**: `ka10018`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| high_low_tp | 고저구분 | Y |
| alacc_rt | 근접율 | Y |
| mrkt_tp | 시장구분 | Y |
| trde_qty_tp | 거래량구분 | Y |
| stk_cnd | 종목조건 | Y |
| crd_cnd | 신용조건 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| trde_qty | 거래량 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| tdy_high_pric | 당일고가 | N |
| tdy_low_pric | 당일저가 | N |

#### Request Example

```json
{"high_low_tp": "1","alacc_rt": "05","mrkt_tp": "000","trde_qty_tp": "0000","stk_cnd": "0","crd_cnd": "0","stex_tp": "1"}
```

---

### 가격급등락요청 (ka10019)

#### 기본 정보

- **API ID**: `ka10019`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| flu_tp | 등락구분 | Y |
| tm_tp | 시간구분 | Y |
| tm | 시간 | Y |
| trde_qty_tp | 거래량구분 | Y |
| stk_cnd | 종목조건 | Y |
| crd_cnd | 신용조건 | Y |
| pric_cnd | 가격조건 | Y |
| updown_incls | 상하한포함 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_cls | 종목분류 | N |
| stk_nm | 종목명 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| base_pric | 기준가 | N |
| cur_prc | 현재가 | N |
| base_pre | 기준대비 | N |
| trde_qty | 거래량 | N |
| jmp_rt | 급등률 | N |

#### Request Example

```json
{"mrkt_tp": "000","flu_tp": "1","tm_tp": "1","tm": "60","trde_qty_tp": "0000","stk_cnd": "0","crd_cnd": "0","pric_cnd": "0","updown_incls": "1","stex_tp": "1"}
```

---

### 거래량갱신요청 (ka10024)

#### 기본 정보

- **API ID**: `ka10024`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| cycle_tp | 주기구분 | Y |
| trde_qty_tp | 거래량구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| prev_trde_qty | 이전거래량 | N |
| now_trde_qty | 현재거래량 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |

#### Request Example

```json
{"mrkt_tp": "000","cycle_tp": "5","trde_qty_tp": "5","stex_tp": "3"}
```

---

### 매물대집중요청 (ka10025)

#### 기본 정보

- **API ID**: `ka10025`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| prps_cnctr_rt | 매물집중비율 | Y |
| cur_prc_entry | 현재가진입 | Y |
| prpscnt | 매물대수 | Y |
| cycle_tp | 주기구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| now_trde_qty | 현재거래량 | N |
| pric_strt | 가격대시작 | N |
| pric_end | 가격대끝 | N |
| prps_qty | 매물량 | N |
| prps_rt | 매물비 | N |

#### Request Example

```json
{"mrkt_tp": "000","prps_cnctr_rt": "50","cur_prc_entry": "0","prpscnt": "10","cycle_tp": "50","stex_tp": "3"}
```

---

### 고저PER요청 (ka10026)

#### 기본 정보

- **API ID**: `ka10026`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| pertp | PER구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| per | PER | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| now_trde_qty | 현재거래량 | N |
| sel_bid | 매도호가 | N |

#### Request Example

```json
{"pertp": "1","stex_tp": "3"}
```

---

### 시가대비등락률요청 (ka10028)

#### 기본 정보

- **API ID**: `ka10028`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| sort_tp | 정렬구분 | Y |
| trde_qty_cnd | 거래량조건 | Y |
| mrkt_tp | 시장구분 | Y |
| updown_incls | 상하한포함 | Y |
| stk_cnd | 종목조건 | Y |
| crd_cnd | 신용조건 | Y |
| trde_prica_cnd | 거래대금조건 | Y |
| flu_cnd | 등락조건 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락률 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| open_pric_pre | 시가대비 | N |
| now_trde_qty | 현재거래량 | N |
| cntr_str | 체결강도 | N |

#### Request Example

```json
{"sort_tp": "1","trde_qty_cnd": "0000","mrkt_tp": "000","updown_incls": "1","stk_cnd": "0","crd_cnd": "0","trde_prica_cnd": "0","flu_cnd": "1","stex_tp": "3"}
```

---

### 거래원매물대분석요청 (ka10043)

#### 기본 정보

- **API ID**: `ka10043`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| qry_dt_tp | 조회기간구분 | Y |
| pot_tp | 시점구분 | Y |
| dt | 기간 | Y |
| sort_base | 정렬기준 | Y |
| mmcm_cd | 회원사코드 | Y |
| stex_tp | 거래소구분 | Y |
| dt | 일자 | N |
| close_pric | 종가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| sel_qty | 매도량 | N |
| buy_qty | 매수량 | N |
| netprps_qty | 순매수수량 | N |
| trde_qty_sum | 거래량합 | N |
| trde_wght | 거래비중 | N |

#### Request Example

```json
{"stk_cd": "005930","strt_dt": "20241031","end_dt": "20241107","qry_dt_tp": "0","pot_tp": "0","dt": "5","sort_base": "1","mmcm_cd": "36","stex_tp": "3"}
```

---

### 거래원순간거래량요청 (ka10052)

#### 기본 정보

- **API ID**: `ka10052`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mmcm_cd | 회원사코드 | Y |
| stk_cd | 종목코드 | N |
| mrkt_tp | 시장구분 | Y |
| qty_tp | 수량구분 | Y |
| pric_tp | 가격구분 | Y |
| stex_tp | 거래소구분 | Y |
| tm | 시간 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| trde_ori_nm | 거래원명 | N |
| tp | 구분 | N |
| mont_trde_qty | 순간거래량 | N |
| acc_netprps | 누적순매수 | N |
| cur_prc | 현재가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |

#### Request Example

```json
{"mmcm_cd": "888","stk_cd": "","mrkt_tp": "0","qty_tp": "0","pric_tp": "0","stex_tp": "3"}
```

---

### 변동성완화장치발동종목요청 (ka10054)

#### 기본 정보

- **API ID**: `ka10054`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| bf_mkrt_tp | 장전구분 | Y |
| stk_cd | 종목코드 | N |
| motn_tp | 발동구분 | Y |
| skip_stk | 제외종목 | Y |
| trde_qty_tp | 거래량구분 | Y |
| min_trde_qty | 최소거래량 | Y |
| max_trde_qty | 최대거래량 | Y |
| trde_prica_tp | 거래대금구분 | Y |
| min_trde_prica | 최소거래대금 | Y |
| max_trde_prica | 최대거래대금 | Y |
| motn_drc | 발동방향 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| acc_trde_qty | 누적거래량 | N |
| motn_pric | 발동가격 | N |
| dynm_dispty_rt | 동적괴리율 | N |
| virelis_time | VI해제시각 | N |
| viaplc_tp | VI적용구분 | N |
| dynm_stdpc | 동적기준가격 | N |
| static_stdpc | 정적기준가격 | N |
| static_dispty_rt | 정적괴리율 | N |
| vimotn_cnt | VI발동횟수 | N |
| stex_tp | 거래소구분 | N |

#### Request Example

```json
{"mrkt_tp": "000","bf_mkrt_tp": "0","stk_cd": "","motn_tp": "0","skip_stk": "000000000","trde_qty_tp": "0","min_trde_qty": "0","max_trde_qty": "0","trde_prica_tp": "0","min_trde_prica": "0","max_trde_prica": "0","motn_drc": "0","stex_tp": "3"}
```

---

### 당일전일체결량요청 (ka10055)

#### 기본 정보

- **API ID**: `ka10055`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tdy_pred | 당일전일 | Y |
| cntr_tm | 체결시간 | N |
| cntr_pric | 체결가 | N |
| pred_pre_sig | 전일대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| cntr_qty | 체결량 | N |
| acc_trde_qty | 누적거래량 | N |
| acc_trde_prica | 누적거래대금 | N |

#### Request Example

```json
{"stk_cd": "005930","tdy_pred": "2"}
```

---

### 투자자별일별매매종목요청 (ka10058)

#### 기본 정보

- **API ID**: `ka10058`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| trde_tp | 매매구분 | Y |
| mrkt_tp | 시장구분 | Y |
| invsr_tp | 투자자구분 | Y |
| stex_tp | 거래소구분 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| netslmt_qty | 순매도수량 | N |
| netslmt_amt | 순매도금액 | N |
| prsm_avg_pric | 추정평균가 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| avg_pric_pre | 평균가대비 | N |
| pre_rt | 대비율 | N |
| dt_trde_qty | 기간거래량 | N |

#### Request Example

```json
{"strt_dt": "20241106","end_dt": "20241107","trde_tp": "2","mrkt_tp": "101","invsr_tp": "8000","stex_tp": "3"}
```

---

### 종목별투자자기관별요청 (ka10059)

#### 기본 정보

- **API ID**: `ka10059`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dt | 일자 | Y |
| stk_cd | 종목코드 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| trde_tp | 매매구분 | Y |
| unit_tp | 단위구분 | Y |
| dt | 일자 | N |
| cur_prc | 현재가 | N |
| pre_sig | 대비기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| acc_trde_qty | 누적거래량 | N |
| acc_trde_prica | 누적거래대금 | N |
| ind_invsr | 개인투자자 | N |
| frgnr_invsr | 외국인투자자 | N |
| orgn | 기관계 | N |
| fnnc_invt | 금융투자 | N |
| insrnc | 보험 | N |
| invtrt | 투신 | N |
| etc_fnnc | 기타금융 | N |
| bank | 은행 | N |
| penfnd_etc | 연기금등 | N |
| samo_fund | 사모펀드 | N |
| natn | 국가 | N |
| etc_corp | 기타법인 | N |
| natfor | 내외국인 | N |

#### Request Example

```json
{"dt": "20241107","stk_cd": "005930","amt_qty_tp": "1","trde_tp": "0","unit_tp": "1000"}
```

---

### 종목별투자자기관별합계요청 (ka10061)

#### 기본 정보

- **API ID**: `ka10061`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| strt_dt | 시작일자 | Y |
| end_dt | 종료일자 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| trde_tp | 매매구분 | Y |
| unit_tp | 단위구분 | Y |
| ind_invsr | 개인투자자 | N |
| frgnr_invsr | 외국인투자자 | N |
| orgn | 기관계 | N |
| fnnc_invt | 금융투자 | N |
| insrnc | 보험 | N |
| invtrt | 투신 | N |
| etc_fnnc | 기타금융 | N |
| bank | 은행 | N |
| penfnd_etc | 연기금등 | N |
| samo_fund | 사모펀드 | N |
| natn | 국가 | N |
| etc_corp | 기타법인 | N |
| natfor | 내외국인 | N |

#### Request Example

```json
{"stk_cd": "005930","strt_dt": "20241007","end_dt": "20241107","amt_qty_tp": "1","trde_tp": "0","unit_tp": "1000"}
```

---

### 당일전일체결요청 (ka10084)

#### 기본 정보

- **API ID**: `ka10084`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tdy_pred | 당일전일 | Y |
| tic_min | 틱분 | Y |
| tm | 시간 | N |
| tm | 시간 | N |
| cur_prc | 현재가 | N |
| pred_pre | 전일대비 | N |
| pre_rt | 대비율 | N |
| pri_sel_bid_unit | 우선매도호가단위 | N |
| pri_buy_bid_unit | 우선매수호가단위 | N |
| cntr_trde_qty | 체결거래량 | N |
| sign | 전일대비기호 | N |
| acc_trde_qty | 누적거래량 | N |
| acc_trde_prica | 누적거래대금 | N |
| cntr_str | 체결강도 | N |
| stex_tp | 거래소구분 | N |

#### Request Example

```json
{"stk_cd": "005930","tdy_pred": "1","tic_min": "0","tm": ""}
```

---

### 관심종목정보요청 (ka10095)

#### 기본 정보

- **API ID**: `ka10095`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| base_pric | 기준가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비기호 | N |
| flu_rt | 등락율 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| cntr_qty | 체결량 | N |
| cntr_str | 체결강도 | N |
| pred_trde_qty_pre | 전일거래량대비 | N |
| sel_bid | 매도호가 | N |
| buy_bid | 매수호가 | N |
| sel_1th_bid | 매도1차호가 | N |
| sel_2th_bid | 매도2차호가 | N |
| sel_3th_bid | 매도3차호가 | N |
| sel_4th_bid | 매도4차호가 | N |
| sel_5th_bid | 매도5차호가 | N |
| buy_1th_bid | 매수1차호가 | N |
| buy_2th_bid | 매수2차호가 | N |
| buy_3th_bid | 매수3차호가 | N |
| buy_4th_bid | 매수4차호가 | N |
| buy_5th_bid | 매수5차호가 | N |
| upl_pric | 상한가 | N |
| lst_pric | 하한가 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| close_pric | 종가 | N |
| cntr_tm | 체결시간 | N |
| exp_cntr_pric | 예상체결가 | N |
| exp_cntr_qty | 예상체결량 | N |
| cap | 자본금 | N |
| fav | 액면가 | N |
| mac | 시가총액 | N |
| stkcnt | 주식수 | N |
| bid_tm | 호가시간 | N |
| dt | 일자 | N |
| pri_sel_req | 우선매도잔량 | N |
| pri_buy_req | 우선매수잔량 | N |
| pri_sel_cnt | 우선매도건수 | N |
| pri_buy_cnt | 우선매수건수 | N |
| tot_sel_req | 총매도잔량 | N |
| tot_buy_req | 총매수잔량 | N |
| tot_sel_cnt | 총매도건수 | N |
| tot_buy_cnt | 총매수건수 | N |
| prty | 패리티 | N |
| gear | 기어링 | N |
| pl_qutr | 손익분기 | N |
| cap_support | 자본지지 | N |
| elwexec_pric | ELW행사가 | N |
| cnvt_rt | 전환비율 | N |
| elwexpr_dt | ELW만기일 | N |
| cntr_engg | 미결제약정 | N |
| cntr_pred_pre | 미결제전일대비 | N |
| theory_pric | 이론가 | N |
| innr_vltl | 내재변동성 | N |
| delta | 델타 | N |
| gam | 감마 | N |
| theta | 쎄타 | N |
| vega | 베가 | N |
| law | 로 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 종목정보 리스트 (ka10099)

#### 기본 정보

- **API ID**: `ka10099`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| code | 종목코드 | N |
| name | 종목명 | N |
| listCount | 상장주식수 | N |
| auditInfo | 감리구분 | N |
| regDay | 상장일 | N |
| lastPrice | 전일종가 | N |
| state | 종목상태 | N |
| marketCode | 시장구분코드 | N |
| marketName | 시장명 | N |
| upName | 업종명 | N |
| upSizeName | 회사크기분류 | N |
| orderWarning | 투자유의종목여부 | N |
| nxtEnable | NXT가능여부 | N |

#### Request Example

```json
{"mrkt_tp": "0"}
```

---

### 종목정보 조회 (ka10100)

#### 기본 정보

- **API ID**: `ka10100`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| code | 종목코드 | N |
| name | 종목명 | N |
| listCount | 상장주식수 | N |
| auditInfo | 감리구분 | N |
| regDay | 상장일 | N |
| lastPrice | 전일종가 | N |
| state | 종목상태 | N |
| marketCode | 시장구분코드 | N |
| marketName | 시장명 | N |
| upName | 업종명 | N |
| upSizeName | 회사크기분류 | N |
| companyClassName | 회사분류 | N |
| orderWarning | 투자유의종목여부 | N |
| nxtEnable | NXT가능여부 | N |

#### Request Example

```json
{"stk_cd": "005930"}
```

---

### 업종코드 리스트 (ka10101)

#### 기본 정보

- **API ID**: `ka10101`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| code | 코드 | N |
| name | 업종명 | N |
| group | 그룹 | N |

#### Request Example

```json
{"mrkt_tp": "0"}
```

---

### 회원사 리스트 (ka10102)

#### 기본 정보

- **API ID**: `ka10102`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| code | 코드 | N |
| name | 업종명 | N |
| gb | 구분 | N |

#### Request Example

```json
{}```Response Example```### {"return_msg": "정상적으로 처리되었습니다","list": [{"code": "001","name": "교 보","gb": "0"}
```

---

### 프로그램순매수상위50요청 (ka90003)

#### 기본 정보

- **API ID**: `ka90003`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| trde_upper_tp | 매매상위구분 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| mrkt_tp | 시장구분 | Y |
| stex_tp | 거래소구분 | Y |
| rank | 순위 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| flu_sig | 등락기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| acc_trde_qty | 누적거래량 | N |
| prm_sell_amt | 프로그램매도금액 | N |
| prm_buy_amt | 프로그램매수금액 | N |
| prm_netprps_amt | 프로그램순매수금액 | N |

#### Request Example

```json
{"trde_upper_tp": "1","amt_qty_tp": "1","mrkt_tp": "P00101","stex_tp": "1"}
```

---

### 종목별프로그램매매현황요청 (ka90004)

#### 기본 정보

- **API ID**: `ka90004`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dt | 일자 | Y |
| mrkt_tp | 시장구분 | Y |
| stex_tp | 거래소구분 | Y |
| tot_1 | 매수체결수량합계 | N |
| tot_2 | 매수체결금액합계 | N |
| tot_3 | 매도체결수량합계 | N |
| tot_4 | 매도체결금액합계 | N |
| tot_5 | 순매수대금합계 | N |
| tot_6 | 합계6 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| flu_sig | 등락기호 | N |
| pred_pre | 전일대비 | N |
| buy_cntr_qty | 매수체결수량 | N |
| buy_cntr_amt | 매수체결금액 | N |
| sel_cntr_qty | 매도체결수량 | N |
| sel_cntr_amt | 매도체결금액 | N |
| netprps_prica | 순매수대금 | N |
| all_trde_rt | 전체거래비율 | N |

#### Request Example

```json
{"dt": "20241125","mrkt_tp": "P00101","stex_tp": "1"}
```

---

### 신용융자 가능종목요청 (kt20016)

#### 기본 정보

- **API ID**: `kt20016`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| crd_stk_grde_tp | 신용종목등급구분 | N |
| mrkt_deal_tp | 시장거래구분 | Y |
| stk_cd | 종목코드 | N |
| crd_loan_able | 신용융자가능여부 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| crd_assr_rt | 신용보증금율 | N |
| repl_pric | 대용가 | N |
| pred_close_pric | 전일종가 | N |
| crd_limit_over_yn | 신용한도초과여부 | N |
| crd_limit_over_txt | 신용한도초과 | N |

#### Request Example

```json
{"crd_stk_grde_tp": "A","mrkt_deal_tp": "%","stk_cd": "039490"}
```

---

### 신용융자 가능문의 (kt20017)

#### 기본 정보

- **API ID**: `kt20017`
- **Method**: `POST`
- **URL**: `/api/dostk/stkinfo`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| crd_alow_yn | 신용가능여부 | N |

#### Request Example

```json
{"stk_cd": "039490"}
```

---

## 주문

### 주식 매수주문 (kt10000)

#### 기본 정보

- **API ID**: `kt10000`
- **Method**: `POST`
- **URL**: `/api/dostk/ordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| stk_cd | 종목코드 | Y |
| ord_qty | 주문수량 | Y |
| ord_uv | 주문단가 | N |
| trde_tp | 매매구분 | Y |
| cond_uv | 조건단가 | N |
| ord_no | 주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | N |

#### Request Example

```json
{```Request Example```"dmst_stex_tp": "KRX","stk_cd": "005930","ord_qty": "1","ord_uv": "","trde_tp": "3","cond_uv": ""}
```

---

### 주식 매도주문 (kt10001)

#### 기본 정보

- **API ID**: `kt10001`
- **Method**: `POST`
- **URL**: `/api/dostk/ordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| stk_cd | 종목코드 | Y |
| ord_qty | 주문수량 | Y |
| ord_uv | 주문단가 | N |
| trde_tp | 매매구분 | Y |
| cond_uv | 조건단가 | N |
| ord_no | 주문번호 | N |
| dmst_stex_tp | 국내거래소구분 | N |

#### Request Example

```json
{```Request Example```"dmst_stex_tp": "KRX","stk_cd": "005930","ord_qty": "1","ord_uv": "","trde_tp": "3","cond_uv": ""}
```

---

### 주식 정정주문 (kt10002)

#### 기본 정보

- **API ID**: `kt10002`
- **Method**: `POST`
- **URL**: `/api/dostk/ordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| orig_ord_no | 원주문번호 | Y |
| stk_cd | 종목코드 | Y |
| mdfy_qty | 정정수량 | Y |
| mdfy_uv | 정정단가 | Y |
| mdfy_cond_uv | 정정조건단가 | N |
| ord_no | 주문번호 | N |
| base_orig_ord_no | 모주문번호 | N |
| mdfy_qty | 정정수량 | N |
| dmst_stex_tp | 국내거래소구분 | N |

#### Request Example

```json
{"dmst_stex_tp": "KRX",```Request Example```"orig_ord_no": "0000139","stk_cd": "005930","mdfy_qty": "1","mdfy_uv": "199700","mdfy_cond_uv": ""}
```

---

### 주식 취소주문 (kt10003)

#### 기본 정보

- **API ID**: `kt10003`
- **Method**: `POST`
- **URL**: `/api/dostk/ordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dmst_stex_tp | 국내거래소구분 | Y |
| orig_ord_no | 원주문번호 | Y |
| stk_cd | 종목코드 | Y |
| cncl_qty | 취소수량 | Y |
| ord_no | 주문번호 | N |
| base_orig_ord_no | 모주문번호 | N |
| cncl_qty | 취소수량 | N |

#### Request Example

```json
{"dmst_stex_tp": "KRX","orig_ord_no": "0000140","stk_cd": "005930","cncl_qty": "1"}
```

---

### 금현물 매수주문 (kt50000)

#### 기본 정보

- **API ID**: `kt50000`
- **Method**: `POST`
- **URL**: `/api/dostk/ordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| ord_qty | 주문수량 | Y |
| ord_uv | 주문단가 | N |
| trde_tp | 매매구분 | Y |
| ord_no | 주문번호 | N |

#### Request Example

```json
{"stk_cd": "M04020000","ord_qty": "1","ord_uv": "160000","trde_tp": "00"}
```

---

### 금현물 매도주문 (kt50001)

#### 기본 정보

- **API ID**: `kt50001`
- **Method**: `POST`
- **URL**: `/api/dostk/ordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| ord_qty | 주문수량 | Y |
| ord_uv | 주문단가 | N |
| trde_tp | 매매구분 | Y |
| ord_no | 주문번호 | N |

#### Request Example

```json
{"stk_cd": "M04020000","ord_qty": "1","ord_uv": "160000","trde_tp": "00"}
```

---

### 금현물 정정주문 (kt50002)

#### 기본 정보

- **API ID**: `kt50002`
- **Method**: `POST`
- **URL**: `/api/dostk/ordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| orig_ord_no | 원주문번호 | Y |
| mdfy_qty | 정정수량 | Y |
| mdfy_uv | 정정단가 | Y |
| ord_no | 주문번호 | N |
| base_orig_ord_no | 모주문번호 | N |
| mdfy_qty | 정정수량 | N |

#### Request Example

```json
{"stk_cd": "M04020000","orig_ord_no": "0000012","mdfy_qty": "1","mdfy_uv": "150000"}
```

---

### 금현물 취소주문 (kt50003)

#### 기본 정보

- **API ID**: `kt50003`
- **Method**: `POST`
- **URL**: `/api/dostk/ordr`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| orig_ord_no | 원주문번호 | Y |
| stk_cd | 종목코드 | Y |
| cncl_qty | 취소수량 | Y |
| ord_no | 주문번호 | N |
| base_orig_ord_no | 모주문번호 | N |
| cncl_qty | 취소수량 | N |

#### Request Example

```json
{"orig_ord_no": "0000014","stk_cd": "M04020000","cncl_qty": "1"}
```

---

## 차트

### 종목별투자자기관별차트요청 (ka10060)

#### 기본 정보

- **API ID**: `ka10060`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| dt | 일자 | Y |
| stk_cd | 종목코드 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| trde_tp | 매매구분 | Y |
| unit_tp | 단위구분 | Y |
| dt | 일자 | N |
| cur_prc | 현재가 | N |
| pred_pre | 전일대비 | N |
| acc_trde_prica | 누적거래대금 | N |
| ind_invsr | 개인투자자 | N |
| frgnr_invsr | 외국인투자자 | N |
| orgn | 기관계 | N |
| fnnc_invt | 금융투자 | N |
| insrnc | 보험 | N |
| invtrt | 투신 | N |
| etc_fnnc | 기타금융 | N |
| bank | 은행 | N |
| penfnd_etc | 연기금등 | N |
| samo_fund | 사모펀드 | N |
| natn | 국가 | N |
| etc_corp | 기타법인 | N |
| natfor | 내외국인 | N |

#### Request Example

```json
{"dt": "20241107","stk_cd": "005930","amt_qty_tp": "1","trde_tp": "0","unit_tp": "1000"}
```

---

### 장중투자자별매매차트요청 (ka10064)

#### 기본 정보

- **API ID**: `ka10064`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| mrkt_tp | 시장구분 | Y |
| amt_qty_tp | 금액수량구분 | Y |
| trde_tp | 매매구분 | Y |
| stk_cd | 종목코드 | Y |
| tm | 시간 | N |
| frgnr_invsr | 외국인투자자 | N |
| orgn | 기관계 | N |
| invtrt | 투신 | N |
| insrnc | 보험 | N |
| bank | 은행 | N |
| penfnd_etc | 연기금등 | N |
| etc_corp | 기타법인 | N |
| natn | 국가 | N |

#### Request Example

```json
{"mrkt_tp": "000","amt_qty_tp": "1","trde_tp": "0","stk_cd": "005930"}
```

---

### 주식틱차트조회요청 (ka10079)

#### 기본 정보

- **API ID**: `ka10079`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tic_scope | 틱범위 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| stk_cd | 종목코드 | N |
| last_tic_cnt | 마지막틱갯수 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| cntr_tm | 체결시간 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비 기호 | N |

#### Request Example

```json
{"stk_cd": "005930","tic_scope": "1","upd_stkpc_tp": "1"}
```

---

### 주식분봉차트조회요청 (ka10080)

#### 기본 정보

- **API ID**: `ka10080`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tic_scope | 틱범위 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| stk_cd | 종목코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| cntr_tm | 체결시간 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비 기호 | N |

#### Request Example

```json
{"stk_cd": "005930","tic_scope": "1","upd_stkpc_tp": "1"}
```

---

### 주식일봉차트조회요청 (ka10081)

#### 기본 정보

- **API ID**: `ka10081`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| base_dt | 기준일자 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| stk_cd | 종목코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| dt | 일자 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비기호 | N |
| trde_tern_rt | 거래회전율 | N |

#### Request Example

```json
{"stk_cd": "005930","base_dt": "20250908","upd_stkpc_tp": "1"}
```

---

### 주식주봉차트조회요청 (ka10082)

#### 기본 정보

- **API ID**: `ka10082`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| base_dt | 기준일자 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| stk_cd | 종목코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| dt | 일자 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비기호 | N |
| trde_tern_rt | 거래회전율 | N |

#### Request Example

```json
{"stk_cd": "005930","base_dt": "20250905","upd_stkpc_tp": "1"}
```

---

### 주식월봉차트조회요청 (ka10083)

#### 기본 정보

- **API ID**: `ka10083`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| base_dt | 기준일자 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| stk_cd | 종목코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| dt | 일자 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비기호 | N |
| trde_tern_rt | 거래회전율 | N |

#### Request Example

```json
{"stk_cd": "005930","base_dt": "20250905","upd_stkpc_tp": "1"}
```

---

### 주식년봉차트조회요청 (ka10094)

#### 기본 정보

- **API ID**: `ka10094`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| base_dt | 기준일자 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| stk_cd | 종목코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| trde_prica | 거래대금 | N |
| dt | 일자 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |

#### Request Example

```json
{"stk_cd": "005930","base_dt": "20250905","upd_stkpc_tp": "1"}
```

---

### 업종틱차트조회요청 (ka20004)

#### 기본 정보

- **API ID**: `ka20004`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| inds_cd | 업종코드 | Y |
| tic_scope | 틱범위 | Y |
| inds_cd | 업종코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| cntr_tm | 체결시간 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비 기호 | N |

#### Request Example

```json
{"inds_cd": "001","tic_scope": "1"}
```

---

### 업종분봉조회요청 (ka20005)

#### 기본 정보

- **API ID**: `ka20005`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| inds_cd | 업종코드 | Y |
| tic_scope | 틱범위 | Y |
| inds_cd | 업종코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| cntr_tm | 체결시간 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| acc_trde_qty | 누적거래량 | N |
| pred_pre | 전일대비 | N |
| pred_pre_sig | 전일대비 기호 | N |

#### Request Example

```json
{"inds_cd": "001","tic_scope": "5"}
```

---

### 업종일봉조회요청 (ka20006)

#### 기본 정보

- **API ID**: `ka20006`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| inds_cd | 업종코드 | Y |
| base_dt | 기준일자 | Y |
| inds_cd | 업종코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| dt | 일자 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| trde_prica | 거래대금 | N |

#### Request Example

```json
{"inds_cd": "001","base_dt": "20250905"}
```

---

### 업종주봉조회요청 (ka20007)

#### 기본 정보

- **API ID**: `ka20007`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| inds_cd | 업종코드 | Y |
| base_dt | 기준일자 | Y |
| inds_cd | 업종코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| dt | 일자 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| trde_prica | 거래대금 | N |

#### Request Example

```json
{"inds_cd": "001","base_dt": "20250905"}
```

---

### 업종월봉조회요청 (ka20008)

#### 기본 정보

- **API ID**: `ka20008`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| inds_cd | 업종코드 | Y |
| base_dt | 기준일자 | Y |
| inds_cd | 업종코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| dt | 일자 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| trde_prica | 거래대금 | N |

#### Request Example

```json
{"inds_cd": "002","base_dt": "20250905"}
```

---

### 업종년봉조회요청 (ka20019)

#### 기본 정보

- **API ID**: `ka20019`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| inds_cd | 업종코드 | Y |
| base_dt | 기준일자 | Y |
| inds_cd | 업종코드 | N |
| cur_prc | 현재가 | N |
| trde_qty | 거래량 | N |
| dt | 일자 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| trde_prica | 거래대금 | N |

#### Request Example

```json
{"inds_cd": "001","base_dt": "20250905"}
```

---

### 금현물틱차트조회요청 (ka50079)

#### 기본 정보

- **API ID**: `ka50079`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tic_scope | 틱범위 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| cur_prc | 현재가 | N |
| pred_pre | 전일대비 | N |
| trde_qty | 거래량 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| cntr_tm | 체결시간 | N |
| dt | 일자 | N |
| pred_pre_sig | 전일대비기호 | N |

#### Request Example

```json
{"stk_cd": "M04020000","tic_scope": "","upd_stkpc_tp": "1"}
```

---

### 금현물분봉차트조회요청 (ka50080)

#### 기본 정보

- **API ID**: `ka50080`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tic_scope | 틱범위 | Y |
| upd_stkpc_tp | 수정주가구분 | N |
| cur_prc | 현재가 | N |
| pred_pre | 전일대비 | N |
| acc_trde_qty | 누적거래량 | N |
| trde_qty | 거래량 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| cntr_tm | 체결시간 | N |
| dt | 일자 | N |
| pred_pre_sig | 전일대비기호 | N |

#### Request Example

```json
{"stk_cd": "M04020000","tic_scope": "","upd_stkpc_tp": "1"}
```

---

### 금현물일봉차트조회요청 (ka50081)

#### 기본 정보

- **API ID**: `ka50081`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| base_dt | 기준일자 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| cur_prc | 현재가 | N |
| acc_trde_qty | 누적 거래량 | N |
| acc_trde_prica | 누적 거래대금 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| dt | 일자 | N |
| pred_pre_sig | 전일대비기호 | N |

#### Request Example

```json
{"stk_cd": "M04020000","base_dt": "20250826","upd_stkpc_tp": "1"}
```

---

### 금현물주봉차트조회요청 (ka50082)

#### 기본 정보

- **API ID**: `ka50082`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| base_dt | 기준일자 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| cur_prc | 현재가 | N |
| acc_trde_qty | 누적 거래량 | N |
| acc_trde_prica | 누적 거래대금 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| dt | 일자 | N |

#### Request Example

```json
{"stk_cd": "M04020000","base_dt": "20250826","upd_stkpc_tp": "1"}
```

---

### 금현물월봉차트조회요청 (ka50083)

#### 기본 정보

- **API ID**: `ka50083`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| base_dt | 기준일자 | Y |
| upd_stkpc_tp | 수정주가구분 | Y |
| cur_prc | 현재가 | N |
| acc_trde_qty | 누적 거래량 | N |
| acc_trde_prica | 누적 거래대금 | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| dt | 일자 | N |

#### Request Example

```json
{"stk_cd": "M04020000","base_dt": "20250826","upd_stkpc_tp": "1"}
```

---

### 금현물당일틱차트조회요청 (ka50091)

#### 기본 정보

- **API ID**: `ka50091`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tic_scope | 틱범위 | Y |
| cntr_pric | 체결가 | N |
| pred_pre | 전일 대비(원) | N |
| trde_qty | 거래량(체결량) | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| cntr_tm | 체결시간 | N |
| dt | 일자 | N |
| pred_pre_sig | 전일대비기호 | N |

#### Request Example

```json
{"stk_cd": "M04020000","tic_scope": "1"}
```

---

### 금현물당일분봉차트조회요청 (ka50092)

#### 기본 정보

- **API ID**: `ka50092`
- **Method**: `POST`
- **URL**: `/api/dostk/chart`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| stk_cd | 종목코드 | Y |
| tic_scope | 틱범위 | Y |
| cntr_pric | 체결가 | N |
| pred_pre | 전일 대비(원) | N |
| acc_trde_qty | 누적 거래량 | N |
| acc_trde_prica | 누적 거래대금 | N |
| trde_qty | 거래량(체결량) | N |
| open_pric | 시가 | N |
| high_pric | 고가 | N |
| low_pric | 저가 | N |
| cntr_tm | 체결시간 | N |
| dt | 일자 | N |
| pred_pre_sig | 전일대비기호 | N |

#### Request Example

```json
{"stk_cd": "M04020000","tic_scope": "1"}
```

---

## 테마

### 테마그룹별요청 (ka90001)

#### 기본 정보

- **API ID**: `ka90001`
- **Method**: `POST`
- **URL**: `/api/dostk/thme`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| qry_tp | 검색구분 | Y |
| stk_cd | 종목코드 | N |
| date_tp | 날짜구분 | Y |
| thema_nm | 테마명 | N |
| flu_pl_amt_tp | 등락수익구분 | Y |
| stex_tp | 거래소구분 | Y |
| thema_grp_cd | 테마그룹코드 | N |
| thema_nm | 테마명 | N |
| stk_num | 종목수 | N |
| flu_sig | 등락기호 | N |
| flu_rt | 등락율 | N |
| rising_stk_num | 상승종목수 | N |
| fall_stk_num | 하락종목수 | N |
| dt_prft_rt | 기간수익률 | N |
| main_stk | 주요종목 | N |

#### Request Example

```json
{"qry_tp": "0","stk_cd": "","date_tp": "10","thema_nm": "","flu_pl_amt_tp": "1","stex_tp": "1"}
```

---

### 테마구성종목요청 (ka90002)

#### 기본 정보

- **API ID**: `ka90002`
- **Method**: `POST`
- **URL**: `/api/dostk/thme`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| date_tp | 날짜구분 | N |
| thema_grp_cd | 테마그룹코드 | Y |
| stex_tp | 거래소구분 | Y |
| flu_rt | 등락률 | N |
| dt_prft_rt | 기간수익률 | N |
| stk_cd | 종목코드 | N |
| stk_nm | 종목명 | N |
| cur_prc | 현재가 | N |
| flu_sig | 등락기호 | N |
| pred_pre | 전일대비 | N |
| flu_rt | 등락율 | N |
| acc_trde_qty | 누적거래량 | N |
| sel_bid | 매도호가 | N |
| sel_req | 매도잔량 | N |
| buy_bid | 매수호가 | N |
| buy_req | 매수잔량 | N |
| dt_prft_rt_n | 기간수익률n | N |

#### Request Example

```json
{"date_tp": "2","thema_grp_cd": "100","stex_tp": "1"}
```

---
