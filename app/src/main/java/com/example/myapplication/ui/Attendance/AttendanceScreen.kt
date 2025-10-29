package com.example.myapplication.ui.Attendance

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.AttendanceType
import java.text.SimpleDateFormat
import java.util.*

data class RegistroAsistencia(
    val tipo: AttendanceType,
    val hora: String,
    val ubicacion: String,
    val fecha: String,
    val lat: Double,
    val lon: Double,
    val synced: Boolean
)

private enum class DayFilter { ALL, CUSTOM }

@Composable
fun AttendanceScreen(
    attendanceViewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    // Fecha inicial (hoy)
    val todayRange = remember {
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis - 1
        Pair(start, end)
    }

    var selectedFilter by remember { mutableStateOf(DayFilter.CUSTOM) }
    var customRange by remember { mutableStateOf<Pair<Long, Long>?>(todayRange) }
    var selectedDateLabel by remember {
        mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date(todayRange.first)))
    }

    fun rangeForFilter(filter: DayFilter): Pair<Long, Long>? {
        return when (filter) {
            DayFilter.ALL -> null
            DayFilter.CUSTOM -> customRange
        }
    }

    val selectedRange = rangeForFilter(selectedFilter)

    val attendanceListLiveData = selectedRange?.let { (start, end) ->
        attendanceViewModel.getAttendancesBetween(start, end)
    } ?: attendanceViewModel.getAllAttendances()

    val allAttendances by attendanceListLiveData.observeAsState(initial = emptyList())

    val context = LocalContext.current

    fun openDatePicker() {
        val c = Calendar.getInstance()
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val end = cal.timeInMillis - 1
                customRange = Pair(start, end)
                selectedDateLabel = SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date(start))
                selectedFilter = DayFilter.CUSTOM
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    val sdfDate = SimpleDateFormat("dd MMM", Locale("es"))
    val sdfTime = SimpleDateFormat("hh:mm a", Locale("es"))

    val registrosUi = allAttendances
        .sortedByDescending { it.timestamp }
        .map { attendance ->
            val date = Date(attendance.timestamp)
            RegistroAsistencia(
                tipo = attendance.type,
                hora = sdfTime.format(date),
                ubicacion = attendance.address?.takeIf { it.isNotBlank() }
                    ?: "Lat: ${attendance.latitude}, Lon: ${attendance.longitude}",
                fecha = sdfDate.format(date),
                lat = attendance.latitude,
                lon = attendance.longitude,
                synced = attendance.synced
            )
        }

    val headerText = when (selectedFilter) {
        DayFilter.ALL -> "Asistencias - Todas"
        DayFilter.CUSTOM -> "Asistencias - $selectedDateLabel"
    }

    Column(modifier = modifier.fillMaxSize()) {

        // 🔵 HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0051A8))
                .padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Historial de Asistencias",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // 🔹 CONTENIDO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // FILTROS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { openDatePicker() }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendario",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = selectedDateLabel)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (selectedFilter == DayFilter.CUSTOM) {
                        TextButton(onClick = {
                            customRange = todayRange
                            selectedDateLabel =
                                SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date(todayRange.first))
                            selectedFilter = DayFilter.CUSTOM
                        }) {
                            Text("Borrar")
                        }
                    }
                }

                FilterChip(
                    selected = selectedFilter == DayFilter.ALL,
                    onClick = {
                        if (selectedFilter == DayFilter.ALL) {
                            selectedFilter = DayFilter.CUSTOM
                            customRange = todayRange
                            selectedDateLabel =
                                SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date(todayRange.first))
                        } else {
                            selectedFilter = DayFilter.ALL
                            customRange = null
                            selectedDateLabel = ""
                        }
                    },
                    label = { Text(if (selectedFilter == DayFilter.ALL) "Ver personalizado" else "Todas") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TÍTULO SECCIÓN
            Text(
                text = "$headerText (${registrosUi.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // LISTA
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (registrosUi.isEmpty()) {
                    item {
                        Text(
                            text = "No hay registros.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    groupedForLazy(registrosUi)
                }
            }
        }
    }
}

// 🔹 Agrupación por fecha
private fun LazyListScope.groupedForLazy(items: List<RegistroAsistencia>) {
    val grouped = items.groupBy { it.fecha }.toSortedMap(compareByDescending { it })
    grouped.forEach { (fecha, list) ->
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = fecha,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        items(list) { registro ->
            RegistroAsistenciaCard(registro)
        }
    }
}

// 🔹 Tarjeta visualmente mejorada
@Composable
fun RegistroAsistenciaCard(registro: RegistroAsistencia) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = if (registro.tipo == AttendanceType.ENTRADA) Color(0xFF2196F3) else Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 6.dp)
                )

                Text(
                    text = registro.tipo.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = registro.hora,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (registro.synced) "✔ Sincronizado" else "⚠ Pendiente",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (registro.synced) Color(0xFF4CAF50) else Color(0xFFFFA000),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = registro.ubicacion,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 2,
                modifier = Modifier.padding(start = 26.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    val uri =
                        "geo:${registro.lat},${registro.lon}?q=${registro.lat},${registro.lon}(${Uri.encode(registro.ubicacion)})".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Abrir mapa",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(registro.ubicacion))
                    Toast.makeText(context, "Ubicación copiada", Toast.LENGTH_SHORT).show()
                }) {
                    Text(
                        text = "Copiar",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
