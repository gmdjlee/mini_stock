# 순위정보(Ranking) 기능 이식 명세서 — 핵심 로직

> **목적**: 이 명세서만으로 순위정보 기능을 다른 Android/Kotlin 프로젝트에 완전하게 구현할 수 있도록 한다.
> **UI 명세**: `RANKING_FEATURE_UI_SPEC.md` 참조

---

## 1. 아키텍처 개요

### 1.1 레이어 구조

```
feature/ranking/
├── domain/                    # 순수 Kotlin (외부 의존성 없음)
│   ├── model/
│   │   ├── RankingModels.kt   # 8 enums + 2 data classes
│   │   └── RankingParams.kt   # 5 API 요청 파라미터 classes
│   ├── repo/
│   │   └── RankingRepo.kt     # Repository 인터페이스
│   └── usecase/
│       └── GetRankingUC.kt    # 비즈니스 로직
├── data/                      # API 통신 + 파싱
│   ├── dto/
│   │   └── RankingDto.kt      # 7 Serializable DTO classes
│   └── repo/
│       ├── RankingRepoImpl.kt          # Repository 구현체
│       ├── RankingParsers.kt           # 5개 유형별 파서
│       ├── RankingParseUtils.kt        # 문자열→숫자 변환 유틸
│       └── ForeignInstitutionExtractor.kt  # ka90009 전용 파서
├── ui/                        # Jetpack Compose UI
│   ├── RankingVm.kt           # ViewModel (12 StateFlow)
│   └── RankingScreen.kt       # 8개 Composable 함수
└── di/
    └── RankingModule.kt       # Hilt DI 바인딩
```

### 1.2 데이터 흐름

```
[UI] RankingScreen
  ↓ 필터 변경 이벤트
[VM] RankingVm
  ↓ rankingType, marketType, exchangeType, etc.
[UC] GetRankingUC
  ↓ RankingType → 적절한 repo 메서드 선택
[Repo Interface] RankingRepo
  ↓
[Repo Impl] RankingRepoImpl
  ↓ params.toRequestBody() → HTTP POST
[API Client] KiwoomApiClient.call()
  ↓ JSON 응답
[Parser] RankingParsers / ForeignInstitutionExtractor
  ↓ DTO → Domain Model 변환
[Result] Result<RankingResult>
  ↑ ViewModel → UI 상태 업데이트
```

### 1.3 캐싱 전략

- **DB 캐싱 없음**: 순위 데이터는 실시간성이 중요하여 영구 저장하지 않음
- **ViewModel 내 메모리 캐싱**: `_fullResult`에 전체 결과 보관 → ETF 제외/개수 필터는 API 재호출 없이 로컬 적용

---

## 2. 외부 의존성 계약

대상 프로젝트에서 아래 인터페이스들을 구현해야 한다.

### 2.1 API 클라이언트

```kotlin
/**
 * Kiwoom REST API 호출 클라이언트.
 * - 카테고리별 rate limiting (RANKING: 500ms 간격)
 * - 401/403 시 토큰 자동 갱신 후 1회 재시도
 */
interface ApiClient {
    suspend fun <T> call(
        apiId: String,              // API 식별자 (e.g., "ka10021")
        url: String,                // 엔드포인트 (e.g., "/api/dostk/rkinfo")
        body: Map<String, String>,  // 요청 바디
        appKey: String,
        secretKey: String,
        baseUrl: String,            // "https://mockapi.kiwoom.com" 또는 "https://api.kiwoom.com"
        parser: (String) -> T       // JSON 응답 문자열 파서
    ): Result<T>
}
```

### 2.2 설정 저장소

```kotlin
interface SettingsProvider {
    fun getApiKeyConfig(): Flow<ApiKeyConfig>
}

data class ApiKeyConfig(
    val appKey: String = "",
    val secretKey: String = "",
    val investmentMode: InvestmentMode = InvestmentMode.MOCK
) {
    fun isValid(): Boolean = appKey.isNotBlank() && secretKey.isNotBlank()
}

enum class InvestmentMode(val displayName: String, val description: String) {
    MOCK("모의투자", "테스트용 모의투자 환경"),
    PRODUCTION("실전투자", "실제 거래가 이루어지는 환경")
}
```

