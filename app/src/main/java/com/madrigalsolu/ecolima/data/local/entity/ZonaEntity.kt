package com.madrigalsolu.ecolima.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a collection zone for ECOLIM S.A.C.
 *
 * SQLite advantage: local lookup table enforces referential integrity
 * via foreign key in [RegistroEntity.zonaId] -> [ZonaEntity.id],
 * and allows offline autocomplete without network.
 *
 * Synced occasionally via GET /zonas; rarely changes.
 */
@Entity(
    tableName = "zonas",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class ZonaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val sector: String,
    val descripcion: String? = null,
    val activa: Boolean = true
)
