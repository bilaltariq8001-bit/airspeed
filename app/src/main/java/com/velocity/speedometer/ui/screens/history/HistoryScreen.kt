package com.velocity.speedometer.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.Trip
import com.velocity.speedometer.domain.util.SpeedConverter
import com.velocity.speedometer.ui.screens.speedometer.unitDisplayLabel
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import com.velocity.speedometer.ui.theme.StatValueTextStyle
import com.velocity.speedometer.ui.theme.VelocityDanger
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onTripClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteTripId by remember { mutableStateOf<Long?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val backCd = stringResource(R.string.cd_back)
    val deleteAllCd = stringResource(R.string.cd_delete_all_trips)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = backCd }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = backCd
                        )
                    }
                },
                actions = {
                    if (uiState.trips.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = deleteAllCd }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = deleteAllCd,
                                tint = VelocityDanger
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (uiState.trips.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.history_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.history_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.trips,
                    key = { it.id }
                ) { trip ->
                    TripHistoryCard(
                        trip = trip,
                        unit = uiState.unit,
                        onClick = { onTripClick(trip.id) },
                        onDeleteClick = { pendingDeleteTripId = trip.id }
                    )
                }
            }
        }
    }

    pendingDeleteTripId?.let { tripId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTripId = null },
            title = { Text(stringResource(R.string.history_delete_one_title)) },
            text = { Text(stringResource(R.string.history_delete_one_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrip(tripId)
                        pendingDeleteTripId = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = VelocityDanger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTripId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.history_delete_all_title)) },
            text = { Text(stringResource(R.string.history_delete_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllTrips()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = VelocityDanger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun TripHistoryCard(
    trip: Trip,
    unit: SpeedUnit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val velocityColors = LocalVelocityColors.current
    val unitLabel = unitDisplayLabel(unit)
    val dateFormatted = remember(trip.startTime) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(trip.startTime))
    }
    val distanceText = SpeedConverter.formatDistance(trip.distanceMeters, unit)
    val durationText = SpeedConverter.formatDuration(trip.durationMs)
    val maxText = "${SpeedConverter.fromMps(trip.maxSpeedMps, unit).roundToInt()} $unitLabel"
    val avgText = "${SpeedConverter.fromMps(trip.avgSpeedMps, unit).roundToInt()} $unitLabel"
    val deleteCd = stringResource(R.string.cd_delete_trip)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, velocityColors.divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = deleteCd }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = deleteCd,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricMiniColumn(label = stringResource(R.string.stat_distance), value = distanceText)
                MetricMiniColumn(label = stringResource(R.string.stat_duration), value = durationText)
                MetricMiniColumn(label = stringResource(R.string.stat_max), value = maxText)
                MetricMiniColumn(label = stringResource(R.string.stat_avg), value = avgText)
            }
        }
    }
}

@Composable
private fun MetricMiniColumn(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = StatValueTextStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