### 2.3 종목 선택 관리자

```kotlin
/**
 * 앱 전역 종목 선택 상태 (Singleton).
 * 순위 목록에서 종목 클릭 시 호출 → 다른 화면에서 해당 종목 분석 표시.
 */
interface StockSelector {
    fun selectTicker(ticker: String, name: String)
}
```

### 2.4 에러 모델

```kotlin
sealed class ApiError(override val message: String) : Exception(message) {
    class AuthError(msg: String) : ApiError(msg)
    class NetworkError(msg: String) : ApiError(msg)
    class RateLimitError(msg: String) : ApiError(msg)
    class ApiCallError(val code: Int, msg: String) : ApiError("[$code] $msg")
    class ParseError(msg: String) : ApiError(msg)
    class TimeoutError(msg: String) : ApiError(msg)
    class NoApiKeyError(
        msg: String = "API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요."
    ) : ApiError(msg)
}
```

---

## 3. 도메인 모델

### 3.1 Enums

```kotlin
enum class MarketType(val code: String, val displayName: String) {
    ALL("000", "전체"),
    KOSPI("001", "KOSPI"),
    KOSDAQ("101", "KOSDAQ")
}

enum class ExchangeType(val code: String, val displayName: String) {
    KRX("1", "KRX"),           // 정규 거래소 (실전투자)
    NXT("2", "NXT"),           // 대체 거래소 (실전투자)
    KRX_MOCK("3", "KRX")      // KRX 모의투자용 (stex_tp: 3)
}

enum class RankingType(val displayName: String, val apiId: String) {
    ORDER_BOOK_SURGE("호가잔량급증", "ka10021"),
    VOLUME_SURGE("거래량급증", "ka10023"),
    DAILY_VOLUME_TOP("당일거래량상위", "ka10030"),
    CREDIT_RATIO_TOP("신용비율상위", "ka10033"),
    FOREIGN_INSTITUTION_TOP("외국인/기관상위", "ka90009")
}

enum class OrderBookDirection(val code: String, val displayName: String) {
    BUY("1", "매수"),
    SELL("2", "매도")
}

enum class ItemCount(val value: Int) {
    FIVE(5), TEN(10), TWENTY(20), THIRTY(30)
}

enum class InvestorType(val displayName: String) {
    FOREIGN("외국인"),
    INSTITUTION("기관"),
    ALL("전체")
}

enum class TradeDirection(val displayName: String) {
    NET_BUY("순매수"),
    NET_SELL("순매도")
}

enum class ValueType(val code: String, val displayName: String) {
    AMOUNT("1", "금액"),
    QUANTITY("2", "수량")
}
```

### 3.2 도메인 데이터 클래스

```kotlin
data class RankingItem(
    val rank: Int,
    val ticker: String,            // 종목 코드 (접미사 제거 후)
    val name: String,              // 종목명
    val currentPrice: Long,        // 현재가
    val priceChange: Long,         // 전일대비 변동
    val priceChangeSign: String,   // "+", "-", "" (parseSign으로 변환)
    val changeRate: Double,        // 등락률 (%)
    // --- 순위 유형별 선택적 필드 ---
    val volume: Long? = null,              // 거래량 (ka10023, ka10030, ka10033)
    val surgeRate: Double? = null,         // 급증률 (ka10021, ka10023)
    val surgeQuantity: Long? = null,       // 급증량 (ka10021, ka10023)
    val creditRatio: Double? = null,       // 신용비율 (ka10033)
    val foreignNetBuy: Long? = null,       // 외인순매수 (ka90009)
    val institutionNetBuy: Long? = null,   // 기관순매수 (ka90009)
    val foreignNetSell: Long? = null,      // 외인순매도 (ka90009)
    val institutionNetSell: Long? = null,  // 기관순매도 (ka90009)
    val totalBuyQuantity: Long? = null,    // 총매수잔량 (ka10021)
    val totalSellQuantity: Long? = null,   // 총매도잔량 (ka10021)
    val netValue: Long? = null             // 대표 표시값 (ka90009 필터 기준)
)

data class RankingResult(
    val rankingType: RankingType,
    val marketType: MarketType,
    val exchangeType: ExchangeType,
    val items: List<RankingItem>,
    val fetchedAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    // 필터 컨텍스트 (ka10021)
    val orderBookDirection: OrderBookDirection? = null,
    // 필터 컨텍스트 (ka90009)
    val investorType: InvestorType? = null,
    val tradeDirection: TradeDirection? = null,
    val valueType: ValueType? = null
)
```

