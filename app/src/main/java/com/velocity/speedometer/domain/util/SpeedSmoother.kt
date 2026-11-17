package com.velocity.speedometer.domain.util

import kotlin.math.abs

/**
 * 1D Kalman filter for GPS speed (in m/s) with stationary noise clamping and
 * physically impossible acceleration jump rejection.
 */
class SpeedSmoother(
    private val processNoiseQ: Float = 0.65f,
    private val baseMeasurementNoiseR: Float = 1.6f,
    private val maxAccuracyMeters: Float = 30f,
    private val maxAccelerationMps2: Float = 25f,
    private val stationaryThresholdMps: Float = 0.35f
) {
    private var estimateMps: Float = 0f
    private var errorCovarianceP: Float = 1f
    private var lastValidTimestampMs: Long = -1L
    private var initialized: Boolean = false

    fun update(
        rawSpeedMps: Float,
        accuracyMeters: Float,
        timestampMs: Long
    ): Float? {
        if (accuracyMeters.isNaN() || accuracyMeters <= 0f || accuracyMeters > maxAccuracyMeters) {
            return null
        }
        if (rawSpeedMps.isNaN() || rawSpeedMps < 0f || rawSpeedMps > 200f) {
            return null
        }

        val cleanedSpeed = if (rawSpeedMps < stationaryThresholdMps) 0f else rawSpeedMps

        if (!initialized || lastValidTimestampMs < 0L) {
            estimateMps = cleanedSpeed
            errorCovarianceP = 1f
            lastValidTimestampMs = timestampMs
            initialized = true
            return estimateMps
        }

        val dtSeconds = ((timestampMs - lastValidTimestampMs).coerceAtLeast(50L)) / 1000f
        if (dtSeconds > 0f) {
            val deltaSpeed = abs(cleanedSpeed - estimateMps)
            val impliedAccel = deltaSpeed / dtSeconds
            // Reject physically impossible instantaneous GPS velocity spikes
            if (impliedAccel > maxAccelerationMps2 && deltaSpeed > 8f) {
                return null
            }
        }

        lastValidTimestampMs = timestampMs

        // Prediction step
        val predictedP = errorCovarianceP + processNoiseQ * dtSeconds

        // Scale measurement noise with GPS horizontal accuracy
        val accuracyScale = (accuracyMeters / 10f).coerceIn(0.5f, 4f)
        val measurementR = baseMeasurementNoiseR * accuracyScale

        // Update step
        val kalmanGain = predictedP / (predictedP + measurementR)
        estimateMps += kalmanGain * (cleanedSpeed - estimateMps)
        errorCovarianceP = (1f - kalmanGain) * predictedP

        if (cleanedSpeed == 0f && estimateMps < stationaryThresholdMps) {
            estimateMps = 0f
        }

        return estimateMps.coerceAtLeast(0f)
    }

    fun reset() {
        estimateMps = 0f
        errorCovarianceP = 1f
        lastValidTimestampMs = -1L
        initialized = false
    }
}
