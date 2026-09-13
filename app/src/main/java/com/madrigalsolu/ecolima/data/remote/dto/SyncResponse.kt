package com.madrigalsolu.ecolima.data.remote.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for POST /registros batch sync.
 */
@Serializable
data class SyncResponse(
    @SerialName("success")
    @SerializedName("success")
    val success: Boolean,

    @SerialName("synced")
    @SerializedName("synced")
    val synced: List<SyncedItem> = emptyList(),

    @SerialName("failed")
    @SerializedName("failed")
    val failed: List<FailedItem> = emptyList(),

    @SerialName("message")
    @SerializedName("message")
    val message: String? = null
)

@Serializable
data class SyncedItem(
    @SerialName("local_temp_id")
    @SerializedName("local_temp_id")
    val localTempId: String? = null,

    @SerialName("remote_id")
    @SerializedName("remote_id")
    val remoteId: String,

    @SerialName("timestamp")
    @SerializedName("timestamp")
    val timestamp: String? = null
)

@Serializable
data class FailedItem(
    @SerialName("local_temp_id")
    @SerializedName("local_temp_id")
    val localTempId: String? = null,

    @SerialName("error")
    @SerializedName("error")
    val error: String,

    @SerialName("code")
    @SerializedName("code")
    val code: Int? = null
)

/**
 * Paginated wrapper for GET /registros
 */
@Serializable
data class RegistroPageResponse(
    @SerialName("data")
    @SerializedName("data")
    val data: List<RegistroDto>,

    @SerialName("total")
    @SerializedName("total")
    val total: Int,

    @SerialName("page")
    @SerializedName("page")
    val page: Int,

    @SerialName("page_size")
    @SerializedName("page_size")
    val pageSize: Int
)

/**
 * Report DTO for GET /reportes
 */
@Serializable
data class ReporteDto(
    @SerialName("zona")
    @SerializedName("zona")
    val zona: String? = null,

    @SerialName("tipo_residuo")
    @SerializedName("tipo_residuo")
    val tipoResiduo: String? = null,

    @SerialName("total_peso_kg")
    @SerializedName("total_peso_kg")
    val totalPesoKg: Double,

    @SerialName("total_volumen_m3")
    @SerializedName("total_volumen_m3")
    val totalVolumenM3: Double,

    @SerialName("cantidad_registros")
    @SerializedName("cantidad_registros")
    val cantidadRegistros: Int,

    @SerialName("desde")
    @SerializedName("desde")
    val desde: String,

    @SerialName("hasta")
    @SerializedName("hasta")
    val hasta: String
)