---

## 4. API 프로토콜

### 4.1 공통 사항

- **HTTP Method**: POST
- **Endpoint**: `/api/dostk/rkinfo` (5개 API 모두 동일)
- **Base URL**: `InvestmentMode`에 따라 결정
  - MOCK: `https://mockapi.kiwoom.com`
  - PRODUCTION: `https://api.kiwoom.com`
- **인증**: Authorization 헤더에 토큰 (TokenManager가 관리)
- **Content-Type**: `application/json`
- **Rate Limiting**: `ApiCategory.RANKING` — 500ms 최소 간격
- **에러 처리**: 401/403 → 토큰 갱신 후 1회 재시도

### 4.2 API별 요청 파라미터

#### ka10021 — 호가잔량급증요청

```kotlin
data class OrderBookSurgeParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val tradeType: String = "1",      // 1: 매수, 2: 매도
    val sortType: String = "1",       // 1: 급증률 내림차순
    val timeType: String = "30",      // 시간 간격 (분)
    val volumeType: String = "1",     // 거래량구분
    val stockCondition: String = "0"  // 0: 전체 종목
) : RankingParams {
    override fun toRequestBody() = mapOf(
        "mrkt_tp" to marketType.code,
        "trde_tp" to tradeType,
        "sort_tp" to sortType,
        "tm_tp" to timeType,
        "trde_qty_tp" to volumeType,
        "stk_cnd" to stockCondition,
        "stex_tp" to exchangeType.code
    )
}
```

#### ka10023 — 거래량급증요청

```kotlin
data class VolumeSurgeParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val sortType: String = "1",
    val timeType: String = "2",
    val volumeType: String = "5",
    val time: String = "",
    val stockCondition: String = "0",
    val priceType: String = "0"
) : RankingParams {
    override fun toRequestBody() = mapOf(
        "mrkt_tp" to marketType.code,
        "sort_tp" to sortType,
        "tm_tp" to timeType,
        "trde_qty_tp" to volumeType,
        "tm" to time,
        "stk_cnd" to stockCondition,
        "pric_tp" to priceType,
        "stex_tp" to exchangeType.code
    )
}
```

#### ka10030 — 당일거래량상위요청

```kotlin
data class DailyVolumeTopParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val sortType: String = "1",
    val managedStockInclude: String = "0",
    val creditType: String = "0",
    val volumeType: String = "0",
    val priceType: String = "0",
    val amountType: String = "0",
    val marketOpenType: String = "0"
) : RankingParams {
    override fun toRequestBody() = mapOf(
        "mrkt_tp" to marketType.code,
        "sort_tp" to sortType,
        "mang_stk_incls" to managedStockInclude,
        "crd_tp" to creditType,
        "trde_qty_tp" to volumeType,
        "pric_tp" to priceType,
        "trde_prica_tp" to amountType,
        "mrkt_open_tp" to marketOpenType,
        "stex_tp" to exchangeType.code
    )
}
```

#### ka10033 — 신용비율상위요청

```kotlin
data class CreditRatioTopParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val volumeType: String = "0",
    val stockCondition: String = "0",
    val upDownInclude: String = "1",
    val creditCondition: String = "0"
) : RankingParams {
    override fun toRequestBody() = mapOf(
        "mrkt_tp" to marketType.code,
        "trde_qty_tp" to volumeType,
        "stk_cnd" to stockCondition,
        "updown_incls" to upDownInclude,
        "crd_cnd" to creditCondition,
        "stex_tp" to exchangeType.code
    )
}
```

#### ka90009 — 외국인기관매매상위요청

