package com.velocity.speedometer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.speedometer.domain.model.GaugeViewMode
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.ThemeMode
import com.velocity.speedometer.domain.model.UserSettings
import com.velocity.speedometer.domain.repository.SettingsRepository
import com.velocity.speedometer.domain.util.SpeedConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = UserSettings()
    )

    fun setUnit(unit: SpeedUnit) {
        viewModelScope.launch { settingsRepository.setUnit(unit) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }

    fun setDefaultView(viewMode: GaugeViewMode) {
        viewModelScope.launch { settingsRepository.setDefaultView(viewMode) }
    }

    fun setSpeedLimitEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSpeedLimitEnabled(enabled) }
    }

    fun setSpeedLimitInCurrentUnit(value: Float, unit: SpeedUnit) {
        val mps = SpeedConverter.toMps(value, unit)
        viewModelScope.launch { settingsRepository.setSpeedLimitMps(mps) }
    }

    fun setAlertVibration(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAlertVibration(enabled) }
    }

    fun setAlertSound(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAlertSound(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun setAutoPauseEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPauseEnabled(enabled) }
    }

    fun setGpsIntervalMs(intervalMs: Long) {
        viewModelScope.launch { settingsRepository.setGpsIntervalMs(intervalMs) }
    }

    fun resetOdometer() {
        viewModelScope.launch { settingsRepository.resetOdometer() }
    }
}
