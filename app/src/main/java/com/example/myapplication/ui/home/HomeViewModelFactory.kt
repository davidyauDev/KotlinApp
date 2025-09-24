package com.example.myapplication.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.AttendanceRepository

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userPreferences = UserPreferences(context)
        val repository = AttendanceRepository(userPreferences, context)
        return HomeViewModel(repository) as T
    }
}
