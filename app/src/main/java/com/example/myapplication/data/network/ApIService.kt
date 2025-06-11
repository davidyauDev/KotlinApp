package com.example.myapplication.data.network

import com.example.myapplication.data.model.LoginRequest
import com.example.myapplication.data.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}