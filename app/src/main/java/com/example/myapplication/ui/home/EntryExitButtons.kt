package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.entity.AttendanceType

@Suppress("DEPRECATION")
@Composable
fun EntryExitButtons(
    onEntry: () -> Unit,
    onExit: () -> Unit,
    onLogout: () -> Unit,
    onViewRoutes: () -> Unit = {},
    isBusy: Boolean = false,
    activeType: AttendanceType = AttendanceType.ENTRADA
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
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
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Entrada",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ENTRADA",
                    fontSize = 15.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
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
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Salida",
                        tint = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SALIDA",
                    color = Color.Black,
                    fontSize = 15.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Nuevo: botón para ver rutas (abre el modal de Rutas del día)
        OutlinedButton(
            onClick = onViewRoutes,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isBusy
        ) {
            Text(text = "Ver rutas")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0051A8),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isBusy
        ) {
            Icon(
                imageVector = Icons.Filled.ExitToApp,
                contentDescription = "Cerrar sesión",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cerrar Sesión",
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}
