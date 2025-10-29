package com.example.myapplication.data.preferences

object SessionManager {
    @Volatile
    var userId: Int? = null
    @Volatile
    var token: String? = null
    @Volatile
    var userName: String? = null
    @Volatile
    var userEmail: String? = null
    @Volatile
    var empCode: String? = null

    fun setSession(
        userId: Int,
        token: String,
        name: String,
        email: String,
        empCode: String? = null
    ) {
        this.userId = userId
        this.token = token
        this.userName = name
        this.userEmail = email
        this.empCode = empCode
    }

    fun clear() {
        this.userId = null
        this.token = null
        this.userName = null
        this.userEmail = null
        this.empCode = null
    }
}
