package com.example.myapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.example.myapplication.data.local.Attendance
import com.example.myapplication.data.local.AttendanceDao
import com.example.myapplication.data.local.AttendanceType
import com.example.myapplication.data.model.response.AttendanceResponse
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class AttendanceRepository(
    private val userPreferences: UserPreferences,
    private val context: Context,
    private val dao: AttendanceDao
) {

    suspend fun saveAttendance(
        latitude: Double,
        longitude: Double,
        type: AttendanceType,
        photo: Bitmap
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

            fun String.toPart(): RequestBody = this.toRequestBody("text/plain".toMediaType())
            fun Int.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
            fun Long.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
            fun Double.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
            fun Boolean.toPart(): RequestBody = (if (this) "1" else "0")
                .toRequestBody("text/plain".toMediaType())

            val photoFile = File.createTempFile("attendance_photo", ".jpg", context.cacheDir)
            FileOutputStream(photoFile).use { out ->
                photo.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val photoRequestBody = photoFile.asRequestBody("image/jpeg".toMediaType())
            val photoPart =
                MultipartBody.Part.createFormData("photo", photoFile.name, photoRequestBody)

            val response: Response<AttendanceResponse> = api.sendAttendance(
                userId.toPart(),
                System.currentTimeMillis().toPart(),
                latitude.toPart(),
                longitude.toPart(),
                (if (type == AttendanceType.ENTRADA) "Inicio de jornada laboral"
                else "Fin de jornada laboral").toPart(),
                (Build.MODEL ?: "Unknown").toPart(),
                batteryPercentage.toPart(),
                4.toPart(),
                networkType.toPart(),
                isInternetAvailable.toPart(),
                (if (type == AttendanceType.ENTRADA) "check_in" else "check_out").toPart(),
                photoPart
            )

            val attendanceEntity = Attendance(
                timestamp = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude,
                notes = if (type == AttendanceType.ENTRADA) "Inicio de jornada laboral" else "Fin de jornada laboral",
                deviceModel = Build.MODEL ?: "Unknown",
                batteryPercentage = batteryPercentage,
                signalStrength = 4, // Podrías calcularlo de forma real
                networkType = networkType,
                isInternetAvailable = isInternetAvailable,
                type = type
            )
            dao.insert(attendanceEntity)
            if (response.isSuccessful) {
                response.body()?.let {
                    Log.d("API_SUCCESS", " ${it.message}")
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía"))
            } else {
                val rawError = response.errorBody()?.string()
                Log.e("API_ERROR", " Error ${response.code()}: $rawError")
                Result.failure(Exception("Error ${response.code()}: $rawError"))
            }
        } catch (e: Exception) {
            Log.e("API_EXCEPTION", " Excepción al enviar asistencia: ${e.message}", e)
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
