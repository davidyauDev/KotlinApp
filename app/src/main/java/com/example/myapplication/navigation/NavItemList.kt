package com.example.myapplication.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import com.example.myapplication.data.model.NavItem

object NavItemList {
    val navItemList = listOf(
        NavItem("Inicio", Icons.Default.Home),
        NavItem("Asistencias", Icons.Default.Person),
        NavItem("Solicitudes", Icons.Default.Settings)
    )
}