package com.example.myapplication.data.model

data class Asistencia(
    val fecha: String, // ej. "9 oct"
    val hora: String,  // ej. "07:41 a. m."
    val lugar: String,
    val destino: String,
    val tipo: String, // "Entrada" o "Salida"
    val estado: String?, // null si fue exitosa, o "Cancelada"
    val monto: Double = 0.0 // opcional
)
