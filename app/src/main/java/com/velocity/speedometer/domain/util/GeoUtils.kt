package com.velocity.speedometer.domain.util

import com.velocity.speedometer.domain.model.GpsSignalQuality
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    const val MAX_ACCEPTABLE_ACCURACY_METERS = 30f
    const val GOOD_ACCURACY_METERS = 10f
    private const val MAX_IMPLIED_SPEED_MPS = 140.0 // ~504 km/h sanity cap for ground vehicles

    fun haversineDistanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun isReasonableMove(
        distanceMeters: Double,
        deltaTimeMs: Long
    ): Boolean {
        if (distanceMeters < 0.0 || deltaTimeMs <= 0L) return false
        val seconds = deltaTimeMs / 1000.0
        val impliedSpeed = distanceMeters / seconds
        return impliedSpeed <= MAX_IMPLIED_SPEED_MPS
    }

    fun classifyGpsQuality(accuracyMeters: Float?): GpsSignalQuality {
        if (accuracyMeters == null || accuracyMeters <= 0f) return GpsSignalQuality.POOR_OR_NONE
        return when {
            accuracyMeters < GOOD_ACCURACY_METERS -> GpsSignalQuality.GOOD
            accuracyMeters <= MAX_ACCEPTABLE_ACCURACY_METERS -> GpsSignalQuality.MODERATE
            else -> GpsSignalQuality.POOR_OR_NONE
        }
    }

    enum class CardinalDirection {
        N, NE, E, SE, S, SW, W, NW
    }

    fun bearingToCardinal(bearingDegrees: Float?): CardinalDirection? {
        if (bearingDegrees == null || bearingDegrees.isNaN()) return null
        val normalized = ((bearingDegrees % 360f) + 360f) % 360f
        val index = (((normalized + 22.5f) / 45f).toInt()) % 8
        return CardinalDirection.entries[index]
    }
}
