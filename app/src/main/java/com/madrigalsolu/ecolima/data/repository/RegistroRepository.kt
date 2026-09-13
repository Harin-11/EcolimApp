package com.madrigalsolu.ecolima.data.repository

import com.madrigalsolu.ecolima.data.local.dao.RegistroDao
import com.madrigalsolu.ecolima.data.local.dao.ZonaDao
import com.madrigalsolu.ecolima.data.local.entity.EstadoRegistro
import com.madrigalsolu.ecolima.data.local.entity.RegistroEntity
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import com.madrigalsolu.ecolima.data.remote.api.EcolimApi
import com.madrigalsolu.ecolima.data.remote.dto.toDto
import com.madrigalsolu.ecolima.data.remote.dto.toEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository combining Room (SQLite) + Retrofit (REST API) for offline-first.
 *
 * ### Pattern advantages
 * - **Single source of truth for UI**: Room Flow (SQLite) -> Compose observes locally.
 * - **Write-through**: [insertLocal] writes to SQLite with PENDIENTE state instantly.
 * - **Sync**: [syncPending] batches PENDIENTE rows, POSTs to API, marks SINCRONIZADO on success.
 *   WorkManager calls this when network is available.
 * - **Read-through**: [refreshFromRemote] pulls server page and replaces/inserts locally.
 *
 * Error handling: network failures keep rows PENDIENTE for retry; server-side
 * validation errors mark ERROR with message for UI correction.
 */
class RegistroRepository(
    private val registroDao: RegistroDao,
    private val zonaDao: ZonaDao,
    private val api: EcolimApi
) {

    // ---- Local ----

    fun getAll(): Flow<List<RegistroEntity>> = registroDao.getAll()

    fun getFiltered(
        tipo: ResiduoTipo? = null,
        estado: EstadoRegistro? = null,
        zona: String? = null,
        fromTs: Long? = null,
        toTs: Long? = null,
        operarioId: String? = null
    ): Flow<List<RegistroEntity>> =
        registroDao.getByFilter(tipo, estado, zona, fromTs, toTs, operarioId)

    fun countPending(): Flow<Int> = registroDao.countPending()

    suspend fun insertLocal(entity: RegistroEntity): Long {
        // Ensure PENDIENTE so sync picks it up
        val toInsert = if (entity.estado == EstadoRegistro.SINCRONIZADO) {
            entity.copy(estado = EstadoRegistro.PENDIENTE, remoteId = null)
        } else entity
        return registroDao.insert(toInsert)
    }

    suspend fun updateLocal(entity: RegistroEntity) = registroDao.update(entity)

    suspend fun deleteLocal(entity: RegistroEntity) = registroDao.delete(entity)

    // ---- Sync ----

    /**
     * Push all pending registros to REST API.
     * @return Result with counts of synced/failed.
     */
    suspend fun syncPending(): SyncResult {
        val pending = registroDao.getPendingSync()
        if (pending.isEmpty()) return SyncResult(0, 0, emptyList())

        val dtos = pending.map { it.toDto() }

        return try {
            val response = api.syncRegistros(dtos)

            // Mark successes
            var syncedCount = 0
            response.synced.forEachIndexed { index, item ->
                // Map by order if server doesn't echo local id
                val local = pending.getOrNull(index) ?: return@forEachIndexed
                // Prefer explicit localTempId mapping if provided
                val target = if (item.localTempId != null) {
                    pending.find { it.id.toString() == item.localTempId }
                } else local
                if (target != null) {
                    registroDao.markSynced(target.id, item.remoteId)
                    syncedCount++
                }
            }
            // If server returns same order without localTempId, assume 1:1
            if (response.synced.size == pending.size && response.failed.isEmpty()) {
                // Already handled above; nothing extra needed
            }

            // Mark failures
            response.failed.forEach { fail ->
                val target = pending.find { it.id.toString() == fail.localTempId }
                    ?: pending.firstOrNull()
                if (target != null) {
                    registroDao.updateEstado(target.id, EstadoRegistro.ERROR, fail.error)
                }
            }

            SyncResult(
                synced = syncedCount,
                failed = response.failed.size,
                errors = response.failed.map { it.error }
            )
        } catch (e: Exception) {
            // Network error: keep as PENDIENTE, optionally annotate lastSyncError
            pending.forEach { entity ->
                registroDao.updateEstado(entity.id, EstadoRegistro.PENDIENTE, e.message)
            }
            SyncResult(0, pending.size, listOf(e.message ?: "Unknown network error"))
        }
    }

    /**
     * Pull latest registros from API and insert locally as SINCRONIZADO.
     */
    suspend fun refreshFromRemote(
        tipoResiduo: String? = null,
        zona: String? = null
    ) {
        val page = api.getRegistros(tipoResiduo = tipoResiduo, zona = zona)
        val entities = page.data.map { it.toEntity(EstadoRegistro.SINCRONIZADO) }
        registroDao.insertAll(entities)
    }

    // ---- Reports ----

    /**
     * Generate aggregated report from local DB (works offline).
     * Mirrors server-side GET /reportes aggregation.
     */
    suspend fun generateReport(
        fromTs: Long,
        toTs: Long,
        tipo: ResiduoTipo? = null
    ): ReportSummary {
        val filtered = registroDao.getByFilterOnce(tipo = tipo, fromTs = fromTs, toTs = toTs)
        val totalPeso = filtered.sumOf { it.pesoKg }
        val totalVol = filtered.sumOf { it.volumenM3 }
        val byTipo = filtered.groupBy { it.tipoResiduo }
            .mapValues { (_, list) -> list.sumOf { it.pesoKg } }
        val byZona = filtered.groupBy { it.zona }
            .mapValues { (_, list) -> list.sumOf { it.pesoKg } }

        return ReportSummary(
            totalRegistros = filtered.size,
            totalPesoKg = totalPeso,
            totalVolumenM3 = totalVol,
            pesoPorTipo = byTipo,
            pesoPorZona = byZona,
            registros = filtered
        )
    }
}

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val synced: Int,
    val failed: Int,
    val errors: List<String>
)

/**
 * Local aggregation for offline report screens / PDF export.
 */
data class ReportSummary(
    val totalRegistros: Int,
    val totalPesoKg: Double,
    val totalVolumenM3: Double,
    val pesoPorTipo: Map<ResiduoTipo, Double>,
    val pesoPorZona: Map<String, Double>,
    val registros: List<RegistroEntity>
)
