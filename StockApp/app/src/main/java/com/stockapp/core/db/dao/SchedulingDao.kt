package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.stockapp.core.db.entity.SchedulingConfigEntity
import com.stockapp.core.db.entity.SyncHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchedulingConfigDao {
    @Query("SELECT * FROM scheduling_config WHERE id = 1")
    fun getConfig(): Flow<SchedulingConfigEntity?>

    @Query("SELECT * FROM scheduling_config WHERE id = 1")
    suspend fun getConfigOnce(): SchedulingConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: SchedulingConfigEntity)

    @Query("UPDATE scheduling_config SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = 1")
    suspend fun setEnabled(enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE scheduling_config SET syncHour = :hour, syncMinute = :minute, updatedAt = :updatedAt WHERE id = 1")
    suspend fun setSyncTime(hour: Int, minute: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE scheduling_config SET lastSyncAt = :syncedAt, lastSyncStatus = :status, lastSyncMessage = :message, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateLastSync(
        syncedAt: Long,
        status: String,
        message: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
}

@Dao
interface SyncHistoryDao {
    @Query("SELECT * FROM sync_history ORDER BY syncedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 10): Flow<List<SyncHistoryEntity>>

    @Query("SELECT * FROM sync_history ORDER BY syncedAt DESC LIMIT 1")
    suspend fun getLatestSync(): SyncHistoryEntity?

    @Insert
    suspend fun insert(history: SyncHistoryEntity): Long

    @Query("UPDATE sync_history SET status = :status, stockCount = :stockCount, analysisCount = :analysisCount, indicatorCount = :indicatorCount, etfCount = :etfCount, etfConstituentCount = :etfConstituentCount, errorMessage = :errorMessage, durationMs = :durationMs WHERE id = :id")
    suspend fun updateSync(
        id: Long,
        status: String,
        stockCount: Int,
        analysisCount: Int,
        indicatorCount: Int,
        etfCount: Int,
        etfConstituentCount: Int,
        errorMessage: String?,
        durationMs: Long
    )

    @Query("DELETE FROM sync_history WHERE id NOT IN (SELECT id FROM sync_history ORDER BY syncedAt DESC LIMIT :keepCount)")
    suspend fun trimHistory(keepCount: Int = 50)

    @Query("DELETE FROM sync_history")
    suspend fun deleteAll()
}
