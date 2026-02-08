package com.stockapp.feature.market.domain.model

/**
 * Market indicator tab types.
 * Matches EtfMonitor_Rel: 공포/탐욕, 과매수/과매도, 자금 동향, Blood Indicator
 */
enum class MarketTab(val title: String) {
    FEAR_GREED("공포/탐욕"),
    OSCILLATOR("과매수/과매도"),
    FUND_FLOW("자금 동향"),
    BLOOD("Blood")
}

/**
 * Market oscillator data (aggregate market breadth).
 * Measures what percentage of stocks are in uptrend vs downtrend.
 */
data class MarketOscillator(
    val date: String,
    val market: String,
    val advanceCount: Int,
    val declineCount: Int,
    val unchangedCount: Int,
    val totalCount: Int,
    val advanceRatio: Double,
    val declineRatio: Double,
    val oscillatorValue: Double,
    val signal: OscillatorSignal
) {
    val netAdvance: Int get() = advanceCount - declineCount
}

/**
 * Oscillator signal classification.
 */
enum class OscillatorSignal(val label: String, val description: String) {
    EXTREME_GREED("극도의 탐욕", "과매수 구간 - 조정 가능성"),
    GREED("탐욕", "상승세 강함"),
    NEUTRAL("중립", "방향성 불분명"),
    FEAR("공포", "하락세 강함"),
    EXTREME_FEAR("극도의 공포", "과매도 구간 - 반등 가능성")
}

/**
 * Market-level Fear & Greed Index.
 * Uses 5 KRX-based indicators instead of Python-dependent ones.
 *
 * Indicators (20% each):
 * 1. Momentum: KOSPI 20일 수익률 기반
 * 2. RSI: KOSPI 14일 RSI
 * 3. Volatility: KOSPI 20일 Historical Volatility
 * 4. Foreign/Institution Net Buy: 외인+기관 순매수 비율
 * 5. Short Selling Ratio: 공매도 비율 (역지표)
 */
data class MarketFearGreed(
    val date: String,
    val overallScore: Double,
    val signal: FearGreedSignal,
    val momentum: IndicatorComponent,
    val rsi: IndicatorComponent,
    val volatility: IndicatorComponent,
    val investorFlow: IndicatorComponent,
    val shortSelling: IndicatorComponent
)

/**
 * Individual indicator component for Fear & Greed.
 */
data class IndicatorComponent(
    val name: String,
    val rawValue: Double,
    val normalizedScore: Double,
    val weight: Double = 0.2,
    val description: String = ""
)

/**
 * Fear & Greed signal classification.
 */
enum class FearGreedSignal(val label: String, val scoreRange: String) {
    EXTREME_GREED("극도의 탐욕", "80-100"),
    GREED("탐욕", "60-80"),
    NEUTRAL("중립", "40-60"),
    FEAR("공포", "20-40"),
    EXTREME_FEAR("극도의 공포", "0-20");

    companion object {
        fun fromScore(score: Double): FearGreedSignal = when {
            score >= 80 -> EXTREME_GREED
            score >= 60 -> GREED
            score >= 40 -> NEUTRAL
            score >= 20 -> FEAR
            else -> EXTREME_FEAR
        }
    }
}

/**
 * Time range for market data queries.
 */
enum class MarketDateRange(val label: String, val days: Int) {
    ONE_MONTH("1개월", 30),
    THREE_MONTHS("3개월", 90),
    SIX_MONTHS("6개월", 180),
    ONE_YEAR("1년", 365),
    THREE_YEARS("3년", 1095),
    FIVE_YEARS("5년", 1825);

    companion object {
        /** Default ranges for most tabs (1M ~ 1Y) */
        val DEFAULT_RANGES = listOf(ONE_MONTH, THREE_MONTHS, SIX_MONTHS, ONE_YEAR)
    }
}

/**
 * Market oscillator history for chart display.
 */
data class OscillatorHistory(
    val dates: List<String>,
    val values: List<Double>,
    val signals: List<OscillatorSignal>,
    val advanceRatios: List<Double>,
    val declineRatios: List<Double>
)

/**
 * Fear & Greed history for chart display.
 */
data class FearGreedHistory(
    val dates: List<String>,
    val scores: List<Double>,
    val signals: List<FearGreedSignal>,
    val indexValues: List<Double> = emptyList()
)

/**
 * Fund flow data - 고객예탁금 + 신용잔고 동향.
 * Uses KRX investor trading data as proxy for fund flow.
 */
data class FundFlowData(
    val date: String,
    val foreignNetBuy: Long,
    val institutionNetBuy: Long,
    val individualNetBuy: Long,
    val totalTradingValue: Long
) {
    val netInstitutionalFlow: Long get() = foreignNetBuy + institutionNetBuy
}

/**
 * Fund flow history for chart display.
 */
data class FundFlowHistory(
    val dates: List<String>,
    val foreignNetBuys: List<Long>,
    val institutionNetBuys: List<Long>,
    val individualNetBuys: List<Long>,
    val totalTradingValues: List<Long>
)

/**
 * Fund flow signal classification.
 */
enum class FundFlowSignal(val label: String, val description: String) {
    STRONG_INFLOW("강한 유입", "외인+기관 대규모 순매수"),
    INFLOW("유입", "외인+기관 순매수"),
    NEUTRAL("중립", "매수/매도 균형"),
    OUTFLOW("유출", "외인+기관 순매도"),
    STRONG_OUTFLOW("강한 유출", "외인+기관 대규모 순매도")
}

/**
 * Blood Indicator data.
 * BLOOD = US 3-Month T-Bill Yield / High Yield Spread
 * Uses Yahoo Finance (^IRX for T-Bill) and FRED API (BAMLH0A0HYM2 for HY Spread).
 *
 * Signal: Risk On (BLOOD > SMA100 * 1.1), Risk Off (BLOOD < SMA100 * 0.9), Neutral
 */
data class BloodIndicatorData(
    val date: String,
    val bloodValue: Double,
    val sma100: Double,
    val tBillYield: Double,
    val hySpread: Double,
    val signal: BloodSignal
)

/**
 * Blood Indicator history for chart display.
 */
data class BloodIndicatorHistory(
    val dates: List<String>,
    val bloodValues: List<Double>,
    val sma100Values: List<Double>,
    val signals: List<BloodSignal>,
    val tBillYields: List<Double> = emptyList(),
    val hySpreadValues: List<Double> = emptyList()
)

/**
 * Blood indicator signal classification.
 */
enum class BloodSignal(val label: String, val description: String) {
    RISK_ON("Risk On", "시장 안정 - T-Bill 수익률 대비 하이일드 스프레드 낮음"),
    NEUTRAL("Neutral", "중립 - 관망 구간"),
    RISK_OFF("Risk Off", "시장 위험 - 하이일드 스프레드 급등")
}
