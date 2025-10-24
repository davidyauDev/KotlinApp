package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AttendanceType {
    ENTRADA,
    SALIDA
}

@Entity(tableName = "attendance_table")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val notes: String,
    val deviceModel: String,
    val batteryPercentage: Int,
    val signalStrength: Int, // Nivel de señal (0 a 4 normalmente)
    val networkType: String, // Ej: WIFI, LTE, 3G, 2G, SIN_CONEXION
    val isInternetAvailable: Boolean ,  // Si puede acceder a internet en ese momento
    val type: AttendanceType ,

    // Nuevo: ruta de la foto guardada localmente (opcional)
    val photoPath: String? = null,

    // Nuevo: si la asistencia ya fue enviada al servidor
    val synced: Boolean = false,

    // Nuevo: external id para idempotencia (UUID generado por cliente)
    val externalId: String = "",

    // Nuevo: dirección human-readable si disponible (reverse geocoding)
    val address: String? = null,

    // Nuevo: contador de reintentos
    val retryCount: Int = 0,

    // Nuevo: id retornado por el servidor (opcional)
    val serverId: Int? = null

)
