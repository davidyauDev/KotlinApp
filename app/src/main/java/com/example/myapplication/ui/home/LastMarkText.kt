package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LastMarkText(viewModel: AttendanceViewModel) {
    val lastAttendanceState = viewModel.lastAttendance.observeAsState()
    val lastAttendance = lastAttendanceState.value

    val displayText = if (lastAttendance != null) {
        val date = Date(lastAttendance.timestamp)
        val formatted = SimpleDateFormat("HH:mm (MMM/dd)", Locale.getDefault()).format(date)
        val type = lastAttendance.type.name
        "Última marca de $type: $formatted"
    } else {
        "Sin registros"
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
