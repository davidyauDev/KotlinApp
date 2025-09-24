package com.example.myapplication.data.repository

import com.example.myapplication.data.model.request.LoginRequest
import com.example.myapplication.data.model.response.LoginResponse
import com.example.myapplication.data.network.ApiService

class AuthRepository(private val api: ApiService) {
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Respuesta vacía"))
            } else {
                if (response.code() == 401) {
                    Result.failure(Exception("Credenciales incorrectas"))
                } else {
                    Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
