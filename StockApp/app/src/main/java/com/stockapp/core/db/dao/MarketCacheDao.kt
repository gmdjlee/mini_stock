package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.MarketCacheEntity

@Dao
interface MarketCacheDao {

    @Query("SELECT * FROM market_cache WHERE cache_key = :cacheKey")
    suspend fun getCache(cacheKey: String): MarketCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: MarketCacheEntity)

    @Query("DELETE FROM market_cache WHERE cache_key = :cacheKey")
    suspend fun delete(cacheKey: String)

    @Query("DELETE FROM market_cache")
    suspend fun deleteAll()
}
