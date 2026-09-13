package com.madrigalsolu.ecolima.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.madrigalsolu.ecolima.data.local.entity.EstadoRegistro
import com.madrigalsolu.ecolima.data.local.entity.RegistroEntity
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import kotlinx.coroutines.flow.Flow

/**
 * DAO for offline-first CRUD + sync queries.
 *
 * All queries are designed for Room's SQLite engine with indices defined
 * in [RegistroEntity]. Flow returns observe local DB reactively (Compose).
 */
@Dao
interface RegistroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RegistroEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RegistroEntity>): List<Long>

    @Update
    suspend fun update(entity: RegistroEntity)

    @Delete
    suspend fun delete(entity: RegistroEntity)

    @Query("DELETE FROM registros WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM registros")
    suspend fun clearAll()

    @Query("SELECT * FROM registros WHERE (remoteId IS NULL OR remoteId NOT IN ('REC-1019', 'REC-1020', 'REC-1021', 'REC-1022', 'REC-1023')) AND operarioId != 'operario@ecolim.pe' ORDER BY timestamp DESC")
    fun getAll(): Flow<List<RegistroEntity>>

    @Query("SELECT * FROM registros WHERE (remoteId IS NULL OR remoteId NOT IN ('REC-1019', 'REC-1020', 'REC-1021', 'REC-1022', 'REC-1023')) AND operarioId != 'operario@ecolim.pe' ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<RegistroEntity>

    @Query("SELECT * FROM registros WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RegistroEntity?

    /**
     * Filtered query for reports/history.
     * Null parameters mean "no filter" (COALESCE pattern with Room).
     */
    @Query(
        """
        SELECT * FROM registros
        WHERE (remoteId IS NULL OR remoteId NOT IN ('REC-1019', 'REC-1020', 'REC-1021', 'REC-1022', 'REC-1023'))
          AND operarioId != 'operario@ecolim.pe'
          AND (:tipo IS NULL OR tipoResiduo = :tipo)
          AND (:estado IS NULL OR estado = :estado)
          AND (:zona IS NULL OR zona = :zona)
          AND (:fromTs IS NULL OR timestamp >= :fromTs)
          AND (:toTs IS NULL OR timestamp <= :toTs)
          AND (:operarioId IS NULL OR operarioId = :operarioId)
        ORDER BY timestamp DESC
        """
    )
    fun getByFilter(
        tipo: ResiduoTipo? = null,
        estado: EstadoRegistro? = null,
        zona: String? = null,
        fromTs: Long? = null,
        toTs: Long? = null,
        operarioId: String? = null
    ): Flow<List<RegistroEntity>>

    /** One-shot version for report generation */
    @Query(
        """
        SELECT * FROM registros
        WHERE (:tipo IS NULL OR tipoResiduo = :tipo)
          AND (:fromTs IS NULL OR timestamp >= :fromTs)
          AND (:toTs IS NULL OR timestamp <= :toTs)
        ORDER BY timestamp DESC
        """
    )
    suspend fun getByFilterOnce(
        tipo: ResiduoTipo? = null,
        fromTs: Long? = null,
        toTs: Long? = null
    ): List<RegistroEntity>

    /** Records awaiting synchronization (offline-first core). */
    @Query("SELECT * FROM registros WHERE estado = 'PENDIENTE' OR estado = 'ERROR' ORDER BY timestamp ASC")
    suspend fun getPendingSync(): List<RegistroEntity>

    @Query("SELECT * FROM registros WHERE estado = :estado ORDER BY timestamp DESC")
    fun getByEstado(estado: EstadoRegistro): Flow<List<RegistroEntity>>

    @Query("SELECT COUNT(*) FROM registros WHERE estado = 'PENDIENTE'")
    fun countPending(): Flow<Int>

    @Query("UPDATE registros SET estado = :estado, lastSyncError = :error WHERE id = :id")
    suspend fun updateEstado(id: Long, estado: EstadoRegistro, error: String? = null)

    @Query("UPDATE registros SET estado = 'SINCRONIZADO', remoteId = :remoteId, lastSyncError = NULL WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: String)

    // Aggregation helpers for dashboard
    @Query("SELECT COALESCE(SUM(pesoKg), 0) FROM registros WHERE timestamp >= :fromTs AND timestamp <= :toTs")
    suspend fun sumPesoKg(fromTs: Long, toTs: Long): Double

    @Query("SELECT COALESCE(SUM(volumenM3), 0) FROM registros WHERE timestamp >= :fromTs AND timestamp <= :toTs")
    suspend fun sumVolumen(fromTs: Long, toTs: Long): Double
}
