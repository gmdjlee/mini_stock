# KIS (한국투자증권) 재무정보 API 명세서

**Version**: 1.0
**Created**: 2026-01-27
**Data Source**: 한국투자증권 REST API
**Base URL**: `https://openapi.koreainvestment.com:9443` (실전) / `https://openapivts.koreainvestment.com:29443` (모의)

---

## 1. API 개요

### 1.1 재무정보 API 목록

| # | API명 | Endpoint | tr_id | 설명 |
|---|-------|----------|-------|------|
| 1 | 국내주식 대차대조표 | /uapi/domestic-stock/v1/finance/balance-sheet | FHKST66430100 | 자산, 부채, 자본 정보 |
| 2 | 국내주식 손익계산서 | /uapi/domestic-stock/v1/finance/income-statement | FHKST66430200 | 매출액, 영업이익, 당기순이익 |
| 3 | 국내주식 재무비율 | /uapi/domestic-stock/v1/finance/financial-ratio | FHKST66430300 | PER, PBR, ROE 등 |
| 4 | 국내주식 수익성비율 | /uapi/domestic-stock/v1/finance/profit-ratio | FHKST66430400 | 영업이익률, 순이익률 등 |
| 5 | 국내주식 기타주요비율 | /uapi/domestic-stock/v1/finance/other-major-ratios | FHKST66430500 | 기타 주요 재무비율 |
| 6 | 국내주식 안정성비율 | /uapi/domestic-stock/v1/finance/stability-ratio | FHKST66430600 | 부채비율, 유동비율 등 |
| 7 | 국내주식 성장성비율 | /uapi/domestic-stock/v1/finance/growth-ratio | FHKST66430800 | 매출증가율, 영업이익증가율 등 |

> ⚠️ **주의**: tr_id 값은 추정치입니다. 실제 API 사용 전 KIS Developers 공식 문서에서 확인이 필요합니다.

### 1.2 공통 헤더

| 헤더명 | 필수 | 설명 | 예시 |
|--------|------|------|------|
| content-type | Y | 컨텐츠 타입 | application/json; charset=utf-8 |
| authorization | Y | 접근 토큰 | Bearer {access_token} |
| appkey | Y | 앱키 | PSxxxxxxxxxxxxxxxx |
| appsecret | Y | 앱시크릿키 | xxxxxxxx... |
| tr_id | Y | 거래ID | FHKST66430100 |
| custtype | N | 고객타입 | P (개인) |

### 1.3 공통 응답 구조

```json
{
  "rt_cd": "0",           // 성공여부 (0: 성공)
  "msg_cd": "MCA00000",   // 메시지코드
  "msg1": "정상처리 되었습니다.",
  "output": [ ... ]       // 실제 데이터 배열
}
```

---

## 2. API 상세 명세

### 2.1 국내주식 대차대조표 (Balance Sheet)

자산, 부채, 자본 등 재무상태표 정보를 조회합니다.

**기본 정보**
- Method: `GET`
- URL: `/uapi/domestic-stock/v1/finance/balance-sheet`
- tr_id: `FHKST66430100`

**Request Parameters (Query String)**

| 파라미터 | 필수 | 타입 | 설명 | 예시 |
|----------|------|------|------|------|
| FID_DIV_CLS_CODE | Y | String | 분류코드 (0: 년, 1: 분기) | "1" |
| fid_cond_mrkt_div_code | Y | String | 시장구분코드 (J: 주식) | "J" |
| fid_input_iscd | Y | String | 종목코드 (6자리) | "005930" |

**Response Fields (output)**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| stac_yymm | String | 결산년월 (YYYYMM) |
| cras | String | 유동자산 |
| fxas | String | 고정자산 |
| total_aset | String | 자산총계 |
| flow_lblt | String | 유동부채 |
| fix_lblt | String | 고정부채 |
| total_lblt | String | 부채총계 |
| cpfn | String | 자본금 |
| cfp_surp | String | 자본잉여금 |
| rere | String | 이익잉여금 |
| total_cptl | String | 자본총계 |

---

### 2.2 국내주식 손익계산서 (Income Statement)

매출액, 영업이익, 당기순이익 등 손익계산서 정보를 조회합니다.

**기본 정보**
- Method: `GET`
- URL: `/uapi/domestic-stock/v1/finance/income-statement`
- tr_id: `FHKST66430200`

**Request Parameters (Query String)**

| 파라미터 | 필수 | 타입 | 설명 | 예시 |
|----------|------|------|------|------|
| FID_DIV_CLS_CODE | Y | String | 분류코드 (0: 년, 1: 분기) | "1" |
| fid_cond_mrkt_div_code | Y | String | 시장구분코드 (J: 주식) | "J" |
| fid_input_iscd | Y | String | 종목코드 (6자리) | "005930" |

**Response Fields (output)**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| stac_yymm | String | 결산년월 (YYYYMM) |
| sale_account | String | 매출액 |
| sale_cost | String | 매출원가 |
| sale_totl_prfi | String | 매출총이익 |
| bsop_prti | String | 영업이익 |
| op_prfi | String | 경상이익 |
| spec_prfi | String | 특별이익 |
| spec_loss | String | 특별손실 |
| thtr_ntin | String | 당기순이익 |

