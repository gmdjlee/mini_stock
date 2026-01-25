package com.stockapp.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily ETF statistics entity.
 * Stores aggregated statistics for ETF constituent changes per day.
 */
@Entity(
    tableName = "daily_etf_statistics",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyEtfStatisticsEntity(
    @PrimaryKey
    val date: String,                     // YYYY-MM-DD
    val newStockCount: Int,               // 신규 편입 종목 수
    val newStockAmount: Long,             // 신규 편입 총 금액
    val removedStockCount: Int,           // 편출 종목 수
    val removedStockAmount: Long,         // 편출 총 금액
    val increasedStockCount: Int,         // 비중 증가 종목 수
    val increasedStockAmount: Long,       // 비중 증가 총 금액
    val decreasedStockCount: Int,         // 비중 감소 종목 수
    val decreasedStockAmount: Long,       // 비중 감소 총 금액
    val cashDepositAmount: Long,          // 예금 총액
    val cashDepositChange: Long,          // 예금 변동액
    val cashDepositChangeRate: Double,    // 예금 변동률 (%)
    val totalEtfCount: Int,               // 총 ETF 수
    val totalHoldingAmount: Long,         // 총 보유 금액
    val calculatedAt: Long                // 계산 시점
)
