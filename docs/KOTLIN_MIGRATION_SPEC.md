# Kotlin Native Migration Specification

## Python (Chaquopy) → Kotlin Native 전환 명세서

**버전**: 1.7
**작성일**: 2026-02-02
**최종 수정일**: 2026-02-03
**상태**: ✅ Completed

---

## 1. 개요

### 1.1 목적

Python (Chaquopy) 기반의 주식 분석 기능을 순수 Kotlin으로 전환하여:
- APK 크기 감소 (~80MB → ~25MB)
- 앱 시작 속도 개선 (Python 초기화 제거)
- 단일 언어 코드베이스로 유지보수성 향상
- 실시간 데이터 기능 추가 용이성 확보

### 1.2 전환 범위

| 기능 | 현재 (Python) | 전환 후 (Kotlin) |
|------|---------------|------------------|
| 종목 검색 | `stock/search.py` | `NativeSearchRepo` |
| 수급 분석 | `stock/analysis.py` | `NativeAnalysisRepo` |
| OHLCV 조회 | `stock/ohlcv.py` | `OhlcvService` |
| Trend Signal | `indicator/trend.py` | `TrendCalculator` |
| Elder Impulse | `indicator/elder.py` | `ElderCalculator` |
| DeMark TD | `indicator/demark.py` | `DemarkCalculator` |
| **실시간 수급** (신규) | - | `RealtimeSupplyRepo` |

### 1.3 기존 Kotlin 인프라 활용

이미 구현된 컴포넌트:
- `KiwoomApiClient.kt` - REST API 호출 (Ranking, ETF에서 사용 중)
- `KisApiClient.kt` - KIS API 호출 (Financial에서 사용 중)
- `TokenManager.kt` - OAuth 토큰 관리
- Domain Models - `StockData`, `TrendSignal`, `ElderImpulse`, `DemarkSetup`

---

## 2. 아키텍처 설계

### 2.1 새 모듈 구조

```
StockApp/app/src/main/java/com/stockapp/
├── core/
│   ├── api/
│   │   ├── KiwoomApiClient.kt          (기존 - 확장)
│   │   ├── KisApiClient.kt             (기존)
│   │   └── TokenManager.kt             (기존)
│   ├── stock/                          (신규)
│   │   ├── api/
│   │   │   ├── StockApiService.kt      # API 인터페이스
│   │   │   └── StockApiModels.kt       # DTO 모델
│   │   ├── calc/
│   │   │   ├── MathUtil.kt             # 수학 유틸리티
│   │   │   ├── OhlcvResampler.kt       # 주/월봉 리샘플링
│   │   │   └── IndicatorCalculator.kt  # 지표 계산
│   │   └── data/
│   │       └── OhlcvService.kt         # OHLCV 서비스
│   ├── config/
│   │   └── FeatureFlags.kt             (신규) # 기능 플래그
│   └── py/
│       ├── PyClient.kt                 (기존 - 유지)
│       └── PyResponse.kt               (기존 - 유지)
├── feature/
│   ├── search/
│   │   └── data/repo/
│   │       ├── SearchRepoImpl.kt       (기존 - 유지)
│   │       ├── NativeSearchRepoImpl.kt (신규)
│   │       └── SearchRepoSelector.kt   (신규) # 선택자
│   ├── analysis/
│   │   └── data/repo/
│   │       ├── AnalysisRepoImpl.kt     (기존 - 유지)
│   │       ├── NativeAnalysisRepoImpl.kt (신규)
│   │       └── AnalysisRepoSelector.kt (신규)
│   ├── indicator/
│   │   └── data/repo/
│   │       ├── IndicatorRepoImpl.kt    (기존 - 유지)
│   │       ├── NativeIndicatorRepoImpl.kt (신규)
│   │       └── IndicatorRepoSelector.kt (신규)
│   └── realtime/                       (신규) # 실시간 수급
│       ├── domain/
│       │   ├── model/RealtimeModels.kt
│       │   ├── repo/RealtimeSupplyRepo.kt
│       │   └── usecase/GetRealtimeSupplyUC.kt
│       ├── data/
│       │   └── repo/RealtimeSupplyRepoImpl.kt
│       ├── ui/
│       │   ├── RealtimeSupplyTab.kt
│       │   └── RealtimeSupplyVm.kt
│       └── di/RealtimeModule.kt
```

### 2.2 Repository 선택 패턴