```kotlin
data class ForeignInstitutionTopParams(
    override val marketType: MarketType,
    override val exchangeType: ExchangeType,
    val amountQtyType: String = "1",   // 1: 금액, 2: 수량
    val queryDateType: String = "1",   // 1: 당일
    val date: String? = null,          // YYYYMMDD (null이면 당일)
    // 파싱 시 사용 (API 요청에는 포함하지 않음)
    val investorType: InvestorType = InvestorType.FOREIGN,
    val tradeDirection: TradeDirection = TradeDirection.NET_BUY
) : RankingParams {
    override fun toRequestBody() = buildMap {
        put("mrkt_tp", marketType.code)
        put("amt_qty_tp", amountQtyType)
        put("qry_dt_tp", queryDateType)
        date?.let { put("date", it) }
        put("stex_tp", exchangeType.code)
    }
}
```

### 4.3 공통 RankingParams 인터페이스

```kotlin
interface RankingParams {
    val marketType: MarketType
    val exchangeType: ExchangeType
    fun toRequestBody(): Map<String, String>
}
```

---

## 5. API 응답 DTO

### 5.1 공통 아이템 DTO (ka10021, ka10023, ka10030, ka10033)

```kotlin
@Serializable
data class RankingItemDto(
    // 공통 필드
    @SerialName("stk_cd") val stkCd: String? = null,          // 종목코드
    @SerialName("stk_nm") val stkNm: String? = null,          // 종목명
    @SerialName("cur_prc") val curPrc: String? = null,        // 현재가
    @SerialName("pred_pre_sig") val predPreSig: String? = null, // 전일대비 부호
    @SerialName("pred_pre") val predPre: String? = null,      // 전일대비
    @SerialName("flu_rt") val fluRt: String? = null,          // 등락률

    // 거래량 관련 (ka10023, ka10030)
    @SerialName("trde_qty") val trdeQty: String? = null,      // 거래량
    @SerialName("now_trde_qty") val nowTrdeQty: String? = null, // 현재거래량
    @SerialName("prev_trde_qty") val prevTrdeQty: String? = null, // 전일거래량
    @SerialName("sdnin_qty") val sdninQty: String? = null,    // 급증수량
    @SerialName("sdnin_rt") val sdninRt: String? = null,      // 급증률
    @SerialName("pred_rt") val predRt: String? = null,        // 전일비율

    // 호가잔량 관련 (ka10021)
    @SerialName("tot_buy_qty") val totBuyQty: String? = null,  // 총매수잔량
    @SerialName("tot_sel_req") val totSelReq: String? = null,  // 총매도호가
    @SerialName("tot_buy_req") val totBuyReq: String? = null,  // 총매수호가
    @SerialName("now") val now: String? = null,                // 현시각
    @SerialName("int") val baseRate: String? = null,           // 기준금리

    // 신용 관련 (ka10033)
    @SerialName("crd_rt") val crdRt: String? = null,          // 신용비율
    @SerialName("sel_req") val selReq: String? = null,        // 매도호가
    @SerialName("buy_req") val buyReq: String? = null,        // 매수호가

    // 순위 관련
    @SerialName("rank") val rank: String? = null,              // 순위
    @SerialName("now_rank") val nowRank: String? = null        // 현재순위
)
```

> **중요**: 모든 필드가 `String?`이며 기본값 `null`. 다른 API에서 반환하지 않는 필드는 null로 남는다.

### 5.2 응답 래퍼 DTO

각 API는 동일한 구조지만 데이터 배열의 키 이름이 다르다:

| API | 응답 래퍼 필드명 | DTO 클래스 |
|-----|------------------|------------|
| ka10021 | `bid_req_sdnin` | `OrderBookSurgeResponse` |
| ka10023 | `trde_qty_sdnin` | `VolumeSurgeResponse` |
| ka10030 | `tdy_trde_qty_top` | `DailyVolumeTopResponse` |
| ka10033 | `crd_rt_top` | `CreditRatioTopResponse` |
| ka90009 | `frgnr_orgn_trde_upper` | `ForeignInstitutionTopResponse` |

```kotlin
// 예시: ka10030 응답
@Serializable
data class DailyVolumeTopResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("tdy_trde_qty_top") val items: List<RankingItemDto>? = null
)
```

> **구현 참고**: `RankingRepoImpl`은 ka10021~ka10033에 대해 특정 응답 DTO로 역직렬화하지 않고, `findAndParseItemsArray()`로 JSON을 동적 탐색하여 배열을 추출한다. 이 방식은 래퍼 필드명이 변경되어도 동작한다.

