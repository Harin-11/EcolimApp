# Pruebas de Usabilidad — ECOLIM S.A.C. (Simuladas)

> **App:** ECOLIM S.A.C. — Android Kotlin + Compose Material 3 Expressive, Room offline-first  
> **Alcance:** 5 pantallas — Login / Home Dashboard / Nuevo Registro / Historial / Reportes  
> **Fecha de pruebas:** Septiembre 2026 (simulación académica)  
> **Versión evaluada:** 1.0 (`versionCode 1`, `compileSdk 37`, `minSdk 24`)

## 1. Objetivo

Evaluar de forma simulada la usabilidad de la app ECOLIM con usuarios representativos del rol operativo y de supervisión, para:

1. Medir eficacia (tasa de éxito), eficiencia (tiempo por tarea) y satisfacción (SUS).
2. Detectar fricciones en flujos críticos de campo (registro con foto/ubicación, filtrado y generación de reportes offline).
3. Proponer mejoras priorizadas antes de una eventual publicación en Google Play.

> **Aclaración metodológica:** las pruebas son **simuladas** para el entregable del curso. No se realizaron con usuarios reales en producción. Los resultados se estiman a partir de heurísticas de Material 3, benchmarks de apps de campo y recorridos cognitivos sobre los prototipos Compose.

## 2. Perfil de participantes

| Grupo | Cantidad | Perfil | Contexto de uso |
|---|---|---|---|
| **Operarios de campo** | 5 | Edad 22–48, experiencia media con Android, uso diario de apps de mensajería y cámara. 2 con baja alfabetización digital. | Registro en ruta, con guantes, luz solar directa, conectividad intermitente. |
| **Supervisores** | 2 | Edad 30–45, experiencia con Excel y reportes a autoridad. | Revisión de historial, filtros y generación de reportes desde oficina o campo. |
| **Total** | **7** | Muestreo intencional cubriendo rol, edad y nivel digital. | — |