---

### 2.3 국내주식 재무비율 (Financial Ratio)

PER, PBR, ROE 등 주요 재무비율을 조회합니다.

**기본 정보**
- Method: `GET`
- URL: `/uapi/domestic-stock/v1/finance/financial-ratio`
- tr_id: `FHKST66430300`

**Request Parameters (Query String)**

| 파라미터 | 필수 | 타입 | 설명 | 예시 |
|----------|------|------|------|------|
| FID_DIV_CLS_CODE | Y | String | 분류코드 (0: 년, 1: 분기) | "1" |
| fid_cond_mrkt_div_code | Y | String | 시장구분코드 (J: 주식) | "J" |
| fid_input_iscd | Y | String | 종목코드 (6자리) | "005930" |

**Response Fields (output)**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| stac_yymm | String | 결산년월 (YYYYMM) |
| grs | String | 매출액증가율 (%) |
| bsop_prfi_inrt | String | 영업이익증가율 (%) |
| ntin_inrt | String | 순이익증가율 (%) |
| roe_val | String | ROE (%) |
| eps | String | EPS (원) |
| sps | String | 주당매출액 |
| bps | String | BPS (원) |
| rsrv_rate | String | 유보율 (%) |
| lblt_rate | String | 부채비율 (%) |

---

### 2.4 국내주식 수익성비율 (Profit Ratio)

영업이익률, 순이익률 등 수익성 관련 비율을 조회합니다.

**기본 정보**
- Method: `GET`
- URL: `/uapi/domestic-stock/v1/finance/profit-ratio`
- tr_id: `FHKST66430400`

**Request Parameters (Query String)**

| 파라미터 | 필수 | 타입 | 설명 | 예시 |
|----------|------|------|------|------|
| FID_DIV_CLS_CODE | Y | String | 분류코드 (0: 년, 1: 분기) | "1" |
| fid_cond_mrkt_div_code | Y | String | 시장구분코드 (J: 주식) | "J" |
| fid_input_iscd | Y | String | 종목코드 (6자리) | "005930" |

**Response Fields (output)**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| stac_yymm | String | 결산년월 (YYYYMM) |
| bsop_prfi_rate | String | 영업이익률 (%) |
| ntin_rate | String | 순이익률 (%) |
| roe_val | String | 자기자본이익률 ROE (%) |
| roa_val | String | 총자산이익률 ROA (%) |
| grs | String | 매출총이익률 (%) |

---

### 2.5 국내주식 기타주요비율 (Other Major Ratios)

기타 주요 재무비율을 조회합니다.

**기본 정보**
- Method: `GET`
- URL: `/uapi/domestic-stock/v1/finance/other-major-ratios`
- tr_id: `FHKST66430500`

**Request Parameters (Query String)**

| 파라미터 | 필수 | 타입 | 설명 | 예시 |
|----------|------|------|------|------|
| FID_DIV_CLS_CODE | Y | String | 분류코드 (0: 년, 1: 분기) | "1" |
| fid_cond_mrkt_div_code | Y | String | 시장구분코드 (J: 주식) | "J" |
| fid_input_iscd | Y | String | 종목코드 (6자리) | "005930" |

**Response Fields (output)**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| stac_yymm | String | 결산년월 (YYYYMM) |
| per | String | 주가수익비율 PER |
| pbr | String | 주가순자산비율 PBR |
| pcr | String | 주가현금비율 PCR |
| psr | String | 주가매출비율 PSR |
| ev_ebitda | String | EV/EBITDA |

---

### 2.6 국내주식 안정성비율 (Stability Ratio)

부채비율, 유동비율, 차입금의존도 등 안정성 관련 비율을 조회합니다.

**기본 정보**
- Method: `GET`
- URL: `/uapi/domestic-stock/v1/finance/stability-ratio`
- tr_id: `FHKST66430600`

**Request Parameters (Query String)**

| 파라미터 | 필수 | 타입 | 설명 | 예시 |
|----------|------|------|------|------|
| FID_DIV_CLS_CODE | Y | String | 분류코드 (0: 년, 1: 분기) | "1" |
| fid_cond_mrkt_div_code | Y | String | 시장구분코드 (J: 주식) | "J" |
| fid_input_iscd | Y | String | 종목코드 (6자리) | "005930" |

**Response Fields (output)**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| stac_yymm | String | 결산년월 (YYYYMM) |
| lblt_rate | String | 부채비율 (%) |
| crnt_rate | String | 유동비율 (%) |
| quck_rate | String | 당좌비율 (%) |
| bram_depn | String | 차입금의존도 (%) |
| rsrv_rate | String | 유보율 (%) |
| inte_cvrg_rate | String | 이자보상비율 (배) |

---

### 2.7 국내주식 성장성비율 (Growth Ratio)

매출증가율, 영업이익증가율 등 성장성 관련 비율을 조회합니다.

**기본 정보**
- Method: `GET`
- URL: `/uapi/domestic-stock/v1/finance/growth-ratio`
- tr_id: `FHKST66430800`

