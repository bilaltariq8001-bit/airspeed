package com.velocity.speedometer.ui.screens.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.util.SpeedConverter
import com.velocity.speedometer.ui.components.SpeedTimeChart
import com.velocity.speedometer.ui.components.StatCard
import com.velocity.speedometer.ui.components.TripRouteMap
import com.velocity.speedometer.ui.screens.speedometer.unitDisplayLabel
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val velocityColors = LocalVelocityColors.current

    val backCd = stringResource(R.string.cd_back)
    val exportGpxCd = stringResource(R.string.cd_export_gpx)
    val exportCsvCd = stringResource(R.string.cd_export_csv)
    val chooserTitle = stringResource(R.string.detail_share_chooser)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.detail_title),
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val trip = uiState.trip
        if (trip == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_no_points),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val unitLabel = unitDisplayLabel(uiState.unit)
            val dateFormatted = remember(trip.startTime) {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(trip.startTime))
            }
            val distanceText = SpeedConverter.formatDistance(trip.distanceMeters, uiState.unit)
            val durationText = SpeedConverter.formatDuration(trip.durationMs)
            val maxText = "${SpeedConverter.fromMps(trip.maxSpeedMps, uiState.unit).roundToInt()} $unitLabel"
            val avgText = "${SpeedConverter.fromMps(trip.avgSpeedMps, uiState.unit).roundToInt()} $unitLabel"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TripRouteMap(points = uiState.trackPoints)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = stringResource(R.string.stat_distance),
                        value = distanceText,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.stat_duration),
                        value = durationText,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = stringResource(R.string.stat_max),
                        value = maxText,
                        valueColor = velocityColors.secondaryAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.stat_avg),
                        value = avgText,
                        modifier = Modifier.weight(1f)
                    )
                }

                SpeedTimeChart(
                    points = uiState.trackPoints,
                    unit = uiState.unit,
                    unitLabel = unitLabel
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.buildGpxPayload()?.let { (filename, content) ->
                                shareTripExport(
                                    context = context,
                                    filename = filename,
                                    mimeType = "application/gpx+xml",
                                    content = content,
                                    chooserTitle = chooserTitle
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .semantics { contentDescription = exportGpxCd },
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Rounded.IosShare, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.detail_export_gpx),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.buildCsvPayload()?.let { (filename, content) ->
                                shareTripExport(
                                    context = context,
                                    filename = filename,
                                    mimeType = "text/csv",
                                    content = content,
                                    chooserTitle = chooserTitle
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .semantics { contentDescription = exportCsvCd },
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.IosShare, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.detail_export_csv),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

private fun shareTripExport(
    context: Context,
    filename: String,
    mimeType: String,
    content: String,
    chooserTitle: String
) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val outFile = File(exportDir, filename)
    outFile.writeText(content)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outFile
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}