**Criterios de inclusión:** rol activo en gestión de residuos, uso previo de smartphone Android, consentimiento informado simulado.  
**Entorno de prueba:** dispositivo Android físico (6,5") + emulador, modo offline y online, luminosidad alta, interrupciones simuladas (llamada entrante, pérdida de red).

## 3. Tareas evaluadas

| ID | Tarea | Descripción paso a paso | Criterio de éxito |
|---|---|---|---|
| **T1** | **Login** (`LoginScreen`) | Abrir app → ingresar credenciales → pulsar Ingresar → llegar a Home. | Acceso a `HomeDashboardScreen` sin error de credenciales en ≤ 2 intentos. |
| **T2** | **Nuevo Registro con QR** (`NewRecordScreen`) | Home → Nuevo Registro → escanear QR de zona/sector (simulado) → seleccionar tipo de residuo → ingresar peso y volumen → capturar foto → marcar EPP completo → Guardar. | Registro persistido en Room con estado `PENDIENTE` y visible en Historial. |
| **T3** | **Filtrar Historial** (`HistoryScreen`) | Historial → aplicar filtros por tipo de residuo, zona y rango de fechas → verificar lista filtrada → abrir detalle de un registro. | Lista filtrada correctamente y detalle muestra foto, peso, zona y timestamp. |
| **T4** | **Generar Reporte** (`ReportsScreen`) | Reportes → seleccionar rango de fechas y zona → generar → verificar agregados (peso total, volumen, cantidad) → exportar/compartir (simulado). | Reporte generado con datos consistentes y opción de exportar visible. |

**Flujo transversal:** todas las tareas se ejecutan con el patrón offline-first; T2 y T4 se prueban también sin conectividad para validar persistencia local y sincronización posterior.

## 4. Métricas

| Métrica | Definición | Instrumento |
|---|---|---|
| **Tiempo por tarea** | Minutos desde inicio hasta criterio de éxito. | Cronómetro + log de navegación. |
| **Tasa de éxito** | % de participantes que completan la tarea sin ayuda crítica. | Observación directa. |
| **Tasa de error** | Errores por intento (campo inválido, foto omitida, filtro mal aplicado). | Conteo de eventos. |
| **SUS (System Usability Scale)** | Cuestionario estándar de 10 ítems (escala Likert 1–5), puntaje 0–100. | Cuestionario post-prueba. |
| **Satisfacción cualitativa** | Comentarios abiertos y escala de dificultad percibida (1 = muy difícil, 5 = muy fácil). | Entrevista breve. |

**Umbrales de referencia (Nielsen / Brooke):**

- Éxito ≥ 85 % = aceptable; ≥ 95 % = excelente.
- SUS ≥ 68 = por encima del promedio; ≥ 80 = excelente.
- Tiempo esperado: T1 < 1,5 min, T2 < 3 min, T3 < 2 min, T4 < 2,5 min.

## 5. Resultados simulados

### 5.1 Tabla resumen por tarea

| Tarea | Tasa de éxito | Tiempo promedio | Errores promedio / participante | Dificultad percibida (1–5) | Observación destacada |
|---|---|---|---|---|---|
| **T1 — Login** | **95 %** (7/7 completan; 1 requirió 2 intentos por mayúsculas) | **1,2 min** | 0,3 | 4,6 | Campo de contraseña con icono mostrar/ocultar valorado; teclado cubre botón en pantallas pequeñas. |
| **T2 — Nuevo Registro con QR** | **88 %** (6/7 sin ayuda; 1 necesitó indicación para QR) | **2,8 min** | 0,7 | 4,1 | Selector de tipo con iconos y colores ayuda; captura de foto con guantes requiere botón grande. Volumen en m³ genera duda en 2 operarios. |
| **T3 — Filtrar Historial** | **92 %** | **1,6 min** | 0,4 | 4,3 | Filtros por chips Material 3 son descubribles; 1 participante no encontró el filtro de fecha a la primera. |
| **T4 — Generar Reporte** | **85 %** | **2,1 min** | 0,6 | 4,0 | Agregados claros; 2 supervisores esperaban botón Exportar más prominente. Sin red, mensaje offline es claro. |
| **Promedio** | **90 %** | **1,9 min** | **0,5** | **4,25** | **Cumple umbrales de eficacia y eficiencia para MVP.** |

### 5.2 Distribución de tiempos (simulada)

```
T1 Login          ████████ 1,2 min  (rango 0,8–1,9)
T2 Nuevo Registro ██████████████████ 2,8 min  (rango 2,1–4,0)
T3 Filtrar Hist.  ██████████ 1,6 min  (rango 1,0–2,4)
T4 Generar Rep.   █████████████ 2,1 min  (rango 1,5–3,2)
```

### 5.3 Errores observados (simulados) — Pareto

| Error | Frecuencia | Tarea | Severidad |
|---|---|---|---|
| Peso/volumen con separador decimal incorrecto (coma vs punto) | 4 eventos | T2 | Media — validación lo bloquea, pero mensaje debe ser más explícito. |
| Foto omitida (usuario no advierte que es requerida) | 2 eventos | T2 | Media |
| Filtro de fecha no descubierto | 2 eventos | T3 | Baja |
| Botón Exportar no encontrado a la primera | 2 eventos | T4 | Baja |
| Credenciales con mayúscula sostenida | 1 evento | T1 | Baja |

## 6. Observaciones cualitativas

**Aspectos positivos:**

- Navegación inferior / FAB de Nuevo Registro es consistente con Material 3 Expressive y reduce toques.
- Estados `PENDIENTE` / `SINCRONIZADO` con color y contador en `HomeDashboardScreen` generan confianza offline.
- Tipografía y contraste cumplen accesibilidad en luz solar (probado con tema claro).

**Fricciones detectadas:**

1. **Teclado numérico:** en T2, el teclado cubre parcialmente el botón Guardar en dispositivos de 5,5". Requiere `windowSoftInputMode=adjustResize` + scroll (ya configurado en `AndroidManifest.xml`, validar en pruebas reales).
2. **Unidad de volumen (m³):** dos operarios dudaron del valor esperado. Falta ejemplo placeholder (ej. "0,80 m³ ≈ 1 contenedor").
3. **QR de zona/sector:** flujo optimizado, pero sin feedback háptico/sonoro al escanear, el usuario duda si la lectura fue exitosa.
4. **Filtros de fecha:** icono de calendario poco contrastado en tema claro.
5. **Exportar reporte:** CTA secundario; supervisores esperan acción primaria.

**Comentarios simulados de participantes:**

- *"Con el QR es mucho más rápido que escribir la zona."* — Operario 3
- *"Me gusta ver cuántos registros me faltan por sincronizar."* — Operario 1
- *"El reporte está claro, pero el botón de exportar debería estar más a la vista."* — Supervisor 2

## 7. Cuestionario SUS (System Usability Scale)

Administrado al finalizar las 4 tareas. Escala Likert 1 (totalmente en desacuerdo) a 5 (totalmente de acuerdo). Ítems estándar de Brooke (1986), adaptados al contexto ECOLIM.

| # | Ítem | Promedio simulado (1–5) |
|---|---|---|
| 1 | Creo que me gustaría usar esta app con frecuencia. | 4,3 |
| 2 | Encuentro la app innecesariamente compleja. (*) | 1,7 |
| 3 | Pienso que la app es fácil de usar. | 4,4 |
| 4 | Necesitaría ayuda de un técnico para usar la app. (*) | 1,9 |
| 5 | Las funciones de la app están bien integradas. | 4,2 |
| 6 | Hay demasiada inconsistencia en la app. (*) | 1,6 |
| 7 | La mayoría de las personas aprendería a usar la app rápidamente. | 4,5 |
| 8 | Encuentro la app muy engorrosa de usar. (*) | 1,5 |
| 9 | Me siento con confianza al usar la app. | 4,1 |
| 10 | Necesité aprender muchas cosas antes de usar la app. (*) | 2,0 |

> (*) Ítems inversos: puntaje bajo es positivo.

**Cálculo SUS simulado:**

- Suma ajustada (ítems impares: valor−1; pares: 5−valor) = 33,6
- **SUS = 33,6 × 2,5 = 84 / 100**
- **Interpretación:** por encima del promedio (68), rango **excelente** (Bangor et al.). Indica alta satisfacción y baja curva de aprendizaje para el MVP.

**Distribución simulada por participante:** 78, 82, 84, 86, 88, 90, 80 → mediana 84.

## 8. Conclusiones

1. **Eficacia y eficiencia alcanzadas:** con 90 % de éxito promedio y 1,9 min por tarea, el MVP cumple los umbrales para operación en campo. T1 y T3 superan el 92 % de éxito; T2 y T4, siendo más complejas, se mantienen en rango aceptable.
2. **Offline-first validado:** la persistencia local y el indicador de sincronización reducen ansiedad ante pérdida de señal, hallazgo crítico para rutas rurales.
3. **Satisfacción alta (SUS 84):** la adhesión a Material 3 Expressive (chips, FAB, cards, estados por color) favorece aprendizaje y confianza, incluso en usuarios con baja alfabetización digital.

## 9. Mejoras propuestas (priorizadas)

| Prioridad | Mejora | Tarea | Esfuerzo | Impacto |
|---|---|---|---|---|
| **P0** | Mensaje de validación explícito para peso/volumen: "Use punto como separador decimal. Ej: 12.5" + ejemplo de m³. | T2 | Bajo | Alto — reduce 50 % de errores observados. |
| **P0** | Feedback háptico + snackbar "Zona escaneada: Zona Norte / Sector A-3" tras QR. | T2 | Bajo | Alto |
| **P1** | Botón Guardar siempre visible con `imePadding()` + scroll; ampliar área táctil a 48 dp mínimo. | T2 | Medio | Alto — accesibilidad con guantes. |
| **P1** | Elevar CTA Exportar a botón primario en `ReportsScreen` y añadir estado vacío offline con ilustración. | T4 | Bajo | Medio |
| **P1** | Reforzar contraste de icono de calendario y añadir label "Desde / Hasta" persistente. | T3 | Bajo | Medio |
| **P2** | Onboarding de 3 pasos la primera vez (Login → Nuevo Registro → Historial) con coach marks. | T1–T3 | Medio | Medio |
| **P2** | Pruebas reales con 5 usuarios adicionales en campo, con registro de tiempo con herramienta (Maze / Lookback) y métricas de accesibilidad (TalkBack). | Todas | Alto | Alto — validar simulación. |

**Próximos pasos sugeridos:**

- Ejecutar ronda real con 5 operarios en dispositivo físico, midiendo SUS y tiempo con cronómetro externo.
- Instrumentar analytics de eventos (registro creado, filtro aplicado, reporte generado) para complementar SUS con datos de uso.
- Iterar P0/P1 antes de la checklist de publicación en Play (ver `docs/PLAY_STORE.md`).

---
*Documento de usabilidad simulada — ECOLIM S.A.C. — Septiembre 2026. Propósito exclusivamente académico.*
