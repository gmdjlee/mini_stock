package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.OhlcvCacheEntity

@Dao
interface OhlcvCacheDao {

    @Query("""
        SELECT * FROM ohlcv_cache
        WHERE ticker = :ticker AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    suspend fun getByTickerAndDateRange(
        ticker: String,
        startDate: String,
        endDate: String
    ): List<OhlcvCacheEntity>

    @Query("SELECT MAX(date) FROM ohlcv_cache WHERE ticker = :ticker")
    suspend fun getLatestDate(ticker: String): String?

    @Query("SELECT MIN(date) FROM ohlcv_cache WHERE ticker = :ticker")
    suspend fun getOldestDate(ticker: String): String?

    @Query("""
        SELECT COUNT(*) FROM ohlcv_cache
        WHERE ticker = :ticker AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun countInRange(ticker: String, startDate: String, endDate: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bars: List<OhlcvCacheEntity>)

    @Query("DELETE FROM ohlcv_cache WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("DELETE FROM ohlcv_cache WHERE ticker = :ticker AND date < :cutoffDate")
    suspend fun deleteOldData(ticker: String, cutoffDate: String)

    @Query("DELETE FROM ohlcv_cache WHERE cachedAt < :threshold")
    suspend fun deleteExpired(threshold: Long)

    @Query("SELECT COUNT(*) FROM ohlcv_cache")
    suspend fun countAll(): Int

    @Query("SELECT DISTINCT ticker FROM ohlcv_cache")
    suspend fun getDistinctTickers(): List<String>

    @Query("DELETE FROM ohlcv_cache")
    suspend fun deleteAll()
}
