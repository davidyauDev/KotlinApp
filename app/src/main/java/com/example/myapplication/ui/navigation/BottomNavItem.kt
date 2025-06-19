package com.example.myapplication.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Asistencia : BottomNavItem(
        route = "asistencia",
        label = "Asistencia",
        icon = Icons.Filled.LocationOn
    )

    object Configuracion : BottomNavItem(
        route = "configuracion",
        label = "Configuración",
        icon = Icons.Filled.Settings
    )
}
