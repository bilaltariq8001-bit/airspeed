package com.velocity.speedometer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.velocity.speedometer.domain.model.GaugeViewMode
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.ThemeMode
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.domain.model.UserSettings
import com.velocity.speedometer.domain.repository.ActiveTripSnapshot
import com.velocity.speedometer.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.velocityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "velocity_preferences"
)

@Singleton
class SettingsDataStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val UNIT = stringPreferencesKey("speed_unit")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_VIEW = stringPreferencesKey("default_view")
        val SPEED_LIMIT_ENABLED = booleanPreferencesKey("speed_limit_enabled")
        val SPEED_LIMIT_MPS = floatPreferencesKey("speed_limit_mps")
        val ALERT_VIBRATION = booleanPreferencesKey("alert_vibration")
        val ALERT_SOUND = booleanPreferencesKey("alert_sound")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val AUTO_PAUSE = booleanPreferencesKey("auto_pause")
        val GPS_INTERVAL_MS = longPreferencesKey("gps_interval_ms")
        val ODOMETER_METERS = doublePreferencesKey("odometer_meters")

        // Process-death recovery keys for active trip
        val ACTIVE_TRIP_ID = longPreferencesKey("active_trip_id")
        val ACTIVE_TRIP_STATE = stringPreferencesKey("active_trip_state")
        val ACTIVE_TRIP_START_MS = longPreferencesKey("active_trip_start_ms")
        val ACTIVE_TRIP_ACCUM_DURATION_MS = longPreferencesKey("active_trip_accum_duration_ms")
        val ACTIVE_TRIP_LAST_RESUMED_MS = longPreferencesKey("active_trip_last_resumed_ms")
        val ACTIVE_TRIP_DISTANCE_M = doublePreferencesKey("active_trip_distance_m")
        val ACTIVE_TRIP_MAX_SPEED_MPS = floatPreferencesKey("active_trip_max_speed_mps")
        val ACTIVE_TRIP_LAST_LAT = doublePreferencesKey("active_trip_last_lat")
        val ACTIVE_TRIP_LAST_LNG = doublePreferencesKey("active_trip_last_lng")
        val ACTIVE_TRIP_LAST_PT_MS = longPreferencesKey("active_trip_last_pt_ms")
    }

    private val safeData: Flow<Preferences> = context.velocityDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val settings: Flow<UserSettings> = safeData.map { prefs ->
        UserSettings(
            unit = prefs[Keys.UNIT]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() }
                ?: SpeedUnit.KMH,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            defaultView = prefs[Keys.DEFAULT_VIEW]?.let { runCatching { GaugeViewMode.valueOf(it) }.getOrNull() }
                ?: GaugeViewMode.ANALOG,
            speedLimitEnabled = prefs[Keys.SPEED_LIMIT_ENABLED] ?: false,
            speedLimitMps = prefs[Keys.SPEED_LIMIT_MPS] ?: (100f / 3.6f),
            alertVibration = prefs[Keys.ALERT_VIBRATION] ?: true,
            alertSound = prefs[Keys.ALERT_SOUND] ?: false,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            autoPauseEnabled = prefs[Keys.AUTO_PAUSE] ?: false,
            gpsIntervalMs = prefs[Keys.GPS_INTERVAL_MS] ?: 500L,
            odometerMeters = prefs[Keys.ODOMETER_METERS] ?: 0.0
        )
    }

    override val activeTripSnapshot: Flow<ActiveTripSnapshot> = safeData.map { prefs ->
        val state = prefs[Keys.ACTIVE_TRIP_STATE]
            ?.let { runCatching { TripState.valueOf(it) }.getOrNull() }
            ?: TripState.IDLE
        ActiveTripSnapshot(
            tripId = prefs[Keys.ACTIVE_TRIP_ID],
            tripState = state,
            startTimeMs = prefs[Keys.ACTIVE_TRIP_START_MS] ?: 0L,
            accumulatedDurationMs = prefs[Keys.ACTIVE_TRIP_ACCUM_DURATION_MS] ?: 0L,
            lastResumedAtMs = prefs[Keys.ACTIVE_TRIP_LAST_RESUMED_MS] ?: 0L,
            distanceMeters = prefs[Keys.ACTIVE_TRIP_DISTANCE_M] ?: 0.0,
            maxSpeedMps = prefs[Keys.ACTIVE_TRIP_MAX_SPEED_MPS] ?: 0f,
            lastLat = prefs[Keys.ACTIVE_TRIP_LAST_LAT],
            lastLng = prefs[Keys.ACTIVE_TRIP_LAST_LNG],
            lastPointTimeMs = prefs[Keys.ACTIVE_TRIP_LAST_PT_MS] ?: 0L
        )
    }

    override suspend fun setUnit(unit: SpeedUnit) {
        context.velocityDataStore.edit { it[Keys.UNIT] = unit.name }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        context.velocityDataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setDefaultView(viewMode: GaugeViewMode) {
        context.velocityDataStore.edit { it[Keys.DEFAULT_VIEW] = viewMode.name }
    }

    override suspend fun setSpeedLimitEnabled(enabled: Boolean) {
        context.velocityDataStore.edit { it[Keys.SPEED_LIMIT_ENABLED] = enabled }
    }

    override suspend fun setSpeedLimitMps(limitMps: Float) {
        context.velocityDataStore.edit { it[Keys.SPEED_LIMIT_MPS] = limitMps.coerceAtLeast(1f) }
    }

    override suspend fun setAlertVibration(enabled: Boolean) {
        context.velocityDataStore.edit { it[Keys.ALERT_VIBRATION] = enabled }
    }

    override suspend fun setAlertSound(enabled: Boolean) {
        context.velocityDataStore.edit { it[Keys.ALERT_SOUND] = enabled }
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        context.velocityDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    override suspend fun setAutoPauseEnabled(enabled: Boolean) {
        context.velocityDataStore.edit { it[Keys.AUTO_PAUSE] = enabled }
    }

    override suspend fun setGpsIntervalMs(intervalMs: Long) {
        context.velocityDataStore.edit { it[Keys.GPS_INTERVAL_MS] = intervalMs }
    }

    override suspend fun addOdometerDistance(deltaMeters: Double) {
        if (deltaMeters <= 0.0) return
        context.velocityDataStore.edit { prefs ->
            val current = prefs[Keys.ODOMETER_METERS] ?: 0.0
            prefs[Keys.ODOMETER_METERS] = current + deltaMeters
        }
    }

    override suspend fun resetOdometer() {
        context.velocityDataStore.edit { it[Keys.ODOMETER_METERS] = 0.0 }
    }

    override suspend fun saveActiveTripSnapshot(snapshot: ActiveTripSnapshot) {
        context.velocityDataStore.edit { prefs ->
            if (snapshot.tripId != null) {
                prefs[Keys.ACTIVE_TRIP_ID] = snapshot.tripId
            } else {
                prefs.remove(Keys.ACTIVE_TRIP_ID)
            }
            prefs[Keys.ACTIVE_TRIP_STATE] = snapshot.tripState.name
            prefs[Keys.ACTIVE_TRIP_START_MS] = snapshot.startTimeMs
            prefs[Keys.ACTIVE_TRIP_ACCUM_DURATION_MS] = snapshot.accumulatedDurationMs
            prefs[Keys.ACTIVE_TRIP_LAST_RESUMED_MS] = snapshot.lastResumedAtMs
            prefs[Keys.ACTIVE_TRIP_DISTANCE_M] = snapshot.distanceMeters
            prefs[Keys.ACTIVE_TRIP_MAX_SPEED_MPS] = snapshot.maxSpeedMps
            if (snapshot.lastLat != null && snapshot.lastLng != null) {
                prefs[Keys.ACTIVE_TRIP_LAST_LAT] = snapshot.lastLat
                prefs[Keys.ACTIVE_TRIP_LAST_LNG] = snapshot.lastLng
            } else {
                prefs.remove(Keys.ACTIVE_TRIP_LAST_LAT)
                prefs.remove(Keys.ACTIVE_TRIP_LAST_LNG)
            }
            prefs[Keys.ACTIVE_TRIP_LAST_PT_MS] = snapshot.lastPointTimeMs
        }
    }

    override suspend fun clearActiveTripSnapshot() {
        context.velocityDataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVE_TRIP_ID)
            prefs.remove(Keys.ACTIVE_TRIP_STATE)
            prefs.remove(Keys.ACTIVE_TRIP_START_MS)
            prefs.remove(Keys.ACTIVE_TRIP_ACCUM_DURATION_MS)
            prefs.remove(Keys.ACTIVE_TRIP_LAST_RESUMED_MS)
            prefs.remove(Keys.ACTIVE_TRIP_DISTANCE_M)
            prefs.remove(Keys.ACTIVE_TRIP_MAX_SPEED_MPS)
            prefs.remove(Keys.ACTIVE_TRIP_LAST_LAT)
            prefs.remove(Keys.ACTIVE_TRIP_LAST_LNG)
            prefs.remove(Keys.ACTIVE_TRIP_LAST_PT_MS)
        }
    }
}
