package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.DailyEtfStatisticsEntity

/**
 * DAO for daily ETF statistics operations.
 */
@Dao
interface DailyEtfStatisticsDao {

    @Query("SELECT * FROM daily_etf_statistics WHERE date = :date")
    suspend fun getByDate(date: String): DailyEtfStatisticsEntity?

    @Query("SELECT * FROM daily_etf_statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getInRange(startDate: String, endDate: String): List<DailyEtfStatisticsEntity>

    @Query("SELECT * FROM daily_etf_statistics ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DailyEtfStatisticsEntity>

    @Query("SELECT * FROM daily_etf_statistics ORDER BY date DESC")
    suspend fun getAllOnce(): List<DailyEtfStatisticsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statistics: DailyEtfStatisticsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statistics: List<DailyEtfStatisticsEntity>)

    @Query("DELETE FROM daily_etf_statistics WHERE date < :date")
    suspend fun deleteOlderThan(date: String)

    @Query("DELETE FROM daily_etf_statistics")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM daily_etf_statistics")
    suspend fun count(): Int
}
