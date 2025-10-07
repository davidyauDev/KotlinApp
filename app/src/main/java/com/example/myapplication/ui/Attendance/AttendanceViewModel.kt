package com.example.myapplication.ui.Attendance
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.data.local.Attendance
import com.example.myapplication.data.local.AttendanceDatabase

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {


    private val context = application.applicationContext
    private val attendanceDao = AttendanceDatabase.getDatabase(application).attendanceDao()
    val lastAttendance: LiveData<Attendance?> = attendanceDao.getLastAttendance()

    private val _signalStrength = MutableLiveData<Int>()

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null

    init {
        setupSignalListener()
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
