package com.example.myapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.myapplication.data.local.Attendance
import com.example.myapplication.data.local.AttendanceDao
import com.example.myapplication.data.local.AttendanceType
import com.example.myapplication.data.model.response.AttendanceResponse
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.preferences.SessionManager
import com.example.myapplication.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

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
            // Prefer in-memory session (set at login) to avoid "usuario no logueado" immediately after login
            val userId = SessionManager.userId ?: userPreferences.userId.first()
            if (userId == 0) return Result.failure(Exception("Usuario no logueado"))

            val token = SessionManager.token ?: userPreferences.userToken.first()
            if (token.isNullOrBlank()) return Result.failure(Exception("Token no disponible"))

            // Guardar la foto en filesDir para que persista entre reinicios
            val timestamp = System.currentTimeMillis()
            val photoFile = File(context.filesDir, "attendance_photo_${timestamp}.jpg")
            FileOutputStream(photoFile).use { out ->
                photo.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val batteryPercentage = getBatteryPercentage()
            val networkType = getNetworkType()
            val isInternetAvailable = isInternetAvailable()

            // Generar externalId para idempotencia
            val externalId = UUID.randomUUID().toString()

            // Crear entidad local y guardar con synced = false
            val attendanceEntity = Attendance(
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude,
                notes = if (type == AttendanceType.ENTRADA) "Inicio de jornada laboral" else "Fin de jornada laboral",
                deviceModel = Build.MODEL ?: "Unknown",
                batteryPercentage = batteryPercentage,
                signalStrength = 4,
                networkType = networkType,
                isInternetAvailable = isInternetAvailable,
                type = type,
                photoPath = photoFile.absolutePath,
                synced = false,
                externalId = externalId,
                address = null,
                retryCount = 0,
                serverId = null
            )

            val insertedId = dao.insert(attendanceEntity)

            // Intentar obtener dirección si hay internet (reverse geocode)
            if (isInternetAvailable) {
                val address = tryReverseGeocode(latitude, longitude)
                if (!address.isNullOrBlank()) {
                    dao.updateAddress(insertedId.toInt(), address)
                }
            }

            // Intentar enviar al servidor inmediatamente si hay internet
            if (isInternetAvailable) {
                val api = RetrofitClient.apiWithToken { token }

                fun String.toPart(): RequestBody = this.toRequestBody("text/plain".toMediaType())
                fun Int.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
                fun Long.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
                fun Double.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
                fun Boolean.toPart(): RequestBody = (if (this) "1" else "0")
                    .toRequestBody("text/plain".toMediaType())

                val photoRequestBody = photoFile.asRequestBody("image/jpeg".toMediaType())
                val photoPart = MultipartBody.Part.createFormData("photo", photoFile.name, photoRequestBody)

                val response: Response<AttendanceResponse> = api.sendAttendance(
                    userId.toPart(),
                    timestamp.toPart(),
                    latitude.toPart(),
                    longitude.toPart(),
                    attendanceEntity.notes.toPart(),
                    attendanceEntity.deviceModel.toPart(),
                    batteryPercentage.toPart(),
                    4.toPart(),
                    networkType.toPart(),
                    isInternetAvailable.toPart(),
                    (if (type == AttendanceType.ENTRADA) "check_in" else "check_out").toPart(),
                    externalId.toPart(),
                    photoPart
                )

                return if (response.isSuccessful) {
                    // Marcar como sincronizada en la DB
                    dao.markAsSynced(insertedId.toInt())

                    // Si el servidor devuelve un serverId, guardarlo
                    response.body()?.serverId?.let { serverId ->
                        try {
                            dao.updateServerId(insertedId.toInt(), serverId)
                        } catch (_: Exception) { }
                    }

                    // Borrar foto local para ahorrar espacio
                    try {
                        if (photoFile.exists()) photoFile.delete()
                    } catch (_: Exception) { }

                    response.body()?.let { Result.success(it) } ?: Result.failure(Exception("Respuesta vacía"))
                } else {
                    // incrementar contador de reintentos
                    dao.incrementRetryCount(insertedId.toInt())
                    val rawError = response.errorBody()?.string()
                    Log.e("API_ERROR", " Error ${response.code()}: $rawError")
                    Result.failure(Exception("Error ${response.code()}: $rawError"))
                }
            } else {
                // Sin internet: ya quedó guardada localmente para sincronizar después
                Result.success(AttendanceResponse(message = "Guardado localmente, pendiente de sincronización"))
            }

        } catch (e: Exception) {
            Log.e("API_EXCEPTION", " Excepción al enviar asistencia: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun tryReverseGeocode(lat: Double, lon: Double): String? {
        return try {
            withContext(Dispatchers.IO) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val list = geocoder.getFromLocation(lat, lon, 1)
                if (!list.isNullOrEmpty()) list[0].getAddressLine(0) else null
            }
        } catch (e: Exception) {
            Log.w("GEOCODER", "Reverse geocode failed: ${e.message}")
            null
        }
    }

    suspend fun syncUnsyncedAttendances(): List<Pair<Int, Boolean>> {
        // Retorna lista de pares (attendanceId, success)
        val results = mutableListOf<Pair<Int, Boolean>>()
        try {
            val token = SessionManager.token ?: userPreferences.userToken.first()
            if (token.isNullOrBlank()) return results
            if (!isInternetAvailable()) return results

            val api = RetrofitClient.apiWithToken { token }

            val unsynced = dao.getUnsyncedAttendances()
            for (att in unsynced) {
                try {
                    // si no tiene address, intentar geocode antes de enviar
                    if (att.address.isNullOrBlank()) {
                        val address = tryReverseGeocode(att.latitude, att.longitude)
                        if (!address.isNullOrBlank()) dao.updateAddress(att.id, address)
                    }

                    val photoFile = att.photoPath?.let { File(it) }
                    val photoPart = if (photoFile != null && photoFile.exists()) {
                        MultipartBody.Part.createFormData("photo", photoFile.name, photoFile.asRequestBody("image/jpeg".toMediaType()))
                    } else null

                    fun String.toPart(): RequestBody = this.toRequestBody("text/plain".toMediaType())
                    fun Int.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
                    fun Long.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
                    fun Double.toPart(): RequestBody = this.toString().toRequestBody("text/plain".toMediaType())
                    fun Boolean.toPart(): RequestBody = (if (this) "1" else "0").toRequestBody("text/plain".toMediaType())

                    // Construir externalId part
                    val externalIdPart = att.externalId.toPart()

                    val callResponse = if (photoPart != null) {
                        api.sendAttendance(
                            userPreferences.userId.first().toPart(),
                            att.timestamp.toPart(),
                            att.latitude.toString().toPart(),
                            att.longitude.toString().toPart(),
                            att.notes.toPart(),
                            att.deviceModel.toPart(),
                            att.batteryPercentage.toPart(),
                            att.signalStrength.toPart(),
                            att.networkType.toPart(),
                            att.isInternetAvailable.toPart(),
                            (if (att.type == AttendanceType.ENTRADA) "check_in" else "check_out").toPart(),
                            externalIdPart,
                            photoPart
                        )
                    } else {
                        val empty = "".toRequestBody("text/plain".toMediaType())
                        val emptyPart = MultipartBody.Part.createFormData("photo", "", empty)
                        api.sendAttendance(
                            userPreferences.userId.first().toPart(),
                            att.timestamp.toPart(),
                            att.latitude.toString().toPart(),
                            att.longitude.toString().toPart(),
                            att.notes.toPart(),
                            att.deviceModel.toPart(),
                            att.batteryPercentage.toPart(),
                            att.signalStrength.toPart(),
                            att.networkType.toPart(),
                            att.isInternetAvailable.toPart(),
                            (if (att.type == AttendanceType.ENTRADA) "check_in" else "check_out").toPart(),
                            externalIdPart,
                            emptyPart
                        )
                    }

                    if (callResponse.isSuccessful) {
                        // Marcar como sincronizada
                        dao.markAsSynced(att.id)

                        // Si el servidor devuelve serverId, actualízalo
                        try {
                            val body = callResponse.body()
                            body?.serverId?.let { sid -> dao.updateServerId(att.id, sid) }
                        } catch (_: Exception) { }

                        // Borrar la foto local si existe
                        try {
                            photoFile?.let { f -> if (f.exists()) f.delete() }
                        } catch (_: Exception) { }

                        results.add(Pair(att.id, true))
                    } else {
                        dao.incrementRetryCount(att.id)
                        results.add(Pair(att.id, false))
                    }

                } catch (e: Exception) {
                    Log.e("SYNC_ERROR", "Error al sincronizar asistencia ${att.id}: ${e.message}")
                    dao.incrementRetryCount(att.id)
                    results.add(Pair(att.id, false))
                }
            }

        } catch (e: Exception) {
            Log.e("SYNC_EXCEPTION", "Excepción durante sincronización: ${e.message}")
        }

        return results
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

    fun getAttendancesBetween(start: Long, end: Long): LiveData<List<Attendance>> =
        dao.getAttendancesBetween(start, end)

    fun getAllAttendances(): LiveData<List<Attendance>> =
        dao.getAllAttendances()

    fun getLastAttendance(): LiveData<Attendance?> {
        return dao.getLastAttendance()
    }

}
