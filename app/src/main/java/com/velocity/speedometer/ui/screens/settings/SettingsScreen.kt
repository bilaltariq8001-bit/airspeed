package com.velocity.speedometer.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.velocity.speedometer.domain.model.GaugeViewMode
import com.velocity.speedometer.domain.model.SpeedUnit
import com.velocity.speedometer.domain.model.ThemeMode
import com.velocity.speedometer.domain.util.SpeedConverter
import com.velocity.speedometer.ui.screens.speedometer.unitDisplayLabel
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import com.velocity.speedometer.ui.theme.VelocityDanger
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val velocityColors = LocalVelocityColors.current
    var showResetOdometerDialog by remember { mutableStateOf(false) }

    val backCd = stringResource(R.string.cd_back)
    val aboutCd = stringResource(R.string.cd_about_button)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Display & Units
            SectionHeader(stringResource(R.string.settings_section_display))
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.settings_unit_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SpeedUnit.entries.forEachIndexed { index, unit ->
                                SegmentedButton(
                                    selected = settings.unit == unit,
                                    onClick = { viewModel.setUnit(unit) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = SpeedUnit.entries.size
                                    )
                                ) {
                                    Text(text = unitDisplayLabel(unit))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = velocityColors.divider)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.settings_theme_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                val label = when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                }
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ThemeMode.entries.size
                                    )
                                ) {
                                    Text(text = label)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = velocityColors.divider)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.settings_default_view_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            GaugeViewMode.entries.forEachIndexed { index, viewMode ->
                                val label = when (viewMode) {
                                    GaugeViewMode.ANALOG -> stringResource(R.string.view_analog)
                                    GaugeViewMode.DIGITAL -> stringResource(R.string.view_digital)
                                }
                                SegmentedButton(
                                    selected = settings.defaultView == viewMode,
                                    onClick = { viewModel.setDefaultView(viewMode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = GaugeViewMode.entries.size
                                    )
                                ) {
                                    Text(text = label)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Speed Limit Alerts
            SectionHeader(stringResource(R.string.settings_section_alerts))
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_speed_limit_toggle),
                        checked = settings.speedLimitEnabled,
                        onCheckedChange = viewModel::setSpeedLimitEnabled
                    )

                    val unitLabel = unitDisplayLabel(settings.unit)
                    val maxScale = settings.unit.scaleMax.toFloat()
                    val currentLimit = SpeedConverter.fromMps(settings.speedLimitMps, settings.unit)
                        .coerceIn(10f, maxScale)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(
                                R.string.settings_speed_limit_value,
                                currentLimit.roundToInt(),
                                unitLabel
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (settings.speedLimitEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Slider(
                            value = currentLimit,
                            onValueChange = { newVal ->
                                val snapped = ((newVal / 5f).roundToInt() * 5f).coerceIn(10f, maxScale)
                                viewModel.setSpeedLimitInCurrentUnit(snapped, settings.unit)
                            },
                            valueRange = 10f..maxScale,
                            enabled = settings.speedLimitEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = velocityColors.divider)

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_alert_vibration),
                        checked = settings.alertVibration,
                        onCheckedChange = viewModel::setAlertVibration
                    )

                    HorizontalDivider(color = velocityColors.divider)

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_alert_sound),
                        checked = settings.alertSound,
                        onCheckedChange = viewModel::setAlertSound
                    )
                }
            }

            // Section 3: GPS & Trip
            SectionHeader(stringResource(R.string.settings_section_tracking))
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_keep_screen_on),
                        checked = settings.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn
                    )

                    HorizontalDivider(color = velocityColors.divider)

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_auto_pause),
                        subtitle = stringResource(R.string.settings_auto_pause_desc),
                        checked = settings.autoPauseEnabled,
                        onCheckedChange = viewModel::setAutoPauseEnabled
                    )

                    HorizontalDivider(color = velocityColors.divider)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.settings_gps_interval),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        val intervals = listOf(
                            500L to stringResource(R.string.interval_500ms),
                            1000L to stringResource(R.string.interval_1s),
                            2000L to stringResource(R.string.interval_2s)
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            intervals.forEachIndexed { index, (ms, label) ->
                                SegmentedButton(
                                    selected = settings.gpsIntervalMs == ms,
                                    onClick = { viewModel.setGpsIntervalMs(ms) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = intervals.size
                                    )
                                ) {
                                    Text(text = label)
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Odometer & Info
            SectionHeader(stringResource(R.string.settings_section_data))
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.settings_odometer_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = SpeedConverter.formatDistance(settings.odometerMeters, settings.unit),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(
                            onClick = { showResetOdometerDialog = true },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, VelocityDanger)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_reset_odometer),
                                color = VelocityDanger,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    HorizontalDivider(color = velocityColors.divider)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable(onClick = onOpenAbout)
                            .semantics { contentDescription = aboutCd },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.settings_about),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetOdometerDialog) {
        AlertDialog(
            onDismissRequest = { showResetOdometerDialog = false },
            title = { Text(stringResource(R.string.settings_reset_odometer_title)) },
            text = { Text(stringResource(R.string.settings_reset_odometer_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetOdometer()
                        showResetOdometerDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_reset),
                        color = VelocityDanger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetOdometerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val velocityColors = LocalVelocityColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, velocityColors.divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
