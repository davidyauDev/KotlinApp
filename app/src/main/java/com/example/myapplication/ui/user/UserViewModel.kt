package com.example.myapplication.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.preferences.SessionManager
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

    private val _userEmpCode = MutableStateFlow("")
    val userEmpCode: StateFlow<String> = _userEmpCode

    init {
        // Initialize from in-memory SessionManager first (set at login)
        SessionManager.userName?.let { _userName.value = it }
        SessionManager.token?.let { _userToken.value = it }
        SessionManager.userId?.let { _userId.value = it }
        SessionManager.userEmail?.let { _userEmail.value = it }
        SessionManager.empCode?.let { _userEmpCode.value = it }

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
        viewModelScope.launch {
            userPreferences.userEmpCode.collect { _userEmpCode.value = it }
        }
    }

    fun saveUser(name: String, token: String, id: Int, email: String, empCode: String? = null) {
        viewModelScope.launch {
            userPreferences.saveUser(name, token, id, email, empCode = empCode ?: _userEmpCode.value.ifBlank { null })
            _userName.value = name
            _userToken.value = token
            _userId.value = id
            _userEmail.value = email
            _userEmpCode.value = empCode ?: _userEmpCode.value
        }
    }

    // Nuevo: actualizar solo en memoria (sin persistir)
    fun setUserInMemory(name: String, token: String, id: Int, email: String) {
        _userName.value = name
        _userToken.value = token
        _userId.value = id
        _userEmail.value = email
    }

    fun setEmpCodeInMemory(empCode: String?) {
        _userEmpCode.value = empCode ?: ""
    }

    fun saveEmpCode(empCode: String?) {
        viewModelScope.launch {
            userPreferences.saveUser(_userName.value, _userToken.value, _userId.value, _userEmail.value, empCode = empCode)
            _userEmpCode.value = empCode ?: ""
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