package com.example.myapplication.ui.home

import android.location.Location
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Obtiene una ubicación precisa y actual, ideal para registrar asistencia.
 *
 * Estrategia:
 *   Primero intenta obtener una ubicación NUEVA con alta precisión (GPS/red).
 *   Si falla, usa la última ubicación conocida solo si es reciente (<1 min) y precisa (<50m).
 *   No utiliza requestLocationUpdates (no es necesario para este caso puntual).
 *
 * @param client Cliente de ubicación (FusedLocationProviderClient).
 * @param timeoutMs Tiempo máximo para obtener la ubicación (por defecto 5s).
 * @param maxAgeMs Antigüedad máxima permitida para aceptar una ubicación del caché (por defecto 1 min).
 * @param minAccuracyMeters Precisión mínima aceptable (por defecto 50 metros).
 * @param preferLastKnownAny Si true, intenta primero la última ubicación conocida y la devuelve sin validar edad/precisión (útil para respuestas inmediatas).
 * @return Devuelve la mejor ubicación válida o null si no se pudo obtener.
 */
suspend fun awaitLocationForAttendance(
    client: FusedLocationProviderClient,
    timeoutMs: Long = 5000L,
    maxAgeMs: Long = 60_000L,
    minAccuracyMeters: Float = 50f,
    preferLastKnownAny: Boolean = false
): Location? {
    // Si el llamante prefiere aceptar cualquier lastKnown rápidamente, probarla primero
    if (preferLastKnownAny) {
        val lastAny = withTimeoutOrNull(1000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                try {
                    client.lastLocation
                        .addOnSuccessListener { loc -> if (!cont.isCompleted) cont.resume(loc) }
                        .addOnFailureListener { if (!cont.isCompleted) cont.resume(null) }
                } catch (_: SecurityException) {
                    if (!cont.isCompleted) cont.resume(null)
                } catch (_: Exception) {
                    if (!cont.isCompleted) cont.resume(null)
                }
                cont.invokeOnCancellation { /* sin acción */ }
            }
        }
        if (lastAny != null) return lastAny
        // si no había lastKnown, se procede a intentar obtener ubicación fresca
    }
    //  Intentar obtener una ubicación nueva (GPS o red, alta precisión)
    val freshLocation = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<Location?> { cont ->
            val cts = CancellationTokenSource()
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        // Si obtenemos ubicación, reanudamos
                        if (!cont.isCompleted) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (!cont.isCompleted) cont.resume(null)
                    }
            } catch (_: SecurityException) {
                // Si faltan permisos, devolvemos null
                if (!cont.isCompleted) cont.resume(null)
            } catch (_: Exception) {
                // Otros errores inesperados
                if (!cont.isCompleted) cont.resume(null)
            }

            // Cancelar si la corrutina se cancela
            cont.invokeOnCancellation {
                try { cts.cancel() } catch (_: Exception) { }
            }
        }
    }

    // Si se obtuvo una ubicación fresca, la devolvemos directamente
    if (freshLocation != null) return freshLocation

    // 2️⃣ Como respaldo, intentar la última ubicación conocida (rápida pero potencialmente vieja)
    val lastKnown = withTimeoutOrNull(1000L) {
        suspendCancellableCoroutine<Location?> { cont ->
            try {
                client.lastLocation
                    .addOnSuccessListener { loc ->
                        if (!cont.isCompleted) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (!cont.isCompleted) cont.resume(null)
                    }
            } catch (_: SecurityException) {
                if (!cont.isCompleted) cont.resume(null)
            } catch (_: Exception) {
                if (!cont.isCompleted) cont.resume(null)
            }
            cont.invokeOnCancellation { /* sin acción */ }
        }
    }

    // Validar si la última ubicación conocida es reciente y precisa
    if (lastKnown != null) {
        val ageMs = System.currentTimeMillis() - lastKnown.time
        val isRecent = ageMs <= maxAgeMs
        val isAccurate = lastKnown.hasAccuracy() && lastKnown.accuracy <= minAccuracyMeters

        // Solo devolver si cumple ambos criterios
        if (isRecent && isAccurate) {
            return lastKnown
        }
    }

    // 3️⃣ Si no hay ubicación válida, devolver null
    return null
}