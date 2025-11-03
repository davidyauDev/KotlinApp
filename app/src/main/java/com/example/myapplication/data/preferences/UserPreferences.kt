package com.example.myapplication.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val USER_NAME_KEY = stringPreferencesKey("USER_NAME")
        val USER_TOKEN_KEY = stringPreferencesKey("USER_TOKEN")
        val USER_ID_KEY = intPreferencesKey("USER_ID")
        val USER_EMAIL_KEY = stringPreferencesKey("USER_EMAIL")
        val USER_EMP_CODE_KEY = stringPreferencesKey("USER_EMP_CODE")

    }

    init {
        // Ejecutar una migración asíncrona: si por alguna razón el valor de USER_ID se guardó como String,
        // intentar parsearlo a Int y reescribirlo como entero para evitar ClassCastException.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.dataStore.data.first()
                val stringKey = stringPreferencesKey("USER_ID")
                val rawString = prefs[stringKey]
                if (rawString != null) {
                    val parsed = rawString.toIntOrNull() ?: 0
                    context.dataStore.edit { mutable ->
                        mutable[USER_ID_KEY] = parsed
                        mutable.remove(stringKey)
                    }
                }
            } catch (_: Exception) {
                // Si falla la migración, no detener la app; se seguirá usando el 0 por defecto.
            }
        }
    }

    val userName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME_KEY] ?: "" }

    val userToken: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_TOKEN_KEY] ?: "" }

    val userId: Flow<Int> = context.dataStore.data
        .map { preferences ->
            try {
                // Intentar obtener como Int (caso normal)
                preferences[USER_ID_KEY] ?: 0
            } catch (_: ClassCastException) {
                // Si el valor en el DataStore es una String por alguna migración previa o error,
                // intentar leer la versión String y parsearla a Int sin provocar crash.
                try {
                    val stringKey = stringPreferencesKey("USER_ID")
                    val raw = preferences[stringKey]
                    raw?.toIntOrNull() ?: 0
                } catch (_: Exception) {
                    0
                }
            }
        }

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