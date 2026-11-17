package com.velocity.speedometer.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.Trip
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val trips: List<Trip> = emptyList(),
    val unit: SpeedUnit = SpeedUnit.KMH
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        tripRepository.observeAllTrips(),
        settingsRepository.settings
    ) { trips, settings ->
        HistoryUiState(trips = trips, unit = settings.unit)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HistoryUiState()
    )

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            tripRepository.deleteTrip(tripId)
        }
    }

    fun deleteAllTrips() {
        viewModelScope.launch {
            tripRepository.deleteAllTrips()
        }
    }
}
