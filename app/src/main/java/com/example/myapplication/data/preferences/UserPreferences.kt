package com.example.myapplication.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val USER_NAME_KEY = stringPreferencesKey("USER_NAME")
        val USER_TOKEN_KEY = stringPreferencesKey("USER_TOKEN")
        val USER_ID_KEY = intPreferencesKey("USER_ID")
        val USER_EMAIL_KEY = stringPreferencesKey("USER_EMAIL")
        val USER_EMP_CODE_KEY = stringPreferencesKey("USER_EMP_CODE")

    }

    val userName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME_KEY] ?: "" }

    val userToken: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_TOKEN_KEY] ?: "" }

    val userId: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[USER_ID_KEY] ?: 0 }

    val userEmail: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_EMAIL_KEY] ?: "" }

    val userEmpCode: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_EMP_CODE_KEY] ?: "" }

    suspend fun saveUser(name: String, token: String, id: Int, email: String, empCode: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
            preferences[USER_TOKEN_KEY] = token
            preferences[USER_ID_KEY] = id
            preferences[USER_EMAIL_KEY] = email
            if (empCode != null) preferences[USER_EMP_CODE_KEY] = empCode
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.clear() }
    }
}