package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.MarketIndicatorCacheEntity

@Dao
interface MarketIndicatorCacheDao {

    @Query("SELECT * FROM market_indicator_cache WHERE `key` = :key AND cachedAt > :minTimestamp LIMIT 1")
    suspend fun getIfFresh(key: String, minTimestamp: Long): MarketIndicatorCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MarketIndicatorCacheEntity)

    @Query("DELETE FROM market_indicator_cache WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM market_indicator_cache")
    suspend fun deleteAll()
}
