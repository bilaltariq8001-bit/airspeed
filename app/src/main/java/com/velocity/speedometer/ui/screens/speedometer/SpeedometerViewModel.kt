package com.velocity.speedometer.ui.screens.speedometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.speedometer.domain.model.GaugeViewMode
import com.velocity.speedometer.domain.model.GpsSignalQuality
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.domain.model.UserSettings
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.domain.repository.TrackingController
import com.velocity.speedometer.domain.util.GeoUtils
import com.velocity.speedometer.domain.util.SpeedConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class SpeedometerUiState(
    val currentSpeedConverted: Float = 0f,
    val maxSpeedConverted: Float = 0f,
    val avgSpeedConverted: Float = 0f,
    val speedLimitConverted: Float? = null,
    val isOverLimit: Boolean = false,
    val isWarningZone: Boolean = false,
    val distanceMeters: Double = 0.0,
    val durationMs: Long = 0L,
    val altitudeMeters: Double? = null,
    val bearingDegrees: Float? = null,
    val cardinalDirection: GeoUtils.CardinalDirection? = null,
    val accuracyMeters: Float? = null,
    val gpsQuality: GpsSignalQuality = GpsSignalQuality.POOR_OR_NONE,
    val tripState: TripState = TripState.IDLE,
    val unit: SpeedUnit = SpeedUnit.KMH,
    val defaultView: GaugeViewMode = GaugeViewMode.ANALOG,
    val keepScreenOn: Boolean = true,
    val odometerMeters: Double = 0.0
)

@HiltViewModel
class SpeedometerViewModel @Inject constructor(
    private val trackingController: TrackingController,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SpeedometerUiState> = combine(
        trackingController.telemetry,
        settingsRepository.settings
    ) { telemetry, settings ->
        buildUiState(telemetry, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SpeedometerUiState()
    )

    private fun buildUiState(
        telemetry: com.velocity.speedometer.domain.model.LiveTelemetry,
        settings: UserSettings
    ): SpeedometerUiState {
        val unit = settings.unit
        val currentSpeed = SpeedConverter.fromMps(telemetry.smoothedSpeedMps, unit)
        val maxSpeed = SpeedConverter.fromMps(telemetry.maxSpeedMps, unit)
        val avgSpeed = SpeedConverter.fromMps(telemetry.avgSpeedMps, unit)

        val limitConverted = if (settings.speedLimitEnabled) {
            SpeedConverter.fromMps(settings.speedLimitMps, unit)
        } else {
            null
        }
        val overLimit = settings.speedLimitEnabled &&
            telemetry.smoothedSpeedMps > settings.speedLimitMps
        val warningZone = settings.speedLimitEnabled &&
            !overLimit &&
            telemetry.smoothedSpeedMps >= settings.speedLimitMps * 0.90f

        return SpeedometerUiState(
            currentSpeedConverted = currentSpeed,
            maxSpeedConverted = maxSpeed,
            avgSpeedConverted = avgSpeed,
            speedLimitConverted = limitConverted,
            isOverLimit = overLimit,
            isWarningZone = warningZone,
            distanceMeters = telemetry.distanceMeters,
            durationMs = telemetry.durationMs,
            altitudeMeters = telemetry.altitudeMeters,
            bearingDegrees = telemetry.bearingDegrees,
            cardinalDirection = GeoUtils.bearingToCardinal(telemetry.bearingDegrees),
            accuracyMeters = telemetry.accuracyMeters,
            gpsQuality = telemetry.gpsQuality,
            tripState = telemetry.tripState,
            unit = unit,
            defaultView = settings.defaultView,
            keepScreenOn = settings.keepScreenOn,
            odometerMeters = settings.odometerMeters
        )
    }

    fun onScreenVisible() {
        trackingController.startForegroundPreview()
    }

    fun onScreenHidden() {
        trackingController.stopForegroundPreview()
    }

    fun onCycleUnit() {
        val next = uiState.value.unit.next()
        viewModelScope.launch {
            settingsRepository.setUnit(next)
        }
    }

    fun onStartTrip() = trackingController.startTrip()

    fun onPauseTrip() = trackingController.pauseTrip()

    fun onResumeTrip() = trackingController.resumeTrip()

    fun onStopTrip() = trackingController.stopTrip()

    companion object {
        fun formatSpeedValue(value: Float): String = value.roundToInt().coerceAtLeast(0).toString()
    }
}
