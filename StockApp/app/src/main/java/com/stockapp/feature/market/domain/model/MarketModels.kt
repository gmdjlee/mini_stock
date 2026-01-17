package com.stockapp.feature.market.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========== Market Indicators Domain Model ==========

/**
 * Combined market indicators data.
 */
data class MarketIndicators(
    val dates: List<String>,
    val deposit: List<Long>,          // 고객예탁금
    val creditLoan: List<Long>,       // 신용융자
    val creditBalance: List<Long>,    // 신용잔고
    val creditRatio: List<Double>     // 신용비율
)

// ========== DTO Classes ==========

@Serializable
data class MarketIndicatorsResponse(
    val ok: Boolean,
    val data: MarketIndicatorsDto? = null,
    val error: MarketError? = null
)

@Serializable
data class MarketIndicatorsDto(
    val dates: List<String>,
    val deposit: List<Long>,
    @SerialName("credit_loan") val creditLoan: List<Long>,
    @SerialName("credit_balance") val creditBalance: List<Long>,
    @SerialName("credit_ratio") val creditRatio: List<Double>
) {
    fun toDomain(): MarketIndicators = MarketIndicators(
        dates = dates,
        deposit = deposit,
        creditLoan = creditLoan,
        creditBalance = creditBalance,
        creditRatio = creditRatio
    )
}

@Serializable
data class DepositResponse(
    val ok: Boolean,
    val data: DepositDto? = null,
    val error: MarketError? = null
)

@Serializable
data class DepositDto(
    val dates: List<String>,
    val deposit: List<Long>,
    @SerialName("credit_loan") val creditLoan: List<Long>
)

@Serializable
data class CreditResponse(
    val ok: Boolean,
    val data: CreditDto? = null,
    val error: MarketError? = null
)

@Serializable
data class CreditDto(
    val dates: List<String>,
    @SerialName("credit_balance") val creditBalance: List<Long>,
    @SerialName("credit_ratio") val creditRatio: List<Double>
)

@Serializable
data class MarketError(
    val code: String,
    val msg: String
)

// ========== Summary for UI ==========

/**
 * Market indicators summary for UI display.
 */
data class MarketSummary(
    val dates: List<String>,
    // Current values (most recent)
    val currentDeposit: Long,
    val currentCreditLoan: Long,
    val currentCreditBalance: Long,
    val currentCreditRatio: Double,
    // Previous day values for comparison
    val prevDeposit: Long,
    val prevCreditLoan: Long,
    val prevCreditBalance: Long,
    val prevCreditRatio: Double,
    // History for charts
    val depositHistory: List<Long>,
    val creditLoanHistory: List<Long>,
    val creditBalanceHistory: List<Long>,
    val creditRatioHistory: List<Double>
) {
    // Changes
    val depositChange: Long get() = currentDeposit - prevDeposit
    val creditLoanChange: Long get() = currentCreditLoan - prevCreditLoan
    val creditBalanceChange: Long get() = currentCreditBalance - prevCreditBalance
    val creditRatioChange: Double get() = currentCreditRatio - prevCreditRatio

    // Formatted values (조 단위)
    val depositFormatted: String
        get() = formatTrillion(currentDeposit)

    val creditLoanFormatted: String
        get() = formatTrillion(currentCreditLoan)

    val creditBalanceFormatted: String
        get() = formatTrillion(currentCreditBalance)

    val creditRatioFormatted: String
        get() = String.format("%.2f%%", currentCreditRatio)

    // Change direction
    val depositChangeDirection: ChangeDirection
        get() = getChangeDirection(depositChange)

    val creditLoanChangeDirection: ChangeDirection
        get() = getChangeDirection(creditLoanChange)

    val creditBalanceChangeDirection: ChangeDirection
        get() = getChangeDirection(creditBalanceChange)

    val creditRatioChangeDirection: ChangeDirection
        get() = getChangeDirection(creditRatioChange)
}

enum class ChangeDirection {
    UP, DOWN, NEUTRAL
}

private fun getChangeDirection(change: Long): ChangeDirection = when {
    change > 0 -> ChangeDirection.UP
    change < 0 -> ChangeDirection.DOWN
    else -> ChangeDirection.NEUTRAL
}

private fun getChangeDirection(change: Double): ChangeDirection = when {
    change > 0.01 -> ChangeDirection.UP
    change < -0.01 -> ChangeDirection.DOWN
    else -> ChangeDirection.NEUTRAL
}

private fun formatTrillion(value: Long): String {
    val trillion = value / 1_000_000_000_000.0
    return String.format("%.1f조", trillion)
}

/**
 * Convert MarketIndicators to MarketSummary.
 */
fun MarketIndicators.toSummary(): MarketSummary {
    val n = dates.size
    return MarketSummary(
        dates = dates,
        currentDeposit = deposit.firstOrNull() ?: 0L,
        currentCreditLoan = creditLoan.firstOrNull() ?: 0L,
        currentCreditBalance = creditBalance.firstOrNull() ?: 0L,
        currentCreditRatio = creditRatio.firstOrNull() ?: 0.0,
        prevDeposit = if (n > 1) deposit[1] else 0L,
        prevCreditLoan = if (n > 1) creditLoan[1] else 0L,
        prevCreditBalance = if (n > 1) creditBalance[1] else 0L,
        prevCreditRatio = if (n > 1) creditRatio[1] else 0.0,
        depositHistory = deposit,
        creditLoanHistory = creditLoan,
        creditBalanceHistory = creditBalance,
        creditRatioHistory = creditRatio
    )
}
