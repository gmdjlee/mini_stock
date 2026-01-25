package com.stockapp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stockapp.core.db.entity.EtfEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EtfDao {
    @Query("SELECT * FROM etfs WHERE isFiltered = 1 ORDER BY etfName")
    suspend fun getFilteredEtfs(): List<EtfEntity>

    @Query("SELECT * FROM etfs WHERE isFiltered = 1 ORDER BY etfName")
    fun observeFilteredEtfs(): Flow<List<EtfEntity>>

    @Query("SELECT * FROM etfs ORDER BY etfName")
    suspend fun getAllEtfs(): List<EtfEntity>

    @Query("SELECT * FROM etfs ORDER BY etfName")
    fun observeAllEtfs(): Flow<List<EtfEntity>>

    @Query("SELECT * FROM etfs WHERE etfCode = :etfCode")
    suspend fun getByCode(etfCode: String): EtfEntity?

    @Query("SELECT * FROM etfs WHERE etfType = :type ORDER BY etfName")
    suspend fun getByType(type: String): List<EtfEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(etf: EtfEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(etfs: List<EtfEntity>)

    @Query("UPDATE etfs SET isFiltered = :isFiltered, updatedAt = :updatedAt WHERE etfCode = :etfCode")
    suspend fun updateFilterStatus(
        etfCode: String,
        isFiltered: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM etfs WHERE etfCode = :etfCode")
    suspend fun delete(etfCode: String)

    @Query("DELETE FROM etfs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM etfs")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM etfs WHERE isFiltered = 1")
    suspend fun countFiltered(): Int
}
