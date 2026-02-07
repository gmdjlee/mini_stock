package com.stockapp.core.krx

/**
 * 데이터 소스 우선순위 전략.
 *
 * 데이터 유형에 따라 적절한 소스를 선택:
 * - 배치 데이터 (종목리스트, OHLCV, 시가총액 등): KRX가 primary
 * - 실시간 데이터 (체결, 호가, 순위 등): Kiwoom/KIS가 primary
 */
enum class DataSourceStrategy {
    /**
     * KRX를 primary로, Kiwoom/KIS를 fallback으로 사용.
     * 배치 데이터 수집에 적합: 종목리스트, OHLCV, 시가총액, 투자자별 거래 등
     */
    KRX_FIRST,

    /**
     * Kiwoom/KIS를 primary로 사용.
     * 실시간 데이터에 적합: 실시간 수급, 호가잔량급증, 거래량급증, 순위 등
     */
    BROKER_FIRST
}
