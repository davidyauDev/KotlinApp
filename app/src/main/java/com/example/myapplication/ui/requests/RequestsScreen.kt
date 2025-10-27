package com.example.myapplication.ui.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon

// Simple data model for previewing the UI
private data class Solicitud(
    val id: String,
    val user: String,
    val date: String,
    val type: String,
    val status: RequestStatus
)

private enum class RequestStatus { PENDING, APPROVED, REJECTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(modifier: Modifier = Modifier) {
    // Sample list (in a real app this will come from ViewModel / repository)
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }

    val samples = remember {
        listOf(
            Solicitud("1", "María López", "Hoy 08:15", "Entrada", RequestStatus.PENDING),
            Solicitud("2", "Carlos Pérez", "Ayer 17:42", "Salida", RequestStatus.APPROVED),
            Solicitud("3", "Ana Gómez", "Hoy 09:03", "Entrada", RequestStatus.REJECTED),
            Solicitud("4", "Luis Martinez", "Hoy 07:58", "Entrada", RequestStatus.PENDING)
        )
    }

    val filtered = remember(query, selectedFilter) {
        samples.filter { s ->
            val matchesQuery = query.isBlank() || (s.user + " " + s.type).contains(query, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Pendientes" -> s.status == RequestStatus.PENDING
                "Aprobadas" -> s.status == RequestStatus.APPROVED
                "Rechazadas" -> s.status == RequestStatus.REJECTED
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Solicitudes", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.weight(1f))
            // Placeholder for a quick action icon
            IconButton(onClick = { /* future: open filters/settings */ }) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Opciones")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar por nombre o tipo (ej. 'Entrada')") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Buscar")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filters as small chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = listOf("Todas", "Pendientes", "Aprobadas", "Rechazadas")
            options.forEach { opt ->
                FilterChip(
                    selected = (opt == selectedFilter),
                    onClick = { selectedFilter = opt },
                    label = { Text(opt) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content list
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No hay solicitudes que coincidan.", color = Color.Gray)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { s ->
                    SolicitudCard(solicitud = s)
                }
            }
        }
    }
}

@Composable
private fun SolicitudCard(solicitud: Solicitud) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)) {

            // Avatar / placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Person, contentDescription = "Usuario", tint = Color(0xFF666666))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = solicitud.user, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.weight(1f))
                    StatusPill(status = solicitud.status)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(text = solicitud.type, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = solicitud.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { /* ver detalles */ }, enabled = false) {
                        Text("Ver")
                    }
                    OutlinedButton(onClick = { /* acciones (aprobar/rechazar) */ }, enabled = false) {
                        Text("Acciones")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: RequestStatus) {
    val (text, color) = when (status) {
        RequestStatus.PENDING -> "Pendiente" to Color(0xFFFFA000)
        RequestStatus.APPROVED -> "Aprobada" to Color(0xFF4CAF50)
        RequestStatus.REJECTED -> "Rechazada" to Color(0xFFF44336)
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = color, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
