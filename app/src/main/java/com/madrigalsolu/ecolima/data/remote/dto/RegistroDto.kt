package com.madrigalsolu.ecolima.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.madrigalsolu.ecolima.data.local.entity.EstadoRegistro
import com.madrigalsolu.ecolima.data.local.entity.RegistroEntity
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for REST API exchange. Uses both Gson (@SerializedName) and
 * kotlinx.serialization (@SerialName) so either converter works.
 *
 * JSON naming follows snake_case as per typical REST conventions,
 * while Kotlin uses camelCase.
 */
@Serializable
data class RegistroDto(
    @SerialName("id")
    @SerializedName("id")
    val id: String? = null,

    @SerialName("zona")
    @SerializedName("zona")
    val zona: String,

    @SerialName("zona_id")
    @SerializedName("zona_id")
    val zonaId: Long? = null,

    @SerialName("sector")
    @SerializedName("sector")
    val sector: String,

    @SerialName("tipo_residuo")
    @SerializedName("tipo_residuo")
    val tipoResiduo: String,

    @SerialName("peso_kg")
    @SerializedName("peso_kg")
    val pesoKg: Double,

    @SerialName("volumen_m3")
    @SerializedName("volumen_m3")
    val volumenM3: Double,

    @SerialName("foto_url")
    @SerializedName("foto_url")
    val fotoUrl: String? = null,

    @SerialName("lat")
    @SerializedName("lat")
    val lat: Double? = null,

    @SerialName("lng")
    @SerializedName("lng")
    val lng: Double? = null,

    /** ISO-8601 string, e.g. 2026-09-12T14:30:00Z */
    @SerialName("timestamp")
    @SerializedName("timestamp")
    val timestamp: String,

    /** Epoch millis alternative for simpler parsing */
    @SerialName("timestamp_ms")
    @SerializedName("timestamp_ms")
    val timestampMs: Long? = null,

    @SerialName("operario_id")
    @SerializedName("operario_id")
    val operarioId: String,

    @SerialName("observaciones")
    @SerializedName("observaciones")
    val observaciones: String? = null,

    @SerialName("epp_completo")
    @SerializedName("epp_completo")
    val eppCompleto: Boolean = false
)

/**
 * Mapping extensions between Entity <-> DTO.
 */
fun RegistroEntity.toDto(): RegistroDto {
    // ISO-8601 from epoch millis
    val iso = java.time.Instant.ofEpochMilli(timestamp).toString()
    return RegistroDto(
        id = remoteId,
        zona = zona,
        zonaId = zonaId,
        sector = sector,
        tipoResiduo = tipoResiduo.name,
        pesoKg = pesoKg,
        volumenM3 = volumenM3,
        fotoUrl = fotoUri,
        lat = lat,
        lng = lng,
        timestamp = iso,
        timestampMs = timestamp,
        operarioId = operarioId,
        observaciones = observaciones,
        eppCompleto = eppCompleto
    )
}

fun RegistroDto.toEntity(estado: EstadoRegistro = EstadoRegistro.SINCRONIZADO): RegistroEntity {
    val epoch = timestampMs ?: try {
        java.time.Instant.parse(timestamp).toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
    return RegistroEntity(
        id = 0, // Room auto-generates; server id goes to remoteId
        zona = zona,
        zonaId = zonaId,
        sector = sector,
        tipoResiduo = try { ResiduoTipo.valueOf(tipoResiduo) } catch (_: Exception) { ResiduoTipo.ORGANICO },
        pesoKg = pesoKg,
        volumenM3 = volumenM3,
        fotoUri = fotoUrl,
        lat = lat,
        lng = lng,
        timestamp = epoch,
        estado = estado,
        operarioId = operarioId,
        observaciones = observaciones,
        eppCompleto = eppCompleto,
        remoteId = id
    )
}