Feature Flag 기반 점진적 전환:

```kotlin
@Singleton
class SearchRepoSelector @Inject constructor(
    private val nativeRepo: NativeSearchRepoImpl,
    private val pyRepo: SearchRepoImpl,
    private val featureFlags: FeatureFlagRepo
) : SearchRepo {

    private suspend fun selectRepo(): SearchRepo {
        return if (featureFlags.isEnabled(FeatureFlags.USE_NATIVE_SEARCH)) {
            nativeRepo
        } else {
            pyRepo
        }
    }

    override suspend fun search(query: String) = selectRepo().search(query)
}
```

### 2.3 Feature Flag 설계

```kotlin
// core/config/FeatureFlags.kt
object FeatureFlags {
    const val USE_NATIVE_SEARCH = "use_native_search"
    const val USE_NATIVE_ANALYSIS = "use_native_analysis"
    const val USE_NATIVE_INDICATOR = "use_native_indicator"
    const val ENABLE_REALTIME_SUPPLY = "enable_realtime_supply"
}

interface FeatureFlagRepo {
    suspend fun isEnabled(flag: String): Boolean
    suspend fun setEnabled(flag: String, enabled: Boolean)
}
```

---

## 3. API 매핑

### 3.1 사용 API 목록

| API ID | 용도 | Python 모듈 | Kotlin 서비스 |
|--------|------|-------------|---------------|
| ka10099 | 종목 리스트 | `search.py` | `StockApiService` |
| ka10001 | 주식 기본정보 | `search.py`, `analysis.py` | `StockApiService` |
| ka10059 | 투자자별 매매 | `analysis.py` | `StockApiService` |
| ka10081 | 일봉 차트 | `ohlcv.py` | `OhlcvService` |
| ka10082 | 주봉 차트 | `ohlcv.py` | `OhlcvService` |
| ka10083 | 월봉 차트 | `ohlcv.py` | `OhlcvService` |
| **ka10063** | **장중 투자자별 매매** (신규) | - | `RealtimeSupplyRepo` |

### 3.2 API 응답 매핑

#### ka10099 - 종목 리스트

```kotlin
// Request
data class StockListRequest(
    val mrktTp: String = "0",     // 0: 전체, 1: KOSPI, 2: KOSDAQ
)

// Response
@Serializable
data class StockListResponse(
    @SerialName("return_code") val returnCode: Int,
    @SerialName("list") val list: List<StockItem>? = null
)

@Serializable
data class StockItem(
    @SerialName("code") val code: String,
    @SerialName("name") val name: String,
    @SerialName("marketName") val marketName: String
)
```

#### ka10059 - 투자자별 매매

```kotlin
@Serializable
data class InvestorTrendResponse(
    @SerialName("return_code") val returnCode: Int,
    @SerialName("stk_invsr_orgn") val data: List<InvestorTrendItem>? = null
)

@Serializable
data class InvestorTrendItem(
    @SerialName("dt") val date: String,
    @SerialName("frgnr_invsr") val foreignNet: Long,    // 외국인 순매수
    @SerialName("orgn") val institutionNet: Long,       // 기관 순매수
    @SerialName("ind_invsr") val individualNet: Long,   // 개인 순매수
    @SerialName("mrkt_tot_amt") val marketCap: Long     // 시가총액 (백만원)
)
```

#### ka10081 - 일봉 차트

```kotlin
@Serializable
data class OhlcvResponse(
    @SerialName("return_code") val returnCode: Int,
    @SerialName("stk_dt_pole_chart_qry") val data: List<OhlcvItem>? = null
)

@Serializable
data class OhlcvItem(
    @SerialName("dt") val date: String,
    @SerialName("open_pric") val open: Int,
    @SerialName("high_pric") val high: Int,
    @SerialName("low_pric") val low: Int,
    @SerialName("cur_prc") val close: Int,
    @SerialName("trde_qty") val volume: Long
)
```

#### ka10063 - 장중 투자자별 매매 (신규)