### 5.3 ka90009 전용 아이템 DTO

ka90009는 한 행에 4개 독립 랭킹(외인순매도, 외인순매수, 기관순매도, 기관순매수)의 데이터가 합쳐져 있다:

```kotlin
@Serializable
data class ForeignInstitutionItemDto(
    // 외인 순매도
    @SerialName("for_netslmt_stk_cd") val forNetslmtStkCd: String? = null,
    @SerialName("for_netslmt_stk_nm") val forNetslmtStkNm: String? = null,
    @SerialName("for_netslmt_amt") val forNetslmtAmt: String? = null,
    @SerialName("for_netslmt_qty") val forNetslmtQty: String? = null,
    // 외인 순매수
    @SerialName("for_netprps_stk_cd") val forNetprpsStkCd: String? = null,
    @SerialName("for_netprps_stk_nm") val forNetprpsStkNm: String? = null,
    @SerialName("for_netprps_amt") val forNetprpsAmt: String? = null,
    @SerialName("for_netprps_qty") val forNetprpsQty: String? = null,
    // 기관 순매도
    @SerialName("orgn_netslmt_stk_cd") val orgnNetslmtStkCd: String? = null,
    @SerialName("orgn_netslmt_stk_nm") val orgnNetslmtStkNm: String? = null,
    @SerialName("orgn_netslmt_amt") val orgnNetslmtAmt: String? = null,
    @SerialName("orgn_netslmt_qty") val orgnNetslmtQty: String? = null,
    // 기관 순매수
    @SerialName("orgn_netprps_stk_cd") val orgnNetprpsStkCd: String? = null,
    @SerialName("orgn_netprps_stk_nm") val orgnNetprpsStkNm: String? = null,
    @SerialName("orgn_netprps_amt") val orgnNetprpsAmt: String? = null,
    @SerialName("orgn_netprps_qty") val orgnNetprpsQty: String? = null
)

@Serializable
data class ForeignInstitutionTopResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String? = null,
    @SerialName("frgnr_orgn_trde_upper") val items: List<ForeignInstitutionItemDto>? = null
)
```

#### ka90009 필드 구조 시각화

```
한 행(row)의 16개 필드:
┌──────────────────┬──────────────────┬──────────────────┬──────────────────┐
│  외인 순매도      │  외인 순매수      │  기관 순매도      │  기관 순매수      │
│  for_netslmt_*   │  for_netprps_*   │  orgn_netslmt_*  │  orgn_netprps_*  │
├──────────────────┼──────────────────┼──────────────────┼──────────────────┤
│  stk_cd (종목코드) │  stk_cd          │  stk_cd          │  stk_cd          │
│  stk_nm (종목명)  │  stk_nm          │  stk_nm          │  stk_nm          │
│  amt (금액)       │  amt             │  amt             │  amt             │
│  qty (수량)       │  qty             │  qty             │  qty             │
└──────────────────┴──────────────────┴──────────────────┴──────────────────┘
```

---

## 6. 응답 파싱 로직

### 6.1 파싱 유틸리티 (RankingParseUtils)

```kotlin
object RankingParseUtils {
    // 종목코드 접미사 목록
    private val TICKER_SUFFIXES = listOf("_AL", "_KS", "_KQ")

    /** 종목코드에서 접미사 제거. "_AL", "_KS", "_KQ" 순서대로 replace. */
    fun cleanTicker(value: String?): String =
        value?.let { ticker ->
            TICKER_SUFFIXES.fold(ticker) { acc, suffix -> acc.replace(suffix, "") }.trim()
        } ?: ""

    /** 문자열 → Long 변환. 콤마/+제거, 실패 시 0 반환. */
    fun parseLong(value: String?): Long =
        value?.replace(",", "")?.replace("+", "")?.trim()?.toLongOrNull() ?: 0

    /** 문자열 → Double 변환. 콤마/+/%제거, 실패 시 0.0 반환. */
    fun parseDouble(value: String?): Double =
        value?.replace(",", "")?.replace("+", "")?.replace("%", "")?.trim()?.toDoubleOrNull() ?: 0.0

    /** 전일대비 부호 변환. 1,2,"+" → "+", 4,5,"-" → "-", 나머지 → "" */
    fun parseSign(value: String?): String = when (value?.trim()) {
        "1", "2", "+" -> "+"
        "4", "5", "-" -> "-"
        else -> ""
    }
}
```

