package com.example.myapplication.ui.theme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.LoginRequest
import com.example.myapplication.data.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val username: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = RetrofitInstance.api.login(LoginRequest(email, password))
                val username = response.data.userData.name
                val token = response.data.accessToken
                _loginState.value = LoginState.Success(username)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Login fallido: ${e.localizedMessage}")
            }
        }
    }
}