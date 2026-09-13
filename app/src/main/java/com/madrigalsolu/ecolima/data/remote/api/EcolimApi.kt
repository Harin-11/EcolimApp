package com.madrigalsolu.ecolima.data.remote.api

import com.madrigalsolu.ecolima.data.remote.dto.RegistroDto
import com.madrigalsolu.ecolima.data.remote.dto.RegistroPageResponse
import com.madrigalsolu.ecolima.data.remote.dto.ReporteDto
import com.madrigalsolu.ecolima.data.remote.dto.SyncResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for ECOLIM S.A.C. REST API.
 *
 * Base URL example: https://api.ecolim.example.com/api/v1/
 * Configure via DI module with OkHttp logging interceptor.
 *
 * All endpoints are suspend functions for coroutine integration.
 */
interface EcolimApi {

    /**
     * Batch sync: push pending local registros to server.
     * Server should be idempotent (dedup by local_temp_id or timestamp+operario).
     */
    @POST("registros")
    suspend fun syncRegistros(@Body batch: List<RegistroDto>): SyncResponse

    /**
     * Fetch paginated registros (for initial load or refresh).
     * Query params mirror DAO filters.
     */
    @GET("registros")
    suspend fun getRegistros(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("tipo_residuo") tipoResiduo: String? = null,
        @Query("zona") zona: String? = null,
        @Query("operario_id") operarioId: String? = null,
        @Query("desde") desde: String? = null, // ISO-8601
        @Query("hasta") hasta: String? = null
    ): RegistroPageResponse

    /**
     * Aggregated reports for dashboard / PDF export.
     */
    @GET("reportes")
    suspend fun getReportes(
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
        @Query("zona") zona: String? = null,
        @Query("tipo_residuo") tipoResiduo: String? = null,
        @Query("group_by") groupBy: String? = null // e.g. "zona", "tipo_residuo"
    ): List<ReporteDto>

    /**
     * Lightweight fetch for zona lookup table.
     */
    @GET("zonas")
    suspend fun getZonas(): List<ZonaDto>
}

/**
 * Simple DTO for GET /zonas (kept here to avoid extra file).
 */
@kotlinx.serialization.Serializable
data class ZonaDto(
    @kotlinx.serialization.SerialName("id")
    @com.google.gson.annotations.SerializedName("id")
    val id: Long,
    @kotlinx.serialization.SerialName("nombre")
    @com.google.gson.annotations.SerializedName("nombre")
    val nombre: String,
    @kotlinx.serialization.SerialName("sector")
    @com.google.gson.annotations.SerializedName("sector")
    val sector: String,
    @kotlinx.serialization.SerialName("descripcion")
    @com.google.gson.annotations.SerializedName("descripcion")
    val descripcion: String? = null,
    @kotlinx.serialization.SerialName("activa")
    @com.google.gson.annotations.SerializedName("activa")
    val activa: Boolean = true
)
