# Simulación de API RESTful — ECOLIM S.A.C.

Base URL simulada: `https://api.ecolim.example.com/api/v1/`

> Para el entregable, la API puede ser mockeada con MockWebServer, json-server o Postman Mock. Los contratos JSON son idénticos en todos los casos.

## 1. Contrato de endpoints

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| `POST /registros` | Batch sync offline-first | Bearer JWT |
| `GET /registros?page=&page_size=&tipo_residuo=&zona=` | Listado paginado | Bearer JWT |
| `GET /reportes?desde=&hasta=&zona=&tipo_residuo=` | Agregados para dashboard | Bearer JWT |
| `GET /zonas` | Catálogo de zonas | Bearer JWT |

## 2. Uso con Retrofit (Kotlin)

```kotlin
// DI module (Hilt / manual)
@Module @InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${tokenProvider()}") // JWT
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        .build()

    @Provides @Singleton
    fun provideRetrofit(okHttp: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.ecolim.example.com/api/v1/")
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideEcolimApi(retrofit: Retrofit): EcolimApi =
        retrofit.create(EcolimApi::class.java)
}

// Repository sync (offline-first)
class RegistroRepository(
    private val dao: RegistroDao,
    private val api: EcolimApi
) {
    suspend fun insertLocal(entity: RegistroEntity) {
        dao.insert(entity.copy(estado = EstadoRegistro.PENDIENTE))
        // Encola sync en background
        SyncWorker.enqueue(context)
    }

    suspend fun syncPending() {
        val pending = dao.getPendingSync()
        if (pending.isEmpty()) return
        val response = api.syncRegistros(pending.map { it.toDto() })
        response.synced.forEachIndexed { i, item ->
            dao.markSynced(pending[i].id, item.remoteId)
        }
    }
}

// Uso en ViewModel
viewModelScope.launch {
    repository.insertLocal(
        RegistroEntity(
            zona = "Zona Norte",
            sector = "A-3",
            tipoResiduo = ResiduoTipo.PLASTICO,
            pesoKg = 12.5,
            volumenM3 = 0.8,
            timestamp = System.currentTimeMillis(),
            operarioId = "op_42",
            eppCompleto = true
        )
    )
    // Observa local
    repository.getFiltered(tipo = ResiduoTipo.PLASTICO).collect { lista ->
        _uiState.value = lista
    }
}
```

## 3. Ejemplos curl

### POST /registros — sincronizar lote pendiente
```bash
curl -X POST https://api.ecolim.example.com/api/v1/registros \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "zona": "Zona Norte",
      "zona_id": 3,
      "sector": "A-3",
      "tipo_residuo": "PLASTICO",
      "peso_kg": 12.5,
      "volumen_m3": 0.8,
      "foto_url": null,
      "lat": -12.0464,
      "lng": -77.0428,
      "timestamp": "2026-09-12T14:30:00Z",
      "timestamp_ms": 1726146600000,
      "operario_id": "op_42",
      "observaciones": "Recolección en mercado mayorista",
      "epp_completo": true
    },
    {
      "zona": "Zona Sur",
      "sector": "B-1",
      "tipo_residuo": "ORGANICO",
      "peso_kg": 25.0,
      "volumen_m3": 1.2,
      "foto_url": "https://cdn.ecolim.example.com/fotos/abc123.jpg",
      "lat": -12.05,
      "lng": -77.03,
      "timestamp": "2026-09-12T15:00:00Z",
      "timestamp_ms": 1726148400000,
      "operario_id": "op_42",
      "observaciones": null,
      "epp_completo": true
    }
  ]'
```

### GET /registros — listado paginado con filtros
```bash
curl "https://api.ecolim.example.com/api/v1/registros?page=1&page_size=20&tipo_residuo=PLASTICO&zona=Zona%20Norte" \
  -H "Authorization: Bearer <JWT>"
```

### GET /reportes — agregados para ReportsScreen
```bash
curl "https://api.ecolim.example.com/api/v1/reportes?desde=2026-09-01T00:00:00Z&hasta=2026-09-12T23:59:59Z&group_by=zona" \
  -H "Authorization: Bearer <JWT>"
```

