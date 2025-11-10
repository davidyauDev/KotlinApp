package com.example.myapplication.data.remote.dto.response

data class AttendanceResponse(
    val message: String,
    val timestamp: Long = 0L,
    // Nuevo: id asignado por el servidor si lo devuelve
    val serverId: Int? = null
)