### 6.2 동적 배열 탐색 (findAndParseItemsArray)

ka10021~ka10033 API의 응답에서 데이터 배열을 동적으로 찾는다:

```
알고리즘:
1. 응답 JSON을 JsonObject로 파싱
2. 메타데이터 필드 건너뛰기: {"return_code", "return_msg", "msg_cd", "msg1"}
3. 남은 필드 중 JsonArray인 것을 찾음
4. 배열의 첫 번째 요소가 JsonObject이면 → List<RankingItemDto>로 역직렬화
5. 배열이 비어있으면 → emptyList 반환
6. 배열을 찾지 못하면 → emptyList 반환
```

### 6.3 유형별 DTO → 도메인 변환 매핑

#### 공통 베이스 변환 (toBaseRankingItem)

```
RankingItemDto → RankingItem:
  rank        = index + 1  (0-based index를 1-based rank로)
  ticker      = cleanTicker(stkCd)
  name        = stkNm ?: ""
  currentPrice = parseLong(curPrc)
  priceChange  = parseLong(predPre)
  priceChangeSign = parseSign(predPreSig)
  changeRate   = 0.0  (기본값, 유형별로 오버라이드)
```

#### ka10021 (호가잔량급증)

```
base 변환 + 추가 필드:
  changeRate       = 0.0 (API에서 미제공)
  surgeQuantity    = parseLong(sdninQty)
  surgeRate        = parseDouble(sdninRt)
  totalBuyQuantity = parseLong(totBuyQty)

RankingResult 추가:
  orderBookDirection = BUY 또는 SELL (params.tradeType == "1" → BUY, "2" → SELL)
```

#### ka10023 (거래량급증)

```
base 변환 + 추가 필드:
  changeRate    = parseDouble(fluRt)
  volume        = parseLong(nowTrdeQty)
  surgeQuantity = parseLong(sdninQty)
  surgeRate     = parseDouble(sdninRt)
```

#### ka10030 (당일거래량상위)

```
base 변환 + 추가 필드:
  changeRate = parseDouble(fluRt)
  volume     = parseLong(trdeQty)
```

#### ka10033 (신용비율상위)

```
base 변환 + 추가 필드:
  changeRate  = parseDouble(fluRt)
  creditRatio = parseDouble(crdRt)
  volume      = parseLong(nowTrdeQty)
```

#### ka90009 (외국인/기관상위) — ForeignInstitutionExtractor

ka90009는 별도 추출기를 사용한다. 한 행에 4개 독립 랭킹이 있으므로 필터 기준으로 선택:

```
DirectionFields 매핑:
  direction == NET_BUY → netprps 계열 필드 사용
  direction == NET_SELL → netslmt 계열 필드 사용

투자자 유형별 추출:
  FOREIGN:
    ticker = for_{direction}_stk_cd
    name   = for_{direction}_stk_nm
    value  = for_{direction}_{amt|qty}  (isAmount 기준)
    → foreignNetBuy 또는 foreignNetSell + netValue 설정

  INSTITUTION:
    ticker = orgn_{direction}_stk_cd  (비어있으면 for 계열로 폴백)
    name   = orgn_{direction}_stk_nm  (비어있으면 for 계열로 폴백)
    value  = orgn_{direction}_{amt|qty}
    → institutionNetBuy 또는 institutionNetSell + netValue 설정

  ALL (전체):
    ticker = for_{direction}_stk_cd
    name   = for_{direction}_stk_nm
    foreignValue     = for_{direction}_{amt|qty}
    institutionValue = orgn_{direction}_{amt|qty}
    netValue         = foreignValue + institutionValue
    → foreignNetBuy/Sell + institutionNetBuy/Sell + netValue 설정

ka90009 RankingItem 특이사항:
  - currentPrice = 0 (API에서 미제공)
  - priceChange = 0
  - priceChangeSign = ""
  - ticker가 비어있는 항목은 결과에서 필터링 (.filter { it.ticker.isNotEmpty() })

RankingResult 추가:
  investorType = params.investorType
  tradeDirection = params.tradeDirection
  valueType = AMOUNT 또는 QUANTITY (params.amountQtyType == "1" → AMOUNT)
```

