package com.velocity.speedometer.domain.model

enum class SpeedUnit(
    val scaleMax: Int,
    val majorTickStep: Int,
    val minorTickStep: Int
) {
    KMH(scaleMax = 240, majorTickStep = 20, minorTickStep = 10),
    MPH(scaleMax = 160, majorTickStep = 20, minorTickStep = 10),
    KNOTS(scaleMax = 130, majorTickStep = 20, minorTickStep = 10);

    fun next(): SpeedUnit = when (this) {
        KMH -> MPH
        MPH -> KNOTS
        KNOTS -> KMH
    }
}

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

enum class GaugeViewMode {
    ANALOG,
    DIGITAL
}

enum class TripState {
    IDLE,
    RECORDING,
    PAUSED,
    AUTO_PAUSED;

    val isActiveOrPaused: Boolean
        get() = this != IDLE
}

enum class GpsSignalQuality {
    GOOD,
    MODERATE,
    POOR_OR_NONE
}
