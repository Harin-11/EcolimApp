package com.madrigalsolu.ecolima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.madrigalsolu.ecolima.data.local.dao.RegistroDao
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import com.madrigalsolu.ecolima.ui.theme.EntregableTheme
import com.madrigalsolu.ecolima.util.ReportExporter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    registroDao: RegistroDao? = null,
    onExportPdf: () -> Unit = {},
    onExportExcel: () -> Unit = {},
) {
    val context = LocalContext.current
    var dateRange by remember { mutableStateOf("Semana") }
    var selectedType by remember { mutableStateOf("Todos") }
    var selectedVolume by remember { mutableStateOf("kg") }
    var exportDialogMessage by remember { mutableStateOf<String?>(null) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var exportedMimeType by remember { mutableStateOf("application/pdf") }

    val dbEntities by (registroDao?.getAll()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) })

    if (exportDialogMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                exportDialogMessage = null
                exportedFile = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            },
            title = {
                Text(
                    text = "Reporte exportado exitosamente",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = exportDialogMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = exportedFile
                        if (file != null) {
                            ReportExporter.shareFile(
                                context = context,
                                file = file,
                                mimeType = exportedMimeType,
                                title = "Reporte ECOLIM ($dateRange)"
                            )
                        }
                        exportDialogMessage = null
                        exportedFile = null
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir / Compartir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        exportDialogMessage = null
                        exportedFile = null
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cerrar")
                }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }

    // Dynamic metrics based on period and unit
    val unitMultiplier = if (selectedVolume == "m³") 0.0014f else 1f
    val unitLabel = if (selectedVolume == "m³") "m³" else "kg"

    val filteredEntities = remember(dbEntities, dateRange, selectedType) {
        val cal = java.util.Calendar.getInstance()
        val dateFiltered = when (dateRange) {
            "Hoy" -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                dbEntities.filter { it.timestamp >= cal.timeInMillis }
            }
            "Semana" -> {
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                dbEntities.filter { it.timestamp >= cal.timeInMillis }
            }
            "Mes" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                dbEntities.filter { it.timestamp >= cal.timeInMillis }
            }
            else -> dbEntities
        }
        if (selectedType == "Todos") {
            dateFiltered
        } else {
            dateFiltered.filter { entity ->
                val typeName = when (entity.tipoResiduo) {
                    ResiduoTipo.PLASTICO -> "Plástico"
                    ResiduoTipo.ORGANICO -> "Orgánico"
                    ResiduoTipo.PAPEL_CARTON -> "Papel"
                    ResiduoTipo.PELIGROSO -> "Peligroso"
                    ResiduoTipo.METAL -> "Metal"
                    ResiduoTipo.VIDRIO -> "Vidrio"
                }
                typeName.contains(selectedType, ignoreCase = true)
            }
        }
    }

    val dbTotalKg = filteredEntities.sumOf { it.pesoKg }
    val dbCount = filteredEntities.size
    val totalAmount = (dbTotalKg * unitMultiplier).toFloat()
    val recordCount = "$dbCount registros"
    val daysDivider = when (dateRange) {
        "Hoy" -> 1f
        "Mes" -> 30f
        else -> 7f
    }
    val avgDay = if (dbCount > 0) totalAmount / daysDivider else 0f

    val totalKgAll = dbEntities.sumOf { it.pesoKg }
    val wasteDistribution = remember(dbEntities) {
        if (totalKgAll > 0) {
            val byType = dbEntities.groupBy { it.tipoResiduo }
            val plasticoKg = byType[ResiduoTipo.PLASTICO]?.sumOf { it.pesoKg } ?: 0.0
            val organicoKg = byType[ResiduoTipo.ORGANICO]?.sumOf { it.pesoKg } ?: 0.0
            val papelKg = byType[ResiduoTipo.PAPEL_CARTON]?.sumOf { it.pesoKg } ?: 0.0
            val peligrosoKg = byType[ResiduoTipo.PELIGROSO]?.sumOf { it.pesoKg } ?: 0.0
            val metalKg = byType[ResiduoTipo.METAL]?.sumOf { it.pesoKg } ?: 0.0
            val vidrioKg = byType[ResiduoTipo.VIDRIO]?.sumOf { it.pesoKg } ?: 0.0
            val otrosKg = metalKg + vidrioKg

            listOf(
                WasteDistributionItem("Plástico", (plasticoKg / totalKgAll).toFloat(), ((plasticoKg / totalKgAll) * 100).toInt(), androidx.compose.ui.graphics.Color(0xFF2E7D32)),
                WasteDistributionItem("Orgánico", (organicoKg / totalKgAll).toFloat(), ((organicoKg / totalKgAll) * 100).toInt(), androidx.compose.ui.graphics.Color(0xFF558B2F)),
                WasteDistributionItem("Papel / Cartón", (papelKg / totalKgAll).toFloat(), ((papelKg / totalKgAll) * 100).toInt(), androidx.compose.ui.graphics.Color(0xFFF57C00)),
                WasteDistributionItem("Peligroso", (peligrosoKg / totalKgAll).toFloat(), ((peligrosoKg / totalKgAll) * 100).toInt(), androidx.compose.ui.graphics.Color(0xFFD32F2F)),
                WasteDistributionItem("Otros", (otrosKg / totalKgAll).toFloat(), ((otrosKg / totalKgAll) * 100).toInt(), androidx.compose.ui.graphics.Color(0xFF78909C)),
            )
        } else {
            emptyList()
        }
    }

    val zoneBreakdown = remember(filteredEntities, selectedVolume) {
        if (filteredEntities.isEmpty()) {
            emptyList()
        } else {
            val totalKg = filteredEntities.sumOf { it.pesoKg }
            val byZone = filteredEntities.groupBy { it.zona }
            byZone.map { (zone, list) ->
                val sumKg = list.sumOf { it.pesoKg }
                val fraction = if (totalKg > 0) (sumKg / totalKg).toFloat() else 0f
                val labelVal = if (selectedVolume == "m³") {
                    String.format(java.util.Locale.US, "%.2f m³", sumKg * unitMultiplier)
                } else {
                    String.format(java.util.Locale.US, "%.1f kg", sumKg)
                }
                Triple(zone, labelVal, fraction)
            }.sortedByDescending { it.third }
        }
    }

    val todayFormatted = remember {
        java.text.SimpleDateFormat("d 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es-PE")).format(java.util.Date())
    }
    val monthFormatted = remember {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.forLanguageTag("es-PE")).format(java.util.Date()).replaceFirstChar { it.uppercase() }
    }
    val weekFormatted = remember {
        val cal = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val endStr = sdf.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
        "${sdf.format(cal.time)} — $endStr"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reportes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Trazabilidad y cumplimiento ambiental",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Unified Filter Card
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Date range header
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(text = "Período de reporte", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = when (dateRange) {
                                        "Hoy" -> todayFormatted
                                        "Mes" -> monthFormatted
                                        else -> weekFormatted
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Period filter chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Hoy", "Semana", "Mes").forEach { range ->
                                FilterChip(
                                    selected = dateRange == range,
                                    onClick = { dateRange = range },
                                    label = { Text(range, style = MaterialTheme.typography.labelMedium) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                )
                            }
                        }

                        // Waste type filter
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Tipo de residuo", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Todos", "Plástico", "Orgánico", "Papel", "Peligroso", "Vidrio", "Metal")) { t ->
                                    FilterChip(
                                        selected = selectedType == t,
                                        onClick = { selectedType = t },
                                        label = { Text(t, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                    )
                                }
                            }
                        }

                        // Unit selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Unidad de medida", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedVolume == "kg",
                                    onClick = { selectedVolume = "kg" },
                                    label = { Text("kg") },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    ),
                                )
                                FilterChip(
                                    selected = selectedVolume == "m³",
                                    onClick = { selectedVolume = "m³" },
                                    label = { Text("m³") },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // KPI row — dynamically connected to filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val formattedTotal = if (selectedVolume == "m³") String.format("%.2f %s", totalAmount, unitLabel) else String.format("%,d %s", totalAmount.toInt(), unitLabel)
                    val formattedAvg = if (selectedVolume == "m³") String.format("%.2f %s", avgDay, unitLabel) else String.format("%,d %s", avgDay.toInt(), unitLabel)
                    KpiCard(label = "Total recolectado", value = formattedTotal, sub = recordCount, modifier = Modifier.weight(1f))
                    KpiCard(label = "Promedio / día", value = formattedAvg, sub = "Período: $dateRange", modifier = Modifier.weight(1f))
                }
            }

            // Waste Distribution Section — clean progress bars instead of fake Canvas chart
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(text = "Distribución por residuo", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = if (selectedType == "Todos") "Clasificación global" else "Filtro: $selectedType",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(text = unitLabel, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        val visibleDistribution = if (selectedType == "Todos") {
                            wasteDistribution
                        } else {
                            wasteDistribution.filter { it.label.contains(selectedType, ignoreCase = true) }
                        }

                        if (visibleDistribution.isEmpty()) {
                            Text(
                                text = "Sin datos de residuos registrados en el sistema",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        } else {
                            visibleDistribution.forEach { item ->
                                val itemAmount = if (selectedVolume == "m³") {
                                    String.format(java.util.Locale.US, "%.2f m³", totalAmount * item.percentage / 100f)
                                } else {
                                    String.format(java.util.Locale.US, "%,d kg", (totalAmount * item.percentage / 100f).toInt())
                                }
                                WasteDistributionRow(
                                    label = item.label,
                                    percentage = "${item.percentage}%",
                                    amount = itemAmount,
                                    fraction = item.fraction,
                                    color = item.color,
                                )
                            }
                        }
                    }
                }
            }

            // Breakdown by zone
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = "Desglose por zona de recolección", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        if (zoneBreakdown.isEmpty()) {
                            Text(
                                text = "Sin datos de zonas para el período seleccionado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        } else {
                            zoneBreakdown.forEach { (zone, labelVal, fraction) ->
                                ZoneRow(zone, labelVal, fraction)
                            }
                        }
                    }
                }
            }

            // Export card
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = "Exportar reporte", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "Genera el reporte consolidado para auditoría y certificación ambiental.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onExportPdf()
                                    try {
                                        val pdfFile = ReportExporter.exportPdf(
                                            context = context,
                                            records = filteredEntities,
                                            period = dateRange,
                                            totalWeight = totalAmount.toDouble(),
                                            totalVolume = (totalAmount * 0.0014).toDouble()
                                        )
                                        exportedFile = pdfFile
                                        exportedMimeType = "application/pdf"
                                        exportDialogMessage = "El reporte oficial en PDF ha sido generado exitosamente con ${filteredEntities.size} registros para el período: $dateRange.\n\nArchivo: ${pdfFile.name}"
                                    } catch (e: Exception) {
                                        exportDialogMessage = "Error al generar PDF: ${e.localizedMessage}"
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar PDF", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    onExportExcel()
                                    try {
                                        val csvFile = ReportExporter.exportCsv(
                                            context = context,
                                            records = filteredEntities,
                                            period = dateRange
                                        )
                                        exportedFile = csvFile
                                        exportedMimeType = "text/csv"
                                        exportDialogMessage = "El consolidado en formato Excel (.csv UTF-8) ha sido generado exitosamente con ${filteredEntities.size} registros para el período: $dateRange.\n\nArchivo: ${csvFile.name}"
                                    } catch (e: Exception) {
                                        exportDialogMessage = "Error al exportar Excel: ${e.localizedMessage}"
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar Excel", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private data class WasteDistributionItem(
    val label: String,
    val fraction: Float,
    val percentage: Int,
    val color: Color,
)

@Composable
private fun WasteDistributionRow(
    label: String,
    percentage: String,
    amount: String,
    fraction: Float,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = amount, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = percentage, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
            Text(text = sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ZoneRow(zone: String, amount: String, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = zone, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(text = amount, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable
private fun ReportsPreview() {
    EntregableTheme {
        ReportsScreen()
    }
}
