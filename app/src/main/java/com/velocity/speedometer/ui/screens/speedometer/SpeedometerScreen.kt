package com.velocity.speedometer.ui.screens.speedometer

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.GaugeViewMode
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.TripState
import com.velocity.speedometer.domain.util.GeoUtils
import com.velocity.speedometer.domain.util.SpeedConverter
import com.velocity.speedometer.ui.components.AnalogGauge
import com.velocity.speedometer.ui.components.DigitalSpeedDisplay
import com.velocity.speedometer.ui.components.GpsStatusPill
import com.velocity.speedometer.ui.components.StatCard
import com.velocity.speedometer.ui.components.TripControlsBar
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import com.velocity.speedometer.ui.theme.VelocityDanger
import com.velocity.speedometer.ui.theme.VelocityWarning
import kotlin.math.roundToInt

@Composable
fun SpeedometerScreen(
    onOpenHud: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: SpeedometerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onScreenVisible()
                Lifecycle.Event.ON_STOP -> viewModel.onScreenHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onScreenVisible()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenHidden()
        }
    }

    DisposableEffect(uiState.keepScreenOn) {
        val activity = context as? Activity
        val window = activity?.window
        if (uiState.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    SpeedometerContent(
        uiState = uiState,
        onCycleUnit = viewModel::onCycleUnit,
        onStartTrip = viewModel::onStartTrip,
        onPauseTrip = viewModel::onPauseTrip,
        onResumeTrip = viewModel::onResumeTrip,
        onStopTrip = viewModel::onStopTrip,
        onOpenHud = onOpenHud,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings
    )
}

@Composable
fun SpeedometerContent(
    uiState: SpeedometerUiState,
    onCycleUnit: () -> Unit,
    onStartTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onOpenHud: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val unitLabel = unitDisplayLabel(uiState.unit)
    val compassLabel = formatCompassLabel(uiState.cardinalDirection, uiState.bearingDegrees)

    val initialPage = if (uiState.defaultView == GaugeViewMode.ANALOG) 0 else 1
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })

    LaunchedEffect(uiState.defaultView) {
        val target = if (uiState.defaultView == GaugeViewMode.ANALOG) 0 else 1
        if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(target)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SpeedometerTopBar(
            uiState = uiState,
            unitLabel = unitLabel,
            onOpenHud = onOpenHud
        )

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpeedDisplayPager(
                    pagerState = pagerState,
                    uiState = uiState,
                    unitLabel = unitLabel,
                    compassLabel = compassLabel,
                    onCycleUnit = onCycleUnit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    StatsGrid(
                        uiState = uiState,
                        unitLabel = unitLabel,
                        compassLabel = compassLabel
                    )
                    TripControlsBar(
                        tripState = uiState.tripState,
                        onStart = onStartTrip,
                        onPause = onPauseTrip,
                        onResume = onResumeTrip,
                        onStop = onStopTrip,
                        onHistoryClick = onOpenHistory,
                        onSettingsClick = onOpenSettings
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SpeedDisplayPager(
                    pagerState = pagerState,
                    uiState = uiState,
                    unitLabel = unitLabel,
                    compassLabel = compassLabel,
                    onCycleUnit = onCycleUnit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatsGrid(
                    uiState = uiState,
                    unitLabel = unitLabel,
                    compassLabel = compassLabel
                )
            }

            TripControlsBar(
                tripState = uiState.tripState,
                onStart = onStartTrip,
                onPause = onPauseTrip,
                onResume = onResumeTrip,
                onStop = onStopTrip,
                onHistoryClick = onOpenHistory,
                onSettingsClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun SpeedometerTopBar(
    uiState: SpeedometerUiState,
    unitLabel: String,
    onOpenHud: () -> Unit
) {
    val velocityColors = LocalVelocityColors.current
    val odometerText = SpeedConverter.formatDistance(uiState.odometerMeters, uiState.unit)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GpsStatusPill(
            accuracyMeters = uiState.accuracyMeters,
            gpsQuality = uiState.gpsQuality
        )

        // Status badge: Over limit, Auto-Paused, or Odometer
        when {
            uiState.isOverLimit -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VelocityDanger.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, VelocityDanger)
                ) {
                    Text(
                        text = stringResource(R.string.over_limit_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = VelocityDanger,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            uiState.tripState == TripState.AUTO_PAUSED -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VelocityWarning.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, VelocityWarning)
                ) {
                    Text(
                        text = stringResource(R.string.trip_auto_paused),
                        style = MaterialTheme.typography.labelSmall,
                        color = VelocityWarning,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            uiState.speedLimitConverted != null -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, velocityColors.divider)
                ) {
                    Text(
                        text = stringResource(
                            R.string.speed_limit_badge,
                            uiState.speedLimitConverted.roundToInt(),
                            unitLabel.uppercase()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = stringResource(R.string.stat_odometer, odometerText),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val hudCd = stringResource(R.string.cd_hud_button)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, velocityColors.divider)
        ) {
            IconButton(
                onClick = onOpenHud,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = hudCd }
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsCar,
                    contentDescription = hudCd,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SpeedDisplayPager(
    pagerState: androidx.compose.foundation.pager.PagerState,
    uiState: SpeedometerUiState,
    unitLabel: String,
    compassLabel: String,
    onCycleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (page == 0) {
                    AnalogGauge(
                        speedValue = uiState.currentSpeedConverted,
                        maxTripSpeedValue = uiState.maxSpeedConverted,
                        speedLimitValue = uiState.speedLimitConverted,
                        unit = uiState.unit,
                        unitLabel = unitLabel,
                        compassLabel = compassLabel,
                        isOverLimit = uiState.isOverLimit,
                        isWarningZone = uiState.isWarningZone,
                        onUnitClick = onCycleUnit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 340.dp)
                    )
                } else {
                    DigitalSpeedDisplay(
                        speedValue = uiState.currentSpeedConverted,
                        unit = uiState.unit,
                        unitLabel = unitLabel,
                        compassLabel = compassLabel,
                        isOverLimit = uiState.isOverLimit,
                        isWarningZone = uiState.isWarningZone,
                        onUnitClick = onCycleUnit,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Subtle swipe indicator dots + mode label
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (active) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
            Text(
                text = stringResource(
                    if (pagerState.currentPage == 0) R.string.mode_analog else R.string.mode_digital
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsGrid(
    uiState: SpeedometerUiState,
    unitLabel: String,
    compassLabel: String
) {
    val maxText = "${uiState.maxSpeedConverted.roundToInt()} $unitLabel"
    val avgText = "${uiState.avgSpeedConverted.roundToInt()} $unitLabel"
    val distanceText = SpeedConverter.formatDistance(uiState.distanceMeters, uiState.unit)
    val durationText = SpeedConverter.formatDuration(uiState.durationMs)
    val altitudeText = SpeedConverter.formatAltitude(uiState.altitudeMeters, uiState.unit)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = stringResource(R.string.stat_max),
                value = maxText,
                valueColor = LocalVelocityColors.current.secondaryAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.stat_avg),
                value = avgText,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.stat_distance),
                value = distanceText,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = stringResource(R.string.stat_duration),
                value = durationText,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.stat_altitude),
                value = altitudeText,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.stat_heading),
                value = compassLabel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun unitDisplayLabel(unit: SpeedUnit): String = when (unit) {
    SpeedUnit.KMH -> stringResource(R.string.unit_kmh)
    SpeedUnit.MPH -> stringResource(R.string.unit_mph)
    SpeedUnit.KNOTS -> stringResource(R.string.unit_knots)
}

@Composable
fun formatCompassLabel(
    direction: GeoUtils.CardinalDirection?,
    bearingDegrees: Float?
): String {
    if (direction == null) return stringResource(R.string.compass_unavailable)
    val dirString = when (direction) {
        GeoUtils.CardinalDirection.N -> stringResource(R.string.compass_n)
        GeoUtils.CardinalDirection.NE -> stringResource(R.string.compass_ne)
        GeoUtils.CardinalDirection.E -> stringResource(R.string.compass_e)
        GeoUtils.CardinalDirection.SE -> stringResource(R.string.compass_se)
        GeoUtils.CardinalDirection.S -> stringResource(R.string.compass_s)
        GeoUtils.CardinalDirection.SW -> stringResource(R.string.compass_sw)
        GeoUtils.CardinalDirection.W -> stringResource(R.string.compass_w)
        GeoUtils.CardinalDirection.NW -> stringResource(R.string.compass_nw)
    }
    return if (bearingDegrees != null) {
        "$dirString ${bearingDegrees.roundToInt()}°"
    } else {
        dirString
    }
}
