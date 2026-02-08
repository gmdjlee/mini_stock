package com.stockapp.feature.market.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for market domain models.
 * Verifies enum logic, computed properties, and boundary conditions.
 */
class MarketModelsTest {

    // ========================================================================
    // FearGreedSignal.fromScore() boundary tests
    // ========================================================================

    @Test
    fun `fromScore returns EXTREME_FEAR for score 0`() {
        assertEquals(FearGreedSignal.EXTREME_FEAR, FearGreedSignal.fromScore(0.0))
    }

    @Test
    fun `fromScore returns EXTREME_FEAR for score 19`() {
        assertEquals(FearGreedSignal.EXTREME_FEAR, FearGreedSignal.fromScore(19.0))
    }

    @Test
    fun `fromScore returns FEAR for score exactly 20`() {
        assertEquals(FearGreedSignal.FEAR, FearGreedSignal.fromScore(20.0))
    }

    @Test
    fun `fromScore returns FEAR for score 39`() {
        assertEquals(FearGreedSignal.FEAR, FearGreedSignal.fromScore(39.0))
    }

    @Test
    fun `fromScore returns NEUTRAL for score exactly 40`() {
        assertEquals(FearGreedSignal.NEUTRAL, FearGreedSignal.fromScore(40.0))
    }

    @Test
    fun `fromScore returns NEUTRAL for score 50`() {
        assertEquals(FearGreedSignal.NEUTRAL, FearGreedSignal.fromScore(50.0))
    }

    @Test
    fun `fromScore returns NEUTRAL for score 59`() {
        assertEquals(FearGreedSignal.NEUTRAL, FearGreedSignal.fromScore(59.0))
    }

    @Test
    fun `fromScore returns GREED for score exactly 60`() {
        assertEquals(FearGreedSignal.GREED, FearGreedSignal.fromScore(60.0))
    }

    @Test
    fun `fromScore returns GREED for score 79`() {
        assertEquals(FearGreedSignal.GREED, FearGreedSignal.fromScore(79.0))
    }

    @Test
    fun `fromScore returns EXTREME_GREED for score exactly 80`() {
        assertEquals(FearGreedSignal.EXTREME_GREED, FearGreedSignal.fromScore(80.0))
    }

    @Test
    fun `fromScore returns EXTREME_GREED for score 100`() {
        assertEquals(FearGreedSignal.EXTREME_GREED, FearGreedSignal.fromScore(100.0))
    }

    @Test
    fun `fromScore returns EXTREME_FEAR for negative score`() {
        assertEquals(FearGreedSignal.EXTREME_FEAR, FearGreedSignal.fromScore(-5.0))
    }

    @Test
    fun `fromScore returns EXTREME_GREED for score above 100`() {
        assertEquals(FearGreedSignal.EXTREME_GREED, FearGreedSignal.fromScore(120.0))
    }

    // ========================================================================
    // FearGreedSignal enum properties
    // ========================================================================

    @Test
    fun `FearGreedSignal has correct labels`() {
        assertEquals("극도의 탐욕", FearGreedSignal.EXTREME_GREED.label)
        assertEquals("탐욕", FearGreedSignal.GREED.label)
        assertEquals("중립", FearGreedSignal.NEUTRAL.label)
        assertEquals("공포", FearGreedSignal.FEAR.label)
        assertEquals("극도의 공포", FearGreedSignal.EXTREME_FEAR.label)
    }

    @Test
    fun `FearGreedSignal has correct score ranges`() {
        assertEquals("80-100", FearGreedSignal.EXTREME_GREED.scoreRange)
        assertEquals("60-80", FearGreedSignal.GREED.scoreRange)
        assertEquals("40-60", FearGreedSignal.NEUTRAL.scoreRange)
        assertEquals("20-40", FearGreedSignal.FEAR.scoreRange)
        assertEquals("0-20", FearGreedSignal.EXTREME_FEAR.scoreRange)
    }

    @Test
    fun `FearGreedSignal has exactly 5 values`() {
        assertEquals(5, FearGreedSignal.values().size)
    }

    // ========================================================================
    // MarketOscillator.netAdvance property
    // ========================================================================

    @Test
    fun `MarketOscillator netAdvance is positive when advances exceed declines`() {
        val oscillator = MarketOscillator(
            date = "2024-01-02",
            market = "KOSPI",
            advanceCount = 600,
            declineCount = 300,
            unchangedCount = 100,
            totalCount = 1000,
            advanceRatio = 0.6,
            declineRatio = 0.3,
            oscillatorValue = 0.6,
            signal = OscillatorSignal.GREED
        )

        assertEquals(300, oscillator.netAdvance)
    }

