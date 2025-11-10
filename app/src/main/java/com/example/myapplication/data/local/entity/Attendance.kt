package com.example.myapplication.data.local.entity

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
    val signalStrength: Int,
    val networkType: String,
    val isInternetAvailable: Boolean,
    val type: AttendanceType,
    val photoPath: String? = null,
    val synced: Boolean = false,
    val externalId: String = "",
    val address: String? = null,
    val retryCount: Int = 0,
    val serverId: Int? = null

)
