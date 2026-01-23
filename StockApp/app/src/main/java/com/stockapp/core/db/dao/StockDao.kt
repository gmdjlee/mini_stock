package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY name ASC")
    fun getAll(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks ORDER BY name ASC LIMIT :limit")
    suspend fun getAllOnce(limit: Int = 10000): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE ticker = :ticker")
    suspend fun getByTicker(ticker: String): StockEntity?

    @Query("SELECT * FROM stocks WHERE name LIKE '%' || :query || '%' OR ticker LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 50")
    suspend fun search(query: String): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE market = :market ORDER BY name ASC")
    suspend fun getByMarket(market: String): List<StockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: StockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<StockEntity>)

    @Query("DELETE FROM stocks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM stocks")
    suspend fun count(): Int

    @Query("SELECT MAX(updatedAt) FROM stocks")
    suspend fun lastUpdated(): Long?
}
