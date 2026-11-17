package com.velocity.speedometer.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.velocity.speedometer.data.location.FusedLocationSource
import com.velocity.speedometer.domain.model.GpsSignalQuality
import com.velocity.speedometer.domain.model.LiveTelemetry
import com.velocity.speedometer.domain.model.LocationSample
import com.velocity.speedometer.domain.model.TrackPoint
import com.velocity.speedometer.domain.model.Trip
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.domain.model.UserSettings
import com.velocity.speedometer.domain.repository.ActiveTripSnapshot
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.domain.repository.TrackingController
import com.velocity.speedometer.domain.repository.TripRepository
import com.velocity.speedometer.domain.util.GeoUtils
import com.velocity.speedometer.domain.util.SpeedSmoother
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationSource: FusedLocationSource,
    private val tripRepository: TripRepository,
    private val settingsRepository: SettingsRepository,
    private val speedAlertManager: SpeedAlertManager
) : TrackingController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val smoother = SpeedSmoother()

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    override val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private var latestSettings: UserSettings = UserSettings()
    private var uiPreviewVisible: Boolean = false
    private var locationJob: Job? = null
    private var durationTickerJob: Job? = null

    private var tripStartTimeMs: Long = 0L
    private var accumulatedDurationMs: Long = 0L
    private var lastResumedAtMs: Long = 0L
    private var lastAcceptedLat: Double? = null
    private var lastAcceptedLng: Double? = null
    private var lastAcceptedPointTimeMs: Long = 0L
    private var lowSpeedSinceMs: Long? = null
    private var lastSnapshotPersistMs: Long = 0L

    init {
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                val intervalChanged = latestSettings.gpsIntervalMs != settings.gpsIntervalMs
                latestSettings = settings
                if (intervalChanged && locationJob?.isActive == true) {
                    restartLocationUpdates()
                }
            }
        }
        scope.launch {
            restoreActiveTripIfNeeded()
        }
    }

    private suspend fun restoreActiveTripIfNeeded() {
        val snapshot = settingsRepository.activeTripSnapshot.first()
        if (snapshot.tripId != null && snapshot.tripState.isActiveOrPaused) {
            tripStartTimeMs = snapshot.startTimeMs
            accumulatedDurationMs = snapshot.accumulatedDurationMs
            lastResumedAtMs = snapshot.lastResumedAtMs
            lastAcceptedLat = snapshot.lastLat
            lastAcceptedLng = snapshot.lastLng
            lastAcceptedPointTimeMs = snapshot.lastPointTimeMs

            val now = System.currentTimeMillis()
            val effectiveDuration = if (snapshot.tripState == TripState.RECORDING) {
                accumulatedDurationMs + (now - lastResumedAtMs).coerceAtLeast(0L)
            } else {
                accumulatedDurationMs
            }
            val avgSpeed = computeAverageSpeed(snapshot.distanceMeters, effectiveDuration)

            _telemetry.update {
                it.copy(
                    tripState = snapshot.tripState,
                    activeTripId = snapshot.tripId,
                    distanceMeters = snapshot.distanceMeters,
                    maxSpeedMps = snapshot.maxSpeedMps,
                    avgSpeedMps = avgSpeed,
                    durationMs = effectiveDuration
                )
            }

            startForegroundService()
            ensureLocationUpdatesRunning()
            if (snapshot.tripState == TripState.RECORDING) {
                startDurationTicker()
            }
        }
    }

    override fun startForegroundPreview() {
        uiPreviewVisible = true
        ensureLocationUpdatesRunning()
    }

    override fun stopForegroundPreview() {
        uiPreviewVisible = false
        if (!_telemetry.value.tripState.isActiveOrPaused) {
            stopLocationUpdates()
        }
    }

    override fun startTrip() {
        if (_telemetry.value.tripState.isActiveOrPaused) return
        scope.launch {
            val now = System.currentTimeMillis()
            val unit = latestSettings.unit
            val newTrip = Trip(
                startTime = now,
                endTime = 0L,
                distanceMeters = 0.0,
                maxSpeedMps = 0f,
                avgSpeedMps = 0f,
                durationMs = 0L,
                unitAtRecording = unit
            )
            val tripId = tripRepository.insertTrip(newTrip)

            tripStartTimeMs = now
            accumulatedDurationMs = 0L
            lastResumedAtMs = now
            lastAcceptedLat = null
            lastAcceptedLng = null
            lastAcceptedPointTimeMs = 0L
            lowSpeedSinceMs = null

            _telemetry.update {
                it.copy(
                    tripState = TripState.RECORDING,
                    activeTripId = tripId,
                    distanceMeters = 0.0,
                    maxSpeedMps = 0f,
                    avgSpeedMps = 0f,
                    durationMs = 0L
                )
            }

            persistSnapshot(force = true)
            startForegroundService()
            ensureLocationUpdatesRunning()
            startDurationTicker()
        }
    }

    override fun pauseTrip() {
        val current = _telemetry.value
        if (current.tripState != TripState.RECORDING) return
        pauseInternal(TripState.PAUSED)
    }

    private fun pauseInternal(targetState: TripState) {
        val now = System.currentTimeMillis()
        accumulatedDurationMs += (now - lastResumedAtMs).coerceAtLeast(0L)
        durationTickerJob?.cancel()
        lowSpeedSinceMs = null

        _telemetry.update {
            val avg = computeAverageSpeed(it.distanceMeters, accumulatedDurationMs)
            it.copy(
                tripState = targetState,
                durationMs = accumulatedDurationMs,
                avgSpeedMps = avg
            )
        }

        scope.launch {
            persistSnapshot(force = true)
        }
    }

    override fun resumeTrip() {
        val current = _telemetry.value
        if (current.tripState != TripState.PAUSED && current.tripState != TripState.AUTO_PAUSED) return
        lastResumedAtMs = System.currentTimeMillis()
        lastAcceptedLat = null
        lastAcceptedLng = null
        lowSpeedSinceMs = null

        _telemetry.update {
            it.copy(tripState = TripState.RECORDING)
        }

        scope.launch {
            persistSnapshot(force = true)
        }
        ensureLocationUpdatesRunning()
        startDurationTicker()
    }

    override fun stopTrip() {
        val current = _telemetry.value
        val tripId = current.activeTripId
        if (!current.tripState.isActiveOrPaused || tripId == null) return

        val now = System.currentTimeMillis()
        val finalDuration = if (current.tripState == TripState.RECORDING) {
            accumulatedDurationMs + (now - lastResumedAtMs).coerceAtLeast(0L)
        } else {
            accumulatedDurationMs
        }
        val finalDistance = current.distanceMeters
        val finalMaxSpeed = current.maxSpeedMps
        val finalAvgSpeed = computeAverageSpeed(finalDistance, finalDuration)

        durationTickerJob?.cancel()
        speedAlertManager.resetCooldown()
        lowSpeedSinceMs = null
        lastAcceptedLat = null
        lastAcceptedLng = null

        _telemetry.update {
            it.copy(
                tripState = TripState.IDLE,
                activeTripId = null,
                distanceMeters = 0.0,
                maxSpeedMps = 0f,
                avgSpeedMps = 0f,
                durationMs = 0L
            )
        }

        scope.launch {
            val existing = tripRepository.getTripById(tripId)
            if (existing != null) {
                tripRepository.updateTrip(
                    existing.copy(
                        endTime = now,
                        distanceMeters = finalDistance,
                        maxSpeedMps = finalMaxSpeed,
                        avgSpeedMps = finalAvgSpeed,
                        durationMs = finalDuration
                    )
                )
            }
            settingsRepository.clearActiveTripSnapshot()
        }

        stopForegroundService()
        if (!uiPreviewVisible) {
            stopLocationUpdates()
        }
    }

    private fun ensureLocationUpdatesRunning() {
        if (locationJob?.isActive == true) return
        restartLocationUpdates()
    }

    private fun restartLocationUpdates() {
        locationJob?.cancel()
        locationJob = scope.launch {
            settingsRepository.settings
                .map { it.gpsIntervalMs }
                .distinctUntilChanged()
                .collectLatest { intervalMs ->
                    locationSource.locationUpdates(intervalMs).collect { sample ->
                        if (sample == null) {
                            _telemetry.update {
                                it.copy(
                                    accuracyMeters = null,
                                    gpsQuality = GpsSignalQuality.POOR_OR_NONE,
                                    rawSpeedMps = 0f,
                                    smoothedSpeedMps = 0f
                                )
                            }
                        } else {
                            handleLocationSample(sample)
                        }
                    }
                }
        }
    }

    private fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
        durationTickerJob?.cancel()
        smoother.reset()
        _telemetry.update {
            it.copy(
                rawSpeedMps = 0f,
                smoothedSpeedMps = 0f
            )
        }
    }

    private fun startDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = scope.launch {
            while (true) {
                delay(500L)
                val current = _telemetry.value
                if (current.tripState != TripState.RECORDING) break
                val now = System.currentTimeMillis()
                val elapsed = accumulatedDurationMs + (now - lastResumedAtMs).coerceAtLeast(0L)
                val avg = computeAverageSpeed(current.distanceMeters, elapsed)
                _telemetry.update {
                    it.copy(
                        durationMs = elapsed,
                        avgSpeedMps = avg
                    )
                }
            }
        }
    }

    private suspend fun handleLocationSample(sample: LocationSample) {
        val quality = GeoUtils.classifyGpsQuality(sample.accuracyMeters)

        // Always update GPS status indicator, even if accuracy is > 30m
        if (sample.accuracyMeters > GeoUtils.MAX_ACCEPTABLE_ACCURACY_METERS) {
            _telemetry.update {
                it.copy(
                    accuracyMeters = sample.accuracyMeters,
                    gpsQuality = quality
                )
            }
            return
        }

        val rawSpeed = deriveRawSpeed(sample)
        val smoothed = smoother.update(
            rawSpeedMps = rawSpeed,
            accuracyMeters = sample.accuracyMeters,
            timestampMs = sample.timestampMs
        ) ?: return

        val settings = latestSettings
        if (settings.speedLimitEnabled && smoothed > settings.speedLimitMps) {
            speedAlertManager.triggerOverLimitAlertIfDue(
                enableVibration = settings.alertVibration,
                enableSound = settings.alertSound,
                nowMs = sample.timestampMs
            )
        } else {
            speedAlertManager.resetCooldown()
        }

        // Auto-pause / auto-resume handling
        val currentState = _telemetry.value.tripState
        if (settings.autoPauseEnabled && currentState == TripState.RECORDING) {
            if (smoothed < AUTO_PAUSE_THRESHOLD_MPS) {
                val since = lowSpeedSinceMs ?: sample.timestampMs.also { lowSpeedSinceMs = it }
                if (sample.timestampMs - since >= AUTO_PAUSE_DURATION_MS) {
                    pauseInternal(TripState.AUTO_PAUSED)
                }
            } else {
                lowSpeedSinceMs = null
            }
        } else if (settings.autoPauseEnabled && currentState == TripState.AUTO_PAUSED) {
            if (smoothed >= AUTO_PAUSE_THRESHOLD_MPS) {
                resumeTrip()
            }
        }

        val updatedState = _telemetry.value.tripState
        val activeTripId = _telemetry.value.activeTripId
        var deltaDistance = 0.0

        if (updatedState == TripState.RECORDING && activeTripId != null) {
            val prevLat = lastAcceptedLat
            val prevLng = lastAcceptedLng
            if (prevLat != null && prevLng != null) {
                val dist = GeoUtils.haversineDistanceMeters(
                    prevLat,
                    prevLng,
                    sample.latitude,
                    sample.longitude
                )
                val dt = (sample.timestampMs - lastAcceptedPointTimeMs).coerceAtLeast(1L)
                if (dist >= MIN_DISTANCE_STEP_METERS &&
                    smoothed > 0f &&
                    GeoUtils.isReasonableMove(dist, dt)
                ) {
                    deltaDistance = dist
                    lastAcceptedLat = sample.latitude
                    lastAcceptedLng = sample.longitude
                    lastAcceptedPointTimeMs = sample.timestampMs
                }
            } else {
                lastAcceptedLat = sample.latitude
                lastAcceptedLng = sample.longitude
                lastAcceptedPointTimeMs = sample.timestampMs
            }

            tripRepository.insertTrackPoint(
                TrackPoint(
                    tripId = activeTripId,
                    timestamp = sample.timestampMs,
                    lat = sample.latitude,
                    lng = sample.longitude,
                    speedMps = smoothed,
                    altitude = if (sample.hasAltitude) sample.altitudeMeters else 0.0,
                    accuracy = sample.accuracyMeters,
                    bearing = if (sample.hasBearing) sample.bearingDegrees else 0f
                )
            )
        }

        if (deltaDistance > 0.0) {
            settingsRepository.addOdometerDistance(deltaDistance)
        }

        val now = System.currentTimeMillis()
        _telemetry.update { prev ->
            val newDistance = if (updatedState == TripState.RECORDING) {
                prev.distanceMeters + deltaDistance
            } else {
                prev.distanceMeters
            }
            val newMax = if (updatedState == TripState.RECORDING) {
                maxOf(prev.maxSpeedMps, smoothed)
            } else {
                prev.maxSpeedMps
            }
            val newDuration = if (updatedState == TripState.RECORDING) {
                accumulatedDurationMs + (now - lastResumedAtMs).coerceAtLeast(0L)
            } else {
                prev.durationMs
            }
            val newAvg = if (updatedState.isActiveOrPaused) {
                computeAverageSpeed(newDistance, newDuration)
            } else {
                0f
            }

            prev.copy(
                rawSpeedMps = rawSpeed,
                smoothedSpeedMps = smoothed,
                maxSpeedMps = newMax,
                avgSpeedMps = newAvg,
                distanceMeters = newDistance,
                durationMs = newDuration,
                altitudeMeters = if (sample.hasAltitude) sample.altitudeMeters else prev.altitudeMeters,
                bearingDegrees = if (sample.hasBearing) sample.bearingDegrees else prev.bearingDegrees,
                accuracyMeters = sample.accuracyMeters,
                gpsQuality = quality
            )
        }

        if (updatedState.isActiveOrPaused) {
            persistSnapshot(force = false)
        }
    }

    private fun deriveRawSpeed(sample: LocationSample): Float {
        if (sample.hasSpeed && sample.speedMps >= 0f) {
            return sample.speedMps
        }
        val prevLat = lastAcceptedLat ?: return 0f
        val prevLng = lastAcceptedLng ?: return 0f
        val dtSeconds = (sample.timestampMs - lastAcceptedPointTimeMs) / 1000f
        if (dtSeconds <= 0.1f) return 0f
        val dist = GeoUtils.haversineDistanceMeters(prevLat, prevLng, sample.latitude, sample.longitude)
        return (dist / dtSeconds).toFloat().coerceAtLeast(0f)
    }

    private suspend fun persistSnapshot(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSnapshotPersistMs < SNAPSHOT_INTERVAL_MS) return
        lastSnapshotPersistMs = now
        val current = _telemetry.value
        settingsRepository.saveActiveTripSnapshot(
            ActiveTripSnapshot(
                tripId = current.activeTripId,
                tripState = current.tripState,
                startTimeMs = tripStartTimeMs,
                accumulatedDurationMs = accumulatedDurationMs,
                lastResumedAtMs = lastResumedAtMs,
                distanceMeters = current.distanceMeters,
                maxSpeedMps = current.maxSpeedMps,
                lastLat = lastAcceptedLat,
                lastLng = lastAcceptedLng,
                lastPointTimeMs = lastAcceptedPointTimeMs
            )
        )
    }

    private fun computeAverageSpeed(distanceMeters: Double, durationMs: Long): Float {
        if (distanceMeters <= 0.0 || durationMs < 1000L) return 0f
        return (distanceMeters / (durationMs / 1000.0)).toFloat().coerceAtLeast(0f)
    }

    private fun startForegroundService() {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_START_FOREGROUND
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopForegroundService() {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }

    private companion object {
        const val AUTO_PAUSE_THRESHOLD_MPS = 2f / 3.6f // 2 km/h
        const val AUTO_PAUSE_DURATION_MS = 60_000L
        const val MIN_DISTANCE_STEP_METERS = 1.5
        const val SNAPSHOT_INTERVAL_MS = 2_000L
    }
}
