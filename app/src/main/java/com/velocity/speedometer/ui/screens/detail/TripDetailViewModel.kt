package com.velocity.speedometer.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.TrackPoint
import com.velocity.speedometer.domain.model.Trip
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.domain.repository.TripRepository
import com.velocity.speedometer.domain.util.TripExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TripDetailUiState(
    val trip: Trip? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val unit: SpeedUnit = SpeedUnit.KMH
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tripRepository: TripRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>("tripId") ?: -1L

    val uiState: StateFlow<TripDetailUiState> = combine(
        tripRepository.observeTripById(tripId),
        tripRepository.observeTrackPoints(tripId),
        settingsRepository.settings
    ) { trip, points, settings ->
        TripDetailUiState(
            trip = trip,
            trackPoints = points,
            unit = settings.unit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TripDetailUiState()
    )

    fun buildGpxPayload(): Pair<String, String>? {
        val state = uiState.value
        val trip = state.trip ?: return null
        return "velocity_trip_${trip.id}.gpx" to TripExporter.toGpx(trip, state.trackPoints)
    }

    fun buildCsvPayload(): Pair<String, String>? {
        val state = uiState.value
        val trip = state.trip ?: return null
        return "velocity_trip_${trip.id}.csv" to TripExporter.toCsv(trip, state.trackPoints)
    }
}
