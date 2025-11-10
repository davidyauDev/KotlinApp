package com.example.myapplication.data.remote.dto.request

data class AttendanceRequest (
    val user_id: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val notes: String,
    val device_model: String,
    val battery_percentage: Int,
    val signal_strength: Int,
    val network_type: String,
    val is_internet_available: Boolean,
    val type: String
)