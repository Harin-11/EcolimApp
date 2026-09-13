package com.madrigalsolu.ecolima.data.local.entity

/**
 * Waste type classification for ECOLIM S.A.C.
 *
 * SQLite stores this as TEXT (enum name). Room TypeConverter handles
 * serialization automatically. Using an enum gives compile-time safety
 * for filtering and reporting, vs raw String.
 *
 * API equivalent: string field "tipo_residuo" with same values.
 */
enum class ResiduoTipo {
    PLASTICO,
    PAPEL_CARTON,
    ORGANICO,
    PELIGROSO,
    METAL,
    VIDRIO
}

/**
 * Sync state for offline-first flow.
 *
 * - PENDIENTE: created offline, not yet pushed to REST API.
 * - SINCRONIZADO: successfully acknowledged by server.
 * - ERROR: last sync attempt failed (kept for retry UI).
 */
enum class EstadoRegistro {
    PENDIENTE,
    SINCRONIZADO,
    ERROR
}
