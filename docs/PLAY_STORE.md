# Requerimientos para Publicación Google Play (Simulado) — ECOLIM S.A.C.

> **App:** ECOLIM S.A.C. — Gestión de residuos  
> **Paquete:** `com.madrigalsolu.ecolima`  
> **Stack:** Android Kotlin + Compose Material 3 Expressive, Room, Retrofit, WorkManager  
> **Fecha:** Septiembre 2026  
> **Estado:** Publicación **simulada** con fines académicos. No se realiza despliegue real a Google Play Console en este entregable.

## 1. Aviso de simulación

Este documento describe los requerimientos reales de Google Play tal como aplicarían a ECOLIM, pero la publicación es **simulada para el curso**. No se crea ficha en Play Console ni se sube un App Bundle a un canal de producción. Los apartados de firma, políticas y checklist se presentan como evidencia de cumplimiento del estándar exigido por Google Play (agosto 2024+).

## 2. Permisos declarados y justificación

Solo se declaran permisos necesarios y justificables ante la revisión de Play. Principio de mínimo privilegio.

| Permiso | Declaración | ¿Por qué lo necesita ECOLIM? | Justificación para Play Console (Data safety / Declaración) |
|---|---|---|---|
| `INTERNET` | Normal | Sincronización REST (`POST /registros`, `GET /reportes`, `GET /zonas`) vía Retrofit/OkHttp. | No requiere aprobación en runtime. Uso evidente: sync offline-first. Explicar en Data safety como "comunicación con servidor propio". |
| `ACCESS_NETWORK_STATE` | Normal | Detectar conectividad para `SyncWorker` (`NetworkType.CONNECTED`) y mostrar estado offline en Home. | Complemento de `INTERNET`. Sin acceso a datos sensibles. |
| `CAMERA` | Dangerous (runtime) | Captura de foto de evidencia en `NewRecordScreen` (CameraX). Se guarda como `fotoUri` (`content://`) y se sincroniza como `foto_url`. | **Obligatorio:** solicitar en runtime con `rememberPermissionState`, explicar propósito ("tomar foto como evidencia del residuo"), y declarar en Data safety → "Fotos y videos — recolectadas, opcional, para trazabilidad". Incluir video de uso si Play lo solicita. |
| `ACCESS_FINE_LOCATION` | Dangerous (runtime) | Georreferenciación del registro (`lat`/`lng`) para trazabilidad y auditoría. No es rastreo continuo, solo captura al momento del registro. | **Sensible:** declarar como "Ubicación precisa — recolectada solo al crear registro, no en segundo plano". Solicitar con `ACCESS_COARSE_LOCATION` como fallback. Justificar que sin ubicación el reporte a autoridad pierde validez. No usar `ACCESS_BACKGROUND_LOCATION` (no aplica; si se usara, requeriría justificación adicional y video). |
| `POST_NOTIFICATIONS` | Dangerous (runtime, Android 13+) | Notificación de resultado de sincronización en segundo plano (`SyncWorker`: "3 registros sincronizados" / "Error de sync — reintentando"). | Declarar como "Notificaciones — para informar estado de sincronización". Solicitar solo tras primer registro pendiente, no al iniciar la app. En Data safety no es dato recolectado, es canal de comunicación. |

**Permisos NO solicitados (y por qué):** `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` (se usa `ActivityResultContracts.TakePicture` con `FileProvider`, no se lee galería), `RECORD_AUDIO`, `READ_CONTACTS`, `ACCESS_BACKGROUND_LOCATION`.

### 2.1 Snippet — `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Red y sincronización -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Funciones de campo -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- Notificación de sync (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Declaración de uso de cámara (para filtrado en Play) -->
    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.camera.autofocus"
        android:required="false" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:theme="@style/Theme.Entregable">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.Entregable"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

> **Estado actual del proyecto:** `app/src/main/AndroidManifest.xml` ya declara `INTERNET` y `ACCESS_NETWORK_STATE`. Para publicación real, añadir `CAMERA`, `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` y `POST_NOTIFICATIONS` como se muestra arriba, junto con el manejo en runtime en `NewRecordScreen` y `SyncWorker`.

## 3. Configuración de build

Valores verificados en `app/build.gradle.kts` del entregable.

| Propiedad | Valor | Nota Play |
|---|---|---|
| `namespace` / `applicationId` | `com.madrigalsolu.ecolima` | Identificador único. No cambiar tras publicar. |
| `compileSdk` | **37** (Android 15+) | Compilar contra SDK más reciente. |
| `targetSdk` | **37** | **Obligatorio:** Play exige `targetSdk` del nivel más reciente (34+ desde ago 2024; 35 desde 2025). Con 37 se cumple con holgura. |
| `minSdk` | **24** (Android 7.0) | Cobertura ~97 % del parque. Coherente con WorkManager y Compose. |
| `versionCode` | **1** | Entero incremental. Cada subida a Play debe incrementarlo. |
| `versionName` | **"1.0"** | Visible al usuario. Semver sugerido. |
| `buildFeatures.compose` | `true` | Requerido para Compose Material 3 Expressive. |

