package com.stockapp.core.stock.calc

/**
 * DeMark TD Setup calculator.
 * Kotlin Native implementation of Python indicator/demark.py
 *
 * TD Setup rules (matching EtfMonitor reference):
 * - Sell Setup: Close(t) > Close(t-4) 연속이면 +1, 아니면 0으로 리셋
 * - Buy Setup: Close(t) < Close(t-4) 연속이면 +1, 아니면 0으로 리셋
 * - Sell과 Buy는 독립적으로 카운트 (동시에 값이 있을 수 있음)
 * - 카운트 한도 없음 (무한 증가)
 */
object DemarkCalculator {

    // Comparison lookback period
    private const val LOOKBACK_PERIOD = 4

    // Minimum required periods
    private const val MIN_PERIODS = 5

    /**
     * DeMark TD Setup result.
     */
    data class DemarkResult(
        val ticker: String,
        val timeframe: String,
        val dates: List<String>,
        val close: List<Int>,
        val sellSetup: List<Int>,
        val buySetup: List<Int>
    )

    /**
     * Active setups summary.
     */
    data class ActiveSetups(
        val currentSell: Int,
        val currentBuy: Int,
        val maxSell: Int,
        val maxBuy: Int,
        val recentSetups: List<SetupEntry>
    )

    /**
     * Individual setup entry.
     */
    data class SetupEntry(
        val date: String,
        val sell: Int,
        val buy: Int
    )

    /**
     * Calculate DeMark TD Setup from OHLCV data.
     *
     * @param ticker Stock code
     * @param dates Date list (newest-first)
     * @param closes Close prices (newest-first)
     * @param timeframe "daily", "weekly", or "monthly"
     * @return DemarkResult or null if insufficient data
     */
    fun calculate(
        ticker: String,
        dates: List<String>,
        closes: List<Int>,
        timeframe: String = "daily"
    ): DemarkResult? {
        if (closes.size < MIN_PERIODS) {
            return null
        }

        val (sellSetup, buySetup) = calcTdSetup(closes)

        return DemarkResult(
            ticker = ticker,
            timeframe = timeframe,
            dates = dates,
            close = closes,
            sellSetup = sellSetup,
            buySetup = buySetup
        )
    }

    /**
     * Calculate TD Setup counts.
     * Python reference: demark.py _calc_td_setup()
     *
     * TD Setup Rules (matching EtfMonitor trend_signal.py _calc_td_setup):
     * - Sell Setup: Close > Close[4] 연속이면 +1, 아니면 0으로 리셋
     * - Buy Setup: Close < Close[4] 연속이면 +1, 아니면 0으로 리셋
     * - 둘은 독립적으로 계산 (동시에 값이 있을 수 있음)
     *
     * Note: Data is in reverse order (newest first)
     *
     * @param closes Close prices (newest-first)
     * @return Pair of (sellSetup, buySetup)
     */
    fun calcTdSetup(closes: List<Int>): Pair<List<Int>, List<Int>> {
        val n = closes.size
        if (n < MIN_PERIODS) {
            return Pair(List(n) { 0 }, List(n) { 0 })
        }

        // Process in chronological order (reverse the list)
        val closesChrono = closes.reversed()
        val sellChrono = IntArray(n)
        val buyChrono = IntArray(n)

        for (i in 0 until n) {
            // Sell Setup: 4일 전보다 위에 있으면 카운트 증가
            if (i >= LOOKBACK_PERIOD && closesChrono[i] > closesChrono[i - LOOKBACK_PERIOD]) {
                sellChrono[i] = sellChrono[i - 1] + 1
            } else {
                sellChrono[i] = 0
            }

            // Buy Setup: 4일 전보다 아래 있으면 카운트 증가
            if (i >= LOOKBACK_PERIOD && closesChrono[i] < closesChrono[i - LOOKBACK_PERIOD]) {
                buyChrono[i] = buyChrono[i - 1] + 1
            } else {
                buyChrono[i] = 0
            }
        }

        // Reverse back to newest-first order
        return Pair(sellChrono.toList().reversed(), buyChrono.toList().reversed())
    }

    /**
     * Get summary of current TD Setup status.
     * Python reference: demark.py get_active_setups()
     *
     * @param sellSetup Sell setup count list
     * @param buySetup Buy setup count list
     * @param dates Date list
     * @return ActiveSetups summary
     */
    fun getActiveSetups(
        sellSetup: List<Int>,
        buySetup: List<Int>,
        dates: List<String>
    ): ActiveSetups {
        if (sellSetup.isEmpty() || buySetup.isEmpty()) {
            return ActiveSetups(
                currentSell = 0,
                currentBuy = 0,
                maxSell = 0,
                maxBuy = 0,
                recentSetups = emptyList()
            )
        }

        // Current status (newest first)
        val currentSell = sellSetup.firstOrNull() ?: 0
        val currentBuy = buySetup.firstOrNull() ?: 0

        // Max values
        val maxSell = sellSetup.maxOrNull() ?: 0
        val maxBuy = buySetup.maxOrNull() ?: 0

        // List recent setups (last 20 bars)
        val recentSetups = mutableListOf<SetupEntry>()
        for (i in 0 until minOf(20, sellSetup.size)) {
            if (sellSetup[i] > 0 || buySetup[i] > 0) {
                recentSetups.add(
                    SetupEntry(
                        date = dates.getOrElse(i) { "" },
                        sell = sellSetup[i],
                        buy = buySetup[i]
                    )
                )
            }
        }

        return ActiveSetups(
            currentSell = currentSell,
            currentBuy = currentBuy,
            maxSell = maxSell,
            maxBuy = maxBuy,
            recentSetups = recentSetups
        )
    }
}
