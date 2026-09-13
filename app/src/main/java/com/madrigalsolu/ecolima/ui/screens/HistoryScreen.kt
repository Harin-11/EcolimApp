package com.madrigalsolu.ecolima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.madrigalsolu.ecolima.data.local.dao.RegistroDao
import com.madrigalsolu.ecolima.data.local.entity.EstadoRegistro
import com.madrigalsolu.ecolima.data.local.entity.RegistroEntity
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import com.madrigalsolu.ecolima.ui.theme.EntregableTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class RecordItem(
    val uniqueKey: String,
    val id: String,
    val type: String,
    val zone: String,
    val amount: String,
    val unit: String,
    val date: String,
    val status: String, // synced / pending
    val registeredBy: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    registroDao: RegistroDao? = null,
    initialStatusFilter: String? = null,
    onRecordClick: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember(initialStatusFilter) { mutableStateOf(initialStatusFilter) }
    var selectedRecordForDetail by remember { mutableStateOf<RecordItem?>(null) }

    val dbEntities by (registroDao?.getAll()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<RegistroEntity>()) })

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val allRecords = remember(dbEntities) {
        if (dbEntities.isNotEmpty()) {
            dbEntities.map { entity ->
                val typeName = when (entity.tipoResiduo) {
                    ResiduoTipo.PLASTICO -> "Plástico"
                    ResiduoTipo.PAPEL_CARTON -> "Papel/Cartón"
                    ResiduoTipo.ORGANICO -> "Orgánico"
                    ResiduoTipo.PELIGROSO -> "Peligroso"
                    ResiduoTipo.METAL -> "Metal"
                    ResiduoTipo.VIDRIO -> "Vidrio"
                }
                RecordItem(
                    uniqueKey = "${entity.id}_${entity.remoteId ?: ""}_${entity.timestamp}",
                    id = entity.remoteId ?: "REC-${entity.id}",
                    type = typeName,
                    zone = entity.zona,
                    amount = if (entity.pesoKg > 0) String.format(Locale.US, "%.1f", entity.pesoKg) else String.format(Locale.US, "%.2f", entity.volumenM3),
                    unit = if (entity.pesoKg > 0) "kg" else "m³",
                    date = dateFormatter.format(Date(entity.timestamp)),
                    status = if (entity.estado == EstadoRegistro.SINCRONIZADO) "synced" else "pending",
                    registeredBy = entity.operarioId.substringBefore("@")
                )
            }
        } else {
            emptyList()
        }
    }

    val wasteFilters = listOf("Plástico", "Papel", "Peligroso", "Orgánico", "Vidrio", "Metal")

    val filtered = remember(allRecords, query, selectedType, statusFilter) {
        allRecords.filter { r ->
            val matchesQuery = query.isBlank() ||
                r.type.contains(query, ignoreCase = true) ||
                r.zone.contains(query, ignoreCase = true) ||
                r.id.contains(query, ignoreCase = true)
            val matchesType = selectedType == null ||
                r.type.contains(selectedType!!, ignoreCase = true)
            val matchesStatus = statusFilter == null || r.status == statusFilter
            matchesQuery && matchesType && matchesStatus
        }
    }

    if (selectedRecordForDetail != null) {
        val rec = selectedRecordForDetail!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedRecordForDetail = null },
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
                    text = "Detalle del Registro",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecordDetailField(label = "Código", value = rec.id)
                    RecordDetailField(label = "Tipo de residuo", value = rec.type)
                    RecordDetailField(label = "Zona", value = rec.zone)
                    RecordDetailField(label = "Cantidad registrada", value = "${rec.amount} ${rec.unit}")
                    RecordDetailField(label = "Fecha y hora", value = rec.date)
                    RecordDetailField(label = "Registrado por", value = rec.registeredBy)
                    RecordDetailField(
                        label = "Estado",
                        value = if (rec.status == "synced") "Sincronizado" else "Guardado localmente",
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { selectedRecordForDetail = null },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cerrar")
                }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de Registros",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // SearchBar — Material 3 Expressive
            item {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { searchActive = false },
                            expanded = searchActive,
                            onExpandedChange = { searchActive = it },
                            placeholder = { Text("Buscar por tipo, zona o ID") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        )
                    },
                    expanded = searchActive,
                    onExpandedChange = { searchActive = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    content = {
                        // Recent searches / suggestions when expanded
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Búsquedas sugeridas",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            listOf("Plástico", "Zona Norte", "Orgánico", "Peligroso").forEach { s ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            query = s
                                            searchActive = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text(text = s, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    },
                )
            }

            // FilterChips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(wasteFilters) { type ->
                        val selected = selectedType == type
                        FilterChip(
                            selected = selected,
                            onClick = { selectedType = if (selected) null else type },
                            label = { Text(type) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { selectedType = null },
                            label = { Text("Todos") },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyHistoryState()
                }
            } else {
                items(filtered, key = { it.uniqueKey }) { record ->
                    HistoryRecordCard(
                        record = record,
                        onClick = {
                            selectedRecordForDetail = record
                            onRecordClick(record.id)
                        },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HistoryRecordCard(record: RecordItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = record.type,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(text = record.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = record.zone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${record.amount} ${record.unit}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(text = "•", color = MaterialTheme.colorScheme.outline)
                    Text(text = record.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(text = "Registrado por: ${record.registeredBy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Guardado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Guardado",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.outline)
            }
            Text(text = "Sin resultados", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                text = "Prueba con otro filtro o término de búsqueda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordDetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable
private fun HistoryScreenPreview() {
    EntregableTheme {
        HistoryScreen()
    }
}
