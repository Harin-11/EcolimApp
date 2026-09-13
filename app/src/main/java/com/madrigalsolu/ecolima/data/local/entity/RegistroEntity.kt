package com.madrigalsolu.ecolima.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Core offline-first entity for ECOLIM S.A.C. collection records.
 *
 * ### SQLite vs API trade-offs (KDoc for deliverable)
 * - **SQLite (Room)**: instant local insert, works without connectivity
 *   (operarios en campo sin señal), transactional, query via Flow.
 *   Indices on [timestamp], [tipoResiduo], [estado] speed up filtered reports.
 * - **REST API**: source of truth for cross-device sync and server-side analytics.
 *   Sync is batched via POST /registros; conflicts resolved by server timestamp.
 * - **Offline-first pattern**: write to SQLite first (PENDIENTE), then
 *   [com.madrigalsolu.ecolima.data.work.SyncWorker] pushes pending rows
 *   when network is available. Advantage: perceived latency = 0, resilience
 *   to flaky mobile networks. Cost: local storage + eventual consistency.
 *
 * Foreign key to [ZonaEntity] enforces ER diagram (Zona 1--N Registro).
 */
@Entity(
    tableName = "registros",
    indices = [
        Index(value = ["zona"]),
        Index(value = ["zonaId"]),
        Index(value = ["tipoResiduo"]),
        Index(value = ["timestamp"]),
        Index(value = ["estado"]),
        Index(value = ["operarioId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ZonaEntity::class,
            parentColumns = ["id"],
            childColumns = ["zonaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class RegistroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Human-readable zone name (denormalized for quick display offline) */
    val zona: String,

    /** FK to zonas table; nullable if zone was free-text */
    @ColumnInfo(name = "zonaId")
    val zonaId: Long? = null,

    val sector: String,

    @ColumnInfo(name = "tipoResiduo")
    val tipoResiduo: ResiduoTipo,

    /** Weight in kilograms */
    val pesoKg: Double,

    /** Volume in cubic meters */
    val volumenM3: Double,

    /** Local content:// or file:// URI for evidence photo */
    val fotoUri: String? = null,

    val lat: Double? = null,
    val lng: Double? = null,

    /** Epoch millis */
    val timestamp: Long,

    /** Sync state: PENDIENTE until server acknowledges */
    val estado: EstadoRegistro = EstadoRegistro.PENDIENTE,

    /** Operator / user that created the record */
    val operarioId: String,

    val observaciones: String? = null,

    /** Whether PPE checklist was completed */
    val eppCompleto: Boolean = false,

    /** Server-side ID after sync (null until SINCRONIZADO) */
    val remoteId: String? = null,

    /** Last sync attempt error message, if any */
    val lastSyncError: String? = null
)
