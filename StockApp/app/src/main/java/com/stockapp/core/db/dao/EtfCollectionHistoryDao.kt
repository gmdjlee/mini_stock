package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.stockapp.core.db.entity.EtfCollectionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EtfCollectionHistoryDao {
    @Query("SELECT * FROM etf_collection_history ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 10): List<EtfCollectionHistoryEntity>

    @Query("SELECT * FROM etf_collection_history ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecentHistory(limit: Int = 10): Flow<List<EtfCollectionHistoryEntity>>

    @Query("SELECT * FROM etf_collection_history WHERE collectedDate = :date")
    suspend fun getByDate(date: String): EtfCollectionHistoryEntity?

    @Query("SELECT * FROM etf_collection_history ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatest(): EtfCollectionHistoryEntity?

    @Query("SELECT * FROM etf_collection_history ORDER BY startedAt DESC LIMIT 1")
    fun observeLatest(): Flow<EtfCollectionHistoryEntity?>

    @Query("SELECT * FROM etf_collection_history WHERE status = :status ORDER BY startedAt DESC")
    suspend fun getByStatus(status: String): List<EtfCollectionHistoryEntity>

    @Insert
    suspend fun insert(history: EtfCollectionHistoryEntity): Long

    @Query("""
        UPDATE etf_collection_history
        SET status = :status,
            totalEtfs = :totalEtfs,
            totalConstituents = :totalConstituents,
            errorMessage = :errorMessage,
            completedAt = :completedAt
        WHERE id = :id
    """)
    suspend fun updateCompletion(
        id: Long,
        status: String,
        totalEtfs: Int,
        totalConstituents: Int,
        errorMessage: String?,
        completedAt: Long
    )

    @Query("DELETE FROM etf_collection_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        DELETE FROM etf_collection_history
        WHERE id NOT IN (
            SELECT id FROM etf_collection_history
            ORDER BY startedAt DESC
            LIMIT :keepCount
        )
    """)
    suspend fun trimHistory(keepCount: Int = 30)

    @Query("DELETE FROM etf_collection_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM etf_collection_history")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM etf_collection_history WHERE status = :status")
    suspend fun countByStatus(status: String): Int
}
