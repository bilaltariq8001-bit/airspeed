package com.velocity.speedometer.domain.repository

import com.velocity.speedometer.domain.model.GaugeViewMode
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.ThemeMode
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

data class ActiveTripSnapshot(
    val tripId: Long?,
    val tripState: TripState,
    val startTimeMs: Long,
    val accumulatedDurationMs: Long,
    val lastResumedAtMs: Long,
    val distanceMeters: Double,
    val maxSpeedMps: Float,
    val lastLat: Double?,
    val lastLng: Double?,
    val lastPointTimeMs: Long
)

interface SettingsRepository {
    val settings: Flow<UserSettings>
    val activeTripSnapshot: Flow<ActiveTripSnapshot>

    suspend fun setUnit(unit: SpeedUnit)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDefaultView(viewMode: GaugeViewMode)
    suspend fun setSpeedLimitEnabled(enabled: Boolean)
    suspend fun setSpeedLimitMps(limitMps: Float)
    suspend fun setAlertVibration(enabled: Boolean)
    suspend fun setAlertSound(enabled: Boolean)
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun setAutoPauseEnabled(enabled: Boolean)
    suspend fun setGpsIntervalMs(intervalMs: Long)
    suspend fun addOdometerDistance(deltaMeters: Double)
    suspend fun resetOdometer()
    suspend fun saveActiveTripSnapshot(snapshot: ActiveTripSnapshot)
    suspend fun clearActiveTripSnapshot()
}
