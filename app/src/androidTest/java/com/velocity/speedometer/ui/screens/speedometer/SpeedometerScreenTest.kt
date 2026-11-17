package com.velocity.speedometer.ui.screens.speedometer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.GaugeViewMode
import com.velocity.speedometer.domain.model.GpsSignalQuality
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.ui.theme.VelocityTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SpeedometerScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun speedometerDisplaysSpeedStatsAndMorphedControls() {
        var startClicked = false
        var unitTapped = false

        val sampleState = SpeedometerUiState(
            currentSpeedConverted = 72f,
            maxSpeedConverted = 95f,
            avgSpeedConverted = 60f,
            distanceMeters = 12_400.0,
            durationMs = 740_000L,
            altitudeMeters = 125.0,
            bearingDegrees = 45f,
            accuracyMeters = 6f,
            gpsQuality = GpsSignalQuality.GOOD,
            tripState = TripState.IDLE,
            unit = SpeedUnit.KMH,
            defaultView = GaugeViewMode.DIGITAL
        )

        composeRule.setContent {
            VelocityTheme {
                SpeedometerContent(
                    uiState = sampleState,
                    onCycleUnit = { unitTapped = true },
                    onStartTrip = { startClicked = true },
                    onPauseTrip = {},
                    onResumeTrip = {},
                    onStopTrip = {},
                    onOpenHud = {},
                    onOpenHistory = {},
                    onOpenSettings = {}
                )
            }
        }

        val startLabel = composeRule.activity.getString(R.string.action_start)
        val unitLabel = composeRule.activity.getString(R.string.unit_kmh).uppercase()
        val maxStatLabel = composeRule.activity.getString(R.string.stat_max)

        composeRule.onNodeWithText("72").assertIsDisplayed()
        composeRule.onNodeWithText(maxStatLabel).assertIsDisplayed()
        composeRule.onNodeWithText(unitLabel).performClick()
        assertTrue(unitTapped)

        composeRule.onNodeWithText(startLabel).assertIsDisplayed().performClick()
        assertTrue(startClicked)
    }

    @Test
    fun recordingStateShowsPauseAndStopControls() {
        val recordingState = SpeedometerUiState(
            currentSpeedConverted = 88f,
            tripState = TripState.RECORDING,
            unit = SpeedUnit.MPH,
            defaultView = GaugeViewMode.ANALOG
        )

        composeRule.setContent {
            VelocityTheme {
                SpeedometerContent(
                    uiState = recordingState,
                    onCycleUnit = {},
                    onStartTrip = {},
                    onPauseTrip = {},
                    onResumeTrip = {},
                    onStopTrip = {},
                    onOpenHud = {},
                    onOpenHistory = {},
                    onOpenSettings = {}
                )
            }
        }

        val pauseCd = composeRule.activity.getString(R.string.cd_pause_trip)
        val stopCd = composeRule.activity.getString(R.string.cd_stop_trip)

        composeRule.onNodeWithContentDescription(pauseCd).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(stopCd).assertIsDisplayed()
    }
}