**Request Parameters (Query String)**

| 파라미터 | 필수 | 타입 | 설명 | 예시 |
|----------|------|------|------|------|
| FID_DIV_CLS_CODE | Y | String | 분류코드 (0: 년, 1: 분기) | "1" |
| fid_cond_mrkt_div_code | Y | String | 시장구분코드 (J: 주식) | "J" |
| fid_input_iscd | Y | String | 종목코드 (6자리) | "005930" |

**Response Fields (output)**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| stac_yymm | String | 결산년월 (YYYYMM) |
| grs | String | 매출액증가율 (%) |
| bsop_prfi_inrt | String | 영업이익증가율 (%) |
| ntin_inrt | String | 순이익증가율 (%) |
| cptl_ntin_rate | String | 자기자본증가율 (%) |
| total_aset_inrt | String | 총자산증가율 (%) |

---

## 3. 데이터 병합 및 저장

### 3.1 결산년월(stac_yymm) 기준 병합

각 API에서 수집한 데이터를 `stac_yymm` (결산년월) 기준으로 병합하여 DB에 저장합니다.

```kotlin
// 병합 데이터 구조
data class MergedFinancialData(
    val ticker: String,              // 종목코드
    val settlementYearMonth: String, // 결산년월 (YYYYMM)

    // 대차대조표
    val totalAssets: Long?,          // 자산총계
    val totalLiabilities: Long?,     // 부채총계
    val totalEquity: Long?,          // 자본총계
    val currentAssets: Long?,        // 유동자산
    val currentLiabilities: Long?,   // 유동부채

    // 손익계산서
    val revenue: Long?,              // 매출액
    val operatingProfit: Long?,      // 영업이익
    val netIncome: Long?,            // 당기순이익

    // 수익성비율
    val operatingMargin: Double?,    // 영업이익률
    val netMargin: Double?,          // 순이익률
    val roe: Double?,                // ROE
    val roa: Double?,                // ROA

    // 안정성비율
    val debtRatio: Double?,          // 부채비율
    val currentRatio: Double?,       // 유동비율
    val borrowingDependency: Double?, // 차입금의존도

    // 성장성비율
    val revenueGrowth: Double?,      // 매출액증가율
    val operatingProfitGrowth: Double?, // 영업이익증가율
    val netIncomeGrowth: Double?,    // 순이익증가율
    val equityGrowth: Double?,       // 자기자본증가율
    val totalAssetsGrowth: Double?   // 총자산증가율
)
```

### 3.2 Room Entity

```kotlin
@Entity(tableName = "financial_data")
data class FinancialDataEntity(
    @PrimaryKey
    val id: String,                  // ticker_stac_yymm (예: 005930_202312)
    val ticker: String,
    val settlementYearMonth: String,
    val data: String,                // JSON serialized MergedFinancialData
    val cachedAt: Long = System.currentTimeMillis()
)
```

---

## 4. 에러 코드

| 코드 | 메시지 | 설명 |
|------|--------|------|
| 0 | 정상처리 되었습니다. | 성공 |
| -1 | 실패 | 일반 오류 |
| -10 | 주식 종목코드가 유효하지 않습니다. | 잘못된 종목코드 |
| -20 | 데이터가 없습니다. | 조회 결과 없음 |

---

## 5. 사용 예시

### 5.1 대차대조표 조회 예시

**Request**
```
GET /uapi/domestic-stock/v1/finance/balance-sheet?FID_DIV_CLS_CODE=1&fid_cond_mrkt_div_code=J&fid_input_iscd=005930

Headers:
  authorization: Bearer {access_token}
  appkey: {appkey}
  appsecret: {appsecret}
  tr_id: FHKST66430100
```

**Response**
```json
{
  "rt_cd": "0",
  "msg_cd": "MCA00000",
  "msg1": "정상처리 되었습니다.",
  "output": [
    {
      "stac_yymm": "202312",
      "cras": "214837590000000",
      "fxas": "271893450000000",
      "total_aset": "486731040000000",
      "flow_lblt": "89234560000000",
      "fix_lblt": "45678900000000",
      "total_lblt": "134913460000000",
      "cpfn": "8975400000000",
      "total_cptl": "351817580000000"
    },
    {
      "stac_yymm": "202309",
      "cras": "208456780000000",
      ...
    }
  ]
}
```

---

## 6. 참고 사항

### 6.1 API 호출 제한

| 항목 | 제한 |
|------|------|
| 초당 요청 제한 | 20건/초 |
| 일일 요청 제한 | 20,000건/일 (개인) |

### 6.2 관련 링크

- KIS Developers 포털: https://apiportal.koreainvestment.com/
- API 문서 (국내주식 종목정보): https://apiportal.koreainvestment.com/apiservice/apiservice-domestic-stock-info

---

## 7. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-01-27 | 최초 작성 (초안) |

> **Note**: 이 문서는 KIS API의 일반적인 패턴을 기반으로 작성된 초안입니다.
> 실제 API 구현 전 KIS Developers 공식 문서에서 정확한 tr_id, 파라미터명, 응답 필드명을 확인하시기 바랍니다.
