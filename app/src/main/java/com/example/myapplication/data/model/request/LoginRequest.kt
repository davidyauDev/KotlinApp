package com.example.myapplication.data.model.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("emp_code")
    val empCode: String,
    val password: String
)