package com.example.myapplication.ui.theme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.LoginRequest
import com.example.myapplication.data.model.UserData
import com.example.myapplication.data.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: UserData, val token: String) : LoginState()
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
                if (response.status == "success") {
                    val user = response.data.user
                    val token = response.data.token
                    _loginState.value = LoginState.Success(user, token)
                } else {
                    _loginState.value = LoginState.Error("Credenciales incorrectas")
                    Log.d("error", _loginState.value.toString())
                }
            } catch (e: Exception) {
                Log.e("LoginError", "Excepción: ${e.localizedMessage}", e)
                _loginState.value = LoginState.Error("Login fallido: ${e.localizedMessage}")
            }
        }
    }
}