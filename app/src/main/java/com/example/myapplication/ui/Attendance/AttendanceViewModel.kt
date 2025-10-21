package com.example.myapplication.ui.Attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.myapplication.data.local.Attendance
import com.example.myapplication.data.repository.AttendanceRepository
import java.util.*

class AttendanceViewModel(
    application: Application,
    private val repository: AttendanceRepository
) : AndroidViewModel(application) {

    fun getLastAttendance(): LiveData<Attendance?> {
        return repository.getLastAttendance()
    }

    fun getAttendancesOfToday(): LiveData<List<Attendance>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis - 1

        return repository.getAttendancesBetween(startOfDay, endOfDay)
    }
}