### 3.1 Snippet — `app/build.gradle.kts` (bloque `android`)

```kotlin
android {
    namespace = "com.madrigalsolu.ecolima"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.madrigalsolu.ecolima"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Para producción real: isMinifyEnabled = true + proguardFiles(...)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
```

### 3.2 App Bundle y firma

- **Formato exigido por Play:** Android App Bundle (`.aab`), no APK. Generar con `./gradlew bundleRelease`.
- **Firma:** Play App Signing (recomendado). Subir `aab` firmado con keystore de release; Play gestiona la clave de distribución. Guardar `keystore.jks`, `keyAlias` y `storePassword` fuera del repo (ej. `local.properties` o Secret Manager).
- **Ejemplo de configuración de firma (no incluir en repo público):**

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore/ecolim.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

## 4. Políticas — Privacidad y datos

### 4.1 Política de privacidad (requerida por Play)

Debe estar publicada en URL accesible y enlazada en Play Console → **Política de privacidad**. Para ECOLIM (simulado):

**URL simulada:** `https://ecolim.example.com/privacidad`

**Contenido mínimo:**

- **Responsable:** ECOLIM S.A.C., contacto `privacidad@ecolim.example.com`.
- **Datos recolectados:**
  - Identificación operativa: `operarioId`, `email` (login) — finalidad: autenticación y trazabilidad.
  - Datos de registro: `zona`, `sector`, `tipoResiduo`, `pesoKg`, `volumenM3`, `timestamp`, `eppCompleto`, `observaciones` — finalidad: gestión operativa y reportes.
  - **Ubicación precisa** (`lat`/`lng`) — solo al crear registro, no en segundo plano — finalidad: georreferenciación de evidencia.
  - **Fotos** (`fotoUri` → `foto_url`) — finalidad: evidencia visual; puede contener fondo con personas de forma incidental.
- **Base legal:** ejecución de servicio de gestión de residuos y cumplimiento de reportes a autoridad.
- **Cifrado:** tránsito con **TLS 1.2+** (OkHttp/Retrofit sobre HTTPS `https://api.ecolim.example.com`); en reposo, fotos en almacenamiento con cifrado del sistema y base Room con cifrado a nivel de dispositivo (Android file-based encryption). Para nivel adicional, evaluar SQLCipher si el cliente lo exige.
- **Retención:** registros operativos 2 años (o plazo legal aplicable), luego anonimización. Fotos 1 año. El usuario puede solicitar eliminación vía `privacidad@ecolim.example.com`.
- **Compartidos con terceros:** solo infraestructura de hosting de API/CDN de fotos (encargado de tratamiento), sin venta de datos ni publicidad.
- **Derechos:** acceso, rectificación, cancelación y oposición (ARCO).
- **Menores:** la app no está dirigida a menores; clasificación "Todos" no implica recolección de datos de menores.

### 4.2 Data safety (Formulario de Seguridad de datos en Play Console)

| Categoría Play | ¿Se recolecta? | ¿Se comparte? | Finalidad | Cifrado en tránsito | Eliminación disponible |
|---|---|---|---|---|---|
| Ubicación precisa | Sí | No | Trazabilidad del registro | Sí | Sí (a solicitud) |
| Fotos y videos | Sí | No (solo backend propio) | Evidencia | Sí | Sí |
| Identificadores (ID de usuario) | Sí (`operarioId`, `email`) | No | Autenticación | Sí | Sí |
| Información personal (nombre) | Sí | No | Identificación operativa | Sí | Sí |

Marcar: **"Los datos se cifran en tránsito" = Sí**, **"Los usuarios pueden solicitar eliminación" = Sí**. No marcar "compartidos con terceros con fines publicitarios".

### 4.3 Contenido y clasificación

- **Clasificación de contenido (IARC):** **Todos (Everyone)** — sin violencia, sin contenido sexual, sin lenguaje inapropiado, sin drogas. Cuestionario IARC: app de productividad empresarial.
- **Anuncios:** **Sin anuncios**. Declarar "No contiene anuncios" en Play Console. No se integra SDK publicitario.
- **Compras integradas:** No aplica.
- **Público objetivo:** profesionales (operarios/supervisores), no niños. No acogida al programa Designed for Families.

## 5. Ficha de Play Store (Store Listing)

### 5.1 Textos

**Descripción corta (≤ 80 caracteres):**

> Gestión de residuos en campo: registro offline, foto y reportes.

**Descripción larga (sugerida, ≤ 4000 caracteres):**

