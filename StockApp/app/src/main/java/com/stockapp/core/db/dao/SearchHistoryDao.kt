package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    suspend fun getRecentList(limit: Int = 20): List<SearchHistoryEntity>

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC")
    suspend fun getAllOnce(): List<SearchHistoryEntity>

    @Query("SELECT * FROM search_history WHERE searchedAt BETWEEN :startMs AND :endMs ORDER BY searchedAt DESC")
    suspend fun getInDateRange(startMs: Long, endMs: Long): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY searchedAt DESC LIMIT :keepCount)")
    suspend fun trimToSize(keepCount: Int = 50)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}
