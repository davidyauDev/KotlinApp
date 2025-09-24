package com.example.myapplication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AttendanceType
import com.example.myapplication.data.repository.AttendanceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar

    fun updateClock() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value =
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                delay(1000)
            }
        }
    }

    fun updateDate() {
        _currentDate.value =
            SimpleDateFormat("EEEE dd, MMM yyyy", Locale("es")).format(Date())
    }

    fun saveAttendance(latitude: Double, longitude: Double, type: AttendanceType) {
        viewModelScope.launch {
            val result = attendanceRepository.saveAttendance(latitude, longitude, type)
            _snackbar.value = result.fold(
                onSuccess = { "✔️ Registro enviado correctamente" },
                onFailure = { "❌ Error al registrar: ${it.message}" }
            )
        }
    }
}
