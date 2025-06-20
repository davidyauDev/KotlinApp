package com.example.myapplication.data.model

data class LoginResponse(
    val status: String,
    val code: String,
    val data: LoginData,
    val message: String,
)