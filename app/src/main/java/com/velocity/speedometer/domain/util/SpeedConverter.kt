package com.velocity.speedometer.domain.util

import com.velocity.speedometer.domain.model.SpeedUnit
import java.util.Locale
import kotlin.math.roundToInt

object SpeedConverter {
    private const val MPS_TO_KMH = 3.6f
    private const val MPS_TO_MPH = 2.2369363f
    private const val MPS_TO_KNOTS = 1.9438445f

    private const val METERS_PER_KM = 1000.0
    private const val METERS_PER_MILE = 1609.344
    private const val METERS_PER_NAUTICAL_MILE = 1852.0
    private const val METERS_TO_FEET = 3.28084

    fun fromMps(speedMps: Float, unit: SpeedUnit): Float {
        if (speedMps <= 0f) return 0f
        return when (unit) {
            SpeedUnit.KMH -> speedMps * MPS_TO_KMH
            SpeedUnit.MPH -> speedMps * MPS_TO_MPH
            SpeedUnit.KNOTS -> speedMps * MPS_TO_KNOTS
        }
    }

    fun toMps(value: Float, unit: SpeedUnit): Float {
        if (value <= 0f) return 0f
        return when (unit) {
            SpeedUnit.KMH -> value / MPS_TO_KMH
            SpeedUnit.MPH -> value / MPS_TO_MPH
            SpeedUnit.KNOTS -> value / MPS_TO_KNOTS
        }
    }

    fun fromMpsRounded(speedMps: Float, unit: SpeedUnit): Int =
        fromMps(speedMps, unit).roundToInt().coerceAtLeast(0)

    fun convertDistance(meters: Double, unit: SpeedUnit): Double {
        if (meters <= 0.0) return 0.0
        return when (unit) {
            SpeedUnit.KMH -> meters / METERS_PER_KM
            SpeedUnit.MPH -> meters / METERS_PER_MILE
            SpeedUnit.KNOTS -> meters / METERS_PER_NAUTICAL_MILE
        }
    }

    fun formatDistance(meters: Double, unit: SpeedUnit): String {
        val safeMeters = meters.coerceAtLeast(0.0)
        return when (unit) {
            SpeedUnit.KMH -> {
                if (safeMeters < 1000.0) {
                    String.format(Locale.US, "%.0f m", safeMeters)
                } else {
                    String.format(Locale.US, "%.2f km", safeMeters / METERS_PER_KM)
                }
            }
            SpeedUnit.MPH -> {
                val miles = safeMeters / METERS_PER_MILE
                String.format(Locale.US, "%.2f mi", miles)
            }
            SpeedUnit.KNOTS -> {
                val nm = safeMeters / METERS_PER_NAUTICAL_MILE
                String.format(Locale.US, "%.2f nm", nm)
            }
        }
    }

    fun formatAltitude(altitudeMeters: Double?, unit: SpeedUnit): String {
        if (altitudeMeters == null) return "--"
        return when (unit) {
            SpeedUnit.MPH -> String.format(Locale.US, "%.0f ft", altitudeMeters * METERS_TO_FEET)
            SpeedUnit.KMH, SpeedUnit.KNOTS -> String.format(Locale.US, "%.0f m", altitudeMeters)
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
