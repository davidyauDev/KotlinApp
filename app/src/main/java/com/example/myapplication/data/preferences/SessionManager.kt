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

    fun setSession(userId: Int, token: String, name: String, email: String) {
        this.userId = userId
        this.token = token
        this.userName = name
        this.userEmail = email
    }

    fun clear() {
        this.userId = null
        this.token = null
        this.userName = null
        this.userEmail = null
    }
}

