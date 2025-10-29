package com.example.myapplication.data.model.response

data class LoginResponse(
    val access_token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Int,
    val name: String,
    val email: String,
    val roles: List<String> = emptyList(),
    val emp_code: String? = null
)