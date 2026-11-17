package com.velocity.speedometer.ui.screens.speedometer

import com.velocity.speedometer.MainDispatcherRule
import com.velocity.speedometer.domain.model.GaugeViewMode
import com.velocity.speedometer.domain.model.GpsSignalQuality
import com.velocity.speedometer.domain.model.LiveTelemetry
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.ThemeMode
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.domain.model.UserSettings
import com.velocity.speedometer.domain.repository.ActiveTripSnapshot
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.domain.repository.TrackingController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedometerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `unit cycling updates SpeedUnit from KMH to MPH to KNOTS`() = runTest {
        val fakeSettings = FakeSettingsRepository()
        val fakeTracking = FakeTrackingController()
        val viewModel = SpeedometerViewModel(fakeTracking, fakeSettings)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertEquals(SpeedUnit.KMH, viewModel.uiState.value.unit)
        viewModel.onCycleUnit()
        assertEquals(SpeedUnit.MPH, viewModel.uiState.value.unit)
        viewModel.onCycleUnit()
        assertEquals(SpeedUnit.KNOTS, viewModel.uiState.value.unit)
        viewModel.onCycleUnit()
        assertEquals(SpeedUnit.KMH, viewModel.uiState.value.unit)

        collectJob.cancel()
    }

    @Test
    fun `speed limit warning and over-limit states evaluate accurately`() = runTest {
        val fakeSettings = FakeSettingsRepository().apply {
            setSpeedLimitEnabled(true)
            setSpeedLimitMps(100f / 3.6f) // 100 km/h
        }
        val fakeTracking = FakeTrackingController()
        val viewModel = SpeedometerViewModel(fakeTracking, fakeSettings)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // 80 km/h -> safe
        fakeTracking.emitSpeed(80f / 3.6f)
        assertFalse(viewModel.uiState.value.isWarningZone)
        assertFalse(viewModel.uiState.value.isOverLimit)

        // 92 km/h -> within 10% warning zone
        fakeTracking.emitSpeed(92f / 3.6f)
        assertTrue(viewModel.uiState.value.isWarningZone)
        assertFalse(viewModel.uiState.value.isOverLimit)

        // 105 km/h -> over limit
        fakeTracking.emitSpeed(105f / 3.6f)
        assertFalse(viewModel.uiState.value.isWarningZone)
        assertTrue(viewModel.uiState.value.isOverLimit)

        collectJob.cancel()
    }

    @Test
    fun `trip actions delegate to TrackingController`() = runTest {
        val fakeSettings = FakeSettingsRepository()
        val fakeTracking = FakeTrackingController()
        val viewModel = SpeedometerViewModel(fakeTracking, fakeSettings)

        viewModel.onStartTrip()
        assertEquals(TripState.RECORDING, fakeTracking.telemetry.value.tripState)

        viewModel.onPauseTrip()
        assertEquals(TripState.PAUSED, fakeTracking.telemetry.value.tripState)

        viewModel.onResumeTrip()
        assertEquals(TripState.RECORDING, fakeTracking.telemetry.value.tripState)

        viewModel.onStopTrip()
        assertEquals(TripState.IDLE, fakeTracking.telemetry.value.tripState)
    }

    private class FakeTrackingController : TrackingController {
        private val _telemetry = MutableStateFlow(
            LiveTelemetry(
                gpsQuality = GpsSignalQuality.GOOD,
                accuracyMeters = 4f
            )
        )
        override val telemetry: StateFlow<LiveTelemetry> = _telemetry

        fun emitSpeed(mps: Float) {
            _telemetry.value = _telemetry.value.copy(smoothedSpeedMps = mps)
        }

        override fun startForegroundPreview() = Unit
        override fun stopForegroundPreview() = Unit
        override fun startTrip() {
            _telemetry.value = _telemetry.value.copy(tripState = TripState.RECORDING, activeTripId = 1L)
        }

        override fun pauseTrip() {
            _telemetry.value = _telemetry.value.copy(tripState = TripState.PAUSED)
        }

        override fun resumeTrip() {
            _telemetry.value = _telemetry.value.copy(tripState = TripState.RECORDING)
        }

        override fun stopTrip() {
            _telemetry.value = _telemetry.value.copy(tripState = TripState.IDLE, activeTripId = null)
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(UserSettings())
        override val settings: Flow<UserSettings> = _settings

        private val _snapshot = MutableStateFlow(
            ActiveTripSnapshot(
                tripId = null,
                tripState = TripState.IDLE,
                startTimeMs = 0L,
                accumulatedDurationMs = 0L,
                lastResumedAtMs = 0L,
                distanceMeters = 0.0,
                maxSpeedMps = 0f,
                lastLat = null,
                lastLng = null,
                lastPointTimeMs = 0L
            )
        )
        override val activeTripSnapshot: Flow<ActiveTripSnapshot> = _snapshot

        override suspend fun setUnit(unit: SpeedUnit) {
            _settings.value = _settings.value.copy(unit = unit)
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            _settings.value = _settings.value.copy(themeMode = themeMode)
        }

        override suspend fun setDefaultView(viewMode: GaugeViewMode) {
            _settings.value = _settings.value.copy(defaultView = viewMode)
        }

        override suspend fun setSpeedLimitEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(speedLimitEnabled = enabled)
        }

        override suspend fun setSpeedLimitMps(limitMps: Float) {
            _settings.value = _settings.value.copy(speedLimitMps = limitMps)
        }

        override suspend fun setAlertVibration(enabled: Boolean) {
            _settings.value = _settings.value.copy(alertVibration = enabled)
        }

        override suspend fun setAlertSound(enabled: Boolean) {
            _settings.value = _settings.value.copy(alertSound = enabled)
        }

        override suspend fun setKeepScreenOn(enabled: Boolean) {
            _settings.value = _settings.value.copy(keepScreenOn = enabled)
        }

        override suspend fun setAutoPauseEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(autoPauseEnabled = enabled)
        }

        override suspend fun setGpsIntervalMs(intervalMs: Long) {
            _settings.value = _settings.value.copy(gpsIntervalMs = intervalMs)
        }

        override suspend fun addOdometerDistance(deltaMeters: Double) {
            _settings.value = _settings.value.copy(
                odometerMeters = _settings.value.odometerMeters + deltaMeters
            )
        }

        override suspend fun resetOdometer() {
            _settings.value = _settings.value.copy(odometerMeters = 0.0)
        }

        override suspend fun saveActiveTripSnapshot(snapshot: ActiveTripSnapshot) {
            _snapshot.value = snapshot
        }

        override suspend fun clearActiveTripSnapshot() {
            _snapshot.value = _snapshot.value.copy(tripId = null, tripState = TripState.IDLE)
        }
    }
}