```kotlin
// Request
data class RealtimeSupplyParams(
    val stkCd: String,               // 종목코드
    val mrktTp: String = "000",      // 000: 전체
    val invsr: String = "6",         // 6: 전체 투자자
    val stexTp: String = "3",        // 3: KRX (모의)
    val amtQtyTp: String = "1"       // 1: 금액
)

// Response
@Serializable
data class RealtimeSupplyResponse(
    @SerialName("return_code") val returnCode: Int,
    @SerialName("cur_prc") val currentPrice: Long? = null,
    @SerialName("netprps_amt") val netBuyAmount: Long? = null,
    @SerialName("buy_amt") val buyAmount: Long? = null,
    @SerialName("sell_amt") val sellAmount: Long? = null,
    @SerialName("netprps_qty") val netBuyQuantity: Long? = null,
    @SerialName("acc_trde_qty") val accumulatedVolume: Long? = null
)
```

---

## 4. 계산 로직 명세

### 4.1 MathUtil 함수

```kotlin
// core/stock/calc/MathUtil.kt
object MathUtil {

    /**
     * Rolling Sum (min_periods=1)
     * Python: analysis.py _rolling_sum()
     */
    fun rollingSum(values: List<Long>, window: Int): List<Long> {
        return values.indices.map { i ->
            val start = maxOf(0, i - window + 1)
            values.subList(start, i + 1).sum()
        }
    }

    /**
     * Simple Moving Average
     * Python: trend.py _calc_ma()
     */
    fun sma(prices: List<Int>, period: Int): List<Double?> {
        return prices.indices.map { i ->
            if (i + period > prices.size) null
            else prices.subList(i, i + period).average()
        }
    }

    /**
     * Exponential Moving Average (adjust=false)
     * Python: elder.py _calc_ema_no_sma()
     */
    fun ema(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty()) return emptyList()

        val alpha = 2.0 / (period + 1)
        val result = mutableListOf(prices.last())  // Chronological: start from oldest

        // Process in chronological order (reverse iteration)
        for (i in prices.size - 2 downTo 0) {
            val emaValue = alpha * prices[i] + (1 - alpha) * result.last()
            result.add(emaValue)
        }

        return result.reversed()  // Back to newest-first
    }

    /**
     * Chaikin Money Flow
     * Python: trend.py _calc_cmf()
     */
    fun cmf(
        highs: List<Int>,
        lows: List<Int>,
        closes: List<Int>,
        volumes: List<Long>,
        period: Int
    ): List<Double> {
        // Money Flow Multiplier: ((C - L) - (H - C)) / (H - L)
        val mfv = highs.indices.map { i ->
            val hlRange = highs[i] - lows[i]
            if (hlRange == 0) 0.0
            else {
                val mfm = ((closes[i] - lows[i]) - (highs[i] - closes[i])).toDouble() / hlRange
                mfm * volumes[i]
            }
        }

        // CMF = Sum(MFV) / Sum(Volume) over period
        return highs.indices.map { i ->
            if (i + period > highs.size) 0.0
            else {
                val sumMfv = mfv.subList(i, i + period).sum()
                val sumVol = volumes.subList(i, i + period).sum()
                if (sumVol == 0L) 0.0 else sumMfv / sumVol
            }
        }
    }

    /**
     * Standard Deviation
     * Python: trend.py _calc_std()
     */
    fun std(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }
}
```

### 4.2 Fear/Greed 계산

