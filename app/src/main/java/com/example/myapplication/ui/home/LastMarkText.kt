package com.example.myapplication.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LastMarkText(viewModel: AttendanceViewModel) {
    val lastAttendanceState = viewModel.getLastAttendance().observeAsState()
    val lastAttendance = lastAttendanceState.value

    val displayText = if (lastAttendance != null) {
        val date = Date(lastAttendance.timestamp)
        val formatted = SimpleDateFormat("hh:mm a · dd MMM", Locale.getDefault()).format(date)
        val type = lastAttendance.type.name.lowercase().replaceFirstChar { it.uppercase() }
        "Última marca de $type: $formatted"
    } else {
        "No se ha registrado ninguna asistencia."
    }

    AnimatedContent(
        targetState = displayText,
        label = "Última Marca Texto"
    ) { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
