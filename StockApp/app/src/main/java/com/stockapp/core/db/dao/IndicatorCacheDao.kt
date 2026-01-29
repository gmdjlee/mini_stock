package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.IndicatorCacheEntity

@Dao
interface IndicatorCacheDao {
    @Query("SELECT * FROM indicator_cache WHERE key = :key")
    suspend fun get(key: String): IndicatorCacheEntity?

    @Query("SELECT * FROM indicator_cache WHERE ticker = :ticker AND type = :type")
    suspend fun getByTickerAndType(ticker: String, type: String): IndicatorCacheEntity?

    @Query("SELECT * FROM indicator_cache")
    suspend fun getAllOnce(): List<IndicatorCacheEntity>

    @Query("SELECT * FROM indicator_cache WHERE cachedAt BETWEEN :startMs AND :endMs")
    suspend fun getInDateRange(startMs: Long, endMs: Long): List<IndicatorCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: IndicatorCacheEntity)

    @Query("DELETE FROM indicator_cache WHERE key = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM indicator_cache WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("DELETE FROM indicator_cache WHERE cachedAt < :threshold")
    suspend fun deleteExpired(threshold: Long)

    @Query("DELETE FROM indicator_cache")
    suspend fun deleteAll()

    companion object {
        fun buildKey(ticker: String, type: String, days: Int): String = "$ticker:$type:$days"
    }
}
