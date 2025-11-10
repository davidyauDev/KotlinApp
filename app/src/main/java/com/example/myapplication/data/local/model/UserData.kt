package com.example.myapplication.data.local.model

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val roles: List<String> = emptyList(),
    val empCode: String? = null
)