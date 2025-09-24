package com.example.myapplication.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.UserData
import com.example.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: UserData, val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = repository.login(email, password)
            result.onSuccess { response ->
                val user = UserData(
                    id = response.user.id,
                    name = response.user.name,
                    email = response.user.email,
                    roles = response.user.roles
                )
                val token = response.access_token
                _loginState.value = LoginState.Success(user, token)
            }.onFailure { e ->
                _loginState.value = LoginState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}