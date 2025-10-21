package com.example.myapplication.ui.Attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.AttendanceType
import java.text.SimpleDateFormat
import androidx.compose.foundation.lazy.items
import java.util.*

data class RegistroAsistencia(
    val tipo: AttendanceType,
    val hora: String,
    val ubicacion: String,
    val fecha: String
)

@Composable
fun AttendanceScreen(
    attendanceViewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val attendancesToday by attendanceViewModel.getAttendancesOfToday().observeAsState(initial = emptyList())
    val registrosUi = attendancesToday.map { attendance ->
        val date = Date(attendance.timestamp)
        val sdfDate = SimpleDateFormat("dd MMM", Locale("es"))
        val sdfTime = SimpleDateFormat("hh:mm a", Locale("es"))
        RegistroAsistencia(
            tipo = attendance.type,
            hora = sdfTime.format(date),
            ubicacion = "Lat: ${attendance.latitude}, Lon: ${attendance.longitude}",
            fecha = sdfDate.format(date)
        )
    }

    val fechaHoy = SimpleDateFormat("dd MMM", Locale("es")).format(Date())
    val registrosHoy = registrosUi.filter { it.fecha.equals(fechaHoy, ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Text(
            text = "Asistencias de Hoy",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (registrosHoy.isEmpty()) {
                item {
                    Text(
                        text = "No hay registros hoy.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(registrosHoy) { registro ->
                    RegistroAsistenciaCard(registro)
                }
            }
        }
    }
}

@Composable
fun RegistroAsistenciaCard(registro: RegistroAsistencia) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = registro.tipo.name,
                    style = TextStyle(
                        color = if (registro.tipo == AttendanceType.ENTRADA) Color(0xFF4A90E2) else Color(0xFF7ED321),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = registro.hora,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = registro.ubicacion,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
        }
    }
}