```kotlin
// core/stock/calc/IndicatorCalculator.kt

/**
 * Fear/Greed Index 계산
 * Python: trend.py _calc_fear_greed_weekly()
 *
 * 구성요소:
 * - Momentum5 (45%): 5기간 로그 수익률
 * - Pos52 (45%): 52기간 범위 내 위치
 * - VolSurge (5%): 거래량 급증률
 * - VolSpike (5%): 변동성 급증률 (음수)
 */
fun calcFearGreed(closes: List<Int>, volumes: List<Long>): List<Double> {
    val n = closes.size
    if (n < 52) return List(n) { 0.0 }

    // Chronological order for calculation
    val closesChron = closes.reversed()
    val volumesChron = volumes.reversed()

    // Component arrays
    val momentum5 = DoubleArray(n)
    val pos52 = DoubleArray(n)
    val volSurge = DoubleArray(n) { 1.0 }
    val volSpike = DoubleArray(n) { 1.0 }
    val returns = DoubleArray(n)

    for (i in 0 until n) {
        // Momentum5: log return over 5 periods * 100
        if (i >= 5 && closesChron[i] > 0 && closesChron[i - 5] > 0) {
            momentum5[i] = (ln(closesChron[i].toDouble()) - ln(closesChron[i - 5].toDouble())) * 100
        }

        // Pos52: Position within 52-period range
        if (i >= 51) {
            val window = closesChron.subList(i - 51, i + 1)
            val low52 = window.minOrNull() ?: 0
            val high52 = window.maxOrNull() ?: 0
            pos52[i] = if (high52 > low52) {
                (closesChron[i] - low52).toDouble() / (high52 - low52)
            } else 0.5
        }

        // Returns for volatility
        if (i >= 1 && closesChron[i - 1] > 0) {
            returns[i] = (closesChron[i] - closesChron[i - 1]).toDouble() / closesChron[i - 1]
        }
    }

    // VolSurge: recent 5-period avg / past 20-period avg
    for (i in 20 until n) {
        val recentVol = volumesChron.subList(i - 4, i + 1).average()
        val pastVol = volumesChron.subList(i - 19, i + 1).average()
        if (pastVol > 0) {
            volSurge[i] = (recentVol / pastVol).coerceIn(0.0, 3.0)
        }
    }

    // VolSpike: recent volatility / past volatility
    for (i in 20 until n) {
        val recentReturns = returns.slice(i - 4..i)
        val pastReturns = returns.slice(i - 19..i)
        val recentStd = MathUtil.std(recentReturns)
        val pastStd = MathUtil.std(pastReturns)
        if (pastStd > 0) {
            volSpike[i] = (recentStd / pastStd).coerceIn(0.0, 3.0)
        }
    }

    // Calculate FG with smoothing
    val fgChron = DoubleArray(n)
    val momentumWindow = 7
    val volumeWindow = 10

    for (i in 10 until n) {
        // m = (Momentum5.rolling(7).mean() / 10).clip(-1, 1.5)
        val mWindow = momentum5.slice(maxOf(0, i - momentumWindow + 1)..i)
        val m = (mWindow.average() / 10).coerceIn(-1.0, 1.5)

        // p = (2 * Pos52.rolling(7).mean() - 1).clip(-1, 1.5)
        val pWindow = pos52.slice(maxOf(0, i - momentumWindow + 1)..i)
        val p = (2 * pWindow.average() - 1).coerceIn(-1.0, 1.5)

        // v = (VolSurge.rolling(10).mean() - 1).clip(-0.5, 1.2)
        val vWindow = volSurge.slice(maxOf(0, i - volumeWindow + 1)..i)
        val v = (vWindow.average() - 1).coerceIn(-0.5, 1.2)

        // vs = -(VolSpike.rolling(10).mean() - 1).clip(-0.5, 1.2)
        val vsWindow = volSpike.slice(maxOf(0, i - volumeWindow + 1)..i)
        val vs = (-(vsWindow.average() - 1)).coerceIn(-0.5, 1.2)

        // FG = 0.45*m + 0.45*p + 0.05*v + 0.05*vs
        fgChron[i] = 0.45 * m + 0.45 * p + 0.05 * v + 0.05 * vs
    }

    return fgChron.toList().reversed()  // Back to newest-first
}
```

### 4.3 MACD 계산

```kotlin
/**
 * MACD (12, 26, 9)
 * Python: elder.py _calc_macd_no_sma()
 */
fun calcMacd(closes: List<Int>): Triple<List<Double>, List<Double>, List<Double>> {
    val closesDouble = closes.map { it.toDouble() }

    val ema12 = MathUtil.ema(closesDouble, 12)
    val ema26 = MathUtil.ema(closesDouble, 26)

    // MACD Line = EMA12 - EMA26
    val macdLine = ema12.zip(ema26) { a, b -> a - b }

    // Signal Line = EMA9 of MACD Line
    val signalLine = MathUtil.ema(macdLine, 9)

    // Histogram = MACD Line - Signal Line
    val histogram = macdLine.zip(signalLine) { a, b -> a - b }

    return Triple(macdLine, signalLine, histogram)
}
```

### 4.4 DeMark TD Setup

