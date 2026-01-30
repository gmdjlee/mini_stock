package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.AnalysisCacheEntity

@Dao
interface AnalysisCacheDao {
    @Query("SELECT * FROM analysis_cache WHERE ticker = :ticker")
    suspend fun get(ticker: String): AnalysisCacheEntity?

    @Query("SELECT * FROM analysis_cache")
    suspend fun getAllOnce(): List<AnalysisCacheEntity>

    @Query("SELECT * FROM analysis_cache WHERE cachedAt BETWEEN :startMs AND :endMs")
    suspend fun getInDateRange(startMs: Long, endMs: Long): List<AnalysisCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: AnalysisCacheEntity)

    @Query("DELETE FROM analysis_cache WHERE ticker = :ticker")
    suspend fun delete(ticker: String)

    @Query("DELETE FROM analysis_cache WHERE cachedAt < :threshold")
    suspend fun deleteExpired(threshold: Long)

    @Query("DELETE FROM analysis_cache")
    suspend fun deleteAll()
}
