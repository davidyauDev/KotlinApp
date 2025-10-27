package com.example.myapplication.ui.home

import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Helper top-level para obtener la ubicación con timeout y limpieza segura del callback
suspend fun awaitLocationWithTimeout(client: FusedLocationProviderClient, timeoutMs: Long = 5000L): Location? {
    return withTimeoutOrNull(timeoutMs) {
        // 1) Try lastLocation quickly (fast path)
        val last = withTimeoutOrNull(1000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                try {
                    try {
                        client.lastLocation.addOnSuccessListener { loc ->
                            if (!cont.isCompleted) cont.resume(loc)
                        }.addOnFailureListener {
                            if (!cont.isCompleted) cont.resume(null)
                        }
                    } catch (_: SecurityException) {
                        // Permission missing -> resume with null
                        if (!cont.isCompleted) cont.resume(null)
                    }
                } catch (_: Exception) {
                    if (!cont.isCompleted) cont.resume(null)
                }

                cont.invokeOnCancellation { /* nothing to cancel for lastLocation */ }
            }
        }
        if (last != null) return@withTimeoutOrNull last

        // 2) Try getCurrentLocation (preferred, uses fused provider to get a fresh fix)
        val current = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cts = CancellationTokenSource()
                try {
                    try {
                        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                            .addOnSuccessListener { loc ->
                                if (!cont.isCompleted) cont.resume(loc)
                            }
                            .addOnFailureListener {
                                if (!cont.isCompleted) cont.resume(null)
                            }
                    } catch (_: SecurityException) {
                        if (!cont.isCompleted) cont.resume(null)
                    }
                } catch (_: Exception) {
                    if (!cont.isCompleted) cont.resume(null)
                }

                cont.invokeOnCancellation { try { cts.cancel() } catch (_: Exception) { } }
            }
        }
        if (current != null) return@withTimeoutOrNull current

        // 3) Fallback to requestLocationUpdates (as before) with single update
        suspendCancellableCoroutine<Location?> { cont ->
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setMaxUpdates(1)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    try {
                        client.removeLocationUpdates(this)
                    } catch (_: Exception) { }
                    if (!cont.isCompleted) cont.resume(result.lastLocation)
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    // no-op
                }
            }

            try {
                client.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            } catch (_: SecurityException) {
                if (!cont.isCompleted) cont.resume(null)
            }

            cont.invokeOnCancellation {
                try {
                    client.removeLocationUpdates(callback)
                } catch (_: Exception) { }
            }
        }
    }
}
