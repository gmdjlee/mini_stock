package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.InvestorTradingCacheEntity

@Dao
interface InvestorTradingCacheDao {

    @Query("""
        SELECT * FROM investor_trading_cache
        WHERE ticker = :ticker AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    suspend fun getByTickerAndDateRange(
        ticker: String,
        startDate: String,
        endDate: String
    ): List<InvestorTradingCacheEntity>

    @Query("SELECT MAX(date) FROM investor_trading_cache WHERE ticker = :ticker")
    suspend fun getLatestDate(ticker: String): String?

    @Query("""
        SELECT COUNT(*) FROM investor_trading_cache
        WHERE ticker = :ticker AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun countInRange(ticker: String, startDate: String, endDate: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<InvestorTradingCacheEntity>)

    @Query("DELETE FROM investor_trading_cache WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("DELETE FROM investor_trading_cache WHERE cachedAt < :threshold")
    suspend fun deleteExpired(threshold: Long)

    @Query("SELECT COUNT(*) FROM investor_trading_cache")
    suspend fun countAll(): Int

    @Query("DELETE FROM investor_trading_cache")
    suspend fun deleteAll()
}
