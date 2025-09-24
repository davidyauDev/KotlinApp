package com.example.myapplication.data.model

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val roles: List<String>
)