package com.example.myapplication.ui.home

import android.location.Location
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.example.myapplication.data.local.LocationDao
import com.example.myapplication.data.local.LocationEntity


sealed class LocationResult {
    data class Success(val location: Location) : LocationResult()
    data class Error(val reason: LocationError) : LocationResult()
}

enum class LocationError {
    PERMISSION_DENIED,
    TIMEOUT,
    GPS_DISABLED,
    NO_LOCATION_AVAILABLE,
    INACCURATE,
    UNKNOWN
}


suspend fun awaitLocationForAttendanceImproved(
    client: FusedLocationProviderClient,
    context: Context,
    dao: LocationDao,
    timeoutMs: Long = 5000L,
    maxAgeMs: Long = 60_000L,
    minAccuracyMeters: Float = 50f,
): LocationResult {
    try {
        val fine = ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            return LocationResult.Error(LocationError.PERMISSION_DENIED)
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            return LocationResult.Error(LocationError.GPS_DISABLED)
        }


        val freshLocation = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cts = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }

        if (freshLocation != null) {
            // guardar en Room la ubicación obtenida en vivo
            try {
                val e = LocationEntity(
                    latitude = freshLocation.latitude,
                    longitude = freshLocation.longitude,
                    accuracy = if (freshLocation.hasAccuracy()) freshLocation.accuracy else 0f,
                    timestamp = freshLocation.time
                )
                dao.insert(e)
            } catch (_: Exception) {
            }
            return LocationResult.Success(freshLocation)
        }

        val lastKnown = withTimeoutOrNull(1000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }

        if (lastKnown != null) {
            val ageMs = System.currentTimeMillis() - lastKnown.time
            val isRecent = ageMs <= maxAgeMs
            val isAccurate = lastKnown.hasAccuracy() && lastKnown.accuracy <= minAccuracyMeters
            if (isRecent && isAccurate) {
                // guardar en Room la ubicación lastKnown válida
                try {
                    val e = LocationEntity(
                        latitude = lastKnown.latitude,
                        longitude = lastKnown.longitude,
                        accuracy = if (lastKnown.hasAccuracy()) lastKnown.accuracy else 0f,
                        timestamp = lastKnown.time
                    )
                    dao.insert(e)
                } catch (_: Exception) {
                }
                return LocationResult.Success(lastKnown)
            } else {
                return LocationResult.Error(LocationError.INACCURATE)
            }
        }

        // Antes de devolver NO_LOCATION_AVAILABLE, intentar leer la última ubicación guardada en Room
        try {
            val stored = dao.getLastLocation()
            if (stored != null) {
                val loc = Location("room")
                loc.latitude = stored.latitude
                loc.longitude = stored.longitude
                try {
                    loc.accuracy = stored.accuracy
                } catch (_: Exception) {
                }
                loc.time = stored.timestamp
                return LocationResult.Success(loc)
            }
        } catch (_: Exception) {
        }

        return LocationResult.Error(LocationError.NO_LOCATION_AVAILABLE)
    } catch (_: SecurityException) {
        return LocationResult.Error(LocationError.PERMISSION_DENIED)
    } catch (_: Exception) {
        return LocationResult.Error(LocationError.UNKNOWN)
    }
}
