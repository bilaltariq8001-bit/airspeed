package com.velocity.speedometer.domain.model

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float,
    val hasSpeed: Boolean,
    val altitudeMeters: Double,
    val hasAltitude: Boolean,
    val accuracyMeters: Float,
    val bearingDegrees: Float,
    val hasBearing: Boolean,
    val timestampMs: Long
)

data class Trip(
    val id: Long = 0L,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val maxSpeedMps: Float,
    val avgSpeedMps: Float,
    val durationMs: Long,
    val unitAtRecording: SpeedUnit
)

data class TrackPoint(
    val id: Long = 0L,
    val tripId: Long,
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val speedMps: Float,
    val altitude: Double,
    val accuracy: Float,
    val bearing: Float
)

data class UserSettings(
    val unit: SpeedUnit = SpeedUnit.KMH,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultView: GaugeViewMode = GaugeViewMode.ANALOG,
    val speedLimitEnabled: Boolean = false,
    val speedLimitMps: Float = 100f / 3.6f,
    val alertVibration: Boolean = true,
    val alertSound: Boolean = false,
    val keepScreenOn: Boolean = true,
    val autoPauseEnabled: Boolean = false,
    val gpsIntervalMs: Long = 500L,
    val odometerMeters: Double = 0.0
)

data class LiveTelemetry(
    val rawSpeedMps: Float = 0f,
    val smoothedSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f,
    val avgSpeedMps: Float = 0f,
    val distanceMeters: Double = 0.0,
    val durationMs: Long = 0L,
    val altitudeMeters: Double? = null,
    val bearingDegrees: Float? = null,
    val accuracyMeters: Float? = null,
    val gpsQuality: GpsSignalQuality = GpsSignalQuality.POOR_OR_NONE,
    val tripState: TripState = TripState.IDLE,
    val activeTripId: Long? = null
)
