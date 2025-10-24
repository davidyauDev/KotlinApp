package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.AttendanceType
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.sp

@Composable
fun EntryExitButtons(
    onEntry: () -> Unit,
    onExit: () -> Unit,
    onLogout: () -> Unit,
    isBusy: Boolean = false,
    activeType: AttendanceType = AttendanceType.ENTRADA
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onEntry,
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                if (isBusy && activeType == AttendanceType.ENTRADA) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Entrada",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = "ENTRADA", fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = onExit,
                enabled = !isBusy,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                if (isBusy && activeType == AttendanceType.SALIDA) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Salida",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = "SALIDA", color = Color.Black, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0051A8),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp),
            enabled = !isBusy
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Cerrar sesión", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión", fontSize = 14.sp)
        }
    }
}