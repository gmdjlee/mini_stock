package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.RealtimeSupplyCacheEntity

/**
 * DAO for realtime supply cache operations.
 */
@Dao
interface RealtimeSupplyCacheDao {

    /**
     * Insert or update cache entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RealtimeSupplyCacheEntity)

    /**
     * Get cached entry by ticker.
     */
    @Query("SELECT * FROM realtime_supply_cache WHERE ticker = :ticker")
    suspend fun get(ticker: String): RealtimeSupplyCacheEntity?

    /**
     * Delete cache entry for a ticker.
     */
    @Query("DELETE FROM realtime_supply_cache WHERE ticker = :ticker")
    suspend fun delete(ticker: String)

    /**
     * Delete all cache entries.
     */
    @Query("DELETE FROM realtime_supply_cache")
    suspend fun deleteAll()

    /**
     * Delete expired entries.
     *
     * @param expirationTime Entries cached before this time will be deleted
     */
    @Query("DELETE FROM realtime_supply_cache WHERE cachedAt < :expirationTime")
    suspend fun deleteExpired(expirationTime: Long)

    /**
     * Get all cached tickers.
     */
    @Query("SELECT ticker FROM realtime_supply_cache")
    suspend fun getAllTickers(): List<String>

    /**
     * Get cache count.
     */
    @Query("SELECT COUNT(*) FROM realtime_supply_cache")
    suspend fun getCount(): Int
}