```kotlin
/**
 * TD Setup 계산
 * Python: demark.py _calc_td_setup()
 *
 * - Sell Setup: Close > Close[4] 연속이면 +1, 아니면 0
 * - Buy Setup: Close < Close[4] 연속이면 +1, 아니면 0
 */
fun calcTdSetup(closes: List<Int>): Pair<List<Int>, List<Int>> {
    val n = closes.size

    // Chronological order
    val closesChron = closes.reversed()
    val sellChron = IntArray(n)
    val buyChron = IntArray(n)

    for (i in 4 until n) {
        // Sell Setup: 4일 전보다 위에 있으면 카운트 증가
        if (closesChron[i] > closesChron[i - 4]) {
            sellChron[i] = sellChron[i - 1] + 1
        } else {
            sellChron[i] = 0
        }

        // Buy Setup: 4일 전보다 아래 있으면 카운트 증가
        if (closesChron[i] < closesChron[i - 4]) {
            buyChron[i] = buyChron[i - 1] + 1
        } else {
            buyChron[i] = 0
        }
    }

    return sellChron.toList().reversed() to buyChron.toList().reversed()
}
```

---

## 5. 단계별 구현 계획

### Phase 1: 핵심 인프라 (2일)

**목표**: 공통 유틸리티 및 Feature Flag 시스템 구축

| 작업 | 파일 | 예상 코드량 |
|------|------|------------|
| MathUtil 구현 | `core/stock/calc/MathUtil.kt` | ~150줄 |
| OhlcvResampler 구현 | `core/stock/calc/OhlcvResampler.kt` | ~100줄 |
| StockApiModels 정의 | `core/stock/api/StockApiModels.kt` | ~150줄 |
| FeatureFlags 구현 | `core/config/FeatureFlags.kt` | ~80줄 |

**검증**:
- MathUtil 단위 테스트 (Python 결과와 비교)
- Rolling sum, EMA, CMF 정확도 검증

---

### Phase 2: 종목 검색 전환 (2일)

**목표**: Search 기능 Kotlin 전환

| 작업 | 파일 | 예상 코드량 |
|------|------|------------|
| NativeSearchRepoImpl | `feature/search/data/repo/NativeSearchRepoImpl.kt` | ~200줄 |
| SearchRepoSelector | `feature/search/data/repo/SearchRepoSelector.kt` | ~50줄 |
| SearchModule 수정 | `feature/search/di/SearchModule.kt` | ~30줄 |

**검증**:
- 동일 검색어로 Python/Kotlin 결과 비교
- 페이지네이션 동작 확인
- 캐시 동작 확인

---

### Phase 3: OHLCV 및 수급 분석 전환 (4일)

**목표**: OHLCV 서비스 및 Analysis 기능 Kotlin 전환

| 작업 | 파일 | 예상 코드량 |
|------|------|------------|
| OhlcvService | `core/stock/data/OhlcvService.kt` | ~250줄 |
| NativeAnalysisRepoImpl | `feature/analysis/data/repo/NativeAnalysisRepoImpl.kt` | ~180줄 |
| AnalysisRepoSelector | `feature/analysis/data/repo/AnalysisRepoSelector.kt` | ~50줄 |

**검증**:
- OHLCV 데이터 일치 확인
- 5일 롤링 합계 계산 검증
- 시가총액 계산 정확도 확인

---

### Phase 4: 기술 지표 전환 (6일)

**목표**: 3개 지표 계산 로직 Kotlin 전환

| 작업 | 파일 | 예상 코드량 |
|------|------|------------|
| IndicatorCalculator | `core/stock/calc/IndicatorCalculator.kt` | ~400줄 |
| NativeIndicatorRepoImpl | `feature/indicator/data/repo/NativeIndicatorRepoImpl.kt` | ~200줄 |
| IndicatorRepoSelector | `feature/indicator/data/repo/IndicatorRepoSelector.kt` | ~50줄 |

**검증 항목**:

| 지표 | 검증 포인트 |
|------|------------|
| Trend Signal | MA 정배열/역배열, CMF 범위 (-1~1), Fear/Greed 범위 |
| Elder Impulse | EMA13 계산, MACD Histogram, 색상 결정 로직 |
| DeMark TD | 4일 전 비교, 카운트 리셋 로직 |

---

### Phase 5: 실시간 수급 기능 (3일)

**목표**: ka10063 API 기반 실시간 수급 데이터 기능 추가

| 작업 | 파일 | 예상 코드량 |
|------|------|------------|
| RealtimeSupplyRepo | `feature/realtime/domain/repo/RealtimeSupplyRepo.kt` | ~30줄 |
| RealtimeSupplyRepoImpl | `feature/realtime/data/repo/RealtimeSupplyRepoImpl.kt` | ~120줄 |
| GetRealtimeSupplyUC | `feature/realtime/domain/usecase/GetRealtimeSupplyUC.kt` | ~40줄 |
| RealtimeSupplyVm | `feature/realtime/ui/RealtimeSupplyVm.kt` | ~80줄 |
| RealtimeSupplyTab | `feature/realtime/ui/RealtimeSupplyTab.kt` | ~150줄 |
| RealtimeModule | `feature/realtime/di/RealtimeModule.kt` | ~40줄 |