    @Test
    fun `MarketOscillator netAdvance is negative when declines exceed advances`() {
        val oscillator = MarketOscillator(
            date = "2024-01-02",
            market = "KOSPI",
            advanceCount = 200,
            declineCount = 700,
            unchangedCount = 100,
            totalCount = 1000,
            advanceRatio = 0.2,
            declineRatio = 0.7,
            oscillatorValue = 0.2,
            signal = OscillatorSignal.EXTREME_FEAR
        )

        assertEquals(-500, oscillator.netAdvance)
    }

    @Test
    fun `MarketOscillator netAdvance is zero when advances equal declines`() {
        val oscillator = MarketOscillator(
            date = "2024-01-02",
            market = "KOSPI",
            advanceCount = 400,
            declineCount = 400,
            unchangedCount = 200,
            totalCount = 1000,
            advanceRatio = 0.4,
            declineRatio = 0.4,
            oscillatorValue = 0.4,
            signal = OscillatorSignal.FEAR
        )

        assertEquals(0, oscillator.netAdvance)
    }

    // ========================================================================
    // OscillatorSignal enum labels
    // ========================================================================

    @Test
    fun `OscillatorSignal has correct labels`() {
        assertEquals("극도의 탐욕", OscillatorSignal.EXTREME_GREED.label)
        assertEquals("탐욕", OscillatorSignal.GREED.label)
        assertEquals("중립", OscillatorSignal.NEUTRAL.label)
        assertEquals("공포", OscillatorSignal.FEAR.label)
        assertEquals("극도의 공포", OscillatorSignal.EXTREME_FEAR.label)
    }

    @Test
    fun `OscillatorSignal has correct descriptions`() {
        assertEquals("과매수 구간 - 조정 가능성", OscillatorSignal.EXTREME_GREED.description)
        assertEquals("상승세 강함", OscillatorSignal.GREED.description)
        assertEquals("방향성 불분명", OscillatorSignal.NEUTRAL.description)
        assertEquals("하락세 강함", OscillatorSignal.FEAR.description)
        assertEquals("과매도 구간 - 반등 가능성", OscillatorSignal.EXTREME_FEAR.description)
    }

    @Test
    fun `OscillatorSignal has exactly 5 values`() {
        assertEquals(5, OscillatorSignal.values().size)
    }

    // ========================================================================
    // MarketTab enum
    // ========================================================================

    @Test
    fun `MarketTab has correct titles`() {
        assertEquals("공포/탐욕", MarketTab.FEAR_GREED.title)
        assertEquals("과매수/과매도", MarketTab.OSCILLATOR.title)
        assertEquals("자금 동향", MarketTab.FUND_FLOW.title)
        assertEquals("Blood", MarketTab.BLOOD.title)
    }

    @Test
    fun `MarketTab has exactly 4 values`() {
        assertEquals(4, MarketTab.values().size)
    }

    // ========================================================================
    // MarketDateRange enum
    // ========================================================================

    @Test
    fun `MarketDateRange has correct days`() {
        assertEquals(30, MarketDateRange.ONE_MONTH.days)
        assertEquals(90, MarketDateRange.THREE_MONTHS.days)
        assertEquals(180, MarketDateRange.SIX_MONTHS.days)
        assertEquals(365, MarketDateRange.ONE_YEAR.days)
    }

    @Test
    fun `MarketDateRange has correct labels`() {
        assertEquals("1개월", MarketDateRange.ONE_MONTH.label)
        assertEquals("3개월", MarketDateRange.THREE_MONTHS.label)
        assertEquals("6개월", MarketDateRange.SIX_MONTHS.label)
        assertEquals("1년", MarketDateRange.ONE_YEAR.label)
    }

    // ========================================================================
    // IndicatorComponent default weight
    // ========================================================================

    @Test
    fun `IndicatorComponent default weight is 0_2`() {
        val component = IndicatorComponent(
            name = "Test",
            rawValue = 50.0,
            normalizedScore = 60.0
        )

        assertEquals(0.2, component.weight, 0.001)
    }

    @Test
    fun `IndicatorComponent default description is empty`() {
        val component = IndicatorComponent(
            name = "Test",
            rawValue = 50.0,
            normalizedScore = 60.0
        )

        assertEquals("", component.description)
    }

    @Test
    fun `IndicatorComponent preserves custom values`() {
        val component = IndicatorComponent(
            name = "모멘텀",
            rawValue = 5.5,
            normalizedScore = 77.5,
            weight = 0.3,
            description = "KOSPI 20일 수익률"
        )

        assertEquals("모멘텀", component.name)
        assertEquals(5.5, component.rawValue, 0.001)
        assertEquals(77.5, component.normalizedScore, 0.001)
        assertEquals(0.3, component.weight, 0.001)
        assertEquals("KOSPI 20일 수익률", component.description)
    }
}
