package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.stockapp.core.db.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY name ASC")
    fun getAll(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks ORDER BY name ASC LIMIT :limit")
    suspend fun getAllOnce(limit: Int = 10000): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE updatedAt BETWEEN :startMs AND :endMs ORDER BY name ASC")
    suspend fun getInDateRange(startMs: Long, endMs: Long): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE ticker = :ticker")
    suspend fun getByTicker(ticker: String): StockEntity?

    @Query("SELECT * FROM stocks WHERE name LIKE '%' || :query || '%' OR ticker LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 50")
    suspend fun search(query: String): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE (name LIKE '%' || :query || '%' OR ticker LIKE '%' || :query || '%') AND market IN (:markets) LIMIT :limit")
    suspend fun searchByQuery(query: String, markets: List<String>, limit: Int = 50): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE market = :market ORDER BY name ASC")
    suspend fun getByMarket(market: String): List<StockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: StockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<StockEntity>)

    @Query("DELETE FROM stocks")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(stocks: List<StockEntity>) {
        deleteAll()
        insertAll(stocks)
    }

    @Query("SELECT COUNT(*) FROM stocks")
    suspend fun count(): Int

    @Query("SELECT MAX(updatedAt) FROM stocks")
    suspend fun lastUpdated(): Long?

    @Query("SELECT ticker FROM stocks")
    suspend fun getAllTickers(): List<String>

    @Query("DELETE FROM stocks WHERE ticker IN (:tickers)")
    suspend fun deleteByTickers(tickers: List<String>)

    /**
     * Smart sync: upsert active stocks and remove delisted ones.
     * More efficient than deleteAll() + insertAll() for 10K+ stocks.
     */
    @Transaction
    suspend fun smartSync(stocks: List<StockEntity>) {
        insertAll(stocks) // OnConflictStrategy.REPLACE = upsert
        val activeTickers = stocks.map { it.ticker }.toSet()
        val allTickers = getAllTickers()
        val inactive = allTickers.filter { it !in activeTickers }
        if (inactive.isNotEmpty()) {
            // SQLite has a limit on variables in IN clause, so batch
            inactive.chunked(500).forEach { batch ->
                deleteByTickers(batch)
            }
        }
    }
}
