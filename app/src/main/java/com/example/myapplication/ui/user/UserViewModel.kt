package com.example.myapplication.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _userToken = MutableStateFlow("")
    val userToken: StateFlow<String> = _userToken

    private val _userId = MutableStateFlow(0)
    val userId: StateFlow<Int> = _userId

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail

    init {
        viewModelScope.launch {
            userPreferences.userName.collect { _userName.value = it }
        }
        viewModelScope.launch {
            userPreferences.userToken.collect { _userToken.value = it }
        }
        viewModelScope.launch {
            userPreferences.userId.collect { _userId.value = it }
        }
        viewModelScope.launch {
            userPreferences.userEmail.collect { _userEmail.value = it }
        }
    }

    fun saveUser(name: String, token: String, id: Int, email: String) {
        viewModelScope.launch {
            userPreferences.saveUser(name, token, id, email)
            _userName.value = name
            _userToken.value = token
            _userId.value = id
            _userEmail.value = email
        }
    }

    fun clearUser() {
        viewModelScope.launch {
            userPreferences.clearUser()
            _userName.value = ""
            _userToken.value = ""
            _userId.value = 0
            _userEmail.value = ""
        }
    }
}