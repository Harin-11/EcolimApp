package com.madrigalsolu.ecolima.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.madrigalsolu.ecolima.data.local.entity.ZonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZonaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zona: ZonaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(zonas: List<ZonaEntity>)

    @Query("SELECT * FROM zonas WHERE activa = 1 ORDER BY nombre ASC")
    fun getAllActive(): Flow<List<ZonaEntity>>

    @Query("SELECT * FROM zonas ORDER BY nombre ASC")
    suspend fun getAllOnce(): List<ZonaEntity>

    @Query("SELECT * FROM zonas WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ZonaEntity?

    @Query("DELETE FROM zonas")
    suspend fun clearAll()
}
