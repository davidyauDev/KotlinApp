package com.example.myapplication.ui.Attendance
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.Attendance
import com.example.myapplication.data.local.AttendanceDatabase
import com.example.myapplication.data.local.AttendanceType
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val attendanceDao = AttendanceDatabase.getDatabase(application).attendanceDao()
    val lastAttendance: LiveData<Attendance?> = attendanceDao.getLastAttendance()

    private val _signalStrength = MutableLiveData<Int>()
    val signalStrength: LiveData<Int> get() = _signalStrength

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null

    init {
        setupSignalListener()
    }

    fun saveAttendance(latitude: Double, longitude: Double, notes: String = "", type: AttendanceType) {
        viewModelScope.launch {
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            val batteryPercentage = getBatteryPercentage(context)
            val currentSignalStrength = signalStrength.value ?: -1
            val networkType = getNetworkType(context)
            val isInternetAvailable = isInternetAvailable(context)

            val attendance = Attendance(
                timestamp = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude,
                notes = notes,
                deviceModel = deviceModel,
                batteryPercentage = batteryPercentage,
                networkType = networkType,
                isInternetAvailable = isInternetAvailable,
                signalStrength = currentSignalStrength,
                type = type
            )
            attendanceDao.insert(attendance)
        }
    }

    private fun getBatteryPercentage(context: Context): Int {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level != -1 && scale != -1) {
            (level * 100) / scale
        } else 0
    }

    private fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "SIN_CONEXION"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "SIN_CONEXION"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOVIL"
            else -> "OTRO"
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @SuppressLint("MissingPermission")
    private fun setupSignalListener() {
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                super.onSignalStrengthsChanged(signalStrength)
                val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    signalStrength.level
                } else {
                    val strength = signalStrength.gsmSignalStrength
                    when {
                        strength <= 2 || strength == 99 -> 0
                        strength >= 12 -> 4
                        strength >= 8 -> 3
                        strength >= 5 -> 2
                        else -> 1
                    }
                }
                _signalStrength.postValue(level)
            }
        }

        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    override fun onCleared() {
        super.onCleared()
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }
}
