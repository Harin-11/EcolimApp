package com.madrigalsolu.ecolima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.madrigalsolu.ecolima.data.local.entity.EstadoRegistro
import com.madrigalsolu.ecolima.data.local.entity.RegistroEntity
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import com.madrigalsolu.ecolima.ui.theme.EntregableTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRecordScreen(
    userEmail: String = "usuario@ecolim.pe",
    onBack: () -> Unit = {},
    onRegister: (RegistroEntity) -> Unit = {},
) {
    var zoneExpanded by remember { mutableStateOf(false) }
    var selectedZone by remember { mutableStateOf("") }
    var selectedWasteType by remember { mutableStateOf("Plástico") }
    var unit by remember { mutableStateOf("kg") }
    var weight by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }

    var zoneError by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf(false) }

    var eppGuantes by remember { mutableStateOf(true) }
    var eppMascarilla by remember { mutableStateOf(true) }
    var eppCasco by remember { mutableStateOf(false) }
    var eppChaleco by remember { mutableStateOf(true) }

    val currentDateTimeStr = remember {
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    }

    val zones = listOf(
        "Zona Norte — Sector A",
        "Zona Norte — Sector B",
        "Zona Centro — Mercado",
        "Zona Sur — Colegio",
        "Zona Industrial",
        "Zona Este — Residencial",
    )
    val wasteTypes = listOf("Plástico", "Papel", "Orgánico", "Peligroso", "Metal", "Vidrio")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nuevo Registro",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Registro offline • se sincronizará automáticamente",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Zone dropdown
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Zona de recolección")
                    ExposedDropdownMenuBox(
                        expanded = zoneExpanded,
                        onExpandedChange = { zoneExpanded = !zoneExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedZone,
                            onValueChange = {},
                            readOnly = true,
                            isError = zoneError,
                            supportingText = if (zoneError) {
                                { Text("Debes seleccionar una zona", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            label = { Text("Selecciona zona") },
                            placeholder = { Text("Ej: Zona Norte — Sector A") },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = zoneExpanded,
                            onDismissRequest = { zoneExpanded = false },
                        ) {
                            zones.forEach { zone ->
                                DropdownMenuItem(
                                    text = { Text(zone) },
                                    onClick = {
                                        selectedZone = zone
                                        zoneError = false
                                        zoneExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }
            }

            // Waste type selector
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Tipo de residuo")
                    Text(
                        text = "Selecciona la categoría para clasificación y pesaje",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(wasteTypes) { type ->
                            FilterChip(
                                selected = selectedWasteType == type,
                                onClick = { selectedWasteType = type },
                                label = { Text(type) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            // Weight / Volume input with unit toggle
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Cantidad")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Unit toggle — FilterChips as toggle
                        FilterChip(
                            selected = unit == "kg",
                            onClick = { unit = "kg" },
                            label = { Text("kg") },
                            leadingIcon = {
                                Icon(Icons.Filled.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                        FilterChip(
                            selected = unit == "m³",
                            onClick = { unit = "m³" },
                            label = { Text("m³") },
                            leadingIcon = {
                                Icon(Icons.Filled.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (unit == "kg") "Peso" else "Volumen",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = weight,
                        onValueChange = {
                            weight = it
                            if (weightError) weightError = false
                        },
                        isError = weightError,
                        supportingText = if (weightError) {
                            { Text("Ingresa una cantidad válida mayor a 0", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        label = { Text(if (unit == "kg") "Peso en kilogramos" else "Volumen en m³") },
                        placeholder = { Text(if (unit == "kg") "Ej: 42.5" else "Ej: 0.85") },
                        suffix = { Text(unit, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        label = { Text("Observaciones (opcional)") },
                        placeholder = { Text("Ej: Contenedor con residuos mezclados...") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            }

            // EPP checklist
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SectionLabel("Checklist EPP")
                    Text(
                        text = "Confirma el equipo de protección utilizado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EppRow("Guantes", eppGuantes) { eppGuantes = it }
                    EppRow("Mascarilla", eppMascarilla) { eppMascarilla = it }
                    EppRow("Casco", eppCasco) { eppCasco = it }
                    EppRow("Chaleco reflectivo", eppChaleco) { eppChaleco = it }
                }
            }

            // Geo + timestamp auto-filled display
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Datos auto-registrados",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoPill(icon = Icons.Filled.LocationOn, label = "Ubicación", value = "-12.0464, -77.0428", modifier = Modifier.weight(1f))
                        InfoPill(icon = Icons.Filled.AccessTime, label = "Fecha y hora", value = currentDateTimeStr, modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = "GPS + timestamp se guardan automáticamente para trazabilidad.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Large expressive button
            Button(
                onClick = {
                    val parsed = weight.toDoubleOrNull()
                    val hasZoneError = selectedZone.isBlank()
                    val hasWeightError = parsed == null || parsed <= 0.0
                    zoneError = hasZoneError
                    weightError = hasWeightError
                    if (!hasZoneError && !hasWeightError) {
                        val amount = parsed!!
                        val now = System.currentTimeMillis()
                        val tipo = when (selectedWasteType) {
                            "Plástico" -> ResiduoTipo.PLASTICO
                            "Papel" -> ResiduoTipo.PAPEL_CARTON
                            "Orgánico" -> ResiduoTipo.ORGANICO
                            "Peligroso" -> ResiduoTipo.PELIGROSO
                            "Metal" -> ResiduoTipo.METAL
                            "Vidrio" -> ResiduoTipo.VIDRIO
                            else -> ResiduoTipo.PLASTICO
                        }
                        val entity = RegistroEntity(
                            zona = selectedZone,
                            sector = selectedZone.substringAfter("—", selectedZone).trim(),
                            tipoResiduo = tipo,
                            pesoKg = if (unit == "kg") amount else amount * 700.0,
                            volumenM3 = if (unit == "m³") amount else amount * 0.0014,
                            timestamp = now,
                            estado = EstadoRegistro.PENDIENTE,
                            operarioId = userEmail,
                            observaciones = observations.ifBlank { null },
                            eppCompleto = eppGuantes && eppMascarilla && eppChaleco
                        )
                        onRegister(entity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registrar recolección",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            Text(
                text = "Se guardará offline y se sincronizará cuando haya conexión.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun EppRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal),
            color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 1400)
@Composable
private fun NewRecordPreview() {
    EntregableTheme {
        NewRecordScreen()
    }
}