> ECOLIM S.A.C. digitaliza la gestión de residuos en campo. Registra peso, volumen y tipo de residuo con foto y ubicación, incluso sin internet. Sincroniza automáticamente cuando hay red y genera reportes por zona y periodo para la autoridad.
>
> • Registro offline-first con Room — trabaja sin señal.
> • Captura de foto y georreferenciación por registro.
> • Filtros por zona, tipo y fecha en Historial y Reportes.
> • Sincronización segura por lotes con el servidor central.
> • Diseño Material 3 Expressive, accesible con guantes y a plena luz.
>
> Pensada para operarios y supervisores de ECOLIM S.A.C.

**Novedades (What’s new) v1.0:**

> Versión inicial: 5 pantallas (Login, Home, Nuevo Registro, Historial, Reportes), modo offline y sincronización automática.

### 5.2 Gráficos

| Recurso | Requisito Play | Estado en entregable |
|---|---|---|
| **Ícono** | 512 × 512 PNG-32, sin alfa, < 1 MB | `app/src/main/res/mipmap-*` (webp) + vector `ic_launcher_foreground.xml` — generar PNG 512 para Console. |
| **Feature graphic** | 1024 × 500 PNG/JPEG | Crear a partir de tema ECOLIM (verde/eco) + mock de Home. |
| **Screenshots teléfono** | Mín. 2, máx. 8 (16:9 o 9:16, 1080×1920 recomendado) | Capturar 5 pantallas Compose: Login, Home Dashboard, Nuevo Registro, Historial, Reportes. |
| **Screenshots tablet (opcional)** | Si se declara soporte tablet | Opcional para 1.0. |

## 6. Checklist pre-lanzamiento

### 6.1 Técnica

- [ ] `targetSdk 37` y `compileSdk 37` verificados (`app/build.gradle.kts`).
- [ ] `versionCode` incrementado respecto a cualquier subida previa.
- [ ] `AndroidManifest.xml` con permisos justificados y `uses-feature camera required=false`.
- [ ] Solicitud en runtime para `CAMERA`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS` con rationale.
- [ ] `./gradlew bundleRelease` genera `.aab` firmado (Play App Signing).
- [ ] Probado en release con `minifyEnabled` (si se activa) y sin ANR/crash en pre-launch report.
- [ ] `data_extraction_rules.xml` / `backup_rules.xml` revisados (no exponer Room DB en backup si contiene datos sensibles).

### 6.2 Contenido y políticas

- [ ] Política de privacidad publicada y URL válida en Console.
- [ ] Data safety completado (ubicación, fotos, cifrado, eliminación).
- [ ] Clasificación IARC completada → Todos.
- [ ] Declaración "Sin anuncios" y sin SDK publicitario.
- [ ] Ficha completa: descripción corta/larga, ícono 512, feature graphic 1024×500, 5 screenshots.

### 6.3 Calidad y pruebas

- [ ] **Pruebas internas (Internal testing)** con 5–10 testers (operarios/supervisores simulados) — canal recomendado para 1.0 antes de producción.
- [ ] Recorrido de las 4 tareas de `docs/USABILIDAD.md` en track interno; SUS ≥ 80 como criterio de salida.
- [ ] Pruebas en dispositivos con Android 7 (minSdk 24) y Android 15 (target 37), con y sin conectividad.
- [ ] Verificación offline: crear registro sin red → confirmar `PENDIENTE` → recuperar red → verificar `SINCRONIZADO` y notificación.
- [ ] Pre-launch report de Play (sin errores de accesibilidad críticos, sin permisos sensibles injustificados).

### 6.4 Post-publicación (simulado)

- [ ] Monitoreo de ANR/crash en Android Vitals.
- [ ] Revisión de comentarios y actualización de `PLAY_STORE.md` con cambios de política (ej. targetSdk anual).
- [ ] Plan de versionado: `versionCode 2` para hotfix, `versionName 1.1` para mejoras P0/P1 de usabilidad.

## 7. Referencias cruzadas

- `app/build.gradle.kts` — `compileSdk 37`, `targetSdk 37`, `minSdk 24`, `versionCode 1`.
- `app/src/main/AndroidManifest.xml` — permisos y `windowSoftInputMode`.
- `docs/ERD.md` — campos `fotoUri`, `lat/lng`, `estado`, `remoteId` que justifican permisos.
- `docs/API_SIMULATION.md` — endpoints que requieren `INTERNET`.
- `docs/COMPARATIVA_TIEMPOS.md` — justificación de valor para descripción de Play.
- `docs/USABILIDAD.md` — tareas que deben pasar en pruebas internas antes de publicar.

---
*Documento de requerimientos Play (simulado) — ECOLIM S.A.C. — Septiembre 2026. Propósito exclusivamente académico; no constituye publicación real.*
