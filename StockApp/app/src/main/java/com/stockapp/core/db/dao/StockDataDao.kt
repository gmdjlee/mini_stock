package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.IndicatorDataEntity
import com.stockapp.core.db.entity.StockAnalysisDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockAnalysisDataDao {
    @Query("SELECT * FROM stock_analysis_data WHERE ticker = :ticker")
    suspend fun getByTicker(ticker: String): StockAnalysisDataEntity?

    @Query("SELECT * FROM stock_analysis_data WHERE ticker = :ticker")
    fun observeByTicker(ticker: String): Flow<StockAnalysisDataEntity?>

    @Query("SELECT * FROM stock_analysis_data ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<StockAnalysisDataEntity>>

    @Query("SELECT * FROM stock_analysis_data")
    suspend fun getAllOnce(): List<StockAnalysisDataEntity>

    @Query("SELECT * FROM stock_analysis_data WHERE updatedAt BETWEEN :startMs AND :endMs")
    suspend fun getInDateRange(startMs: Long, endMs: Long): List<StockAnalysisDataEntity>

    @Query("SELECT * FROM stock_analysis_data WHERE signalType IN ('STRONG_BUY', 'BUY') ORDER BY supplyRatio DESC")
    fun getBuySignals(): Flow<List<StockAnalysisDataEntity>>

    @Query("SELECT * FROM stock_analysis_data WHERE signalType IN ('STRONG_SELL', 'SELL') ORDER BY supplyRatio ASC")
    fun getSellSignals(): Flow<List<StockAnalysisDataEntity>>

    @Query("SELECT * FROM stock_analysis_data WHERE market = :market ORDER BY updatedAt DESC")
    fun getByMarket(market: String): Flow<List<StockAnalysisDataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(data: StockAnalysisDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(data: List<StockAnalysisDataEntity>)

    @Query("SELECT lastAnalyzedDate FROM stock_analysis_data WHERE ticker = :ticker")
    suspend fun getLastAnalyzedDate(ticker: String): String?

    @Query("SELECT COUNT(*) FROM stock_analysis_data")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM stock_analysis_data WHERE updatedAt > :since")
    suspend fun countUpdatedSince(since: Long): Int

    @Query("DELETE FROM stock_analysis_data WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("DELETE FROM stock_analysis_data")
    suspend fun deleteAll()

    @Query("DELETE FROM stock_analysis_data WHERE updatedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface IndicatorDataDao {
    @Query("SELECT * FROM indicator_data WHERE ticker = :ticker AND indicatorType = :type")
    suspend fun get(ticker: String, type: String): IndicatorDataEntity?

    @Query("SELECT * FROM indicator_data WHERE ticker = :ticker AND indicatorType = :type")
    fun observe(ticker: String, type: String): Flow<IndicatorDataEntity?>

    @Query("SELECT * FROM indicator_data WHERE ticker = :ticker")
    fun observeAllForTicker(ticker: String): Flow<List<IndicatorDataEntity>>

    @Query("SELECT * FROM indicator_data WHERE indicatorType = :type ORDER BY updatedAt DESC")
    fun getByType(type: String): Flow<List<IndicatorDataEntity>>

    @Query("SELECT * FROM indicator_data")
    suspend fun getAllOnce(): List<IndicatorDataEntity>

    @Query("SELECT * FROM indicator_data WHERE updatedAt BETWEEN :startMs AND :endMs")
    suspend fun getInDateRange(startMs: Long, endMs: Long): List<IndicatorDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(data: IndicatorDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(data: List<IndicatorDataEntity>)

    @Query("SELECT lastCalculatedDate FROM indicator_data WHERE ticker = :ticker AND indicatorType = :type")
    suspend fun getLastCalculatedDate(ticker: String, type: String): String?

    @Query("SELECT COUNT(*) FROM indicator_data")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM indicator_data WHERE indicatorType = :type")
    suspend fun countByType(type: String): Int

    @Query("DELETE FROM indicator_data WHERE ticker = :ticker AND indicatorType = :type")
    suspend fun delete(ticker: String, type: String)

    @Query("DELETE FROM indicator_data WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("DELETE FROM indicator_data")
    suspend fun deleteAll()

    @Query("DELETE FROM indicator_data WHERE updatedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
