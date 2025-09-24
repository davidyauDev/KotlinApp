package com.example.myapplication.data.network

import com.example.myapplication.data.model.request.AttendanceRequest
import com.example.myapplication.data.model.request.LoginRequest
import com.example.myapplication.data.model.response.AttendanceResponse
import com.example.myapplication.data.model.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("attendances")
    suspend fun sendAttendance(@Body request: AttendanceRequest): Response<AttendanceResponse>

}