package com.example.myapplication.ui.home

import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Helper top-level para obtener la ubicación con timeout y limpieza segura del callback
suspend fun awaitLocationWithTimeout(client: FusedLocationProviderClient, timeoutMs: Long = 5000L): Location? {
    return withTimeoutOrNull(timeoutMs) {
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