**UI 설계**:
- Analysis 화면에 "실시간" 탭 추가
- 장중 (09:00-15:30) 에만 활성화
- 자동 새로고침 (선택적, 5분 간격)

---

### Phase 6: 통합 테스트 및 검증 (3일)

**목표**: 전체 기능 통합 테스트 및 Python 결과와 비교 검증

| 테스트 유형 | 대상 | 방법 |
|------------|------|------|
| 단위 테스트 | MathUtil, Calculator | JUnit, 골든 테스트 |
| 통합 테스트 | Repository | Python vs Kotlin 결과 비교 |
| UI 테스트 | Screen | 수동 검증 |

---

### Phase 7: 정리 및 문서화 (1일)

**목표**: Feature Flag 기본값 변경, 문서 업데이트

| 작업 | 설명 |
|------|------|
| Feature Flag 기본값 | Native를 기본으로 설정 |
| CLAUDE.md 업데이트 | 새 아키텍처 반영 |
| 코드 정리 | 미사용 코드 제거 (선택적) |

---

## 6. 테스트 전략

### 6.1 골든 테스트 (Golden Test)

Python 결과를 골든 데이터로 사용:

```kotlin
@Test
fun `calcFearGreed matches Python output`() {
    // Given: Python으로 계산한 참조 데이터
    val closes = listOf(55000, 55100, 54900, ...)  // 180개
    val volumes = listOf(1000000L, 1200000L, ...)
    val pythonResult = listOf(0.23, 0.25, 0.21, ...)  // Python 계산 결과

    // When
    val kotlinResult = IndicatorCalculator.calcFearGreed(closes, volumes)

    // Then: 0.01% 오차 허용
    kotlinResult.zip(pythonResult).forEach { (k, p) ->
        assertThat(k).isWithin(0.0001).of(p)
    }
}
```

### 6.2 비교 테스트 (A/B Test)

동일 입력에 대해 Python과 Kotlin 결과 실시간 비교:

```kotlin
@Test
fun `native analysis matches python analysis`() = runTest {
    val ticker = "005930"

    val pyResult = pyRepo.getAnalysis(ticker, 180, useCache = false)
    val nativeResult = nativeRepo.getAnalysis(ticker, 180, useCache = false)

    assertThat(nativeResult.getOrThrow().dates)
        .isEqualTo(pyResult.getOrThrow().dates)
    assertThat(nativeResult.getOrThrow().for5d)
        .containsExactlyElementsIn(pyResult.getOrThrow().for5d)
}
```

### 6.3 성능 테스트

```kotlin
@Test
fun `native calculation is faster than python`() = runTest {
    val ticker = "005930"

    val pyTime = measureTimeMillis {
        repeat(10) { pyRepo.getIndicator(ticker, 180) }
    }

    val nativeTime = measureTimeMillis {
        repeat(10) { nativeRepo.getIndicator(ticker, 180) }
    }

    // Native가 최소 2배 빠를 것으로 예상
    assertThat(nativeTime).isLessThan(pyTime / 2)
}
```

---

## 7. 롤백 계획

### 7.1 Feature Flag 롤백

```kotlin
// Settings에서 사용자가 직접 토글 가능
class SettingsVm {
    fun disableNativeMode() {
        viewModelScope.launch {
            featureFlagRepo.setEnabled(FeatureFlags.USE_NATIVE_SEARCH, false)
            featureFlagRepo.setEnabled(FeatureFlags.USE_NATIVE_ANALYSIS, false)
            featureFlagRepo.setEnabled(FeatureFlags.USE_NATIVE_INDICATOR, false)
        }
    }
}
```

### 7.2 롤백 트리거

| 상황 | 조치 |
|------|------|
| 에러율 1% 이상 증가 | 해당 기능 Native 비활성화 |
| 계산 결과 불일치 보고 | 조사 후 Python 폴백 |
| ANR 0.1% 이상 증가 | 성능 프로파일링 |

### 7.3 코드 유지

