package com.example.myapplication.data.repository

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.myapplication.data.local.AttendanceType
import com.example.myapplication.data.model.request.AttendanceRequest
import com.example.myapplication.data.model.response.AttendanceResponse
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import retrofit2.Response

class AttendanceRepository(
    private val userPreferences: UserPreferences,
    private val context: Context
) {
    suspend fun saveAttendance(
        latitude: Double,
        longitude: Double,
        type: AttendanceType
    ): Result<AttendanceResponse> {
        return try {
            val userId = userPreferences.userId.first() ?: 0
            if (userId == 0) return Result.failure(Exception("Usuario no logueado"))

            val token = userPreferences.userToken.first()
            if (token.isNullOrEmpty()) return Result.failure(Exception("Token no disponible"))

            val api = RetrofitClient.apiWithToken { token }

            val batteryPercentage = getBatteryPercentage()
            val networkType = getNetworkType()
            val isInternetAvailable = isInternetAvailable()

            val request = AttendanceRequest(
                user_id = userId,
                timestamp = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude,
                notes = if (type == AttendanceType.ENTRADA) {
                    "Inicio de jornada laboral en la oficina."
                } else {
                    "Fin de jornada laboral en la oficina."
                },
                device_model = Build.MODEL ?: "Unknown",
                battery_percentage = batteryPercentage,
                signal_strength = 4,
                network_type = networkType,
                is_internet_available = isInternetAvailable,
                type = if (type == AttendanceType.ENTRADA) "check_in" else "check_out"
            )

            val response: Response<AttendanceResponse> = api.sendAttendance(request)

            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Respuesta vacía"))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getBatteryPercentage(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "SIN_CONEXION"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "SIN_CONEXION"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOVIL"
            else -> "OTRO"
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
