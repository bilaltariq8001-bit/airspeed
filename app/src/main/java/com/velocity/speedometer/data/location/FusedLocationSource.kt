package com.velocity.speedometer.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.velocity.speedometer.domain.model.LocationSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FusedLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMs: Long): Flow<LocationSample?> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val safeInterval = intervalMs.coerceIn(500L, 5000L)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, safeInterval)
            .setMinUpdateIntervalMillis((safeInterval / 2L).coerceAtLeast(250L))
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    trySend(location.toSample())
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    trySend(null)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    private fun Location.toSample(): LocationSample = LocationSample(
        latitude = latitude,
        longitude = longitude,
        speedMps = if (hasSpeed()) speed else 0f,
        hasSpeed = hasSpeed(),
        altitudeMeters = if (hasAltitude()) altitude else 0.0,
        hasAltitude = hasAltitude(),
        accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
        bearingDegrees = if (hasBearing()) bearing else 0f,
        hasBearing = hasBearing(),
        timestampMs = if (time > 0L) time else System.currentTimeMillis()
    )
}
