package com.example.myapplication.data.model

data class LoginResponse(
    val status: Int,
    val code: String,
    val data: LoginData
)