전환 완료 후에도 Python 코드 30일간 유지:
- `PyClient.kt` - 유지
- `SearchRepoImpl.kt` (Python 버전) - 유지
- `AnalysisRepoImpl.kt` (Python 버전) - 유지
- `IndicatorRepoImpl.kt` (Python 버전) - 유지

---

## 8. 일정 요약

| Phase | 기간 | 산출물 |
|-------|------|--------|
| Phase 1 | Day 1-2 | 핵심 인프라, Feature Flag |
| Phase 2 | Day 3-4 | 종목 검색 Kotlin 전환 |
| Phase 3 | Day 5-8 | OHLCV, 수급 분석 전환 |
| Phase 4 | Day 9-14 | 기술 지표 전환 |
| Phase 5 | Day 15-17 | 실시간 수급 기능 |
| Phase 6 | Day 18-20 | 통합 테스트 |
| Phase 7 | Day 21 | 문서화, 정리 |

**총 예상 기간**: 21일 (3주)

---

## 9. 예상 코드량

| 카테고리 | 예상 줄 수 |
|----------|-----------|
| Core Infrastructure | ~200 |
| MathUtil | ~150 |
| OhlcvService | ~250 |
| Native Search | ~250 |
| Native Analysis | ~230 |
| IndicatorCalculator | ~400 |
| Native Indicator Repo | ~250 |
| Realtime Supply | ~460 |
| Feature Flags | ~80 |
| Unit Tests | ~500 |
| **Total** | **~2,770줄** |

---

## 10. 의존성 다이어그램

```
Phase 1: Core Infrastructure
    └── MathUtil, OhlcvResampler, StockApiModels, FeatureFlags
         │
Phase 2: Search ──────────────────────────────────────┐
         │                                            │
Phase 3: OHLCV ───┬── Analysis ───────────────────────┤
                  │                                   │
Phase 4: ─────────┴── Indicators ─────────────────────┤
         │  TrendCalculator                           │
         │  ElderCalculator                           │
         │  DemarkCalculator                          │
                                                      │
Phase 5: Realtime Supply ─────────────────────────────┘
         │
Phase 6: Integration Tests
         │
Phase 7: Documentation & Cleanup
```

---

## 11. 승인 체크리스트

### Phase 완료 기준

- [x] **Phase 1**: MathUtil 단위 테스트 100% 통과 (2026-02-02 완료)
- [x] **Phase 2**: 검색 결과 Python과 동일 (2026-02-02 완료)
- [x] **Phase 3**: 수급 분석 결과 Python과 동일 (2026-02-03 완료)
- [x] **Phase 4**: 3개 지표 모두 Python과 동일 (2026-02-03 완료)
- [x] **Phase 5**: 실시간 수급 UI 정상 동작 (2026-02-03 완료)
- [x] **Phase 6**: 전체 통합 테스트 통과 (2026-02-03 완료)
- [x] **Phase 7**: 문서 업데이트 완료 (2026-02-03 완료)

---

## 12. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-02 | 초안 작성 |
| 1.1 | 2026-02-02 | Phase 1 완료 - MathUtil, OhlcvResampler, StockApiModels, FeatureFlags 구현 |
| 1.2 | 2026-02-02 | Phase 2 완료 - NativeSearchRepoImpl, SearchRepoSelector 구현 |
| 1.3 | 2026-02-03 | Phase 3 완료 - OhlcvService, NativeAnalysisRepoImpl, AnalysisRepoSelector 구현 |
| 1.4 | 2026-02-03 | Phase 4 완료 - TrendCalculator, ElderCalculator, DemarkCalculator, NativeIndicatorRepoImpl, IndicatorRepoSelector 구현 |
| 1.5 | 2026-02-03 | Phase 5 완료 - RealtimeSupplyRepo, NativeRealtimeSupplyRepoImpl, RealtimeSupplyTab, DB v10 마이그레이션 구현 |
| 1.6 | 2026-02-03 | Phase 6 완료 - 통합 테스트: TrendCalculatorTest (21개), ElderCalculatorTest (23개), DemarkCalculatorTest (22개), AnalysisRepoSelectorTest (12개), IndicatorRepoSelectorTest (17개) 추가 (총 205개 테스트) |
| 1.7 | 2026-02-03 | Phase 7 완료 - Feature Flag 기본값 `true`로 변경, Settings 고급 탭 추가 (Native/Python 전환 UI), 문서 업데이트 완료, **전체 마이그레이션 완료** |
