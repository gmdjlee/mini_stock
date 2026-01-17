package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.ConditionCacheEntity

@Dao
interface ConditionCacheDao {

    @Query("SELECT * FROM condition_cache WHERE cache_key = :cacheKey")
    suspend fun getCache(cacheKey: String): ConditionCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ConditionCacheEntity)

    @Query("DELETE FROM condition_cache WHERE cache_key = :cacheKey")
    suspend fun delete(cacheKey: String)

    @Query("DELETE FROM condition_cache")
    suspend fun deleteAll()
}
