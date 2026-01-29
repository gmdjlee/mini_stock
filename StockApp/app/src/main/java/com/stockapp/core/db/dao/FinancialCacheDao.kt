package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.FinancialCacheEntity

@Dao
interface FinancialCacheDao {
    @Query("SELECT * FROM financial_cache WHERE ticker = :ticker")
    suspend fun get(ticker: String): FinancialCacheEntity?

    @Query("SELECT * FROM financial_cache")
    suspend fun getAllOnce(): List<FinancialCacheEntity>

    @Query("SELECT * FROM financial_cache WHERE cachedAt BETWEEN :startMs AND :endMs")
    suspend fun getInDateRange(startMs: Long, endMs: Long): List<FinancialCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: FinancialCacheEntity)

    @Query("DELETE FROM financial_cache WHERE ticker = :ticker")
    suspend fun delete(ticker: String)

    @Query("DELETE FROM financial_cache WHERE cachedAt < :threshold")
    suspend fun deleteExpired(threshold: Long)

    @Query("DELETE FROM financial_cache")
    suspend fun deleteAll()
}