---

## 7. Repository

### 7.1 인터페이스

```kotlin
interface RankingRepo {
    suspend fun getOrderBookSurge(params: OrderBookSurgeParams): Result<RankingResult>
    suspend fun getVolumeSurge(params: VolumeSurgeParams): Result<RankingResult>
    suspend fun getDailyVolumeTop(params: DailyVolumeTopParams): Result<RankingResult>
    suspend fun getCreditRatioTop(params: CreditRatioTopParams): Result<RankingResult>
    suspend fun getForeignInstitutionTop(params: ForeignInstitutionTopParams): Result<RankingResult>
}
```

### 7.2 구현체 핵심 로직

```
각 메서드의 동작:
1. getApiConfig() → SettingsProvider에서 appKey, secretKey, investmentMode 조회
   - isValid() == false → throw ApiError.NoApiKeyError()
   - baseUrl = MOCK → mockapi.kiwoom.com, PRODUCTION → api.kiwoom.com
2. apiClient.call(apiId, "/api/dostk/rkinfo", params.toRequestBody(), ...) 호출
3. 응답 파싱:
   - ka10021~ka10033: findAndParseItemsArray(json) → RankingParsers.parseXxx()
   - ka90009: json.decodeFromString<ForeignInstitutionTopResponse>() → RankingParsers.parseForeignInstitutionTopResponse()
4. 에러 시 Result.failure(ApiError) 반환
```

---

## 8. UseCase (GetRankingUC)

```kotlin
class GetRankingUC @Inject constructor(
    private val repo: RankingRepo
) {
    suspend operator fun invoke(
        rankingType: RankingType,
        marketType: MarketType,
        exchangeType: ExchangeType,
        itemCount: ItemCount = ItemCount.TEN,
        orderBookDirection: OrderBookDirection = OrderBookDirection.BUY,
        investorType: InvestorType = InvestorType.FOREIGN,
        tradeDirection: TradeDirection = TradeDirection.NET_BUY,
        valueType: ValueType = ValueType.AMOUNT
    ): Result<RankingResult>
}
```

#### 유형별 repo 메서드 매핑

| RankingType | Repo 메서드 | 전달 파라미터 |
|-------------|------------|-------------|
| ORDER_BOOK_SURGE | `getOrderBookSurge()` | marketType, exchangeType, tradeType=orderBookDirection.code |
| VOLUME_SURGE | `getVolumeSurge()` | marketType, exchangeType |
| DAILY_VOLUME_TOP | `getDailyVolumeTop()` | marketType, exchangeType |
| CREDIT_RATIO_TOP | `getCreditRatioTop()` | marketType, exchangeType |
| FOREIGN_INSTITUTION_TOP | `getForeignInstitutionTop()` | marketType, exchangeType, amountQtyType=valueType.code, investorType, tradeDirection |

**후처리**: `result.map { it.copy(items = it.items.take(itemCount.value)) }`

---

## 9. DI (의존성 주입)

```kotlin
// Hilt 사용 시:
@Module
@InstallIn(SingletonComponent::class)
abstract class RankingModule {
    @Binds @Singleton
    abstract fun bindRankingRepo(impl: RankingRepoImpl): RankingRepo
}

// 다른 DI 프레임워크 사용 시:
// RankingRepo → RankingRepoImpl (Singleton)
// GetRankingUC → constructor(RankingRepo)
// RankingVm → constructor(GetRankingUC, SettingsProvider, StockSelector)
```

---

## 10. 기술 스택 요구사항

| 라이브러리 | 용도 | 필수 여부 |
|-----------|------|----------|
| Kotlinx Serialization | JSON 파싱 (DTO) | 필수 |
| Kotlin Coroutines | suspend 함수, Flow | 필수 |
| Jetpack Compose | UI | 필수 (UI 구현 시) |
| Hilt | DI | 대체 가능 (Koin 등) |
| OkHttp | HTTP 클라이언트 | 대체 가능 (Ktor 등) |