## 4. JSON de ejemplo (mock)

### Request `POST /registros` (ya mostrado arriba)

### Response `POST /registros` — `SyncResponse`
```json
{
  "success": true,
  "synced": [
    { "local_temp_id": "1", "remote_id": "srv_9f3a1c", "timestamp": "2026-09-12T14:30:01Z" },
    { "local_temp_id": "2", "remote_id": "srv_9f3a1d", "timestamp": "2026-09-12T15:00:01Z" }
  ],
  "failed": [],
  "message": "2 registros sincronizados"
}
```

### Response con errores parciales
```json
{
  "success": false,
  "synced": [
    { "local_temp_id": "1", "remote_id": "srv_9f3a1c" }
  ],
  "failed": [
    { "local_temp_id": "2", "error": "peso_kg debe ser > 0", "code": 422 }
  ],
  "message": "1 sincronizado, 1 fallido"
}
```

### Response `GET /registros`
```json
{
  "data": [
    {
      "id": "srv_9f3a1c",
      "zona": "Zona Norte",
      "zona_id": 3,
      "sector": "A-3",
      "tipo_residuo": "PLASTICO",
      "peso_kg": 12.5,
      "volumen_m3": 0.8,
      "foto_url": null,
      "lat": -12.0464,
      "lng": -77.0428,
      "timestamp": "2026-09-12T14:30:00Z",
      "timestamp_ms": 1726146600000,
      "operario_id": "op_42",
      "observaciones": "Recolección en mercado mayorista",
      "epp_completo": true
    }
  ],
  "total": 1,
  "page": 1,
  "page_size": 50
}
```

### Response `GET /reportes`
```json
[
  {
    "zona": "Zona Norte",
    "tipo_residuo": null,
    "total_peso_kg": 152.3,
    "total_volumen_m3": 12.4,
    "cantidad_registros": 18,
    "desde": "2026-09-01T00:00:00Z",
    "hasta": "2026-09-12T23:59:59Z"
  },
  {
    "zona": "Zona Sur",
    "tipo_residuo": null,
    "total_peso_kg": 98.7,
    "total_volumen_m3": 8.1,
    "cantidad_registros": 12,
    "desde": "2026-09-01T00:00:00Z",
    "hasta": "2026-09-12T23:59:59Z"
  }
]
```

### Response `GET /zonas`
```json
[
  { "id": 1, "nombre": "Zona Norte", "sector": "A", "descripcion": "Mercado mayorista", "activa": true },
  { "id": 2, "nombre": "Zona Sur", "sector": "B", "descripcion": "Zona industrial", "activa": true },
  { "id": 3, "nombre": "Zona Centro", "sector": "C", "descripcion": "Centro histórico", "activa": true }
]
```

## 5. Mock local con MockWebServer (test)

```kotlin
@Test
fun syncRegistros_mock() = runTest {
    val server = MockWebServer()
    server.enqueue(MockResponse().setBody("""
        {"success":true,"synced":[{"local_temp_id":"1","remote_id":"srv_1"}],"failed":[],"message":"ok"}
    """.trimIndent()))
    server.start()

    val api = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(EcolimApi::class.java)

    val res = api.syncRegistros(listOf(RegistroDto(
        zona="Zona Norte", sector="A-3", tipoResiduo="PLASTICO",
        pesoKg=1.0, volumenM3=0.1, timestamp="2026-09-12T00:00:00Z", operarioId="op_1"
    )))
    assertTrue(res.success)
    server.shutdown()
}
```

## 6. Códigos HTTP

- `200 OK` — sync exitoso (incluso con `failed` parciales)
- `401 Unauthorized` — JWT inválido/expirado
- `422 Unprocessable Entity` — validación de campos (pesoKg, tipoResiduo)
- `500 Internal Server Error` — reintentar con WorkManager

## 7. Archivos relacionados

- `data/remote/api/EcolimApi.kt` — interfaz Retrofit
- `data/remote/dto/RegistroDto.kt` / `SyncResponse.kt` — contratos
- `data/repository/RegistroRepository.kt` — `syncPending()` batch
- `data/work/SyncWorker.kt` — reintento con `NetworkType.CONNECTED`
