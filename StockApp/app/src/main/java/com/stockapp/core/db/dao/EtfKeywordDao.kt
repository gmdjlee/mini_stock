package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.stockapp.core.db.entity.EtfKeywordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EtfKeywordDao {
    @Query("SELECT * FROM etf_keywords WHERE isEnabled = 1 ORDER BY filterType, keyword")
    suspend fun getEnabledKeywords(): List<EtfKeywordEntity>

    @Query("SELECT * FROM etf_keywords WHERE isEnabled = 1 ORDER BY filterType, keyword")
    fun observeEnabledKeywords(): Flow<List<EtfKeywordEntity>>

    @Query("SELECT * FROM etf_keywords ORDER BY filterType, keyword")
    suspend fun getAllKeywords(): List<EtfKeywordEntity>

    @Query("SELECT * FROM etf_keywords ORDER BY filterType, keyword")
    fun observeAllKeywords(): Flow<List<EtfKeywordEntity>>

    @Query("SELECT * FROM etf_keywords WHERE filterType = :type AND isEnabled = 1 ORDER BY keyword")
    suspend fun getKeywordsByType(type: String): List<EtfKeywordEntity>

    @Query("SELECT * FROM etf_keywords WHERE filterType = :type ORDER BY keyword")
    fun observeKeywordsByType(type: String): Flow<List<EtfKeywordEntity>>

    @Query("SELECT * FROM etf_keywords WHERE id = :id")
    suspend fun getById(id: Long): EtfKeywordEntity?

    @Insert
    suspend fun insert(keyword: EtfKeywordEntity): Long

    @Delete
    suspend fun delete(keyword: EtfKeywordEntity)

    @Query("DELETE FROM etf_keywords WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE etf_keywords SET isEnabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE etf_keywords SET keyword = :keyword WHERE id = :id")
    suspend fun updateKeyword(id: Long, keyword: String)

    @Query("DELETE FROM etf_keywords WHERE filterType = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM etf_keywords")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM etf_keywords")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM etf_keywords WHERE filterType = :type AND isEnabled = 1")
    suspend fun countEnabledByType(type: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM etf_keywords WHERE keyword = :keyword AND filterType = :type)")
    suspend fun exists(keyword: String, type: String): Boolean
}
